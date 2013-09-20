// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Checks for nodes with uninteresting tags that are in no way
 *
 * @author frsantos
 */
public class UntaggedNode extends Test {

    protected static final int UNTAGGED_NODE_BLANK = 201;
    protected static final int UNTAGGED_NODE_FIXME = 202;
    protected static final int UNTAGGED_NODE_NOTE = 203;
    protected static final int UNTAGGED_NODE_CREATED_BY = 204;
    protected static final int UNTAGGED_NODE_WATCH = 205;
    protected static final int UNTAGGED_NODE_SOURCE = 206;
    protected static final int UNTAGGED_NODE_OTHER = 207;

    /**
     * Constructor
     */
    public UntaggedNode() {
        super(tr("Untagged and unconnected nodes"),
                tr("This test checks for untagged nodes that are not part of any way."));
    }

    @Override
    public void visit(Collection<OsmPrimitive> selection) {
        for (OsmPrimitive p : selection) {
            if (p.isUsable() && p instanceof Node) {
                p.accept(this);
            }
        }
    }

    @Override
    public void visit(Node n) {
        if (n.isUsable() && !n.isTagged() && n.getReferrers().isEmpty()) {
            String errorMessage = tr("Unconnected nodes without physical tags");
            if (!n.hasKeys()) {
                String msg = marktr("No tags");
                errors.add(new TestError(this, Severity.WARNING, errorMessage, tr(msg), msg, UNTAGGED_NODE_BLANK, n));
                return;
            }
            for (Map.Entry<String, String> tag : n.getKeys().entrySet()) {
                String key = tag.getKey();
                if (contains(tag, "fixme") || contains(tag, "FIXME")) {
                    /* translation note: don't translate quoted words */
                    String msg = marktr("Has tag containing ''fixme'' or ''FIXME''");
                    errors.add(new TestError(this, Severity.WARNING, errorMessage, tr(msg), msg, UNTAGGED_NODE_FIXME, n));
                    return;
                }

                String msg = null;
                int code = 0;
                if (key.startsWith("note") || key.startsWith("comment") || key.startsWith("description")) {
                    /* translation note: don't translate quoted words */
                    msg = marktr("Has key ''note'' or ''comment'' or ''description''");
                    code = UNTAGGED_NODE_NOTE;
                } else if (key.startsWith("created_by") || key.startsWith("converted_by")) {
                    /* translation note: don't translate quoted words */
                    msg = marktr("Has key ''created_by'' or ''converted_by''");
                    code = UNTAGGED_NODE_CREATED_BY;
                } else if (key.startsWith("watch")) {
                    /* translation note: don't translate quoted words */
                    msg = marktr("Has key ''watch''");
                    code = UNTAGGED_NODE_WATCH;
                } else if (key.startsWith("source")) {
                    /* translation note: don't translate quoted words */
                    msg = marktr("Has key ''source''");
                    code = UNTAGGED_NODE_SOURCE;
                }
                if (msg != null) {
                    errors.add(new TestError(this, Severity.WARNING, errorMessage, tr(msg), msg, code, n));
                    return;
                }
            }
            // Does not happen, but just to be sure. Maybe definition of uninteresting tags changes in future.
            errors.add(new TestError(this, Severity.WARNING, errorMessage, tr("Other"), "Other", UNTAGGED_NODE_OTHER, n));
        }
    }

    private boolean contains(Map.Entry<String, String> tag, String s) {
        return tag.getKey().indexOf(s) != -1 || tag.getValue().indexOf(s) != -1;
    }

    @Override
    public Command fixError(TestError testError) {
        return deletePrimitivesIfNeeded(testError.getPrimitives());
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (testError.getTester() instanceof UntaggedNode) {
            int code = testError.getCode();
            switch (code) {
            case UNTAGGED_NODE_BLANK:
            case UNTAGGED_NODE_CREATED_BY:
            case UNTAGGED_NODE_WATCH:
            case UNTAGGED_NODE_SOURCE:
                return true;
            }
        }
        return false;
    }
}
