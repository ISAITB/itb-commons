/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The limits applied to the remote resources loaded during a single validation, together with the usage recorded so far.
 * <p>
 * The budget applies to the thread that opened it (see {@link #open(ApplicationConfig.RemoteLimits)}). Remote lookups made
 * while no budget is open (e.g. the preloading of the artefacts configured for a domain) are not limited. Every remote
 * request is expected to be recorded via {@link #onRequest(URI)} and every response body to be read via
 * {@link #wrap(URI, InputStream)}; both are done by {@link BudgetedHttpClient}, through which all HTTP requests should be made.
 * <p>
 * Once a limit is exceeded the budget is exhausted: the (localised) exception for the first violation is thrown by every further
 * request, so that a validation cannot continue to load resources after having crossed a limit. As the libraries that make
 * the requests may wrap the exception raised, {@link #rethrowIfExceeded()} can be used to report the actual reason.
 */
public final class RemoteResourceBudget {

    private static final ThreadLocal<RemoteResourceBudget> CURRENT = new ThreadLocal<>();
    private static final ScheduledThreadPoolExecutor WATCHDOG = newWatchdog();

    private final long maxResourceSize;
    private final int maxRequests;
    private final long maxTotalSize;
    private final Duration requestTimeout;
    private int requests = 0;
    private long totalSize = 0;
    private ValidatorException violation;

    /**
     * Constructor.
     *
     * @param limits The limits to apply.
     */
    private RemoteResourceBudget(ApplicationConfig.RemoteLimits limits) {
        this.maxResourceSize = limits.getMaxResourceSize().toBytes();
        this.maxRequests = limits.getMaxRequests();
        this.maxTotalSize = limits.getMaxTotalSize().toBytes();
        this.requestTimeout = Duration.ofMillis(limits.getRequestTimeout());
    }

    /**
     * An open scope, to be closed once the work it covers is complete.
     */
    public interface Scope extends AutoCloseable {

        /**
         * Close the scope, restoring the state before it was opened.
         */
        @Override
        void close();

    }

    private static ScheduledThreadPoolExecutor newWatchdog() {
        var executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            var thread = new Thread(runnable, "remote-resource-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    /**
     * Open a budget for the current thread. If a budget is already open (e.g. for the encompassing web request) it is
     * kept, meaning that nested scopes share the same budget.
     *
     * @param limits The limits to apply.
     * @return The scope to close once the work is complete.
     */
    public static Scope open(ApplicationConfig.RemoteLimits limits) {
        if (CURRENT.get() != null) {
            return () -> {};
        }
        CURRENT.set(new RemoteResourceBudget(limits));
        return CURRENT::remove;
    }

    /**
     * Exempt the current thread from the limits, for work that is not driven by a validation request (e.g. the download
     * of artefacts configured by the validator's administrator).
     *
     * @return The scope to close once the exempt work is complete.
     */
    public static Scope suspend() {
        var previous = CURRENT.get();
        if (previous == null) {
            return () -> {};
        }
        CURRENT.remove();
        return () -> CURRENT.set(previous);
    }

    /**
     * @return The budget of the current thread (null if the lookups of the thread are not limited).
     */
    public static RemoteResourceBudget current() {
        return CURRENT.get();
    }

    /**
     * Report the reason why the current thread's budget was exhausted, if this is the case. This is to be used when a
     * failure is reported by a library that does not preserve the exception raised on exceeding a limit.
     *
     * @throws ValidatorException If a limit was exceeded.
     */
    public static void rethrowIfExceeded() {
        var budget = CURRENT.get();
        if (budget != null) {
            synchronized (budget) {
                if (budget.violation != null) {
                    throw budget.violation;
                }
            }
        }
    }

    /**
     * Record a remote request, which includes those made to follow redirects.
     *
     * @param uri The URI of the request.
     * @throws ValidatorException If a limit has been exceeded.
     */
    public synchronized void onRequest(URI uri) {
        if (violation != null) {
            throw violation;
        }
        requests += 1;
        if (requests > maxRequests) {
            throw fail(new ValidatorException("validator.label.exception.remoteTooManyRequests", maxRequests));
        }
    }

    /**
     * @return The maximum time allowed for a request.
     */
    public Duration requestTimeout() {
        return requestTimeout;
    }

    /**
     * Wrap the body of a response so that its reading counts towards the budget and is limited accordingly.
     *
     * @param uri The URI of the request.
     * @param body The response body.
     * @return The stream to use in place of the body.
     */
    public InputStream wrap(URI uri, InputStream body) {
        return wrap(uri, body, -1);
    }

    /**
     * Wrap the body of a response so that its reading counts towards the budget and is limited accordingly.
     *
     * @param uri The URI of the request.
     * @param body The response body.
     * @param declaredLength The length declared by the response (negative if unknown). A declared length that exceeds
     *                       a limit is rejected before any of the body is read.
     * @return The stream to use in place of the body.
     */
    public InputStream wrap(URI uri, InputStream body, long declaredLength) {
        return new LimitedStream(uri, body, declaredLength);
    }

    private synchronized ValidatorException fail(ValidatorException exception) {
        if (violation == null) {
            violation = exception;
        }
        return violation;
    }

    private synchronized void addSize(long bytes) {
        totalSize += bytes;
    }

    private synchronized long currentTotalSize() {
        return totalSize;
    }

    private synchronized boolean isTotalSizeExceeded() {
        return totalSize > maxTotalSize;
    }

    /**
     * Stream that counts and limits the bytes read from a response.
     */
    private final class LimitedStream extends FilterInputStream {

        private final URI uri;
        private final long deadline;
        private final ScheduledFuture<?> watchdog;
        private final long declaredLength;
        private boolean declaredLengthChecked = false;
        private long size = 0;
        private volatile boolean timedOut = false;

        private LimitedStream(URI uri, InputStream body, long declaredLength) {
            super(body);
            this.uri = uri;
            this.declaredLength = declaredLength;
            this.deadline = System.nanoTime() + requestTimeout.toNanos();
            // The timeout also needs to apply to reads that block, which can only be done by closing the stream.
            this.watchdog = WATCHDOG.schedule(this::expire, requestTimeout.toNanos(), TimeUnit.NANOSECONDS);
        }

        private void expire() {
            timedOut = true;
            //noinspection ThrowableNotThrown
            fail(new ValidatorException("validator.label.exception.remoteRequestTimedOut", uri.toString(), requestTimeout.toMillis() / 1000.0));
            closeQuietly();
        }

        private void closeQuietly() {
            watchdog.cancel(false);
            try {
                in.close();
            } catch (IOException e) {
                // Ignore: we are failing already.
            }
        }

        private void checkAllowed() {
            synchronized (RemoteResourceBudget.this) {
                if (violation != null) {
                    closeQuietly();
                    throw violation;
                }
            }
            if (System.nanoTime() - deadline > 0) {
                expire();
                throw violation;
            }
            if (!declaredLengthChecked) {
                declaredLengthChecked = true;
                if (declaredLength > maxResourceSize) {
                    closeQuietly();
                    throw fail(new ValidatorException("validator.label.exception.remoteResourceTooLarge", uri.toString(), FileUtils.byteCountToDisplaySize(maxResourceSize)));
                }
                if (declaredLength > 0 && declaredLength > maxTotalSize - currentTotalSize()) {
                    closeQuietly();
                    throw fail(new ValidatorException("validator.label.exception.remoteTotalTooLarge", FileUtils.byteCountToDisplaySize(maxTotalSize)));
                }
            }
        }

        private void account(long bytes) {
            size += bytes;
            addSize(bytes);
            if (size > maxResourceSize) {
                closeQuietly();
                throw fail(new ValidatorException("validator.label.exception.remoteResourceTooLarge", uri.toString(), FileUtils.byteCountToDisplaySize(maxResourceSize)));
            }
            if (isTotalSizeExceeded()) {
                closeQuietly();
                throw fail(new ValidatorException("validator.label.exception.remoteTotalTooLarge", FileUtils.byteCountToDisplaySize(maxTotalSize)));
            }
        }

        private IOException translate(IOException e) {
            if (timedOut) {
                throw violation;
            }
            return e;
        }

        @Override
        public int read() throws IOException {
            checkAllowed();
            try {
                int value = in.read();
                if (value < 0) {
                    watchdog.cancel(false);
                } else {
                    account(1);
                }
                return value;
            } catch (IOException e) {
                throw translate(e);
            }
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            checkAllowed();
            try {
                int count = in.read(buffer, offset, length);
                if (count < 0) {
                    watchdog.cancel(false);
                } else {
                    account(count);
                }
                return count;
            } catch (IOException e) {
                throw translate(e);
            }
        }

        @Override
        public long skip(long n) throws IOException {
            // Skipped bytes are also received, so they are read to be counted.
            byte[] discard = new byte[(int) Math.min(8192, Math.max(n, 0))];
            long skipped = 0;
            while (skipped < n) {
                int count = read(discard, 0, (int) Math.min(discard.length, n - skipped));
                if (count < 0) {
                    break;
                }
                skipped += count;
            }
            return skipped;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void close() throws IOException {
            watchdog.cancel(false);
            super.close();
        }

    }

}
