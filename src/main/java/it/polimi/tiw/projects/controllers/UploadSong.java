package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        // Chiama l'init della classe base per gestione database
        super.init();
        
        // Inizializza il file storage manager specifico per questa servlet
        try {
            FileStorageManager.initialize(getServletContext());
        } catch (UnavailableException e) {
            throw new ServletException("Failed to initialize file storage: " + e.getMessage(), e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Controllo autenticazione
        User user = checkLogin(request, response);
        if (user == null) {
            return;         }
        
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
        
        // Variabili per gestione file e cleanup
        String songFilePath = null;
        String albumCoverPath = null;
        
        try {
            // Parsing e validazione parametri 
            songName = StringEscapeUtils.escapeJava(request.getParameter("songName"));
            if (!isEmpty(songName)) {
                formValues.put("songName", songName);
            } else {
                errorMessages.put("nameError", "Il titolo della canzone è obbligatorio");
                hasErrors = true;
            }
            
            albumName = StringEscapeUtils.escapeJava(request.getParameter("albumName"));
            if (!isEmpty(albumName)) {
                formValues.put("albumName", albumName);
            } else {
                errorMessages.put("albumError", "Il titolo dell'album è obbligatorio");
                hasErrors = true;
            }
            
            artistName = StringEscapeUtils.escapeJava(request.getParameter("artistName"));
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
            
            genre = StringEscapeUtils.escapeJava(request.getParameter("genre"));
            if (!isEmpty(genre)) {
                formValues.put("genre", genre);
            } else {
                errorMessages.put("genreError", "Il genere musicale è obbligatorio");
                hasErrors = true;
            }
            
            // Validazione file
            albumCoverPart = request.getPart("albumCover");
            if (albumCoverPart == null || albumCoverPart.getSize() <= 0) {
                errorMessages.put("albumCoverError", "L'immagine di copertina è obbligatoria");
                hasErrors = true;
            } else if (albumCoverPart.getSize() > 5 * 1024 * 1024) {
                errorMessages.put("albumCoverError", "L'immagine di copertina non può superare i 5MB");
                hasErrors = true;
            } else if (!isValidImageFile(albumCoverPart)) {
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
            } else if (!isValidAudioFile(songFilePart)) {
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
            
            // Se non ci sono errori, procedi con l'upload
            if (!hasErrors) {
                if (albumCoverPart != null && albumCoverPart.getSize() > 0) {
                    String albumCoverFileName = getUniqueFileName(albumCoverPart.getSubmittedFileName());
                    albumCoverPath = saveFile(albumCoverPart, "covers", albumCoverFileName);
                }
            
                if (songFilePart != null && songFilePart.getSize() > 0) {
                    String songFileName = getUniqueFileName(songFilePart.getSubmittedFileName());
                    songFilePath = saveFile(songFilePart, "songs", songFileName);
                }
                
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
                    deleteUploadedFiles(albumCoverPath, songFilePath);
                    errorMessages.put("generalError", "Non è stato possibile caricare la canzone. Controlla che i valori siano corretti.");
                    hasErrors = true;
                } else {
                    successMessage = "Canzone '" + songName + "' caricata con successo!";
                    formValues.clear();
                }
            }
            
        } catch (SQLException e) {
            deleteUploadedFiles(albumCoverPath, songFilePath);
            errorMessages.put("generalError", "Errore del database: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        } catch (Exception e) {
            deleteUploadedFiles(albumCoverPath, songFilePath);
            errorMessages.put("generalError", "Errore durante l'elaborazione dei dati: " + e.getMessage());
            hasErrors = true;
            e.printStackTrace();
        }
        
        // PATTERN POST-REDIRECT-GET
        String homePath = getServletContext().getContextPath() + "/Home";
        
        // Aggiungi prefissi per distinguere errori
        Map<String, String> uploadErrors = null;
        Map<String, String> uploadValues = null;
        
        if (!errorMessages.isEmpty()) {
            // Cancella eventuali file caricati se ci sono errori
            deleteUploadedFiles(albumCoverPath, songFilePath);
            
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
    
    // Unique file's name generator
    private String getUniqueFileName(String originalFileName) {
        String extension = "";
        
        if (originalFileName.contains("."))
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
        } else {
            uploadPath = FileStorageManager.getAudioFilesPath();
            relativePath = FileStorageManager.getRelativeAudioPath(fileName);
        }
        
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
    
    private void deleteUploadedFiles(String coverPath, String audioPath) {
        if (coverPath != null) {
            try {
                File coverFile = new File(FileStorageManager.getBaseStoragePath() + coverPath);
                if (coverFile.exists()) {
                    coverFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (audioPath != null) {
            try {
                File audioFile = new File(FileStorageManager.getBaseStoragePath() + audioPath);
                if (audioFile.exists()) {
                    audioFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}