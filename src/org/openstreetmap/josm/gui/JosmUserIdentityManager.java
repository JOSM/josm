// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.text.MessageFormat;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.Preferences.StringSetting;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * JosmUserStateManager is a global object which keeps track of what JOSM knows about
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
 * The global JosmUserStateManager listens to {@link PreferenceChangeEvent}s and keeps track
 * of what the current JOSM instance knows about the current user. Other subsystems can
 * let the global JosmUserStateManager know in case they fully identify the current user, see
 * {@link #setFullyIdentified}.
 *
 * The information kept by the JosmUserStateManager can be used to
 * <ul>
 *   <li>safely query changesets owned by the current user based on its user id, not on its user name</li>
 *   <li>safely search for objects last touched by the current user based on its user id, not on its user name</li>
 * </ul>
 *
 */
public final class JosmUserIdentityManager implements PreferenceChangedListener{

    static private JosmUserIdentityManager instance;

    /**
     * Replies the unique instance of the JOSM user identity manager
     *
     * @return the unique instance of the JOSM user identity manager
     */
    static public JosmUserIdentityManager getInstance() {
        if (instance == null) {
            instance = new JosmUserIdentityManager();
            if (OsmApi.isUsingOAuth() && OAuthAccessTokenHolder.getInstance().containsAccessToken()) {
                try {
                    instance.initFromOAuth(Main.parent);
                } catch (Throwable t) {
                    Main.error(t);
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
     * Remebers the fact that the current JOSM user is partially identified
     * by the user name of its OSM account.
     *
     * @param userName the user name. Must not be null. Must not be empty (whitespace only).
     * @throws IllegalArgumentException thrown if userName is null
     * @throws IllegalArgumentException thrown if userName is empty
     */
    public void setPartiallyIdentified(String userName) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(userName, "userName");
        if (userName.trim().isEmpty())
            throw new IllegalArgumentException(MessageFormat.format("Expected non-empty value for parameter ''{0}'', got ''{1}''", "userName", userName));
        this.userName = userName;
        userInfo = null;
    }

    /**
     * Remembers the fact that the current JOSM user is fully identified with a
     * verified pair of user name and user id.
     *
     * @param username the user name. Must not be null. Must not be empty.
     * @param userinfo additional information about the user, retrieved from the OSM server and including the user id
     * @throws IllegalArgumentException thrown if userName is null
     * @throws IllegalArgumentException thrown if userName is empty
     * @throws IllegalArgumentException thrown if userinfo is null
     */
    public void setFullyIdentified(String username, UserInfo userinfo) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(username, "username");
        if (username.trim().isEmpty())
            throw new IllegalArgumentException(tr("Expected non-empty value for parameter ''{0}'', got ''{1}''", "userName", userName));
        CheckParameterUtil.ensureParameterNotNull(userinfo, "userinfo");
        this.userName = username;
        this.userInfo = userinfo;
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
            } else {
                // same name in the preferences as JOSM already knows about.
                // keep the state, be it partially or fully identified
            }
        }
    }

    /**
     * Initializes the user identity manager from OAuth request of user details.
     * This method should be called if {@code osm-server.auth-method} is set to {@code oauth}.
     * @param parent component relative to which the {@link PleaseWaitDialog} is displayed.
     * @see #initFromPreferences
     * @since 5434
     */
    public void initFromOAuth(Component parent) {
        try {
            UserInfo info = new OsmServerUserInfoReader().fetchUserInfo(NullProgressMonitor.INSTANCE);
            setFullyIdentified(info.getDisplayName(), info);
        } catch (IllegalArgumentException e) {
            Main.error(e);
        } catch (OsmTransferException e) {
            Main.error(e);
        }
    }

    /**
     * Replies true if the user with name <code>username</code> is the current
     * user
     *
     * @param username the user name
     * @return true if the user with name <code>username</code> is the current
     * user
     */
    public boolean isCurrentUser(String username) {
        if (username == null || this.userName == null) return false;
        return this.userName.equals(username);
    }

    /* ------------------------------------------------------------------- */
    /* interface PreferenceChangeListener                                  */
    /* ------------------------------------------------------------------- */
    @Override
    public void preferenceChanged(PreferenceChangeEvent evt) {
        if (evt.getKey().equals("osm-server.username")) {
            if (!(evt.getNewValue() instanceof StringSetting)) return;
            String newValue = ((StringSetting) evt.getNewValue()).getValue();
            if (newValue == null || newValue.trim().length() == 0) {
                setAnonymous();
            } else {
                if (! newValue.equals(userName)) {
                    setPartiallyIdentified(newValue);
                }
            }
            return;

        } else if (evt.getKey().equals("osm-server.url")) {
            if (!(evt.getNewValue() instanceof StringSetting)) return;
            String newValue = ((StringSetting) evt.getNewValue()).getValue();
            if (newValue == null || newValue.trim().isEmpty()) {
                setAnonymous();
            } else if (isFullyIdentified()) {
                setPartiallyIdentified(getUserName());
            }

        } else if (evt.getKey().equals("oauth.access-token.key")) {
            accessTokenKeyChanged = true;

        } else if (evt.getKey().equals("oauth.access-token.secret")) {
            accessTokenSecretChanged = true;
        }

        if (accessTokenKeyChanged && accessTokenSecretChanged) {
            accessTokenKeyChanged = false;
            accessTokenSecretChanged = false;
            if (OsmApi.isUsingOAuth()) {
                try {
                    instance.initFromOAuth(Main.parent);
                } catch (Throwable t) {
                    Main.error(t);
                }
            }
        }
    }
}
