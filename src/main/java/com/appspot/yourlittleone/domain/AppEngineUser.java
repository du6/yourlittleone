package com.appspot.yourlittleone.domain;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public final class AppEngineUser {
	@Id
	private final String email;
	private final User user;

	public AppEngineUser(User user) {
		this.user = user;
		this.email = user.getEmail();
	}

	public User getUser() {
		return user;
	}

	public Key<AppEngineUser> getKey() {
		return Key.create(AppEngineUser.class, email);
	}
}
