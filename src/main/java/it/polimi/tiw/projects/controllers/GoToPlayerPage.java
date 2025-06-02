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

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

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
		JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);     WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
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
    	
//    	// Get song name from request
//        String songName = request.getParameter("songName");
//        if (songName == null || songName.isEmpty()) {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Song name is required");
//            return;
//        }
        
        // Get the playlist ID for back navigation
        String playlistId = request.getParameter("playlistId");
        
        SongDAO songDAO = new SongDAO(connection);
        Song song = null;
        
        try {
        	// Get song details
        	String songIDStr = request.getParameter("songID");
        	int songID = Integer.parseInt(songIDStr);
        	song = songDAO.getSongByIDAndUser(songID, user.getId());
            
            if (song == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Song not found or access denied");
                return;
            }
            
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
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving song data");
            e.printStackTrace();
        } catch (NumberFormatException e) {
        	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Song ID missing or not valid");
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
