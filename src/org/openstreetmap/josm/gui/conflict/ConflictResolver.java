// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.VersionConflictResolveCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.nodes.NodeListMergeModel;
import org.openstreetmap.josm.gui.conflict.nodes.NodeListMerger;
import org.openstreetmap.josm.gui.conflict.properties.PropertiesMergeModel;
import org.openstreetmap.josm.gui.conflict.properties.PropertiesMerger;
import org.openstreetmap.josm.gui.conflict.relation.RelationMemberListMergeModel;
import org.openstreetmap.josm.gui.conflict.relation.RelationMemberMerger;
import org.openstreetmap.josm.gui.conflict.tags.TagMergeModel;
import org.openstreetmap.josm.gui.conflict.tags.TagMerger;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An UI component for resolving conflicts between two {@see OsmPrimitive}s.
 * 
 */
public class ConflictResolver extends JPanel implements PropertyChangeListener  {

    private static final Logger logger = Logger.getLogger(ConflictResolver.class.getName());

    private JTabbedPane tabbedPane = null;
    private TagMerger tagMerger;
    private NodeListMerger nodeListMerger;
    private RelationMemberMerger relationMemberMerger;
    private PropertiesMerger propertiesMerger;
    private OsmPrimitive my;
    private OsmPrimitive their;

    private ImageIcon mergeComplete;
    private ImageIcon mergeIncomplete;

    protected void loadIcons() {
        mergeComplete = ImageProvider.get("dialogs/conflict","mergecomplete.png" );
        mergeIncomplete = ImageProvider.get("dialogs/conflict","mergeincomplete.png" );
    }

    protected void build() {
        tabbedPane = new JTabbedPane();

        propertiesMerger = new PropertiesMerger();
        propertiesMerger.setName("panel.propertiesmerger");
        propertiesMerger.getModel().addPropertyChangeListener(this);
        tabbedPane.add(tr("Properties"), propertiesMerger);

        tagMerger = new TagMerger();
        tagMerger.setName("panel.tagmerger");
        tagMerger.getModel().addPropertyChangeListener(this);
        tabbedPane.add(tr("Tags"), tagMerger);

        nodeListMerger = new NodeListMerger();
        nodeListMerger.setName("panel.nodelistmerger");
        nodeListMerger.getModel().addPropertyChangeListener(this);
        tabbedPane.add(tr("Nodes"), nodeListMerger);

        relationMemberMerger = new RelationMemberMerger();
        relationMemberMerger.setName("panel.relationmembermerger");
        relationMemberMerger.getModel().addPropertyChangeListener(this);
        tabbedPane.add(tr("Members"), relationMemberMerger);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    public ConflictResolver() {
        build();
        loadIcons();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TagMergeModel.PROP_NUM_UNDECIDED_TAGS)) {
            int newValue = (Integer)evt.getNewValue();
            if (newValue == 0) {
                tabbedPane.setTitleAt(1, tr("Tags"));
                tabbedPane.setToolTipTextAt(1, tr("No pending tag conflicts to be resolved"));
                tabbedPane.setIconAt(1, mergeComplete);
            } else {
                tabbedPane.setTitleAt(1, tr("Tags({0} conflicts)", newValue));
                tabbedPane.setToolTipTextAt(1, tr("{0} pending tag conflicts to be resolved"));
                tabbedPane.setIconAt(1, mergeIncomplete);
            }
        } else if (evt.getPropertyName().equals(ListMergeModel.FROZEN_PROP)) {
            boolean frozen = (Boolean)evt.getNewValue();
            if (frozen && evt.getSource() == nodeListMerger.getModel()) {
                tabbedPane.setTitleAt(2, tr("Nodes(resolved)"));
                tabbedPane.setToolTipTextAt(2, tr("Merged node list frozen. No pending conflicts in the node list of this way"));
                tabbedPane.setIconAt(2, mergeComplete);
            } else {
                tabbedPane.setTitleAt(2, tr("Nodes(with conflicts)"));
                tabbedPane.setToolTipTextAt(2,tr("Pending conflicts in the node list of this way"));
                tabbedPane.setIconAt(2, mergeIncomplete);
            }
            if (frozen && evt.getSource() == relationMemberMerger.getModel()) {
                tabbedPane.setTitleAt(3, tr("Members(resolved)"));
                tabbedPane.setToolTipTextAt(3, tr("Merged member list frozen. No pending conflicts in the member list of this relation"));
                tabbedPane.setIconAt(3, mergeComplete);
            } else {
                tabbedPane.setTitleAt(3, tr("Members(with conflicts)"));
                tabbedPane.setToolTipTextAt(3, tr("Pending conflicts in the member list of this relation"));
                tabbedPane.setIconAt(3, mergeIncomplete);
            }
        } else if (evt.getPropertyName().equals(PropertiesMergeModel.RESOLVED_COMPLETELY_PROP)) {
            boolean resolved = (Boolean)evt.getNewValue();
            if (resolved) {
                tabbedPane.setTitleAt(0, tr("Properties"));
                tabbedPane.setToolTipTextAt(0, tr("No pending property conflicts"));
                tabbedPane.setIconAt(0, mergeComplete);
            } else {
                tabbedPane.setTitleAt(0, tr("Properties(with conflicts)"));
                tabbedPane.setToolTipTextAt(0, tr("Pending property conflicts to be resolved"));
                tabbedPane.setIconAt(0, mergeIncomplete);
            }
        }
    }


    /**
     * populates the conflict resolver with the conflicts between my and their
     * 
     * @param my   my primitive (i.e. the primitive in the local dataset)
     * @param their their primitive (i.e. the primitive in the server dataset)
     * 
     */
    public void populate(OsmPrimitive my, OsmPrimitive their) {
        this.my = my;
        this.their =  their;
        propertiesMerger.getModel().populate(my, their);
        tabbedPane.setEnabledAt(0, true);
        tagMerger.getModel().populate(my, their);
        tabbedPane.setEnabledAt(1, true);

        if (my instanceof Node) {
            tabbedPane.setEnabledAt(2,false);
            tabbedPane.setEnabledAt(3,false);
        } else if (my instanceof Way) {
            nodeListMerger.populate((Way)my, (Way)their);
            tabbedPane.setEnabledAt(2, true);
            tabbedPane.setEnabledAt(3, false);
            tabbedPane.setTitleAt(3,tr("Members"));
            tabbedPane.setIconAt(3, null);
        } else if (my instanceof Relation) {
            relationMemberMerger.populate((Relation)my, (Relation)their);
            tabbedPane.setEnabledAt(2, false);
            tabbedPane.setTitleAt(2,tr("Nodes"));
            tabbedPane.setIconAt(2, null);
            tabbedPane.setEnabledAt(3, true);
        }
    }

    /**
     * Builds the resolution command(s) for for the resolved conflicts in this
     * ConflictResolver
     * 
     * @return the resolution command
     */
    public Command buildResolveCommand() {
        ArrayList<Command> commands = new ArrayList<Command>();
        if (tagMerger.getModel().getNumResolvedConflicts() > 0) {
            commands.add(tagMerger.getModel().buildResolveCommand(my, their));
        }
        commands.addAll(propertiesMerger.getModel().buildResolveCommand(my, their));
        if (my instanceof Way && nodeListMerger.getModel().isFrozen()) {
            NodeListMergeModel model  =(NodeListMergeModel)nodeListMerger.getModel();
            commands.add(model.buildResolveCommand((Way)my, (Way)their));
        } else if (my instanceof Relation && relationMemberMerger.getModel().isFrozen()) {
            RelationMemberListMergeModel model  =(RelationMemberListMergeModel)relationMemberMerger.getModel();
            commands.add(model.buildResolveCommand((Relation)my, (Relation)their));
        }
        if (isResolvedCompletely()) {
            commands.add(
                    new VersionConflictResolveCommand(my, their)
            );
        }
        return new SequenceCommand(tr("Conflict Resolution"), commands);
    }

    public boolean isResolvedCompletely() {
        if (my instanceof Node) {
            // resolve the version conflict if this is a node and all tag
            // conflicts have been resolved
            //
            if (tagMerger.getModel().isResolvedCompletely()
                    && propertiesMerger.getModel().isResolvedCompletely())
                return true;
        } else if (my instanceof Way) {
            // resolve the version conflict if this is a way, all tag
            // conflicts have been resolved, and conflicts in the node list
            // have been resolved
            //
            if (tagMerger.getModel().isResolvedCompletely()
                    &&  propertiesMerger.getModel().isResolvedCompletely()
                    && nodeListMerger.getModel().isFrozen())
                return true;
        }  else if (my instanceof Relation) {
            // resolve the version conflict if this is a relation, all tag
            // conflicts and all conflicts in the member list
            // have been resolved
            //
            if (tagMerger.getModel().isResolvedCompletely()
                    &&  propertiesMerger.getModel().isResolvedCompletely()
                    && relationMemberMerger.getModel().isFrozen())
                return true;
        }
        return false;
    }
}
