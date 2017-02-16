package com.google.devrel.training.conference.domain;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNotDefault;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.googlecode.objectify.Key;

import java.util.Calendar;
import java.util.Date;
import java.util.List;



/**
 * Conference class stores conference information.
 */

@Entity
@Cache
public class Conference {
    public static final String DEFAULT_CITY = "Default City";
    public static final List<String> DEFAULT_TOPICS = ImmutableList.of("Default", "Topic");

    /**
     * The id for the datastore key.
     *
     * We use automatic id assignment for entities of Conference class.
     */
    @Id
    private Long id;

    /**
     * The name of the conference.
     */
    @Index
    private String name;

    /**
     * The description of the conference.
     */
    private String description;

    /**
     * Holds Profile key as the parent.
     */
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Profile> profileKey;

    /**
     * The userId of the organizer.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String organizerUserId;

    /**
     * Topics related to this conference.
     */
    @Index
    private List<String> topics;

    /**
     * The name of the city that the conference takes place.
     */
    @Index (IfNotDefault.class)
    private String city;

    /**
     * The starting date of this conference.
     */
    private Date startDate;

    /**
     * The ending date of this conference.
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
     * The maximum capacity of this conference.
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
    private  Conference() {}

    public Conference(final long id, final String organizerUserId,
                      final ConferenceForm conferenceForm) {
        Preconditions.checkNotNull(conferenceForm.getName(), "The name is required");
        this.id = id;
        this.profileKey = Key.create(Profile.class, organizerUserId);
		this.organizerUserId = organizerUserId;
        updateWithConferenceForm(conferenceForm);
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

    @ApiResourceProperty(ignored =  AnnotationBoolean.TRUE)
    public Key<Profile> getProfileKey() {
        return profileKey;
    }

    //Get a String version of the key
    public String getWebsafeKey() {
        return Key.create(profileKey, Conference.class, id).getString();
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getOrganizerUserId() {
        return organizerUserId;
    }

    public String getOrganizerDisplayName() {
        // Profile organizer = ofy().load().key(Key.create(Profile.class, organizationUserId)).now();
        Profile organizer = ofy().load().key(getProfileKey()).now();
        if(organizer == null) {
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

    public String getCity() {
        return city;
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
     * Updates the Conference with ConferenceForm.
     * This method is used upon object creation as well as updating existing Conferences.
     *
     * @param conferenceForm contains form data sent from the client.
     */
    public void updateWithConferenceForm(ConferenceForm conferenceForm) {
        this.name = conferenceForm.getName();
        this.description = conferenceForm.getDescription();
        List<String> topics = conferenceForm.getTopics();
        this.topics = topics == null || topics.isEmpty() ? DEFAULT_TOPICS : topics;
        this.city = conferenceForm.getCity() == null ? DEFAULT_CITY : conferenceForm.getCity();

        Date startDate = conferenceForm.getStartDate();
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
        Date endDate = conferenceForm.getEndDate();
        this.endDate =  endDate == null ? null : new Date(endDate.getTime());

        if (this.startDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.startDate);
            //Calendar month is 0 based
            this.month = calendar.get(calendar.MONTH) + 1;
        }

        // Check maxAttendees value is against the number of already allocated seats
        int seatsAllocated = maxAttendees - seatsAvailable;

        if (conferenceForm.getMaxAttendees() < seatsAvailable) {
            throw new IllegalArgumentException(seatsAllocated + "seats are already allocated, "
            + "but you tried to set maxAttendees to " + conferenceForm.getMaxAttendees());
        }
        // The initial number of seats available is the same as max Attendees.
        // However if there are already some seats allocated we should subtract that numbers.
        this.maxAttendees = conferenceForm.getMaxAttendees();
        this.seatsAvailable = this.maxAttendees - seatsAllocated;
    }


    public void bookSeats(final int number) {
        if (seatsAvailable < number)
            throw new IllegalArgumentException("There are no seats available");

        seatsAvailable = seatsAvailable - number;
    }

    public void giveBackSeats(final int number) {
        if (seatsAvailable + number > maxAttendees)
            throw new IllegalArgumentException("The number of seats exceeds the capacity");

        seatsAvailable = seatsAvailable + number;
    }


    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Id: " + id + "\n")
                .append("Name: ").append(name).append("\n");

        if (city != null)
            stringBuilder.append("City: ").append(city).append("\n");

        if (topics != null) {
            stringBuilder.append("Topics: \n");
            for (String topic: topics)
                stringBuilder.append("\t").append("topic").append("\n");
        }

        if (startDate != null)
            stringBuilder.append("Start Date: ").append(startDate.toString()).append("\n");

        if(endDate != null)
            stringBuilder.append("End Date: ").append(endDate.toString()).append("\n");

        stringBuilder.append("Max Attendees: ").append(maxAttendees).append("\n");

        return stringBuilder.toString();
    }

}
