// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.util.HashMap;

/**
 * A simple class to keep a list of user names.
 *
 * Instead of storing user names as strings with every OSM primitive, we store
 * a reference to an user object, and make sure that for each username there
 * is only one user object.
 *
 * @author fred
 *
 */
public class User {

    /** storage for existing User objects. */
    private static HashMap<String,User> userMap = new HashMap<String,User>();

    /** the username. */
    public String name;

    /** the user ID (since API 0.6) */
    public String uid;

    /** private constructor, only called from get method. */
    private User(String name) {
        this.name = name;
    }

    /** returns a new or existing User object that represents the given name. */
    public static User get(String name) {
        User user = userMap.get(name);
        if (user == null) {
            user = new User(name);
            userMap.put(name, user);
        }
        return user;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (uid == null) {
            if (other.uid != null)
                return false;
        } else if (!uid.equals(other.uid))
            return false;
        return true;
    }
}
