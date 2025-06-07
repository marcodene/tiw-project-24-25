package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/Account")
public class GoToAccountPage extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection = null;
       
    public GoToAccountPage() {
        super();
    }
    
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);     
        WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// If the user is not logged in (not present in session) redirect to the login
        String loginpath = getServletContext().getContextPath(); // path to the loginPage
        HttpSession session = request.getSession();
        if(session.isNew() || session.getAttribute("user") == null) {
            response.sendRedirect(loginpath);
            return;
        }
        
        // Set up template context
        String path = "/WEB-INF/AccountPage.html";
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        
        // PATTERN POST-REDIRECT-GET: Leggi i flash messages dalla sessione
        
        // Recupera e rimuove gli errori strutturati per campo
        Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
        
        // Filtra errori account (con prefisso "account_")
        Map<String, String> accountFieldErrors = new HashMap<>();
        for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
            if (entry.getKey().startsWith("account_")) {
                // Rimuovi prefisso "account_" per compatibilità template
                String cleanKey = entry.getKey().substring(8); // rimuove "account_"
                accountFieldErrors.put(cleanKey, entry.getValue());
            }
        }
        
        // Imposta variabili per template
        if (!accountFieldErrors.isEmpty()) {
            ctx.setVariable("errorMessages", accountFieldErrors);
        }
        
        templateEngine.process(path, ctx, response.getWriter());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}