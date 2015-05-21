// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Arrays;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.Utils;

/**
 * A ReverseWayNoTagCorrector warns about ways that should not be reversed
 * because their semantic meaning cannot be preserved in that case.
 * E.g. natural=coastline, natural=cliff, barrier=retaining_wall cannot be changed.
 * @see ReverseWayTagCorrector for handling of tags that can be modified (oneway=yes, etc.)
 * @since 5724
 */
public final class ReverseWayNoTagCorrector {

    private ReverseWayNoTagCorrector() {
        // Hide default constructor for utils classes
    }

    /**
     * Tags that imply a semantic meaning from the way direction and cannot be changed.
     */
    public static final TagCollection directionalTags = new TagCollection(Arrays.asList(new Tag[]{
            new Tag("natural", "coastline"),
            new Tag("natural", "cliff"),
            new Tag("barrier", "guard_rail"),
            new Tag("barrier", "kerb"),
            new Tag("barrier", "retaining_wall"),
            new Tag("man_made", "embankment"),
            new Tag("waterway", "stream"),
            new Tag("waterway", "river"),
            new Tag("waterway", "ditch"),
            new Tag("waterway", "drain"),
            new Tag("waterway", "canal")
    }));

    /**
     * Replies the tags that imply a semantic meaning from <code>way</code> direction and cannot be changed.
     * @param way The way to look for
     * @return tags that imply a semantic meaning from <code>way</code> direction and cannot be changed
     */
    public static TagCollection getDirectionalTags(Way way) {
        return directionalTags.intersect(TagCollection.from(way));
    }

    /**
     * Tests whether way can be reversed without semantic change.
     * Looks for tags like natural=cliff, barrier=retaining_wall.
     * @param way The way to check
     * @return false if the semantic meaning change if the way is reversed, true otherwise.
     */
    public static boolean isReversible(Way way) {
        return getDirectionalTags(way).isEmpty();
    }

    protected static String getHTML(TagCollection tags) {
        if (tags.size() == 1) {
            return tags.iterator().next().toString();
        } else if (tags.size() > 1) {
            return Utils.joinAsHtmlUnorderedList(tags);
        } else {
            return "";
        }
    }

    protected static boolean confirmReverseWay(Way way, TagCollection tags) {
        String msg = trn(
                // Singular, if a single tag is impacted
                "<html>You are going to reverse the way ''{0}'',"
                + "<br/> whose semantic meaning of its tag ''{1}'' is defined by its direction.<br/>"
                + "Do you really want to change the way direction, thus its semantic meaning?</html>",
                // Plural, if several tags are impacted
                "<html>You are going to reverse the way ''{0}'',"
                + "<br/> whose semantic meaning of these tags are defined by its direction:<br/>{1}"
                + "Do you really want to change the way direction, thus its semantic meaning?</html>",
                tags.size(),
                way.getDisplayName(DefaultNameFormatter.getInstance()),
                getHTML(tags)
            );
        int ret = ConditionalOptionPaneUtil.showOptionDialog(
                "reverse_directional_way",
                Main.parent,
                msg,
                tr("Reverse directional way."),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                null
        );
        switch(ret) {
            case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
            case JOptionPane.YES_OPTION:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks the given way can be safely reversed and asks user to confirm the operation if it not the case.
     * @param way The way to check
     * @throws UserCancelException If the user cancels the operation
     */
    public static void checkAndConfirmReverseWay(Way way) throws UserCancelException {
        TagCollection tags = getDirectionalTags(way);
        if (!tags.isEmpty() && !confirmReverseWay(way, tags)) {
            throw new UserCancelException();
        }
    }
}
