package org.poc.objs.service.web

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

class SpaRoutingFilterTest {

    private lateinit var workbench: SpaRoutingFilter
    private lateinit var domain: SpaRoutingFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var chain: FilterChain

    @BeforeEach
    fun setUp() {
        workbench = SpaRoutingFilter("/workbench", "/workbench/index.html", false)
        domain = SpaRoutingFilter("/app", "/app/index.html", true)
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        chain = mock(FilterChain::class.java)
        `when`(request.contextPath).thenReturn("")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/", "", "/app/index.html"])
    fun shouldRedirectRootToApp_whenDomainFilter(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        domain.doFilter(request, response, chain)
        verify(response).sendRedirect("/app/")
        verifyNoInteractions(chain)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/", "/api/v1/objs/graphs"])
    fun shouldPassThroughNonWorkbench_whenWorkbenchFilter(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        workbench.doFilter(request, response, chain)
        verify(chain).doFilter(request, response)
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["/workbench/style.css", "/workbench/assets/index.js"])
    fun shouldPassThroughStaticWorkbenchFile(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        workbench.doFilter(request, response, chain)
        verify(chain).doFilter(request, response)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/app/", "/app/collections", "/app/collections/abc-123/objects/x"])
    fun shouldForwardDomainSpaRoute(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        val dispatcher = mock(RequestDispatcher::class.java)
        `when`(request.getRequestDispatcher("/app/index.html")).thenReturn(dispatcher)
        domain.doFilter(request, response, chain)
        verify(dispatcher).forward(request, response)
        verifyNoInteractions(chain)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/workbench/", "/workbench/explorer", "/workbench/model/Dataset/1.0.0"])
    fun shouldForwardWorkbenchSpaRoute(url: String) {
        `when`(request.requestURI).thenReturn(url)
        `when`(request.method).thenReturn("GET")
        val dispatcher = mock(RequestDispatcher::class.java)
        `when`(request.getRequestDispatcher("/workbench/index.html")).thenReturn(dispatcher)
        workbench.doFilter(request, response, chain)
        verify(dispatcher).forward(request, response)
        verifyNoInteractions(chain)
    }
}
