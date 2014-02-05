// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.ModifiedConflictResolveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.VersionConflictResolveCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.nodes.NodeListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.nodes.NodeListMerger;
import org.openstreetmap.josm.gui.conflict.pair.properties.PropertiesMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.properties.PropertiesMerger;
import org.openstreetmap.josm.gui.conflict.pair.relation.RelationMemberListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.relation.RelationMemberMerger;
import org.openstreetmap.josm.gui.conflict.pair.tags.TagMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.tags.TagMerger;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An UI component for resolving conflicts between two {@link OsmPrimitive}s.
 *
 * This component emits {@link PropertyChangeEvent}s for three properties:
 * <ul>
 *   <li>{@link #RESOLVED_COMPLETELY_PROP} - new value is <code>true</code>, if the conflict is
 *   completely resolved</li>
 *   <li>{@link #MY_PRIMITIVE_PROP} - new value is the {@link OsmPrimitive} in the role of
 *   my primitive</li>
 *   <li>{@link #THEIR_PRIMITIVE_PROP} - new value is the {@link OsmPrimitive} in the role of
 *   their primitive</li>
 * </ul>
 *
 */
public class ConflictResolver extends JPanel implements PropertyChangeListener  {

    /* -------------------------------------------------------------------------------------- */
    /* Property names                                                                         */
    /* -------------------------------------------------------------------------------------- */
    /** name of the property indicating whether all conflicts are resolved,
     *  {@link #isResolvedCompletely()}
     */
    static public final String RESOLVED_COMPLETELY_PROP = ConflictResolver.class.getName() + ".resolvedCompletely";
    /**
     * name of the property for the {@link OsmPrimitive} in the role "my"
     */
    static public final String MY_PRIMITIVE_PROP = ConflictResolver.class.getName() + ".myPrimitive";

    /**
     * name of the property for the {@link OsmPrimitive} in the role "my"
     */
    static public final String THEIR_PRIMITIVE_PROP = ConflictResolver.class.getName() + ".theirPrimitive";

    private JTabbedPane tabbedPane = null;
    private TagMerger tagMerger;
    private NodeListMerger nodeListMerger;
    private RelationMemberMerger relationMemberMerger;
    private PropertiesMerger propertiesMerger;
    private final List<IConflictResolver> conflictResolvers = new ArrayList<IConflictResolver>();
    private OsmPrimitive my;
    private OsmPrimitive their;
    private Conflict<? extends OsmPrimitive> conflict;

    private ImageIcon mergeComplete;
    private ImageIcon mergeIncomplete;

    /** indicates whether the current conflict is resolved completely */
    private boolean resolvedCompletely;

    /**
     * loads the required icons
     */
    protected void loadIcons() {
        mergeComplete = ImageProvider.get("dialogs", "valid.png" );
        mergeIncomplete = ImageProvider.get("dialogs/conflict", "mergeincomplete.png" );
    }

    /**
     * builds the UI
     */
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

        conflictResolvers.add(propertiesMerger);
        conflictResolvers.add(tagMerger);
        conflictResolvers.add(nodeListMerger);
        conflictResolvers.add(relationMemberMerger);
    }

    /**
     * constructor
     */
    public ConflictResolver() {
        resolvedCompletely = false;
        build();
        loadIcons();
    }

    /**
     * Sets the {@link OsmPrimitive} in the role "my"
     *
     * @param my the primitive in the role "my"
     */
    protected void setMy(OsmPrimitive my) {
        OsmPrimitive old = this.my;
        this.my = my;
        if (old != this.my) {
            firePropertyChange(MY_PRIMITIVE_PROP, old, this.my);
        }
    }

    /**
     * Sets the {@link OsmPrimitive} in the role "their".
     *
     * @param their the primitive in the role "their"
     */
    protected void setTheir(OsmPrimitive their) {
        OsmPrimitive old = this.their;
        this.their = their;
        if (old != this.their) {
            firePropertyChange(THEIR_PRIMITIVE_PROP, old, this.their);
        }
    }

    /**
     * handles property change events
     * @param evt the event
     * @see TagMergeModel
     * @see ListMergeModel
     * @see PropertiesMergeModel
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TagMergeModel.PROP_NUM_UNDECIDED_TAGS)) {
            int newValue = (Integer)evt.getNewValue();
            if (newValue == 0) {
                tabbedPane.setTitleAt(1, tr("Tags"));
                tabbedPane.setToolTipTextAt(1, tr("No pending tag conflicts to be resolved"));
                tabbedPane.setIconAt(1, mergeComplete);
            } else {
                tabbedPane.setTitleAt(1, trn("Tags({0} conflict)", "Tags({0} conflicts)", newValue, newValue));
                tabbedPane.setToolTipTextAt(1, trn("{0} pending tag conflict to be resolved", "{0} pending tag conflicts to be resolved", newValue, newValue));
                tabbedPane.setIconAt(1, mergeIncomplete);
            }
            updateResolvedCompletely();
        } else if (evt.getPropertyName().equals(ListMergeModel.FROZEN_PROP)) {
            boolean frozen = (Boolean)evt.getNewValue();
            if (evt.getSource() == nodeListMerger.getModel() && my instanceof Way) {
                if (frozen) {
                    tabbedPane.setTitleAt(2, tr("Nodes(resolved)"));
                    tabbedPane.setToolTipTextAt(2, tr("Merged node list frozen. No pending conflicts in the node list of this way"));
                    tabbedPane.setIconAt(2, mergeComplete);
                } else {
                    tabbedPane.setTitleAt(2, tr("Nodes(with conflicts)"));
                    tabbedPane.setToolTipTextAt(2,tr("Pending conflicts in the node list of this way"));
                    tabbedPane.setIconAt(2, mergeIncomplete);
                }
            } else if (evt.getSource() == relationMemberMerger.getModel() && my instanceof Relation) {
                if (frozen) {
                    tabbedPane.setTitleAt(3, tr("Members(resolved)"));
                    tabbedPane.setToolTipTextAt(3, tr("Merged member list frozen. No pending conflicts in the member list of this relation"));
                    tabbedPane.setIconAt(3, mergeComplete);
                } else {
                    tabbedPane.setTitleAt(3, tr("Members(with conflicts)"));
                    tabbedPane.setToolTipTextAt(3, tr("Pending conflicts in the member list of this relation"));
                    tabbedPane.setIconAt(3, mergeIncomplete);
                }
            }
            updateResolvedCompletely();
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
            updateResolvedCompletely();
        } else if (PropertiesMergeModel.DELETE_PRIMITIVE_PROP.equals(evt.getPropertyName())) {
            for (IConflictResolver resolver: conflictResolvers) {
                resolver.deletePrimitive((Boolean) evt.getNewValue());
            }
        }
    }

    /**
     * populates the conflict resolver with the conflicts between my and their
     *
     * @param conflict the conflict data set
     */
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        setMy(conflict.getMy());
        setTheir(conflict.getTheir());
        this.conflict = conflict;
        propertiesMerger.populate(conflict);

        tabbedPane.setEnabledAt(0, true);
        tagMerger.populate(conflict);
        tabbedPane.setEnabledAt(1, true);

        if (my instanceof Node) {
            tabbedPane.setEnabledAt(2,false);
            tabbedPane.setEnabledAt(3,false);
        } else if (my instanceof Way) {
            nodeListMerger.populate(conflict);
            tabbedPane.setEnabledAt(2, true);
            tabbedPane.setEnabledAt(3, false);
            tabbedPane.setTitleAt(3,tr("Members"));
            tabbedPane.setIconAt(3, null);
        } else if (my instanceof Relation) {
            relationMemberMerger.populate(conflict);
            tabbedPane.setEnabledAt(2, false);
            tabbedPane.setTitleAt(2,tr("Nodes"));
            tabbedPane.setIconAt(2, null);
            tabbedPane.setEnabledAt(3, true);
        }
        updateResolvedCompletely();
        selectFirstTabWithConflicts();
    }

    public void selectFirstTabWithConflicts() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.isEnabledAt(i) && mergeIncomplete.equals(tabbedPane.getIconAt(i))) {
                tabbedPane.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Builds the resolution command(s) for the resolved conflicts in this
     * ConflictResolver
     *
     * @return the resolution command
     */
    public Command buildResolveCommand() {
        List<Command> commands = new ArrayList<Command>();

        if (tagMerger.getModel().getNumResolvedConflicts() > 0) {
            commands.add(tagMerger.getModel().buildResolveCommand(conflict));
        }
        commands.addAll(propertiesMerger.getModel().buildResolveCommand(conflict));
        if (my instanceof Way && nodeListMerger.getModel().isFrozen()) {
            NodeListMergeModel model = (NodeListMergeModel) nodeListMerger.getModel();
            commands.add(model.buildResolveCommand(conflict));
        } else if (my instanceof Relation && relationMemberMerger.getModel().isFrozen()) {
            RelationMemberListMergeModel model = (RelationMemberListMergeModel) relationMemberMerger.getModel();
            commands.add(model.buildResolveCommand((Relation) my, (Relation) their));
        }
        if (isResolvedCompletely()) {
            commands.add(new VersionConflictResolveCommand(conflict));
            commands.add(new ModifiedConflictResolveCommand(conflict));
        }
        return new SequenceCommand(tr("Conflict Resolution"), commands);
    }

    /**
     * Updates the state of the property {@link #RESOLVED_COMPLETELY_PROP}
     *
     */
    protected void updateResolvedCompletely() {
        boolean oldValueResolvedCompletely = resolvedCompletely;
        if (my instanceof Node) {
            // resolve the version conflict if this is a node and all tag
            // conflicts have been resolved
            //
            this.resolvedCompletely =
                tagMerger.getModel().isResolvedCompletely()
                && propertiesMerger.getModel().isResolvedCompletely();
        } else if (my instanceof Way) {
            // resolve the version conflict if this is a way, all tag
            // conflicts have been resolved, and conflicts in the node list
            // have been resolved
            //
            this.resolvedCompletely =
                tagMerger.getModel().isResolvedCompletely()
                &&  propertiesMerger.getModel().isResolvedCompletely()
                && nodeListMerger.getModel().isFrozen();
        }  else if (my instanceof Relation) {
            // resolve the version conflict if this is a relation, all tag
            // conflicts and all conflicts in the member list
            // have been resolved
            //
            this.resolvedCompletely =
                tagMerger.getModel().isResolvedCompletely()
                &&  propertiesMerger.getModel().isResolvedCompletely()
                && relationMemberMerger.getModel().isFrozen();
        }
        if (this.resolvedCompletely != oldValueResolvedCompletely) {
            firePropertyChange(RESOLVED_COMPLETELY_PROP, oldValueResolvedCompletely, this.resolvedCompletely);
        }
    }

    /**
     * Replies true all differences in this conflicts are resolved
     *
     * @return true all differences in this conflicts are resolved
     */
    public boolean isResolvedCompletely() {
        return resolvedCompletely;
    }

    public void unregisterListeners() {
        nodeListMerger.unlinkAsListener();
        relationMemberMerger.unlinkAsListener();
    }
}
