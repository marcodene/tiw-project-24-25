package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.SongDAO;

@WebServlet("/DeleteSong")
public class DeleteSong extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public DeleteSong() {
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
        
        // Mappa per messaggi di errore 
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        String successMessage = null;
        
        // Parsing parametri
        int songID = getIntParam(request, "songID");
        String playlistIdStr = request.getParameter("playlistId"); // Per il redirect intelligente
        
        // Validazione 
        if (songID == -1) {
            errorMessages.put("generalError", "Song ID is required");
            hasErrors = true;
        }
        
        // Se non ci sono errori di validazione, procedi con la cancellazione
        if (!hasErrors) {
            SongDAO songDAO = new SongDAO(connection);
            try {
                // Verify song belongs to the user and get song name for success message
                Song song = songDAO.getSongByIDAndUser(songID, user.getId());
                if (song == null) {
                    errorMessages.put("generalError", "Song not found or you don't have permission to delete it");
                    hasErrors = true;
                } else {
                    // Delete the song
                    boolean success = songDAO.deleteSong(songID, user.getId());
                    
                    if (!success) {
                        errorMessages.put("generalError", "Failed to delete the song. Please try again.");
                        hasErrors = true;
                    } else {
                        successMessage = "Song '" + song.getName() + "' by " + song.getArtistName() + " deleted successfully!";
                    }
                }
                
            } catch (SQLException e) {
                errorMessages.put("generalError", "Database error during song deletion: " + e.getMessage());
                hasErrors = true;
                e.printStackTrace();
            }
        }
        
        // REDIRECT INTELLIGENTE basato su dove è stata chiamata la cancellazione
        String redirectPath;
        if (!isEmpty(playlistIdStr)) {
            // Se proveniamo da una playlist, torniamo a quella playlist
            redirectPath = getServletContext().getContextPath() + "/GoToPlaylistPage?playlistId=" + playlistIdStr;
        } else {
            // Altrimenti, torniamo alla home page
            redirectPath = getServletContext().getContextPath() + "/Home";
        }
        
        // PATTERN POST-REDIRECT-GET 
        doRedirect(request, response, redirectPath, successMessage, errorMessages, null);
    }
}