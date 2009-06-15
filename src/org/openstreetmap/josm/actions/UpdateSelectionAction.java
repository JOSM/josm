// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.PurgePrimitivesCommand;
import org.openstreetmap.josm.command.UndeletePrimitivesCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This action synchronizes a set of primitives with their state on the server.
 * 
 *
 */
public class UpdateSelectionAction extends JosmAction {

    static public int DEFAULT_MAX_SIZE_UPDATE_SELECTION = 50;

    /**
     * Undelete a node which is already deleted on the server. The API
     * doesn't offer a call for "undeleting" a node. We therefore create
     * a clone of the node which we flag as new. On the next upload the
     * server will assign the node a new id.
     * 
     * @param node the node to undelete
     */
    protected void  undeleteNode(Node node) {
        UndeletePrimitivesCommand cmd = new UndeletePrimitivesCommand(node);
        Main.main.undoRedo.add(cmd);
    }

    /**
     * Undelete a way which is already deleted on the server.
     * 
     * This method also checks whether there are additional nodes referred to by
     * this way which are deleted on the server too.
     * 
     * @param way the way to undelete
     * @see #undeleteNode(Node)
     */
    protected void undeleteWay(final Way way) {
        class NodeGoneChecker extends PleaseWaitRunnable {

            UndeletePrimitivesCommand cmd = null;

            public NodeGoneChecker() {
                super(tr("Undeleting Way..."), false);
            }

            @Override
            protected void cancel() {
                OsmApi.getOsmApi().cancel();
            }

            @Override
            protected void finish() {
                if (cmd != null) {
                    Main.main.undoRedo.add(cmd);
                }
            }

            /**
             * replies the subset of the node list which already
             * have an assigned id
             * 
             * @param way  the way
             * @return the node list
             */
            protected ArrayList<Node> getCandidateNodes(Way way) {
                ArrayList<Node> candidates = new ArrayList<Node>();
                for (Node n : way.nodes) {
                    if (n.id > 0 && ! candidates.contains(n)) {
                        candidates.add(n);
                    }
                }
                return candidates;
            }

            /**
             * checks whether a specific node is deleted on the server
             * 
             * @param n the node
             * @return true, if the node is deleted; false, otherwise
             * @throws OsmTransferException thrown, if an error occurs while communicating with the API
             */
            protected boolean isGone(Node n) throws OsmTransferException {
                OsmServerObjectReader reader = new OsmServerObjectReader(n.id, OsmPrimitiveType.from(n), true);
                try {
                    reader.parseOsm();
                } catch(OsmApiException e) {
                    if (e.getResponseCode() == HttpURLConnection.HTTP_GONE)
                        return true;
                    throw e;
                } catch(OsmTransferException e) {
                    throw e;
                }
                return false;
            }

            /**
             * displays a confirmation message. The user has to confirm that additional dependent
             * nodes should be undeleted too.
             * 
             * @param way  the way
             * @param dependent a list of dependent nodes which have to be undelete too
             * @return true, if the user confirms; false, otherwise
             */
            protected boolean confirmUndeleteDependentPrimitives(Way way, ArrayList<OsmPrimitive> dependent) {
                String [] options = {
                        tr("Yes, undelete them too"),
                        tr("No, cancel operation")
                };
                int ret = JOptionPane.showOptionDialog(
                        Main.parent,
                        tr("<html>There are {0} additional nodes used by way {1}<br>"
                                + "which are deleted on the server.<br>"
                                + "<br>"
                                + "Do you want to undelete these nodes too?</html>",
                                Long.toString(dependent.size()), Long.toString(way.id)),
                                tr("Undelete additional nodes?"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]
                );

                switch(ret) {
                case JOptionPane.CLOSED_OPTION: return false;
                case JOptionPane.YES_OPTION: return true;
                case JOptionPane.NO_OPTION: return false;
                }
                return false;

            }

            @Override
            protected void realRun() throws SAXException, IOException, OsmTransferException {
                ArrayList<Node> candidate = getCandidateNodes(way);
                ArrayList<OsmPrimitive> toDelete = new ArrayList<OsmPrimitive>();
                Main.pleaseWaitDlg.progress.setMinimum(0);
                Main.pleaseWaitDlg.progress.setMaximum(candidate.size());

                for (int i=0; i<candidate.size();i++) {
                    Node n = candidate.get(i);
                    Main.pleaseWaitDlg.progress.setValue(i);
                    Main.pleaseWaitDlg.currentAction.setText(tr("Checking whether node {0} is gone ...", n.id));
                    if (isGone(n)) {
                        toDelete.add(n);
                    }
                }
                if (toDelete.size() > 0) {
                    if (!confirmUndeleteDependentPrimitives(way, toDelete))
                        return;
                }

                toDelete.add(way);
                cmd = new UndeletePrimitivesCommand(toDelete);
            }
        }

        Main.worker.submit(new NodeGoneChecker());
    }

    /**
     * Undelete a relation which is already deleted on the server.
     * 
     * This method  checks whether there are additional primitives referred to by
     * this relation which are already deleted on the server.
     * 
     * @param r the relation
     * @see #undeleteNode(Node)
     */
    protected void undeleteRelation(final Relation r) {
        class RelationMemberGoneChecker extends PleaseWaitRunnable {

            UndeletePrimitivesCommand cmd = null;

            public RelationMemberGoneChecker() {
                super(tr("Undeleting relation..."), false);
            }

            @Override
            protected void cancel() {
                OsmApi.getOsmApi().cancel();
            }

            @Override
            protected void finish() {
                if (cmd != null) {
                    Main.main.undoRedo.add(cmd);
                }
            }

            protected ArrayList<OsmPrimitive> getCandidateRelationMembers(Relation r) {
                ArrayList<OsmPrimitive> candidates = new ArrayList<OsmPrimitive>();
                for (RelationMember m : r.members) {
                    if (m.member.id > 0 && !candidates.contains(m.member)) {
                        candidates.add(m.member);
                    }
                }
                return candidates;
            }

            protected boolean isGone(OsmPrimitive primitive) throws OsmTransferException {
                OsmServerObjectReader reader = new OsmServerObjectReader(
                        primitive.id,
                        OsmPrimitiveType.from(primitive),
                        true);
                try {
                    reader.parseOsm();
                } catch(OsmApiException e) {
                    if (e.getResponseCode() == HttpURLConnection.HTTP_GONE)
                        return true;
                    throw e;
                } catch(OsmTransferException e) {
                    throw e;
                }
                return false;
            }

            protected boolean confirmUndeleteDependentPrimitives(Relation r, ArrayList<OsmPrimitive> dependent) {
                String [] options = {
                        tr("Yes, undelete them too"),
                        tr("No, cancel operation")
                };
                int ret = JOptionPane.showOptionDialog(
                        Main.parent,
                        tr("<html>There are {0} additional primitives referred to by relation {1}<br>"
                                + "which are deleted on the server.<br>"
                                + "<br>"
                                + "Do you want to undelete them too?</html>",
                                Long.toString(dependent.size()), Long.toString(r.id)),
                                tr("Undelete dependent primitives?"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]
                );

                switch(ret) {
                case JOptionPane.CLOSED_OPTION: return false;
                case JOptionPane.YES_OPTION: return true;
                case JOptionPane.NO_OPTION: return false;
                }
                return false;

            }

            @Override
            protected void realRun() throws SAXException, IOException, OsmTransferException {
                ArrayList<OsmPrimitive> candidate = getCandidateRelationMembers(r);
                ArrayList<OsmPrimitive> toDelete = new ArrayList<OsmPrimitive>();
                Main.pleaseWaitDlg.progress.setMinimum(0);
                Main.pleaseWaitDlg.progress.setMaximum(candidate.size());

                for (int i=0; i<candidate.size();i++) {
                    OsmPrimitive primitive = candidate.get(i);
                    Main.pleaseWaitDlg.progress.setValue(i);
                    Main.pleaseWaitDlg.currentAction.setText(tr("Checking whether primitive {0} is gone ...", primitive.id));
                    if (isGone(primitive)) {
                        toDelete.add(primitive);
                    }
                }
                if (toDelete.size() > 0) {
                    if (!confirmUndeleteDependentPrimitives(r, toDelete))
                        return;
                }

                toDelete.add(r);
                cmd = new UndeletePrimitivesCommand(toDelete);
            }
        }

        Main.worker.submit(new RelationMemberGoneChecker());
    }

    /**
     * User has decided to keep his local version of a primitive which had been deleted
     * on the server
     * 
     * @param id the primitive id
     */
    protected void handlePrimitiveGoneKeepMine(long id) {
        OsmPrimitive primitive = Main.main.editLayer().data.getPrimitiveById(id);
        if (primitive instanceof Node) {
            undeleteNode((Node)primitive);
        } else if (primitive instanceof Way) {
            undeleteWay((Way)primitive);
        } else if (primitive instanceof Relation) {
            undeleteRelation((Relation)primitive);
        }
    }

    /**
     * User has decided to delete his local version of a primitive which had been deleted
     * on the server
     * 
     * @param id the primitive id
     */
    protected void handlePrimitiveGoneDeleteMine(long id) {
        OsmPrimitive primitive = Main.main.editLayer().data.getPrimitiveById(id);
        PurgePrimitivesCommand cmd = new PurgePrimitivesCommand(primitive);
        Main.main.undoRedo.add(cmd);
        Main.map.mapView.repaint();
    }

    /**
     * handle an exception thrown because a primitive was deleted on the server
     * 
     * @param id the primitive id
     */
    protected void handlePrimitiveGoneException(long id) {
        Object[] options = new Object[] {
                tr("Keep mine"),
                tr("Delete mine"),
                tr("Cancel")
        };
        Object defaultOption = options[0];
        String msg =  tr("<html>The OSM primitive with id <strong>{0}</strong> has been deleted<br>"
                + "on the server by another mapper.<br>"
                + "<br>"
                + "Click <strong>{1}</strong> to keep your primitive and ignore the deleted state.<br>"
                + "Your primitive will be assigend a new id.<br>"
                + "Click <strong>{2}</strong> to accept the state on the server and to delete your primitive.<br>"
                + "Click <strong>{3}</strong> to cancel.<br>",
                id, options[0], options[1], options[2]
        );
        int ret = JOptionPane.showOptionDialog(
                null,
                msg,
                tr("Primitive deleted on the server"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                defaultOption
        );
        switch(ret) {
        case JOptionPane.CLOSED_OPTION: return;
        case JOptionPane.CANCEL_OPTION: return;
        case 0: handlePrimitiveGoneKeepMine(id); break;
        case 1: handlePrimitiveGoneDeleteMine(id); break;
        default:
            // should not happen
            throw new IllegalStateException(tr("unexpected return value. Got {0}", ret));
        }
    }

    /**
     * handle an exception thrown during updating a primitive
     * 
     * @param id the id of the primitive
     * @param e the exception
     */
    protected void handleUpdateException(long id, Exception e) {
        if (e instanceof OsmApiException) {
            OsmApiException ex = (OsmApiException)e;
            // if the primitive was deleted on the server ask the user
            // whether he wants to undelete it
            //
            if (ex.getResponseCode() == HttpURLConnection.HTTP_GONE) {
                handlePrimitiveGoneException(id);
                return;
            }
        }

        e.printStackTrace();
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("Failed to update the selected primitives."),
                tr("Update failed"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * 
     * @param id
     */
    protected void handleMissingPrimitive(long id) {
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("Could not find primitive with id {0} in the current dataset", id),
                tr("Missing primitive"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Updates the primitive with id <code>id</code> with the current state kept on the server.
     * 
     * @param id
     */
    public void updatePrimitive(long id) {
        OsmPrimitive primitive = Main.main.editLayer().data.getPrimitiveById(id);
        if (primitive == null) {
            handleMissingPrimitive(id);
            return;
        }
        OsmServerObjectReader reader = new OsmServerObjectReader(
                id,
                OsmPrimitiveType.from(primitive),
                true);
        DataSet ds = null;
        try {
            ds = reader.parseOsm();
        } catch(Exception e) {
            handleUpdateException(id, e);
            return;
        }
        Main.main.editLayer().mergeFrom(ds);
    }


    public UpdateSelectionAction() {
        super(tr("Update Selection"),
                "updateselection",
                tr("Updates the currently selected primitives from the server"),
                Shortcut.registerShortcut("file:updateselection",
                        tr("Update Selection"),
                        KeyEvent.VK_U,
                        Shortcut.GROUP_HOTKEY + Shortcut.GROUPS_ALT2),
                        true);
    }


    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = Main.ds.getSelected();
        if (selection.size() == 0) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("There are no selected primitives to update."),
                    tr("Selection empty"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // check whether the current selection has an acceptable range.
        // We don't want to hammer the API with hundreds of individual
        // GET requests.
        //
        if (selection.size() > DEFAULT_MAX_SIZE_UPDATE_SELECTION) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>There are  <strong>{0}</strong> primitives <br>"
                            + "selected for individual update. Please reduce the selection<br>"
                            + "to max. {1} primitives.</html>",
                            selection.size(), DEFAULT_MAX_SIZE_UPDATE_SELECTION
                    ),
                    tr("Selection too big"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        for(OsmPrimitive primitive : selection) {
            // FIXME: users should be able to abort this loop
            //
            updatePrimitive(primitive.id);
        }
    }
}
