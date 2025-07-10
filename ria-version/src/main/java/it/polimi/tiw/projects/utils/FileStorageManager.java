package it.polimi.tiw.projects.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.Part;

public class FileStorageManager {
    private static final String PROPERTIES_FILE = "/WEB-INF/file_storage.properties";
    private static String baseStoragePath;
    private static String coverImagesPath;
    private static String audioFilesPath;
    private static boolean initialized = false;
    
    // Directory consentite
    private static final List<String> ALLOWED_DIRS = Arrays.asList("covers", "songs");
    
    // Estensioni consentite per tipo
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private static final List<String> ALLOWED_AUDIO_EXTENSIONS = Arrays.asList("mp3", "wav", "ogg", "m4a");
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "mp3", "wav", "ogg", "m4a"
    );
    
    // MIME types consentiti
    private static final List<String> ALLOWED_IMAGE_MIMES = Arrays.asList("image/jpeg", "image/png", "image/gif");
    private static final List<String> ALLOWED_AUDIO_MIMES = Arrays.asList(
        "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4", "audio/m4a", "application/ogg"
    );
    
    // Limiti di dimensione
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    


    /**
     * Inizializza i percorsi di storage dai file di configurazione
     */
    public static void initialize(ServletContext context) throws UnavailableException {
        if (initialized) return;
        
        Properties storageProperties = new Properties();
        
        try {
            // Carica il file di proprietà
            InputStream inputStream = context.getResourceAsStream(PROPERTIES_FILE);
            if (inputStream == null) {
                throw new UnavailableException("File storage configuration file not found: " + PROPERTIES_FILE);
            }
            
            // Carica le proprietà dal file
            storageProperties.load(inputStream);
            inputStream.close();
            
            // Ottieni i percorsi dal file properties
            baseStoragePath = storageProperties.getProperty("baseStoragePath");
            String coverDir = storageProperties.getProperty("coverImagesDir", "covers");
            String audioDir = storageProperties.getProperty("audioFilesDir", "songs");
            
            // Verifica che il percorso base sia specificato
            if (baseStoragePath == null || baseStoragePath.trim().isEmpty()) {
                throw new UnavailableException("Missing base storage path in configuration file");
            }
            
            // Crea directory base se non esiste
            File baseDir = new File(baseStoragePath);
            if (!baseDir.exists()) {
                if (!baseDir.mkdirs()) {
                    throw new UnavailableException("Cannot create base storage directory: " + baseStoragePath);
                }
            }
            
            // Costruisci i percorsi completi
            coverImagesPath = baseStoragePath + File.separator + coverDir;
            audioFilesPath = baseStoragePath + File.separator + audioDir;
            
            // Crea le sottodirectory
            new File(coverImagesPath).mkdirs();
            new File(audioFilesPath).mkdirs();
            
            initialized = true;
            
        } catch (IOException e) {
            throw new UnavailableException("Error reading file storage configuration: " + e.getMessage());
        }
    }
    
    /**
     * Restituisce il percorso base per lo storage dei file
     */
    public static String getBaseStoragePath() {
        return baseStoragePath;
    }
    
    /**
     * Restituisce il percorso completo per le immagini di copertina
     */
    public static String getCoverImagesPath() {
        return coverImagesPath;
    }
    
    /**
     * Restituisce il percorso completo per i file audio
     */
    public static String getAudioFilesPath() {
        return audioFilesPath;
    }
    
    /**
     * Restituisce il percorso relativo per un file di copertina
     */
    public static String getRelativeCoverPath(String filename) {
        return "/covers/" + filename;
    }
    
    /**
     * Restituisce il percorso relativo per un file audio
     */
    public static String getRelativeAudioPath(String filename) {
        return "/songs/" + filename;
    }
    
    
    /**
     * Valida se un percorso è sicuro (no path traversal, directory consentita, etc.)
     * @param relativePath Il percorso relativo da validare (es. "/covers/image.jpg")
     * @return true se il percorso è sicuro, false altrimenti
     */
    public static boolean isPathSafe(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }
        
        // Rimuovi lo slash iniziale se presente
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        
        // Controlli di sicurezza di base
        if (cleanPath.contains("..") || cleanPath.contains("\\") || cleanPath.isEmpty()) {
            return false;
        }
        
        // Verifica struttura del percorso (deve essere directory/filename)
        String[] parts = cleanPath.split("/");
        if (parts.length != 2) {
            return false;
        }
        
        // Verifica che la directory sia consentita
        String directory = parts[0];
        if (!ALLOWED_DIRS.contains(directory)) {
            return false;
        }
        
        // Verifica nome file valido
        String filename = parts[1];
        if (filename.isEmpty() || filename.equals(".") || filename.equals("..") || filename.startsWith(".")) {
            return false;
        }
        
        // Verifica estensione
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            return false;
        }
        
        String extension = filename.substring(dotIndex + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
    
    /**
     * Valida e normalizza un percorso relativo, lanciando SecurityException se non sicuro
     * @param relativePath Il percorso relativo da validare
     * @return Il Path normalizzato e sicuro
     * @throws SecurityException se il percorso non è sicuro
     */
    public static Path validateAndNormalizePath(String relativePath) throws SecurityException {
        if (!isPathSafe(relativePath)) {
            throw new SecurityException("Invalid or unsafe path: " + relativePath);
        }
        
        // Rimuovi lo slash iniziale se presente
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        
        try {
            Path basePath = Paths.get(baseStoragePath).toAbsolutePath().normalize();
            Path targetPath = basePath.resolve(cleanPath).normalize();
            
            // Verifica che il percorso sia ancora dentro la base directory
            if (!targetPath.startsWith(basePath)) {
                throw new SecurityException("Path traversal detected");
            }
            
            return targetPath;
            
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                throw (SecurityException) e;
            }
            throw new SecurityException("Error validating path: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se un file Part è un'immagine valida
     * @param filePart Il file Part da validare
     * @return true se è un'immagine valida, false altrimenti
     */
    public static boolean isValidImageFile(Part filePart) {
        if (filePart == null || filePart.getSize() == 0) {
            return false;
        }
        
        // Controllo dimensione
        if (filePart.getSize() > MAX_FILE_SIZE) {
            return false;
        }
        
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null) {
            return false;
        }
        
        fileName = fileName.toLowerCase();
        String contentType = filePart.getContentType();
        
        // Verifica estensione
        boolean validExtension = false;
        for (String ext : ALLOWED_IMAGE_EXTENSIONS) {
            if (fileName.endsWith("." + ext)) {
                validExtension = true;
                break;
            }
        }
        
        // Verifica MIME type
        boolean validMimeType = contentType != null && ALLOWED_IMAGE_MIMES.contains(contentType);
        
        return validExtension && validMimeType;
    }
    
    /**
     * Verifica se un file Part è un file audio valido
     * @param filePart Il file Part da validare
     * @return true se è un file audio valido, false altrimenti
     */
    public static boolean isValidAudioFile(Part filePart) {
        if (filePart == null || filePart.getSize() == 0) {
            return false;
        }
        
        // Controllo dimensione
        if (filePart.getSize() > MAX_FILE_SIZE) {
            return false;
        }
        
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null) {
            return false;
        }
        
        fileName = fileName.toLowerCase();
        String contentType = filePart.getContentType();
        
        // Verifica estensione
        boolean validExtension = false;
        for (String ext : ALLOWED_AUDIO_EXTENSIONS) {
            if (fileName.endsWith("." + ext)) {
                validExtension = true;
                break;
            }
        }
        
        // Verifica MIME type (più permissivo per audio)
        boolean validMimeType = contentType != null && 
                               (contentType.startsWith("audio/") || contentType.equals("application/ogg"));
        
        return validExtension && validMimeType;
    }
    
    /**
     * Salva in modo sicuro un file uploadato
     * @param filePart Il file Part da salvare
     * @param directory La directory di destinazione ("covers" o "songs")
     * @return Il percorso relativo del file salvato
     * @throws IOException se si verifica un errore I/O
     * @throws SecurityException se il file non è valido o sicuro
     */
    public static String saveUploadedFile(Part filePart, String directory) throws IOException, SecurityException {
        if (!ALLOWED_DIRS.contains(directory)) {
            throw new SecurityException("Invalid directory: " + directory);
        }
        
        // Valida il file in base al tipo
        if (directory.equals("covers") && !isValidImageFile(filePart)) {
            throw new SecurityException("Invalid image file");
        } else if (directory.equals("songs") && !isValidAudioFile(filePart)) {
            throw new SecurityException("Invalid audio file");
        }
        
        // Genera nome file univoco
        String uniqueFileName = generateUniqueFileName(filePart.getSubmittedFileName());
        
        // Determina il percorso completo
        String fullDirPath = directory.equals("covers") ? coverImagesPath : audioFilesPath;
        File uploadDir = new File(fullDirPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        File targetFile = new File(uploadDir, uniqueFileName);
        
        // Salva il file
        InputStream input = filePart.getInputStream();
        Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        
        // Ritorna il percorso relativo
        return "/" + directory + "/" + uniqueFileName;
    }
    
    /**
     * Elimina un file in modo sicuro
     * @param relativePath Il percorso relativo del file da eliminare
     * @return true se l'eliminazione ha avuto successo, false altrimenti
     * @throws SecurityException se il percorso non è sicuro
     */
    public static boolean deleteFileSecurely(String relativePath) throws SecurityException {
        try {
            Path targetPath = validateAndNormalizePath(relativePath);
            File targetFile = targetPath.toFile();
            
            // Se il file non esiste, consideriamo l'operazione completata con successo
            if (!targetFile.exists()) {
                return true;
            }
            
            // Verifica che sia un file normale
            if (!targetFile.isFile()) {
                throw new SecurityException("Target is not a regular file");
            }
            
            // Elimina il file
            boolean deleted = targetFile.delete();
            return deleted;
            
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Error deleting file: " + e.getMessage());
        }
    }
    
    /**
     * Ottiene un file in modo sicuro per la lettura
     * @param relativePath Il percorso relativo del file
     * @return Il File validato
     * @throws SecurityException se il percorso non è sicuro
     * @throws FileNotFoundException se il file non esiste
     */
    public static File getFileSecurely(String relativePath) throws SecurityException, FileNotFoundException {
        Path targetPath = validateAndNormalizePath(relativePath);
        File file = targetPath.toFile();
        
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }
        
        if (!file.isFile()) {
            throw new SecurityException("Target is not a regular file");
        }
        
        return file;
    }
    
    /**
     * Genera un nome file univoco mantenendo l'estensione originale
     * @param originalFileName Il nome file originale
     * @return Un nome file univoco
     */
    public static String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * Elimina uno o più file in modo sicuro (best effort, non lancia eccezioni)
     * Utile per cleanup in caso di errori
     * @param relativePaths I percorsi relativi dei file da eliminare
     */
    public static void cleanupFiles(String... relativePaths) {
        if (relativePaths == null) return;
        
        for (String path : relativePaths) {
            if (path != null && !path.isEmpty()) {
                try {
                    deleteFileSecurely(path);
                } catch (Exception e) {
                    // Il cleanup deve essere "best effort"
                    // Non propaga l'eccezione
                }
            }
        }
    }
}