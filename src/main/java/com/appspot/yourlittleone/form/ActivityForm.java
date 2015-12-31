package com.appspot.yourlittleone.form;

import com.google.common.collect.ImmutableList;

import java.util.Date;
import java.util.List;

/**
 * A simple Java object (POJO) representing a Activity form sent from the client.
 */
public final class ActivityForm {
    /**
     * The name of the activity.
     */
    private String name;

    /**
     * The description of the activity.
     */
    private String description;

    /**
     * Topics that are discussed in this activity.
     */
    private List<String> topics;

    /**
     * The location where the activity will take place.
     */
    private String location;

    /**
     * The start date and time of the activity.
     */
    private Date startDate;

    /**
     * The end date and time of the activity.
     */
    private Date endDate;

    /**
     * The capacity of the activity.
     */
    private int maxAttendees;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTopics() {
        return topics;
    }

    public String getLocation() {
        return location;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getMaxAttendees() {
        return maxAttendees;
    }
}
