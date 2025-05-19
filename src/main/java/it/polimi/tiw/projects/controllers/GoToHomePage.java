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
import java.util.List;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.utils.ConnectionHandler;
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
		JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);     WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
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
    	
    	ctx.setVariable("playlists", playlists);
    	ctx.setVariable("genres", genres);
    	ctx.setVariable("songs", songs);
		
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
