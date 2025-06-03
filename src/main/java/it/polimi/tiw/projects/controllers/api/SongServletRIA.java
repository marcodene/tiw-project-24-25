package it.polimi.tiw.projects.controllers.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import com.google.gson.Gson;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FileStorageManager; // Assuming this utility is appropriately configured

@WebServlet(name = "SongServletRIA", urlPatterns = {"/api/songs", "/api/songs/*"})
@MultipartConfig
public class SongServletRIA extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson = new Gson();
    private String baseStoragePath;
    private String audioFilesDir;
    private String coverImagesDir;


    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionHandler.getConnection(getServletContext());
        // Initialize FileStorageManager and get base paths
        // This might throw if properties are not found, should be handled
        try {
            FileStorageManager.initialize(getServletContext()); 
            baseStoragePath = FileStorageManager.getBaseStoragePath();
            audioFilesDir = FileStorageManager.getAudioFilesPath();
            coverImagesDir = FileStorageManager.getCoverImagesPath();
        } catch (Exception e) {
            throw new ServletException("Failed to initialize FileStorageManager", e);
        }
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
        SongDAO songDAO = new SongDAO(connection);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/songs - List all songs for the user
                List<Song> songs = songDAO.getAllSongsByUserId(user.getId());
                List<Map<String, Object>> songsJson = new ArrayList<>();
                for (Song s : songs) {
                    songsJson.add(s.toJSON());
                }
                sendSuccess(response, songsJson, HttpServletResponse.SC_OK);
            } else {
                // GET /api/songs/{id} - Get specific song
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) { // e.g., /<id>
                    try {
                        int songId = Integer.parseInt(pathParts[1]);
                        Song song = songDAO.getSongByIDAndUser(songId, user.getId());
                        if (song != null) {
                            sendSuccess(response, song.toJSON(), HttpServletResponse.SC_OK);
                        } else {
                            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Song not found or access denied.");
                        }
                    } catch (NumberFormatException e) {
                        sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid song ID format.");
                    }
                } else {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path for GET song request.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) // Upload new song
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated for song upload.");
            return;
        }
        User user = (User) session.getAttribute("user");

        // Retrieve parts from multipart request
        String title = request.getParameter("title");
        String albumName = request.getParameter("albumName");
        String artistName = request.getParameter("artistName");
        String yearStr = request.getParameter("albumReleaseYear");
        String genreName = request.getParameter("genreName"); // Client sends genre name

        Part audioFilePart = request.getPart("audioFile"); // <input type="file" name="audioFile">
        Part imageFilePart = request.getPart("imageFile"); // <input type="file" name="imageFile"> (optional)

        Map<String, String> errors = new HashMap<>();
        if (title == null || title.trim().isEmpty()) errors.put("title", "Title is required.");
        if (albumName == null || albumName.trim().isEmpty()) errors.put("albumName", "Album name is required.");
        if (artistName == null || artistName.trim().isEmpty()) errors.put("artistName", "Artist name is required.");
        if (yearStr == null || yearStr.trim().isEmpty()) errors.put("albumReleaseYear", "Release year is required.");
        if (genreName == null || genreName.trim().isEmpty()) errors.put("genreName", "Genre is required.");
        if (audioFilePart == null || audioFilePart.getSize() == 0) errors.put("audioFile", "Audio file is required.");

        int albumReleaseYear = 0;
        if (yearStr != null && !yearStr.trim().isEmpty()) {
            try {
                albumReleaseYear = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                errors.put("albumReleaseYear", "Invalid year format.");
            }
        }
        
        GenreDAO genreDAO = new GenreDAO(connection);
        int genreId = -1;
        if(genreName != null && !genreName.trim().isEmpty()){
            try {
                genreId = genreDAO.getGenreIdByName(genreName);
                if(genreId == -1) errors.put("genreName", "Genre not found: " + genreName);
            } catch (SQLException e) {
                errors.put("genreName", "Error verifying genre: " + e.getMessage());
            }
        }


        if (!errors.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Validation failed", errors);
            return;
        }

        SongDAO songDAO = new SongDAO(connection);
        try {
            if (songDAO.existsSongWithSameData(title, albumName, artistName, albumReleaseYear, genreId, user.getId())) {
                sendError(response, HttpServletResponse.SC_CONFLICT, "This song already exists in your library.");
                return;
            }

            // File saving logic
            String audioFileName = null;
            String imageFileName = null;
            String relativeAudioPath = null;
            String relativeImagePath = null;

            // Save audio file
            String originalAudioFileName = Paths.get(audioFilePart.getSubmittedFileName()).getFileName().toString();
            String audioExtension = originalAudioFileName.substring(originalAudioFileName.lastIndexOf("."));
            audioFileName = UUID.randomUUID().toString() + audioExtension;
            File audioUploadDir = new File(baseStoragePath + File.separator + audioFilesDir);
            if (!audioUploadDir.exists()) audioUploadDir.mkdirs();
            File audioFile = new File(audioUploadDir, audioFileName);
            try (InputStream input = audioFilePart.getInputStream()) {
                Files.copy(input, audioFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                relativeAudioPath = audioFilesDir + File.separator + audioFileName;
            } catch (IOException e) {
                e.printStackTrace();
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save audio file.");
                return;
            }

            // Save image file (if provided)
            if (imageFilePart != null && imageFilePart.getSize() > 0) {
                String originalImageFileName = Paths.get(imageFilePart.getSubmittedFileName()).getFileName().toString();
                String imageExtension = originalImageFileName.substring(originalImageFileName.lastIndexOf("."));
                imageFileName = UUID.randomUUID().toString() + imageExtension;
                File imageUploadDir = new File(baseStoragePath + File.separator + coverImagesDir);
                if (!imageUploadDir.exists()) imageUploadDir.mkdirs();
                File imageFile = new File(imageUploadDir, imageFileName);
                try (InputStream input = imageFilePart.getInputStream()) {
                    Files.copy(input, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    relativeImagePath = coverImagesDir + File.separator + imageFileName;
                } catch (IOException e) {
                    e.printStackTrace();
                    // If image saving fails, maybe proceed without image or send error? For now, error.
                    sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save image file.");
                    // Cleanup audio file if image saving fails?
                    if(audioFile.exists()) audioFile.delete();
                    return;
                }
            }

            Song newSong = new Song();
            newSong.setUserID(user.getId());
            newSong.setName(title);
            newSong.setAlbumName(albumName);
            newSong.setArtistName(artistName);
            newSong.setAlbumReleaseYear(albumReleaseYear);
            newSong.setGenre(genreName); // DAO will resolve genreName to ID via getGenreIdByName
            newSong.setAudioFilePath(relativeAudioPath);
            newSong.setAlbumCoverPath(relativeImagePath);

            Song createdSong = songDAO.uploadSong(newSong); // This now returns the created Song bean

            if (createdSong != null) {
                sendSuccess(response, createdSong.toJSON(), HttpServletResponse.SC_CREATED);
            } else {
                // Should not happen if DAO throws SQLException on failure
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Song creation failed in DAO.");
                 // Cleanup files if DAO failed after files were saved
                if(audioFile.exists()) audioFile.delete();
                if(relativeImagePath != null) {
                    File imageToDelete = new File(baseStoragePath + File.separator + relativeImagePath);
                    if(imageToDelete.exists()) imageToDelete.delete();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during song upload: " + e.getMessage());
        } catch (Exception e) { // Catch other potential errors like file saving issues
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
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

        if (pathInfo == null || pathInfo.split("/").length < 2) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Song ID is required for deletion.");
            return;
        }

        String[] pathParts = pathInfo.split("/"); // pathInfo starts with "/"
        try {
            int songId = Integer.parseInt(pathParts[1]);
            SongDAO songDAO = new SongDAO(connection);

            // DAO's deleteSong method already checks ownership and handles file deletion
            boolean success = songDAO.deleteSong(songId, user.getId());
            if (success) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                // This could mean song not found for user, or other issue.
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Song not found or failed to delete.");
            }
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid song ID format.");
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
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
