package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.context.WebContext;

import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/GoToRegisterPage")
public class GoToRegisterPage extends ServletBase {
    private static final long serialVersionUID = 1L;
       
    public GoToRegisterPage() {
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
        // Controllo sessione inverso
        if (redirectIfLogged(request, response)) {
            return; 
        }
    
        // Creazione WebContext utilizzando ServletBase
        WebContext ctx = createContext(request, response);
        
        // PATTERN POST-REDIRECT-GET
        setupRegisterPageMessages(ctx, request);
        
        // Check if coming from login page with error
        String loginError = request.getParameter("loginError");
        boolean hasLoginError = Boolean.parseBoolean(loginError); 
        ctx.setVariable("showRegisterButton", hasLoginError);
        
        // Rendering utilizzando il template engine della ServletBase
        String templatePath = "/WEB-INF/RegisterPage.html";
        templateEngine.process(templatePath, ctx, response.getWriter());
    }
    
    /**
     * Gestisce la logica complessa dei flash messages
     */
    private void setupRegisterPageMessages(WebContext ctx, HttpServletRequest request) {
     
        // === REGISTER FLASH MESSAGES (SOLO ERRORI) ===
        // Recupera e rimuove gli errori e valori strutturati dalla sessione
        Map<String, String> allFieldErrors = FlashMessagesManager.getAndClearFieldErrors(request);
        Map<String, String> allFormValues = FlashMessagesManager.getAndClearFormValues(request);
        
        // Filtra errori e valori per register
        Map<String, String> registerFieldErrors = new HashMap<>();
        Map<String, String> registerFormValues = new HashMap<>();

        if (allFieldErrors != null) {
            for (Map.Entry<String, String> entry : allFieldErrors.entrySet()) {
                String key = entry.getKey();

                if (!isEmpty(key) && key.startsWith("register_")) {
                	String cleanKey = key.substring(9);
                    registerFieldErrors.put(cleanKey, entry.getValue());
                }
            }
        }

        if (allFormValues != null) {
            for (Map.Entry<String, String> entry : allFormValues.entrySet()) {
                String key = entry.getKey();
                
                if (!isEmpty(key) && key.startsWith("register_")) {
                	String cleanKey = key.substring(9);
                    registerFormValues.put(cleanKey, entry.getValue());
                }
            }
        }

        if (!registerFieldErrors.isEmpty()) {
            ctx.setVariable("errorMessages", registerFieldErrors);
        }
        if (!registerFormValues.isEmpty()) {
            ctx.setVariable("formValues", registerFormValues);
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}