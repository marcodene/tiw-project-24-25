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

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/GoToPlaylistPage")
public class GoToPlaylistPage extends ServletBase {
    private static final long serialVersionUID = 1L;
    private static final int SONGS_PER_PAGE = 5;
       
    public GoToPlaylistPage() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione
        User user = checkLogin(request, response);
        if (user == null) {
            return; // checkLogin ha già fatto il redirect
        }
        
        // Parsing e validazione parametri 
        int playlistId = getIntParam(request, "playlistId");
        if (playlistId == -1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist ID is required");
            return;
        }
        
        // Get current page (for pagination) - usa getIntParam con default 0
        int currentPage = getIntParam(request, "page");
        if (currentPage == -1) {
            currentPage = 0; // Default se non specificato o invalido
        }
        if (currentPage < 0) {
            currentPage = 0; // Assicura che non sia negativo
        }
        
        // Caricamento dati dal database con gestione errori
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        
        try {
            // Verify playlist belongs to user
            Playlist playlist = playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId());
            if (playlist == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Playlist not found or access denied");
                return;
            }
            
            // Get all songs in the playlist
            List<Song> allSongs = playlistDAO.getSongsFromPlaylist(playlistId);
            
            // Get songs not in playlist for adding
            List<Song> availableSongs = playlistDAO.getSongsNotInPlaylist(playlistId, user.getId());
            
            // Creazione WebContext
            WebContext ctx = createContext(request, response);
            
            // Calculate pagination and setup template variables
            calculatePagination(allSongs, currentPage, ctx);
            
            // Imposta altri dati principali
            ctx.setVariable("playlist", playlist);
            ctx.setVariable("availableSongs", availableSongs);
            ctx.setVariable("currentPage", currentPage);
            
            // PATTERN POST-REDIRECT-GET
            setupFlashMessages(ctx, request);
            
            // Rendering 
            String templatePath = "/WEB-INF/PlaylistPage.html";
            templateEngine.process(templatePath, ctx, response.getWriter());
            
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Error retrieving playlist data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calcola i dati di paginazione per le canzoni
     * Restituisce i dati come variabili separate per semplicità
     */
    private void calculatePagination(List<Song> allSongs, int currentPage, WebContext ctx) {
        int totalSongs = allSongs.size();
        int totalPages = (int) Math.ceil((double) totalSongs / SONGS_PER_PAGE);
        
        // Get songs for current page
        int startIndex = currentPage * SONGS_PER_PAGE;
        int endIndex = Math.min(startIndex + SONGS_PER_PAGE, totalSongs);
        List<Song> currentPageSongs = allSongs.subList(startIndex, endIndex);
        
        // Calculate navigation flags
        boolean hasPrevious = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;
        
        // Imposta direttamente nel context
        ctx.setVariable("currentPageSongs", currentPageSongs);
        ctx.setVariable("totalPages", totalPages);
        ctx.setVariable("hasPrevious", hasPrevious);
        ctx.setVariable("hasNext", hasNext);
    }
    
    /**
     * Gestisce la logica dei flash messages
     */
    private void setupFlashMessages(WebContext ctx, HttpServletRequest request) {
    	
        // === MESSAGGI DI SUCCESSO ===
        List<String> successMessages = FlashMessagesManager.getAndClearSuccessMessages(request);
        if (successMessages != null && !successMessages.isEmpty()) {
        	ctx.setVariable("successMessage", successMessages.get(0));
        }
        
        // === FLASH MESSAGES PER ERRORI ===
        // Recupera e rimuove gli errori strutturati per campo
        Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);

        Map<String, String> displayErrors = new HashMap<>();
        if (allFieldErrors != null) {
            for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
                String key = entry.getKey();
  
                if (!isEmpty(key)) {
                    if (key.startsWith("addSongs_")) {
                    	String cleanKey = key.substring(9); // rimuove "addSongs_"
                        displayErrors.put(cleanKey, entry.getValue());
                    } else if ("generalError".equals(key)) {                       
                        displayErrors.put("generalError", entry.getValue());
                    }
                }
            }
        }
        
        // Imposta variabili per template
        if (!displayErrors.isEmpty()) {
            ctx.setVariable("errorMessages", displayErrors);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}