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

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

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
        
        // Mappa per messaggi di errore strutturati
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        String successMessage = null;
        
        // Get playlist ID from request
        String playlistIdStr = request.getParameter("playlistId");
        
        if (playlistIdStr == null || playlistIdStr.isEmpty()) {
            errorMessages.put("deletePlaylist_playlistError", "Playlist ID is required");
            hasErrors = true;
        }
        
        int playlistId = -1;
        if (!hasErrors) {
            try {
                playlistId = Integer.parseInt(playlistIdStr);
            } catch (NumberFormatException e) {
                errorMessages.put("deletePlaylist_playlistError", "Invalid playlist ID format");
                hasErrors = true;
            }
        }
        
        // Se non ci sono errori di validazione, procedi con la cancellazione
        if (!hasErrors) {
            PlaylistDAO playlistDAO = new PlaylistDAO(connection);
            try {
                // First verify playlist belongs to the user and get playlist name for success message
                Playlist playlist = playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId());
                if (playlist == null) {
                    errorMessages.put("deletePlaylist_playlistError", "Playlist not found or you don't have permission to delete it");
                    hasErrors = true;
                } else {
                    // Delete the playlist
                    boolean success = playlistDAO.deletePlaylist(playlistId, user.getId());
                    
                    if (!success) {
                        errorMessages.put("deletePlaylist_generalError", "Failed to delete the playlist. Please try again.");
                        hasErrors = true;
                    } else {
                        successMessage = "Playlist '" + playlist.getName() + "' deleted successfully!";
                    }
                }
                
            } catch (SQLException e) {
                errorMessages.put("deletePlaylist_generalError", "Database error during playlist deletion: " + e.getMessage());
                hasErrors = true;
                e.printStackTrace();
            }
        }
        
        // PATTERN POST-REDIRECT-GET
        
        // Aggiungi messaggio di successo
        if (successMessage != null) {
            FlashMessagesManager.addSuccessMessage(request, successMessage);
        }
        
        // Aggiungi errori strutturati
        if (!errorMessages.isEmpty()) {
            FlashMessagesManager.addFieldErrors(request, errorMessages);
        }
        
        // SEMPRE redirect alla home page (pattern PRG)
        response.sendRedirect(getServletContext().getContextPath() + "/Home");
    }
    
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}