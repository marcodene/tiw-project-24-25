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
import java.util.List;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;
import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;

@WebServlet("/Home")
public class GoToHomePage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;
       
    public GoToHomePage() {
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
    
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	// If the user is not logged in (not present in session) redirect to the login
    	String loginpath = getServletContext().getContextPath(); // path to the loginPage
    	HttpSession session = request.getSession();
    	if(session.isNew() || session.getAttribute("user")==null) {
    		response.sendRedirect(loginpath);
    		return;
    	}
    	
    	User user = (User) session.getAttribute("user");
    	
    	PlaylistDAO playlistDAO = new PlaylistDAO(connection);
    	List<Playlist> playlists = null;
    	try {
			playlists = playlistDAO.getAllPlaylistsByUserId(user.getId());
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover playlists");
			return;
		}
    	GenreDAO genreDAO = new GenreDAO(connection);
		List<String> genres = null;
    	try {
    		genres = genreDAO.getAllGenresNames();
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover genres");
			return;
		}
    	
    	SongDAO songDAO = new SongDAO(connection);
		List<Song> songs = null;
    	try {
    		songs = songDAO.getAllSongsByUserId(user.getId());
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover songs");
			return;
		}
    	
    	String path = "/WEB-INF/Home.html";
    	ServletContext servletContext = getServletContext();
    	JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
    	WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
    	
    	// Aggiungi sempre i dati principali
    	ctx.setVariable("playlists", playlists);
    	ctx.setVariable("genres", genres);
    	ctx.setVariable("songs", songs);
    	
    	// PATTERN POST-REDIRECT-GET: Leggi i flash messages dalla sessione
    	
    	// === MESSAGGI DI SUCCESSO ===
    	List<String> successMessages = FlashMessagesManager.getAndClearSuccessMessages(request);
    	if (!successMessages.isEmpty()) {
    	    // Per compatibilità con template esistente, usa il primo messaggio
    	    ctx.setVariable("successMessage", successMessages.get(0));
    	}
    	
    	// === PLAYLIST FLASH MESSAGES ===
    	// Recupera e rimuove gli errori strutturati per campo (mantiene UX originale)
    	Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
    	Map<String, String> allFormValues = FlashMessagesManager.getAndClearFormValues(request);
    	
    	// Separa errori playlist da errori upload
    	Map<String, String> playlistFieldErrors = new HashMap<>();
    	Map<String, String> playlistFormValues = new HashMap<>();
    	Map<String, String> uploadFieldErrors = new HashMap<>();
    	Map<String, String> uploadFormValues = new HashMap<>();
    	
    	// Filtra errori e valori per tipo usando prefissi sistematici
    	for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
    	    if (entry.getKey().startsWith("upload_")) {
    	        // Rimuovi prefisso "upload_" per compatibilità template
    	        String cleanKey = entry.getKey().substring(7); // rimuove "upload_"
    	        uploadFieldErrors.put(cleanKey, entry.getValue());
    	    } else if (entry.getKey().startsWith("playlist_")) {
    	        // Rimuovi prefisso "playlist_" per compatibilità template
    	        String cleanKey = entry.getKey().substring(9); // rimuove "playlist_"
    	        playlistFieldErrors.put(cleanKey, entry.getValue());
    	    } else {
    	        // Errori senza prefisso (legacy) - tratta come playlist per retrocompatibilità
    	        playlistFieldErrors.put(entry.getKey(), entry.getValue());
    	    }
    	}
    	
    	for (Map.Entry<String, String> entry : allFormValues.entrySet()) {
    	    if (entry.getKey().startsWith("upload_")) {
    	        // Rimuovi prefisso "upload_" per compatibilità template
    	        String cleanKey = entry.getKey().substring(7); // rimuove "upload_"
    	        uploadFormValues.put(cleanKey, entry.getValue());
    	    } else if (entry.getKey().startsWith("playlist_")) {
    	        // Rimuovi prefisso "playlist_" per compatibilità template
    	        String cleanKey = entry.getKey().substring(9); // rimuove "playlist_"
    	        playlistFormValues.put(cleanKey, entry.getValue());
    	    } else {
    	        // Valori senza prefisso (legacy) - tratta come playlist per retrocompatibilità
    	        playlistFormValues.put(entry.getKey(), entry.getValue());
    	    }
    	}
    	
    	// Imposta variabili per playlist
    	if (!playlistFieldErrors.isEmpty()) {
    	    ctx.setVariable("playlistErrorMessages", playlistFieldErrors);
    	}
    	if (!playlistFormValues.isEmpty()) {
    	    ctx.setVariable("playlistFormValues", playlistFormValues);
    	}
    	
    	// Imposta variabili per upload (compatibilità template esistente)
    	if (!uploadFieldErrors.isEmpty()) {
    	    ctx.setVariable("errorMessages", uploadFieldErrors);
    	}
    	if (!uploadFormValues.isEmpty()) {
    	    ctx.setVariable("formValues", uploadFormValues);
    	}
    	
    	templateEngine.process(path, ctx, response.getWriter());
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
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