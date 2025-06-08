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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import it.polimi.tiw.projects.utils.FileStorageManager;

@WebServlet(urlPatterns = {"/GetFile/*", "/GetImage/*", "/GetAudio/*"})
public class FileServingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // Whitelist of allowed subdirectories
    // Only files in these directories can be served to prevent access to system files
    private static final List<String> ALLOWED_DIRS = Arrays.asList("covers", "songs");
    
    // Whitelist of allowed file extensions
    // Prevents serving of potentially dangerous files like .jsp, .class, .properties
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "mp3", "wav", "ogg", "m4a"
    );
    
    public void init() throws ServletException {
        try {
            FileStorageManager.initialize(getServletContext());
        } catch (UnavailableException e) {
            throw new ServletException("Failed to initialize file storage", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Authentication
        // Ensure only logged-in users can access files
        // This prevents unauthorized access to uploaded content
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized");
            return;
        }
        
        // Extract the requested file path from URL
        // Example: /GetFile/covers/image.jpg -> pathInfo = "/covers/image.jpg"
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file path");
            return;
        }
        
        // Remove leading slash for easier processing
        // "/covers/image.jpg" becomes "covers/image.jpg"
        String requestedPath = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        
        // Block dangerous patterns
        // ".." allows directory traversal (e.g., "../../../etc/passwd")
        // "\" can be used for path traversal on Windows systems
        if (requestedPath.contains("..") || requestedPath.contains("\\")) {
            // Log this as potential attack attempt
            System.err.println("Path traversal attempt blocked: " + requestedPath + 
                             " from IP: " + request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid path");
            return;
        }
        
        // Validate path structure
        // Path must be exactly "directory/filename" format
        // This prevents complex paths like "covers/subfolder/../../secret.txt"
        String[] parts = requestedPath.split("/");
        if (parts.length != 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path structure");
            return;
        }
        
        // Validate directory is in whitelist
        // Only allow access to files in "covers" or "songs" directories
        String directory = parts[0];
        if (!ALLOWED_DIRS.contains(directory)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to this directory is not allowed");
            return;
        }
        
        // Validate filename has proper extension
        String filename = parts[1];
        int dotIndex = filename.lastIndexOf(".");
        
        // Ensure filename has an extension and doesn't start/end with dot
        // This blocks hidden files (.htaccess) and files without extensions
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename");
            return;
        }
        
        // Validate file extension is allowed
        // Only serve known safe file types to prevent serving config files, scripts, etc.
        String extension = filename.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "File type not allowed");
            return;
        }
        
        // Use Java NIO Path API for safe path construction
        // This API properly handles path normalization and prevents injection attacks
        try {
            // Get the absolute, normalized base path
            // normalize() removes any . or .. segments
            Path basePath = Paths.get(FileStorageManager.getBaseStoragePath())
                                 .toAbsolutePath()
                                 .normalize();
            
            // Safely resolve the requested file path
            // resolve() safely combines paths without string concatenation vulnerabilities
            Path filePath = basePath.resolve(requestedPath).normalize();
            
            // Ensure resolved path is within base directory
            // This is the most important check - it ensures that after all path resolution,
            // the final path is still within our storage directory
            // This prevents attacks like: "covers/../../../etc/passwd"
            if (!filePath.startsWith(basePath)) {
                System.err.println("Path escape attempt! Requested: " + requestedPath + 
                                 ", Resolved to: " + filePath);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            
            // Verify the file exists and is actually a file (not a directory)
            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            // Determine appropriate content type based on file
            // First try to get it from servlet context, then use defaults
            String contentType = getServletContext().getMimeType(filename);
            if (contentType == null) {
                // Default content types based on directory
                contentType = directory.equals("covers") ? "image/jpeg" : "audio/mpeg";
            }
            
            // Set response headers
            response.setContentType(contentType);
            response.setContentLength((int) file.length());
            
            // SECURITY HEADER: Prevent browser from MIME-sniffing
            // This stops browsers from executing files as different types than declared
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // SECURITY HEADER: Control how file is displayed
            // "inline" allows browser to display, but filename parameter prevents XSS in filename
            response.setHeader("Content-Disposition", "inline; filename=\"" + 
                             filename.replaceAll("[\"]", "") + "\"");
            
            // Stream the file content to the response
            // Using Files.copy is safe and efficient
            Files.copy(filePath, response.getOutputStream());
            
        } catch (Exception e) {
            // Don't expose internal error details to potential attackers
            System.err.println("Error serving file: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}