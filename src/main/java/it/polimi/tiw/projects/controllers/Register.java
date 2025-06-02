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

        formValues.put("username", username);
        formValues.put("name", name);
        formValues.put("surname", surname);

        // Validations
        if (username == null || username.trim().isEmpty()) {
            errorMessages.put("usernameError", "Username must not be empty.");
            hasErrors = true;
        }

        if (name == null || name.trim().isEmpty()) {
            errorMessages.put("nameError", "Name must not be empty.");
            hasErrors = true;
        }

        if (surname == null || surname.trim().isEmpty()) {
            errorMessages.put("surnameError", "Surname must not be empty.");
            hasErrors = true;
        }

        if (password == null || password.trim().isEmpty()) {
            errorMessages.put("passwordError", "Password must not be empty.");
            hasErrors = true;
        } else if (password.length() < 4) {
            errorMessages.put("passwordError", "Password must be at least 4 characters long.");
            hasErrors = true;
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            errorMessages.put("confirmPasswordError", "Password confirmation must not be empty.");
            hasErrors = true;
        } else if (!confirmPassword.equals(password)) {
            errorMessages.put("confirmPasswordError", "Passwords do not match.");
            hasErrors = true;
        }

        try {
            if (!hasErrors && userDAO.existsUsername(username)) {
                errorMessages.put("usernameError", "Username is already taken.");
                hasErrors = true;
            }

            if (!hasErrors) {
                int userId = userDAO.insertUser(username, password, name, surname);
                if (userId <= 0) {
                    errorMessages.put("generalError", "Registration failed. Please try again later.");
                    hasErrors = true;
                } else {
                    successMessage = "User registered successfully!";
                    formValues.clear();
                }
            }
        } catch (SQLException e) {
            errorMessages.put("generalError", "Database error: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        } catch (Exception e) {
            errorMessages.put("generalError", "An unexpected error occurred: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        }

        request.setAttribute("errorMessages", errorMessages);
        request.setAttribute("formValues", formValues);
        if (successMessage != null) {
            request.setAttribute("successMessage", successMessage);
        }

        request.getRequestDispatcher("/GoToRegisterPage").forward(request, response);
    }

    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}