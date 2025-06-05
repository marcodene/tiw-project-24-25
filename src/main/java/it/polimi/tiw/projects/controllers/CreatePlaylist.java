package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FlashMessagesManager;

@WebServlet("/CreatePlaylist")
public class CreatePlaylist extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
     
    public CreatePlaylist() {
        super();
    }
    
    public void init() throws ServletException {
    	connection = ConnectionHandler.getConnection(getServletContext());
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// if the user is not logged in (not present in session) redirect to the login
		HttpSession session = request.getSession();
		if(session.isNew() || session.getAttribute("user")==null) {
			String loginPagePath = getServletContext().getContextPath() + "/";
			response.sendRedirect(loginPagePath);
			return;
		}
		
		// Crea una mappa per i messaggi di errore
		Map<String, String> errorMessages = new HashMap<>();
		String successMessage = null;
		boolean hasErrors = false;
		
		String playlistName = null;
		
		int[] selectedSongIDs = null;
		String[] selectedSongIDStrings = request.getParameterValues("selectedSongs");

		if (selectedSongIDStrings != null) {
		    selectedSongIDs = new int[selectedSongIDStrings.length];
		    try {
		        for (int i = 0; i < selectedSongIDStrings.length; i++) {
		            selectedSongIDs[i] = Integer.parseInt(selectedSongIDStrings[i]);
		        }
		    } catch (NumberFormatException e) {
		        errorMessages.put("playlist_songsError", "ID delle canzoni non validi");
		        hasErrors = true;
		    }
		}
		
		User user = (User) session.getAttribute("user");
		SongDAO songDAO = new SongDAO(connection);
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		
		try {
			playlistName = StringEscapeUtils.escapeJava(request.getParameter("playlistName"));
			
			if (playlistName == null || playlistName.isEmpty()) {
			    errorMessages.put("playlist_nameError", "Il nome della playlist è obbligatorio");
			    hasErrors = true;
			}
			
			if (selectedSongIDs == null || selectedSongIDs.length == 0) {
			    errorMessages.put("playlist_songsError", "Devi selezionare almeno una canzone");
			    hasErrors = true;
			}
			
			if(containsDuplicates(selectedSongIDs)) {
				errorMessages.put("playlist_songsError", "La playlist non può contenere canzoni duplicate");
				hasErrors = true;
			}
			
			try {
				if(!hasErrors && !songDAO.existAllSongsByIDsAndUser(selectedSongIDs, user.getId())) {
				    errorMessages.put("playlist_songsError", "Tutte le canzoni devono essere già state caricate");
				    hasErrors = true;
			    }
			
			    // Verifica se esiste già una playlist con questo nome per l'utente
			    if(!hasErrors && playlistDAO.existsPlaylistByNameAndUser(playlistName, user.getId())) {
				    errorMessages.put("playlist_nameError", "Esiste già una playlist con questo nome");
				    hasErrors = true;
			    }
			    
			    // Se non ci sono errori, procedi con la creazione
			    if(!hasErrors) {
			    	boolean success = playlistDAO.createPlaylist(playlistName, selectedSongIDs, user.getId());
    			
    			    if (!success) {
        	            errorMessages.put("playlist_generalError", "La playlist non è stata creata. Verifica che i valori siano corretti.");
        	            hasErrors = true;
        	        } else {
        	            // Creazione completata con successo
        	            successMessage = "Playlist '" + playlistName + "' creata con successo!";
        	        }
			    }
			} catch (SQLException e) {
				errorMessages.put("playlist_generalError", "Errore del database: " + e.getMessage());
				hasErrors = true;
				e.printStackTrace();
			}
			
		} catch (Exception e) {
			errorMessages.put("playlist_generalError", "Errore durante l'elaborazione dei dati: " + e.getMessage());
			hasErrors = true;
			e.printStackTrace();
		}
		
		// PATTERN POST-REDIRECT-GET: Manteniamo l'UX originale
		
		// Aggiungi messaggio di successo come flash message
		if (successMessage != null) {
		    FlashMessagesManager.addSuccessMessage(request, successMessage);
		}
		
		// Aggiungi errori strutturati per campo (per mantenere UX originale)
		if (!errorMessages.isEmpty()) {
		    FlashMessagesManager.addFieldErrors(request, errorMessages);
		}
		
		// Aggiungi valori del form (solo se ci sono errori, per ri-popolare i campi)
		if (hasErrors && playlistName != null) {
		    Map<String, String> formValues = new HashMap<>();
		    formValues.put("name", playlistName);  // Chiave corretta per compatibilità template
		    FlashMessagesManager.addFormValues(request, formValues);
		}
		
		// REDIRECT invece di forward - questo elimina le duplicazioni!
		String homePath = getServletContext().getContextPath() + "/Home";
		response.sendRedirect(homePath);
	}
	
	private boolean containsDuplicates(int[] songIDs) {
	    if (songIDs == null || songIDs.length < 2) {
	        return false;
	    }
	    
	    Set<Integer> uniqueSongs = new HashSet<>();
	    
	    for (int songID : songIDs) {
	        if (!uniqueSongs.add(songID)) {
	            return true;
	        }
	    }
	    
	    return false;
	}
	
	public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}