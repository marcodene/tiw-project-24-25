package it.polimi.tiw.projects.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
				if(!result.isBeforeFirst())
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
			
			// Get all songs for this user to delete physical files later
			String getSongsQuery = "SELECT file, albumCover FROM Song WHERE userID = ?";
			List<String[]> filesToDelete = new ArrayList<>();
			
			try (PreparedStatement pstatement = connection.prepareStatement(getSongsQuery)) {
				pstatement.setInt(1, userId);
				try (ResultSet result = pstatement.executeQuery()) {
					while (result.next()) {
						String[] files = new String[2];
						files[0] = result.getString("file"); // Audio file
						files[1] = result.getString("albumCover"); // Cover image
						filesToDelete.add(files);
					}
				}
			}
			
			// Delete the user (cascading deletes will take care of playlists and songs)
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
			
			// After successful database deletion, delete physical files
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
}
