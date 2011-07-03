// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.MirroredInputStream;
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
public class User {

    static private AtomicLong uidCounter = new AtomicLong();

    /**
     * the map of known users
     */
    private static HashMap<Long,User> userMap = new HashMap<Long,User>();
    private static HashSet<Long> relicensingUsers = null;
    private static HashSet<Long> nonRelicensingUsers = null;

    private static long getNextLocalUid() {
        return uidCounter.decrementAndGet();
    }

    /**
     * Creates a local user with the given name
     *
     * @param name the name
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
     */
    public static User  createOsmUser(long uid, String name) {
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

    public static void initRelicensingInformation() {
        if (relicensingUsers == null) {
            loadRelicensingInformation(false);
        }
    }

    public static void loadRelicensingInformation(boolean clean) {
        relicensingUsers = new HashSet<Long>();
        nonRelicensingUsers = new HashSet<Long>();
        try {
            MirroredInputStream stream = new MirroredInputStream(
                 Main.pref.get("url.licensechange",
                    "http://planet.openstreetmap.org/users_agreed/users_agreed.txt"),
                 clean ? 1 : 7200);
            try {
                InputStreamReader r;
                r = new InputStreamReader(stream);
                BufferedReader reader = new BufferedReader(r);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    try {
                        Long id = new Long(Long.parseLong(line.trim()));
                        relicensingUsers.add(id);
                    } catch (java.lang.NumberFormatException ex) {
                    }
                }
            }
            finally {
                stream.close();
            }
        } catch (IOException ex) {
        }

        try {
            MirroredInputStream stream = new MirroredInputStream(
                Main.pref.get("url.licensechange_reject",
                    "http://planet.openstreetmap.org/users_agreed/users_disagreed.txt"),
                clean ? 1 : 7200);
            try {
                InputStreamReader r;
                r = new InputStreamReader(stream);
                BufferedReader reader = new BufferedReader(r);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    try {
                        Long id = new Long(Long.parseLong(line.trim()));
                        nonRelicensingUsers.add(id);
                    } catch (java.lang.NumberFormatException ex) {
                    }
                }
            }
            finally {
                stream.close();
            }
        } catch (IOException ex) {
        }
    }

    /** the user name */
    private final HashSet<String> names = new HashSet<String>();
    /** the user id */
    private final long uid;
    private int relicensingStatus = STATUS_UNKNOWN;

    public static final int STATUS_UNKNOWN = -1;
    public static final int STATUS_UNDECIDED = 0;
    public static final int STATUS_AGREED = 1;
    public static final int STATUS_NOT_AGREED = 2;
    public static final int STATUS_AUTO_AGREED = 3;
    public static final int STATUS_ANONYMOUS = 4;

    /** 
    * Finds out this user's relicensing status and saves it for quicker
    * access.
    */
    public int getRelicensingStatus() {
        if (relicensingStatus != STATUS_UNKNOWN) return relicensingStatus;
        if (uid >= 286582) return (relicensingStatus = STATUS_AUTO_AGREED);
        if (relicensingUsers == null) return STATUS_UNKNOWN;
        Long id = new Long(uid);
        if (relicensingUsers.contains(id)) return (relicensingStatus = STATUS_AGREED);
        if (nonRelicensingUsers == null) return STATUS_UNKNOWN;
        if (nonRelicensingUsers.contains(id)) return (relicensingStatus = STATUS_NOT_AGREED);
        return STATUS_UNDECIDED;
    }

    /**
    * Sets this user's relicensing status. This can be used if relicensing
    * information is available from another source so that directly looking
    * at the users_agreed/users_not_agreed files it not required.
    */
    public void setRelicensingStatus(int status) {
        relicensingStatus = status;
    }

    /**
     * Replies the user name
     *
     * @return the user name. Never null, but may be the empty string
     */
    public String getName() {
        return Utils.join("/", names);
    }

    /**
     * Returns the list of user names
     *
     * @returns list of names
     */
    public ArrayList<String> getNames() {
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

    public boolean isOsmUser() {
        return uid > 0;
    }

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
        StringBuffer s = new StringBuffer();
        s.append("id:"+uid);
        if (names.size() == 1) {
            s.append(" name:"+getName());
        }
        else if (names.size() > 1) {
            s.append(String.format(" %d names:%s", names.size(), getName()));
        }
        return s.toString();
    }
}
