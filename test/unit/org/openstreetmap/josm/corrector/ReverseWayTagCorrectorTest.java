// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.Tag;

/**
 * Unit tests of {@link ReverseWayTagCorrector} class.
 */
public class ReverseWayTagCorrectorTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        Main.initApplicationPreferences();
    }
    
    /**
     * Test of {@link ReverseWayTagCorrector.TagSwitcher#apply} method.
     */
    @Test
    public void testTagSwitch() {
        // oneway
        assertSwitch(new Tag("oneway", "yes"), new Tag("oneway", "-1"));
        assertSwitch(new Tag("oneway", "true"), new Tag("oneway", "-1"));
        assertSwitch(new Tag("oneway", "-1"), new Tag("oneway", "yes"));
        assertSwitch(new Tag("oneway", "no"), new Tag("oneway", "no"));
        assertSwitch(new Tag("oneway", "something"), new Tag("oneway", "something"));
        // incline/direction
        for (String k : new String[]{"incline", "direction"}) {
            assertSwitch(new Tag(k, "up"), new Tag(k, "down"));
            assertSwitch(new Tag(k, "down"), new Tag(k, "up"));
            assertSwitch(new Tag(k, "something"), new Tag(k, "something"));
        }
        // :forward/:backward (see #8518)
        assertSwitch(new Tag("turn:forward", "right"), new Tag("turn:backward", "right"));
        assertSwitch(new Tag("change:forward", "not_right"), new Tag("change:backward", "not_right"));
        assertSwitch(new Tag("placement:forward", "right_of:1"), new Tag("placement:backward", "right_of:1"));
        assertSwitch(new Tag("turn:lanes:forward", "left|right"), new Tag("turn:lanes:backward", "left|right"));
        assertSwitch(new Tag("change:lanes:forward", "not_right|only_left"), new Tag("change:lanes:backward", "not_right|only_left"));
        // keys
        assertSwitch(new Tag("forward", "something"), new Tag("backward", "something"));
        assertSwitch(new Tag("backward", "something"), new Tag("forward", "something"));
        assertSwitch(new Tag("up", "something"), new Tag("down", "something"));
        assertSwitch(new Tag("down", "something"), new Tag("up", "something"));
        assertSwitch(new Tag("east", "something"), new Tag("west", "something"));
        assertSwitch(new Tag("west", "something"), new Tag("east", "something"));
        assertSwitch(new Tag("south", "something"), new Tag("north", "something"));
        assertSwitch(new Tag("north", "something"), new Tag("south", "something"));
        // values
        assertSwitch(new Tag("something", "forward"), new Tag("something", "backward"));
        assertSwitch(new Tag("something", "backward"), new Tag("something", "forward"));
        assertSwitch(new Tag("something", "up"), new Tag("something", "down"));
        assertSwitch(new Tag("something", "down"), new Tag("something", "up"));
        assertSwitch(new Tag("something", "east"), new Tag("something", "west"));
        assertSwitch(new Tag("something", "west"), new Tag("something", "east"));
        assertSwitch(new Tag("something", "south"), new Tag("something", "north"));
        assertSwitch(new Tag("something", "north"), new Tag("something", "south"));
        // value[:_]suffix
        assertSwitch(new Tag("something", "forward:suffix"), new Tag("something", "backward:suffix"));
        assertSwitch(new Tag("something", "backward_suffix"), new Tag("something", "forward_suffix"));
        assertSwitch(new Tag("something", "up:suffix"), new Tag("something", "down:suffix"));
        assertSwitch(new Tag("something", "down_suffix"), new Tag("something", "up_suffix"));
        assertSwitch(new Tag("something", "east:suffix"), new Tag("something", "west:suffix"));
        assertSwitch(new Tag("something", "west_suffix"), new Tag("something", "east_suffix"));
        assertSwitch(new Tag("something", "south:suffix"), new Tag("something", "north:suffix"));
        assertSwitch(new Tag("something", "north_suffix"), new Tag("something", "south_suffix"));
        // prefix[:_]value
        assertSwitch(new Tag("something", "prefix:forward"), new Tag("something", "prefix:backward"));
        assertSwitch(new Tag("something", "prefix_backward"), new Tag("something", "prefix_forward"));
        assertSwitch(new Tag("something", "prefix:up"), new Tag("something", "prefix:down"));
        assertSwitch(new Tag("something", "prefix_down"), new Tag("something", "prefix_up"));
        assertSwitch(new Tag("something", "prefix:east"), new Tag("something", "prefix:west"));
        assertSwitch(new Tag("something", "prefix_west"), new Tag("something", "prefix_east"));
        assertSwitch(new Tag("something", "prefix:south"), new Tag("something", "prefix:north"));
        assertSwitch(new Tag("something", "prefix_north"), new Tag("something", "prefix_south"));
        // prefix[:_]value[:_]suffix
        assertSwitch(new Tag("something", "prefix:forward:suffix"), new Tag("something", "prefix:backward:suffix"));
        assertSwitch(new Tag("something", "prefix_backward:suffix"), new Tag("something", "prefix_forward:suffix"));
        assertSwitch(new Tag("something", "prefix:up_suffix"), new Tag("something", "prefix:down_suffix"));
        assertSwitch(new Tag("something", "prefix_down_suffix"), new Tag("something", "prefix_up_suffix"));
        assertSwitch(new Tag("something", "prefix:east:suffix"), new Tag("something", "prefix:west:suffix"));
        assertSwitch(new Tag("something", "prefix_west:suffix"), new Tag("something", "prefix_east:suffix"));
        assertSwitch(new Tag("something", "prefix:south_suffix"), new Tag("something", "prefix:north_suffix"));
        assertSwitch(new Tag("something", "prefix_north_suffix"), new Tag("something", "prefix_south_suffix"));
        // #8499
        assertSwitch(new Tag("type", "drawdown"), new Tag("type", "drawdown"));
    }
    
    private void assertSwitch(Tag oldTag, Tag newTag) {
        Assert.assertEquals(ReverseWayTagCorrector.TagSwitcher.apply(oldTag), newTag);
    }
}
