package com.google.devrel.training.conference.domain;

/**
 * Created by Sangram on 2/16/2017.
 */
public class Announcement {

    private String message;

    public Announcement() {}

    public  Announcement(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
