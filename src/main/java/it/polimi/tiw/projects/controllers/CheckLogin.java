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

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.beans.User;


@WebServlet("/CheckLogin") 
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;
    
    public CheckLogin() {
        super();
    }
    
    public void init() throws ServletException {
    	ServletContext servletContext = getServletContext();
    	connection = ConnectionHandler.getConnection(servletContext);
    	JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);     
    	WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> errorMessages = new HashMap<>();
		boolean hasErrors = false;
		
		String username = null;
		String password = null;
		
		try {
			username = StringEscapeUtils.escapeJava(request.getParameter("username"));
			password = StringEscapeUtils.escapeJava(request.getParameter("password"));
			
			// Validazione parametri
			if(username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
				errorMessages.put("login_credentialsError", "Username and password are required");
				hasErrors = true;
			}
		} catch (Exception e) {
			errorMessages.put("login_generalError", "Invalid request data: " + e.getMessage());
			hasErrors = true;
		}
		
		User user = null;
		
		// Se non ci sono errori di validazione, controlla le credenziali
		if (!hasErrors) {
			UserDAO userDao = new UserDAO(connection);
			try {
				user = userDao.checkCredentials(username, password);
				
				if (user == null) {
					errorMessages.put("login_credentialsError", "Incorrect username or password");
					hasErrors = true;
				}
			} catch (SQLException e) {
				errorMessages.put("login_generalError", "Database error occurred. Please try again later.");
				hasErrors = true;
				e.printStackTrace();
			}
		}
		
		// PATTERN POST-REDIRECT-GET: Gestione risultato login
		
		if (!hasErrors && user != null) {
			// LOGIN RIUSCITO: Crea sessione e redirect alla Home
			request.getSession().setAttribute("user", user);
			String homePath = getServletContext().getContextPath() + "/Home";
			response.sendRedirect(homePath);
		} else {
			// LOGIN FALLITO: Flash messages di errore e redirect alla LoginPage
			if (!errorMessages.isEmpty()) {
				FlashMessagesManager.addFieldErrors(request, errorMessages);
			}
			
			String loginPath = getServletContext().getContextPath() + "/";
			response.sendRedirect(loginPath);
		}
	}
	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}