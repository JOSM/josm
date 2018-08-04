// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.correction.TagCorrection;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link ReverseWayTagCorrector} class.
 */
public class ReverseWayTagCorrectorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests that {@code ReverseWayTagCorrector.TagSwitcher} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(ReverseWayTagCorrector.TagSwitcher.class);
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
        // direction=forward/backward/...
        assertSwitch(new Tag("direction", "forward"), new Tag("direction", "backward"));
        assertSwitch(new Tag("direction", "backward"), new Tag("direction", "forward"));
        // :left/:right with oneway (see #10977)
        assertSwitch(new Tag("cycleway:left:oneway", "-1"), new Tag("cycleway:right:oneway", "yes"));
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
        // values
        assertSwitch(new Tag("something", "forward"), new Tag("something", "backward"));
        assertSwitch(new Tag("something", "backward"), new Tag("something", "forward"));
        assertSwitch(new Tag("something", "up"), new Tag("something", "down"));
        assertSwitch(new Tag("something", "down"), new Tag("something", "up"));
        // value[:_]suffix
        assertSwitch(new Tag("something", "forward:suffix"), new Tag("something", "backward:suffix"));
        assertSwitch(new Tag("something", "backward_suffix"), new Tag("something", "forward_suffix"));
        assertSwitch(new Tag("something", "up:suffix"), new Tag("something", "down:suffix"));
        assertSwitch(new Tag("something", "down_suffix"), new Tag("something", "up_suffix"));
        // prefix[:_]value
        assertSwitch(new Tag("something", "prefix:forward"), new Tag("something", "prefix:backward"));
        assertSwitch(new Tag("something", "prefix_backward"), new Tag("something", "prefix_forward"));
        assertSwitch(new Tag("something", "prefix:up"), new Tag("something", "prefix:down"));
        assertSwitch(new Tag("something", "prefix_down"), new Tag("something", "prefix_up"));
        // prefix[:_]value[:_]suffix
        assertSwitch(new Tag("something", "prefix:forward:suffix"), new Tag("something", "prefix:backward:suffix"));
        assertSwitch(new Tag("something", "prefix_backward:suffix"), new Tag("something", "prefix_forward:suffix"));
        assertSwitch(new Tag("something", "prefix:up_suffix"), new Tag("something", "prefix:down_suffix"));
        assertSwitch(new Tag("something", "prefix_down_suffix"), new Tag("something", "prefix_up_suffix"));
        // #8499
        assertSwitch(new Tag("type", "drawdown"), new Tag("type", "drawdown"));
    }

    private void assertSwitch(Tag oldTag, Tag newTag) {
        Assert.assertEquals(ReverseWayTagCorrector.TagSwitcher.apply(oldTag), newTag);
    }

    private Map<OsmPrimitive, List<TagCorrection>> getTagCorrectionsForWay(String middleNodeTags) {
        final OsmPrimitive n1 = OsmUtils.createPrimitive("node");
        final OsmPrimitive n2 = OsmUtils.createPrimitive("node " + middleNodeTags);
        final OsmPrimitive n3 = OsmUtils.createPrimitive("node");
        final Way w = new Way();
        Stream.of(n1, n2, n3).map(Node.class::cast).forEach(w::addNode);
        return ReverseWayTagCorrector.getTagCorrectionsMap(w);
    }

    /**
     * Test tag correction on way nodes
     */
    @Test
    public void testSwitchingWayNodes() {
        final Map<OsmPrimitive, List<TagCorrection>> tagCorrections = getTagCorrectionsForWay("direction=forward");
        Assert.assertEquals(1, tagCorrections.size());
        Assert.assertEquals(Collections.singletonList(new TagCorrection("direction", "forward", "direction", "backward")),
                tagCorrections.values().iterator().next());
    }

    /**
     * Test tag correction on way nodes are not applied for absolute values such as compass cardinal directions
     */
    @Test
    public void testNotSwitchingWayNodes() {
        Assert.assertEquals(0, getTagCorrectionsForWay("direction=SSW").size());
        Assert.assertEquals(0, getTagCorrectionsForWay("direction=145").size());
    }
}
