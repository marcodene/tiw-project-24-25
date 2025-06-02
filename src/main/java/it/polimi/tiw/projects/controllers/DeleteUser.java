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

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

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
        
        // Optional verification step - require password confirmation
        String password = request.getParameter("password");
        if (password == null || password.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password confirmation is required");
            return;
        }
        
        // Verify password
        UserDAO userDAO = new UserDAO(connection);
        try {
            User verifiedUser = userDAO.checkCredentials(user.getUsername(), password);
            if (verifiedUser == null) {
                // Password doesn't match
                request.setAttribute("deleteAccountError", "Incorrect password. Account deletion failed.");
                request.getRequestDispatcher("/Account").forward(request, response);
                return;
            }
            
            // Delete the user
            boolean success = userDAO.deleteUser(user.getId());
            
            if (!success) {
                request.setAttribute("deleteAccountError", "Failed to delete account. Please try again.");
                request.getRequestDispatcher("/Account").forward(request, response);
                return;
            }
            
            // Invalidate session and redirect to login page
            session.invalidate();
            response.sendRedirect(getServletContext().getContextPath() + "/");
            
        } catch (SQLException e) {
            request.setAttribute("deleteAccountError", "Database error: " + e.getMessage());
            request.getRequestDispatcher("/Account").forward(request, response);
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