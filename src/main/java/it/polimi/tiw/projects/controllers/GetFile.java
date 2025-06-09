package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import it.polimi.tiw.projects.utils.FileStorageManager;

/**
 * Servlet unificata per servire sia audio che immagini in modo sicuro
 * Sostituisce GetAudio e GetImage con un'implementazione centralizzata
 */
@WebServlet(urlPatterns = {"/GetFile/*", "/GetAudio/*", "/GetImage/*"})
public class GetFile extends ServletBase {
    private static final long serialVersionUID = 1L;

    public GetFile() {
        super();
    }
    
    @Override
    protected boolean needsDatabase() {
        return false;
    }
    
    @Override
    protected boolean needsTemplateEngine() {
        return false;
    }
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            FileStorageManager.initialize(getServletContext());
        } catch (UnavailableException e) {
            throw new ServletException("Failed to initialize file storage", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // ===== CONTROLLO AUTENTICAZIONE =====
        // Non usiamo checkLogin() perché fa redirect, mentre qui serve un errore HTTP
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized");
            return;
        }
        
        // Estrai il percorso del file
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file path");
            return;
        }
        
        // ===== USA FileStorageManager PER SICUREZZA =====
        try {
            // Ottieni il file in modo sicuro
            File file = FileStorageManager.getFileSecurely(pathInfo);
            
            // Determina il content type
            String contentType = getServletContext().getMimeType(file.getName());
            if (contentType == null) {
                // Determina in base al percorso o estensione
                String lowerPath = pathInfo.toLowerCase();
                if (lowerPath.contains("/covers/") || 
                    lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || 
                    lowerPath.endsWith(".png") || lowerPath.endsWith(".gif")) {
                    contentType = "image/jpeg";
                } else if (lowerPath.contains("/songs/") || 
                           lowerPath.endsWith(".mp3") || lowerPath.endsWith(".wav") || 
                           lowerPath.endsWith(".ogg") || lowerPath.endsWith(".m4a")) {
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        } catch (FileNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serving file");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}

// ===== VANTAGGI DI QUESTA IMPLEMENTAZIONE =====
// 1. UNA SOLA SERVLET invece di due (GetAudio + GetImage)
// 2. Sicurezza centralizzata in FileStorageManager
// 3. Codice più pulito e manutenibile
// 4. Supporta tutti i percorsi legacy (/GetAudio/*, /GetImage/*, /GetFile/*)