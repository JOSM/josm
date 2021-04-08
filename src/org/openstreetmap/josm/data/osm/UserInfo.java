// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.time.Instant;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Public user information.
 * @since 2115
 */
public class UserInfo {
    /** the user id */
    private int id;
    /** the display name */
    private String displayName;
    /** the date this user was created */
    private Instant accountCreated;
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

    /**
     * Returns the user identifier.
     * @return the user identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the user identifier.
     * @param id the user identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the display name.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     * @param displayName display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the date at which the account has been created.
     * @return the user account creation date
     */
    public Instant getAccountCreated() {
        return accountCreated;
    }

    /**
     * Sets the date at which the account has been created.
     * @param accountCreated user account creation date
     */
    public void setAccountCreated(Instant accountCreated) {
        this.accountCreated = accountCreated;
    }

    /**
     * Returns the user home coordinates, if set.
     * @return the user home lat/lon or null
     */
    public LatLon getHome() {
        return home;
    }

    /**
     * Sets the user home coordinates.
     * @param home user home lat/lon or null
     */
    public void setHome(LatLon home) {
        this.home = home;
    }

    /**
     * Returns the public account description.
     * @return the public account description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the public account description.
     * @param description public account description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the list of preferred languages.
     * @return the list of preferred languages
     */
    public List<String> getLanguages() {
        return languages;
    }

    /**
     * Sets the list of preferred languages.
     * @param languages list of preferred languages
     */
    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    /**
     * Returns the user home zoom level.
     * @return the user home zoom level
     */
    public int getHomeZoom() {
        return homeZoom;
    }

    /**
     * Sets the user home zoom level.
     * @param homeZoom user home zoom level
     */
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
