package it.polimi.tiw.projects.beans;

import java.util.HashMap;
import java.util.Map;

public class Song {
	private int ID;
	private int userID;
    private String name;
    private String albumName;
    private String artistName;
    private int albumReleaseYear;
    private String genre;
    private String albumCoverPath;
    private String audioFilePath;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

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

	public Map<String, Object> toJSON() {
		Map<String, Object> jsonSong = new HashMap<>();
		jsonSong.put("ID", ID);
		jsonSong.put("userID", userID);
		jsonSong.put("name", name);
		jsonSong.put("albumName", albumName);
		jsonSong.put("artistName", artistName);
		jsonSong.put("albumReleaseYear", albumReleaseYear);
		jsonSong.put("genre", genre);
		jsonSong.put("albumCoverPath", albumCoverPath);
		jsonSong.put("audioFilePath", audioFilePath);
		return jsonSong;
	}
}
