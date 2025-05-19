package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;

import java.sql.Date;

public class PlaylistDAO {
	private Connection connection;
	
	public PlaylistDAO (Connection connection) {
		this.connection = connection;
	}
	
	public boolean createPlaylist(String name, String[] songNames, int userID) throws SQLException {
        // Store the current auto-commit state to restore it later
        boolean originalAutoCommit = connection.getAutoCommit();
        
        try {
            // Start transaction
            connection.setAutoCommit(false);
            
            // 1. Insert the playlist record
            int playlistID = insertPlaylist(name, userID);
            
            // 2. Get song IDs from song names
            SongDAO songDAO = new SongDAO(connection);
            
            // 3. Add songs to playlist
            for (String songName : songNames) {
                int songID = songDAO.getSongIDByNameAndUser(songName, userID);
                
                // Skip invalid songs (this shouldn't happen if validation was done properly)
                if (songID <= 0) {
                    continue;
                }
                
                // Add song to playlist
                addSongToPlaylist(playlistID, songID);
            }
            
            // Commit transaction
            connection.commit();
            return true;
        } catch (SQLException e) {
            // Something went wrong, rollback transaction
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                // Log rollback error
                rollbackEx.printStackTrace();
            }
            throw e; // Re-throw the original exception
        } finally {
            // Restore original auto-commit state
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                // Log error
                e.printStackTrace();
            }
        }
    }
	
	private int insertPlaylist(String name, int userID) throws SQLException {
        String query = "INSERT INTO Playlist (name, creationDate, userID) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstatement.setString(1, name);
            // Use current date for creation date
            pstatement.setDate(2, new Date(new java.util.Date().getTime()));
            pstatement.setInt(3, userID);
            
            int affectedRows = pstatement.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating playlist failed, no rows affected.");
            }
            
            // Get the generated playlist ID
            try (ResultSet generatedKeys = pstatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating playlist failed, no ID obtained.");
                }
            }
        }
    }
	
	private void addSongToPlaylist(int playlistID, int songID) throws SQLException {
        String query = "INSERT INTO PlaylistSong (playlistID, songID) VALUES (?, ?)";
        
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, playlistID);
            pstatement.setInt(2, songID);
            pstatement.executeUpdate();
        }
    }

	public boolean existsPlaylistByNameAndUser(String name, int userID) throws SQLException{
		String query = "SELECT name FROM Song WHERE name=? and userID=?";
		
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, name);
			pstatement.setInt(2, userID);
			try (ResultSet result = pstatement.executeQuery();) {
				if(!result.isBeforeFirst())
					return false;
				else {
					return true;
				}
			}
		}
	}

	public List<Playlist> getAllPlaylistsByUserId(int userID) throws SQLException {
		String query = "SELECT * FROM Playlist WHERE userID = ?";
	    List<Playlist> playlists = new ArrayList<>();
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, userID);
	        try (ResultSet result = pstatement.executeQuery()) {
	            while (result.next()) {
	                Playlist playlist = new Playlist();
	                playlist.setID(result.getInt("ID"));
	                playlist.setUserID(result.getInt("userID"));
	                playlist.setName(result.getString("name"));
	                playlist.setCreationDate(result.getDate("creationDate"));
	                
	                
	                playlists.add(playlist);
	            }
	        }
	    }
	    
	    return playlists;
	}
}
