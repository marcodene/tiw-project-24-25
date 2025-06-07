package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.thymeleaf.context.WebContext;

import it.polimi.tiw.projects.utils.FlashMessagesManager;
import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;

@WebServlet("/Home")
public class GoToHomePage extends ServletBase {
    private static final long serialVersionUID = 1L;
       
    public GoToHomePage() {
        super();
    }
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione
        User user = checkLogin(request, response);
        if (user == null) {
            return;
        }
        
        // Caricamento dati dal database con gestione errori
        List<Playlist> playlists = null;
        List<String> genres = null;
        List<Song> songs = null;
        
        try {
            // Carica playlist dell'utente
            PlaylistDAO playlistDAO = new PlaylistDAO(connection);
            playlists = playlistDAO.getAllPlaylistsByUserId(user.getId());
            
            // Carica tutti i generi disponibili
            GenreDAO genreDAO = new GenreDAO(connection);
            genres = genreDAO.getAllGenresNames();
            
            // Carica canzoni dell'utente
            SongDAO songDAO = new SongDAO(connection);
            songs = songDAO.getAllSongsByUserId(user.getId());
            
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Error loading page data: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Creazione WebContext 
        WebContext ctx = createContext(request, response);
        
        // Aggiungi sempre i dati principali
        ctx.setVariable("playlists", playlists);
        ctx.setVariable("genres", genres);
        ctx.setVariable("songs", songs);
        
        // PATTERN POST-REDIRECT-GET
        setupFlashMessages(ctx, request);
        
        // Rendering
        String templatePath = "/WEB-INF/Home.html";
        templateEngine.process(templatePath, ctx, response.getWriter());
    }
    
    /**
     * Gestisce la logica complessa dei flash messages con prefissi sistematici
     * Estrae in metodo separato per leggibilità
     */
    private void setupFlashMessages(WebContext ctx, HttpServletRequest request) {
    	
        // === MESSAGGI DI SUCCESSO ===
        List<String> successMessages = FlashMessagesManager.getAndClearSuccessMessages(request);
        if (successMessages != null && !successMessages.isEmpty()) {
            ctx.setVariable("successMessage", successMessages.get(0));
        }
        
        // === FLASH MESSAGES CON PREFISSI ===
        // Recupera e rimuove gli errori strutturati per campo (mantiene UX originale)
        Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
        Map<String, String> allFormValues = FlashMessagesManager.getAndClearFormValues(request);
        
        // Separa errori playlist da errori upload
        Map<String, String> playlistFieldErrors = new HashMap<>();
        Map<String, String> playlistFormValues = new HashMap<>();
        Map<String, String> uploadFieldErrors = new HashMap<>();
        Map<String, String> uploadFormValues = new HashMap<>();
        
        // Filtra errori e valori per tipo
        if (allFieldErrors != null) {
            for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                if (!isEmpty(key)) {
                    if (key.startsWith("upload_")) {
                        // Rimuovi prefisso "upload_"
                        String cleanKey = key.substring(7);
                        uploadFieldErrors.put(cleanKey, value);
                    } else if (key.startsWith("playlist_")) {
                        // Rimuovi prefisso "playlist_" 
                        String cleanKey = key.substring(9);
                        playlistFieldErrors.put(cleanKey, value);
                    } else {
                        // Errori senza prefisso 
                        playlistFieldErrors.put(key, value);
                    }
                }
            }
        }
        
        if (allFormValues != null) {
            for (Map.Entry<String, String> entry : allFormValues.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Usa isEmpty() 
                if (!isEmpty(key)) {
                    if (key.startsWith("upload_")) {
                        // Rimuovi prefisso "upload_" 
                        String cleanKey = key.substring(7);
                        uploadFormValues.put(cleanKey, value);
                    } else if (key.startsWith("playlist_")) {
                        // Rimuovi prefisso "playlist_" 
                        String cleanKey = key.substring(9);
                        playlistFormValues.put(cleanKey, value);
                    } else {
                        // Valori senza prefisso
                        playlistFormValues.put(key, value);
                    }
                }
            }
        }
        
        if (!playlistFieldErrors.isEmpty()) {
            ctx.setVariable("playlistErrorMessages", playlistFieldErrors);
        }
        if (!playlistFormValues.isEmpty()) {
            ctx.setVariable("playlistFormValues", playlistFormValues);
        }
        if (!uploadFieldErrors.isEmpty()) {
            ctx.setVariable("errorMessages", uploadFieldErrors);
        }
        if (!uploadFormValues.isEmpty()) {
            ctx.setVariable("formValues", uploadFormValues);
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}