package it.polimi.tiw.projects.controllers.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Year;
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
import it.polimi.tiw.projects.utils.FileStorageManager;

@WebServlet(name = "SongServletRIA", urlPatterns = {"/api/songs", "/api/songs/*"})
@MultipartConfig
public class SongServletRIA extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private Gson gson = new Gson();
    private String baseStoragePath;
    public static final int MIN_RELEASE_YEAR = 1600;
    public static final int MAX_RELEASE_YEAR = Year.now().getValue();

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        connection = ConnectionHandler.getConnection(getServletContext());
        // Initialize FileStorageManager and get base paths
        try {
            FileStorageManager.initialize(getServletContext()); 
            baseStoragePath = FileStorageManager.getBaseStoragePath();
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated for song upload.");
            return;
        }
        User user = (User) session.getAttribute("user");

        // Extract form data and validate
        Map<String, Object> formData = extractFormData(request);
        Map<String, String> errors = validateFormData(formData);
        
        // Get and validate files
        Part audioFilePart = request.getPart("audioFile");
        Part imageFilePart = request.getPart("imageFile");
        
        // Validate files
        validateFiles(audioFilePart, imageFilePart, errors);
        
        if (!errors.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Validation failed", errors);
            return;
        }

        // Validate genre exists
        try {
            validateGenre(formData, errors);
            if (!errors.isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Validation failed", errors);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error validating genre: " + e.getMessage());
            return;
        }

        // Check if song already exists
        try {
            if (songExists(formData, user.getId())) {
                sendError(response, HttpServletResponse.SC_CONFLICT, "This song already exists in your library.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error checking song existence: " + e.getMessage());
            return;
        }

        // Upload files and get paths
        Map<String, String> filePaths = null;
        try {
            filePaths = uploadFiles(audioFilePart, imageFilePart);
        } catch (IOException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error uploading files: " + e.getMessage());
            return;
        }

        // Create and save the song
        try {
            Song createdSong = createAndSaveSong(formData, filePaths, user.getId());
            if (createdSong != null) {
                sendSuccess(response, createdSong.toJSON(), HttpServletResponse.SC_CREATED);
            } else {
                // Clean up files since database operation failed
                deleteUploadedFiles(filePaths.get("imagePath"), filePaths.get("audioPath"));
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Song creation failed in database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Clean up files since database operation failed
            deleteUploadedFiles(filePaths.get("imagePath"), filePaths.get("audioPath"));
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during song upload: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            // Clean up files since an error occurred
            deleteUploadedFiles(filePaths.get("imagePath"), filePaths.get("audioPath"));
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Extracts form data from the request
     */
    private Map<String, Object> extractFormData(HttpServletRequest request) {
        Map<String, Object> formData = new HashMap<>();
        formData.put("title", request.getParameter("title"));
        formData.put("albumName", request.getParameter("albumName"));
        formData.put("artistName", request.getParameter("artistName"));
        formData.put("albumReleaseYear", request.getParameter("albumReleaseYear"));
        formData.put("genreName", request.getParameter("genreName"));
        return formData;
    }

    /**
     * Validates form data and returns any errors
     */
    private Map<String, String> validateFormData(Map<String, Object> formData) {
        Map<String, String> errors = new HashMap<>();
        
        // Validate required text fields
        String title = (String) formData.get("title");
        String albumName = (String) formData.get("albumName");
        String artistName = (String) formData.get("artistName");
        String yearStr = (String) formData.get("albumReleaseYear");
        String genreName = (String) formData.get("genreName");
        
        if (title == null || title.trim().isEmpty()) 
            errors.put("title", "Title is required.");
        if (albumName == null || albumName.trim().isEmpty()) 
            errors.put("albumName", "Album name is required.");
        if (artistName == null || artistName.trim().isEmpty()) 
            errors.put("artistName", "Artist name is required.");
        if (yearStr == null || yearStr.trim().isEmpty()) 
            errors.put("albumReleaseYear", "Release year is required.");
        if (genreName == null || genreName.trim().isEmpty()) 
            errors.put("genreName", "Genre is required.");
        
        // Validate year format and range
        if (yearStr != null && !yearStr.trim().isEmpty()) {
            try {
                int albumReleaseYear = Integer.parseInt(yearStr);
                formData.put("albumReleaseYearInt", albumReleaseYear); // Store parsed int for later use
                
                if (albumReleaseYear < MIN_RELEASE_YEAR || albumReleaseYear > MAX_RELEASE_YEAR) {
                    errors.put("albumReleaseYear", "Year must be between " + MIN_RELEASE_YEAR + " and " + MAX_RELEASE_YEAR);
                }
            } catch (NumberFormatException e) {
                errors.put("albumReleaseYear", "Invalid year format.");
            }
        }
        
        return errors;
    }

    /**
     * Validates uploaded files and adds any errors to the provided map
     */
    private void validateFiles(Part audioFilePart, Part imageFilePart, Map<String, String> errors) {
        // Validate audio file
        if (audioFilePart == null || audioFilePart.getSize() == 0) {
            errors.put("audioFile", "Audio file is required.");
        } else if (audioFilePart.getSize() > 10 * 1024 * 1024) { // 10MB limit
            errors.put("audioFile", "Audio file must be smaller than 10MB.");
        } else if (!isValidAudioFile(audioFilePart)) {
            errors.put("audioFile", "Invalid audio file format. Only MP3, WAV, OGG, and M4A files are allowed.");
        }

        // Validate image file (optional)
        if (imageFilePart != null && imageFilePart.getSize() > 0) {
            if (imageFilePart.getSize() > 5 * 1024 * 1024) { // 5MB limit
                errors.put("imageFile", "Image file must be smaller than 5MB.");
            } else if (!isValidImageFile(imageFilePart)) {
                errors.put("imageFile", "Invalid image file format. Only JPG, PNG, and GIF files are allowed.");
            }
        }
    }

    /**
     * Validates that the genre exists in the database
     */
    private void validateGenre(Map<String, Object> formData, Map<String, String> errors) throws SQLException {
        String genreName = (String) formData.get("genreName");
        if (genreName != null && !genreName.trim().isEmpty()) {
            GenreDAO genreDAO = new GenreDAO(connection);
            int genreId = genreDAO.getGenreIdByName(genreName);
            if (genreId == -1) {
                errors.put("genreName", "Genre not found: " + genreName);
            } else {
                formData.put("genreId", genreId); // Store for later use
            }
        }
    }

    /**
     * Checks if a song with the same data already exists
     */
    private boolean songExists(Map<String, Object> formData, int userId) throws SQLException {
        SongDAO songDAO = new SongDAO(connection);
        String title = (String) formData.get("title");
        String albumName = (String) formData.get("albumName");
        String artistName = (String) formData.get("artistName");
        int albumReleaseYear = (int) formData.get("albumReleaseYearInt");
        int genreId = (int) formData.get("genreId");
        
        return songDAO.existsSongWithSameData(title, albumName, artistName, albumReleaseYear, genreId, userId);
    }

    /**
     * Uploads files and returns their relative paths
     */
    private Map<String, String> uploadFiles(Part audioFilePart, Part imageFilePart) throws IOException {
        Map<String, String> filePaths = new HashMap<>();
        
        // Save audio file
        String audioFileName = getUniqueFileName(audioFilePart.getSubmittedFileName());
        String audioFilesDir = FileStorageManager.getAudioFilesPath(); // Full path for saving
        File audioUploadDir = new File(audioFilesDir);
        if (!audioUploadDir.exists()) audioUploadDir.mkdirs();
        File audioFile = new File(audioUploadDir, audioFileName);
        
        try (InputStream input = audioFilePart.getInputStream()) {
            Files.copy(input, audioFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // Store ONLY the relative path
            String relativeAudioPath = "/songs/" + audioFileName;
            filePaths.put("audioPath", relativeAudioPath);
        }

        // Save image file (if not provided usa a default cover)
    	String imageFileName;
    	if(imageFilePart != null && imageFilePart.getSize() > 0)
    		imageFileName = getUniqueFileName(imageFilePart.getSubmittedFileName());
    	else
    		imageFileName = "default.jpg";
        String coverImagesDir = FileStorageManager.getCoverImagesPath(); // Full path for saving
        File imageUploadDir = new File(coverImagesDir);
        if (!imageUploadDir.exists()) imageUploadDir.mkdirs();
        File imageFile = new File(imageUploadDir, imageFileName);
        
        try (InputStream input = imageFilePart.getInputStream()) {
            Files.copy(input, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // Store ONLY the relative path
            String relativeImagePath = "/covers/" + imageFileName;
            filePaths.put("imagePath", relativeImagePath);
        } catch (IOException e) {
            // If image saving fails, clean up audio file and throw exception
            if (audioFile.exists()) audioFile.delete();
            throw e;
        }
    
        return filePaths;
    }

    /**
     * Creates a unique filename for uploaded files
     */
    private String getUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Creates and saves a song to the database
     */
    private Song createAndSaveSong(Map<String, Object> formData, Map<String, String> filePaths, int userId) throws SQLException {
        Song newSong = new Song();
        newSong.setUserID(userId);
        newSong.setName((String) formData.get("title"));
        newSong.setAlbumName((String) formData.get("albumName"));
        newSong.setArtistName((String) formData.get("artistName"));
        newSong.setAlbumReleaseYear((int) formData.get("albumReleaseYearInt"));
        newSong.setGenre((String) formData.get("genreName"));
        newSong.setAudioFilePath(filePaths.get("audioPath"));
        newSong.setAlbumCoverPath(filePaths.get("imagePath"));

        SongDAO songDAO = new SongDAO(connection);
        return songDAO.uploadSong(newSong);
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
    
    // Verify if the file is a valid image file
    private boolean isValidImageFile(Part filePart) {
        String fileName = filePart.getSubmittedFileName().toLowerCase();
        String contentType = filePart.getContentType();
        
        // Check file extension
        boolean validExtension = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                                 fileName.endsWith(".png") || fileName.endsWith(".gif");
        
        // Check MIME type
        boolean validMimeType = contentType != null && 
                               (contentType.equals("image/jpeg") || 
                                contentType.equals("image/png") || 
                                contentType.equals("image/gif"));
        
        return validExtension && validMimeType;
    }
    
    // Verify if the file is a valid audio file
    private boolean isValidAudioFile(Part filePart) {
        String fileName = filePart.getSubmittedFileName().toLowerCase();
        String contentType = filePart.getContentType();
        
        // Check file extension
        boolean validExtension = fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
                                 fileName.endsWith(".ogg") || fileName.endsWith(".m4a");
        
        // Check MIME type
        boolean validMimeType = contentType != null && 
                               (contentType.startsWith("audio/") || 
                                contentType.equals("application/ogg"));
        
        return validExtension && validMimeType;
    }
    
    // Helper method to delete uploaded files in case of errors
    private void deleteUploadedFiles(String coverPath, String audioPath) {
        if (coverPath != null) {
            try {
                // Remove leading slash if present for correct path construction
                String relativePath = coverPath.startsWith("/") ? coverPath.substring(1) : coverPath;
                File coverFile = new File(FileStorageManager.getBaseStoragePath(), relativePath);
                if (coverFile.exists()) {
                    coverFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (audioPath != null) {
            try {
                // Remove leading slash if present for correct path construction
                String relativePath = audioPath.startsWith("/") ? audioPath.substring(1) : audioPath;
                File audioFile = new File(FileStorageManager.getBaseStoragePath(), relativePath);
                if (audioFile.exists()) {
                    audioFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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