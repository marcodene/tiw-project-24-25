
package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import it.polimi.tiw.projects.utils.FileStorageManager;

@WebServlet(urlPatterns = {"/GetFile/*", "/GetImage/*", "/GetAudio/*"})
public class FileServingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    public void init() throws ServletException {
        try {
            FileStorageManager.initialize(getServletContext());
        } catch (UnavailableException e) {
            throw new ServletException("Failed to initialize file storage", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized");
            return;
        }
        
        // Estrai il percorso richiesto
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file path");
            return;
        }
        
        try {
            // Ottieni il file in modo sicuro - FileStorageManager gestisce tutte le validazioni
            File file = FileStorageManager.getFileSecurely(pathInfo);
            
            // Determina il content type
            String contentType = getServletContext().getMimeType(file.getName());
            if (contentType == null) {
                // Determina in base alla directory
                if (pathInfo.contains("/covers/")) {
                    contentType = "image/jpeg";
                } else if (pathInfo.contains("/songs/")) {
                    contentType = "audio/mpeg";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            // Imposta headers di risposta
            response.setContentType(contentType);
            response.setContentLength((int) file.length());
            
            // Headers di sicurezza
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Disposition", "inline; filename=\"" + 
                             file.getName().replaceAll("[\"]", "") + "\"");
            
            // Stream del file
            Files.copy(file.toPath(), response.getOutputStream());
            
        } catch (SecurityException e) {
            // FileStorageManager ha già loggato l'evento di sicurezza internamente
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            
        } catch (FileNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            
        } catch (Exception e) {
            // Non esporre dettagli interni
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}