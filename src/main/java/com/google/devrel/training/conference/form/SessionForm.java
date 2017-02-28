package com.google.devrel.training.conference.form;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Sangram on 2/25/2017.
 */
public class SessionForm {

    private String sessionName;

    private String sessionHighlight;

    private String typeOfSession;

    private Date sessionDate;

    private Date startTime;

    private Date sessionDuration;

    private List<String> speakers = new ArrayList<String>(0);

    private SessionForm(){}

    public SessionForm(String sessionName, String sessionHighlight,
                       String typeOfSession, List<String> speakers, Date sessionDate,
                       Date startTime, Date sessionDuration) {
        this.sessionName = sessionName;
        this.sessionHighlight = sessionHighlight;
        this.typeOfSession = typeOfSession;
        this.speakers.addAll(speakers);
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.sessionDuration = sessionDuration;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getSessionHighlight() {
        return sessionHighlight;
    }

    public String getTypeOfSession() {
        return typeOfSession;
    }

    public Date getSessionDate() {
        return sessionDate;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getSessionDuration() {
        return sessionDuration;
    }

    public List<String> getSpeakers() {
        return speakers;
    }
}
