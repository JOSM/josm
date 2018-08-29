// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link UserIdentityManager} class.
 */
public class UserIdentityManagerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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

        UserIdentityManager im = UserIdentityManager.getInstance();

        // created ?
        assertNotNull(im);

        UserIdentityManager im2 = UserIdentityManager.getInstance();

        // only one instance
        assertSame(im, im2);
    }

    /**
     * Unit test of {@link UserIdentityManager#setAnonymous}.
     */
    @Test
    public void testSetAnonymous() {
        UserIdentityManager im = UserIdentityManager.getInstance();

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
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - nominal case.
     */
    @Test
    public void testSetPartiallyIdentified() {
        UserIdentityManager im = UserIdentityManager.getInstance();
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
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - null case.
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public void testSetPartiallyIdentifiedNull() {
        UserIdentityManager.getInstance().setPartiallyIdentified(null);
    }

    /**
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - empty case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPartiallyIdentifiedEmpty() {
        UserIdentityManager.getInstance().setPartiallyIdentified("");
    }

    /**
     * Unit test of {@link UserIdentityManager#setPartiallyIdentified} - blank case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetPartiallyIdentifiedBlank() {
        UserIdentityManager.getInstance().setPartiallyIdentified("  \t  ");
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - nominal case.
     */
    @Test
    public void testSetFullyIdentified() {
        UserIdentityManager im = UserIdentityManager.getInstance();

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
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - null name case.
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public void testSetFullyIdentifiedNullName() {
        UserIdentityManager.getInstance().setFullyIdentified(null, newUserInfo());
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - empty name case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFullyIdentifiedEmptyName() {
        UserIdentityManager.getInstance().setFullyIdentified("", newUserInfo());
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - blank name case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFullyIdentifiedBlankName() {
        UserIdentityManager.getInstance().setFullyIdentified(" \t ", newUserInfo());
    }

    /**
     * Unit test of {@link UserIdentityManager#setFullyIdentified} - null info case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetFullyIdentifiedNullInfo() {
        UserIdentityManager.getInstance().setFullyIdentified("test", null);
    }

    /**
     * Preferences include neither an url nor a user name => we have an anonymous user
     */
    @Test
    public void testInitFromPreferences1() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            Config.getPref().put("osm-server.url", null);
            Config.getPref().put("osm-server.username", null);

            im.initFromPreferences();

            assertTrue(im.isAnonymous());
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include neither an url nor a user name => we have an anonymous user
     */
    @Test
    public void testInitFromPreferences2() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
            Config.getPref().put("osm-server.username", null);

            im.initFromPreferences();

            assertTrue(im.isAnonymous());
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name => we have a partially identified user
     */
    @Test
    public void testInitFromPreferences3() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            // reset it
            im.setAnonymous();

            Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
            Config.getPref().put("osm-server.username", "test");

            im.initFromPreferences();

            assertTrue(im.isPartiallyIdentified());
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name which is different from the current
     * user name and we are currently fully identifed => josm user becomes
     * partially identified
     */
    @Test
    public void testInitFromPreferences4() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            im.setFullyIdentified("test1", newUserInfo());

            Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
            Config.getPref().put("osm-server.username", "test2");

            im.initFromPreferences();

            assertTrue(im.isPartiallyIdentified());
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    /**
     * Preferences include an user name which is the same as the current
     * user name and we are currently fully identifed => josm user remains
     * fully identified
     */
    @Test
    public void testInitFromPreferences5() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // for this test we disable the listener
        Config.getPref().removePreferenceChangeListener(im);

        try {
            im.setFullyIdentified("test1", new UserInfo());

            Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
            Config.getPref().put("osm-server.username", "test1");

            im.initFromPreferences();

            assertTrue(im.isFullyIdentified());
        } finally {
            Config.getPref().addPreferenceChangeListener(im);
        }
    }

    @Test
    public void testApiUrlChanged() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isAnonymous());

        Config.getPref().put("osm-server.url", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setPartiallyIdentified("test");

        Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        Config.getPref().put("osm-server.url", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setFullyIdentified("test", newUserInfo());

        Config.getPref().put("osm-server.url", "http://api.openstreetmap.org");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        // reset it
        im.setFullyIdentified("test", newUserInfo());

        Config.getPref().put("osm-server.url", null);
        assertTrue(im.isAnonymous());
    }

    @Test
    public void testUserNameChanged() {
        UserIdentityManager im = UserIdentityManager.getInstance();

        // reset it
        im.setAnonymous();

        Config.getPref().put("osm-server.username", "test");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test", im.getUserName());

        Config.getPref().put("osm-server.username", null);
        assertTrue(im.isAnonymous());
        assertEquals(User.getAnonymous(), im.asUser());

        // reset it
        im.setPartiallyIdentified("test1");

        Config.getPref().put("osm-server.username", "test2");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test2", im.getUserName());
        User usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test2", usr.getName());

        Config.getPref().put("osm-server.username", null);
        assertTrue(im.isAnonymous());

        // reset it
        im.setFullyIdentified("test1", newUserInfo());

        Config.getPref().put("osm-server.username", "test2");
        assertTrue(im.isPartiallyIdentified());
        assertEquals("test2", im.getUserName());
        usr = im.asUser();
        assertEquals(0, usr.getId());
        assertEquals("test2", usr.getName());

        // reset it
        im.setFullyIdentified("test1", newUserInfo());

        Config.getPref().put("osm-server.username", null);
        assertTrue(im.isAnonymous());
    }
}
