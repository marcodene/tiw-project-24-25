package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.coyote.BadRequestException;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.FileStorageManager;

@MultipartConfig
@WebServlet("/UploadSong")
public class UploadSong extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final int MIN_RELEASE_YEAR = 1600;
    public static final int MAX_RELEASE_YEAR = Year.now().getValue();
	
	private Connection connection;
       
    public UploadSong() {
        super();
    }
    
    public void init() throws ServletException {
    	connection = ConnectionHandler.getConnection(getServletContext());
    	
    	// Inizializza il file storage manager
        try {
            FileStorageManager.initialize(getServletContext());
        } catch (UnavailableException e) {
            throw new ServletException("Failed to initialize file storage: " + e.getMessage(), e);
        }
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
		
		// Get and parse all parameters from request
		boolean hasErrors = false;
		String songName = null;
		String albumName = null;
		String artistName = null;
		Integer albumReleaseYear = null;
		String genre = null;
		Part albumCoverPart = null;
        Part songFilePart = null;
        
		try {
			// Validazione del nome della canzone
			songName = StringEscapeUtils.escapeJava(request.getParameter("songName"));
			formValues.put("songName", songName);
			if (songName == null || songName.trim().isEmpty()) {
				errorMessages.put("nameError", "Il titolo della canzone è obbligatorio");
				hasErrors = true;
			}
			
			// Validazione del nome dell'album
			albumName = StringEscapeUtils.escapeJava(request.getParameter("albumName"));
			formValues.put("albumName", albumName);
			if (albumName == null || albumName.trim().isEmpty()) {
				errorMessages.put("albumError", "Il titolo dell'album è obbligatorio");
				hasErrors = true;
			}
			
			// Validazione del nome dell'artista
			artistName = StringEscapeUtils.escapeJava(request.getParameter("artistName"));
			formValues.put("artistName", artistName);
			if (artistName == null || artistName.trim().isEmpty()) {
				errorMessages.put("artistError", "Il nome dell'artista è obbligatorio");
				hasErrors = true;
			}
			
			// Validazione dell'anno di pubblicazione
			String yearStr = request.getParameter("albumReleaseYear");
			if (yearStr == null || yearStr.trim().isEmpty()) {
				errorMessages.put("yearError", "L'anno di pubblicazione è obbligatorio");
				hasErrors = true;
			} else {
				try {
					albumReleaseYear = Integer.parseInt(yearStr);
					formValues.put("albumReleaseYear", String.valueOf(albumReleaseYear));
					
					if(albumReleaseYear < MIN_RELEASE_YEAR || albumReleaseYear > MAX_RELEASE_YEAR) {
						errorMessages.put("yearError", "L'anno di pubblicazione deve essere compreso tra " + 
								MIN_RELEASE_YEAR + " e " + MAX_RELEASE_YEAR);
						hasErrors = true;
					}
				} catch (NumberFormatException e) {
					errorMessages.put("yearError", "L'anno di pubblicazione deve essere un numero valido");
					hasErrors = true;
				}
			}
			
			// Validazione del genere musicale
			genre = StringEscapeUtils.escapeJava(request.getParameter("genre"));
			formValues.put("genre", genre);
			if (genre == null || genre.trim().isEmpty()) {
				errorMessages.put("genreError", "Il genere musicale è obbligatorio");
				hasErrors = true;
			}
			
			// Validazione dei file
			albumCoverPart = request.getPart("albumCover");
			if (albumCoverPart == null || albumCoverPart.getSize() <= 0) {
				errorMessages.put("albumCoverError", "L'immagine di copertina è obbligatoria");
				hasErrors = true;
			} else if (!isValidImageFile(albumCoverPart)) {
				errorMessages.put("albumCoverError", "Il file deve essere un'immagine valida (JPEG, PNG, GIF)");
				hasErrors = true;
			}
			
            songFilePart = request.getPart("songFile");
            if (songFilePart == null || songFilePart.getSize() <= 0) {
				errorMessages.put("songFileError", "Il file audio è obbligatorio");
				hasErrors = true;
			} else if (!isValidAudioFile(songFilePart)) {
				errorMessages.put("songFileError", "Il file deve essere un audio valido (MP3, WAV, OGG)");
				hasErrors = true;
			}
			
			User user = (User) session.getAttribute("user");
            SongDAO songDAO = new SongDAO(connection);
            GenreDAO genreDAO = new GenreDAO(connection);
            
            try {
            	// Verifica esistenza canzone con stesso nome
            	if(songDAO.existsSongByNameAndUser(songName, user.getId())) {
            		errorMessages.put("nameError", "Una canzone con questo nome esiste già");
            		hasErrors = true;
            	}
            	
            	// Verifica genere musicale valido
            	if(!genreDAO.existsGenreByName(genre)) {
            		errorMessages.put("genreError", "Il genere musicale selezionato non è valido");
            		hasErrors = true;
            	}
            	
            	// Se non ci sono errori, procedi con l'upload
            	if(!hasErrors) {
            	    Song song = new Song();
            	    song.setUserID(user.getId());
            	    song.setName(songName);
            	    song.setAlbumName(albumName);
            	    song.setArtistName(artistName);
            	    song.setAlbumReleaseYear(albumReleaseYear);
            	    song.setGenre(genre);
            			
            	    if(albumCoverPart != null && albumCoverPart.getSize() > 0) {
            		    String albumCoverFileName = getUniqueFileName(albumCoverPart.getSubmittedFileName());
    				    String albumCoverPath = saveFile(albumCoverPart, "covers", albumCoverFileName);
    				    song.setAlbumCoverPath(albumCoverPath);
            	    }
            	
            	    if(songFilePart != null && songFilePart.getSize() > 0) {
            		    String songFileName = getUniqueFileName(songFilePart.getSubmittedFileName());
    				    String songFilePath = saveFile(songFilePart, "songs", songFileName);
    				    song.setAudioFilePath(songFilePath);
            	    }
            			
            	    boolean success = songDAO.uploadSong(song);
            	
            	    if (!success) {
            		    errorMessages.put("generalError", "Non è stato possibile caricare la canzone. Controlla che i valori siano corretti.");
            		    hasErrors = true;
            	    } else {
            	        // Upload completato con successo
            	        successMessage = "Canzone '" + songName + "' caricata con successo!";
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
		    request.setAttribute("errorMessages", errorMessages);
		}
		if (!formValues.isEmpty()) {
		    request.setAttribute("formValues", formValues);
		}
		if (successMessage != null) {
		    request.setAttribute("successMessage", successMessage);
		}
		
		// Forward alla servlet GoToHomePage
		request.getRequestDispatcher("/Home").forward(request, response);
	}

	// Unique file's name generator
	private String getUniqueFileName(String originalFileName) {
		String extension = "";
		
		if(originalFileName.contains("."))
			extension = originalFileName.substring(originalFileName.lastIndexOf("."));
			
		return UUID.randomUUID().toString() + extension;
	}
	
	// Method to save a file on filesystem
	private String saveFile(Part filePart, String subdirectory, String fileName) throws IOException {
		String uploadPath;
        String relativePath;
        
        if ("covers".equals(subdirectory)) {
            uploadPath = FileStorageManager.getCoverImagesPath();
            relativePath = FileStorageManager.getRelativeCoverPath(fileName);
        } else { // songs
            uploadPath = FileStorageManager.getAudioFilesPath();
            relativePath = FileStorageManager.getRelativeAudioPath(fileName);
        }
        
        // Percorso completo del file
        String filePath = uploadPath + File.separator + fileName;
        
        // Salva il file
        try {
            filePart.write(filePath);
            System.out.println("Saved file to: " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
            throw new IOException("Impossibile salvare il file. Verifica che il server abbia i permessi di scrittura.", e);
        }
        
        // Ritorna il percorso relativo per l'accesso web
        return relativePath;
	}
	
	// Verify if the file is a valid image file
	private boolean isValidImageFile(Part filePart) {
	    String fileName = filePart.getSubmittedFileName().toLowerCase();
	    String contentType = filePart.getContentType();
	    
	    // Check file extension
	    boolean validExtension = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
	                             fileName.endsWith(".png") || fileName.endsWith(".gif");
	    
	    // Check MIME type
	    boolean validMimeType = contentType != null && 
	                           (contentType.equals("image/jpeg") || 
	                            contentType.equals("image/png") || 
	                            contentType.equals("image/gif"));
	    
	    return validExtension && validMimeType;
	}
	
	// Verify if the file is a valid audio file
	private boolean isValidAudioFile(Part filePart) {
	    String fileName = filePart.getSubmittedFileName().toLowerCase();
	    String contentType = filePart.getContentType();
	    
	    // Check file extension
	    boolean validExtension = fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
	                             fileName.endsWith(".ogg") || fileName.endsWith(".m4a");
	    
	    // Check MIME type
	    boolean validMimeType = contentType != null && 
	                           (contentType.startsWith("audio/") || 
	                            contentType.equals("application/ogg"));
	    
	    return validExtension && validMimeType;
	}
	
	public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}