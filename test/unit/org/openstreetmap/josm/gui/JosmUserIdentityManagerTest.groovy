// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.Test
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.osm.User
import org.openstreetmap.josm.data.osm.UserInfo

class JosmUserIdentityManagerTest {

    final shouldFail = new GroovyTestCase().&shouldFail

    @BeforeClass
    public static void initTestCase() {
        JOSMFixture.createUnitTestFixture().init()
    }

    @Test
    public void test_SingletonAccess() {

        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // created ?
        assert im != null

        JosmUserIdentityManager im2 = JosmUserIdentityManager.getInstance()

        // only one instance
        assert im == im2
    }

    @Test
    public void test_setAnonymous() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        im.setPartiallyIdentified "test"
        im.setAnonymous()

        assert im.isAnonymous()
        assert ! im.isPartiallyIdentified()
        assert ! im.isFullyIdentified()

        assert im.getUserId() == 0
        assert im.getUserName() == null
        assert im.getUserInfo() == null
        assert im.asUser() == User.anonymous
    }

    @Test
    public void test_setPartiallyIdentified() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        im.setPartiallyIdentified "test"

        shouldFail(IllegalArgumentException) {
            im.setPartiallyIdentified null
        }

        shouldFail(IllegalArgumentException) {
            im.setPartiallyIdentified ""
        }

        shouldFail(IllegalArgumentException) {
            im.setPartiallyIdentified "  \t  "
        }

        im.setPartiallyIdentified "test"

        assert ! im.isAnonymous()
        assert im.isPartiallyIdentified()
        assert ! im.isFullyIdentified()

        assert im.getUserId() == 0
        assert im.getUserName() == "test"
        assert im.getUserInfo() == null
        assert im.asUser() == new User(0, "test")
    }


    @Test
    public void test_setFullyIdentified() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        UserInfo userInfo = new UserInfo(id: 1, description: "a description")

        im.setFullyIdentified "test", userInfo

        shouldFail(IllegalArgumentException) {
            im.setFullyIdentified null, userInfo
        }
        shouldFail(IllegalArgumentException) {
            im.setFullyIdentified "", userInfo
        }
        shouldFail(IllegalArgumentException) {
            im.setFullyIdentified " \t ", userInfo
        }
        shouldFail(IllegalArgumentException) {
            im.setFullyIdentified "test", null
        }

        im.setFullyIdentified "test", userInfo

        assert ! im.isAnonymous()
        assert ! im.isPartiallyIdentified()
        assert im.isFullyIdentified()

        assert im.getUserId() == 1
        assert im.getUserName() == "test"
        assert im.getUserInfo() == userInfo
        assert im.asUser() == new User(1, "test")
    }

    /**
     * Preferences include neither an url nor a user name => we have an anonymous user
     */
    @Test
    public void initFromPreferences_1() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // reset it
        im.@userName = null
        im.@userInfo = null

        Main.pref.put "osm-server.url", null
        Main.pref.put "osm-server.username", null

        im.initFromPreferences()

        assert im.isAnonymous()
    }

    /**
     * Preferences include neither an url nor a user name => we have an anonymous user
     */
    @Test
    public void initFromPreferences_2() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // reset it
        im.@userName = null
        im.@userInfo = null

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener im

        try {
            Main.pref.put "osm-server.url", "http://api.openstreetmap.org"
            Main.pref.put "osm-server.username", null

            im.initFromPreferences()

            assert im.isAnonymous()
        } finally {
            Main.pref.addPreferenceChangeListener im
        }
    }

    /**
     * Preferences include an user name => we have a partially identified user
     */
    @Test
    public void initFromPreferences_3() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener im

        try {
            // reset it
            im.@userName = null
            im.@userInfo = null

            Main.pref.put "osm-server.url", "http://api.openstreetmap.org"
            Main.pref.put "osm-server.username", "test"

            im.initFromPreferences()

            assert im.isPartiallyIdentified()
        } finally {
            Main.pref.addPreferenceChangeListener im
        }
    }

    /**
     * Preferences include an user name which is different from the current
     * user name and we are currently fully identifed => josm user becomes
     * partially identified
     */
    @Test
    public void initFromPreferences_4() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener im

        try {
            im.setFullyIdentified "test1", new UserInfo(id: 1)

            Main.pref.put "osm-server.url", "http://api.openstreetmap.org"
            Main.pref.put "osm-server.username", "test2"

            im.initFromPreferences()

            assert im.isPartiallyIdentified()
        } finally {
            Main.pref.addPreferenceChangeListener im
        }
    }

    /**
     * Preferences include an user name which is the same as the current
     * user name and we are currently fully identifed => josm user remains
     * fully identified
     */
    @Test
    public void initFromPreferences_5() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener im

        try {
            im.setFullyIdentified "test1", new UserInfo(id: 1)

            Main.pref.put "osm-server.url", "http://api.openstreetmap.org"
            Main.pref.put "osm-server.username", "test1"

            im.initFromPreferences()

            assert im.isFullyIdentified()
        } finally {
            Main.pref.addPreferenceChangeListener im
        }
    }

    @Test
    public void apiUrlChanged() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // reset it
        im.@userName = null
        im.@userInfo = null

        Main.pref.put "osm-server.url", "http://api.openstreetmap.org"
        assert im.isAnonymous()

         Main.pref.put "osm-server.url", null
         assert im.isAnonymous()

        // reset it
        im.@userName = "test"
        im.@userInfo = null

        Main.pref.put "osm-server.url", "http://api.openstreetmap.org"
        assert im.isPartiallyIdentified()
        assert im.getUserName() == "test"

        Main.pref.put "osm-server.url", null
        assert im.isAnonymous()

        // reset it
        im.@userName = "test"
        im.@userInfo = new UserInfo(id:1)

        Main.pref.put "osm-server.url", "http://api.openstreetmap.org"
        assert im.isPartiallyIdentified()
        assert im.getUserName() == "test"

        // reset it
        im.@userName = "test"
        im.@userInfo = new UserInfo(id:1)


        Main.pref.put "osm-server.url", null
        assert im.isAnonymous()
    }

    @Test
    public void userNameChanged() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance()

        // reset it
        im.@userName = null
        im.@userInfo = null

        Main.pref.put "osm-server.username", "test"
        assert im.isPartiallyIdentified()
        assert im.getUserName() == "test"

        Main.pref.put "osm-server.username", null
        assert im.isAnonymous()
        assert im.asUser() == User.anonymous

        // reset it
        im.@userName = "test1"
        im.@userInfo = null

        Main.pref.put "osm-server.username", "test2"
        assert im.isPartiallyIdentified()
        assert im.getUserName() == "test2"
        assert im.asUser() == new User(0, "test2")

        Main.pref.put "osm-server.username", null
        assert im.isAnonymous()

        // reset it
        im.@userName = "test1"
        im.@userInfo = new UserInfo(id:1)

        Main.pref.put "osm-server.username", "test2"
        assert im.isPartiallyIdentified()
        assert im.getUserName() == "test2"
        assert im.asUser() == new User(0, "test2")

        // reset it
        im.@userName = "test1"
        im.@userInfo = new UserInfo(id:1)


        Main.pref.put "osm-server.username", null
        assert im.isAnonymous()
    }
}
