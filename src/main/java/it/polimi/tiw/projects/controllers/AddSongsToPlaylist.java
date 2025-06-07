package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;

@WebServlet("/AddSongsToPlaylist")
public class AddSongsToPlaylist extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public AddSongsToPlaylist() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione utilizzando ServletBase
        User user = checkLogin(request, response);
        if (user == null) {
            return; // checkLogin ha già fatto il redirect
        }
        
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        SongDAO songDAO = new SongDAO(connection);
        
        // Mappa per messaggi di errore strutturati (pattern PRG standard)
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        String successMessage = null;
        
        // Parsing parametri utilizzando ServletBase
        int playlistId = getIntParam(request, "playlistId");
        String[] selectedSongIDStrings = request.getParameterValues("selectedSongs");
        int[] selectedSongIDs = null;
        
        try {
            // Validazione playlistId utilizzando ServletBase
            if (playlistId == -1) {
                errorMessages.put("addSongs_playlistError", "Playlist ID is required");
                hasErrors = true;
            }
            
            // Validazione selezione canzoni
            if (selectedSongIDStrings == null || selectedSongIDStrings.length == 0) {
                // No songs selected, redirect back to playlist page without error
                String playlistPagePath = getServletContext().getContextPath() + 
                    "/GoToPlaylistPage?playlistId=" + playlistId;
                response.sendRedirect(playlistPagePath);
                return;
            }
            
            // Parse degli ID delle canzoni
            if (!hasErrors) {
                selectedSongIDs = new int[selectedSongIDStrings.length];
                try {
                    for (int i = 0; i < selectedSongIDStrings.length; i++) {
                        selectedSongIDs[i] = Integer.parseInt(selectedSongIDStrings[i]);
                    }
                } catch (NumberFormatException e) {
                    errorMessages.put("addSongs_songsError", "Invalid song ID format");
                    hasErrors = true;
                }
            }
            
            // Check for duplicates in selected songs
            if (!hasErrors && containsDuplicates(selectedSongIDs)) {
                errorMessages.put("addSongs_songsError", "Cannot select the same song multiple times");
                hasErrors = true;
            }
            
            // Verifiche di sicurezza
            if (!hasErrors) {
                try {
                    // Verify playlist belongs to user
                    if (playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId()) == null) {
                        errorMessages.put("addSongs_playlistError", "Playlist not found or access denied");
                        hasErrors = true;
                    }
                    
                    // Verify all selected songs belong to user
                    if (!hasErrors && !songDAO.existAllSongsByIDsAndUser(selectedSongIDs, user.getId())) {
                        errorMessages.put("addSongs_songsError", "All selected songs must belong to you");
                        hasErrors = true;
                    }
                } catch (SQLException e) {
                    errorMessages.put("addSongs_generalError", "Error verifying playlist and songs: " + e.getMessage());
                    hasErrors = true;
                    e.printStackTrace();
                }
            }
             
        } catch (Exception e) {
            errorMessages.put("addSongs_generalError", "Error processing request: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        }
        
        // Se non ci sono errori, procedi con l'aggiunta delle canzoni
        if (!hasErrors) {
            try {
            	
                boolean success = playlistDAO.addSongsToPlaylist(playlistId, selectedSongIDs, user.getId());
                
                if (!success) {
                    errorMessages.put("addSongs_generalError", "Failed to add songs to playlist");
                    hasErrors = true;
                } else {
                    successMessage = "Songs added to playlist successfully!";
                }
                
            } catch (SQLException e) {
                errorMessages.put("addSongs_generalError", "Error adding songs to playlist: " + e.getMessage());
                hasErrors = true;
                e.printStackTrace();
            }
        }
        
        // PATTERN POST-REDIRECT-GET utilizzando ServletBase
        String playlistPagePath = getServletContext().getContextPath() + 
            "/GoToPlaylistPage?playlistId=" + playlistId;
        doRedirect(request, response, playlistPagePath, successMessage, errorMessages, null);
    }
    
    private boolean containsDuplicates(int[] songIDs) {
        if (songIDs == null || songIDs.length < 2) {
            return false;
        }
        
        Set<Integer> uniqueSongs = new HashSet<>();
        
        for (int songID : songIDs) {
            if (!uniqueSongs.add(songID)) {
                return true;
            }
        }
        
        return false;
    }
}