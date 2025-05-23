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

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/AddSongsToPlaylist")
public class AddSongsToPlaylist extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
    
    public AddSongsToPlaylist() {
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
		
		User user = (User) session.getAttribute("user");
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        SongDAO songDAO = new SongDAO(connection);
        
        boolean isBadRequest = false;
        String playlistIdStr = null;
        String[] selectedSongNames = null;
        int playlistId = -1;
        
        try {
        	playlistIdStr = request.getParameter("playlistId");
            selectedSongNames = request.getParameterValues("selectedSongs");
            
            if (playlistIdStr == null || playlistIdStr.isEmpty()) {
                isBadRequest = true;
            } else {
                playlistId = Integer.parseInt(playlistIdStr);
            }
            
            if (selectedSongNames == null || selectedSongNames.length == 0) {
                // No songs selected, redirect back to playlist page
                String playlistPagePath = getServletContext().getContextPath() + 
                    "/GoToPlaylistPage?playlistId=" + playlistId;
                response.sendRedirect(playlistPagePath);
                return;
            }
            
            // Check for duplicates in selected songs
            if (containsDuplicates(selectedSongNames)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                    "Cannot select the same song multiple times");
                return;
            }
            
            // Verify playlist belongs to user
            if (playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId()) == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                    "Playlist not found or access denied");
                return;
            }
            
            // Verify all selected songs belong to user
            if (!songDAO.existAllSongsByNamesAndUser(selectedSongNames, user.getId())) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                    "All selected songs must belong to you");
                return;
            }
             
        } catch (NumberFormatException e) {
            isBadRequest = true;
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Error verifying playlist and songs");
            return;
        }
        
        if (isBadRequest) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                "Incorrect or missing parameters");
            return;
        }
        
        
        try {
            // Add songs to playlist
            boolean success = playlistDAO.addSongsToPlaylist(playlistId, selectedSongNames, user.getId());
            
            if (!success) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Failed to add songs to playlist");
                return;
            }
            
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Error adding songs to playlist");
            e.printStackTrace();
            return;
        }
        
        // Redirect back to playlist page (first page to see the updated playlist)
        String playlistPagePath = getServletContext().getContextPath() + 
            "/GoToPlaylistPage?playlistId=" + playlistId;
        response.sendRedirect(playlistPagePath);
	}
	
	private boolean containsDuplicates(String[] songNames) {
        if (songNames == null || songNames.length < 2) {
            return false;
        }
        
        Set<String> uniqueSongs = new HashSet<>();
        
        for (String songName : songNames) {
            if (!uniqueSongs.add(songName)) {
                return true;
            }
        }
        
        return false;
    }
    
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
