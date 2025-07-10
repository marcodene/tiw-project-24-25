package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GenreDAO {
	private Connection connection;
	
	public GenreDAO (Connection connection) {
		this.connection = connection;
	}
	
	public List<String> getAllGenresNames() throws SQLException{
		String query = "SELECT name FROM Genre";
		List<String> genresNames = new ArrayList<>();
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					genresNames.add(result.getString("name"));
				}
			}
		}
		
		return genresNames;
	}
	
	public boolean existsGenreByName (String name) throws SQLException{
		String query = "SELECT name FROM Genre WHERE name=?";
		
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, name);
			try (ResultSet result = pstatement.executeQuery();) {
				if(!result.isBeforeFirst())
					return false;
				else {
					return true;
				}
			}
		}
	}
	
	public Integer getGenreIdByName (String name) throws SQLException {
		String query = "SELECT ID FROM Genre WHERE name=?";
		Integer ID;
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, name);
			try (ResultSet result = pstatement.executeQuery();) {
				if(!result.isBeforeFirst())
					return null;
				else {
					result.next();
					return result.getInt("ID");
				}
			}
		}
	}
}
