package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet per servire risorse statiche come CSS e JavaScript
 */
@WebServlet("/css/*")
public class CssServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String CSS_MIME_TYPE = "text/css";
    private static final String BASE_DIR = "style"; // Directory che contiene i file CSS
    
    public CssServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();
        
        
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not specified");
            return;
        }
        
        // Costruisci il percorso completo
        String realPath = getServletContext().getRealPath(BASE_DIR + "/" + pathInfo);
        
        // Verifica l'esistenza del file
        File file = new File(realPath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File CSS not found: " + realPath);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            return;
        }
        
        // Controllo di sicurezza: verifica che il file sia all'interno della directory di base
        if (!realPath.startsWith(getServletContext().getRealPath("/" + BASE_DIR))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        // Verifica che l'estensione del file sia .css
        if (!pathInfo.toLowerCase().endsWith(".css")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only CSS file are allowed");
            return;
        }
        
        
        // Imposta il content type e la lunghezza
        response.setContentType(CSS_MIME_TYPE);
        response.setContentLength((int) file.length());
        
        // Invia il file al client
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file.toPath(), out);
        }
	}
	
	/**
     * Estrae l'estensione del file dal nome del file
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
