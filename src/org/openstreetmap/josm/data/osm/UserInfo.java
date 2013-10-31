// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;

public class UserInfo {
    /** the user id */
    private int id;
    /** the display name */
    private String displayName;
    /** the date this user was created */
    private Date accountCreated;
    /** the home location */
    private LatLon home;
    /** the zoom level for the home location */
    private int homeZoom;
    /** the profile description */
    private String description;
    /** the list of preferred languages */
    private List<String> languages;
    /** the number of unread messages */
    private int unreadMessages;

    /**
     * Constructs a new {@code UserInfo}.
     */
    public UserInfo() {
        id = 0;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public Date getAccountCreated() {
        return accountCreated;
    }
    public void setAccountCreated(Date accountCreated) {
        this.accountCreated = accountCreated;
    }
    public LatLon getHome() {
        return home;
    }
    public void setHome(LatLon home) {
        this.home = home;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public List<String> getLanguages() {
        return languages;
    }
    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public int getHomeZoom() {
        return homeZoom;
    }

    public void setHomeZoom(int homeZoom) {
        this.homeZoom = homeZoom;
    }

    /**
     * Replies the number of unread messages
     * @return the number of unread messages
     * @since 6349
     */
    public final int getUnreadMessages() {
        return unreadMessages;
    }

    /**
     * Sets the number of unread messages
     * @param unreadMessages the number of unread messages
     * @since 6349
     */
    public final void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }
}
