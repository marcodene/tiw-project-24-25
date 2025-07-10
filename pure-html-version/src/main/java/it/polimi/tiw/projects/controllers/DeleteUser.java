package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;

@WebServlet("/DeleteUser")
public class DeleteUser extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public DeleteUser() {
        super();
    }
    
    @Override
    protected boolean needsTemplateEngine() {
        return false;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione utilizzando ServletBase
        User user = checkLogin(request, response);
        if (user == null) {
            return;
        }
        
        // Mappa per messaggi di errore strutturati (seguendo lo standard PRG)
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        
        String password = request.getParameter("password");
        if (isEmpty(password)) {
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
                        // Non possiamo usare flash messages perché invalidiamo la sessione
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            session.invalidate();
                        }
                        
                        // Redirect con parametro URL (necessario per sessione invalidata)
                        doRedirectWithParams(response, 
                            getServletContext().getContextPath() + "/", 
                            "accountDeleted", "true");
                        return;
                    }
                }
            } catch (SQLException e) {
                errorMessages.put("account_generalError", "Database error: " + e.getMessage());
                hasErrors = true;
                e.printStackTrace();
            }
        }
        
        // PATTERN POST-REDIRECT-GET: Se ci sono errori, usa il metodo della ServletBase
        String accountPath = getServletContext().getContextPath() + "/Account";
        doRedirect(request, response, accountPath, null, errorMessages, null);
    }
}