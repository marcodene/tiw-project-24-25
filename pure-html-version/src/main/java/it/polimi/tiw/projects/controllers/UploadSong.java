package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.FileStorageManager;

@MultipartConfig
@WebServlet("/UploadSong")
public class UploadSong extends ServletBase {
    private static final long serialVersionUID = 1L;
    public static final int MIN_RELEASE_YEAR = 1600;
    public static final int MAX_RELEASE_YEAR = Year.now().getValue();
       
    public UploadSong() {
        super();
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
            throw new ServletException("Failed to initialize file storage: " + e.getMessage(), e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Controllo autenticazione
        User user = checkLogin(request, response);
        if (user == null) {
            return;
        }
        
        // Crea una mappa per i messaggi di errore e valori del form
        Map<String, String> errorMessages = new HashMap<>();
        Map<String, String> formValues = new HashMap<>();
        String successMessage = null;
        boolean hasErrors = false;
        
        // Variabili per dati validati
        String songName = null;
        String albumName = null;
        String artistName = null;
        Integer albumReleaseYear = null;
        String genre = null;
        Part albumCoverPart = null;
        Part songFilePart = null;
        
        // Variabili per gestione file
        String songFilePath = null;
        String albumCoverPath = null;
        
        try {
            // ===== PARSING E VALIDAZIONE PARAMETRI =====
            songName = request.getParameter("songName");
            if (!isEmpty(songName)) {
                formValues.put("songName", songName);
            } else {
                errorMessages.put("nameError", "Il titolo della canzone è obbligatorio");
                hasErrors = true;
            }
            
            albumName = request.getParameter("albumName");
            if (!isEmpty(albumName)) {
                formValues.put("albumName", albumName);
            } else {
                errorMessages.put("albumError", "Il titolo dell'album è obbligatorio");
                hasErrors = true;
            }
            
            artistName = request.getParameter("artistName");
            if (!isEmpty(artistName)) {
                formValues.put("artistName", artistName);
            } else {
                errorMessages.put("artistError", "Il nome dell'artista è obbligatorio");
                hasErrors = true;
            }
            
            // Validazione dell'anno di pubblicazione
            String yearStr = request.getParameter("albumReleaseYear");
            if (isEmpty(yearStr)) {
                errorMessages.put("yearError", "L'anno di pubblicazione è obbligatorio");
                hasErrors = true;
            } else {
                try {
                    albumReleaseYear = Integer.parseInt(yearStr);
                    formValues.put("albumReleaseYear", String.valueOf(albumReleaseYear));
                    
                    if (albumReleaseYear < MIN_RELEASE_YEAR || albumReleaseYear > MAX_RELEASE_YEAR) {
                        errorMessages.put("yearError", "L'anno di pubblicazione deve essere compreso tra " + 
                                MIN_RELEASE_YEAR + " e " + MAX_RELEASE_YEAR);
                        hasErrors = true;
                    }
                } catch (NumberFormatException e) {
                    errorMessages.put("yearError", "L'anno di pubblicazione deve essere un numero valido");
                    hasErrors = true;
                }
            }
            
            genre = request.getParameter("genre");
            if (!isEmpty(genre)) {
                formValues.put("genre", genre);
            } else {
                errorMessages.put("genreError", "Il genere musicale è obbligatorio");
                hasErrors = true;
            }
            
            // ===== VALIDAZIONE FILE CON FileStorageManager =====
            albumCoverPart = request.getPart("albumCover");
            if (albumCoverPart == null || albumCoverPart.getSize() <= 0) {
                errorMessages.put("albumCoverError", "L'immagine di copertina è obbligatoria");
                hasErrors = true;
            } else if (albumCoverPart.getSize() > 5 * 1024 * 1024) {
                errorMessages.put("albumCoverError", "L'immagine di copertina non può superare i 5MB");
                hasErrors = true;
            } else if (!FileStorageManager.isValidImageFile(albumCoverPart)) { // ===== USA FileStorageManager =====
                errorMessages.put("albumCoverError", "Il file deve essere un'immagine valida (JPEG, PNG, GIF)");
                hasErrors = true;
            }
            
            songFilePart = request.getPart("songFile");
            if (songFilePart == null || songFilePart.getSize() <= 0) {
                errorMessages.put("songFileError", "Il file audio è obbligatorio");
                hasErrors = true;
            } else if (songFilePart.getSize() > 10 * 1024 * 1024) {
                errorMessages.put("songFileError", "Il file audio non può superare i 10MB");
                hasErrors = true;
            } else if (!FileStorageManager.isValidAudioFile(songFilePart)) { // ===== USA FileStorageManager =====
                errorMessages.put("songFileError", "Il file deve essere un audio valido (MP3, WAV, OGG)");
                hasErrors = true;
            }
            
            // Validazioni database se non ci sono errori di input
            if (!hasErrors) {
                SongDAO songDAO = new SongDAO(connection);
                GenreDAO genreDAO = new GenreDAO(connection);
                
                // Verifica genere musicale valido
                if (!genreDAO.existsGenreByName(genre)) {
                    errorMessages.put("genreError", "Il genere musicale selezionato non è valido");
                    hasErrors = true;
                }
                
                // Verifica duplicati
                int genreId = genreDAO.getGenreIdByName(genre);
                if (genreId > 0 && songDAO.existsSongWithSameData(songName, albumName, artistName, 
                        albumReleaseYear, genreId, user.getId())) {
                    errorMessages.put("generalError", "Esiste già una canzone identica con questi dati");
                    hasErrors = true;
                }
            }
            
            // ===== SE NON CI SONO ERRORI, PROCEDI CON L'UPLOAD =====
            if (!hasErrors) {
                try {
                    albumCoverPath = FileStorageManager.saveUploadedFile(albumCoverPart, "covers");
                    songFilePath = FileStorageManager.saveUploadedFile(songFilePart, "songs");
                    
                    // Crea e salva canzone nel database
                    Song song = new Song();
                    song.setUserID(user.getId());
                    song.setName(songName);
                    song.setAlbumName(albumName);
                    song.setArtistName(artistName);
                    song.setAlbumReleaseYear(albumReleaseYear);
                    song.setGenre(genre);
                    song.setAlbumCoverPath(albumCoverPath);
                    song.setAudioFilePath(songFilePath);
                    
                    SongDAO songDAO = new SongDAO(connection);
                    boolean success = songDAO.uploadSong(song);
                
                    if (!success) {
                        FileStorageManager.cleanupFiles(albumCoverPath, songFilePath);
                        errorMessages.put("generalError", "Non è stato possibile caricare la canzone. Controlla che i valori siano corretti.");
                        hasErrors = true;
                    } else {
                        successMessage = "Canzone '" + songName + "' caricata con successo!";
                        formValues.clear();
                    }
                    
                } catch (SecurityException e) {
                    // ===== GESTIONE ERRORI DI SICUREZZA =====
                    FileStorageManager.cleanupFiles(albumCoverPath, songFilePath);
                    errorMessages.put("generalError", "Errore di sicurezza durante il caricamento: " + e.getMessage());
                    hasErrors = true;
                }
            }
            
        } catch (SQLException e) {
            FileStorageManager.cleanupFiles(albumCoverPath, songFilePath);
            errorMessages.put("generalError", "Errore del database: " + e.getMessage());
            hasErrors = true;
        } catch (Exception e) {
            FileStorageManager.cleanupFiles(albumCoverPath, songFilePath);
            errorMessages.put("generalError", "Errore durante l'elaborazione dei dati: " + e.getMessage());
            hasErrors = true;
        }
        
        // ===== PATTERN POST-REDIRECT-GET =====
        String homePath = getServletContext().getContextPath() + "/Home";
        
        // Aggiungi prefissi per distinguere errori
        Map<String, String> uploadErrors = null;
        Map<String, String> uploadValues = null;
        
        if (!errorMessages.isEmpty()) {
            uploadErrors = new HashMap<>();
            for (Map.Entry<String, String> error : errorMessages.entrySet()) {
                uploadErrors.put("upload_" + error.getKey(), error.getValue());
            }
        }
        
        // Aggiungi valori del form (solo se ci sono errori, per ri-popolare i campi)
        if (hasErrors && !formValues.isEmpty()) {
            uploadValues = new HashMap<>();
            for (Map.Entry<String, String> value : formValues.entrySet()) {
                uploadValues.put("upload_" + value.getKey(), value.getValue());
            }
        }
        
        doRedirect(request, response, homePath, successMessage, uploadErrors, uploadValues);
    }
}
