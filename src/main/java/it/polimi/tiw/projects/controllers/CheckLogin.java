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

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;
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

	protected void doPost (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String username = null;
		String password = null;
		
		try {
			username = StringEscapeUtils.escapeJava(request.getParameter("username"));
			password = StringEscapeUtils.escapeJava(request.getParameter("password"));
			
			if(username == null || password==null || username.isEmpty() || password.isEmpty()) {
				throw new Exception("Missing or empty credential value");
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty credential value");
			return;
		}
		
		// query db to authenticate for user
		UserDAO userDao = new UserDAO(connection);
		User user = null;
		try {
			user = userDao.checkCredentials(username, password);
		}catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not Possible to check credentials");
			return;
		}
		
		// If the user exists, add info to the session and go to home page, otherwise
		// show login page with error message
		String path;
		if(user == null) {
			JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
			WebContext context = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
			
			context.setVariable("errorMsg", "Incorrect username or password");
			path = "/WEB-INF/index.html";
			templateEngine.process(path, context, response.getWriter());
		} else {
			request.getSession().setAttribute("user",  user);
			path = getServletContext().getContextPath() + "/Home";
			response.sendRedirect(path);
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