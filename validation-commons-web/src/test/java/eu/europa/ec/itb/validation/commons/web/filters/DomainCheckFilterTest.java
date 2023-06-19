package eu.europa.ec.itb.validation.commons.web.filters;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import eu.europa.ec.itb.validation.commons.web.Constants;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DomainCheckFilterTest {

    private WebDomainConfigCache<?> domainConfigCache;
    private DomainCheckFilter<?, ?> filter;

    @BeforeEach
    void setupFilter() throws NoSuchFieldException, IllegalAccessException {
        var domainWithoutForm = mock(WebDomainConfig.class);
        when(domainWithoutForm.isDefined()).thenReturn(true);
        when(domainWithoutForm.getChannels()).thenReturn(Set.of(ValidatorChannel.SOAP_API));
        when(domainWithoutForm.isSupportMinimalUserInterface()).thenReturn(true);
        var domainWithForm = mock(WebDomainConfig.class);
        when(domainWithForm.isDefined()).thenReturn(true);
        when(domainWithForm.getChannels()).thenReturn(Set.of(ValidatorChannel.FORM));
        when(domainWithForm.isSupportMinimalUserInterface()).thenReturn(true);
        var domainNotDefined = mock(WebDomainConfig.class);
        when(domainNotDefined.isDefined()).thenReturn(false);
        var domainWithoutMinimalUI = mock(WebDomainConfig.class);
        when(domainWithoutMinimalUI.isDefined()).thenReturn(true);
        when(domainWithoutMinimalUI.getChannels()).thenReturn(Set.of(ValidatorChannel.FORM));
        when(domainWithoutMinimalUI.isSupportMinimalUserInterface()).thenReturn(false);
        var domainWithNPE = mock(WebDomainConfig.class);
        when(domainWithNPE.isDefined()).thenReturn(true);
        when(domainWithNPE.getChannels()).thenThrow(NullPointerException.class);

        domainConfigCache = mock(WebDomainConfigCache.class);
        when(domainConfigCache.getConfigForDomainName("domainWithoutForm")).thenAnswer(call -> domainWithoutForm);
        when(domainConfigCache.getConfigForDomainName("domainWithForm")).thenAnswer(call -> domainWithForm);
        when(domainConfigCache.getConfigForDomainName("domainNotDefined")).thenAnswer(call -> domainNotDefined);
        when(domainConfigCache.getConfigForDomainName("domainWithoutMinimalUI")).thenAnswer(call -> domainWithoutMinimalUI);
        when(domainConfigCache.getConfigForDomainName("nullDomain")).thenReturn(null);
        when(domainConfigCache.getConfigForDomainName("domainWithNPE")).thenAnswer(call -> domainWithNPE);

        filter = new DomainCheckFilter<>();
        var domainConfigsField = DomainCheckFilter.class.getDeclaredField("domainConfigs");
        domainConfigsField.setAccessible(true);
        domainConfigsField.set(filter, domainConfigCache);
    }

    private HttpServletRequest getRequest(String path, String expectedDomainName, Boolean expectedMinimalFlag) {
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getServletPath()).thenReturn(path);
        doAnswer(call -> {
            if (WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE.equals(call.getArgument(0))) {
                assertSame(domainConfigCache.getConfigForDomainName(expectedDomainName), call.getArgument(1));
            } else if (Constants.IS_MINIMAL.equals(call.getArgument(0))) {
                assertEquals(expectedMinimalFlag, call.getArgument(1));
            } else {
                fail("Unexpected call to setAttribute");
            }
            return null;
        }).when(request).setAttribute(anyString(), any());
        return request;
    }

    @Test
    void testValidateDomainNoForm() {
        var request = getRequest("/domainWithoutForm/upload", null, false);
        assertThrows(NotFoundException.class, () -> filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void testValidateDomainNull() {
        var request = getRequest("/nullDomain/upload", null, false);
        assertThrows(NotFoundException.class, () -> filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void testValidateDomainNotDefined() {
        var request = getRequest("/domainNotDefined/upload", null, false);
        assertThrows(NotFoundException.class, () -> filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void testValidateDomainForMinimalUIOK() {
        var request = getRequest("/domainWithForm/uploadm", "domainWithForm", true);
        assertDoesNotThrow(() -> filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void testValidateDomainForMinimalUINOK() {
        var request = getRequest("/domainWithoutMinimalUI/uploadm", null, true);
        assertThrows(NotFoundException.class, () -> filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void testUnableToDetermineDomain() {
        var request = getRequest("///uploadm", null, true);
        assertThrows(NotFoundException.class, () -> filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void testUnexpectedError() {
        var request = getRequest("/domainWithNPE/uploadm", "domainWithNPE", true);
        assertThrows(NotFoundException.class, () -> filter.doFilterInternal(request, mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

}
