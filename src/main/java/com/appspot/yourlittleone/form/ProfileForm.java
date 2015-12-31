package com.appspot.yourlittleone.form;

/**
 * Pojo representing a profile form on the client side.
 */
public final class ProfileForm {
    /**
     * Any string user wants us to display him/her on this system.
     */
    private String displayName;

    /**
     * User's gender
     */
    private Gender gender;

    private ProfileForm () {}

    public String getDisplayName() {
        return displayName;
    }

    public Gender getGender() {
        return gender;
    }

    /**
     * Enum representing gender.
     */
    public static enum Gender {
        Male,
        Female,
        Third,
        You_Guess
    }
}
