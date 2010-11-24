// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.Bag;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Check area type ways for errors
 *
 * @author stoecker
 */
public class UnclosedWays extends Test {
    /** The already detected errors */
    Bag<Way, Way> _errorWays;

    /**
     * Constructor
     */
    public UnclosedWays() {
        super(tr("Unclosed Ways."), tr("This tests if ways which should be circular are closed."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        _errorWays = new Bag<Way, Way>();
    }

    @Override
    public void endTest() {
        _errorWays = null;
        super.endTest();
    }

    private String type;
    private String etype;
    private int mode;

    public void set(int m, String text, String desc) {
        etype = MessageFormat.format(text, desc);
        type = tr(text, tr(desc));
        mode = m;
    }

    public void set(int m, String text) {
        etype = text;
        type = tr(text);
        mode = m;
    }

    @Override
    public void visit(Way w) {
        String test;
        type = etype = null;
        mode = 0;

        if (!w.isUsable())
            return;

        test = w.get("natural");
        if (test != null && !"coastline".equals(test) && !"cliff".equals(test))
            set(1101, marktr("natural type {0}"), test);
        test = w.get("landuse");
        if (test != null)
            set(1102, marktr("landuse type {0}"), test);
        test = w.get("amenities");
        if (test != null)
            set(1103, marktr("amenities type {0}"), test);
        test = w.get("sport");
        if (test != null && !test.equals("water_slide"))
            set(1104, marktr("sport type {0}"), test);
        test = w.get("tourism");
        if (test != null)
            set(1105, marktr("tourism type {0}"), test);
        test = w.get("shop");
        if (test != null)
            set(1106, marktr("shop type {0}"), test);
        test = w.get("leisure");
        if (test != null)
            set(1107, marktr("leisure type {0}"), test);
        test = w.get("waterway");
        if (test != null && test.equals("riverbank"))
            set(1108, marktr("waterway type {0}"), test);
        Boolean btest = OsmUtils.getOsmBoolean(w.get("building"));
        if (btest != null && btest)
            set(1120, marktr("building"));
        btest = OsmUtils.getOsmBoolean(w.get("area"));
        if (btest != null && btest)
            set(1130, marktr("area"));

        if (type != null && !w.isClosed()) {
            for (OsmPrimitive parent: w.getReferrers()) {
                if (parent instanceof Relation && "multipolygon".equals(parent.get("type")))
                    return;
            }
            Node f = w.firstNode();
            Node l = w.lastNode();

            List<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
            List<OsmPrimitive> highlight = new ArrayList<OsmPrimitive>();
            primitives.add(w);

            // The important parts of an unclosed way are the first and
            // the last node which should be connected, therefore we highlight them
            highlight.add(f);
            highlight.add(l);

            errors.add(new TestError(this, Severity.WARNING, tr("Unclosed way"),
                            type, etype, mode, primitives, highlight));
            _errorWays.add(w, w);
        }
    }
}
