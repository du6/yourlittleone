package com.appspot.yourlittleone.spi;

import static com.appspot.yourlittleone.service.OfyService.factory;
import static com.appspot.yourlittleone.service.OfyService.ofy;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.appspot.yourlittleone.Constants;
import com.appspot.yourlittleone.domain.Activity;
import com.appspot.yourlittleone.domain.Announcement;
import com.appspot.yourlittleone.domain.AppEngineUser;
import com.appspot.yourlittleone.domain.Profile;
import com.appspot.yourlittleone.form.ActivityForm;
import com.appspot.yourlittleone.form.ProfileForm;
import com.appspot.yourlittleone.form.ProfileForm.Gender;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Work;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

/**
 * Defines Activity APIs.
 */

@Api(
        name = "activity",
        version = "v1",
        scopes = { Constants.EMAIL_SCOPE },
        clientIds = { Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID,
                Constants.API_EXPLORER_CLIENT_ID},
        audiences = {Constants.ANDROID_AUDIENCE},
        description = "Activity API for creating and regestering activities," +
                " and for creating and getting user Profiles"
)
public final class ActivityApi {

    private static final Logger LOG = Logger.getLogger(ActivityApi.class.getName());

    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    private static Profile getProfileFromUser(User user, String userId) {
        // First fetch it from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, userId)).now();
        if (profile == null) {
            // Create a new Profile if not exist.
            String email = user.getEmail();
            profile = new Profile(userId,
                    extractDefaultDisplayNameFromEmail(email), email, Gender.You_Guess);
        }
        return profile;
    }

    /**
     * This is an ugly workaround for null userId for Android clients.
     *
     * @param user A User object injected by the cloud endpoints.
     * @return the App Engine userId for the user.
     */
    private static String getUserId(User user) {
        String userId = user.getUserId();
        if (userId == null) {
            LOG.info("userId is null, so trying to obtain it from the datastore.");
            AppEngineUser appEngineUser = new AppEngineUser(user);
            ofy().save().entity(appEngineUser).now();
            // Begin new session for not using session cache.
            Objectify objectify = ofy().factory().begin();
            AppEngineUser savedUser = objectify.load().key(appEngineUser.getKey()).now();
            userId = savedUser.getUser().getUserId();
            LOG.info("Obtained the userId: " + userId);
        }
        return userId;
    }

    /**
     * Just a wrapper for Boolean.
     */
    public static final class WrappedBoolean {

        private final Boolean result;

        public WrappedBoolean(Boolean result) {
            this.result = result;
        }

        public Boolean getResult() {
            return result;
        }
    }

    /**
     * A wrapper class that can embrace a generic result or some kind of exception.
     *
     * Use this wrapper class for the return type of objectify transaction.
     * <pre>
     * {@code
     * // The transaction that returns Activity object.
     * TxResult<Activity> result = ofy().transact(new Work<TxResult<Activity>>() {
     *     public TxResult<Activity> run() {
     *         // Code here.
     *         // To throw 404
     *         return new TxResult<>(new NotFoundException("No such activity"));
     *         // To return a activity.
     *         Activity activity = somehow.getActivity();
     *         return new TxResult<>(activity);
     *     }
     * }
     * // Actually the NotFoundException will be thrown here.
     * return result.getResult();
     * </pre>
     *
     * @param <ResultType> The type of the actual return object.
     */
    private static final class TxResult<ResultType> {

        private ResultType result;

        private Throwable exception;

        private TxResult(ResultType result) {
            this.result = result;
        }

        private TxResult(Throwable exception) {
            if (exception instanceof NotFoundException ||
                    exception instanceof ForbiddenException ||
                    exception instanceof ConflictException) {
                this.exception = exception;
            } else {
                throw new IllegalArgumentException("Exception not supported.");
            }
        }

        private ResultType getResult() throws NotFoundException, ForbiddenException, ConflictException {
            if (exception instanceof NotFoundException) {
                throw (NotFoundException) exception;
            }
            if (exception instanceof ForbiddenException) {
                throw (ForbiddenException) exception;
            }
            if (exception instanceof ConflictException) {
                throw (ConflictException) exception;
            }
            return result;
        }
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud endpoints system
     * automatically inject the User object.
     *
     * @param user A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        return ofy().load().key(Key.create(Profile.class, getUserId(user))).now();
    }

    /**
     * Creates or updates a Profile object associated with the given user object.
     *
     * @param user A User object injected by the cloud endpoints.
     * @param profileForm A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    public Profile saveProfile(final User user, final ProfileForm profileForm)
            throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        String displayName = profileForm.getDisplayName();
        Gender gender = profileForm.getGender();

        Profile profile = ofy().load().key(Key.create(Profile.class, getUserId(user))).now();
        if (profile == null) {
            // Populate displayName and gender with the default values if null.
            if (displayName == null) {
                displayName = extractDefaultDisplayNameFromEmail(user.getEmail());
            }
            if (gender == null) {
                gender = Gender.You_Guess;
            }
            profile = new Profile(getUserId(user), displayName, user.getEmail(), gender);
        } else {
            profile.update(displayName, gender);
        }
        ofy().save().entity(profile).now();
        return profile;
    }

    /**
     * Creates a new Activity object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param activityForm An ActivityForm object representing user's inputs.
     * @return A newly created Activity Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createActivity", path = "activity", httpMethod = HttpMethod.POST)
    public Activity createActivity(final User user, final ActivityForm activityForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Allocate Id first, in order to make the transaction idempotent.
        Key<Profile> profileKey = Key.create(Profile.class, getUserId(user));
        final Key<Activity> activityKey = factory().allocateId(profileKey, Activity.class);
        final long activityId = activityKey.getId();
        final Queue queue = QueueFactory.getDefaultQueue();
        final String userId = getUserId(user);
        // Start a transaction.
        Activity activity = ofy().transact(new Work<Activity>() {
            @Override
            public Activity run() {
                // Fetch user's Profile.
                Profile profile = getProfileFromUser(user, userId);
                Activity activity = new Activity(activityId, userId, activityForm);
                // Save Activity and Profile.
                ofy().save().entities(activity, profile).now();
                queue.add(ofy().getTransaction(),
                        TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                        .param("email", profile.getMainEmail())
                        .param("activityInfo", activity.toString()));
                return activity;
            }
        });
        return activity;
    }

    /**
     * Updates the existing activity with the given activityId.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param activityForm An ActivityForm object representing user's inputs.
     * @param websafeActivityKey The String representation of the activity key.
     * @return Updated Activity object.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Activity with the given activityId.
     * @throws ForbiddenException when the user is not the owner of the activity.
     */
    @ApiMethod(
            name = "updateActivity",
            path = "activity/{websafeActivityKey}",
            httpMethod = HttpMethod.PUT
    )
    public Activity updateActivity(final User user, final ActivityForm activityForm,
                                       @Named("websafeActivityKey")
                                       final String websafeActivityKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        final String userId = getUserId(user);
        // Update the activity with the activityForm sent from the client.
        // Need a transaction because we need to safely preserve the number of allocated seats.
        TxResult<Activity> result = ofy().transact(new Work<TxResult<Activity>>() {
            @Override
            public TxResult<Activity> run() {
                // If there is no Activity with the id, throw a 404 error.
                Key<Activity> activityKey = Key.create(websafeActivityKey);
                Activity activity = ofy().load().key(activityKey).now();
                if (activity == null) {
                    return new TxResult<>(
                            new NotFoundException("No Activity found with the key: "
                                    + websafeActivityKey));
                }
                // If the user is not the owner, throw a 403 error.
                Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();
                if (profile == null ||
                        !activity.getOrganizerUserId().equals(userId)) {
                    return new TxResult<>(
                            new ForbiddenException("Only the owner can update the activity."));
                }
                activity.updateWithActivityForm(activityForm);
                ofy().save().entity(activity).now();
                return new TxResult<>(activity);
            }
        });
        // NotFoundException or ForbiddenException is actually thrown here.
        return result.getResult();
    }

    /**
     * Returns an Activity object with the given activityId.
     *
     * @param websafeActivityKey The String representation of the Activity Key.
     * @return a Activity object with the given activityId.
     * @throws NotFoundException when there is no Activity with the given activityId.
     */
    @ApiMethod(
            name = "getActivity",
            path = "activity/{websafeActivityKey}",
            httpMethod = HttpMethod.GET
    )
    public Activity getActivity(
            @Named("websafeActivityKey") final String websafeActivityKey)
            throws NotFoundException {
        Key<Activity> activityKey = Key.create(websafeActivityKey);
        Activity activity = ofy().load().key(activityKey).now();
        if (activity == null) {
            throw new NotFoundException("No Activity found with key: " + websafeActivityKey);
        }
        return activity;
    }

    /**
     * Returns a collection of Activity Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Activities that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getActivitysToAttend",
            path = "getActivitysToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Activity> getActivitysToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, getUserId(user))).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> keyStringsToAttend = profile.getActivityKeysToAttend();
        List<Key<Activity>> keysToAttend = new ArrayList<>();
        for (String keyString : keyStringsToAttend) {
            keysToAttend.add(Key.<Activity>create(keyString));
        }
        return ofy().load().keys(keysToAttend).values();
    }


    /**
     * Returns a list of Activities that the user created.
     * In order to receive the websafeActivityKey via the JSON params, uses a POST method.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a list of Activities that the user created.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getActivitysCreated",
            path = "getActivitysCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Activity> getActivitysCreated(final User user) throws UnauthorizedException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        String userId = getUserId(user);
        return ofy().load().type(Activity.class)
                .ancestor(Key.create(Profile.class, userId))
                .order("name").list();
    }

    /**
     * Registers to the specified Activity.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeActivityKey The String representation of the Activity Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Activity with the given activityId.
     */
    @ApiMethod(
            name = "registerForActivity",
            path = "activity/{websafeActivityKey}/registration",
            httpMethod = HttpMethod.POST
    )
    public WrappedBoolean registerForActivity(final User user,
                                         @Named("websafeActivityKey")
                                         final String websafeActivityKey)
        throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        final String userId = getUserId(user);
        TxResult<Boolean> result = ofy().transact(new Work<TxResult<Boolean>>() {
            @Override
            public TxResult<Boolean> run() {
                Key<Activity> activityKey = Key.create(websafeActivityKey);
                Activity activity = ofy().load().key(activityKey).now();
                // 404 when there is no Activity with the given activityId.
                if (activity == null) {
                    return new TxResult<>(new NotFoundException(
                            "No Activity found with key: " + websafeActivityKey));
                }
                // Registration happens here.
                Profile profile = getProfileFromUser(user, userId);
                if (profile.getActivityKeysToAttend().contains(websafeActivityKey)) {
                    return new TxResult<>(new ConflictException("You have already registered for this activity"));
                } else if (activity.getSeatsAvailable() <= 0) {
                    return new TxResult<>(new ConflictException("There are no seats available."));
                } else {
                    profile.addToActivityKeysToAttend(websafeActivityKey);
                    activity.bookSeats(1);
                    ofy().save().entities(profile, activity).now();
                    return new TxResult<>(true);
                }
            }
        });
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }

    /**
     * Unregister from the specified Activity.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeActivityKey The String representation of the Activity Key to unregister
     *                             from.
     * @return Boolean true when success, otherwise false.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Activity with the given activityId.
     */
    @ApiMethod(
            name = "unregisterFromActivity",
            path = "activity/{websafeActivityKey}/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromActivity(final User user,
                                            @Named("websafeActivityKey")
                                            final String websafeActivityKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        final String userId = getUserId(user);
        TxResult<Boolean> result = ofy().transact(new Work<TxResult<Boolean>>() {
            @Override
            public TxResult<Boolean> run() {
                Key<Activity> activityKey = Key.create(websafeActivityKey);
                Activity activity = ofy().load().key(activityKey).now();
                // 404 when there is no Activity with the given activityId.
                if (activity == null) {
                    return new TxResult<>(new NotFoundException(
                            "No Activity found with key: " + websafeActivityKey));
                }
                // Un-registering from the Activity.
                Profile profile = getProfileFromUser(user, userId);
                if (profile.getActivityKeysToAttend().contains(websafeActivityKey)) {
                    profile.unregisterFromActivity(websafeActivityKey);
                    activity.giveBackSeats(1);
                    ofy().save().entities(profile, activity).now();
                    return new TxResult<>(true);
                } else {
                    return new TxResult<>(false);
                }
            }
        });
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }
}
