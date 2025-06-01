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
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/DeletePlaylist")
public class DeletePlaylist extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    
    public DeletePlaylist() {
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
        
        // Get playlist ID from request
        String playlistIdStr = request.getParameter("playlistId");
        
        if (playlistIdStr == null || playlistIdStr.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist ID is required");
            return;
        }
        
        int playlistId;
        try {
            playlistId = Integer.parseInt(playlistIdStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID format");
            return;
        }
        
        // Delete the playlist
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        try {
            // First verify playlist belongs to the user
            if (playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId()) == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have permission to delete this playlist");
                return;
            }
            
            boolean success = playlistDAO.deletePlaylist(playlistId, user.getId());
            
            if (!success) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete the playlist");
                return;
            }
            
            // Redirect to home page
            response.sendRedirect(getServletContext().getContextPath() + "/Home");
            
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during playlist deletion");
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