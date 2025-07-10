package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.thymeleaf.context.WebContext;

import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/")
public class GoToLoginPage extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    public GoToLoginPage() {
        super();
    }

    @Override
    protected boolean needsDatabase() {
        return false;
    }

    @Override
    protected boolean needsAuth() {
        return false;
    }

    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo sessione inverso utilizzando ServletBase
        if (redirectIfLogged(request, response)) {
            return;
        }
        
        // Creazione WebContext
        WebContext ctx = createContext(request, response);
        
        // FLASH MESSAGES
        setupLoginPageMessages(ctx, request);
        
        // Rendering
        String templatePath = "/WEB-INF/index.html";
        templateEngine.process(templatePath, ctx, response.getWriter());
    }
    
    /**
     * Gestisce la logica complessa dei flash messages per la pagina di login
     * Include messaggi da registrazione, logout, account deleted e errori login
     */
    private void setupLoginPageMessages(WebContext ctx, HttpServletRequest request) {
    	
        // === MESSAGGI DI SUCCESSO (es. dalla registrazione) ===
        List<String> successMessages = FlashMessagesManager.getAndClearSuccessMessages(request);
        if (successMessages != null && !successMessages.isEmpty()) {
            ctx.setVariable("successMessage", successMessages.get(0));
        }
        
        // === ACCOUNT DELETED SUCCESS MESSAGE ===
        // Caso speciale: account cancellato (sessione invalidata, usiamo parametro URL)
        String accountDeleted = request.getParameter("accountDeleted");
        if ("true".equals(accountDeleted)) {
            ctx.setVariable("successMessage", "Account deleted successfully.");
        }
        
        // === LOGOUT SUCCESS MESSAGE ===
        // Caso speciale: logout (sessione invalidata, usiamo parametro URL)
        String loggedOut = request.getParameter("loggedOut");
        if ("true".equals(loggedOut)) {
            ctx.setVariable("successMessage", "You have been logged out successfully. See you soon!");
        }
        
        // === MESSAGGI DI ERRORE LOGIN ===
        Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
        
        // Filtra errori login
        if (allFieldErrors != null) {
            for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
                String key = entry.getKey();
                
                if (!isEmpty(key) && key.startsWith("login_")) {
                	String cleanKey = key.substring(6); 
                    if ("credentialsError".equals(cleanKey) || "generalError".equals(cleanKey)) {
                        ctx.setVariable("errorMsg", entry.getValue());
                        break;                    }
                }
            }
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}