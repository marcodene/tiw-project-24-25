package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

// Path changed to handle /api/playlists/*
@WebServlet(name = "PlaylistServletRIA", urlPatterns = {"/api/playlists", "/api/playlists/*"})
public class PlaylistServletRIA extends HttpServlet {
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
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }
        User user = (User) session.getAttribute("user");
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/playlists - List all playlists
                List<Playlist> playlists = playlistDAO.getAllPlaylistsByUserId(user.getId());
                List<Map<String, Object>> playlistsJson = new ArrayList<>();
                for (Playlist p : playlists) {
                    playlistsJson.add(p.toJSON()); 
                }
                sendSuccess(response, playlistsJson, HttpServletResponse.SC_OK);
            } else {
                // GET /api/playlists/{id} - Get specific playlist
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) { // e.g., /<id>
                    try {
                        int playlistId = Integer.parseInt(pathParts[1]);
                        // Fetch with songs and custom order
                        Playlist playlist = playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId(), true); 
                        if (playlist != null) {
                            sendSuccess(response, playlist.toJSON(), HttpServletResponse.SC_OK);
                        } else {
                            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Playlist not found or access denied.");
                        }
                    } catch (NumberFormatException e) {
                        sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID format.");
                    }
                } else {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path for GET request.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) // Create Playlist or Add Songs
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }
        User user = (User) session.getAttribute("user");
        String pathInfo = request.getPathInfo();

        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Map<String, Object> payload;
        try {
            payload = gson.fromJson(requestBody, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (JsonSyntaxException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format.");
            return;
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        SongDAO songDAO = new SongDAO(connection);

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/playlists - Create new playlist
                String playlistName = (String) payload.get("name");
                List<Double> songIdDoubles = (List<Double>) payload.get("songIDs"); // Gson parses numbers as Doubles

                Map<String, String> errors = new HashMap<>();
                if (playlistName == null || playlistName.trim().isEmpty()) errors.put("name", "Playlist name required.");
                if (songIdDoubles == null || songIdDoubles.isEmpty()) errors.put("songIDs", "At least one song ID required.");
                if (!errors.isEmpty()) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Validation failed", errors);
                    return;
                }
                int[] songIDs = songIdDoubles.stream().mapToInt(Double::intValue).toArray();

                if (playlistDAO.existsPlaylistByNameAndUser(playlistName, user.getId())) {
                    errors.put("name", "Playlist with this name already exists.");
                    sendError(response, HttpServletResponse.SC_CONFLICT, "Playlist creation failed", errors);
                    return;
                }
                if (!songDAO.existAllSongsByIDsAndUser(songIDs, user.getId())) {
                     errors.put("songIDs", "One or more songs are invalid or do not belong to user.");
                     sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Playlist creation failed", errors);
                     return;
                }
                
                Playlist newPlaylist = playlistDAO.createPlaylist(playlistName, songIDs, user.getId());
                if (newPlaylist != null) {
                    sendSuccess(response, newPlaylist.toJSON(), HttpServletResponse.SC_CREATED);
                } else {
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Playlist creation failed.");
                }
            } else {
                // POST /api/playlists/{id}/songs - Add songs to existing playlist
                String[] pathParts = pathInfo.split("/");
                // Expected path: /{playlistId}/songs
                if (pathParts.length == 3 && "songs".equals(pathParts[2])) {
                    try {
                        int playlistId = Integer.parseInt(pathParts[1]);
                        List<Double> songIdDoubles = (List<Double>) payload.get("songIDs");
                        if (songIdDoubles == null || songIdDoubles.isEmpty()) {
                            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Song IDs are required.");
                            return;
                        }
                        int[] songIDs = songIdDoubles.stream().mapToInt(Double::intValue).toArray();

                        // Check playlist ownership
                        if (playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId()) == null) {
                            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Playlist not found or access denied.");
                            return;
                        }
                        // Validate songs belong to user (DAO's addSongsToPlaylist should also do this)
                         if (!songDAO.existAllSongsByIDsAndUser(songIDs, user.getId())) {
                            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "One or more songs are invalid or do not belong to user.");
                            return;
                        }

                        boolean success = playlistDAO.addSongsToPlaylist(playlistId, songIDs, user.getId());
                        if (success) {
                            Playlist updatedPlaylist = playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId(), true); // Fetch updated
                            sendSuccess(response, updatedPlaylist.toJSON(), HttpServletResponse.SC_OK);
                        } else {
                            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to add songs.");
                        }
                    } catch (NumberFormatException e) {
                        sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID.");
                    }
                } else {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path for POST.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) // Reorder playlist
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }
        User user = (User) session.getAttribute("user");
        String pathInfo = request.getPathInfo();

        // Expected path: /{playlistId}/order
        if (pathInfo == null || pathInfo.isEmpty()) {
             sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Playlist ID required for reorder.");
             return;
        }
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length != 3 || !"order".equals(pathParts[2])) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path for PUT request. Expected /api/playlists/{id}/order");
            return;
        }
        
        int playlistId;
        try {
            playlistId = Integer.parseInt(pathParts[1]);
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID format.");
            return;
        }

        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Map<String, List<Double>> payload; // Expecting {"songIDs": [id1, id2, ...]}
        try {
            payload = gson.fromJson(requestBody, new TypeToken<Map<String, List<Double>>>(){}.getType());
        } catch (JsonSyntaxException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format. Expected {\"songIDs\": [...]}");
            return;
        }
        
        List<Double> songIdDoubles = payload.get("songIDs");
        if (songIdDoubles == null) { 
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "songIDs array is required in payload.");
            return;
        }
        List<Integer> songIdsInOrder = songIdDoubles.stream().mapToInt(Double::intValue).boxed().collect(Collectors.toList());

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        try {
            // Check playlist ownership (implicit in saveCustomSongOrder through getPlaylistByIdAndUser)
             if (playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId()) == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Playlist not found or access denied.");
                return;
            }
            // Validation is handled in the DAO's saveCustomSongOrder method
            playlistDAO.saveCustomSongOrder(playlistId, songIdsInOrder, user.getId());
            Playlist updatedPlaylist = playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId(), true); // Fetch updated
            sendSuccess(response, updatedPlaylist.toJSON(), HttpServletResponse.SC_OK);

        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }
        User user = (User) session.getAttribute("user");
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Playlist ID is required for deletion.");
            return;
        }

        String[] pathParts = pathInfo.split("/");
        if (pathParts.length == 2) { // e.g., /<id>
            try {
                int playlistId = Integer.parseInt(pathParts[1]);
                PlaylistDAO playlistDAO = new PlaylistDAO(connection);

                // Verify playlist belongs to user before deleting (getPlaylistByIdAndUser does this)
                if (playlistDAO.getPlaylistByIdAndUser(playlistId, user.getId()) == null) {
                     sendError(response, HttpServletResponse.SC_NOT_FOUND, "Playlist not found or access denied.");
                     return;
                }

                boolean success = playlistDAO.deletePlaylist(playlistId, user.getId());
                if (success) {
                    // Standard practice for DELETE is to return 204 No Content on success
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT); 
                } else {
                    // This might happen if the playlist was deleted between check and actual delete, or another issue.
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete playlist.");
                }
            } catch (NumberFormatException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID format.");
            } catch (SQLException e) {
                e.printStackTrace();
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
            }
        } else {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path for DELETE request.");
        }
    }

    private void sendSuccess(HttpServletResponse response, Object data, int statusCode) throws IOException {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("data", data);
        response.setStatus(statusCode);
        response.getWriter().write(gson.toJson(responseMap));
    }
    
    private void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        sendError(response, statusCode, message, null);
    }

    private void sendError(HttpServletResponse response, int statusCode, String message, Map<String, String> errors) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            errorResponse.put("errors", errors);
        }
        response.setStatus(statusCode);
        response.getWriter().write(gson.toJson(errorResponse));
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
