package it.polimi.tiw.projects.beans;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class Playlist {
	private int ID;
	private int userID;
    private String name;
    private Date creationDate;
    private List<Song> songs = new ArrayList<>();
	
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
		this.creationDate= creationDate;
	}
	
	public List<Song> getSongs() {
		return songs;
	}
	public void setSongs(List<Song> songs) {
		this.songs = songs;
	}
    
    
}
