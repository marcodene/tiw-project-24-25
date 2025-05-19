package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
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
import java.text.ParseException;
import java.time.Year;
import java.util.UUID;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.coyote.BadRequestException;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.GenreDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

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
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// if the user is not logged in (not present in session) redirect to the login
		HttpSession session = request.getSession();
		if(session.isNew() || session.getAttribute("user")==null) {
			String loginPagePath = getServletContext().getContextPath() + "/";
			response.sendRedirect(loginPagePath);
			return;
		}
		
		// Get and parse all parameters from request
		boolean isBadRequest = false;
		String songName = null;
		String albumName = null;
		String artistName = null;
		Integer albumReleaseYear = null;
		String genre = null;
		Part albumCoverPart = null;
        Part songFilePart = null;
		try {
			songName = StringEscapeUtils.escapeJava(request.getParameter("songName"));
			albumName = StringEscapeUtils.escapeJava(request.getParameter("albumName"));
			artistName = StringEscapeUtils.escapeJava(request.getParameter("artistName"));
			albumReleaseYear = Integer.parseInt(request.getParameter("albumReleaseYear"));
			genre = StringEscapeUtils.escapeJava(request.getParameter("genre"));
			albumCoverPart = request.getPart("albumCover");
            songFilePart = request.getPart("songFile");
			
			/*
			 * devo controllare che:
			 * il titolo della canzone non esista già
			 * l'anno non sia posteriore alla data di oggi
			 * il genere musicale esista tra quelli presenti nel database
			 * che le estensioni dei file siano correti, sia immagine che musica
			 */
            User user = (User) session.getAttribute("user");
            Song song = new Song();
            SongDAO songDAO = new SongDAO(connection);
            GenreDAO genreDAO = new GenreDAO(connection);
            
            try {
            	
            	isBadRequest = songDAO.existsSongByNameAndUser(songName, user.getId())
            			|| albumReleaseYear > MAX_RELEASE_YEAR
            			|| albumReleaseYear < MIN_RELEASE_YEAR
            			|| !genreDAO.existsGenreByName(genre);
            	
            	
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
            	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
            	    		"The song wasn't uploaded to the database. Please check the values are correct.");
            	    return;
            	}
				
			} catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to upload song");
				e.printStackTrace();
			}
            
			
			
		}catch (NumberFormatException | NullPointerException | BadRequestException e) {
			isBadRequest = true;
			e.printStackTrace();
		}
		
		if (isBadRequest) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect or missing param values");
			return;
		}
		
		
	
		String homePagePath = getServletContext().getContextPath() + "/Home";
		response.sendRedirect(homePagePath);
		return;
		
		
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
		String uploadPath = getServletContext().getRealPath("/WEB-INF/uploads/" + subdirectory);
		
		File uploadDir = new File(uploadPath);
		if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
		
		String filePath = uploadPath + File.separator + fileName;
		filePart.write(filePath);
		
		// Return a relative path to be saved in the database
        return "/uploads/" + subdirectory + "/" + fileName;
	}

}
