package com.appspot.yourlittleone.domain;

import com.google.common.collect.ImmutableList;
import com.appspot.yourlittleone.form.ProfileForm.Gender;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile class stores user's profile data.
 */
@Entity
public final class Profile {
	/**
	 * Use userId (get from User's API getUserId()) as the datastore key.
	 */
	@Id
	private String userId;

	/**
	 * Any string user wants us to display him/her on this system.
	 */
	private String displayName;

	/**
	 * User's main e-mail address.
	 */
	private String mainEmail;

	/**
	 * The user's gender. Options are defined as an Enum in ProfileForm
	 */
	private Gender gender;

	/**
	 * Keys of the activities that this user registers to attend.
	 */
	private List<String> activityKeysToAttend = new ArrayList<>(0);
	
	/**
   * Just making the default constructor private.
   */
  private Profile() {}

	/**
	 * Public constructor for Profile.
	 * 
	 * @param userId
	 *          The datastore key.
	 * @param displayName
	 *          Any string user wants us to display him/her on this system.
	 * @param mainEmail
	 *          User's main e-mail address.
	 * @param gender
	 *          User's gender (Enum is in ProfileForm)
	 */
	public Profile(String userId, String displayName, String mainEmail, Gender gender) {
		this.userId = userId;
		this.displayName = displayName;
		this.mainEmail = mainEmail;
		this.gender = gender;
	}

	/**
	 * Getter for userId.
	 * 
	 * @return userId.
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Getter for displayName.
	 * 
	 * @return displayName.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Getter for mainEmail.
	 * 
	 * @return mainEmail.
	 */
	public String getMainEmail() {
		return mainEmail;
	}

	/**
	 * Getter for gender.
	 * 
	 * @return gender.
	 */
	public Gender getGender() {
		return gender;
	}

	/**
	 * Getter for activityKeysToAttend.
	 * 
	 * @return an immutable copy of activityKeysToAttend.
	 */
	public List<String> getActivityKeysToAttend() {
		return ImmutableList.copyOf(activityKeysToAttend);
	}

	/**
	 * Update the Profile with the given displayName and gender
	 * 
	 * @param displayName
	 * @param gender
	 */
	public void update(String displayName, Gender gender) {
		if (displayName != null) {
			this.displayName = displayName;
		}
		if (gender != null) {
			this.gender = gender;
		}
	}

	/**
	 * Adds a activityKey to activityKeysToAttend.
	 *
	 * The method addToActivityKeysToAttend is not thread-safe, but we need a
	 * transaction for calling this method after all, so it is not a practical
	 * issue.
	 *
	 * @param activityKey
	 *          a websafe String representation of the activityKey.
	 */
	public void addToActivityKeysToAttend(String activityKey) {
		activityKeysToAttend.add(activityKey);
	}

	/**
	 * Remove the activityKey from activityKeysToAttend.
	 *
	 * @param activityKey
	 *          a websafe String representation of the activityKey.
	 */
	public void unregisterFromActivity(String activityKey) {
		if (activityKeysToAttend.contains(activityKey)) {
			activityKeysToAttend.remove(activityKey);
		} else {
			throw new IllegalArgumentException("Invalid activity key: " + activityKey);
		}
	}
}
