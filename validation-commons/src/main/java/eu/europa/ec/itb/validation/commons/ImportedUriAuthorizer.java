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

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Class used to validate URIs referenced within validation artefacts.
 */
public class ImportedUriAuthorizer {

    private static final List<CidrInfo> CIDR_BLACKLIST = List.of(
            // IPv4
            CidrInfo.from("127.0.0.0/8"),       // loopback
            CidrInfo.from("10.0.0.0/8"),        // RFC 1918
            CidrInfo.from("172.16.0.0/12"),     // RFC 1918
            CidrInfo.from("192.168.0.0/16"),    // RFC 1918
            CidrInfo.from("169.254.0.0/16"),    // link-local / IMDS
            CidrInfo.from("0.0.0.0/8"),         // "this" network
            CidrInfo.from("100.64.0.0/10"),     // shared address space (RFC 6598)
            CidrInfo.from("192.0.0.0/24"),      // IETF protocol assignments
            CidrInfo.from("198.18.0.0/15"),     // benchmarking
            CidrInfo.from("192.0.2.0/24"),      // TEST-NET-1
            CidrInfo.from("198.51.100.0/24"),   // TEST-NET-2
            CidrInfo.from("203.0.113.0/24"),    // TEST-NET-3
            CidrInfo.from("240.0.0.0/4"),       // reserved
            // IPv6
            CidrInfo.from("::1/128"),           // loopback
            CidrInfo.from("fd00::/8"),          // unique local
            CidrInfo.from("fe80::/10"),         // link-local
            CidrInfo.from("fc00::/7"),          // unique local (full range)
            CidrInfo.from("::/128"),            // unspecified
            CidrInfo.from("100::/64")           // discard prefix
    );

    private final Collection<NormalizedURI> whitelistUris;

    /**
     * Constructor.
     *
     * @param whitelistUris The whitelist base URI paths.
     */
    private ImportedUriAuthorizer(Collection<NormalizedURI> whitelistUris) {
        this.whitelistUris = whitelistUris;
    }

    /**
     * Check whether the provided URI is considered trusted.
     *
     * @param uri The URI to check.
     * @return The check result.
     * @throws ValidatorException If the URI is blocked.
     */
    public boolean isUriAllowed(URI uri) {
        if (!uri.isAbsolute()) {
            return true;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ValidatorException("validator.label.exception.notAllowedToReadImportedUri", uri);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ValidatorException("validator.label.exception.notAllowedToReadImportedUri", uri);
        }
        boolean internalOrReserved = hostRequiresWhitelist(host);
        if (!internalOrReserved) {
            // Public address: allow without URI-prefix whitelist checks.
            return true;
        }
        // Internal/reserved address: require explicit whitelist prefix match.
        var normalizedUri = NormalizedURI.of(uri);
        boolean allowed = whitelistUris.stream()
                .anyMatch(whitelistedUri -> whitelistedUri.isPrefixOf(normalizedUri));
        if (!allowed) {
            throw new ValidatorException("validator.label.exception.notAllowedToReadImportedUri", uri);
        }
        return true;
    }

    /**
     * Check whether the provided URI is considered trusted.
     *
     * @param uri The URI to check.
     * @return The check result.
     * @throws ValidatorException If the URI is blocked.
     * @throws IllegalArgumentException If the URI string fails to be parsed.
     */
    public boolean isUriAllowed(String uri) {
        return isUriAllowed(URI.create(uri));
    }

    /**
     * Check whether the provided host needs whitelist checking.
     *
     * @param host The host to check.
     * @return The check result.
     */
    private boolean hostRequiresWhitelist(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            // Fail-safe: unresolved hosts are treated as disallowed/internal-risk.
            return true;
        }
        // Fail-safe for mixed records: if any resolved address is internal/reserved, treat host as internal/reserved.
        for (InetAddress address : addresses) {
            if (addressRequiresWhitelist(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the provided address requires whitelist checking.
     *
     * @param address The address to check.
     * @return The check result.
     */
    private boolean addressRequiresWhitelist(InetAddress address) {
        if (address.isLoopbackAddress() ||
                address.isSiteLocalAddress() ||
                address.isLinkLocalAddress() ||
                address.isAnyLocalAddress() ||
                address.isMulticastAddress()) {
            return true;
        }
        return isInBlacklist(address);
    }

    /**
     * Check whether the provided address is in the internal address blacklist.
     *
     * @param address The address to check.
     * @return The check result.
     */
    private boolean isInBlacklist(InetAddress address) {
        return CIDR_BLACKLIST.stream().anyMatch(cidr -> isInCidrRange(address, cidr));
    }

    /**
     * Check whether the provided address is within the provided range.
     *
     * @param address The address to check.
     * @param cidr The range to consider.
     * @return The check result.
     */
    private boolean isInCidrRange(InetAddress address, CidrInfo cidr) {
        byte[] addressBytes = address.getAddress();
        if (addressBytes.length != cidr.rangeAddressBytes().length) {
            return false;
        }
        int fullBytes = cidr.prefixLength() / 8;
        int remainingBits = cidr.prefixLength() % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (addressBytes[i] != cidr.rangeAddressBytes()[i]) {
                return false;
            }
        }
        if (remainingBits > 0) {
            int mask = 0xFF << (8 - remainingBits);
            return (addressBytes[fullBytes] & mask) == (cidr.rangeAddressBytes()[fullBytes] & mask);
        }
        return true;
    }

    /**
     * Construct an authorizer (if needed) based on the application configuration, domain configuration and specific validation type.
     *
     * @param appConfig The application configuration.
     * @param domainConfig The domain configuration.
     * @param validationType The specific validation type.
     * @return The authorizer to use (or empty if no authorization is needed).
     */
    public static Optional<ImportedUriAuthorizer> from(ApplicationConfig appConfig, DomainConfig domainConfig, String validationType) {
        if (domainConfig.getArtifactInfo().get(validationType).getOverallExternalArtifactSupport() == ExternalArtifactSupport.NONE) {
            // If no user-provided artefacts are supported then no authorization is needed (we don't authorize internally configured artefacts).
            return Optional.empty();
        } else {
            // If user-provided artefacts are possible then all referenced URIs must be public or - if internal - whitelisted (directly or via base URIs).
            return Optional.of(new ImportedUriAuthorizer(appConfig.getNormalizedAllowedUriImports()));
        }
    }

    /**
     * Helper record to record CIDR info.
     *
     * @param rangeAddressBytes The address bytes.
     * @param prefixLength The prefix length.
     */
    private record CidrInfo(byte[] rangeAddressBytes, int prefixLength) {
        private static CidrInfo from(String cidr) {
            String[] parts = StringUtils.split(cidr, '/');
            try {
                int prefixLength = Integer.parseInt(parts[1]);
                return new CidrInfo(InetAddress.getByName(parts[0]).getAddress(), prefixLength);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to parse CIDR", e);
            }
        }
    }

}
