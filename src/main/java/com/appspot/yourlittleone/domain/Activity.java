package com.appspot.yourlittleone.domain;

import static com.appspot.yourlittleone.service.OfyService.ofy;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.appspot.yourlittleone.form.ActivityForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Activity class stores activity information.
 */
@Entity
public final class Activity {

    private static final String DEFAULT_LOCATION = "Default Location";

    private static final List<String> DEFAULT_TOPICS = ImmutableList.of("Default", "Topic");

    /**
     * The id for the datastore key.
     *
     * We use automatic id assignment for entities of Activity class.
     */
    @Id
    private Long id;

    /**
     * The name of the activity.
     */
    @Index
    private String name;

    /**
     * The description of the activity.
     */
    private String description;

    /**
     * Holds Profile key as the parent.
     */
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Profile> profileKey;

    /**
     * The gplus_id of the organizer.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String organizerUserId;

    /**
     * Topics related to this activity.
     */
    @Index
    private List<String> topics;

    /**
     * The location that the activity takes place.
     */
    @Index
    private String location;

    /**
     * The starting date and time of this activity.
     */
    private Date startDate;

    /**
     * The ending date and time of this activity.
     */
    private Date endDate;

    /**
     * Indicating the starting month derived from startDate.
     *
     * We need this for a composite query specifying the starting month.
     */
    @Index
    private int month;

    /**
     * The maximum capacity of this activity.
     */
    @Index
    private int maxAttendees;

    /**
     * Number of seats currently available.
     */
    @Index
    private int seatsAvailable;
    
    /**
     * Just making the default constructor private.
     */
    private Activity() {}

    public Activity(final long id, final String organizerUserId,
                      final ActivityForm activityForm) {
        Preconditions.checkNotNull(activityForm.getName(), "The name is required");
        this.id = id;
        this.profileKey = Key.create(Profile.class, organizerUserId);
        this.organizerUserId = organizerUserId;
        updateWithActivityForm(activityForm);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Profile> getProfileKey() {
        return profileKey;
    }

    public String getWebsafeKey() {
        return Key.create(profileKey, Activity.class, id).getString();
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getOrganizerUserId() {
        return organizerUserId;
    }

    /**
     * Returns organizer's display name.
     * @return organizer's display name. If there is no Profile, return his/her gplusId.
     */
    public String getOrganizerDisplayName() {
        Profile organizer = ofy().load().key(Key.create(Profile.class, organizerUserId)).now();
        if (organizer == null) {
            return organizerUserId;
        } else {
            return organizer.getDisplayName();
        }
    }

    /**
     * Returns a defensive copy of topics if not null.
     * @return a defensive copy of topics if not null.
     */
    public List<String> getTopics() {
        return topics == null ? null : ImmutableList.copyOf(topics);
    }

    public String getLocation() {
        return location;
    }

    /**
     * Returns a defensive copy of startDate if not null.
     * @return a defensive copy of startDate if not null.
     */
    public Date getStartDate() {
        return startDate == null ? null : new Date(startDate.getTime());
    }

    /**
     * Returns a defensive copy of endDate if not null.
     * @return a defensive copy of endDate if not null.
     */
    public Date getEndDate() {
        return endDate == null ? null : new Date(endDate.getTime());
    }

    public int getMonth() {
        return month;
    }

    public int getMaxAttendees() {
        return maxAttendees;
    }

    public int getSeatsAvailable() {
        return seatsAvailable;
    }

    /**
     * Updates the Activity with ActivityForm.
     * This method is used upon object creation as well as updating existing Activities.
     *
     * @param activityForm contains form data sent from the client.
     */
    public void updateWithActivityForm(ActivityForm activityForm) {
        this.name = activityForm.getName();
        this.description = activityForm.getDescription();
        List<String> topics = activityForm.getTopics();
        this.topics = topics == null || topics.isEmpty() ? DEFAULT_TOPICS : topics;
        this.location = activityForm.getLocation() == null ? DEFAULT_LOCATION : activityForm.getLocation();

        Date startDate = activityForm.getStartDate();
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
        Date endDate = activityForm.getEndDate();
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
        if (this.startDate != null) {
            // Getting the starting month for a composite query.
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.startDate);
            // Calendar.MONTH is zero based, so adding 1.
            this.month = calendar.get(calendar.MONTH) + 1;
        }
        // Check maxAttendees value against the number of already allocated seats.
        int seatsAllocated = maxAttendees - seatsAvailable;
        if (activityForm.getMaxAttendees() < seatsAllocated) {
            throw new IllegalArgumentException(seatsAllocated + " seats are already allocated, "
                    + "but you tried to set maxAttendees to " + activityForm.getMaxAttendees());
        }
        // The initial number of seatsAvailable is the same as maxAttendees.
        // However, if there are already some seats allocated, we should subtract that numbers.
        this.maxAttendees = activityForm.getMaxAttendees();
        this.seatsAvailable = this.maxAttendees - seatsAllocated;
    }

    public void bookSeats(final int number) {
        if (seatsAvailable < number) {
        	if (seatsAvailable > 0) {
        		throw new IllegalArgumentException("There are only" + seatsAvailable + "seats available.");
        	} else {
            throw new IllegalArgumentException("There are no seats available.");
        	}
        }
        seatsAvailable = seatsAvailable - number;
    }

    public void giveBackSeats(final int number) {
        if (seatsAvailable + number > maxAttendees) {
            throw new IllegalArgumentException("The number of seats will exceeds the capacity.");
        }
        seatsAvailable = seatsAvailable + number;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Id: " + id + "\n")
                .append("Name: ").append(name).append("\n");
        if (location != null) {
            stringBuilder.append("Location: ").append(location).append("\n");
        }
        if (topics != null && topics.size() > 0) {
            stringBuilder.append("Topics:\n");
            for (String topic : topics) {
                stringBuilder.append("\t").append(topic).append("\n");
            }
        }
        if (startDate != null) {
            stringBuilder.append("StartDate: ").append(startDate.toString()).append("\n");
        }
        if (endDate != null) {
            stringBuilder.append("EndDate: ").append(endDate.toString()).append("\n");
        }
        stringBuilder.append("Max Attendees: ").append(maxAttendees).append("\n");
        return stringBuilder.toString();
    }
}
