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
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/DeleteSong")
public class DeleteSong extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    
    public DeleteSong() {
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
        
        // Get song ID and optional playlist ID from request
        String songIDStr = request.getParameter("songID");
        String playlistIdStr = request.getParameter("playlistId");
        
        if (songIDStr == null || songIDStr.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Song ID is required");
            return;
        }
        
        int songID;
        try {
            songID = Integer.parseInt(songIDStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid song ID format");
            return;
        }
        
        // Delete the song
        SongDAO songDAO = new SongDAO(connection);
        try {
            // Verify song belongs to the user before deletion
            if (songDAO.getSongByIDAndUser(songID, user.getId()) == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have permission to delete this song");
                return;
            }
            
            boolean success = songDAO.deleteSong(songID, user.getId());
            
            if (!success) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete the song");
                return;
            }
            
            // Redirect to appropriate page
            if (playlistIdStr != null && !playlistIdStr.isEmpty()) {
                // If we came from a playlist, go back to that playlist
                response.sendRedirect(getServletContext().getContextPath() + "/GoToPlaylistPage?playlistId=" + playlistIdStr);
            } else {
                // Otherwise, go to home page
                response.sendRedirect(getServletContext().getContextPath() + "/Home");
            }
            
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during song deletion");
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