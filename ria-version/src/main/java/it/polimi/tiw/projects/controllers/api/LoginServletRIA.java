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

@WebServlet("/api/login")
public class LoginServletRIA extends HttpServlet {
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
        String password = request.getParameter("password");
        

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        UserDAO userDAO = new UserDAO(connection);
        User user;
        try {
            user = userDAO.checkCredentials(username, password);
        } catch (SQLException e) {
            e.printStackTrace(); // Log error
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during login.");
            return;
        }

        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
        } else {
            HttpSession session = request.getSession(true); // Create session if one does not exist
            session.setAttribute("user", user);
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", "success");
            responseMap.put("data", user.toJSON()); 
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(responseMap));
        }
    }

    private void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        
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
