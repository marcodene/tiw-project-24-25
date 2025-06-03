package it.polimi.tiw.projects.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;

public class ConnectionHandler {
	private static final String PROPERTIES_FILE = "/WEB-INF/database.properties";

	public static Connection getConnection(ServletContext context) throws UnavailableException {
		Connection connection = null;
		Properties dbProperties = new Properties();
		
		try {
			// Carica il file di proprietà dalla directory WEB-INF
			InputStream inputStream = context.getResourceAsStream(PROPERTIES_FILE);
			if (inputStream == null) {
				throw new UnavailableException("Database configuration file not found: " + PROPERTIES_FILE);
			}
			
			// Carica le proprietà dal file
			dbProperties.load(inputStream);
			inputStream.close();
			
			// Ottieni le credenziali dal file properties
			String driver = dbProperties.getProperty("dbDriver");
			String url = dbProperties.getProperty("dbUrl");
			String user = dbProperties.getProperty("dbUser");
			String password = dbProperties.getProperty("dbPassword");
			
			// Verifica che tutte le proprietà necessarie siano presenti
			if (driver == null || url == null || user == null || password == null) {
				throw new UnavailableException("Missing database connection parameters in configuration file");
			}
			// Stabilisci la connessione
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);
			
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		}catch (IOException e) {
			throw new UnavailableException("Error reading database configuration file: " + e.getMessage());
		}
		return connection;
	}

	public static void closeConnection(Connection connection) throws SQLException {
		if (connection != null) {
			connection.close();
		}
	}
	
}
