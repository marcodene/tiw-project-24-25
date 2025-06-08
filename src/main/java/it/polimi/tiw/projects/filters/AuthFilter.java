package it.polimi.tiw.projects.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter to check if the user is authenticated for API requests
 */
public class AuthFilter implements Filter {
    
    public AuthFilter() {
    }
    
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // Allow access to login page and related resources
        if (requestURI.endsWith("/login.html") || 
            requestURI.endsWith("/index.html") ||
            requestURI.equals(contextPath + "/") ||
            requestURI.endsWith("/css/style.css") ||
            requestURI.endsWith("/js/utils.js") ||
            requestURI.endsWith("/js/auth.js") ||
            requestURI.endsWith("/js/app.js") ||
            requestURI.endsWith("/js/router.js") ||
            requestURI.endsWith("/js/state.js") ||
            requestURI.contains("/js/components/") ||
            requestURI.contains("/api/login") ||
            requestURI.contains("/api/register") ||
            requestURI.contains("/api/checkAuth")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check if user is logged in
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            // For API requests, return JSON error
            if (requestURI.startsWith(contextPath + "/api/")) {
                httpResponse.setContentType("application/json");
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().println("{ \"error\": \"User not logged in\" }");
                return;
            }
            // For page requests, redirect to login
            else {
                httpResponse.sendRedirect(contextPath + "/login.html");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    public void destroy() {
    }
}