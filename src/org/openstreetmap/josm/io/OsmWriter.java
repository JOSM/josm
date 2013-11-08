// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.DateUtils;

/**
 * Save the dataset into a stream as osm intern xml format. This is not using any
 * xml library for storing.
 * @author imi
 */
public class OsmWriter extends XmlWriter implements PrimitiveVisitor {

    public static final String DEFAULT_API_VERSION = "0.6";

    private boolean osmConform;
    private boolean withBody = true;
    private boolean isOsmChange;
    private String version;
    private Changeset changeset;

    /**
     * Do not call this directly. Use OsmWriterFactory instead.
     */
    protected OsmWriter(PrintWriter out, boolean osmConform, String version) {
        super(out);
        this.osmConform = osmConform;
        this.version = (version == null ? DEFAULT_API_VERSION : version);
    }

    public void setWithBody(boolean wb) {
        this.withBody = wb;
    }

    public void setIsOsmChange(boolean isOsmChange) {
        this.isOsmChange = isOsmChange;
    }

    public void setChangeset(Changeset cs) {
        this.changeset = cs;
    }
    public void setVersion(String v) {
        this.version = v;
    }

    public void header() {
        header(null);
    }

    public void header(Boolean upload) {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.print("<osm version='");
        out.print(version);
        if (upload != null) {
            out.print("' upload='");
            out.print(upload);
        }
        out.println("' generator='JOSM'>");
    }

    public void footer() {
        out.println("</osm>");
    }

    protected static final Comparator<OsmPrimitive> byIdComparator = new Comparator<OsmPrimitive>() {
        @Override public int compare(OsmPrimitive o1, OsmPrimitive o2) {
            return (o1.getUniqueId()<o2.getUniqueId() ? -1 : (o1.getUniqueId()==o2.getUniqueId() ? 0 : 1));
        }
    };

    protected <T extends OsmPrimitive> Collection<T> sortById(Collection<T> primitives) {
        List<T> result = new ArrayList<T>(primitives.size());
        result.addAll(primitives);
        Collections.sort(result, byIdComparator);
        return result;
    }

    public void writeLayer(OsmDataLayer layer) {
        header(!layer.isUploadDiscouraged());
        writeDataSources(layer.data);
        writeContent(layer.data);
        footer();
    }

    /**
     * Writes the contents of the given dataset (nodes, then ways, then relations)
     * @param ds The dataset to write
     */
    public void writeContent(DataSet ds) {
        writeNodes(ds.getNodes());
        writeWays(ds.getWays());
        writeRelations(ds.getRelations());
    }

    /**
     * Writes the given nodes sorted by id
     * @param nodes The nodes to write
     * @since 5737
     */
    public void writeNodes(Collection<Node> nodes) {
        for (Node n : sortById(nodes)) {
            if (shouldWrite(n)) {
                visit(n);
            }
        }
    }

    /**
     * Writes the given ways sorted by id
     * @param ways The ways to write
     * @since 5737
     */
    public void writeWays(Collection<Way> ways) {
        for (Way w : sortById(ways)) {
            if (shouldWrite(w)) {
                visit(w);
            }
        }
    }

    /**
     * Writes the given relations sorted by id
     * @param relations The relations to write
     * @since 5737
     */
    public void writeRelations(Collection<Relation> relations) {
        for (Relation r : sortById(relations)) {
            if (shouldWrite(r)) {
                visit(r);
            }
        }
    }

    protected boolean shouldWrite(OsmPrimitive osm) {
        return !osm.isNewOrUndeleted() || !osm.isDeleted();
    }

    public void writeDataSources(DataSet ds) {
        for (DataSource s : ds.dataSources) {
            out.println("  <bounds minlat='"
                    + s.bounds.getMinLat()+"' minlon='"
                    + s.bounds.getMinLon()+"' maxlat='"
                    + s.bounds.getMaxLat()+"' maxlon='"
                    + s.bounds.getMaxLon()
                    +"' origin='"+XmlWriter.encode(s.origin)+"' />");
        }
    }

    @Override
    public void visit(INode n) {
        if (n.isIncomplete()) return;
        addCommon(n, "node");
        if (!withBody) {
            out.println("/>");
        } else {
            if (n.getCoor() != null) {
                out.print(" lat='"+n.getCoor().lat()+"' lon='"+n.getCoor().lon()+"'");
            }
            addTags(n, "node", true);
        }
    }

    @Override
    public void visit(IWay w) {
        if (w.isIncomplete()) return;
        addCommon(w, "way");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (int i=0; i<w.getNodesCount(); ++i) {
                out.println("    <nd ref='"+w.getNodeId(i) +"' />");
            }
            addTags(w, "way", false);
        }
    }

    @Override
    public void visit(IRelation e) {
        if (e.isIncomplete()) return;
        addCommon(e, "relation");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (int i=0; i<e.getMembersCount(); ++i) {
                out.print("    <member type='");
                out.print(e.getMemberType(i).getAPIName());
                out.println("' ref='"+e.getMemberId(i)+"' role='" +
                        XmlWriter.encode(e.getRole(i)) + "' />");
            }
            addTags(e, "relation", false);
        }
    }

    public void visit(Changeset cs) {
        out.print("  <changeset ");
        out.print(" id='"+cs.getId()+"'");
        if (cs.getUser() != null) {
            out.print(" user='"+cs.getUser().getName() +"'");
            out.print(" uid='"+cs.getUser().getId() +"'");
        }
        if (cs.getCreatedAt() != null) {
            out.print(" created_at='"+DateUtils.fromDate(cs.getCreatedAt()) +"'");
        }
        if (cs.getClosedAt() != null) {
            out.print(" closed_at='"+DateUtils.fromDate(cs.getClosedAt()) +"'");
        }
        out.print(" open='"+ (cs.isOpen() ? "true" : "false") +"'");
        if (cs.getMin() != null) {
            out.print(" min_lon='"+ cs.getMin().lonToString(CoordinateFormat.DECIMAL_DEGREES) +"'");
            out.print(" min_lat='"+ cs.getMin().latToString(CoordinateFormat.DECIMAL_DEGREES) +"'");
        }
        if (cs.getMax() != null) {
            out.print(" max_lon='"+ cs.getMin().lonToString(CoordinateFormat.DECIMAL_DEGREES) +"'");
            out.print(" max_lat='"+ cs.getMin().latToString(CoordinateFormat.DECIMAL_DEGREES) +"'");
        }
        out.println(">");
        addTags(cs, "changeset", false); // also writes closing </changeset>
    }

    protected static final Comparator<Entry<String, String>> byKeyComparator = new Comparator<Entry<String,String>>() {
        @Override public int compare(Entry<String, String> o1, Entry<String, String> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };

    protected void addTags(Tagged osm, String tagname, boolean tagOpen) {
        if (osm.hasKeys()) {
            if (tagOpen) {
                out.println(">");
            }
            List<Entry<String, String>> entries = new ArrayList<Entry<String,String>>(osm.getKeys().entrySet());
            Collections.sort(entries, byKeyComparator);
            for (Entry<String, String> e : entries) {
                out.println("    <tag k='"+ XmlWriter.encode(e.getKey()) +
                        "' v='"+XmlWriter.encode(e.getValue())+ "' />");
            }
            out.println("  </" + tagname + ">");
        } else if (tagOpen) {
            out.println(" />");
        } else {
            out.println("  </" + tagname + ">");
        }
    }

    /**
     * Add the common part as the form of the tag as well as the XML attributes
     * id, action, user, and visible.
     */
    protected void addCommon(IPrimitive osm, String tagname) {
        out.print("  <"+tagname);
        if (osm.getUniqueId() != 0) {
            out.print(" id='"+ osm.getUniqueId()+"'");
        } else
            throw new IllegalStateException(tr("Unexpected id 0 for osm primitive found"));
        if (!isOsmChange) {
            if (!osmConform) {
                String action = null;
                if (osm.isDeleted()) {
                    action = "delete";
                } else if (osm.isModified()) {
                    action = "modify";
                }
                if (action != null) {
                    out.print(" action='"+action+"'");
                }
            }
            if (!osm.isTimestampEmpty()) {
                out.print(" timestamp='"+DateUtils.fromDate(osm.getTimestamp())+"'");
            }
            // user and visible added with 0.4 API
            if (osm.getUser() != null) {
                if(osm.getUser().isLocalUser()) {
                    out.print(" user='"+XmlWriter.encode(osm.getUser().getName())+"'");
                } else if (osm.getUser().isOsmUser()) {
                    // uid added with 0.6
                    out.print(" uid='"+ osm.getUser().getId()+"'");
                    out.print(" user='"+XmlWriter.encode(osm.getUser().getName())+"'");
                }
            }
            out.print(" visible='"+osm.isVisible()+"'");
        }
        if (osm.getVersion() != 0) {
            out.print(" version='"+osm.getVersion()+"'");
        }
        if (this.changeset != null && this.changeset.getId() != 0) {
            out.print(" changeset='"+this.changeset.getId()+"'" );
        } else if (osm.getChangesetId() > 0 && !osm.isNew()) {
            out.print(" changeset='"+osm.getChangesetId()+"'" );
        }
    }
}
