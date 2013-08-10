// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.TreeSet;
import javax.swing.JOptionPane;

import javax.swing.SwingUtilities;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.MultipolygonCreate;
import org.openstreetmap.josm.data.osm.MultipolygonCreate.JoinedPolygon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
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
            KeyEvent.VK_A, Shortcut.ALT_CTRL), true);
    }
    /**
     * The action button has been clicked
     *
     * @param e Action Event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (Main.main.getEditLayer() == null) {
            new Notification(
                    tr("No data loaded."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        Collection<Way> selectedWays = Main.main.getCurrentDataSet().getSelectedWays();

        if (selectedWays.size() < 1) {
            // Sometimes it make sense creating multipoly of only one way (so it will form outer way)
            // and then splitting the way later (so there are multiple ways forming outer way)
            new Notification(
                    tr("You must select at least one way."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
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
            List<Command> list = this.removeTagsFromWaysIfNeeded(relation);
            list.add(new AddCommand(relation));
            Main.main.undoRedo.add(new SequenceCommand(tr("Create multipolygon"), list));
            // Use 'SwingUtilities.invokeLater' to make sure the relationListDialog
            // knows about the new relation before we try to select it.
            // (Yes, we are already in event dispatch thread. But DatasetEventManager
            // uses 'SwingUtilities.invokeLater' to fire events so we have to do
            // the same.)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
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

    /**
      * Enable this action only if something is selected
      *
      * @param selection the current selection, gets tested for emptyness
      */
    @Override protected void updateEnabledState(Collection < ? extends OsmPrimitive > selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    /**
     * This method analyzes ways and creates multipolygon.
     * @param selectedWays list of selected ways
     * @return <code>null</code>, if there was a problem with the ways.
     */
    private MultipolygonCreate analyzeWays(Collection < Way > selectedWays) {

        MultipolygonCreate pol = new MultipolygonCreate();
        String error = pol.makeFromWays(selectedWays);

        if (error != null) {
            new Notification(error)
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .show();
            return null;
        } else {
            return pol;
        }
    }

    /**
     * Builds a relation from polygon ways.
     * @param pol data storage class containing polygon information
     * @return multipolygon relation
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

    static public final List<String> DEFAULT_LINEAR_TAGS = Arrays.asList(new String[] {"barrier", "source"});

    /**
     * This method removes tags/value pairs from inner and outer ways and put them on relation if necessary
     * Function was extended in reltoolbox plugin by Zverikk and copied back to the core
     * @param relation the multipolygon style relation to process
     * @return a list of commands to execute
     */
    private List<Command> removeTagsFromWaysIfNeeded( Relation relation ) {
        Map<String, String> values = new HashMap<String, String>();

        if( relation.hasKeys() ) {
            for( String key : relation.keySet() ) {
                values.put(key, relation.get(key));
            }
        }

        List<Way> innerWays = new ArrayList<Way>();
        List<Way> outerWays = new ArrayList<Way>();

        Set<String> conflictingKeys = new TreeSet<String>();

        for( RelationMember m : relation.getMembers() ) {

            if( m.hasRole() && "inner".equals(m.getRole()) && m.isWay() && m.getWay().hasKeys() ) {
                innerWays.add(m.getWay());
            }

            if( m.hasRole() && "outer".equals(m.getRole()) && m.isWay() && m.getWay().hasKeys() ) {
                Way way = m.getWay();
                outerWays.add(way);

                for( String key : way.keySet() ) {
                    if( !values.containsKey(key) ) { //relation values take precedence
                        values.put(key, way.get(key));
                    } else if( !relation.hasKey(key) && !values.get(key).equals(way.get(key)) ) {
                        conflictingKeys.add(key);
                    }
                }
            }
        }

        // filter out empty key conflicts - we need second iteration
        if( !Main.pref.getBoolean("multipoly.alltags", false) )
            for( RelationMember m : relation.getMembers() )
                if( m.hasRole() && m.getRole().equals("outer") && m.isWay() )
                    for( String key : values.keySet() )
                        if( !m.getWay().hasKey(key) && !relation.hasKey(key) )
                            conflictingKeys.add(key);

        for( String key : conflictingKeys )
            values.remove(key);

        for( String linearTag : Main.pref.getCollection("multipoly.lineartagstokeep", DEFAULT_LINEAR_TAGS) )
            values.remove(linearTag);

        if( values.containsKey("natural") && values.get("natural").equals("coastline") )
            values.remove("natural");

        values.put("area", "yes");

        List<Command> commands = new ArrayList<Command>();
        boolean moveTags = Main.pref.getBoolean("multipoly.movetags", true);

        for( String key : values.keySet() ) {
            List<OsmPrimitive> affectedWays = new ArrayList<OsmPrimitive>();
            String value = values.get(key);

            for( Way way : innerWays ) {
                if( way.hasKey(key) && (value.equals(way.get(key))) ) {
                    affectedWays.add(way);
                }
            }

            if( moveTags ) {
                // remove duplicated tags from outer ways
                for( Way way : outerWays ) {
                    if( way.hasKey(key) ) {
                        affectedWays.add(way);
                    }
                }
            }

            if(!affectedWays.isEmpty()) {
                // reset key tag on affected ways
                commands.add(new ChangePropertyCommand(affectedWays, key, null));
            }
        }

        if( moveTags ) {
            // add those tag values to the relation

            boolean fixed = false;
            Relation r2 = new Relation(relation);
            for( String key : values.keySet() ) {
                if( !r2.hasKey(key) && !key.equals("area") ) {
                    if( relation.isNew() )
                        relation.put(key, values.get(key));
                    else
                        r2.put(key, values.get(key));
                    fixed = true;
                }
            }
            if( fixed && !relation.isNew() )
                commands.add(new ChangeCommand(relation, r2));
        }

        return commands;
    }
}
