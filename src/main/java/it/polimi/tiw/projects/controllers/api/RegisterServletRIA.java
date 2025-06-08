package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/api/register")
public class RegisterServletRIA extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson = new Gson();

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String name = request.getParameter("name");
        String surname = request.getParameter("surname");
        String password = request.getParameter("password");
        // Assuming client side sends confirmPassword, but server only needs one password for user creation
        // String confirmPassword = request.getParameter("confirmPassword"); 

        Map<String, String> errors = new HashMap<>();
        if (username == null || username.trim().isEmpty()) {
            errors.put("username", "Username is required.");
        }
        if (name == null || name.trim().isEmpty()) {
            errors.put("name", "Name is required.");
        }
        if (surname == null || surname.trim().isEmpty()) {
            errors.put("surname", "Surname is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            errors.put("password", "Password is required.");
        }

        if (!errors.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Validation failed.", errors);
            return;
        }

        UserDAO userDAO = new UserDAO(connection);
        User newUser = null;
        try {
            // Check if username already exists
            if (userDAO.isUsernameTaken(username)) {
                errors.put("username", "Username is already taken.");
                sendError(response, HttpServletResponse.SC_CONFLICT, "Registration failed.", errors);
                return;
            }

            // Attempt to create user
            newUser = userDAO.createUser(username, password, name, surname);

            if (newUser == null) {
                // This case might indicate an issue with createUser logic if no exception was thrown
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User creation failed for an unknown reason.", null);
            } else {
                // Registration successful, create session and log the user in
                HttpSession session = request.getSession(true);
                session.setAttribute("user", newUser);

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("status", "success");
                responseMap.put("data", newUser.toJSON());
                response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created for new resource
                response.getWriter().write(gson.toJson(responseMap));
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Log error
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during registration: " + e.getMessage(), null);
        }
    }

    private void sendError(HttpServletResponse response, int statusCode, String message, Map<String, String> errors) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            errorResponse.put("errors", errors);
        }
        
        response.setStatus(statusCode);
        response.getWriter().write(gson.toJson(errorResponse));
    }

    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
