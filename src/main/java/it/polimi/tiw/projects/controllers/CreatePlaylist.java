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

import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;

@WebServlet("/CreatePlaylist")
public class CreatePlaylist extends ServletBase {
    private static final long serialVersionUID = 1L;
     
    public CreatePlaylist() {
        super();
    }
    
    // Override per specificare che NON ha bisogno del template engine (fa solo redirect)
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
        
        // Crea una mappa per i messaggi di errore
        Map<String, String> errorMessages = new HashMap<>();
        Map<String, String> formValues = new HashMap<>();
        String successMessage = null;
        boolean hasErrors = false;
        
        String playlistName = null;
        int[] selectedSongIDs = null;
        String[] selectedSongIDStrings = request.getParameterValues("selectedSongs");

        if (selectedSongIDStrings != null) {
            selectedSongIDs = new int[selectedSongIDStrings.length];
            try {
                for (int i = 0; i < selectedSongIDStrings.length; i++) {
                    selectedSongIDs[i] = Integer.parseInt(selectedSongIDStrings[i]);
                }
            } catch (NumberFormatException e) {
                errorMessages.put("playlist_songsError", "ID delle canzoni non validi");
                hasErrors = true;
            }
        }
        
        SongDAO songDAO = new SongDAO(connection);
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        
        try {
            playlistName = StringEscapeUtils.escapeJava(request.getParameter("playlistName"));
            
            // Validazione utilizzando ServletBase
            if (isEmpty(playlistName)) {
                errorMessages.put("playlist_nameError", "Il nome della playlist è obbligatorio");
                hasErrors = true;
            } else {
                // Aggiungi ai form values solo se valido
                formValues.put("name", playlistName);
            }
            
            if (selectedSongIDs == null || selectedSongIDs.length == 0) {
                errorMessages.put("playlist_songsError", "Devi selezionare almeno una canzone");
                hasErrors = true;
            }
            
            if (containsDuplicates(selectedSongIDs)) {
                errorMessages.put("playlist_songsError", "La playlist non può contenere canzoni duplicate");
                hasErrors = true;
            }
            
            try {
                if (!hasErrors && !songDAO.existAllSongsByIDsAndUser(selectedSongIDs, user.getId())) {
                    errorMessages.put("playlist_songsError", "Tutte le canzoni devono essere già state caricate");
                    hasErrors = true;
                }
            
                // Verifica se esiste già una playlist con questo nome per l'utente
                if (!hasErrors && playlistDAO.existsPlaylistByNameAndUser(playlistName, user.getId())) {
                    errorMessages.put("playlist_nameError", "Esiste già una playlist con questo nome");
                    hasErrors = true;
                }
                
                // Se non ci sono errori, procedi con la creazione
                if (!hasErrors) {
                    boolean success = playlistDAO.createPlaylist(playlistName, selectedSongIDs, user.getId());
        
                    if (!success) {
                        errorMessages.put("playlist_generalError", "La playlist non è stata creata. Verifica che i valori siano corretti.");
                        hasErrors = true;
                    } else {
                        // Creazione completata con successo
                        successMessage = "Playlist '" + playlistName + "' creata con successo!";
                        // Non manteniamo i valori del form dopo il successo
                        formValues.clear();
                    }
                }
            } catch (SQLException e) {
                errorMessages.put("playlist_generalError", "Errore del database: " + e.getMessage());
                hasErrors = true;
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            errorMessages.put("playlist_generalError", "Errore durante l'elaborazione dei dati: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        }
        
        // PATTERN POST-REDIRECT-GET utilizzando ServletBase
        String homePath = getServletContext().getContextPath() + "/Home";
        
        // Passa i form values solo se ci sono errori (per ri-popolare i campi)
        Map<String, String> finalFormValues = hasErrors ? formValues : null;
        doRedirect(request, response, homePath, successMessage, errorMessages, finalFormValues);
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