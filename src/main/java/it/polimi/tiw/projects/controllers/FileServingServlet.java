package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.io.OutputStream;
import java.util.logging.Logger;

import it.polimi.tiw.projects.utils.FileStorageManager;

// Map to both original paths for backward compatibility
@WebServlet(urlPatterns = {"/GetFile/*", "/GetImage/*", "/GetAudio/*"})
public class FileServingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(FileServingServlet.class.getName());

    public FileServingServlet() {
        super();
    }
    
    public void init() throws ServletException {
        // Initialize the file storage manager
        try {
            FileStorageManager.initialize(getServletContext());
            // Log the base storage path to verify it's correct
            System.out.println("FileServingServlet initialized with base path: " + FileStorageManager.getBaseStoragePath());
        } catch (UnavailableException e) {
            System.out.println("Failed to initialize file storage: " + e.getMessage());
            throw new ServletException("Failed to initialize file storage: " + e.getMessage(), e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Log request details
        System.out.println("File request received: " + request.getServletPath() + request.getPathInfo());
        
        // Check if user is authenticated
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") == null) {
            System.out.println("Unauthorized access attempt: " + request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized");
            return;
        }
        
        // Extract the file path from the request
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            System.out.println("Missing file path in request");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file path");
            return;
        }
        
        // Log the original path info
        System.out.println("Original path info: " + pathInfo);
        
        // Remove leading slash if present
        String relativePath = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        System.out.println("Relative path after removing leading slash: " + relativePath);
        
        // Determine if this is an image or audio request based on servlet path or file extension
        String servletPath = request.getServletPath();
        boolean isImageRequest = servletPath.equals("/GetImage") || 
                                (servletPath.equals("/GetFile") && (pathInfo.contains("/covers/") || relativePath.startsWith("covers/")));
        boolean isAudioRequest = servletPath.equals("/GetAudio") || 
                                (servletPath.equals("/GetFile") && (pathInfo.contains("/songs/") || relativePath.startsWith("songs/")));
        
        System.out.println("Request type: " + (isImageRequest ? "Image" : (isAudioRequest ? "Audio" : "Unknown")));
        
        // Handle case where the path doesn't include the subdirectory (covers/ or songs/)
        if (!relativePath.startsWith("covers/") && !relativePath.startsWith("songs/")) {
            // If path contains subdirectory with a leading slash
            if (relativePath.contains("/covers/")) {
                relativePath = relativePath.substring(relativePath.indexOf("/covers/") + 1);
                System.out.println("Extracted path after /covers/: " + relativePath);
            } else if (relativePath.contains("/songs/")) {
                relativePath = relativePath.substring(relativePath.indexOf("/songs/") + 1);
                System.out.println("Extracted path after /songs/: " + relativePath);
            } else {
                // Try to determine appropriate subdirectory
                if (isImageRequest) {
                    relativePath = "covers/" + relativePath;
                    System.out.println("Added covers/ prefix: " + relativePath);
                } else if (isAudioRequest) {
                    relativePath = "songs/" + relativePath;
                    System.out.println("Added songs/ prefix: " + relativePath);
                } else {
                    // Can't determine file type, return error
                    System.out.println("Could not determine file type for path: " + relativePath);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                                     "Could not determine file type (image or audio)");
                    return;
                }
            }
        }
        
        // Get storage root path
        String storageRoot = FileStorageManager.getBaseStoragePath();
        System.out.println("Storage root path: " + storageRoot);
        
        // Build the complete path by combining base path and relative path
        File file = new File(storageRoot, relativePath);
        String fullPath = file.getAbsolutePath();
        System.out.println("Full path constructed: " + fullPath);
        
        // Check if file exists
        if (!file.exists()) {
            System.out.println("File not found: " + fullPath);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + relativePath);
            return;
        }
        
        if (!file.isFile()) {
            System.out.println("Path exists but is not a file: " + fullPath);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Path exists but is not a file: " + relativePath);
            return;
        }
        
        // Security check - verify the file is within the uploads folder
        if (!fullPath.startsWith(storageRoot)) {
            System.out.println("Security violation - path traversal attempt: " + fullPath);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied - path traversal attempt");
            return;
        }
        
        // Determine content type
        String contentType = getServletContext().getMimeType(file.getName());
        if (contentType == null) {
            // Default to generic type based on determined file type
            contentType = isImageRequest ? "image/jpeg" : (isAudioRequest ? "audio/mpeg" : "application/octet-stream");
        }
        System.out.println("Content type determined: " + contentType);
        
        // Set response headers
        response.setContentType(contentType);
        response.setContentLength((int) file.length());
        
        // Send the file to client
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file.toPath(), out);
            System.out.println("File sent successfully: " + relativePath);
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
            throw e;
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}