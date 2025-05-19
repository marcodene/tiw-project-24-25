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

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/GoToPlaylistPage")
public class GoToPlaylistPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
    private Connection connection = null;
    private static final int SONGS_PER_PAGE = 5;
       
    public GoToPlaylistPage() {
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

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Check if user is logged in
        String loginPath = getServletContext().getContextPath();
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") == null) {
            response.sendRedirect(loginPath);
            return;
        }
        
    	User user = (User) session.getAttribute("user");
        
        // Get playlist ID from request
        String playlistIdStr = request.getParameter("playlistId");
        if (playlistIdStr == null || playlistIdStr.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist ID is required");
            return;
        }
        
        int playlistId;
        try {
            playlistId = Integer.parseInt(playlistIdStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID");
            return;
        }
        
        // Get current page (for pagination)
        String pageStr = request.getParameter("page");
        int currentPage = 0;
        if (pageStr != null && !pageStr.isEmpty()) {
            try {
                currentPage = Integer.parseInt(pageStr);
                if (currentPage < 0) currentPage = 0;
            } catch (NumberFormatException e) {
                currentPage = 0;
            }
        }
        
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
            
            // Calculate pagination
            int totalSongs = allSongs.size();
            int totalPages = (int) Math.ceil((double) totalSongs / SONGS_PER_PAGE);
            
            // Get songs for current page
            int startIndex = currentPage * SONGS_PER_PAGE;
            int endIndex = Math.min(startIndex + SONGS_PER_PAGE, totalSongs);
            List<Song> currentPageSongs = allSongs.subList(startIndex, endIndex);
            
            // Calculate navigation flags
            boolean hasPrevious = currentPage > 0;
            boolean hasNext = currentPage < totalPages - 1;
            
            // Get songs not in playlist for adding
            List<Song> availableSongs = playlistDAO.getSongsNotInPlaylist(playlistId, user.getId());
            
            // Set up template context
            String path = "/WEB-INF/PlaylistPage.html";
            ServletContext servletContext = getServletContext();
            JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
            WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
            
            ctx.setVariable("playlist", playlist);
            ctx.setVariable("currentPageSongs", currentPageSongs);
            ctx.setVariable("availableSongs", availableSongs);
            ctx.setVariable("currentPage", currentPage); 
            ctx.setVariable("totalPages", totalPages);
            ctx.setVariable("hasPrevious", hasPrevious);
            ctx.setVariable("hasNext", hasNext);
            //ctx.setVariable("isFirstPage", currentPage == 0);
            //ctx.setVariable("isLastPage", currentPage == totalPages - 1);
            
            templateEngine.process(path, ctx, response.getWriter());
        }catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving playlist data");
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
