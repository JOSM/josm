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
}
