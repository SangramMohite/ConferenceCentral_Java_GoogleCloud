package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.factory;
import static com.google.devrel.training.conference.service.OfyService.ofy;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.*;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.form.SessionForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Defines conference APIs.
 */
@Api(name = "conference",
        version = "v1",
        scopes = {Constants.EMAIL_SCOPE, "https://www.googleapis.com/auth/plus.login email"},
        clientIds =
                {
                Constants.WEB_CLIENT_ID,
                Constants.API_EXPLORER_CLIENT_ID,
                Constants.ANDROID_CLIENT_ID
                },
        audiences = {Constants.ANDROID_AUDIENCE},
        description = "API for the Conference Central Backend application.")
public class ConferenceApi {

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * This is an ugly workaround for null userId for Android clients.
     *
     * @param user A User object injected by the cloud endpoints.
     * @return the App Engine userId for the user.
     */
    private static String getUserId(User user) {
        String userId = user.getUserId();
        Logger Log = Logger.getLogger(ConferenceApi.class.getName());
        if (userId == null) {
            Log.info("userId is null, so trying to obtain it from the datastore.");
            AppEngineUser appEngineUser = new AppEngineUser(user);
            ofy().save().entity(appEngineUser).now();
            // Begin new session for not using session cache.
            Objectify objectify = ofy().factory().begin();
            AppEngineUser savedUser = objectify.load().key(appEngineUser.getKey()).now();
            userId = savedUser.getUser().getUserId();
            Log.info("Obtained the userId: " + userId);
        }
        return userId;
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user        A User object injected by the cloud endpoints.
     * @param profileForm A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException when the User object is null.
     */


    // Declare this method as a method available externally through Endpoints
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    // The request that invokes this method should provide data that
    // conforms to the fields defined in ProfileForm

    // TODO 1 Pass the ProfileForm parameter
    // TODO 2 Pass the User parameter
    public Profile saveProfile(final User user, ProfileForm profileForm) throws UnauthorizedException {
        // If the user is not logged in, throw an UnauthorizedException
        if (user == null) {
            throw new UnauthorizedException("Authorization Required");

        }
        // Get the userId and mainEmail
        String mainEmail = user.getEmail();
        String userId = getUserId(user);

        // Set the teeShirtSize to the value sent by the ProfileForm, if sent
        // otherwise leave it as the default value
//        if (profileForm.getTeeShirtSize() != null)
        TeeShirtSize teeShirtSize = profileForm.getTeeShirtSize();

        // Set the displayName to the value sent by the ProfileForm, if sent
        // otherwise set it to null
        String displayName = profileForm.getDisplayName();

        // Get the existing profile if it exists.
        Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

        if (profile != null) {
            profile.update(displayName, teeShirtSize);
//            if (displayName != null)
//                profile.updateDisplayName(displayName);
//            if (teeShirtSize != null)
//                profile.updateTeeShirtSize(teeShirtSize);
        } else {
            // If the displayName is null, set it to default value based on the user's email
            // by calling extractDefaultDisplayNameFromEmail(...)
            if (displayName == null) {
                displayName = extractDefaultDisplayNameFromEmail(mainEmail);
            }
            if (profileForm.getTeeShirtSize() == null) {
                teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
            }
            // Create a new Profile entity from the
            // userId, displayName, mainEmail and teeShirtSize
            profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
        }

        // TODO 3 (In Lesson 3)
        // Save the Profile entity in the datastore

        ofy().save().entity(profile).now();
        // Return the profile
        return profile;
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
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

        // TODO
        // load the Profile Entity
        String userId = getUserId(user);
        Key key = Key.create(Profile.class, userId);
        return (Profile) ofy().load().key(key).now();

    }

    /**
     * Gets the Profile entity for the current user
     * or creates it if it doesn't exist
     *
     * @param user the logged in user
     * @return user's Profile
     */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, getUserId(user))).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String email = user.getEmail();
            profile = new Profile(getUserId(user),
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
        }
        return profile;
    }

    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user           A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
            throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId of the logged in User
        final String userId = getUserId(user);

        // Get the key for the User's Profile
        Key<Profile> profileKey = Key.create(Profile.class, userId);

        // Allocate a key for the conference -- let App Engine allocate the ID
        // Don't forget to include the parent Profile in the allocated ID
        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        // Get the Conference Id from the Key
        final long conferenceId = conferenceKey.getId();
//        final Queue queue = QueueFactory.getQueue("email-queue");

        final Queue queue = QueueFactory.getDefaultQueue();

        // Get the existing Profile entity for the current user if there is one
        // Otherwise create a new Profile entity with default values

        Conference conference = ofy().transact(new Work<Conference>() {
            @Override
            public Conference run() {

                Profile profile = getProfileFromUser(user);
                if (profile == null) {
                    String email = user.getEmail();
                    profile = new Profile(getUserId(user),
                            extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
                }

                // TODO (Lesson 4)
                // Create a new Conference Entity, specifying the user's Profile entity
                // as the parent of the conference
                Conference conference = new Conference(conferenceId, userId, conferenceForm);

                // TODO (Lesson 4)
                // Save Conference and Profile Entities
//        ofy().save().entities(profile, conference).now();
                ofy().save().entity(profile).now();
                ofy().save().entity(conference).now();
                queue.add(ofy().getTransaction(),
                        TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                                .param("email", profile.getMainEmail())
                                .param("conferenceInfo", conference.toString()));
                return conference;
            }
        });

        return conference;
    }

    /**
     * Get all the conference objects from the datastore sorted b conference name.
     *
     * @return list of all the created Conference Object sorted by conference name.
     */
    @ApiMethod(name = "queryConferences", path = "queryConferences", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {
        //
//        Query<Conference> query = ofy().load().type(Conference.class).order("name");
//        return conferenceQueryForm.getQuery().list();

        Iterable<Conference> conferenceItearable = conferenceQueryForm.getQuery();
        List result = new ArrayList<>(0);
        List organizersKey = new ArrayList<>(0);

        for (Conference conference : conferenceItearable) {
            organizersKey.add(Key.create(Profile.class, conference.getOrganizerUserId()));
            result.add(conference);
        }
        // To avoid separate datastore gets for each conference, pre-fetch the Profiles.
        ofy().load().keys(organizersKey);

        return result;
    }

    /**
     * Get all the conferences created by the user.
     *
     * @param user the logged in user
     * @return list of all the conferences created by the user.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getConferencesCreated",
            path = "getConferencesCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user)
            throws UnauthorizedException {
        if (user == null)
            throw new UnauthorizedException("Authorization required");

        String userId = getUserId(user);
        Key key = Key.create(Profile.class, userId);
        Query<Conference> query = ofy().load().type(Conference.class).order("name").ancestor(key);

        return query.list();
    }

    public List<Conference> filterPlayground() {

        Query<Conference> query = ofy().load().type(Conference.class);
        query = query.filter("city =", "Tokyo");
        query = query.filter("seatsAvailable >", 0).
                filter("seatsAvailable <", 10).
                order("seatsAvailable").
                order("name").
                order("month");

        return query.list();
    }

    /**
     * Returns a Conference object with the given conferenceId.
     *
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return a Conference object with the given conferenceId.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "getConference",
            path = "conference/{websafeConferenceKey}",
            httpMethod = HttpMethod.GET
    )
    public Conference getConference(
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws NotFoundException {

        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null)
            throw new NotFoundException("No Conference found with the conference key: " + websafeConferenceKey);

        return conference;
    }


    public static class WrappedBoolean {
        private final Boolean result;
        private final String reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }

    }


    /**
     * Register to attend the specified Conference.
     *
     * @param user                 An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException     when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "registerForConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.POST
    )

    public WrappedBoolean registerForConference(final User user,
                                                @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException,
            ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        final String userId = getUserId(user);

        // TODO
        // Start transaction
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {

                    // TODO
                    // Get the conference key -- you can get it from websafeConferenceKey
                    // Will throw ForbiddenException if the key cannot be created
                    Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                    // TODO
                    // Get the Conference entity from the datastore
                    Conference conference = ofy().load().key(conferenceKey).now();

                    // 404 when there is no Conference with the given conferenceId.
                    if (conference == null) {
                        return new WrappedBoolean(false,
                                "No Conference found with key: "
                                        + websafeConferenceKey);
                    }

                    // TODO
                    // Get the user's Profile entity
                    Profile profile = getProfile(user);

                    // Has the user already registered to attend this conference?
                    if (profile.getConferenceKeysToAttend().contains(
                            websafeConferenceKey)) {
                        return new WrappedBoolean(false, "Already registered");
                    } else if (conference.getSeatsAvailable() <= 0) {
                        return new WrappedBoolean(false, "No seats available");
                    } else {
                        // All looks good, go ahead and book the seat

                        // TODO
                        // Add the websafeConferenceKey to the profile's
                        // conferencesToAttend property
                        profile.addToConferenceKeysToAttend(websafeConferenceKey);

                        // TODO
                        // Decrease the conference's seatsAvailable
                        // You can use the bookSeats() method on Conference
                        conference.bookSeats(1);

                        // TODO
                        // Save the Conference and Profile entities
                        ofy().save().entities(profile, conference).now();

                        // We are booked!
                        return new WrappedBoolean(true, "Registration successful");
                    }

                } catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }

        });


        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException(result.getReason());
            } else if (result.getReason() == "Already registered") {
                throw new ConflictException("You have already registered");
            } else if (result.getReason() == "No seats available") {
                throw new ConflictException("There are no seats available");
            } else {
                throw new ForbiddenException("Unknown exception");
            }
        }
        return result;
    }

    /**
     * Returns a collection of Conference Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Conferences that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getConferencesToAttend",
            path = "getConferencesToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Conference> getConferencesToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // TODO
        // Get the Profile entity for the user
        Profile profile = getProfile(user); // Change this;
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }

        // TODO
        // Get the value of the profile's conferenceKeysToAttend property
        List<String> keyStringsToAttend = profile.getConferenceKeysToAttend(); // change this

        // TODO
        // Iterate over keyStringsToAttend,
        // and return a Collection of the
        // Conference entities that the user has registered to attend
        List<Conference> conferences = new ArrayList<>(0);
        for (String keyStringToAttend : keyStringsToAttend) {
            conferences.add(getConference(keyStringToAttend));
        }
        return conferences;  // change this
    }

    /**
     * @param user                 An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey the unique key used for each conference
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException thrown if the user is not logged in
     * @throws NotFoundException     thrown if the conference does not exist
     * @throws ForbiddenException    Unknown exception
     * @throws ConflictException     thrown if the user is not registered but tries to unregister from it
     */
    @ApiMethod(
            name = "unregisterFromConference",
            path = "conference/websafeConferenceKey/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromConference(final User user,
                                                   @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {

        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO
        // 1. get user from user id

        final String userId = getUserId(user);

        // 2. start transaction to unregister
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {
                    // 3. find the conference using websafeConferenceKey
                    Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
                    Conference conference = ofy().load().key(conferenceKey).now();
                    // 404 when there is no Conference with the given conferenceId.
                    if (conference == null) {
                        return new WrappedBoolean(false,
                                "No Conference found with key: "
                                        + websafeConferenceKey);
                    }

                    Profile profile = getProfile(user);

                    if (!profile.getConferenceKeysToAttend().contains(
                            websafeConferenceKey)) {
                        return new WrappedBoolean(false, "Not registered");
                    } else if (conference.getSeatsAvailable() >= conference.getMaxAttendees()) {
                        return new WrappedBoolean(false, "Conference is already full");
                    } else {
                        profile.unregisterFromConference(websafeConferenceKey);
                        conference.giveBackSeats(1);
                        ofy().save().entities(profile, conference).now();
                        return new WrappedBoolean(true, "Unregistered from Conference");
                    }
                } catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }
        });

        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException(result.getReason());
            } else if (result.getReason() == "Not registered") {
                throw new ConflictException("You have not registered for the conference");
            } else if (result.getReason() == "Conference is already full") {
                throw new ConflictException("Conference is already full");
            } else {
                throw new ForbiddenException("Unknown exception");
            }
        }

        return result;
    }

    @ApiMethod(
            name = "getAnnouncement",
            path = "announcement",
            httpMethod = HttpMethod.GET
    )
    public Announcement getAnnouncement() {

        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
        String announcementKey = Constants.MEMCACHE_ANNOUNCEMENTS_KEY;

        Object announcementObject = memcacheService.get(announcementKey);
        if (announcementObject != null)
            return new Announcement(announcementObject.toString());

        return null;
    }

    @ApiMethod(
            name = "createSession",
            path = "conference/{websafeConferenceKey}/createSession",
            httpMethod = HttpMethod.POST
    )

    public Session createSession(final SessionForm sessionForm,
                                 @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws NotFoundException {

        final Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        final Key<Session> sessionKey = factory().allocateId(conferenceKey, Session.class);
        final long sessionId = sessionKey.getId();

        Session session = ofy().transact(new Work<Session>() {
            @Override
            public Session run() {
                Conference conference = ofy().load().key(conferenceKey).now();
                if (conference == null) {
                    return null;
                }
                List<String> speakers = sessionForm.getSpeakers();

                for (String speakersName : speakers) {
                    Key<Speaker> speakerKey = Key.create(Speaker.class, speakersName);
                    Speaker speaker = ofy().load().key(speakerKey).now();

                    if (speaker == null) {
                        speakerKey = factory().allocateId(Speaker.class);
                        final long speakerId = speakerKey.getId();
                        speaker = new Speaker(speakerId, speakersName, sessionKey);
                        ofy().save().entity(speaker).now();
                    } else {
                        speaker.addSessionToSpeakersList(sessionKey);
                        ofy().save().entity(speaker).now();
                    }
                }

                Session session = new Session(sessionId, conferenceKey, sessionForm);
                ofy().save().entity(conference).now();
                ofy().save().entity(session).now();
                return session;
            }
        });

        if (session == null) {
            throw new NotFoundException("No conference with key: " + websafeConferenceKey);
        } else
            return session;
    }
}