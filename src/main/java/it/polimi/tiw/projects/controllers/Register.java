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

import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;


@WebServlet("/Register")
public class Register extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") != null) {
            String homePagePath = getServletContext().getContextPath() + "/Home";
            response.sendRedirect(homePagePath);
            return;
        }

        Map<String, String> errorMessages = new HashMap<>();
        Map<String, String> formValues = new HashMap<>();
        String successMessage = null;
        boolean hasErrors = false;

        String username = StringEscapeUtils.escapeJava(request.getParameter("username"));
        String name = StringEscapeUtils.escapeJava(request.getParameter("name"));
        String surname = StringEscapeUtils.escapeJava(request.getParameter("surname"));
        String password = StringEscapeUtils.escapeJava(request.getParameter("password"));
        String confirmPassword = StringEscapeUtils.escapeJava(request.getParameter("confirmPassword"));

        UserDAO userDAO = new UserDAO(connection);

        // Aggiungi solo valori NON sensibili ai formValues (MAI le password!)
        if (username != null && !username.trim().isEmpty()) {
            formValues.put("username", username);
        }
        if (name != null && !name.trim().isEmpty()) {
            formValues.put("name", name);
        }
        if (surname != null && !surname.trim().isEmpty()) {
            formValues.put("surname", surname);
        }
        // IMPORTANTE: NON aggiungere mai password o confirmPassword per sicurezza!

        // Validazioni con prefissi sistematici "register_"
        if (username == null || username.trim().isEmpty()) {
            errorMessages.put("register_usernameError", "Username must not be empty.");
            hasErrors = true;
        }

        if (name == null || name.trim().isEmpty()) {
            errorMessages.put("register_nameError", "Name must not be empty.");
            hasErrors = true;
        }

        if (surname == null || surname.trim().isEmpty()) {
            errorMessages.put("register_surnameError", "Surname must not be empty.");
            hasErrors = true;
        }

        if (password == null || password.trim().isEmpty()) {
            errorMessages.put("register_passwordError", "Password must not be empty.");
            hasErrors = true;
        } else if (password.length() < 4) {
            errorMessages.put("register_passwordError", "Password must be at least 4 characters long.");
            hasErrors = true;
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            errorMessages.put("register_confirmPasswordError", "Password confirmation must not be empty.");
            hasErrors = true;
        } else if (!confirmPassword.equals(password)) {
            errorMessages.put("register_confirmPasswordError", "Passwords do not match.");
            hasErrors = true;
        }

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
                    // Non manteniamo i valori del form dopo registrazione riuscita
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

        // PATTERN POST-REDIRECT-GET: Redirect condizionale in base al risultato
        
        if (successMessage != null) {
            // SUCCESSO: Redirect alla login page con messaggio di successo
            FlashMessagesManager.addSuccessMessage(request, successMessage);
            String loginPath = getServletContext().getContextPath() + "/GoToLoginPage";
            response.sendRedirect(loginPath);
        } else {
            // ERRORI: Redirect alla register page con errori e valori form
            if (!errorMessages.isEmpty()) {
                FlashMessagesManager.addFieldErrors(request, errorMessages);
            }
            
            if (hasErrors && !formValues.isEmpty()) {
                Map<String, String> registerValues = new HashMap<>();
                for (Map.Entry<String, String> value : formValues.entrySet()) {
                    registerValues.put("register_" + value.getKey(), value.getValue());
                }
                FlashMessagesManager.addFormValues(request, registerValues);
            }
            
            String registerPath = getServletContext().getContextPath() + "/GoToRegisterPage";
            response.sendRedirect(registerPath);
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