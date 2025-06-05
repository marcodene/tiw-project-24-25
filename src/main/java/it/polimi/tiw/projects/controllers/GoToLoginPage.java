package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;


@WebServlet("/")
public class GoToLoginPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
    
    public GoToLoginPage() {
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
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// If the user is logged in (already present in session) redirect to the HomePage
    	String homePagePath = getServletContext().getContextPath() + "/Home";
    	HttpSession session = request.getSession();
    	if(session!=null && session.getAttribute("user")!=null) {
    		response.sendRedirect(homePagePath);
    		return;
    	}
    	
    	String path = "/WEB-INF/index.html";
    	ServletContext servletContext = getServletContext();
    	JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
    	WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
    	
    	// FLASH MESSAGES: Leggi messaggi dalla sessione
    	
    	// === MESSAGGI DI SUCCESSO (es. dalla registrazione) ===
    	List<String> successMessages = FlashMessagesManager.getAndClearSuccessMessages(request);
    	if (!successMessages.isEmpty()) {
    	    ctx.setVariable("successMessage", successMessages.get(0));
    	}
    	
    	// === MESSAGGI DI ERRORE LOGIN ===
    	Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
    	
    	// Filtra errori login (con prefisso "login_")
    	for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
    	    if (entry.getKey().startsWith("login_")) {
    	        // Per compatibilità con template esistente, usa errorMsg
    	        String cleanKey = entry.getKey().substring(6); // rimuove "login_"
    	        if ("credentialsError".equals(cleanKey) || "generalError".equals(cleanKey)) {
    	            ctx.setVariable("errorMsg", entry.getValue());
    	            break; // Prendi solo il primo errore per semplicità
    	        }
    	    }
    	}
    	
		templateEngine.process(path, ctx, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}