package com.google.devrel.training.conference.servlet;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Joiner;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.devrel.training.conference.service.OfyService.ofy;

/**
 * Created by Sangram on 2/16/2017.
 */
public class SetAnnouncementServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Iterable<Conference> iterable = ofy().load().type(Conference.class)
                .filter("seatsAvailable <", 5)
                .filter("seatsAvailable >", 0);

        //Get names of nearly sold out conferences
        List<String> conferenceNames = new ArrayList<>(0);
        for (Conference conference: iterable) {
            conferenceNames.add(conference.getName());
        }

        if (conferenceNames.size() > 0) {
            StringBuilder announcementStringBuilder = new StringBuilder(
                    " Oh Look!! Last chance to attend!! THe following conferences are nearly sold out");
            Joiner joiner = Joiner.on(", ").skipNulls();
            announcementStringBuilder.append(joiner.join(conferenceNames));

            // Get Memcache Service
            MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();

            // Put the announcement String in memcache
            // keyed by Constants.MEMCACHE_ANNOUNCEMENTS_KEY
            String announcementKey = Constants.MEMCACHE_ANNOUNCEMENTS_KEY;
            String announcementText = announcementStringBuilder.toString();

            memcacheService.put(announcementKey, announcementText);
        }

        // Set the response status to 204 which means
        // the request was successful but there's no data to send back
        // Browser stays on the same page if the get came from the browser

        response.setStatus(204);


    }
}
