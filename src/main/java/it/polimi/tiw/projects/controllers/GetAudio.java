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

import it.polimi.tiw.projects.utils.FileStorageManager;

@WebServlet("/GetAudio/*")
public class GetAudio extends HttpServlet {
	private static final long serialVersionUID = 1L;

    public GetAudio() {
        super();
    }
    
    public void init() throws ServletException {
        // Inizializza il file storage manager
        try {
            FileStorageManager.initialize(getServletContext());
        } catch (UnavailableException e) {
            throw new ServletException("Failed to initialize file storage: " + e.getMessage(), e);
        }
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Check if user is authenticated
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized");
            return;
        }
        
        // Extract the audio file path from the request
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing audio file path");
            return;
        }
        
        // Remove leading "/" if present
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        
        // Get storage root path
        String storageRoot = FileStorageManager.getBaseStoragePath();
        
        // Build the complete path
        String audioPath = storageRoot + File.separator + pathInfo;
        
        // Check if file exists
        File audioFile = new File(audioPath);
        if (!audioFile.exists() || !audioFile.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Audio file not found");
            return;
        }
        
        // Security check - verify the file is within the uploads folder
        if (!audioPath.startsWith(storageRoot)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        // Determine content type
        String contentType = getServletContext().getMimeType(audioPath);
        if (contentType == null) {
            // Default to generic audio type if MIME type can't be determined
            contentType = "audio/mpeg";
        }
        
        // Set response headers
        response.setContentType(contentType);
        response.setContentLength((int) audioFile.length());
        
        // Send the file to client
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(audioFile.toPath(), out);
        }
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
