package it.polimi.tiw.projects.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;

public class FileStorageManager {
    private static final String PROPERTIES_FILE = "/WEB-INF/file_storage.properties";
    private static String baseStoragePath;
    private static String coverImagesPath;
    private static String audioFilesPath;
    private static boolean initialized = false;

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
            
            boolean useFallback = Boolean.parseBoolean(storageProperties.getProperty("useFallbackPath", "true"));
            String fallbackPath = storageProperties.getProperty("fallbackBasePath");
            
            // Verifica che il percorso base sia specificato
            if (baseStoragePath == null || baseStoragePath.trim().isEmpty()) {
                if (useFallback && fallbackPath != null && !fallbackPath.trim().isEmpty()) {
                    // Sostituisce ${user.home} con il percorso effettivo
                    baseStoragePath = fallbackPath.replace("${user.home}", System.getProperty("user.home"));
                } else {
                    throw new UnavailableException("Missing base storage path in configuration file");
                }
            }
            
            // Crea directory base se non esiste
            File baseDir = new File(baseStoragePath);
            if (!baseDir.exists()) {
                boolean created = baseDir.mkdirs();
                if (!created) {
                    System.err.println("Warning: Could not create base storage directory: " + baseStoragePath);
                    
                    // Se non è possibile creare la directory specificata, fallback alla home directory
                    if (useFallback) {
                        baseStoragePath = System.getProperty("user.home") + File.separator + "music_webapp_storage";
                        baseDir = new File(baseStoragePath);
                        baseDir.mkdirs();
                    }
                }
            }
            
            // Costruisci i percorsi completi
            coverImagesPath = baseStoragePath + File.separator + coverDir;
            audioFilesPath = baseStoragePath + File.separator + audioDir;
            
            // Crea le sottodirectory
            new File(coverImagesPath).mkdirs();
            new File(audioFilesPath).mkdirs();
            
            System.out.println("File storage initialized:");
            System.out.println("Base path: " + baseStoragePath);
            System.out.println("Covers path: " + coverImagesPath);
            System.out.println("Audio files path: " + audioFilesPath);
            
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
}