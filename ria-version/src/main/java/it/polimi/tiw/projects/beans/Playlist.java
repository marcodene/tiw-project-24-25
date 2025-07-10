package it.polimi.tiw.projects.beans;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Playlist {
	private int ID;
	private int userID;
    private String name;
    private Date creationDate;
    private List<Song> songs = new ArrayList<>();
    // Field for custom order, could be null if not set
    private List<Integer> customSongOrder = null;


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

	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public List<Song> getSongs() {
		return songs;
	}
	public void setSongs(List<Song> songs) {
		this.songs = songs;
	}

    public List<Integer> getCustomSongOrder() {
        return customSongOrder;
    }

    public void setCustomSongOrder(List<Integer> customSongOrder) {
        this.customSongOrder = customSongOrder;
    }

	public Map<String, Object> toJSON() {
		Map<String, Object> jsonPlaylist = new HashMap<>();
		jsonPlaylist.put("ID", ID);
		jsonPlaylist.put("userID", userID);
		jsonPlaylist.put("name", name);
		if (creationDate != null) {
            // Using ISO 8601 format "yyyy-MM-dd" for date
			jsonPlaylist.put("creationDate", new SimpleDateFormat("yyyy-MM-dd").format(creationDate));
		} else {
			jsonPlaylist.put("creationDate", null);
		}
		
		List<Map<String, Object>> jsonSongs = new ArrayList<>();
		if (songs != null) {
			for (Song song : songs) {
				jsonSongs.add(song.toJSON());
			}
		}
		jsonPlaylist.put("songs", jsonSongs);
        
        if (customSongOrder != null) {
            jsonPlaylist.put("customSongOrder", customSongOrder);
        }
		return jsonPlaylist;
	}
}
