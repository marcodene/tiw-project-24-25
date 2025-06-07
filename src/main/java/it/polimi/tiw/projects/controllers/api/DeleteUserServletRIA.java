package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

@WebServlet("/api/deleteUser")
public class DeleteUserServletRIA extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private Gson gson = new Gson();
    
    public DeleteUserServletRIA() {
        super();
    }
    
    public void init() throws ServletException {
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        Map<String, Object> responseMap = new HashMap<>();
        
        if (session == null || session.getAttribute("user") == null) {
            responseMap.put("status", "error");
            responseMap.put("message", "No active session found");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(responseMap));
            return;
        }
        
        User user = (User) session.getAttribute("user");
        
        String password = request.getParameter("password");
        if (password == null || password.isEmpty()) {
            responseMap.put("status", "error");
            responseMap.put("message", "Password confirmation is required");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(responseMap));
            return;
        }
        
        UserDAO userDAO = new UserDAO(connection);
        try {
            User verifiedUser = userDAO.checkCredentials(user.getUsername(), password);
            if (verifiedUser == null) {
                responseMap.put("status", "error");
                responseMap.put("message", "Incorrect password. Account deletion failed.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(responseMap));
                return;
            }
            
            boolean success = userDAO.deleteUser(user.getId());
            
            if (!success) {
                responseMap.put("status", "error");
                responseMap.put("message", "Failed to delete account. Please try again.");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(gson.toJson(responseMap));
                return;
            }
            
            session.invalidate();
            
            responseMap.put("status", "success");
            responseMap.put("message", "Account deleted successfully");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(responseMap));
            
        } catch (SQLException e) {
            responseMap.put("status", "error");
            responseMap.put("message", "Database error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(responseMap));
            e.printStackTrace();
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