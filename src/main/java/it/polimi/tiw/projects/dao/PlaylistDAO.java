package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;

public class PlaylistDAO {
	private Connection connection;
	
	public PlaylistDAO (Connection connection) {
		this.connection = connection;
	}
	
	// Modified to return the created Playlist object
	public Playlist createPlaylist(String name, int[] songIDs, int userID) throws SQLException {
	    boolean originalAutoCommit = connection.getAutoCommit();
	    int playlistID = -1;
	    
	    try {
	        connection.setAutoCommit(false);
	        
	        if (existsPlaylistByNameAndUser(name, userID)) {
	            // Consider throwing a custom exception or returning null/specific error code
	            // For now, SQLException is fine as per original structure.
	            throw new SQLException("Playlist with name '" + name + "' already exists for this user.");
	        }
	        
	        playlistID = insertPlaylistAndGetId(name, userID);
	        
	        SongDAO songDAO = new SongDAO(connection); // Assuming SongDAO is available and connection managed
	        for (int songID : songIDs) {
	            if (!songDAO.songBelongsToUser(songID, userID)) {
	                throw new SQLException("Song ID " + songID + " does not belong to user " + userID);
	            }
	            addSongToPlaylistAssociation(playlistID, songID);
	        }
	        
	        connection.commit();
	        
	        // After successful commit, fetch the created playlist with its songs
	        return getPlaylistByIdAndUser(playlistID, userID, true); // true to fetch songs

	    } catch (SQLException e) {
	        try {
	            connection.rollback();
	        } catch (SQLException rollbackEx) {
	            rollbackEx.printStackTrace(); // Log rollback error
	        }
	        throw e; // Re-throw original exception
	    } finally {
	        try {
	            connection.setAutoCommit(originalAutoCommit);
	        } catch (SQLException e) {
	            e.printStackTrace(); // Log error setting autocommit back
	        }
	    }
	}
	
	// Renamed from insertPlaylist to clarify it returns ID
	private int insertPlaylistAndGetId(String name, int userID) throws SQLException {
        String query = "INSERT INTO Playlist (name, creationDate, userID) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstatement.setString(1, name);
            pstatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            //pstatement.setDate(2, new Date(new java.util.Date().getTime())); // Current date
            pstatement.setInt(3, userID);
            
            int affectedRows = pstatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating playlist failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Return generated playlist ID
                } else {
                    throw new SQLException("Creating playlist failed, no ID obtained.");
                }
            }
        }
    }
	
	// Renamed from addSongToPlaylist for clarity
	private void addSongToPlaylistAssociation(int playlistID, int songID) throws SQLException {
        String query = "INSERT INTO PlaylistSong (playlistID, songID, customOrder) VALUES (?, ?, NULL)"; // Assuming customOrder can be NULL initially
        
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, playlistID);
            pstatement.setInt(2, songID);
            pstatement.executeUpdate();
        }
    }

    // Fixed to query Playlist table
	public boolean existsPlaylistByNameAndUser(String name, int userID) throws SQLException{
		String query = "SELECT ID FROM Playlist WHERE name = ? AND userID = ?"; // Corrected table and columns
		
		try (PreparedStatement pstatement = connection.prepareStatement(query)) {
			pstatement.setString(1, name);
			pstatement.setInt(2, userID);
			try (ResultSet result = pstatement.executeQuery()) {
				return result.isBeforeFirst(); // True if a playlist with this name for this user exists
			}
		}
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
	                Timestamp ts = result.getTimestamp("creationDate");
	                playlist.setCreationDate(new java.sql.Date(ts.getTime()));
	                // Songs are not fetched here for efficiency in list view, fetch them on demand for playlist detail view
	                playlists.add(playlist);
	            }
	        }
	    }
	    return playlists;
	}
	
    // Added new method as required by PlaylistServletRIA
    public Playlist getPlaylistByNameAndUser(String name, int userId) throws SQLException {
        return getPlaylistByNameAndUser(name, userId, false); // Default to not fetching songs
    }

    public Playlist getPlaylistByNameAndUser(String name, int userId, boolean fetchSongs) throws SQLException {
	    String query = "SELECT * FROM Playlist WHERE name = ? AND userID = ?";
	    Playlist playlist = null;
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setString(1, name);
	        pstatement.setInt(2, userId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                playlist = new Playlist();
	                playlist.setID(result.getInt("ID"));
	                playlist.setUserID(result.getInt("userID"));
	                playlist.setName(result.getString("name"));
	                playlist.setCreationDate(result.getDate("creationDate"));
	                if (fetchSongs) {
	                    playlist.setSongs(getSongsFromPlaylistOrdered(playlist.getID()));
                        // Also fetch custom order if exists
                        playlist.setCustomSongOrder(getCustomSongOrder(playlist.getID()));
	                }
	            }
	        }
	    }
	    return playlist;
	}

	public Playlist getPlaylistByIdAndUser(int playlistId, int userId) throws SQLException {
        return getPlaylistByIdAndUser(playlistId, userId, false); // Default to not fetching songs
    }

	public Playlist getPlaylistByIdAndUser(int playlistId, int userId, boolean fetchSongs) throws SQLException {
	    String query = "SELECT * FROM Playlist WHERE ID = ? AND userID = ?";
	    Playlist playlist = null;
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, playlistId);
	        pstatement.setInt(2, userId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                playlist = new Playlist();
	                playlist.setID(result.getInt("ID"));
	                playlist.setUserID(result.getInt("userID"));
	                playlist.setName(result.getString("name"));
	                playlist.setCreationDate(result.getDate("creationDate"));
	                if (fetchSongs) {
	                    playlist.setSongs(getSongsFromPlaylistOrdered(playlist.getID()));
                        playlist.setCustomSongOrder(getCustomSongOrder(playlist.getID()));
	                }
	            }
	        }
	    }
	    return playlist;
	}
	
    // Renamed from getSongsFromPlaylist to getSongsFromPlaylistOrdered for clarity
	public List<Song> getSongsFromPlaylistOrdered(int playlistId) throws SQLException {
	    // Check if custom order exists
        List<Integer> customOrder = getCustomSongOrder(playlistId);
        List<Song> songs = new ArrayList<>();
        String query;

        if (customOrder != null && !customOrder.isEmpty()) {
            // Fetch songs based on custom order
            query = "SELECT s.*, g.name as genreName, ps.customOrder " +
                    "FROM Song s " +
                    "JOIN PlaylistSong ps ON s.ID = ps.songID " +
                    "JOIN Genre g ON s.genreID = g.ID " +
                    "WHERE ps.playlistID = ?";
            Map<Integer, Song> songMap = new HashMap<>();
             try (PreparedStatement pstatement = connection.prepareStatement(query)) {
                pstatement.setInt(1, playlistId);
                try (ResultSet result = pstatement.executeQuery()) {
                    while (result.next()) {
                        Song song = mapRowToSong(result);
                        songMap.put(song.getID(), song);
                    }
                }
            }
            for(Integer songId : customOrder) {
                if(songMap.containsKey(songId)) {
                    songs.add(songMap.get(songId));
                }
            }

        } else {
            // Fetch songs with default order: artist, then year
            query = "SELECT s.*, g.name as genreName, ps.customOrder " +
                    "FROM Song s " +
                    "JOIN PlaylistSong ps ON s.ID = ps.songID " +
                    "JOIN Genre g ON s.genreID = g.ID " +
                    "WHERE ps.playlistID = ? " +
                    "ORDER BY s.albumArtist ASC, s.albumReleaseYear ASC";
            try (PreparedStatement pstatement = connection.prepareStatement(query)) {
                pstatement.setInt(1, playlistId);
                try (ResultSet result = pstatement.executeQuery()) {
                    while (result.next()) {
                        songs.add(mapRowToSong(result));
                    }
                }
            }
        }
	    return songs;
	}
	
	public List<Song> getSongsNotInPlaylist(int playlistId, int userId) throws SQLException {
	    String query = "SELECT s.*, g.name as genreName FROM Song s JOIN Genre g ON s.genreID = g.ID " +
	                   "WHERE s.userID = ? AND s.ID NOT IN (SELECT ps.songID FROM PlaylistSong ps WHERE ps.playlistID = ?) " +
	                   "ORDER BY s.albumArtist ASC, s.albumReleaseYear ASC";
	    List<Song> songs = new ArrayList<>();
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, userId);
	        pstatement.setInt(2, playlistId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            while (result.next()) {
	                songs.add(mapRowToSong(result));
	            }
	        }
	    }
	    return songs;
	}
	
	public boolean addSongsToPlaylist(int playlistId, int[] songIDs, int userId) throws SQLException {
	    boolean originalAutoCommit = connection.getAutoCommit();
	    try {
	        connection.setAutoCommit(false);
	        SongDAO songDAO = new SongDAO(connection);

	        // Controlla se la playlist ha già un ordine personalizzato
	        List<Integer> existingOrder = getCustomSongOrder(playlistId);
	        boolean hasCustomOrder = (existingOrder != null && !existingOrder.isEmpty());
	        
	        // Se non c'è un ordine personalizzato, non impostare customOrder
	        if (!hasCustomOrder) {
	            // Aggiungi le canzoni senza customOrder
	            for (int songID : songIDs) {
	                if (!songDAO.songBelongsToUser(songID, userId)) {
	                    throw new SQLException("Song ID " + songID + " does not belong to user " + userId);
	                }
	                if (!isSongInPlaylist(playlistId, songID)) {
	                    addSongToPlaylistAssociation(playlistId, songID); // Usa il metodo senza order
	                }
	            }
	        } else {
	            // Se c'è già un ordine personalizzato, calcola il maxOrder
	            int maxOrder = 0;
	            String maxOrderQuery = "SELECT MAX(customOrder) FROM PlaylistSong WHERE playlistID = ?";
	            try (PreparedStatement pstatement = connection.prepareStatement(maxOrderQuery)) {
	                pstatement.setInt(1, playlistId);
	                try (ResultSet rs = pstatement.executeQuery()) {
	                    if (rs.next()) {
	                        maxOrder = rs.getInt(1);
	                    }
	                }
	            }

	            for (int songID : songIDs) {
	                if (!songDAO.songBelongsToUser(songID, userId)) {
	                    throw new SQLException("Song ID " + songID + " does not belong to user " + userId);
	                }
	                if (!isSongInPlaylist(playlistId, songID)) {
	                    maxOrder++;
	                    addSongToPlaylistAssociationWithOrder(playlistId, songID, maxOrder);
	                }
	            }
	        }
	        
	        connection.commit();
	        return true;
	    } catch (SQLException e) {
	        connection.rollback();
	        throw e;
	    } finally {
	        connection.setAutoCommit(originalAutoCommit);
	    }
	}
	
	private boolean isSongInPlaylist(int playlistId, int songId) throws SQLException {
	    String query = "SELECT COUNT(*) FROM PlaylistSong WHERE playlistID = ? AND songID = ?";
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, playlistId);
	        pstatement.setInt(2, songId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            return result.next() && result.getInt(1) > 0;
	        }
	    }
	}
	
	public boolean deletePlaylist(int playlistId, int userId) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
	    try {
	        connection.setAutoCommit(false);
	        if (getPlaylistByIdAndUser(playlistId, userId) == null) return false; // Verify ownership
	        
	        String deleteAssociations = "DELETE FROM PlaylistSong WHERE playlistID = ?";
	        try (PreparedStatement pstatement = connection.prepareStatement(deleteAssociations)) {
	            pstatement.setInt(1, playlistId);
	            pstatement.executeUpdate();
	        }
	        
	        String deletePlaylist = "DELETE FROM Playlist WHERE ID = ? AND userID = ?";
	        try (PreparedStatement pstatement = connection.prepareStatement(deletePlaylist)) {
	            pstatement.setInt(1, playlistId);
	            pstatement.setInt(2, userId); // Already ensured user owns it
	            int affectedRows = pstatement.executeUpdate();
	            if (affectedRows == 0) {
	                connection.rollback(); return false;
	            }
	        }
	        connection.commit();
	        return true;
	    } catch (SQLException e) {
	        connection.rollback(); throw e;
	    } finally {
	        connection.setAutoCommit(originalAutoCommit);
	    }
	}

    // Helper to map ResultSet row to Song bean
    private Song mapRowToSong(ResultSet result) throws SQLException {
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
        return song;
    }

    // Methods for custom order
    public List<Integer> getCustomSongOrder(int playlistId) throws SQLException {
        List<Integer> order = new ArrayList<>();
        // Query PlaylistSong table, ordering by 'customOrder' field
        String query = "SELECT songID FROM PlaylistSong WHERE playlistID = ? AND customOrder IS NOT NULL ORDER BY customOrder ASC";
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, playlistId);
            try (ResultSet rs = pstatement.executeQuery()) {
                while (rs.next()) {
                    order.add(rs.getInt("songID"));
                }
            }
        }
        return order.isEmpty() ? null : order; // Return null if no custom order set
    }

    public void saveCustomSongOrder(int playlistId, List<Integer> songIdsInOrder, int userId) throws SQLException {
        // First, verify playlist belongs to user
        if (getPlaylistByIdAndUser(playlistId, userId) == null) {
            throw new SQLException("Playlist not found or user mismatch.");
        }
        
        // Validate that all songIdsInOrder actually belong to this playlist
        String validateQuery = "SELECT COUNT(*) FROM PlaylistSong WHERE playlistID = ? AND songID = ?";
        try (PreparedStatement validateStatement = connection.prepareStatement(validateQuery)) {
            for (Integer songId : songIdsInOrder) {
                validateStatement.setInt(1, playlistId);
                validateStatement.setInt(2, songId);
                try (ResultSet rs = validateStatement.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        throw new SQLException("Song ID " + songId + " does not belong to playlist " + playlistId);
                    }
                }
            }
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            // Get all songs currently in the playlist
            List<Integer> allSongsInPlaylist = new ArrayList<>();
            String getAllSongsQuery = "SELECT songID FROM PlaylistSong WHERE playlistID = ?";
            try (PreparedStatement getAllStatement = connection.prepareStatement(getAllSongsQuery)) {
                getAllStatement.setInt(1, playlistId);
                try (ResultSet rs = getAllStatement.executeQuery()) {
                    while (rs.next()) {
                        allSongsInPlaylist.add(rs.getInt("songID"));
                    }
                }
            }
            
            // Update customOrder for songs in the new order (1-based)
            String updateQuery = "UPDATE PlaylistSong SET customOrder = ? WHERE playlistID = ? AND songID = ?";
            try (PreparedStatement pstatement = connection.prepareStatement(updateQuery)) {
                for (int i = 0; i < songIdsInOrder.size(); i++) {
                    pstatement.setInt(1, i + 1); // Order is 1-based
                    pstatement.setInt(2, playlistId);
                    pstatement.setInt(3, songIdsInOrder.get(i));
                    pstatement.addBatch();
                }
                pstatement.executeBatch();
            }
            
            // Assign order to songs not in the reorder list (they go after the ordered ones)
            int nextOrder = songIdsInOrder.size() + 1;
            for (Integer songId : allSongsInPlaylist) {
                if (!songIdsInOrder.contains(songId)) {
                    try (PreparedStatement pstatement = connection.prepareStatement(updateQuery)) {
                        pstatement.setInt(1, nextOrder++);
                        pstatement.setInt(2, playlistId);
                        pstatement.setInt(3, songId);
                        pstatement.executeUpdate();
                    }
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
    
    // New method for adding songs with order, used by addSongsToPlaylist
    private void addSongToPlaylistAssociationWithOrder(int playlistID, int songID, int order) throws SQLException {
        String query = "INSERT INTO PlaylistSong (playlistID, songID, customOrder) VALUES (?, ?, ?)";
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, playlistID);
            pstatement.setInt(2, songID);
            pstatement.setInt(3, order); // Set the order
            pstatement.executeUpdate();
        }
    }

}
