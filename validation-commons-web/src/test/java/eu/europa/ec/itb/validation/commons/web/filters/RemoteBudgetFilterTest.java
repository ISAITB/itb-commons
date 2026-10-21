package eu.europa.ec.itb.validation.commons.web.filters;

import eu.europa.ec.itb.validation.commons.RemoteResourceBudget;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that each request is processed with the limits on remote resources applied.
 */
class RemoteBudgetFilterTest {

    private final RemoteBudgetFilter filter = new RemoteBudgetFilter();

    @BeforeEach
    void setUp() {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getRemoteLimits()).thenReturn(new ApplicationConfig.RemoteLimits());
        ReflectionTestUtils.setField(filter, "appConfig", appConfig);
    }

    @Test
    void testRequestIsProcessedWithABudgetThatIsRemovedAfterwards() throws Exception {
        var budgetInRequest = new AtomicReference<RemoteResourceBudget>();
        assertNull(RemoteResourceBudget.current());
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> budgetInRequest.set(RemoteResourceBudget.current()));
        assertNotNull(budgetInRequest.get());
        assertNull(RemoteResourceBudget.current(), "The budget must not leak to other requests handled by the same thread");
    }

    @Test
    void testEachRequestHasItsOwnBudget() throws Exception {
        var first = new AtomicReference<RemoteResourceBudget>();
        var second = new AtomicReference<RemoteResourceBudget>();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> first.set(RemoteResourceBudget.current()));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> second.set(RemoteResourceBudget.current()));
        assertNotSame(first.get(), second.get());
    }

    @Test
    void testBudgetIsRemovedIfProcessingFails() {
        assertThrows(ServletException.class, () -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            throw new ServletException("Failed");
        }));
        assertNull(RemoteResourceBudget.current());
    }

}
