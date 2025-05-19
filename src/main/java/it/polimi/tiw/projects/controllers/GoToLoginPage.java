package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/")
public class GoToLoginPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
    
    public GoToLoginPage() {
        super();
    }
    
    public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);     WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
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
    	
		templateEngine.process(path, ctx, response.getWriter());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
