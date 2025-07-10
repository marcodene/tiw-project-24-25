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
import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/Register")
public class Register extends ServletBase {
    private static final long serialVersionUID = 1L;

    public Register() {
        super();
    }

    @Override
    protected boolean needsTemplateEngine() {
        return false;
    }

    @Override
    protected boolean needsAuth() {
        return false;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo sessione inverso utilizzando ServletBase
        if (redirectIfLogged(request, response)) {
            return; 
        }

        Map<String, String> errorMessages = new HashMap<>();
        Map<String, String> formValues = new HashMap<>();
        String successMessage = null;
        boolean hasErrors = false;

        // Parsing parametri
        String username = request.getParameter("username");
        String name = request.getParameter("name");
        String surname = request.getParameter("surname");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        UserDAO userDAO = new UserDAO(connection);

        // Aggiungi solo valori NON sensibili ai formValues (MAI le password!)
        if (!isEmpty(username)) {
            formValues.put("username", username);
        }
        if (!isEmpty(name)) {
            formValues.put("name", name);
        }
        if (!isEmpty(surname)) {
            formValues.put("surname", surname);
        }
        
        // Validazioni con prefissi sistematici 
        if (isEmpty(username)) {
            errorMessages.put("register_usernameError", "Username must not be empty.");
            hasErrors = true;
        }

        if (isEmpty(name)) {
            errorMessages.put("register_nameError", "Name must not be empty.");
            hasErrors = true;
        }

        if (isEmpty(surname)) {
            errorMessages.put("register_surnameError", "Surname must not be empty.");
            hasErrors = true;
        }

        if (isEmpty(password)) {
            errorMessages.put("register_passwordError", "Password must not be empty.");
            hasErrors = true;
            
        } else if (password.length() < 4) {
            errorMessages.put("register_passwordError", "Password must be at least 4 characters long.");
            hasErrors = true;
        }

        if (isEmpty(confirmPassword)) {
            errorMessages.put("register_confirmPasswordError", "Password confirmation must not be empty.");
            hasErrors = true;
            
        } else if (!confirmPassword.equals(password)) {
            errorMessages.put("register_confirmPasswordError", "Passwords do not match.");
            hasErrors = true;
        }

        // Validazioni database e inserimento utente
        try {
            if (!hasErrors && userDAO.existsUsername(username)) {
                errorMessages.put("register_usernameError", "Username is already taken.");
                hasErrors = true;
            }

            if (!hasErrors) {
                int userId = userDAO.insertUser(username, password, name, surname);
                if (userId <= 0) {
                    errorMessages.put("register_generalError", "Registration failed. Please try again later.");
                    hasErrors = true;
                } else {
                    successMessage = "User registered successfully!";
                    formValues.clear();
                }
            }
            
        } catch (SQLException e) {
            errorMessages.put("register_generalError", "Database error: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        } catch (Exception e) {
            errorMessages.put("register_generalError", "An unexpected error occurred: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        }

        // PATTERN POST-REDIRECT-GET
        
        if (successMessage != null) {
            // SUCCESSO: Redirect alla login page con messaggio di successo
        	
            String loginPath = getServletContext().getContextPath() + "/";
            doRedirect(request, response, loginPath, successMessage, null, null);
            
        } else {
            // ERRORI: Usa doRedirect della ServletBase per gestire errori e form values
            Map<String, String> finalFormValues = null;
            if (hasErrors && !formValues.isEmpty()) {
                finalFormValues = new HashMap<>();
                for (Map.Entry<String, String> value : formValues.entrySet()) {
                    finalFormValues.put("register_" + value.getKey(), value.getValue());
                }
            }
            
            String registerPath = getServletContext().getContextPath() + "/GoToRegisterPage";
            doRedirect(request, response, registerPath, null, errorMessages, finalFormValues);
        }
    }
}