package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/CreatePlaylist")
public class CreatePlaylist extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
     
    public CreatePlaylist() {
        super();
    }
    
    public void init() throws ServletException {
    	connection = ConnectionHandler.getConnection(getServletContext());
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// if the user is not logged in (not present in session) redirect to the login
		HttpSession session = request.getSession();
		if(session.isNew() || session.getAttribute("user")==null) {
			String loginPagePath = getServletContext().getContextPath() + "/";
			response.sendRedirect(loginPagePath);
			return;
		}
		
		boolean isBadRequest = false;
		String playlistName = null;
		String[] selectedSongNames = null;
		
		User user = (User) session.getAttribute("user");
		SongDAO songDAO = new SongDAO(connection);
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		
		try {
			playlistName = StringEscapeUtils.escapeJava(request.getParameter("playlistName"));
			selectedSongNames = request.getParameterValues("selectedSongs");
			if (playlistName == null || playlistName.isEmpty() || selectedSongNames == null || selectedSongNames.length == 0) {
	            isBadRequest = true;
	        }
			isBadRequest = playlistName == null || playlistName.isEmpty()
					|| selectedSongNames == null || selectedSongNames.length == 0;
			
			if(containsDuplicates(selectedSongNames)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist cannot contain duplicated songs");
				return;
			}
			
			if(!songDAO.existAllSongsByNamesAndUser(selectedSongNames, user.getId())) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "All songs must already been uploaded");
				return;
			}
			
			if(playlistDAO.existsPlaylistByNameAndUser(playlistName, user.getId())) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A playlist with this name already exists");
				return;
			}
			
		}catch (Exception e) {
			isBadRequest = true;
			e.printStackTrace();
		}
		
		try {
			boolean success = playlistDAO.createPlaylist(playlistName, selectedSongNames, user.getId());
			
			if (!success) {
        	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
        	    		"The playlist wasn't created. Please check the values are correct.");
        	    return;
        	}
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to create playlist");
			e.printStackTrace();
			return;
		}
		
		
		if (isBadRequest) {
	        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect or missing param values");
	        return;
	    }
		
		
		String homePagePath = getServletContext().getContextPath() + "/Home";
	    response.sendRedirect(homePagePath);
	    return;
		
	}
	
	private boolean containsDuplicates(String[] songNames) {
	    // If array is null or has less than 2 elements, it can't have duplicates
	    if (songNames == null || songNames.length < 2) {
	        return false;
	    }
	    
	    // Use a HashSet to track unique song names
	    // HashSet garantisce unicità degli elementi
	    Set<String> uniqueSongs = new HashSet<>();
	    
	    // Iterate through all song names
	    for (String songName : songNames) {
	        // If this song is already in the set, it's a duplicate
	        if (!uniqueSongs.add(songName)) {
	            return true; // Duplicato trovato
	        }
	    }
	    
	    // No duplicates found
	    return false;
	}

}
