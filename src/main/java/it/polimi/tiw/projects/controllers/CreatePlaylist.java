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
		
		// Crea una mappa per i messaggi di errore e valori del form
		Map<String, String> errorMessages = new HashMap<>();
		Map<String, String> formValues = new HashMap<>();
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
		        errorMessages.put("selectedSongsError", "ID delle canzoni non validi");
		        hasErrors = true;
		    }
		}
		
		User user = (User) session.getAttribute("user");
		SongDAO songDAO = new SongDAO(connection);
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		
		try {
			playlistName = StringEscapeUtils.escapeJava(request.getParameter("playlistName"));
			formValues.put("playlistName", playlistName);
			
			
			if (playlistName == null || playlistName.isEmpty()) {
			    errorMessages.put("playlistNameError", "Il nome della playlist è obbligatorio");
			    hasErrors = true;
			}
			
			if (selectedSongIDs == null || selectedSongIDs.length == 0) {
			    errorMessages.put("selectedSongsError", "Devi selezionare almeno una canzone");
			    hasErrors = true;
			}
			
			if(containsDuplicates(selectedSongIDs)) {
				errorMessages.put("selectedSongsError", "La playlist non può contenere canzoni duplicate");
				hasErrors = true;
			}
			
			try {
				if(!hasErrors && !songDAO.existAllSongsByIDsAndUser(selectedSongIDs, user.getId())) {
				    errorMessages.put("selectedSongsError", "Tutte le canzoni devono essere già state caricate");
				    hasErrors = true;
			    }
			
			    if(!hasErrors && playlistDAO.existsPlaylistByNameAndUser(playlistName, user.getId())) {
				    errorMessages.put("playlistNameError", "Esiste già una playlist con questo nome");
				    hasErrors = true;
			    }
			    
			    // Se non ci sono errori, procedi con la creazione
			    if(!hasErrors) {
			    	boolean success = playlistDAO.createPlaylist(playlistName, selectedSongIDs, user.getId());
    			
    			    if (!success) {
        	            errorMessages.put("generalError", "La playlist non è stata creata. Verifica che i valori siano corretti.");
        	            hasErrors = true;
        	        } else {
        	            // Creazione completata con successo
        	            successMessage = "Playlist '" + playlistName + "' creata con successo!";
        	            // Pulisci i valori del form dopo il successo
        	            formValues.clear();
        	        }
			    }
			} catch (SQLException e) {
				errorMessages.put("generalError", "Errore del database: " + e.getMessage());
				hasErrors = true;
				e.printStackTrace();
			}
			
		} catch (Exception e) {
			errorMessages.put("generalError", "Errore durante l'elaborazione dei dati: " + e.getMessage());
			hasErrors = true;
			e.printStackTrace();
		}
		
		// Aggiungiamo gli errori e i valori come attributi della request (non della sessione)
		if (!errorMessages.isEmpty()) {
		    request.setAttribute("playlistErrorMessages", errorMessages);
		}
		if (!formValues.isEmpty()) {
		    request.setAttribute("playlistFormValues", formValues);
		}
		if (successMessage != null) {
		    request.setAttribute("successMessage", successMessage);
		}
		
		// Forward alla servlet GoToHomePage
		request.getRequestDispatcher("/Home").forward(request, response);
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