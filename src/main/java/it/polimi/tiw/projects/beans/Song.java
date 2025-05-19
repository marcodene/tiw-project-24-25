package it.polimi.tiw.projects.beans;

public class Song {
	private int userID;
    private String name;
    private String albumName;
    private String artistName;
    private int albumReleaseYear;
    private String genre;
    private String albumCoverPath;
    private String audioFilePath;
    
    
	public int getUserID() {
		return userID;
	}
	public void setUserID(int userID) {
		this.userID = userID;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getAlbumName() {
		return albumName;
	}
	public void setAlbumName(String albumName) {
		this.albumName = albumName;
	}
	
	public String getArtistName() {
		return artistName;
	}
	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}
	
	public int getAlbumReleaseYear() {
		return albumReleaseYear;
	}
	public void setAlbumReleaseYear(int albumReleaseYear) {
		this.albumReleaseYear = albumReleaseYear;
	}
	
	public String getGenre() {
		return genre;
	}
	public void setGenre(String genre) {
		this.genre = genre;
	}
	
	public String getAlbumCoverPath() {
		return albumCoverPath;
	}
	public void setAlbumCoverPath(String albumCoverPath) {
		this.albumCoverPath = albumCoverPath;
	}
	
	public String getAudioFilePath() {
		return audioFilePath;
	}
	public void setAudioFilePath(String audioFilePath) {
		this.audioFilePath = audioFilePath;
	}
    
    
	
}
