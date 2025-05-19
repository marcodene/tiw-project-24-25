package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import it.polimi.tiw.projects.beans.User;

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
}
