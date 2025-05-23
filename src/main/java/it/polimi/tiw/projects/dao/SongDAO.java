package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;

public class SongDAO {
	private Connection connection;
	
	public SongDAO (Connection connection) {
		this.connection = connection;
	}
	
	public boolean existsSongByNameAndUser (String name, int userID ) throws SQLException {
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

	public boolean uploadSong(Song song) throws SQLException {
		String query = "INSERT INTO Song (userID, name, genreID, file, albumCover, albumName, albumArtist, albumReleaseYear) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		
		try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			pstatement.setInt(1, song.getUserID());
			pstatement.setString(2, song.getName());
			
			GenreDAO genreDAO = new GenreDAO(connection);
			// TODO non può restituire errore perchè il controllo di correttezza del genre è già stato fatto
			int genreId = genreDAO.getGenreIdByName(song.getGenre());
	        pstatement.setInt(3, genreId);
	        
	        //TODO togliere il fatto che il file audio sia opzionale
	        if (song.getAudioFilePath() != null && !song.getAudioFilePath().isEmpty()) {
	            pstatement.setString(4, song.getAudioFilePath());
	        } else {
	            pstatement.setNull(4, java.sql.Types.VARCHAR);
	        }
	        
	        if (song.getAlbumCoverPath() != null && !song.getAlbumCoverPath().isEmpty()) {
	            pstatement.setString(5, song.getAlbumCoverPath());
	        } else {
	            pstatement.setNull(5, java.sql.Types.VARCHAR);
	        }
	        pstatement.setString(6, song.getAlbumName());
	        pstatement.setString(7, song.getArtistName());
	        pstatement.setInt(8, song.getAlbumReleaseYear());
	        
	        int affectedRows = pstatement.executeUpdate();
	        
	        return affectedRows > 0;
		}
	}
	
	public List<Song> getAllSongsByUserId(int userID) throws SQLException {
		String query = "SELECT s.*, g.name as genreName FROM Song s JOIN Genre g ON s.genreID = g.ID WHERE s.userID = ? ORDER BY s.albumArtist, s.albumReleaseYear";
	    List<Song> songs = new ArrayList<>();
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, userID);
	        try (ResultSet result = pstatement.executeQuery()) {
	            while (result.next()) {
	                Song song = new Song();
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
	
	public boolean existAllSongsByNamesAndUser(String[] songNames, int userID) throws SQLException {
		for (String songName : songNames) {
		    if(!existsSongByNameAndUser(songName, userID))
		    	return false;
		}
		return true;
	}
	
	public int getSongIDByNameAndUser(String songName, int userID) throws SQLException {
	    String query = "SELECT ID FROM Song WHERE name = ? AND userID = ?";
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setString(1, songName);
	        pstatement.setInt(2, userID);
	        
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                return result.getInt("ID");
	            } else {
	                return -1; // Song not found
	            }
	        }
	    }
	}
	

	public Song getSongByNameAndUser(String songName, int userID) throws SQLException {
	    String query = "SELECT s.*, g.name as genreName FROM Song s " +
	                  "JOIN Genre g ON s.genreID = g.ID " +
	                  "WHERE s.name = ? AND s.userID = ?";
	    
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setString(1, songName);
	        pstatement.setInt(2, userID);
	        
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                Song song = new Song();
	                song.setUserID(result.getInt("userID"));
	                song.setName(result.getString("name"));
	                song.setAlbumName(result.getString("albumName"));
	                song.setArtistName(result.getString("albumArtist"));
	                song.setAlbumReleaseYear(result.getInt("albumReleaseYear"));
	                song.setGenre(result.getString("genreName"));
	                song.setAlbumCoverPath(result.getString("albumCover"));
	                song.setAudioFilePath(result.getString("file"));
	                
	                return song;
	            } else {
	                return null; // Song not found
	            }
	        }
	    }
	}
}
