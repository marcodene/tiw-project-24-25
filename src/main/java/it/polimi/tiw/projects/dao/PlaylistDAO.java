package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
	
	public boolean createPlaylist(String name, int[] songIDs, int userID) throws SQLException {
	    boolean originalAutoCommit = connection.getAutoCommit();
	    
	    try {
	        connection.setAutoCommit(false);
	        
	        if (existsPlaylistByNameAndUser(name, userID)) {
	            throw new SQLException("Playlist with name '" + name + "' already exists");
	        }
	        
	        // Insert the playlist record
	        int playlistID = insertPlaylist(name, userID);
	        
	        // Verifica che tutti i songID appartengano all'utente
	        SongDAO songDAO = new SongDAO(connection);
	        for (int songID : songIDs) {
	            if (!songDAO.songBelongsToUser(songID, userID)) {
	                throw new SQLException("Song ID " + songID + " does not belong to user");
	            }
	        }
	        
	        // Add songs to playlist usando direttamente gli ID
	        for (int songID : songIDs) {
	            addSongToPlaylist(playlistID, songID);
	        }
	        
	        connection.commit();
	        return true;
	    } catch (SQLException e) { //TODO gestire meglio questa gestione degli errori
	        try {
	            connection.rollback();
	        } catch (SQLException rollbackEx) {
	            rollbackEx.printStackTrace();
	        }
	        throw e;
	    } finally {
	        try {
	            connection.setAutoCommit(originalAutoCommit);
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	private int insertPlaylist(String name, int userID) throws SQLException {
        String query = "INSERT INTO Playlist (name, creationDate, userID) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstatement.setString(1, name);
            // Use current date for creation date
            //pstatement.setDate(2, new Date(new java.util.Date().getTime()));
            pstatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
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
	    String query = "SELECT COUNT(*) FROM Playlist WHERE name=? and userID=?";
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setString(1, name);
	        pstatement.setInt(2, userID);
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                int count = result.getInt(1);
	                return count > 0;
	            }
	        }
	    }
	    return false;
	}
	
	public List<Playlist> getAllPlaylistsByUserId(int userID) throws SQLException {
		String query = "SELECT * FROM Playlist WHERE userID = ? ORDER BY creationDate DESC;";
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
	
	/**
	 * Get playlist by ID and user ID to verify ownership
	 */
	public Playlist getPlaylistByIdAndUser(int playlistId, int userId) throws SQLException {
	    String query = "SELECT * FROM Playlist WHERE ID = ? AND userID = ?";
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, playlistId);
	        pstatement.setInt(2, userId);
	        
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                Playlist playlist = new Playlist();
	                playlist.setID(result.getInt("ID"));
	                playlist.setUserID(result.getInt("userID"));
	                playlist.setName(result.getString("name"));
	                playlist.setCreationDate(result.getDate("creationDate"));
	                return playlist;
	            } else {
	                return null; // Playlist not found or doesn't belong to user
	            }
	        }
	    }
	}
	
	/**
	 * Get songs from a specific playlist ordered by artist and release year
	 */
	public List<Song> getSongsFromPlaylist(int playlistId) throws SQLException {
	    String query = """
	        SELECT s.*, g.name as genreName 
	        FROM Song s 
	        JOIN PlaylistSong ps ON s.ID = ps.songID 
	        JOIN Genre g ON s.genreID = g.ID 
	        WHERE ps.playlistID = ? 
	        ORDER BY s.albumArtist ASC, s.albumReleaseYear ASC
	    """;
	    
	    List<Song> songs = new ArrayList<>();
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, playlistId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            while (result.next()) {
	                Song song = new Song();
	                song.setID(result.getInt("ID"));
	                song.setUserID(result.getInt("userID"));
	                song.setName(result.getString("name"));
	                song.setAlbumName(result.getString("albumName"));
	                song.setArtistName(result.getString("albumArtist"));
	                song.setAlbumReleaseYear(result.getInt("albumReleaseYear"));
	                song.setGenre(result.getString("genreName"));
	                song.setAlbumCoverPath(result.getString("albumCover"));
	                song.setAudioFilePath(result.getString("file"));
	                
	                songs.add(song);
	            }
	        }
	    }
	    
	    return songs;
	}
	
	/**
	 * Get songs not in a specific playlist for adding to playlist
	 */
	public List<Song> getSongsNotInPlaylist(int playlistId, int userId) throws SQLException {
	    String query = """
	        SELECT s.*, g.name as genreName 
	        FROM Song s 
	        JOIN Genre g ON s.genreID = g.ID 
	        WHERE s.userID = ? 
	        AND s.ID NOT IN (
	            SELECT ps.songID 
	            FROM PlaylistSong ps 
	            WHERE ps.playlistID = ?
	        )
	        ORDER BY s.albumArtist ASC, s.albumReleaseYear ASC
	    """;
	    
	    List<Song> songs = new ArrayList<>();
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, userId);
	        pstatement.setInt(2, playlistId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            while (result.next()) {
	                Song song = new Song();
	                song.setID(result.getInt("ID"));
	                song.setUserID(result.getInt("userID"));
	                song.setName(result.getString("name"));
	                song.setAlbumName(result.getString("albumName"));
	                song.setArtistName(result.getString("albumArtist"));
	                song.setAlbumReleaseYear(result.getInt("albumReleaseYear"));
	                song.setGenre(result.getString("genreName"));
	                song.setAlbumCoverPath(result.getString("albumCover"));
	                song.setAudioFilePath(result.getString("file"));
	                
	                songs.add(song);
	            }
	        }
	    }
	    
	    return songs;
	}
	
	/**
	 * Add songs to existing playlist
	 */
	public boolean addSongsToPlaylist(int playlistId, int[] songIDs, int userId) throws SQLException {
	    boolean originalAutoCommit = connection.getAutoCommit();
	    
	    try {
	        connection.setAutoCommit(false);
	        
	        for (int songID : songIDs) {
	            // Check if song is already in playlist
	            if (!isSongInPlaylist(playlistId, songID)) {
	                addSongToPlaylist(playlistId, songID);
	            }
	        }
	        
	        connection.commit();
	        return true;
	    } catch (SQLException e) {
	        try {
	            connection.rollback();
	        } catch (SQLException rollbackEx) {
	            rollbackEx.printStackTrace();
	        }
	        throw e;
	    } finally {
	        try {
	            connection.setAutoCommit(originalAutoCommit);
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	/**
	 * Check if a song is already in a playlist
	 */
	private boolean isSongInPlaylist(int playlistId, int songId) throws SQLException {
	    String query = "SELECT COUNT(*) FROM PlaylistSong WHERE playlistID = ? AND songID = ?";
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, playlistId);
	        pstatement.setInt(2, songId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                return result.getInt(1) > 0;
	            }
	        }
	    }
	    return false;
	}
	
	/**
	 * Delete a playlist and its song associations without deleting the songs themselves
	 */
	public boolean deletePlaylist(int playlistId, int userId) throws SQLException {
	    boolean originalAutoCommit = connection.getAutoCommit();
	    
	    try {
	        connection.setAutoCommit(false);
	        
	        // First verify playlist belongs to user
	        String checkQuery = "SELECT ID FROM Playlist WHERE ID = ? AND userID = ?";
	        try (PreparedStatement pstatement = connection.prepareStatement(checkQuery)) {
	            pstatement.setInt(1, playlistId);
	            pstatement.setInt(2, userId);
	            try (ResultSet result = pstatement.executeQuery()) {
	                if (!result.next()) {
	                    // Playlist doesn't exist or doesn't belong to user
	                    return false;
	                }
	            }
	        }
	        
	        // Delete associations in PlaylistSong
	        String deleteAssociations = "DELETE FROM PlaylistSong WHERE playlistID = ?";
	        try (PreparedStatement pstatement = connection.prepareStatement(deleteAssociations)) {
	            pstatement.setInt(1, playlistId);
	            pstatement.executeUpdate();
	        }
	        
	        // Delete the playlist
	        String deletePlaylist = "DELETE FROM Playlist WHERE ID = ? AND userID = ?";
	        try (PreparedStatement pstatement = connection.prepareStatement(deletePlaylist)) {
	            pstatement.setInt(1, playlistId);
	            pstatement.setInt(2, userId);
	            int affectedRows = pstatement.executeUpdate();
	            
	            if (affectedRows == 0) {
	                connection.rollback();
	                return false;
	            }
	        }
	        
	        connection.commit();
	        return true;
	        
	    } catch (SQLException e) {
	        connection.rollback();
	        throw e;
	    } finally {
	        try {
	            connection.setAutoCommit(originalAutoCommit);
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
	}
}
