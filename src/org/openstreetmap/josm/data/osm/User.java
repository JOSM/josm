// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.openstreetmap.josm.tools.Utils;

/**
 * A simple class to keep a list of user names.
 *
 * Instead of storing user names as strings with every OSM primitive, we store
 * a reference to an user object, and make sure that for each username there
 * is only one user object.
 *
 *
 */
public final class User {

    static private AtomicLong uidCounter = new AtomicLong();

    /**
     * the map of known users
     */
    private static Map<Long,User> userMap = new HashMap<Long,User>();
    private final static User anonymous = createLocalUser(tr("<anonymous>"));

    private static long getNextLocalUid() {
        return uidCounter.decrementAndGet();
    }

    /**
     * Creates a local user with the given name
     *
     * @param name the name
     * @return a new local user with the given name
     */
    public static User createLocalUser(String name) {
        for(long i = -1; i >= uidCounter.get(); --i)
        {
          User olduser = getById(i);
          if(olduser != null && olduser.hasName(name))
            return olduser;
        }
        User user = new User(getNextLocalUid(), name);
        userMap.put(user.getId(), user);
        return user;
    }

    /**
     * Creates a user known to the OSM server
     *
     * @param uid  the user id
     * @param name the name
     * @return a new OSM user with the given name and uid
     */
    public static User createOsmUser(long uid, String name) {
        User user = userMap.get(uid);
        if (user == null) {
            user = new User(uid, name);
            userMap.put(user.getId(), user);
        }
        if (name != null) user.addName(name);
        return user;
    }

    /**
     * clears the static map of user ids to user objects
     *
     */
    public static void clearUserMap() {
        userMap.clear();
    }

    /**
     * Returns the user with user id <code>uid</code> or null if this user doesn't exist
     *
     * @param uid the user id
     * @return the user; null, if there is no user with  this id
     */
    public static User getById(long uid) {
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
    public static List<User> getByName(String name) {
        if (name == null) {
            name = "";
        }
        List<User> ret = new ArrayList<User>();
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
        return anonymous;
    }

    /** the user name */
    private final Set<String> names = new HashSet<String>();
    /** the user id */
    private final long uid;

    /**
     * Replies the user name
     *
     * @return the user name. Never <code>null</code>, but may be the empty string
     */
    public String getName() {
        return Utils.join("/", names);
    }

    /**
     * Returns the list of user names
     *
     * @return list of names
     */
    public List<String> getNames() {
        return new ArrayList<String>(names);
    }

    /**
     * Adds a user name to the list if it is not there, yet.
     *
     * @param name
     */
    public void addName(String name) {
        names.add(name);
    }

    /**
     * Returns true if the name is in the names list
     *
     * @param name
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

    /** private constructor, only called from get method. */
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
        final int prime = 31;
        int result = 1;
        result = prime * result + getName().hashCode();
        result = prime * result + (int) (uid ^ (uid >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof User))
            return false;
        User other = (User) obj;
        if (uid != other.uid)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("id:").append(uid);
        if (names.size() == 1) {
            s.append(" name:").append(getName());
        }
        else if (names.size() > 1) {
            s.append(String.format(" %d names:%s", names.size(), getName()));
        }
        return s.toString();
    }
}
