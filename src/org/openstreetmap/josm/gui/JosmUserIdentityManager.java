// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.data.preferences.StringSetting;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

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
 *
 */
public final class JosmUserIdentityManager implements PreferenceChangedListener {

    private static JosmUserIdentityManager instance;

    /**
     * Replies the unique instance of the JOSM user identity manager
     *
     * @return the unique instance of the JOSM user identity manager
     */
    public static synchronized JosmUserIdentityManager getInstance() {
        if (instance == null) {
            instance = new JosmUserIdentityManager();
            if (OsmApi.isUsingOAuth() && OAuthAccessTokenHolder.getInstance().containsAccessToken() &&
                    !Main.isOffline(OnlineResource.OSM_API)) {
                try {
                    instance.initFromOAuth();
                } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                    Logging.error(e);
                    // Fall back to preferences if OAuth identification fails for any reason
                    instance.initFromPreferences();
                }
            } else {
                instance.initFromPreferences();
            }
            Main.pref.addPreferenceChangeListener(instance);
        }
        return instance;
    }

    private String userName;
    private UserInfo userInfo;
    private boolean accessTokenKeyChanged;
    private boolean accessTokenSecretChanged;

    private JosmUserIdentityManager() {
    }

    /**
     * Remembers the fact that the current JOSM user is anonymous.
     */
    public void setAnonymous() {
        userName = null;
        userInfo = null;
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
        CheckParameterUtil.ensureParameterNotNull(userName, "userName");
        String trimmedUserName = userName.trim();
        if (trimmedUserName.isEmpty())
            throw new IllegalArgumentException(
                    MessageFormat.format("Expected non-empty value for parameter ''{0}'', got ''{1}''", "userName", userName));
        this.userName = trimmedUserName;
        userInfo = null;
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
        CheckParameterUtil.ensureParameterNotNull(userName, "userName");
        String trimmedUserName = userName.trim();
        if (trimmedUserName.isEmpty())
            throw new IllegalArgumentException(tr("Expected non-empty value for parameter ''{0}'', got ''{1}''", "userName", userName));
        CheckParameterUtil.ensureParameterNotNull(userInfo, "userInfo");
        this.userName = trimmedUserName;
        this.userInfo = userInfo;
    }

    /**
     * Replies true if the current JOSM user is anonymous.
     *
     * @return {@code true} if the current user is anonymous.
     */
    public boolean isAnonymous() {
        return userName == null && userInfo == null;
    }

    /**
     * Replies true if the current JOSM user is partially identified.
     *
     * @return true if the current JOSM user is partially identified.
     */
    public boolean isPartiallyIdentified() {
        return userName != null && userInfo == null;
    }

    /**
     * Replies true if the current JOSM user is fully identified.
     *
     * @return true if the current JOSM user is fully identified.
     */
    public boolean isFullyIdentified() {
        return userName != null && userInfo != null;
    }

    /**
     * Replies the user name of the current JOSM user. null, if {@link #isAnonymous()} is true.
     *
     * @return  the user name of the current JOSM user
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Replies the user id of the current JOSM user. 0, if {@link #isAnonymous()} or
     * {@link #isPartiallyIdentified()} is true.
     *
     * @return  the user id of the current JOSM user
     */
    public int getUserId() {
        if (userInfo == null) return 0;
        return userInfo.getId();
    }

    /**
     * Replies verified additional information about the current user if the user is
     * {@link #isFullyIdentified()}.
     *
     * @return verified additional information about the current user
     */
    public UserInfo getUserInfo() {
        return userInfo;
    }

    /**
     * Returns the identity as a {@link User} object
     *
     * @return the identity as user, or {@link User#getAnonymous()} if {@link #isAnonymous()}
     */
    public User asUser() {
        return isAnonymous() ? User.getAnonymous() : User.createOsmUser(userInfo != null ? userInfo.getId() : 0, userName);
    }

    /**
     * Initializes the user identity manager from Basic Authentication values in the {@link org.openstreetmap.josm.data.Preferences}
     * This method should be called if {@code osm-server.auth-method} is set to {@code basic}.
     * @see #initFromOAuth
     */
    public void initFromPreferences() {
        String userName = CredentialsManager.getInstance().getUsername();
        if (isAnonymous()) {
            if (userName != null && !userName.trim().isEmpty()) {
                setPartiallyIdentified(userName);
            }
        } else {
            if (userName != null && !userName.equals(this.userName)) {
                setPartiallyIdentified(userName);
            }
            // else: same name in the preferences as JOSM already knows about.
            // keep the state, be it partially or fully identified
        }
    }

    /**
     * Initializes the user identity manager from OAuth request of user details.
     * This method should be called if {@code osm-server.auth-method} is set to {@code oauth}.
     * @see #initFromPreferences
     * @since 5434
     */
    public void initFromOAuth() {
        try {
            UserInfo info = new OsmServerUserInfoReader().fetchUserInfo(NullProgressMonitor.INSTANCE);
            setFullyIdentified(info.getDisplayName(), info);
        } catch (IllegalArgumentException | OsmTransferException e) {
            Logging.error(e);
        }
    }

    /**
     * Replies true if the user with name <code>username</code> is the current user
     *
     * @param username the user name
     * @return true if the user with name <code>username</code> is the current user
     */
    public boolean isCurrentUser(String username) {
        return this.userName != null && this.userName.equals(username);
    }

    /**
     * Replies true if the current user is {@link #isFullyIdentified() fully identified} and the {@link #getUserId() user ids} match,
     * or if the current user is not {@link #isFullyIdentified() fully identified} and the {@link #userName user names} match.
     *
     * @param user the user to test
     * @return true if given user is the current user
     */
    public boolean isCurrentUser(User user) {
        if (user == null) {
            return false;
        } else if (isFullyIdentified()) {
            return getUserId() == user.getId();
        } else {
            return isCurrentUser(user.getName());
        }
    }

    /* ------------------------------------------------------------------- */
    /* interface PreferenceChangeListener                                  */
    /* ------------------------------------------------------------------- */
    @Override
    public void preferenceChanged(PreferenceChangeEvent evt) {
        switch (evt.getKey()) {
        case "osm-server.username":
            String newUserName = null;
            if (evt.getNewValue() instanceof StringSetting) {
                newUserName = ((StringSetting) evt.getNewValue()).getValue();
            }
            if (newUserName == null || newUserName.trim().isEmpty()) {
                setAnonymous();
            } else {
                if (!newUserName.equals(userName)) {
                    setPartiallyIdentified(newUserName);
                }
            }
            return;
        case "osm-server.url":
            String newUrl = null;
            if (evt.getNewValue() instanceof StringSetting) {
                newUrl = ((StringSetting) evt.getNewValue()).getValue();
            }
            if (newUrl == null || newUrl.trim().isEmpty()) {
                setAnonymous();
            } else if (isFullyIdentified()) {
                setPartiallyIdentified(getUserName());
            }
            break;
        case "oauth.access-token.key":
            accessTokenKeyChanged = true;
            break;
        case "oauth.access-token.secret":
            accessTokenSecretChanged = true;
            break;
        default: // Do nothing
        }

        if (accessTokenKeyChanged && accessTokenSecretChanged) {
            accessTokenKeyChanged = false;
            accessTokenSecretChanged = false;
            if (OsmApi.isUsingOAuth()) {
                getInstance().initFromOAuth();
            }
        }
    }
}
