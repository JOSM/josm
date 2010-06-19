// License: GPL. Copyright 2007 by Immanuel Scholz and others
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
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.DateUtils;

/**
 * Save the dataset into a stream as osm intern xml format. This is not using any
 * xml library for storing.
 * @author imi
 */
public class OsmWriter extends XmlWriter implements Visitor {

    public static final String DEFAULT_API_VERSION = "0.6";

    private boolean osmConform;
    private boolean withBody = true;
    private String version;
    private Changeset changeset;

    public OsmWriter(PrintWriter out, boolean osmConform, String version) {
        super(out);
        this.osmConform = osmConform;
        this.version = (version == null ? DEFAULT_API_VERSION : version);
    }

    public void setWithBody(boolean wb) {
        this.withBody = wb;
    }
    public void setChangeset(Changeset cs) {
        this.changeset = cs;
    }
    public void setVersion(String v) {
        this.version = v;
    }

    public void header() {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.print("<osm version='");
        out.print(version);
        out.println("' generator='JOSM'>");
    }
    public void footer() {
        out.println("</osm>");
    }

    private static final Comparator<OsmPrimitive> byIdComparator = new Comparator<OsmPrimitive>() {
        public int compare(OsmPrimitive o1, OsmPrimitive o2) {
            return (o1.getUniqueId()<o2.getUniqueId() ? -1 : (o1.getUniqueId()==o2.getUniqueId() ? 0 : 1));
        }
    };

    private Collection<OsmPrimitive> sortById(Collection<? extends OsmPrimitive> primitives) {
        List<OsmPrimitive> result = new ArrayList<OsmPrimitive>(primitives.size());
        result.addAll(primitives);
        Collections.sort(result, byIdComparator);
        return result;
    }

    public void writeContent(DataSet ds) {
        for (OsmPrimitive n : sortById(ds.getNodes())) {
            if (shouldWrite(n)) {
                visit((Node)n);
            }
        }
        for (OsmPrimitive w : sortById(ds.getWays())) {
            if (shouldWrite(w)) {
                visit((Way)w);
            }
        }
        for (OsmPrimitive e: sortById(ds.getRelations())) {
            if (shouldWrite(e)) {
                visit((Relation)e);
            }
        }
    }

    private boolean shouldWrite(OsmPrimitive osm) {
        return !osm.isNewOrUndeleted() || !osm.isDeleted();
    }

    public void writeDataSources(DataSet ds) {
        for (DataSource s : ds.dataSources) {
            out.println("  <bounds minlat='"
                    + s.bounds.getMin().lat()+"' minlon='"
                    + s.bounds.getMin().lon()+"' maxlat='"
                    + s.bounds.getMax().lat()+"' maxlon='"
                    + s.bounds.getMax().lon()
                    +"' origin='"+XmlWriter.encode(s.origin)+"' />");
        }
    }

    public void visit(Node n) {
        if (n.isIncomplete()) return;
        addCommon(n, "node");
        out.print(" lat='"+n.getCoor().lat()+"' lon='"+n.getCoor().lon()+"'");
        if (!withBody) {
            out.println("/>");
        } else {
            addTags(n, "node", true);
        }
    }

    public void visit(Way w) {
        if (w.isIncomplete()) return;
        addCommon(w, "way");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (Node n : w.getNodes()) {
                out.println("    <nd ref='"+n.getUniqueId()+"' />");
            }
            addTags(w, "way", false);
        }
    }

    public void visit(Relation e) {
        if (e.isIncomplete()) return;
        addCommon(e, "relation");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (RelationMember em : e.getMembers()) {
                out.print("    <member type='");
                out.print(OsmPrimitiveType.from(em.getMember()).getAPIName());
                out.println("' ref='"+em.getMember().getUniqueId()+"' role='" +
                        XmlWriter.encode(em.getRole()) + "' />");
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

    private static final Comparator<Entry<String, String>> byKeyComparator = new Comparator<Entry<String,String>>() {
        public int compare(Entry<String, String> o1, Entry<String, String> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };

    private void addTags(Tagged osm, String tagname, boolean tagOpen) {
        if (osm.hasKeys()) {
            if (tagOpen) {
                out.println(">");
            }
            List<Entry<String, String>> entries = new ArrayList<Entry<String,String>>(osm.getKeys().entrySet());
            Collections.sort(entries, byKeyComparator);
            for (Entry<String, String> e : entries) {
                if ((osm instanceof Changeset) || !("created_by".equals(e.getKey()))) {
                    out.println("    <tag k='"+ XmlWriter.encode(e.getKey()) +
                            "' v='"+XmlWriter.encode(e.getValue())+ "' />");
                }
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
    private void addCommon(OsmPrimitive osm, String tagname) {
        out.print("  <"+tagname);
        if (osm.getUniqueId() != 0) {
            out.print(" id='"+ osm.getUniqueId()+"'");
        } else
            throw new IllegalStateException(tr("Unexpected id 0 for osm primitive found"));
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
        if (osm.getVersion() != 0) {
            out.print(" version='"+osm.getVersion()+"'");
        }
        if (this.changeset != null && this.changeset.getId() != 0) {
            out.print(" changeset='"+this.changeset.getId()+"'" );
        } else if (osm.getChangesetId() > 0 && !osm.isNew()) {
            out.print(" changeset='"+osm.getChangesetId()+"'" );
        }
    }

    public void close() {
        out.close();
    }

    public void flush() {
        out.flush();
    }
}
