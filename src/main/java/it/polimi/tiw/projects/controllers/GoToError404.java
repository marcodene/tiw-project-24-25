package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.thymeleaf.context.WebContext;

@WebServlet("/Error404")
public class GoToError404 extends ServletBase {
    private static final long serialVersionUID = 1L;

    public GoToError404() {
        super();
    }
    
    @Override
    protected boolean needsDatabase() {
        return false;
    }
    
    @Override
    protected boolean needsAuth() {
        return false;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Creazione WebContext
        WebContext ctx = createContext(request, response);
        
        // Rendering
        String templatePath = "/WEB-INF/404.html";
        templateEngine.process(templatePath, ctx, response.getWriter());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}