// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.UserInfo;
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
 * id of the users OSM account. The later is retrieved from the OSM server with a
 * <tt>GET /api/0.6/user/details</tt> request, submitted with the user name and password
 * of the current user.
 *
 * The global JosmUserStateManager listens to {@see PreferenceChangeEvent}s and keeps track
 * of what the current JOSM instance knows about the current user. Other subsystems can
 * let the global JosmUserStateManager know in case they fully identify the current user, see
 * {@see #setFullyIdentified(String, long)}.
 *
 * The information kept by the JosmUserStateManager can be used to
 * <ul>
 *   <li>safely query changesets owned by the current user based on its user id, not on its user name</li>
 *   <li>safely search for objects last touched by the current user  based on its user id, not on its user name</li>
 * </ul>
 *
 */
public class JosmUserIdentityManager implements PreferenceChangedListener{

    static private JosmUserIdentityManager instance;

    /**
     * Replies the unique instance of the JOSM user identity manager
     *
     * @return the unique instance of the JOSM user identity manager
     */
    static public JosmUserIdentityManager getInstance() {
        if (instance == null) {
            instance = new JosmUserIdentityManager();
            instance.initFromPreferences();
            Main.pref.addPreferenceChangeListener(instance);
        }
        return instance;
    }

    private String userName;
    private UserInfo userInfo;

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
        if (userName.trim().equals(""))
            throw new IllegalArgumentException(MessageFormat.format("Expected non-empty value for parameter ''{0}'', got ''{1}''", "userName", userName));
        this.userName = userName;
        userInfo = null;
    }

    /**
     * Remembers the fact that the current JOSM user is fully identified with a
     * verified pair of user name and user id.
     *
     * @param userName the user name. Must not be null. Must not be empty.
     * @param userinfo additional information about the user, retrieved from the OSM server and including the user id
     * @throws IllegalArgumentException thrown if userName is null
     * @throws IllegalArgumentException thrown if userName is empty
     * @throws IllegalArgumentException thrown if userinfo is null
     */
    public void setFullyIdentified(String username, UserInfo userinfo) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(username, "username");
        if (username.trim().equals(""))
            throw new IllegalArgumentException(tr("Expected non-empty value for parameter ''{0}'', got ''{1}''", "userName", userName));
        CheckParameterUtil.ensureParameterNotNull(userinfo, "userinfo");
        this.userName = username;
        this.userInfo = userinfo;
    }

    /**
     * Replies true if the current JOSM user is anonymous.
     *
     * @return true if the current user is anonymous.
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
     * Replies the user name of the current JOSM user. null, if {@see #isAnonymous()} is true.
     *
     * @return  the user name of the current JOSM user
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Replies the user id of the current JOSM user. 0, if {@see #isAnonymous()} or
     * {@see #isPartiallyIdentified()} is true.
     *
     * @return  the user id of the current JOSM user
     */
    public int getUserId() {
        if (userInfo == null) return 0;
        return userInfo.getId();
    }

    /**
     * Replies verified additional information about the current user if the user is
     * {@see #isFullyIdentified()}.
     *
     * @return verified additional information about the current user
     */
    public UserInfo getUserInfo() {
        return userInfo;
    }
    /**
     * Initializes the user identity manager from values in the {@see org.openstreetmap.josm.data.Preferences}
     */
    public void initFromPreferences() {
        String userName = Main.pref.get("osm-server.username");
        if (isAnonymous()) {
            if (userName != null && ! userName.trim().equals("")) {
                setPartiallyIdentified(userName);
            }
        } else {
            if (!userName.equals(this.userName)) {
                setPartiallyIdentified(userName);
            } else {
                // same name in the preferences as JOSM already knows about;
                // keep the state, be it partially or fully identified
            }
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
    public void preferenceChanged(PreferenceChangeEvent evt) {
        if (evt.getKey().equals("osm-server.username")) {
            String newValue = evt.getNewValue();
            if (newValue == null || newValue.trim().length() == 0) {
                setAnonymous();
            } else {
                if (! newValue.equals(userName)) {
                    setPartiallyIdentified(newValue);
                }
            }
            return;
        }

        if (evt.getKey().equals("osm-server.url")) {
            String newValue = evt.getNewValue();
            if (newValue == null || newValue.trim().equals("")) {
                setAnonymous();
            } else if (isFullyIdentified()) {
                setPartiallyIdentified(getUserName());
            }
        }
    }
}
