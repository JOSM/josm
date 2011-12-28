// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import javax.swing.SwingUtilities;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.MultipolygonCreate;
import org.openstreetmap.josm.data.osm.MultipolygonCreate.JoinedPolygon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Create multipolygon from selected ways automatically.
 *
 * New relation with type=multipolygon is created
 *
 * If one or more of ways is already in relation with type=multipolygon or the
 * way is not closed, then error is reported and no relation is created
 *
 * The "inner" and "outer" roles are guessed automatically. First, bbox is
 * calculated for each way. then the largest area is assumed to be outside and
 * the rest inside. In cases with one "outside" area and several cut-ins, the
 * guess should be always good ... In more complex (multiple outer areas) or
 * buggy (inner and outer ways intersect) scenarios the result is likely to be
 * wrong.
 */
public class CreateMultipolygonAction extends JosmAction {

    public CreateMultipolygonAction() {
        super(tr("Create multipolygon"), "multipoly_create", tr("Create multipolygon."),
                Shortcut.registerShortcut("tools:multipoly", tr("Tool: {0}", tr("Create multipolygon")),
                        KeyEvent.VK_A, Shortcut.GROUP_DIRECT, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK ), true);
    }
    /**
     * The action button has been clicked
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent e) {
        if (Main.main.getEditLayer() == null) {
            JOptionPane.showMessageDialog(Main.parent, tr("No data loaded."));
            return;
        }

        Collection<Way> selectedWays = Main.main.getCurrentDataSet().getSelectedWays();

        if (selectedWays.size() < 1) {
            // Sometimes it make sense creating multipoly of only one way (so it will form outer way)
            // and then splitting the way later (so there are multiple ways forming outer way)
            JOptionPane.showMessageDialog(Main.parent, tr("You must select at least one way."));
            return;
        }

        MultipolygonCreate polygon = this.analyzeWays(selectedWays);

        if (polygon == null)
            return;                   //could not make multipolygon.

        final Relation relation = this.createRelation(polygon);

        if (Main.pref.getBoolean("multipoly.show-relation-editor", false)) {
            //Open relation edit window, if set up in preferences
            RelationEditor editor = RelationEditor.getEditor(Main.main.getEditLayer(), relation, null);

            editor.setModal(true);
            editor.setVisible(true);

            //TODO: cannot get the resulting relation from RelationEditor :(.
            /*
            if (relationCountBefore < relationCountAfter) {
                //relation saved, clean up the tags
                List<Command> list = this.removeTagsFromInnerWays(relation);
                if (list.size() > 0)
                {
                    Main.main.undoRedo.add(new SequenceCommand(tr("Remove tags from multipolygon inner ways"), list));
                }
            }
             */

        } else {
            //Just add the relation
            List<Command> list = this.removeTagsFromInnerWays(relation);
            list.add(new AddCommand(relation));
            Main.main.undoRedo.add(new SequenceCommand(tr("Create multipolygon"), list));
            // Use 'SwingUtilities.invokeLater' to make sure the relationListDialog
            // knows about the new relation before we try to select it.
            // (Yes, we are already in event dispatch thread. But DatasetEventManager
            // uses 'SwingUtilities.invokeLater' to fire events so we have to do
            // the same.)
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Main.map.relationListDialog.selectRelation(relation);
                }
            });
        }


    }

    /** Enable this action only if something is selected */
    @Override protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    /** Enable this action only if something is selected */
    @Override protected void updateEnabledState(Collection < ? extends OsmPrimitive > selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    /**
     * This method analyzes ways and creates multipolygon.
     * @param selectedWays
     * @return null, if there was a problem with the ways.
     */
    private MultipolygonCreate analyzeWays(Collection < Way > selectedWays) {

        MultipolygonCreate pol = new MultipolygonCreate();
        String error = pol.makeFromWays(selectedWays);

        if (error != null) {
            JOptionPane.showMessageDialog(Main.parent, error);
            return null;
        } else {
            return pol;
        }
    }

    /**
     * Builds a relation from polygon ways.
     * @param pol
     * @return
     */
    private Relation createRelation(MultipolygonCreate pol) {
        // Create new relation
        Relation rel = new Relation();
        rel.put("type", "multipolygon");
        // Add ways to it
        for (JoinedPolygon jway:pol.outerWays) {
            for (Way way:jway.ways) {
                rel.addMember(new RelationMember("outer", way));
            }
        }

        for (JoinedPolygon jway:pol.innerWays) {
            for (Way way:jway.ways) {
                rel.addMember(new RelationMember("inner", way));
            }
        }
        return rel;
    }

    /**
     * This method removes tags/value pairs from inner ways that are present in relation or outer ways.
     * @param relation
     */
    private List<Command> removeTagsFromInnerWays(Relation relation) {
        Map<String, String> values = new HashMap<String, String>();

        if (relation.hasKeys()){
            for(String key: relation.keySet()) {
                values.put(key, relation.get(key));
            }
        }

        List<Way> innerWays = new ArrayList<Way>();

        for (RelationMember m: relation.getMembers()) {

            if (m.hasRole() && m.getRole() == "inner" && m.isWay() && m.getWay().hasKeys()) {
                innerWays.add(m.getWay());
            }

            if (m.hasRole() && m.getRole() == "outer" && m.isWay() && m.getWay().hasKeys()) {
                Way way = m.getWay();
                for (String key: way.keySet()) {
                    if (!values.containsKey(key)) { //relation values take precedence
                        values.put(key, way.get(key));
                    }
                }
            }
        }

        List<Command> commands = new ArrayList<Command>();

        for(String key: values.keySet()) {
            List<OsmPrimitive> affectedWays = new ArrayList<OsmPrimitive>();
            String value = values.get(key);

            for (Way way: innerWays) {
                if (value.equals(way.get(key))) {
                    affectedWays.add(way);
                }
            }

            if (affectedWays.size() > 0) {
                commands.add(new ChangePropertyCommand(affectedWays, key, null));
            }
        }

        return commands;
    }
}
