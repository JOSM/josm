// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

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

    /**
     * Writes OSM header with normal download and upload policies.
     */
    public void header() {
        header(DownloadPolicy.NORMAL, UploadPolicy.NORMAL);
    }

    /**
     * Writes OSM header with given download upload policies.
     * @param download download policy
     * @param upload upload policy
     * @since 13485
     */
    public void header(DownloadPolicy download, UploadPolicy upload) {
        header(download, upload, false);
    }

    private void header(DownloadPolicy download, UploadPolicy upload, boolean locked) {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.print("<osm version='");
        out.print(version);
        if (download != null && download != DownloadPolicy.NORMAL) {
            out.print("' download='");
            out.print(download.getXmlFlag());
        }
        if (upload != null && upload != UploadPolicy.NORMAL) {
            out.print("' upload='");
            out.print(upload.getXmlFlag());
        }
        if (locked) {
            out.print("' locked='true");
        }
        out.println("' generator='JOSM'>");
    }

    /**
     * Writes OSM footer.
     */
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
        header(data.getDownloadPolicy(), data.getUploadPolicy(), data.isLocked());
        writeDataSources(data);
        writeContent(data);
        footer();
    }

    /**
     * Writes the contents of the given dataset (nodes, then ways, then relations)
     * @param ds The dataset to write
     */
    public void writeContent(DataSet ds) {
        setWithVisible(UploadPolicy.NORMAL == ds.getUploadPolicy());
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

    /**
     * Writes data sources with their respective bounds.
     * @param ds data set
     */
    public void writeDataSources(DataSet ds) {
        for (DataSource s : ds.getDataSources()) {
            out.append("  <bounds minlat='").append(DecimalDegreesCoordinateFormat.INSTANCE.latToString(s.bounds.getMin()));
            out.append("' minlon='").append(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(s.bounds.getMin()));
            out.append("' maxlat='").append(DecimalDegreesCoordinateFormat.INSTANCE.latToString(s.bounds.getMax()));
            out.append("' maxlon='").append(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(s.bounds.getMax()));
            out.append("' origin='").append(XmlWriter.encode(s.origin)).append("' />");
            out.println();
        }
    }

    void writeLatLon(LatLon ll) {
        if (ll != null) {
            out.append(" lat='").append(LatLon.cDdHighPrecisionFormatter.format(ll.lat())).append("'");
            out.append(" lon='").append(LatLon.cDdHighPrecisionFormatter.format(ll.lon())).append("'");
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
    public void visit(IWay<?> w) {
        if (w.isIncomplete()) return;
        addCommon(w, "way");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (int i = 0; i < w.getNodesCount(); ++i) {
                out.append("    <nd ref='").append(String.valueOf(w.getNodeId(i))).append("' />");
                out.println();
            }
            addTags(w, "way", false);
        }
    }

    @Override
    public void visit(IRelation<?> e) {
        if (e.isIncomplete()) return;
        addCommon(e, "relation");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (int i = 0; i < e.getMembersCount(); ++i) {
                out.print("    <member type='");
                out.print(e.getMemberType(i).getAPIName());
                out.append("' ref='").append(String.valueOf(e.getMemberId(i)));
                out.append("' role='").append(XmlWriter.encode(e.getRole(i))).append("' />");
                out.println();
            }
            addTags(e, "relation", false);
        }
    }

    /**
     * Visiting call for changesets.
     * @param cs changeset
     */
    public void visit(Changeset cs) {
        out.append("  <changeset id='").append(String.valueOf(cs.getId())).append("'");
        if (cs.getUser() != null) {
            out.append(" user='").append(XmlWriter.encode(cs.getUser().getName())).append("'");
            out.append(" uid='").append(String.valueOf(cs.getUser().getId())).append("'");
        }
        Instant createdDate = cs.getCreatedAt();
        if (createdDate != null) {
            out.append(" created_at='").append(String.valueOf(createdDate)).append("'");
        }
        Instant closedDate = cs.getClosedAt();
        if (closedDate != null) {
            out.append(" closed_at='").append(String.valueOf(closedDate)).append("'");
        }
        out.append(" open='").append(cs.isOpen() ? "true" : "false").append("'");
        if (cs.getMin() != null) {
            out.append(" min_lon='").append(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(cs.getMin())).append("'");
            out.append(" min_lat='").append(DecimalDegreesCoordinateFormat.INSTANCE.latToString(cs.getMin())).append("'");
        }
        if (cs.getMax() != null) {
            out.append(" max_lon='").append(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(cs.getMax())).append("'");
            out.append(" max_lat='").append(DecimalDegreesCoordinateFormat.INSTANCE.latToString(cs.getMax())).append("'");
        }
        out.println(">");
        addTags(cs, "changeset", false); // also writes closing </changeset>
    }

    protected static final Comparator<Entry<String, String>> byKeyComparator = Entry.comparingByKey();

    protected void addTags(Tagged osm, String tagname, boolean tagOpen) {
        if (osm.hasKeys()) {
            if (tagOpen) {
                out.println(">");
            }
            List<Entry<String, String>> entries = new ArrayList<>(osm.getKeys().entrySet());
            entries.sort(byKeyComparator);
            for (Entry<String, String> e : entries) {
                out.append("    <tag k='").append(XmlWriter.encode(e.getKey()));
                out.append("' v='").append(XmlWriter.encode(e.getValue())).append("' />");
                out.println();
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
        out.append("  <").append(tagname);
        if (osm.getUniqueId() != 0) {
            out.append(" id='").append(String.valueOf(osm.getUniqueId())).append("'");
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
                    out.append(" action='").append(action).append("'");
                }
            }
            if (!osm.isTimestampEmpty()) {
                out.append(" timestamp='").append(String.valueOf(osm.getInstant())).append("'");
            }
            // user and visible added with 0.4 API
            if (osm.getUser() != null) {
                if (osm.getUser().isLocalUser()) {
                    out.append(" user='").append(XmlWriter.encode(osm.getUser().getName())).append("'");
                } else if (osm.getUser().isOsmUser()) {
                    // uid added with 0.6
                    out.append(" uid='").append(String.valueOf(osm.getUser().getId())).append("'");
                    out.append(" user='").append(XmlWriter.encode(osm.getUser().getName())).append("'");
                }
            }
            if (withVisible) {
                out.append(" visible='").append(String.valueOf(osm.isVisible())).append("'");
            }
        }
        if (osm.getVersion() != 0) {
            out.append(" version='").append(String.valueOf(osm.getVersion())).append("'");
        }
        if (this.changeset != null && this.changeset.getId() != 0) {
            out.append(" changeset='").append(String.valueOf(this.changeset.getId())).append("'");
        } else if (osm.getChangesetId() > 0 && !osm.isNew()) {
            out.append(" changeset='").append(String.valueOf(osm.getChangesetId())).append("'");
        }
    }
}
