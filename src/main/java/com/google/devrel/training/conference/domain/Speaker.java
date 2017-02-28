package com.google.devrel.training.conference.domain;

import com.google.devrel.training.conference.form.ProfileForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sangram on 2/25/2017.
 */
@Entity
public class Speaker {

    @Id
    private Long speakerId;

    @Index
    private String name;

    @Index
    private List<Key<Session>> sessionsScheduled = new ArrayList<Key<Session>>(0);

    private Speaker(){}

    public Speaker(Long speakerId, String name, Key<Session> sessionKey) {
        this.speakerId = speakerId;
        this.name = name;
        addSessionToSpeakersList(sessionKey);
    }

    public List<Key<Session>> getSessionsScheduled() {
        return sessionsScheduled;
    }

    public void addSessionToSpeakersList(Key<Session> sessionKey) {
        this.sessionsScheduled.add(sessionKey);
    }
}
