// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSet.UploadPolicy;
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
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Save the dataset into a stream as osm intern xml format. This is not using any xml library for storing.
 * @author imi
 * @since 59
 */
public class OsmWriter extends XmlWriter implements PrimitiveVisitor {

    /** Default OSM API version */
    public static final String DEFAULT_API_VERSION = "0.6";

    private final boolean osmConform;
    private boolean withBody = true;
    private boolean withVisible = true;
    private boolean isOsmChange;
    private String version;
    private Changeset changeset;

    /**
     * Constructs a new {@code OsmWriter}.
     * Do not call this directly. Use {@link OsmWriterFactory} instead.
     * @param out print writer
     * @param osmConform if {@code true}, prevents modification attributes to be written to the common part
     * @param version OSM API version (0.6)
     */
    protected OsmWriter(PrintWriter out, boolean osmConform, String version) {
        super(out);
        this.osmConform = osmConform;
        this.version = version == null ? DEFAULT_API_VERSION : version;
    }

    /**
     * Sets whether body must be written.
     * @param wb if {@code true} body will be written.
     */
    public void setWithBody(boolean wb) {
        this.withBody = wb;
    }

    /**
     * Sets whether 'visible' attribute must be written.
     * @param wv if {@code true} 'visible' attribute will be written.
     * @since 12019
     */
    public void setWithVisible(boolean wv) {
        this.withVisible = wv;
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
        header(UploadPolicy.NORMAL);
    }

    public void header(UploadPolicy upload) {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.print("<osm version='");
        out.print(version);
        if (upload != null && upload != UploadPolicy.NORMAL) {
            out.print("' upload='");
            out.print(upload.getXmlFlag());
        }
        out.println("' generator='JOSM'>");
    }

    public void footer() {
        out.println("</osm>");
    }

    /**
     * Sorts {@code -1} &rarr; {@code -infinity}, then {@code +1} &rarr; {@code +infinity}
     */
    protected static final Comparator<AbstractPrimitive> byIdComparator = (o1, o2) -> {
        final long i1 = o1.getUniqueId();
        final long i2 = o2.getUniqueId();
        if (i1 < 0 && i2 < 0) {
            return Long.compare(i2, i1);
        } else {
            return Long.compare(i1, i2);
        }
    };

    protected <T extends OsmPrimitive> Collection<T> sortById(Collection<T> primitives) {
        List<T> result = new ArrayList<>(primitives.size());
        result.addAll(primitives);
        result.sort(byIdComparator);
        return result;
    }

    /**
     * Writes the full OSM file for the given data set (header, data sources, osm data, footer).
     * @param data OSM data set
     * @since 12800
     */
    public void write(DataSet data) {
        header(data.getUploadPolicy());
        writeDataSources(data);
        writeContent(data);
        footer();
    }

    /**
     * Writes the contents of the given dataset (nodes, then ways, then relations)
     * @param ds The dataset to write
     */
    public void writeContent(DataSet ds) {
        setWithVisible(UploadPolicy.NORMAL.equals(ds.getUploadPolicy()));
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
        for (DataSource s : ds.getDataSources()) {
            out.println("  <bounds minlat='"
                    + DecimalDegreesCoordinateFormat.INSTANCE.latToString(s.bounds.getMin())
                    +"' minlon='"
                    + DecimalDegreesCoordinateFormat.INSTANCE.lonToString(s.bounds.getMin())
                    +"' maxlat='"
                    + DecimalDegreesCoordinateFormat.INSTANCE.latToString(s.bounds.getMax())
                    +"' maxlon='"
                    + DecimalDegreesCoordinateFormat.INSTANCE.lonToString(s.bounds.getMax())
                    +"' origin='"+XmlWriter.encode(s.origin)+"' />");
        }
    }

    void writeLatLon(LatLon ll) {
        if (ll != null) {
            out.print(" lat='"+LatLon.cDdHighPecisionFormatter.format(ll.lat())+
                     "' lon='"+LatLon.cDdHighPecisionFormatter.format(ll.lon())+'\'');
        }
    }

    @Override
    public void visit(INode n) {
        if (n.isIncomplete()) return;
        addCommon(n, "node");
        if (!withBody) {
            out.println("/>");
        } else {
            writeLatLon(n.getCoor());
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
            for (int i = 0; i < w.getNodesCount(); ++i) {
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
            for (int i = 0; i < e.getMembersCount(); ++i) {
                out.print("    <member type='");
                out.print(e.getMemberType(i).getAPIName());
                out.println("' ref='"+e.getMemberId(i)+"' role='" +
                        XmlWriter.encode(e.getRole(i)) + "' />");
            }
            addTags(e, "relation", false);
        }
    }

    public void visit(Changeset cs) {
        out.print("  <changeset id='"+cs.getId()+'\'');
        if (cs.getUser() != null) {
            out.print(" user='"+ XmlWriter.encode(cs.getUser().getName()) +'\'');
            out.print(" uid='"+cs.getUser().getId() +'\'');
        }
        Date createdDate = cs.getCreatedAt();
        if (createdDate != null) {
            out.print(" created_at='"+DateUtils.fromDate(createdDate) +'\'');
        }
        Date closedDate = cs.getClosedAt();
        if (closedDate != null) {
            out.print(" closed_at='"+DateUtils.fromDate(closedDate) +'\'');
        }
        out.print(" open='"+ (cs.isOpen() ? "true" : "false") +'\'');
        if (cs.getMin() != null) {
            out.print(" min_lon='"+ DecimalDegreesCoordinateFormat.INSTANCE.lonToString(cs.getMin()) +'\'');
            out.print(" min_lat='"+ DecimalDegreesCoordinateFormat.INSTANCE.latToString(cs.getMin()) +'\'');
        }
        if (cs.getMax() != null) {
            out.print(" max_lon='"+ DecimalDegreesCoordinateFormat.INSTANCE.lonToString(cs.getMin()) +'\'');
            out.print(" max_lat='"+ DecimalDegreesCoordinateFormat.INSTANCE.latToString(cs.getMin()) +'\'');
        }
        out.println(">");
        addTags(cs, "changeset", false); // also writes closing </changeset>
    }

    protected static final Comparator<Entry<String, String>> byKeyComparator = (o1, o2) -> o1.getKey().compareTo(o2.getKey());

    protected void addTags(Tagged osm, String tagname, boolean tagOpen) {
        if (osm.hasKeys()) {
            if (tagOpen) {
                out.println(">");
            }
            List<Entry<String, String>> entries = new ArrayList<>(osm.getKeys().entrySet());
            entries.sort(byKeyComparator);
            for (Entry<String, String> e : entries) {
                out.println("    <tag k='"+ XmlWriter.encode(e.getKey()) +
                        "' v='"+XmlWriter.encode(e.getValue())+ "' />");
            }
            out.println("  </" + tagname + '>');
        } else if (tagOpen) {
            out.println(" />");
        } else {
            out.println("  </" + tagname + '>');
        }
    }

    /**
     * Add the common part as the form of the tag as well as the XML attributes
     * id, action, user, and visible.
     * @param osm osm primitive
     * @param tagname XML tag matching osm primitive (node, way, relation)
     */
    protected void addCommon(IPrimitive osm, String tagname) {
        out.print("  <"+tagname);
        if (osm.getUniqueId() != 0) {
            out.print(" id='"+ osm.getUniqueId()+'\'');
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
                    out.print(" action='"+action+'\'');
                }
            }
            if (!osm.isTimestampEmpty()) {
                out.print(" timestamp='"+DateUtils.fromTimestamp(osm.getRawTimestamp())+'\'');
            }
            // user and visible added with 0.4 API
            if (osm.getUser() != null) {
                if (osm.getUser().isLocalUser()) {
                    out.print(" user='"+XmlWriter.encode(osm.getUser().getName())+'\'');
                } else if (osm.getUser().isOsmUser()) {
                    // uid added with 0.6
                    out.print(" uid='"+ osm.getUser().getId()+'\'');
                    out.print(" user='"+XmlWriter.encode(osm.getUser().getName())+'\'');
                }
            }
            if (withVisible) {
                out.print(" visible='"+osm.isVisible()+'\'');
            }
        }
        if (osm.getVersion() != 0) {
            out.print(" version='"+osm.getVersion()+'\'');
        }
        if (this.changeset != null && this.changeset.getId() != 0) {
            out.print(" changeset='"+this.changeset.getId()+'\'');
        } else if (osm.getChangesetId() > 0 && !osm.isNew()) {
            out.print(" changeset='"+osm.getChangesetId()+'\'');
        }
    }
}
