package it.polimi.tiw.projects.controllers;

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

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/DeleteUser")
public class DeleteUser extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    
    public DeleteUser() {
        super();
    }
    
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") == null) {
            String loginPath = getServletContext().getContextPath() + "/";
            response.sendRedirect(loginPath);
            return;
        }
        
        User user = (User) session.getAttribute("user");
        
        // Mappa per messaggi di errore strutturati (seguendo lo standard PRG)
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        
        // Optional verification step - require password confirmation
        String password = request.getParameter("password");
        if (password == null || password.isEmpty()) {
            errorMessages.put("account_passwordError", "Password confirmation is required");
            hasErrors = true;
        }
        
        // Se non ci sono errori di validazione, procedi con la verifica password e cancellazione
        if (!hasErrors) {
            UserDAO userDAO = new UserDAO(connection);
            try {
                // Verify password
                User verifiedUser = userDAO.checkCredentials(user.getUsername(), password);
                if (verifiedUser == null) {
                    // Password doesn't match
                    errorMessages.put("account_passwordError", "Incorrect password. Account deletion failed.");
                    hasErrors = true;
                } else {
                    // Delete the user
                    boolean success = userDAO.deleteUser(user.getId());
                    
                    if (!success) {
                        errorMessages.put("account_generalError", "Failed to delete account. Please try again.");
                        hasErrors = true;
                    } else {
                        // SUCCESS: Invalidate session and redirect with success parameter
                        session.invalidate();
                        response.sendRedirect(getServletContext().getContextPath() + "/?accountDeleted=true");
                        return;
                    }
                }
            } catch (SQLException e) {
                errorMessages.put("account_generalError", "Database error: " + e.getMessage());
                hasErrors = true;
                e.printStackTrace();
            }
        }
        
        // PATTERN POST-REDIRECT-GET: Se ci sono errori, aggiungi flash messages e redirect
        if (hasErrors && !errorMessages.isEmpty()) {
            FlashMessagesManager.addFieldErrors(request, errorMessages);
        }
        
        // SEMPRE redirect, mai forward (pattern PRG)
        String accountPath = getServletContext().getContextPath() + "/Account";
        response.sendRedirect(accountPath);
    }
    
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}