package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.io.OutputStream;

import it.polimi.tiw.projects.utils.FileStorageManager;

@WebServlet("/GetImage/*")
public class GetImage extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public GetImage() {
        super();
    }
    
    // Override per specificare che NON ha bisogno del database
    @Override
    protected boolean needsDatabase() {
        return false;
    }
    
    // Override per specificare che NON ha bisogno del template engine
    @Override
    protected boolean needsTemplateEngine() {
        return false;
    }
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        // Inizializza il file storage manager specifico per questa servlet
        try {
            FileStorageManager.initialize(getServletContext());
        } catch (UnavailableException e) {
            throw new ServletException("Failed to initialize file storage: " + e.getMessage(), e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione manuale (appropriato per servlet che servono risorse)
        // Non usiamo checkLogin() perché fa redirect, mentre qui serve un errore HTTP diretto
        HttpSession session = request.getSession();
        if (session.isNew() || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Non autorizzato");
            return;
        }
        
        // Estrae il percorso dell'immagine dalla richiesta
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Percorso immagine mancante");
            return;
        }
        
        // Rimuove "/" iniziale se presente
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        
        // Get storage root path
        String storageRoot = FileStorageManager.getBaseStoragePath();
        
        // Costruisce il percorso completo
        String imagePath = storageRoot + File.separator + pathInfo;
        
        // Verifica l'esistenza del file
        File imageFile = new File(imagePath);
        if (!imageFile.exists() || !imageFile.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Immagine non trovata");
            return;
        }
        
        // Verifica che il file sia all'interno della cartella uploads (sicurezza)
        if (!imagePath.startsWith(storageRoot)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
            return;
        }
        
        // Determina il content type
        String contentType = getServletContext().getMimeType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        // Imposta gli header della risposta
        response.setContentType(contentType);
        response.setContentLength((int) imageFile.length());
        
        // Invia il file al client
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(imageFile.toPath(), out);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}