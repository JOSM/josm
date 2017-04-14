// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.UserInfo;

/**
 * Unit tests of {@link JosmUserIdentityManager} class.
 */
public class JosmUserIdentityManagerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void initTestCase() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static UserInfo newUserInfo() {
        return newUserInfo(1, "a description");
    }

    private static UserInfo newUserInfo(int id, String description) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setDescription(description);
        return userInfo;
    }

    /**
     * Test singleton access.
     */
    @Test
    public void testSingletonAccess() {

        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // created ?
        assertNotNull(im);

        JosmUserIdentityManager im2 = JosmUserIdentityManager.getInstance();

        // only one instance
        assertSame(im, im2);
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setAnonymous}.
     */
    @Test
    public void testSetAnonymous() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        im.setPartiallyIdentified("test");
        im.setAnonymous();

        assertTrue(im.isAnonymous());
        assertFalse(im.isPartiallyIdentified());
        assertFalse(im.isFullyIdentified());

        assertEquals(0, im.getUserId());
        assertNull(im.getUserName());
        assertNull(im.getUserInfo());
        assertSame(User.getAnonymous(), im.asUser());
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setPartiallyIdentified} - nominal case.
     */
    @Test
    public void testSetPartiallyIdentified() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
        im.setPartiallyIdentified("test");

        assertFalse(im.isAnonymous());
        assertTrue(im.isPartiallyIdentified());
        assertFalse(im.isFullyIdentified());

        assertEquals(0, im.getUserId());
        assertEquals("test", im.getUserName());
        assertNull(im.getUserInfo());
        User usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test", usr.getName());
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setPartiallyIdentified} - null case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPartiallyIdentifiedNull() {
        JosmUserIdentityManager.getInstance().setPartiallyIdentified(null);
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setPartiallyIdentified} - empty case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPartiallyIdentifiedEmpty() {
        JosmUserIdentityManager.getInstance().setPartiallyIdentified("");
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setPartiallyIdentified} - blank case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPartiallyIdentifiedBlank() {
        JosmUserIdentityManager.getInstance().setPartiallyIdentified("  \t  ");
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setFullyIdentified} - nominal case.
     */
    @Test
    public void testSetFullyIdentified() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        UserInfo userInfo = newUserInfo();

        im.setFullyIdentified("test", userInfo);

        assertFalse(im.isAnonymous());
        assertFalse(im.isPartiallyIdentified());
        assertTrue(im.isFullyIdentified());

        assertEquals(1, im.getUserId());
        assertEquals("test", im.getUserName());
        assertEquals(userInfo, im.getUserInfo());
        User usr = im.asUser();
        assertEquals(1, usr.getId());
        assertEquals("test", usr.getName());
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setFullyIdentified} - null name case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFullyIdentifiedNullName() {
        JosmUserIdentityManager.getInstance().setFullyIdentified(null, newUserInfo());
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setFullyIdentified} - empty name case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFullyIdentifiedEmptyName() {
        JosmUserIdentityManager.getInstance().setFullyIdentified("", newUserInfo());
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setFullyIdentified} - blank name case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFullyIdentifiedBlankName() {
        JosmUserIdentityManager.getInstance().setFullyIdentified(" \t ", newUserInfo());
    }

    /**
     * Unit test of {@link JosmUserIdentityManager#setFullyIdentified} - null info case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFullyIdentifiedNullInfo() {
        JosmUserIdentityManager.getInstance().setFullyIdentified("test", null);
    }

    /**
     * Preferences include neither an url nor a user name => we have an anonymous user
     */
    @Test
    public void testInitFromPreferences1() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener(im);

        try {
            Main.pref.put("osm-server.url", null);
            Main.pref.put("osm-server.username", null);

            im.initFromPreferences();

            assertTrue(im.isAnonymous());
        } finally {
            Main.pref.addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include neither an url nor a user name => we have an anonymous user
     */
    @Test
    public void testInitFromPreferences2() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener(im);

        try {
            Main.pref.put("osm-server.url", "http://api.openstreetmap.org");
            Main.pref.put("osm-server.username", null);

            im.initFromPreferences();

            assertTrue(im.isAnonymous());
        } finally {
            Main.pref.addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name => we have a partially identified user
     */
    @Test
    public void testInitFromPreferences3() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener(im);

        try {
            // reset it
            im.setAnonymous();

            Main.pref.put("osm-server.url", "http://api.openstreetmap.org");
            Main.pref.put("osm-server.username", "test");

            im.initFromPreferences();

            assertTrue(im.isPartiallyIdentified());
        } finally {
            Main.pref.addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name which is different from the current
     * user name and we are currently fully identifed => josm user becomes
     * partially identified
     */
    @Test
    public void testInitFromPreferences4() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener(im);

        try {
            im.setFullyIdentified("test1", newUserInfo());

            Main.pref.put("osm-server.url", "http://api.openstreetmap.org");
            Main.pref.put("osm-server.username", "test2");

            im.initFromPreferences();

            assertTrue(im.isPartiallyIdentified());
        } finally {
            Main.pref.addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name which is the same as the current
     * user name and we are currently fully identifed => josm user remains
     * fully identified
     */
    @Test
    public void testInitFromPreferences5() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // for this test we disable the listener
        Main.pref.removePreferenceChangeListener(im);

        try {
            im.setFullyIdentified("test1", new UserInfo());

            Main.pref.put("osm-server.url", "http://api.openstreetmap.org");
            Main.pref.put("osm-server.username", "test1");

            im.initFromPreferences();

            assertTrue(im.isFullyIdentified());
        } finally {
            Main.pref.addPreferenceChangeListener(im);
        }
    }

    @Test
    public void testApiUrlChanged() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        Main.pref.put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isAnonymous());

        Main.pref.put("osm-server.url", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setPartiallyIdentified("test");

        Main.pref.put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        Main.pref.put("osm-server.url", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setFullyIdentified("test", newUserInfo());

        Main.pref.put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        // reset it
        im.setFullyIdentified("test", newUserInfo());

        Main.pref.put("osm-server.url", null);
        assertTrue(im.isAnonymous());
    }

    @Test
    //@Ignore
    public void testUserNameChanged() {
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        Main.pref.put("osm-server.username", "test");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        Main.pref.put("osm-server.username", null);
        assertTrue(im.isAnonymous());
        assertEquals(User.getAnonymous(), im.asUser());

        // reset it
        im.setPartiallyIdentified("test1");

        Main.pref.put("osm-server.username", "test2");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test2", im.getUserName());
        User usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test2", usr.getName());

        Main.pref.put("osm-server.username", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setFullyIdentified("test1", newUserInfo());

        Main.pref.put("osm-server.username", "test2");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test2", im.getUserName());
        usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test2", usr.getName());

        // reset it
        im.setFullyIdentified("test1", newUserInfo());

        Main.pref.put("osm-server.username", null);
        assertTrue(im.isAnonymous());
    }
}
