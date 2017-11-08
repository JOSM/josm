// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple class to keep a list of user names.
 *
 * Instead of storing user names as strings with every OSM primitive, we store
 * a reference to an user object, and make sure that for each username there
 * is only one user object.
 *
 * @since 227
 */
public final class User {

    private static long uidCounter;

    /**
     * the map of known users
     */
    private static Map<Long, User> userMap = new HashMap<>();

    /**
     * The anonymous user is a local user used in places where no user is known.
     * @see #getAnonymous()
     */
    private static final User ANONYMOUS = createLocalUser(tr("<anonymous>"));

    private static long getNextLocalUid() {
        uidCounter--;
        return uidCounter;
    }

    /**
     * Creates a local user with the given name
     *
     * @param name the name
     * @return a new local user with the given name
     */
    public static synchronized User createLocalUser(String name) {
        for (long i = -1; i >= uidCounter; --i) {
            User olduser = getById(i);
            if (olduser != null && olduser.hasName(name))
                return olduser;
        }
        User user = new User(getNextLocalUid(), name);
        userMap.put(user.getId(), user);
        return user;
    }

    private static User lastUser;

    /**
     * Creates a user known to the OSM server
     *
     * @param uid  the user id
     * @param name the name
     * @return a new OSM user with the given name and uid
     */
    public static synchronized User createOsmUser(long uid, String name) {

        if (lastUser != null && lastUser.getId() == uid) {
            if (name != null) {
                lastUser.setPreferredName(name);
            }
            return lastUser;
        }

        User user = userMap.computeIfAbsent(uid, k -> new User(uid, name));
        if (name != null) user.addName(name);

        lastUser = user;

        return user;
    }

    /**
     * clears the static map of user ids to user objects
     */
    public static synchronized void clearUserMap() {
        userMap.clear();
        lastUser = null;
    }

    /**
     * Returns the user with user id <code>uid</code> or null if this user doesn't exist
     *
     * @param uid the user id
     * @return the user; null, if there is no user with  this id
     */
    public static synchronized User getById(long uid) {
        return userMap.get(uid);
    }

    /**
     * Returns the list of users with name <code>name</code> or the empty list if
     * no such users exist
     *
     * @param name the user name
     * @return the list of users with name <code>name</code> or the empty list if
     * no such users exist
     */
    public static synchronized List<User> getByName(String name) {
        if (name == null) {
            name = "";
        }
        List<User> ret = new ArrayList<>();
        for (User user: userMap.values()) {
            if (user.hasName(name)) {
                ret.add(user);
            }
        }
        return ret;
    }

    /**
     * Replies the anonymous user
     * @return The anonymous user
     */
    public static User getAnonymous() {
        return ANONYMOUS;
    }

    /** the user name */
    private final LinkedHashSet<String> names = new LinkedHashSet<>();
    /** the user id */
    private final long uid;

    /**
     * Replies the user name
     *
     * @return the user name. Never <code>null</code>, but may be the empty string
     * @see #getByName(String)
     * @see #createOsmUser(long, String)
     * @see #createLocalUser(String)
     */
    public String getName() {
        return names.isEmpty() ? "" : names.iterator().next();
    }

    /**
     * Returns the list of user names
     *
     * @return list of names
     */
    public List<String> getNames() {
        return new ArrayList<>(names);
    }

    /**
     * Adds a user name to the list if it is not there, yet.
     *
     * @param name User name
     * @throws NullPointerException if name is null
     */
    public void addName(String name) {
        names.add(Objects.requireNonNull(name, "name"));
    }

    /**
     * Sets the preferred user name, i.e., the one that will be returned when calling {@link #getName()}.
     *
     * Rationale: A user can change its name multiple times and after reading various (outdated w.r.t. user name)
     * data files it is unclear which is the up-to-date user name.
     * @param name the preferred user name to set
     * @throws NullPointerException if name is null
     */
    public void setPreferredName(String name) {
        if (names.size() == 1 && names.contains(name)) {
            return;
        }
        final Collection<String> allNames = new LinkedHashSet<>(names);
        names.clear();
        names.add(Objects.requireNonNull(name, "name"));
        names.addAll(allNames);
    }

    /**
     * Returns true if the name is in the names list
     *
     * @param name User name
     * @return <code>true</code> if the name is in the names list
     */
    public boolean hasName(String name) {
        return names.contains(name);
    }

    /**
     * Replies the user id. If this user is known to the OSM server the positive user id
     * from the server is replied. Otherwise, a negative local value is replied.
     *
     * A negative local is only unique during an editing session. It is lost when the
     * application is closed and there is no guarantee that a negative local user id is
     * always bound to a user with the same name.
     *
     * @return the user id
     */
    public long getId() {
        return uid;
    }

    /**
     * Private constructor, only called from get method.
     * @param uid user id
     * @param name user name
     */
    private User(long uid, String name) {
        this.uid = uid;
        if (name != null) {
            addName(name);
        }
    }

    /**
     * Determines if this user is known to OSM
     * @return {@code true} if this user is known to OSM, {@code false} otherwise
     */
    public boolean isOsmUser() {
        return uid > 0;
    }

    /**
     * Determines if this user is local
     * @return {@code true} if this user is local, {@code false} otherwise
     */
    public boolean isLocalUser() {
        return uid < 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return uid == user.uid;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("id:").append(uid);
        if (names.size() == 1) {
            s.append(" name:").append(getName());
        } else if (names.size() > 1) {
            s.append(String.format(" %d names:%s", names.size(), getName()));
        }
        return s.toString();
    }
}
