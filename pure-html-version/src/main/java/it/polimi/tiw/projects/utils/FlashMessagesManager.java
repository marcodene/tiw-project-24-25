package it.polimi.tiw.projects.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FlashMessagesManager - Gestisce i messaggi temporanei per il pattern Post-Redirect-Get
 * 
 * Questo manager permette di memorizzare messaggi nella sessione che vengono automaticamente
 * rimossi dopo la prima lettura, implementando così il meccanismo dei "flash messages"
 * tipico del pattern PRG.
 */
public class FlashMessagesManager {
    
    // Chiavi per i diversi tipi di messaggi nella sessione
    private static final String SUCCESS_MESSAGES_KEY = "flash_success_messages";
    private static final String ERROR_MESSAGES_KEY = "flash_error_messages";
    private static final String INFO_MESSAGES_KEY = "flash_info_messages";
    
    // Chiavi per errori strutturati e valori form (per mantenere UX originale)
    private static final String FIELD_ERRORS_KEY = "flash_field_errors";
    private static final String FORM_VALUES_KEY = "flash_form_values";
    
    /**
     * Aggiunge un messaggio di successo ai flash messages
     * @param request la HttpServletRequest per accedere alla sessione
     * @param message il messaggio da aggiungere
     */
    public static void addSuccessMessage(HttpServletRequest request, String message) {
        addMessage(request, SUCCESS_MESSAGES_KEY, message);
    }
    
    /**
     * Aggiunge un messaggio di errore ai flash messages
     * @param request la HttpServletRequest per accedere alla sessione
     * @param message il messaggio da aggiungere
     */
    public static void addErrorMessage(HttpServletRequest request, String message) {
        addMessage(request, ERROR_MESSAGES_KEY, message);
    }
    
    /**
     * Aggiunge un messaggio informativo ai flash messages
     * @param request la HttpServletRequest per accedere alla sessione
     * @param message il messaggio da aggiungere
     */
    public static void addInfoMessage(HttpServletRequest request, String message) {
        addMessage(request, INFO_MESSAGES_KEY, message);
    }
    
    /**
     * Metodo privato per aggiungere un messaggio alla sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @param key la chiave nella sessione dove memorizzare il messaggio
     * @param message il messaggio da aggiungere
     */
    @SuppressWarnings("unchecked")
    private static void addMessage(HttpServletRequest request, String key, String message) {
        HttpSession session = request.getSession();
        List<String> messages = (List<String>) session.getAttribute(key);
        
        // Se non esistono messaggi per questa chiave, crea una nuova lista
        if (messages == null) {
            messages = new ArrayList<>();
            session.setAttribute(key, messages);
        }
        
        messages.add(message);
    }
    
    /**
     * Recupera e rimuove tutti i messaggi di successo dalla sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return lista dei messaggi di successo (vuota se non ce ne sono)
     */
    public static List<String> getAndClearSuccessMessages(HttpServletRequest request) {
        return getAndClearMessages(request, SUCCESS_MESSAGES_KEY);
    }
    
    /**
     * Recupera e rimuove tutti i messaggi di errore dalla sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return lista dei messaggi di errore (vuota se non ce ne sono)
     */
    public static List<String> getAndClearErrorMessages(HttpServletRequest request) {
        return getAndClearMessages(request, ERROR_MESSAGES_KEY);
    }
    
    /**
     * Recupera e rimuove tutti i messaggi informativi dalla sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return lista dei messaggi informativi (vuota se non ce ne sono)
     */
    public static List<String> getAndClearInfoMessages(HttpServletRequest request) {
        return getAndClearMessages(request, INFO_MESSAGES_KEY);
    }
    
    /**
     * Metodo privato per recuperare e rimuovere messaggi dalla sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @param key la chiave nella sessione da cui recuperare i messaggi
     * @return lista dei messaggi (vuota se non ce ne sono)
     */
    @SuppressWarnings("unchecked")
    private static List<String> getAndClearMessages(HttpServletRequest request, String key) {
        HttpSession session = request.getSession();
        List<String> messages = (List<String>) session.getAttribute(key);
        
        // Rimuove immediatamente i messaggi dalla sessione (comportamento "flash")
        session.removeAttribute(key);
        
        // Restituisce i messaggi trovati o una lista vuota
        return messages != null ? messages : new ArrayList<>();
    }
    
    /**
     * Verifica se ci sono messaggi di successo nella sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return true se ci sono messaggi di successo, false altrimenti
     */
    public static boolean hasSuccessMessages(HttpServletRequest request) {
        return hasMessages(request, SUCCESS_MESSAGES_KEY);
    }
    
    /**
     * Verifica se ci sono messaggi di errore nella sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return true se ci sono messaggi di errore, false altrimenti
     */
    public static boolean hasErrorMessages(HttpServletRequest request) {
        return hasMessages(request, ERROR_MESSAGES_KEY);
    }
    
    /**
     * Verifica se ci sono messaggi informativi nella sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return true se ci sono messaggi informativi, false altrimenti
     */
    public static boolean hasInfoMessages(HttpServletRequest request) {
        return hasMessages(request, INFO_MESSAGES_KEY);
    }
    
    /**
     * Metodo privato per verificare la presenza di messaggi nella sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @param key la chiave nella sessione da verificare
     * @return true se ci sono messaggi, false altrimenti
     */
    @SuppressWarnings("unchecked")
    private static boolean hasMessages(HttpServletRequest request, String key) {
        HttpSession session = request.getSession();
        List<String> messages = (List<String>) session.getAttribute(key);
        return messages != null && !messages.isEmpty();
    }
    
    /**
     * Rimuove tutti i messaggi flash dalla sessione
     * Utile per situazioni di cleanup o logout
     * @param request la HttpServletRequest per accedere alla sessione
     */
    public static void clearAllMessages(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.removeAttribute(SUCCESS_MESSAGES_KEY);
        session.removeAttribute(ERROR_MESSAGES_KEY);
        session.removeAttribute(INFO_MESSAGES_KEY);
        session.removeAttribute(FIELD_ERRORS_KEY);
        session.removeAttribute(FORM_VALUES_KEY);
    }
    
    // === GESTIONE ERRORI STRUTTURATI PER CAMPO ===
    
    /**
     * Aggiunge una mappa di errori specifici per campo ai flash messages
     * @param request la HttpServletRequest per accedere alla sessione
     * @param fieldErrors mappa con errori specifici per ogni campo del form
     */
    @SuppressWarnings("unchecked")
    public static void addFieldErrors(HttpServletRequest request, Map<String, String> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return;
        }
        
        HttpSession session = request.getSession();
        Map<String, String> existingErrors = (Map<String, String>) session.getAttribute(FIELD_ERRORS_KEY);
        
        if (existingErrors == null) {
            existingErrors = new HashMap<>();
            session.setAttribute(FIELD_ERRORS_KEY, existingErrors);
        }
        
        existingErrors.putAll(fieldErrors);
    }
    
    /**
     * Recupera e rimuove tutti gli errori specifici per campo dalla sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return mappa degli errori per campo (vuota se non ce ne sono)
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getAndClearFieldErrors(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, String> fieldErrors = (Map<String, String>) session.getAttribute(FIELD_ERRORS_KEY);
        
        // Rimuove immediatamente gli errori dalla sessione (comportamento "flash")
        session.removeAttribute(FIELD_ERRORS_KEY);
        
        // Restituisce gli errori trovati o una mappa vuota
        return fieldErrors != null ? fieldErrors : new HashMap<>();
    }
    
    // === GESTIONE VALORI DEL FORM ===
    
    /**
     * Aggiunge una mappa di valori del form ai flash messages
     * @param request la HttpServletRequest per accedere alla sessione
     * @param formValues mappa con i valori di ogni campo del form
     */
    @SuppressWarnings("unchecked")
    public static void addFormValues(HttpServletRequest request, Map<String, String> formValues) {
        if (formValues == null || formValues.isEmpty()) {
            return;
        }
        
        HttpSession session = request.getSession();
        Map<String, String> existingValues = (Map<String, String>) session.getAttribute(FORM_VALUES_KEY);
        
        if (existingValues == null) {
            existingValues = new HashMap<>();
            session.setAttribute(FORM_VALUES_KEY, existingValues);
        }
        
        existingValues.putAll(formValues);
    }
    
    /**
     * Recupera e rimuove tutti i valori del form dalla sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return mappa dei valori del form (vuota se non ce ne sono)
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getAndClearFormValues(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, String> formValues = (Map<String, String>) session.getAttribute(FORM_VALUES_KEY);
        
        // Rimuove immediatamente i valori dalla sessione (comportamento "flash")
        session.removeAttribute(FORM_VALUES_KEY);
        
        // Restituisce i valori trovati o una mappa vuota
        return formValues != null ? formValues : new HashMap<>();
    }
    
    /**
     * Verifica se ci sono errori specifici per campo nella sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return true se ci sono errori per campo, false altrimenti
     */
    @SuppressWarnings("unchecked")
    public static boolean hasFieldErrors(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, String> fieldErrors = (Map<String, String>) session.getAttribute(FIELD_ERRORS_KEY);
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
    
    /**
     * Verifica se ci sono valori del form nella sessione
     * @param request la HttpServletRequest per accedere alla sessione
     * @return true se ci sono valori del form, false altrimenti
     */
    @SuppressWarnings("unchecked")
    public static boolean hasFormValues(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, String> formValues = (Map<String, String>) session.getAttribute(FORM_VALUES_KEY);
        return formValues != null && !formValues.isEmpty();
    }
}