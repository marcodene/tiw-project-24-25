package it.polimi.tiw.projects.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.utils.FileStorageManager;

public class UserDAO {
	private Connection connection;
	
	public UserDAO (Connection connection) {
		this.connection = connection;
	}
	
	public User checkCredentials (String username, String password) throws SQLException {
		String query = "SELECT  id, username, name, surname FROM User  WHERE username = ? AND password =?";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, username);
			pstatement.setString(2, password);
			try (ResultSet result = pstatement.executeQuery();) {
				if(!result.isBeforeFirst()) // No user found
					return null;
				else {
					result.next();
					User user = new User();
					user.setId(result.getInt("id"));
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					return user;
				}
			}
		}
	}
	
	public boolean deleteUser(int userId) throws SQLException {
		boolean originalAutoCommit = connection.getAutoCommit();
		
		try {
			connection.setAutoCommit(false);
			
			String getSongsQuery = "SELECT file, albumCover FROM Song WHERE userID = ?";
			List<String[]> filesToDelete = new ArrayList<>();
			
			try (PreparedStatement pstatement = connection.prepareStatement(getSongsQuery)) {
				pstatement.setInt(1, userId);
				try (ResultSet result = pstatement.executeQuery()) {
					while (result.next()) {
						String[] files = new String[2];
						files[0] = result.getString("file"); 
						files[1] = result.getString("albumCover"); 
						filesToDelete.add(files);
					}
				}
			}
			
			String deleteUserQuery = "DELETE FROM User WHERE id = ?";
			try (PreparedStatement pstatement = connection.prepareStatement(deleteUserQuery)) {
				pstatement.setInt(1, userId);
				int affectedRows = pstatement.executeUpdate();
				
				if (affectedRows == 0) {
					connection.rollback();
					return false; 
				}
			}
			
			connection.commit();
			
			for (String[] files : filesToDelete) {
				String audioFilePath = files[0];
				String coverFilePath = files[1];
				
				if (audioFilePath != null && !audioFilePath.isEmpty()) {
					File audioFile = new File(FileStorageManager.getBaseStoragePath() + audioFilePath);
					if (audioFile.exists()) {
						audioFile.delete();
					}
				}
				
				if (coverFilePath != null && !coverFilePath.isEmpty()) {
					File coverFile = new File(FileStorageManager.getBaseStoragePath() + coverFilePath);
					if (coverFile.exists()) {
						coverFile.delete();
					}
				}
			}
			
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
	
	// Renamed for clarity and consistency with RegisterServletRIA intention
	public boolean isUsernameTaken(String username) throws SQLException{
		String query = "SELECT id FROM User WHERE username = ?";
	    try (PreparedStatement pstatement = connection.prepareStatement(query)) {
	        pstatement.setString(1, username);
	        try (ResultSet result = pstatement.executeQuery()) {
	            return result.isBeforeFirst(); 
	        }
	    }
	}
	
	// Internal method to insert user and get ID
	private int insertUserAndGetId(String username, String password, String name, String surname) throws SQLException {
	    String query = "INSERT INTO User (username, password, name, surname) VALUES (?, ?, ?, ?)";
	    try (PreparedStatement pstatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
	        pstatement.setString(1, username);
	        pstatement.setString(2, password); // Remember to hash passwords in a real application!
	        pstatement.setString(3, name);
	        pstatement.setString(4, surname);
	        int rowsAffected = pstatement.executeUpdate();
	        
	        if (rowsAffected == 0) {
                throw new SQLException("Creating User failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                	return generatedKeys.getInt(1); // Return the new user ID
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
	    }
	}

    // New method for RegisterServletRIA to use
    public User createUser(String username, String password, String name, String surname) throws SQLException {
        int newUserId = insertUserAndGetId(username, password, name, surname);
        
        // Now fetch the newly created user to return the full User object
        String query = "SELECT id, username, name, surname FROM User WHERE id = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setInt(1, newUserId);
            try (ResultSet result = pstatement.executeQuery()) {
                if (result.next()) {
                    User user = new User();
                    user.setId(result.getInt("id"));
                    user.setUsername(result.getString("username"));
                    user.setName(result.getString("name"));
                    user.setSurname(result.getString("surname"));
                    return user;
                } else {
                    // Should not happen if insertUserAndGetId succeeded without error
                    throw new SQLException("Failed to retrieve newly created user with id: " + newUserId);
                }
            }
        }
    }
}
