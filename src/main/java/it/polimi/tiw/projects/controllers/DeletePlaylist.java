package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;

@WebServlet("/DeletePlaylist")
public class DeletePlaylist extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public DeletePlaylist() {
        super();
    }
    
    @Override
    protected boolean needsTemplateEngine() {
        return false;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione utilizzando ServletBase
        User user = checkLogin(request, response);
        if (user == null) {
            return; // checkLogin ha già fatto il redirect
        }
        
        // Mappa per messaggi di errore strutturati
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        String successMessage = null;
        
        // Parsing parametri
        int playlistId = getIntParam(request, "playlistId");
        
        // Validazione 
        if (playlistId == -1) {
            errorMessages.put("deletePlaylist_playlistError", "Playlist ID is required");
            hasErrors = true;
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
        String homePath = getServletContext().getContextPath() + "/Home";
        doRedirect(request, response, homePath, successMessage, errorMessages, null);
    }
}