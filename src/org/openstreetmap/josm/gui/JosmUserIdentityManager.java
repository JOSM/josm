// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.UserInfo;

/**
 * JosmUserIdentityManager is a global object which keeps track of what JOSM knows about
 * the identity of the current user.
 *
 * JOSM can be operated anonymously provided the current user never invokes an operation
 * on the OSM server which required authentication. In this case JOSM neither knows
 * the user name of the OSM account of the current user nor its unique id. Perhaps the
 * user doesn't have one.
 *
 * If the current user supplies a user name and a password in the JOSM preferences JOSM
 * can partially identify the user.
 *
 * The current user is fully identified if JOSM knows both the user name and the unique
 * id of the users OSM account. The latter is retrieved from the OSM server with a
 * <tt>GET /api/0.6/user/details</tt> request, submitted with the user name and password
 * of the current user.
 *
 * The global JosmUserIdentityManager listens to {@link PreferenceChangeEvent}s and keeps track
 * of what the current JOSM instance knows about the current user. Other subsystems can
 * let the global JosmUserIdentityManager know in case they fully identify the current user, see
 * {@link #setFullyIdentified}.
 *
 * The information kept by the JosmUserIdentityManager can be used to
 * <ul>
 *   <li>safely query changesets owned by the current user based on its user id, not on its user name</li>
 *   <li>safely search for objects last touched by the current user based on its user id, not on its user name</li>
 * </ul>
 * @since 2689 (creation)
 * @deprecated to be removed end of 2017. Use {@link UserIdentityManager} instead
 */
@Deprecated
public final class JosmUserIdentityManager {

    private static JosmUserIdentityManager instance;

    /**
     * Replies the unique instance of the JOSM user identity manager
     *
     * @return the unique instance of the JOSM user identity manager
     */
    public static synchronized JosmUserIdentityManager getInstance() {
        if (instance == null) {
            instance = new JosmUserIdentityManager();
            UserIdentityManager.getInstance();
        }
        return instance;
    }

    private JosmUserIdentityManager() {
    }

    /**
     * Remembers the fact that the current JOSM user is anonymous.
     */
    public void setAnonymous() {
        UserIdentityManager.getInstance().setAnonymous();
    }

    /**
     * Remembers the fact that the current JOSM user is partially identified
     * by the user name of its OSM account.
     *
     * @param userName the user name. Must not be null. Must not be empty (whitespace only).
     * @throws IllegalArgumentException if userName is null
     * @throws IllegalArgumentException if userName is empty
     */
    public void setPartiallyIdentified(String userName) {
        UserIdentityManager.getInstance().setPartiallyIdentified(userName);
    }

    /**
     * Remembers the fact that the current JOSM user is fully identified with a
     * verified pair of user name and user id.
     *
     * @param userName the user name. Must not be null. Must not be empty.
     * @param userInfo additional information about the user, retrieved from the OSM server and including the user id
     * @throws IllegalArgumentException if userName is null
     * @throws IllegalArgumentException if userName is empty
     * @throws IllegalArgumentException if userInfo is null
     */
    public void setFullyIdentified(String userName, UserInfo userInfo) {
        UserIdentityManager.getInstance().setFullyIdentified(userName, userInfo);
    }

    /**
     * Replies true if the current JOSM user is anonymous.
     *
     * @return {@code true} if the current user is anonymous.
     */
    public boolean isAnonymous() {
        return UserIdentityManager.getInstance().isAnonymous();
    }

    /**
     * Replies true if the current JOSM user is partially identified.
     *
     * @return true if the current JOSM user is partially identified.
     */
    public boolean isPartiallyIdentified() {
        return UserIdentityManager.getInstance().isPartiallyIdentified();
    }

    /**
     * Replies true if the current JOSM user is fully identified.
     *
     * @return true if the current JOSM user is fully identified.
     */
    public boolean isFullyIdentified() {
        return UserIdentityManager.getInstance().isFullyIdentified();
    }

    /**
     * Replies the user name of the current JOSM user. null, if {@link #isAnonymous()} is true.
     *
     * @return  the user name of the current JOSM user
     */
    public String getUserName() {
        return UserIdentityManager.getInstance().getUserName();
    }

    /**
     * Replies the user id of the current JOSM user. 0, if {@link #isAnonymous()} or
     * {@link #isPartiallyIdentified()} is true.
     *
     * @return  the user id of the current JOSM user
     */
    public int getUserId() {
        return UserIdentityManager.getInstance().getUserId();
    }

    /**
     * Replies verified additional information about the current user if the user is
     * {@link #isFullyIdentified()}.
     *
     * @return verified additional information about the current user
     */
    public UserInfo getUserInfo() {
        return UserIdentityManager.getInstance().getUserInfo();
    }

    /**
     * Returns the identity as a {@link User} object
     *
     * @return the identity as user, or {@link User#getAnonymous()} if {@link #isAnonymous()}
     */
    public User asUser() {
        return UserIdentityManager.getInstance().asUser();
    }

    /**
     * Initializes the user identity manager from Basic Authentication values in the {@link org.openstreetmap.josm.data.Preferences}
     * This method should be called if {@code osm-server.auth-method} is set to {@code basic}.
     * @see #initFromOAuth
     */
    public void initFromPreferences() {
        UserIdentityManager.getInstance().initFromPreferences();
    }

    /**
     * Initializes the user identity manager from OAuth request of user details.
     * This method should be called if {@code osm-server.auth-method} is set to {@code oauth}.
     * @see #initFromPreferences
     * @since 5434
     */
    public void initFromOAuth() {
        UserIdentityManager.getInstance().initFromOAuth();
    }

    /**
     * Replies true if the user with name <code>username</code> is the current user
     *
     * @param username the user name
     * @return true if the user with name <code>username</code> is the current user
     */
    public boolean isCurrentUser(String username) {
        return UserIdentityManager.getInstance().isCurrentUser(username);
    }

    /**
     * Replies true if the current user is {@link #isFullyIdentified() fully identified} and the {@link #getUserId() user ids} match,
     * or if the current user is not {@link #isFullyIdentified() fully identified} and the {@link #getUserName() user names} match.
     *
     * @param user the user to test
     * @return true if given user is the current user
     */
    public boolean isCurrentUser(User user) {
        return UserIdentityManager.getInstance().isCurrentUser(user);
    }
}
