package com.google.devrel.training.conference.domain;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.repackaged.com.google.common.base.Preconditions;
import com.google.devrel.training.conference.form.SessionForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.devrel.training.conference.service.OfyService.ofy;

/**
 * Created by Sangram on 2/24/2017.
 */
@Entity
public class Session {

    /**
     * The id for Datastore key;
     *
     * Use automatic Id assignment for each session.
     */
    @Id
    private Long id;

    /**
     * Session name
     */
    @Index
    private String sessionName;

    /**
     * A short description of the session
     */
    private String sessionHighlights;

    /**
     * Holds the Conference key as the parent.
     */
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Conference> conferenceKey;

    /**
     * The user Id of the speaker for the session.
     */
    @Index
    private List<String> speakerUserIds = new ArrayList<String>(0);

    /**
     * The type of the session the user has.
     */
    @Index
    private String typeOfSession;

    /**
     * The date the session is scheduled.
     */
    @Index
    private Date sessionDate;

    /**
     * The start time of the session.
     */
    @Index
    private Date startTime;

    /**
     * The duration of the session.
     */
    @Index
    private Date sessionDuration;


    private Session() {}

    public Session(final long id, final Key<Conference> conferenceKey,
                   final SessionForm sessionForm
                   ) {
        Preconditions.checkNotNull(sessionForm.getSessionName(), "Name is required");
        this.id = id;
        this.conferenceKey = conferenceKey;
        updateWithSessionForm(sessionForm);
    }

    private void updateWithSessionForm(SessionForm sessionForm) {
        this.sessionName = sessionForm.getSessionName();
        this.sessionHighlights = sessionForm.getSessionHighlight();
        this.sessionDate = sessionForm.getSessionDate();
        this.startTime = sessionForm.getStartTime();
        this.sessionDuration = sessionForm.getSessionDuration();
        this.typeOfSession = sessionForm.getTypeOfSession();
        speakerUserIds.addAll(sessionForm.getSpeakers());
    }


    public Long getId() {
        return id;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getSessionHighlights() {
        return sessionHighlights;
    }

    public Key<Conference> getConferenceKey() {
        return conferenceKey;
    }

    public List<String> getSpeakerUserIds() {
        return speakerUserIds;
    }

    public String getTypeOfSession() {
        return typeOfSession;
    }

    public Date getSessionDate() {
        return sessionDate == null ? null : new Date(sessionDate.getTime());
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getSessionDuration() {
        return sessionDuration;
    }

    public String getConferenceName() {
        Conference conference = ofy().load().key(getConferenceKey()).now();
        if (conference == null)
            return ""; //todo: return something meaningful
        else
            return conference.getName();
    }
}
