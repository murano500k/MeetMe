package com.stc.meetme.model;

/**
 * Created by artem on 10/25/16.
 */

public class User {
	private String name;
	private String photoUrl;
	private String userId;
	private String key;
	private String token;


	public User() {
	}

	public User(String userId, String name, String photoUrl, String key) {
		this.name = name;
		this.photoUrl = photoUrl;
		this.key = key;
		this.userId=userId;
		token=null;
	}

	public String getToken() {
		return token;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public String getUserId() {
		return userId;
	}
}
