// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.DontShowAgainInfo;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command to delete a number of primitives from the dataset.
 * @author imi
 */
public class DeleteCommand extends Command {

    /**
     * The primitive that get deleted.
     */
    private final Collection<? extends OsmPrimitive> data;

    /**
     * Constructor for a collection of data
     */
    public DeleteCommand(Collection<? extends OsmPrimitive> data) {
        this.data = data;
    }

    /**
     * Constructor for a single data item. Use the collection constructor to delete multiple
     * objects.
     */
    public DeleteCommand(OsmPrimitive data) {
        this.data = Collections.singleton(data);
    }

    @Override public boolean executeCommand() {
        super.executeCommand();
        for (OsmPrimitive osm : data) {
            osm.delete(true);
        }
        return true;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        deleted.addAll(data);
    }

    @Override public MutableTreeNode description() {
        NameVisitor v = new NameVisitor();

        if (data.size() == 1) {
            data.iterator().next().visit(v);
            return new DefaultMutableTreeNode(new JLabel(tr("Delete {1} {0}", v.name, tr(v.className)), v.icon,
                    JLabel.HORIZONTAL));
        }

        String cname = null;
        String cnamem = null;
        for (OsmPrimitive osm : data) {
            osm.visit(v);
            if (cname == null) {
                cname = v.className;
                cnamem = v.classNamePlural;
            } else if (!cname.equals(v.className)) {
                cname = "object";
                cnamem = trn("object", "objects", 2);
            }
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JLabel(tr("Delete {0} {1}", data.size(), trn(
                cname, cnamem, data.size())), ImageProvider.get("data", cname), JLabel.HORIZONTAL));
        for (OsmPrimitive osm : data) {
            osm.visit(v);
            root.add(new DefaultMutableTreeNode(v.toLabel()));
        }
        return root;
    }

    /**
     * Delete the primitives and everything they reference.
     *
     * If a node is deleted, the node and all ways and relations the node is part of are deleted as
     * well.
     *
     * If a way is deleted, all relations the way is member of are also deleted.
     *
     * If a way is deleted, only the way and no nodes are deleted.
     *
     * @param selection The list of all object to be deleted.
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     */
    public static Command deleteWithReferences(Collection<? extends OsmPrimitive> selection) {
        CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
        for (OsmPrimitive osm : selection)
            osm.visit(v);
        v.data.addAll(selection);
        if (v.data.isEmpty())
            return null;
        if (!checkAndConfirmOutlyingDeletes(v.data))
            return null;
        return new DeleteCommand(v.data);
    }

    /**
     * Try to delete all given primitives.
     *
     * If a node is used by a way, it's removed from that way. If a node or a way is used by a
     * relation, inform the user and do not delete.
     *
     * If this would cause ways with less than 2 nodes to be created, delete these ways instead. If
     * they are part of a relation, inform the user and do not delete.
     *
     * @param selection The objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     */
    private static int testRelation(Relation ref, OsmPrimitive osm) {
        NameVisitor n = new NameVisitor();
        ref.visit(n);
        NameVisitor s = new NameVisitor();
        osm.visit(s);
        String role = new String();
        for (RelationMember m : ref.members) {
            if (m.member == osm) {
                role = m.role;
                break;
            }
        }
        if (role.length() > 0) {
            return new ExtendedDialog(Main.parent, 
                        tr("Conflicting relation"), 
                        tr("Selection \"{0}\" is used by relation \"{1}\" with role {2}.\nDelete from relation?",
                            s.name, n.name, role),
                        new String[] {tr("Delete from relation"), tr("Cancel")}, 
                        new String[] {"dialogs/delete.png", "cancel.png"}).getValue();  
        } else {
            return new ExtendedDialog(Main.parent, 
                        tr("Conflicting relation"), 
                        tr("Selection \"{0}\" is used by relation \"{1}\".\nDelete from relation?",
                            s.name, n.name),
                        new String[] {tr("Delete from relation"), tr("Cancel")}, 
                        new String[] {"dialogs/delete.png", "cancel.png"}).getValue();  
        }
    }

    public static Command delete(Collection<? extends OsmPrimitive> selection) {
        return delete(selection, true);
    }

    public static Command delete(Collection<? extends OsmPrimitive> selection, boolean alsoDeleteNodesInWay) {
        if (selection.isEmpty())
            return null;

        Collection<OsmPrimitive> del = new HashSet<OsmPrimitive>(selection);
        Collection<Way> waysToBeChanged = new HashSet<Way>();
        HashMap<OsmPrimitive, Collection<OsmPrimitive>> relationsToBeChanged = new HashMap<OsmPrimitive, Collection<OsmPrimitive>>();

        if (alsoDeleteNodesInWay) {
            // Delete untagged nodes that are to be unreferenced.
            Collection<OsmPrimitive> delNodes = new HashSet<OsmPrimitive>();
            for (OsmPrimitive osm : del) {
                if (osm instanceof Way) {
                    for (Node n : ((Way) osm).nodes) {
                        if (!n.tagged) {
                            CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds, false);
                            n.visit(v);
                            v.data.removeAll(del);
                            if (v.data.isEmpty()) {
                                delNodes.add(n);
                            }
                        }
                    }
                }
            }
            del.addAll(delNodes);
        }

        if (!checkAndConfirmOutlyingDeletes(del))
            return null;

        for (OsmPrimitive osm : del) {
            CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds, false);
            osm.visit(v);
            for (OsmPrimitive ref : v.data) {
                if (del.contains(ref))
                    continue;
                if (ref instanceof Way) {
                    waysToBeChanged.add((Way) ref);
                } else if (ref instanceof Relation) {
                    if (testRelation((Relation) ref, osm) == 1) {
                        Collection<OsmPrimitive> relset = relationsToBeChanged.get(ref);
                        if (relset == null)
                            relset = new HashSet<OsmPrimitive>();
                        relset.add(osm);
                        relationsToBeChanged.put(ref, relset);
                    } else
                        return null;
                } else {
                    return null;
                }
            }
        }

        Collection<Command> cmds = new LinkedList<Command>();
        for (Way w : waysToBeChanged) {
            Way wnew = new Way(w);
            wnew.nodes.removeAll(del);
            if (wnew.nodes.size() < 2) {
                del.add(w);

                CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds, false);
                w.visit(v);
                for (OsmPrimitive ref : v.data) {
                    if (del.contains(ref))
                        continue;
                    if (ref instanceof Relation) {
                        Boolean found = false;
                        Collection<OsmPrimitive> relset = relationsToBeChanged.get(ref);
                        if (relset == null)
                            relset = new HashSet<OsmPrimitive>();
                        else {
                            for (OsmPrimitive m : relset) {
                                if (m == w) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            if (testRelation((Relation) ref, w) == 1) {
                                relset.add(w);
                                relationsToBeChanged.put(ref, relset);
                            } else
                                return null;
                        }
                    } else {
                        return null;
                    }
                }
            } else {
                cmds.add(new ChangeCommand(w, wnew));
            }
        }

        Iterator<OsmPrimitive> iterator = relationsToBeChanged.keySet().iterator();
        while (iterator.hasNext()) {
            Relation cur = (Relation) iterator.next();
            Relation rel = new Relation(cur);
            for (OsmPrimitive osm : relationsToBeChanged.get(cur)) {
                for (RelationMember rm : rel.members) {
                    if (rm.member == osm) {
                        RelationMember mem = new RelationMember();
                        mem.role = rm.role;
                        mem.member = rm.member;
                        rel.members.remove(mem);
                        break;
                    }
                }
            }
            cmds.add(new ChangeCommand(cur, rel));
        }

        if (!del.isEmpty())
            cmds.add(new DeleteCommand(del));

        return new SequenceCommand(tr("Delete"), cmds);
    }

    public static Command deleteWaySegment(WaySegment ws) {
        List<Node> n1 = new ArrayList<Node>(), n2 = new ArrayList<Node>();

        n1.addAll(ws.way.nodes.subList(0, ws.lowerIndex + 1));
        n2.addAll(ws.way.nodes.subList(ws.lowerIndex + 1, ws.way.nodes.size()));

        if (n1.size() < 2 && n2.size() < 2) {
            return new DeleteCommand(Collections.singleton(ws.way));
        }

        Way wnew = new Way(ws.way);
        wnew.nodes.clear();

        if (n1.size() < 2) {
            wnew.nodes.addAll(n2);
            return new ChangeCommand(ws.way, wnew);
        } else if (n2.size() < 2) {
            wnew.nodes.addAll(n1);
            return new ChangeCommand(ws.way, wnew);
        } else {
            Collection<Command> cmds = new LinkedList<Command>();

            wnew.nodes.addAll(n1);
            cmds.add(new ChangeCommand(ws.way, wnew));

            Way wnew2 = new Way();
            if (wnew.keys != null) {
                wnew2.keys = new HashMap<String, String>(wnew.keys);
                wnew2.checkTagged();
                wnew2.checkDirectionTagged();
            }
            wnew2.nodes.addAll(n2);
            cmds.add(new AddCommand(wnew2));

            return new SequenceCommand(tr("Split way segment"), cmds);
        }
    }

    /**
     * Check whether user is about to delete data outside of the download area.
     * Request confirmation if he is.
     */
    private static boolean checkAndConfirmOutlyingDeletes(Collection<OsmPrimitive> del) {
        Area a = Main.ds.getDataSourceArea();
        if (a != null) {
            for (OsmPrimitive osm : del) {
                if (osm instanceof Node && osm.id != 0) {
                    Node n = (Node) osm;
                    if (!a.contains(n.coor)) {
                        JPanel msg = new JPanel(new GridBagLayout());
                        msg.add(new JLabel(
                            "<html>" +
                            // leave message in one tr() as there is a grammatical connection.
                            tr("You are about to delete nodes outside of the area you have downloaded." +
                            "<br>" +
                            "This can cause problems because other objects (that you don't see) might use them." +
                            "<br>" +
                            "Do you really want to delete?") + "</html>"));
                        return DontShowAgainInfo.show("delete_outside_nodes", msg, false, JOptionPane.YES_NO_OPTION, JOptionPane.YES_OPTION);
                    }

                }
            }
        }
        return true;
    }
}
