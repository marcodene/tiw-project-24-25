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
        
        // Skip for OPTIONS requests (preflight)
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check if user is logged in
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            httpResponse.setContentType("application/json");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().println("{ \"error\": \"User not logged in\" }");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    public void destroy() {
    }
}