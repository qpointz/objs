package org.poc.objs.sbom.web

import jakarta.servlet.FilterChain
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class SbomSpaRoutingFilterTest {

    private lateinit var filter: SbomSpaRoutingFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var chain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = SbomSpaRoutingFilter()
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        chain = mock(FilterChain::class.java)
        `when`(request.contextPath).thenReturn("")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/", "", "/ui", "/ui/index.html"])
    fun shouldRedirectRootToUiSlash(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        filter.doFilter(request, response, chain)
        verify(response).sendRedirect("/ui/")
        verifyNoInteractions(chain)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/api/v1/example/sbom/applications", "/workbench/explorer"])
    fun shouldPassThroughNonUi(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        filter.doFilter(request, response, chain)
        verify(chain).doFilter(request, response)
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["/ui/assets/index.js", "/ui/style.css"])
    fun shouldPassThroughStaticFile(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        filter.doFilter(request, response, chain)
        verify(chain).doFilter(request, response)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/ui/", "/ui/applications", "/ui/applications/abc/versions/v1", "/ui/portfolios", "/ui/portfolios/abc"])
    fun shouldForwardSpaRoute(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        val dispatcher = mock(RequestDispatcher::class.java)
        `when`(request.getRequestDispatcher("/ui/index.html")).thenReturn(dispatcher)
        filter.doFilter(request, response, chain)
        verify(dispatcher).forward(request, response)
        verifyNoInteractions(chain)
    }
}
