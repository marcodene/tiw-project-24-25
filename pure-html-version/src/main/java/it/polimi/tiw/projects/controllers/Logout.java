package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/Logout")
public class Logout extends ServletBase {
    private static final long serialVersionUID = 1L;
       
    public Logout() {
        super();
    }

    @Override
    protected boolean needsDatabase() {
        return false;
    }

    @Override
    protected boolean needsTemplateEngine() {
        return false;
    }

    @Override
    protected boolean needsAuth() {
        return false;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        // Se la sessione esiste, invalida e fai logout con messaggio
        if (session != null) {
            // Invalidare la sessione cancella anche i flash messages,
            // quindi usiamo parametro URL come per account deletion
            session.invalidate();
            
            // Redirect alla login page con messaggio di logout 
            doRedirectWithParams(response, getServletContext().getContextPath() + "/", "loggedOut", "true");
        } else {
            // Se non c'è sessione, vai semplicemente al login
            String loginPath = getServletContext().getContextPath() + "/";
            response.sendRedirect(loginPath);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}