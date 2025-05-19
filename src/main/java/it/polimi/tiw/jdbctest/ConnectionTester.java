package it.polimi.tiw.jdbctest;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
public class ConnectionTester extends HttpServlet {
	private Connection connection = null;
	
	private static final long serialVersionUID = 1L;
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ServletContext servletContext = getServletContext();
		String result = "Connection worked";
		try {
			connection = ConnectionHandler.getConnection(servletContext);
		}catch (Exception e){
			result = "Connection failed";
			e.printStackTrace();
		}
		
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		out.println(result);
		out.close();
	}
}