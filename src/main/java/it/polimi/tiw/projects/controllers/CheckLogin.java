package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.beans.User;

@WebServlet("/CheckLogin") 
public class CheckLogin extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public CheckLogin() {
        super();
    }
    
    // Override per specificare che questa servlet NON ha bisogno di autenticazione
    @Override
    protected boolean needsAuth() {
        return false;
    }
    
    // Override per specificare che NON ha bisogno del template engine (fa solo redirect)
    @Override
    protected boolean needsTemplateEngine() {
        return false;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        
        String username = null;
        String password = null;
        
        try {
            username = request.getParameter("username");
            password = request.getParameter("password");
            
            // Validazione parametri utilizzando ServletBase
            if (isEmpty(username) || isEmpty(password)) {
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
        
        // PATTERN POST-REDIRECT-GET utilizzando ServletBase
        
        if (!hasErrors && user != null) {
            // LOGIN RIUSCITO: Crea sessione e redirect alla Home
            request.getSession().setAttribute("user", user);
            String homePath = getServletContext().getContextPath() + "/Home";
            response.sendRedirect(homePath);
        } else {
            // LOGIN FALLITO: Redirect alla LoginPage con errori
            String loginPath = getServletContext().getContextPath() + "/";
            doRedirect(request, response, loginPath, null, errorMessages, null);
        }
    }
}