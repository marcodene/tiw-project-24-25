package it.polimi.tiw.projects.dao;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.utils.FileStorageManager; // Assuming this utility is set up

public class SongDAO {
	private Connection connection;
	
	public SongDAO (Connection connection) {
		this.connection = connection;
	}

	public boolean existsSongWithSameData(String name, String albumName, String artistName, int albumReleaseYear, int genreID, int userID) throws SQLException {
	    String query = "SELECT COUNT(*) FROM Song WHERE name=? AND albumName=? AND albumArtist=? AND albumReleaseYear=? AND genreID=? AND userID=?";
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setString(1, name);
	        pstatement.setString(2, albumName);
	        pstatement.setString(3, artistName);
	        pstatement.setInt(4, albumReleaseYear);
	        pstatement.setInt(5, genreID);
	        pstatement.setInt(6, userID);
	        
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	                return result.getInt(1) > 0;
	            }
	        }
	    }
	    return false;
	}
	
	// Modified to return the created Song object
	public Song uploadSong(Song song) throws SQLException {
		String query = "INSERT INTO Song (userID, name, genreID, file, albumCover, albumName, albumArtist, albumReleaseYear) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		int generatedSongId = -1;
		
		// Validate genre is provided and exists
		if (song.getGenre() == null || song.getGenre().trim().isEmpty()) {
		    throw new SQLException("Genre is mandatory and cannot be empty.");
		}
		
		GenreDAO genreDAO = new GenreDAO(connection);
		Integer genreIdInteger;
		try {
			genreIdInteger = genreDAO.getGenreIdByName(song.getGenre().trim());
			if (genreIdInteger == null) {
			    throw new SQLException("Genre '" + song.getGenre() + "' not found. Please select a valid genre.");
			}
		} catch (SQLException e) {
		    // If GenreDAO throws an error finding the genre, propagate it
		    throw new SQLException("Error validating genre: " + song.getGenre(), e);
		}
		
		int genreId = genreIdInteger; // Safe conversion since we checked for null

		try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			pstatement.setInt(1, song.getUserID());
			pstatement.setString(2, song.getName());
			pstatement.setInt(3, genreId); // Use resolved genreId
	        
            // Audio file path is mandatory for RIA
        if (song.getAudioFilePath() == null || song.getAudioFilePath().trim().isEmpty()) {
            throw new SQLException("Audio file is mandatory and cannot be empty.");
        }
        pstatement.setString(4, song.getAudioFilePath());
	        
            // Album cover is now mandatory
        if (song.getAlbumCoverPath() == null || song.getAlbumCoverPath().trim().isEmpty()) {
            throw new SQLException("Album cover is mandatory and cannot be empty.");
        }
        pstatement.setString(5, song.getAlbumCoverPath());
	        pstatement.setString(6, song.getAlbumName());
	        pstatement.setString(7, song.getArtistName());
	        pstatement.setInt(8, song.getAlbumReleaseYear());
	        
	        int affectedRows = pstatement.executeUpdate();

	        if (affectedRows > 0) {
	        	try (ResultSet generatedKeys = pstatement.getGeneratedKeys()) {
	                if (generatedKeys.next()) {
	                    generatedSongId = generatedKeys.getInt(1);
	                    // Fetch the newly created song to return it (this ensures all fields are fresh from DB)
	                    return getSongByIDAndUser(generatedSongId, song.getUserID());
	                } else {
	                    throw new SQLException("Creating song failed, no ID obtained.");
	                }
	            }
	        } else {
	        	throw new SQLException("Creating song failed, no rows affected.");
	        }
		}
	}
	
	public List<Song> getAllSongsByUserId(int userID) throws SQLException {
		String query = "SELECT s.*, g.name as genreName FROM Song s JOIN Genre g ON s.genreID = g.ID WHERE s.userID = ? ORDER BY s.albumArtist ASC, s.albumReleaseYear ASC";
	    List<Song> songs = new ArrayList<>();
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, userID);
	        try (ResultSet result = pstatement.executeQuery()) {
	            while (result.next()) {
	                songs.add(mapRowToSong(result));
	            }
	        }
	    }
	    return songs;
	}
	
	public boolean existAllSongsByIDsAndUser(int[] songIDs, int userID) throws SQLException {
	    if (songIDs == null || songIDs.length == 0) return true; // Or false, depending on desired logic for empty array
	    for (int songID : songIDs) {
	        if (!songBelongsToUser(songID, userID)) {
	            return false;
	        }
	    }
	    return true;
	}
	
	public Song getSongByIDAndUser(int songId, int userId) throws SQLException {
	    String query = "SELECT s.*, g.name as genreName FROM Song s JOIN Genre g ON s.genreID = g.ID WHERE s.ID = ? AND s.userID = ?";
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, songId);
	        pstatement.setInt(2, userId);
	        try (ResultSet result = pstatement.executeQuery()) {
	            if (result.next()) {
	            	return mapRowToSong(result);
	            } else {
	                return null; 
	            }
	        }
	    }
	}
	
	public boolean deleteSong(int songID, int userID) throws SQLException {
	    boolean originalAutoCommit = connection.getAutoCommit();
	    try {
	        connection.setAutoCommit(false);
	        Song song = getSongByIDAndUser(songID, userID);
	        if (song == null) { // Already checks ownership
	            connection.rollback(); // Nothing to delete if song not found or not owned
	            return false; // Or throw new SQLException("Song not found or access denied");
	        }
	        
	        String deleteRelations = "DELETE FROM PlaylistSong WHERE songID = ?";
	        try (PreparedStatement pstmt = connection.prepareStatement(deleteRelations)) {
	            pstmt.setInt(1, songID);
	            pstmt.executeUpdate(); // Delete associations first
	        }
	        
	        String deleteSong = "DELETE FROM Song WHERE ID = ?"; // userID check already done by getSongByIDAndUser
	        try (PreparedStatement pstmt = connection.prepareStatement(deleteSong)) {
	            pstmt.setInt(1, songID);
	            int affected = pstmt.executeUpdate();
	            if (affected == 0) {
	                connection.rollback();
	                throw new SQLException("Failed to delete song record, no rows affected.");
	            }
	        }
	        connection.commit();
	        deletePhysicalFiles(song); // Delete files after successful DB commit
	        return true;
	    } catch (SQLException e) {
	        connection.rollback();
	        throw e;
	    } finally {
	        connection.setAutoCommit(originalAutoCommit);
	    }
	}
	
	private void deletePhysicalFiles(Song song) {
	    String baseStoragePath = null;
	    Path basePath = null;
	    
	    try {
	        baseStoragePath = FileStorageManager.getBaseStoragePath();
	        
	        if (baseStoragePath == null || baseStoragePath.trim().isEmpty()) {
	            throw new IllegalStateException("FileStorageManager not properly initialized - base storage path is null or empty");
	        }
	        
	        // Convert to Path for safe handling and normalize
	        basePath = Paths.get(baseStoragePath).toAbsolutePath().normalize();
	        
	    } catch (Exception e) {
	        System.err.println("Error: Could not get base storage path for deleting files. " + e.getMessage());
	        System.err.println("File deletion aborted to prevent incorrect path operations.");
	        return;
	    }

	    // Securely delete cover file with path validation
	    if (song.getAlbumCoverPath() != null && !song.getAlbumCoverPath().isEmpty()) {
	        try {
	            if (!deleteFileSecurely(basePath, song.getAlbumCoverPath(), "covers", "cover")) {
	                System.err.println("Failed to securely delete cover file: " + song.getAlbumCoverPath());
	            }
	        } catch (Exception e) {
	            System.err.println("Error deleting cover file: " + e.getMessage());
	        }
	    }
	    
	    // Securely delete audio file with path validation
	    if (song.getAudioFilePath() != null && !song.getAudioFilePath().isEmpty()) {
	        try {
	            if (!deleteFileSecurely(basePath, song.getAudioFilePath(), "songs", "audio")) {
	                System.err.println("Failed to securely delete audio file: " + song.getAudioFilePath());
	            }
	        } catch (Exception e) {
	            System.err.println("Error deleting audio file: " + e.getMessage());
	        }
	    }
	}
	
	/**
	 * Securely deletes a file with path validation to prevent path traversal attacks.
	 * This method ensures that files can only be deleted from the expected directories
	 * and prevents malicious paths from escaping the base storage directory.
	 * 
	 * @param basePath The base storage path as a normalized Path object (e.g., /var/webapp/storage)
	 * @param relativePath The relative file path from database (e.g., "/covers/filename.jpg")
	 * @param expectedDir The expected directory name ("covers" or "songs")
	 * @param fileType Description for logging ("cover" or "audio")
	 * @return true if file was successfully deleted or didn't exist, false on failure
	 */
	private boolean deleteFileSecurely(Path basePath, String relativePath, String expectedDir, String fileType) {
	    try {
	        // Clean the path by removing leading slash if present
	        // This converts "/covers/file.jpg" to "covers/file.jpg" for consistent processing
	        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
	        
	        // Perform comprehensive security validation
	        // This checks for dangerous characters and ensures proper path structure
	        if (!isPathSafe(cleanPath, expectedDir)) {
	            System.err.println("Security: Unsafe " + fileType + " path rejected: " + relativePath);
	            return false;
	        }
	        
	        // Build the target path safely using Java NIO Path API
	        // The resolve() method safely combines paths, and normalize() removes any ".." or "." components
	        // Example: basePath="/var/storage" + cleanPath="covers/file.jpg" → "/var/storage/covers/file.jpg"
	        Path targetPath = basePath.resolve(cleanPath).normalize();
	        
	        // This ensures the final path is still within our base directory
	        // Even if an attacker somehow bypassed previous checks, this prevents directory escape
	        // Example: if targetPath="/etc/passwd", it won't start with basePath="/var/storage"
	        if (!targetPath.startsWith(basePath)) {
	            System.err.println("Security: Path traversal blocked for " + fileType + ": " + relativePath);
	            return false;
	        }
	        
	        // Step 5: Perform the actual file deletion with proper validation
	        return performFileDeletion(targetPath, fileType);
	        
	    } catch (Exception e) {
	        System.err.println("Error during " + fileType + " file deletion: " + e.getMessage());
	        return false;
	    }
	}

	/**
	 * Validates if a path is safe for file operations by checking for common attack patterns.
	 * This method implements multiple layers of validation to prevent path traversal attacks.
	 * 
	 * @param cleanPath The cleaned path without leading slash (e.g., "covers/file.jpg")
	 * @param expectedDir The directory that should contain the file ("covers" or "songs")
	 * @return true if the path is safe to use, false if it contains dangerous patterns
	 */
	private boolean isPathSafe(String cleanPath, String expectedDir) {
	    // Block dangerous characters that could be used for path traversal
	    // ".." moves up one directory level, "\" is Windows path separator that could bypass checks
	    if (cleanPath.contains("..") || cleanPath.contains("\\") || cleanPath.isEmpty()) {
	        return false;
	    }
	    
	    // Enforce strict path structure - must be exactly "directory/filename"
	    // This prevents paths like "covers/subfolder/file.jpg" or just "file.jpg"
	    // Split by "/" and ensure we get exactly 2 parts: [directory, filename]
	    String[] parts = cleanPath.split("/");
	    if (parts.length != 2 || !expectedDir.equals(parts[0])) {
	        return false;
	    }
	    
	    // Validate the filename itself
	    // Block empty names, current directory ".", parent directory "..", and hidden files starting with "."
	    // This prevents attacks using special directory references
	    String filename = parts[1];
	    return !filename.isEmpty() && 
	           !filename.equals(".") && 
	           !filename.equals("..") && 
	           !filename.startsWith(".");
	}

	/**
	 * Performs the actual file deletion with proper validation and error handling.
	 * This method handles the final steps of file deletion after all security checks have passed.
	 * 
	 * @param targetPath The fully validated and safe path to the file to delete
	 * @param fileType Description for logging purposes ("cover" or "audio")
	 * @return true if deletion succeeded or file didn't exist, false on failure
	 */
	private boolean performFileDeletion(Path targetPath, String fileType) {
	    // Convert Path to File object for deletion operation
	    File targetFile = targetPath.toFile();
	    
	    // File doesn't exist - this is actually OK
	    // The file might have been deleted already, or never existed due to upload failure
	    if (!targetFile.exists()) {
	        System.out.println(fileType + " file not found (already deleted or never existed): " + targetPath);
	        return true; // Consider this a success since the end goal (file not existing) is achieved
	    }
	    
	    // Target exists but is not a regular file (could be a directory or special file)
	    // This is a security concern - we should never try to delete directories or special files
	    if (!targetFile.isFile()) {
	        System.err.println("Security: Target is not a regular file, deletion blocked: " + targetPath);
	        return false;
	    }
	    
	    // File exists and is a regular file - attempt deletion
	    if (targetFile.delete()) {
	        System.out.println("Successfully deleted " + fileType + " file: " + targetPath);
	        return true;
	    } else {
	        // Deletion failed - could be due to file permissions, file being in use, or filesystem issues
	        System.err.println("Failed to delete " + fileType + " file (filesystem error): " + targetPath);
	        return false;
	    }
	}
	
	public boolean songBelongsToUser(int songID, int userID) throws SQLException {
	    String query = "SELECT COUNT(*) FROM Song WHERE ID = ? AND userID = ?";
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setInt(1, songID);
	        pstatement.setInt(2, userID);
	        try (ResultSet result = pstatement.executeQuery()) {
	            return result.next() && result.getInt(1) > 0;
	        }
	    }
	}

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

}
