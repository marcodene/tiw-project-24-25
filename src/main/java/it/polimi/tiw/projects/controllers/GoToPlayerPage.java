package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.context.WebContext;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.SongDAO;

@WebServlet("/GoToPlayerPage")
public class GoToPlayerPage extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public GoToPlayerPage() {
        super();
    }    

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione 
        User user = checkLogin(request, response);
        if (user == null) {
            return;
        }
        
        // Mappa per messaggi di errore 
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        
        // Get the playlist ID for back navigation
        String playlistId = request.getParameter("playlistId");
        
        // Parsing parametri
        int songID = getIntParam(request, "songID");
        
        // Validazione 
        if (songID == -1) {
            errorMessages.put("generalError", "Song ID is required to play a song");
            hasErrors = true;
        }
        
        Song song = null;
        
        // Se non ci sono errori di validazione, carica la canzone
        if (!hasErrors) {
            SongDAO songDAO = new SongDAO(connection);
            try {
                // Get song details and verify user ownership
                song = songDAO.getSongByIDAndUser(songID, user.getId());
                
                if (song == null) {
                    errorMessages.put("generalError", "Song not found or you don't have access to it");
                    hasErrors = true;
                }
                
            } catch (SQLException e) {
                errorMessages.put("generalError", "Database error while loading song: " + e.getMessage());
                hasErrors = true;
                e.printStackTrace();
            }
        }
        
        // PATTERN POST-REDIRECT-GET
        if (hasErrors) {
        	
        	String redirectPath = determineRedirectPath(playlistId);
            doRedirect(request, response, redirectPath, null, errorMessages, null);
            return;
        }

        try {
            // Creazione WebContext
            WebContext ctx = createContext(request, response);
            
            ctx.setVariable("song", song);
            if (!isEmpty(playlistId)) {
                ctx.setVariable("playlistId", playlistId);
            }
            
            // Rendering
            String templatePath = "/WEB-INF/PlayerPage.html";
            templateEngine.process(templatePath, ctx, response.getWriter());
            
        } catch (Exception e) {
            // Se c'è un errore gestisco con flash message
            Map<String, String> renderErrorMessages = new HashMap<>();
            renderErrorMessages.put("generalError", "Error displaying the player page: " + e.getMessage());
            
            // Redirect
            String redirectPath = determineRedirectPath(playlistId);
            doRedirect(request, response, redirectPath, null, renderErrorMessages, null);
            e.printStackTrace();
        }
    }
    
    /**
     * Determina il percorso di redirect intelligente basato sulla presenza del playlistId
     * Estrae la logica di redirect per evitare duplicazione
     */
    private String determineRedirectPath(String playlistId) {
        if (!isEmpty(playlistId)) {
            // Se abbiamo playlistId, torna alla playlist
            return getServletContext().getContextPath() + "/GoToPlaylistPage?playlistId=" + playlistId;
        } else {
            // Altrimenti, torna alla home
            return getServletContext().getContextPath() + "/Home";
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}