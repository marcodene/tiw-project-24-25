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

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

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
        
        // Mappa per messaggi di errore strutturati (pattern PRG standard)
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        String successMessage = null;
        
        // Get song ID and optional playlist ID from request
        String songIDStr = request.getParameter("songID");
        String playlistIdStr = request.getParameter("playlistId"); // Per il redirect intelligente
        
        if (songIDStr == null || songIDStr.isEmpty()) {
            errorMessages.put("generalError", "Song ID is required");
            hasErrors = true;
        }
        
        int songID = -1;
        if (!hasErrors) {
            try {
                songID = Integer.parseInt(songIDStr);
            } catch (NumberFormatException e) {
                errorMessages.put("generalError", "Invalid song ID format");
                hasErrors = true;
            }
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
        
        // PATTERN POST-REDIRECT-GET: Gestione flash messages
        
        // Aggiungi messaggio di successo
        if (successMessage != null) {
            FlashMessagesManager.addSuccessMessage(request, successMessage);
        }
        
        // Aggiungi errori strutturati
        if (!errorMessages.isEmpty()) {
            FlashMessagesManager.addFieldErrors(request, errorMessages);
        }
        
        // REDIRECT INTELLIGENTE basato su dove è stata chiamata la cancellazione
        String redirectPath;
        if (playlistIdStr != null && !playlistIdStr.isEmpty()) {
            // Se proveniamo da una playlist, torniamo a quella playlist
            redirectPath = getServletContext().getContextPath() + "/GoToPlaylistPage?playlistId=" + playlistIdStr;
        } else {
            // Altrimenti, torniamo alla home page
            redirectPath = getServletContext().getContextPath() + "/Home";
        }
        
        // SEMPRE redirect (pattern PRG)
        response.sendRedirect(redirectPath);
    }
    
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}