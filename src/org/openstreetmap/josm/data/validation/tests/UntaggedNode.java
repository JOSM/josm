// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Locale;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.KeyValueVisitor;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Checks for nodes with uninteresting tags that are in no way
 *
 * @author frsantos
 */
public class UntaggedNode extends Test implements KeyValueVisitor {

    protected static final int UNTAGGED_NODE_BLANK = 201;
    protected static final int UNTAGGED_NODE_FIXME = 202;
    protected static final int UNTAGGED_NODE_NOTE = 203;
    protected static final int UNTAGGED_NODE_CREATED_BY = 204;
    protected static final int UNTAGGED_NODE_WATCH = 205;
    protected static final int UNTAGGED_NODE_SOURCE = 206;
    protected static final int UNTAGGED_NODE_OTHER = 207;
    protected static final String ERROR_MESSAGE = tr("Unconnected nodes without physical tags");

    /**
     * Constructor
     */
    public UntaggedNode() {
        super(tr("Untagged and unconnected nodes"),
                tr("This test checks for untagged nodes that are not part of any way."));
    }

    @Override
    public void visit(Node n) {
        if (n.isUsable() && !n.isTagged() && n.getReferrers().isEmpty()) {

            if (!n.hasKeys() && IN_DOWNLOADED_AREA.test(n)) {
                errors.add(TestError.builder(this, Severity.WARNING, UNTAGGED_NODE_BLANK)
                        .message(ERROR_MESSAGE, marktr("No tags"))
                        .primitives(n)
                        .build());
                return;
            }
            n.visitKeys(this);
        }
    }

    private static OsmPrimitive[] castPrim(Tagged n) {
        return n instanceof OsmPrimitive ? (new OsmPrimitive[]{(OsmPrimitive) n}) : (new OsmPrimitive[0]);
    }

    @Override
    public void visitKeyValue(Tagged n, String key, String value) {
        if (key.toLowerCase(Locale.ENGLISH).contains("fixme") || value.toLowerCase(Locale.ENGLISH).contains("fixme")) {
            /* translation note: don't translate quoted words */
            errors.add(TestError.builder(this, Severity.WARNING, UNTAGGED_NODE_FIXME)
                    .message(ERROR_MESSAGE, marktr("Has tag containing ''fixme'' or ''FIXME''"))
                    .primitives(castPrim(n))
                    .build());
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
            errors.add(TestError.builder(this, Severity.WARNING, code)
                    .message(ERROR_MESSAGE, msg)
                    .primitives(castPrim(n))
                    .build());
            return;
        }
        // Does not happen, but just to be sure. Maybe definition of uninteresting tags changes in future.
        errors.add(TestError.builder(this, Severity.WARNING, UNTAGGED_NODE_OTHER)
                .message(ERROR_MESSAGE, marktr("Other"))
                .primitives(castPrim(n))
                .build());
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
