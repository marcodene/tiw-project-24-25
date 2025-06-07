package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

public abstract class ServletBase extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    protected Connection connection;
    protected TemplateEngine templateEngine;
    
    protected boolean needsDatabase() {
        return true;
    }
    
    protected boolean needsTemplateEngine() {
        return true;
    }
    
    protected boolean needsAuth() {
        return true;
    }
    
    public void init() throws ServletException {
        if (needsDatabase()) {
            connection = ConnectionHandler.getConnection(getServletContext());
        }
        
        if (needsTemplateEngine()) {
            setupTemplateEngine();
        }
    }
    
    private void setupTemplateEngine() {
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);     
        WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
    }
    
    public void destroy() {
        if (connection != null) {
            try {
                ConnectionHandler.closeConnection(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Controlla se l'utente è loggato
    protected User checkLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!needsAuth()) {
            return null;
        }
        
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") == null) {
            String loginPath = getServletContext().getContextPath() + "/";
            response.sendRedirect(loginPath);
            return null;
        }
        
        return (User) session.getAttribute("user");
    }
    
    // Redirect se già loggato (per login e register)
    protected boolean redirectIfLogged(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (session != null && session.getAttribute("user") != null) {
            String homePath = getServletContext().getContextPath() + "/Home";
            response.sendRedirect(homePath);
            return true;
        }
        return false;
    }
    
    // Crea il context per thymeleaf
    protected WebContext createContext(HttpServletRequest request, HttpServletResponse response) {
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        return new WebContext(webApplication.buildExchange(request, response), request.getLocale());
    }
    
    // Pattern Post-Redirect-Get
    protected void doRedirect(HttpServletRequest request, HttpServletResponse response,
                             String redirectPath, String successMessage,
                             Map<String, String> errors, Map<String, String> formValues) throws IOException {
        
        if (successMessage != null) {
            FlashMessagesManager.addSuccessMessage(request, successMessage);
        }
        
        if (errors != null && !errors.isEmpty()) {
            FlashMessagesManager.addFieldErrors(request, errors);
        }
        
        if (formValues != null && !formValues.isEmpty()) {
            FlashMessagesManager.addFormValues(request, formValues);
        }
        
        response.sendRedirect(redirectPath);
    }
    
    // Pattern Post-Redirect-Get semplificato
    protected void doRedirect(HttpServletRequest request, HttpServletResponse response,
                             String redirectPath, String successMessage) throws IOException {
        doRedirect(request, response, redirectPath, successMessage, null, null);
    }
    
    protected void doRedirect(HttpServletRequest request, HttpServletResponse response,
                             String redirectPath, Map<String, String> errors) throws IOException {
        doRedirect(request, response, redirectPath, null, errors, null);
    }
    
    // Parsing di parametri int
    protected int getIntParam(HttpServletRequest request, String paramName) {
        String param = request.getParameter(paramName);
        if (param == null || param.trim().isEmpty()) {
            return -1;
        }
        
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    // Check se stringa è vuota
    protected boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    // Redirect con parametri URL (per quando la sessione è invalidata)
    protected void doRedirectWithParams(HttpServletResponse response, String path, String... params) throws IOException {
        StringBuilder url = new StringBuilder(path);
        
        if (params.length > 0) {
            url.append("?");
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) url.append("&");
                url.append(params[i]).append("=").append(params[i + 1]);
            }
        }
        
        response.sendRedirect(url.toString());
    }
}