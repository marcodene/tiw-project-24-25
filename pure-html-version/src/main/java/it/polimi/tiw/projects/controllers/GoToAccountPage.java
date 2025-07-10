package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.context.WebContext;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/Account")
public class GoToAccountPage extends ServletBase {
    private static final long serialVersionUID = 1L;
       
    public GoToAccountPage() {
        super();
    }
    
    @Override
    protected boolean needsDatabase() {
        return false;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione utilizzando ServletBase
        User user = checkLogin(request, response);
        if (user == null) {
            return;
        }
        
        // Creazione WebContext 
        WebContext ctx = createContext(request, response);
        
        // PATTERN POST-REDIRECT-GET: 
        // Recupera e rimuove gli errori strutturati per campo
        Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
        
        // Filtra errori account (con prefisso "account_")
        Map<String, String> accountFieldErrors = new HashMap<>();
        for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
            if (entry.getKey().startsWith("account_")) {
                // Rimuovi prefisso "account_" per compatibilità template
                String cleanKey = entry.getKey().substring(8); // rimuove "account_"
                accountFieldErrors.put(cleanKey, entry.getValue());
            }
        }
        
        // Imposta variabili per template
        if (!accountFieldErrors.isEmpty()) {
            ctx.setVariable("errorMessages", accountFieldErrors);
        }
        
        // Rendering 
        String templatePath = "/WEB-INF/AccountPage.html";
        templateEngine.process(templatePath, ctx, response.getWriter());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}