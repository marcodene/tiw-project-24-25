package it.polimi.tiw.projects.beans;

import java.util.HashMap;
import java.util.Map;

public class User {
	private int id;
	private String username;
	private String name;
	private String surname;
	// Password is not stored in the bean, so no need to exclude it from toJSON

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public Map<String, Object> toJSON() {
		Map<String, Object> jsonUser = new HashMap<>();
		jsonUser.put("id", id);
		jsonUser.put("username", username);
		jsonUser.put("name", name);
		jsonUser.put("surname", surname);
		return jsonUser;
	}
}
