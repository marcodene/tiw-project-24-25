package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // Although not strictly needed for GET genres, good practice for consistency if auth were added

import com.google.gson.Gson;

import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/api/genres")
public class GenreServletRIA extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson = new Gson();

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionHandler.getConnection(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Optional: Check for user session if genres should be protected,
        // but typically genre lists are public. For this project, let's assume public.
        /* 
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }
        */

        GenreDAO genreDAO = new GenreDAO(connection);
        try {
            List<String> genreNames = genreDAO.getAllGenresNames();
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", "success");
            responseMap.put("data", genreNames); // Sending list of strings directly
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(responseMap));

        } catch (SQLException e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Database error fetching genres: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(errorResponse));
        }
    }

    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
