package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
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

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/GoToPlayerPage")
public class GoToPlayerPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
    private TemplateEngine templateEngine;
    
    public GoToPlayerPage() {
        super();
    }
    
    public void init() throws ServletException {
    	ServletContext servletContext = getServletContext();
		JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);     
		WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
		connection = ConnectionHandler.getConnection(getServletContext());
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// If the user is not logged in (not present in session) redirect to the login
    	String loginpath = getServletContext().getContextPath(); // path to the loginPage
    	HttpSession session = request.getSession();
    	if(session.isNew() || session.getAttribute("user")==null) {
    		response.sendRedirect(loginpath);
    		return;
    	}
    	
    	User user = (User) session.getAttribute("user");
        
        // Mappa per messaggi di errore strutturati (pattern PRG standard)
        Map<String, String> errorMessages = new HashMap<>();
        boolean hasErrors = false;
        
        // Get the playlist ID for back navigation (optional)
        String playlistId = request.getParameter("playlistId");
        
        // Get song ID from request (required)
        String songIDStr = request.getParameter("songID");
        if (songIDStr == null || songIDStr.isEmpty()) {
            errorMessages.put("generalError", "Song ID is required to play a song");
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
        
        // PATTERN POST-REDIRECT-GET: Se ci sono errori, redirect con flash messages
        if (hasErrors) {
            // Aggiungi errori ai flash messages
            FlashMessagesManager.addFieldErrors(request, errorMessages);
            
            // REDIRECT INTELLIGENTE basato su dove dovremmo tornare
            String redirectPath;
            if (playlistId != null && !playlistId.isEmpty()) {
                // Se abbiamo playlistId, torna alla playlist
                redirectPath = getServletContext().getContextPath() + "/GoToPlaylistPage?playlistId=" + playlistId;
            } else {
                // Altrimenti, torna alla home
                redirectPath = getServletContext().getContextPath() + "/Home";
            }
            
            response.sendRedirect(redirectPath);
            return;
        }
        
        // Se arriviamo qui, tutto è andato bene - mostra la pagina del player
        try {
            // Set up template context
            String path = "/WEB-INF/PlayerPage.html";
            ServletContext servletContext = getServletContext();
            JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
            
            ctx.setVariable("song", song);
            if (playlistId != null && !playlistId.isEmpty()) {
                ctx.setVariable("playlistId", playlistId);
            }
            
            templateEngine.process(path, ctx, response.getWriter());
            
        } catch (Exception e) {
            // Se c'è un errore nel rendering del template, gestiscilo con flash message
            Map<String, String> renderErrorMessages = new HashMap<>();
            renderErrorMessages.put("generalError", "Error displaying the player page: " + e.getMessage());
            FlashMessagesManager.addFieldErrors(request, renderErrorMessages);
            
            // Redirect appropriato
            String redirectPath;
            if (playlistId != null && !playlistId.isEmpty()) {
                redirectPath = getServletContext().getContextPath() + "/GoToPlaylistPage?playlistId=" + playlistId;
            } else {
                redirectPath = getServletContext().getContextPath() + "/Home";
            }
            
            response.sendRedirect(redirectPath);
            e.printStackTrace();
        }
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}