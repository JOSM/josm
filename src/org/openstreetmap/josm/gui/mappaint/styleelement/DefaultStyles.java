// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.StyleElementList;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement.BoxProvider;

/**
 * Default element styles.
 * @since 14193
 */
public final class DefaultStyles implements StyleKeys {

    private DefaultStyles() {
        // Hide public constructor
    }

    /**
     * The style used for simple nodes
     */
    public static final NodeElement SIMPLE_NODE_ELEMSTYLE;

    /**
     * A box provider that provides the size of a simple node
     */
    public static final BoxProvider SIMPLE_NODE_ELEMSTYLE_BOXPROVIDER;

    static {
        MultiCascade mc = new MultiCascade();
        mc.getOrCreateCascade("default");
        SIMPLE_NODE_ELEMSTYLE = NodeElement.create(new Environment(null, mc, "default", null), 4.1f, true);
        if (SIMPLE_NODE_ELEMSTYLE == null) throw new AssertionError();
        SIMPLE_NODE_ELEMSTYLE_BOXPROVIDER = SIMPLE_NODE_ELEMSTYLE.getBoxProvider();
    }

    /**
     * The default style a simple node should use for it's text
     */
    public static final BoxTextElement SIMPLE_NODE_TEXT_ELEMSTYLE;

    static {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put(TEXT, Keyword.AUTO);
        Node n = new Node();
        n.put("name", "dummy");
        SIMPLE_NODE_TEXT_ELEMSTYLE = BoxTextElement.create(new Environment(n, mc, "default", null), SIMPLE_NODE_ELEMSTYLE.getBoxProvider());
        if (SIMPLE_NODE_TEXT_ELEMSTYLE == null) throw new AssertionError();
    }

    /**
     * The default styles that are used for nodes.
     * @see DefaultStyles#SIMPLE_NODE_ELEMSTYLE
     */
    public static final StyleElementList DEFAULT_NODE_STYLELIST = new StyleElementList(DefaultStyles.SIMPLE_NODE_ELEMSTYLE);

    /**
     * The default styles that are used for nodes with text.
     */
    public static final StyleElementList DEFAULT_NODE_STYLELIST_TEXT = new StyleElementList(DefaultStyles.SIMPLE_NODE_ELEMSTYLE,
            DefaultStyles.SIMPLE_NODE_TEXT_ELEMSTYLE);
}
