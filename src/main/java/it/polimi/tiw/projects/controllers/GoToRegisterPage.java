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
import java.util.List;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;
import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;

@WebServlet("/GoToRegisterPage")
public class GoToRegisterPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;
       
       
    public GoToRegisterPage() {
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
		// If the user is logged in (already present in session) redirect to the HomePage
        String homePagePath = getServletContext().getContextPath() + "/Home";
        HttpSession session = request.getSession();
        if(session!=null && session.getAttribute("user")!=null) {
            response.sendRedirect(homePagePath);
            return;
        }
        
        String path = "/WEB-INF/RegisterPage.html";
        
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        
        // PATTERN POST-REDIRECT-GET: Leggi SOLO gli errori dalla sessione
        // I messaggi di successo ora vanno direttamente alla LoginPage
        
        // === REGISTER FLASH MESSAGES (SOLO ERRORI) ===
        // Recupera e rimuove gli errori e valori strutturati dalla sessione
        Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
        Map<String, String> allFormValues = FlashMessagesManager.getAndClearFormValues(request);
        
        // Filtra errori e valori per register (con prefisso "register_")
        Map<String, String> registerFieldErrors = new HashMap<>();
        Map<String, String> registerFormValues = new HashMap<>();
        
        // Filtra errori con prefisso "register_"
        for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
            if (entry.getKey().startsWith("register_")) {
                // Rimuovi prefisso "register_" per compatibilità template
                String cleanKey = entry.getKey().substring(9); // rimuove "register_"
                registerFieldErrors.put(cleanKey, entry.getValue());
            }
        }
        
        // Filtra valori con prefisso "register_"
        for (Map.Entry<String, String> entry : allFormValues.entrySet()) {
            if (entry.getKey().startsWith("register_")) {
                // Rimuovi prefisso "register_" per compatibilità template
                String cleanKey = entry.getKey().substring(9); // rimuove "register_"
                registerFormValues.put(cleanKey, entry.getValue());
            }
        }
        
        // Imposta variabili per template (solo errori, nessun successMessage)
        if (!registerFieldErrors.isEmpty()) {
            ctx.setVariable("errorMessages", registerFieldErrors);
        }
        if (!registerFormValues.isEmpty()) {
            ctx.setVariable("formValues", registerFormValues);
        }
        
        // Check if coming from login page with error
        String loginError = request.getParameter("loginError");
        boolean hasLoginError = loginError != null && loginError.equals("true");
        ctx.setVariable("showRegisterButton", hasLoginError);
        
        templateEngine.process(path, ctx, response.getWriter());
    }
    
    // Needed to handle forwards from Register servlet in case of errors
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