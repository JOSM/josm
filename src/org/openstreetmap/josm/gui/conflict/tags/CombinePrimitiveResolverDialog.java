package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This dialog helps to resolve conflicts occurring when ways are combined ore
 * primitives are merged.
 * 
 * There is a singleton instance of this dialog which can be retrieved using
 * {@see #getInstance()}.
 * 
 * The dialog uses two models: one  for resolving tag conflicts, the other
 * for resolving conflicts in relation memberships. For both models there are accessors,
 * i.e {@see #getTagConflictResolverModel()} and {@see #getRelationMemberConflictResolverModel()}.
 * 
 * Models have to be <strong>populated</strong> before the dialog is launched. Example:
 * <pre>
 *    CombinePrimitiveResolverDialog dialog = CombinePrimitiveResolverDialog.getInstance();
 *    dialog.getTagConflictResolverModel().populate(aTagCollection);
 *    dialog.getRelationMemberConflictResolverModel().populate(aRelationLinkCollection);
 *    dialog.prepareDefaultDecisions();
 * </pre>
 * 
 * You should also set the target primitive which other primitives (ways or nodes) are
 * merged to, see {@see #setTargetPrimitive(OsmPrimitive)}.
 * 
 * After the dialog closed use {@see #isCancelled()} to check whether the user cancelled
 * the dialog. If it wasn't canceled you may build a collection of {@see Command} objects
 * which reflect the conflict resolution decisions the user made in the dialog:
 * see {@see #buildResolutionCommands()}
 * 
 *
 */
public class CombinePrimitiveResolverDialog extends JDialog {

    static private CombinePrimitiveResolverDialog instance;

    public static CombinePrimitiveResolverDialog getInstance() {
        if (instance == null) {
            instance = new CombinePrimitiveResolverDialog(Main.parent);
        }
        return instance;
    }

    private JSplitPane spTagConflictTypes;
    private TagConflictResolver pnlTagConflictResolver;
    private RelationMemberConflictResolver pnlRelationMemberConflictResolver;
    private boolean cancelled;
    private JPanel pnlButtons;
    private OsmPrimitive targetPrimitive;



    public OsmPrimitive getTargetPrimitmive() {
        return targetPrimitive;
    }

    public void setTargetPrimitive(OsmPrimitive primitive) {
        this.targetPrimitive = primitive;
        updateTitle();
    }

    protected void updateTitle() {
        if (targetPrimitive == null) {
            setTitle(tr("Conflicts when combining primitives"));
            return;
        }
        if (targetPrimitive instanceof Way) {
            setTitle(
                    tr(
                            "Conflicts when combining ways - combined way is ''{0}''",
                            targetPrimitive.getDisplayName(DefaultNameFormatter.getInstance())
                    )
            );
        } else if (targetPrimitive instanceof Node) {
            setTitle(
                    tr(
                            "Conflicts when merging nodes - merged node is ''{0}''",
                            targetPrimitive.getDisplayName(DefaultNameFormatter.getInstance())
                    )
            );
        }
    }

    protected void build() {
        getContentPane().setLayout(new BorderLayout());
        updateTitle();
        spTagConflictTypes = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        spTagConflictTypes.setTopComponent(buildTagConflictResolverPanel());
        spTagConflictTypes.setBottomComponent(buildRelationMemberConflictResolverPanel());
        getContentPane().add(pnlButtons = buildButtonPanel(), BorderLayout.SOUTH);
        addWindowListener(new AdjustDividerLocationAction());
    }

    protected JPanel buildTagConflictResolverPanel() {
        pnlTagConflictResolver = new TagConflictResolver();
        return pnlTagConflictResolver;
    }

    protected JPanel buildRelationMemberConflictResolverPanel() {
        pnlRelationMemberConflictResolver = new RelationMemberConflictResolver();
        return pnlRelationMemberConflictResolver;
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        // -- apply button
        ApplyAction applyAction = new ApplyAction();
        pnlTagConflictResolver.getModel().addPropertyChangeListener(applyAction);
        pnlRelationMemberConflictResolver.getModel().addPropertyChangeListener(applyAction);
        pnl.add(new SideButton(applyAction));

        // -- cancel button
        CancelAction cancelAction = new CancelAction();
        pnl.add(new SideButton(cancelAction));

        return pnl;
    }

    public CombinePrimitiveResolverDialog(Component owner) {
        super(JOptionPane.getFrameForComponent(owner),true /* modal */);
        build();
    }

    public TagConflictResolverModel getTagConflictResolverModel() {
        return pnlTagConflictResolver.getModel();
    }

    public RelationMemberConflictResolverModel getRelationMemberConflictResolverModel() {
        return pnlRelationMemberConflictResolver.getModel();
    }

    protected List<Command> buildTagChangeCommand(OsmPrimitive primitive, TagCollection tc) {
        LinkedList<Command> cmds = new LinkedList<Command>();
        for (String key : tc.getKeys()) {
            if (tc.hasUniqueEmptyValue(key)) {
                if (primitive.get(key) != null) {
                    cmds.add(new ChangePropertyCommand(primitive, key, null));
                }
            } else {
                String value = tc.getJoinedValues(key);
                if (!value.equals(primitive.get(key))) {
                    cmds.add(new ChangePropertyCommand(primitive, key, value));
                }
            }
        }
        return cmds;
    }

    public List<Command> buildResolutionCommands() {
        List<Command> cmds = new LinkedList<Command>();

        if (getTagConflictResolverModel().getNumDecisions() >0) {
            TagCollection tc = getTagConflictResolverModel().getResolution();
            cmds.addAll(buildTagChangeCommand(targetPrimitive, tc));
        }

        if (getRelationMemberConflictResolverModel().getNumDecisions() >0) {
            cmds.addAll(getRelationMemberConflictResolverModel().buildResolutionCommands(targetPrimitive));
        }

        Command cmd = pnlRelationMemberConflictResolver.buildTagApplyCommands(
                getRelationMemberConflictResolverModel().getModifiedRelations(targetPrimitive)
        );
        if (cmd != null) {
            cmds.add(cmd);
        }
        return cmds;
    }

    protected void prepareDefaultTagDecisions() {
        TagConflictResolverModel model = getTagConflictResolverModel();
        for (int i =0; i< model.getRowCount(); i++) {
            MultiValueResolutionDecision decision = model.getDecision(i);
            List<String> values = decision.getValues();
            values.remove("");
            if (values.size() == 1) {
                decision.keepOne(values.get(0));
            } else {
                decision.keepAll();
            }
        }
        model.refresh();
    }

    protected void prepareDefaultRelationDecisions() {
        RelationMemberConflictResolverModel model = getRelationMemberConflictResolverModel();
        for (int i=0; i < model.getNumDecisions(); i++) {
            model.getDecision(i).decide(RelationMemberConflictDecisionType.REPLACE);
        }
        model.refresh();
    }

    public void prepareDefaultDecisions() {
        prepareDefaultTagDecisions();
        prepareDefaultRelationDecisions();
    }

    protected JPanel buildEmptyConflictsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(new JLabel(tr("No conflicts to resolve")));
        return pnl;
    }

    protected void prepareGUIBeforeConflictResolutionStarts() {
        RelationMemberConflictResolverModel relModel = getRelationMemberConflictResolverModel();
        TagConflictResolverModel tagModel = getTagConflictResolverModel();
        getContentPane().removeAll();
        if (relModel.getNumDecisions() > 0 && tagModel.getNumDecisions() > 0) {
            spTagConflictTypes.setTopComponent(pnlTagConflictResolver);
            spTagConflictTypes.setBottomComponent(pnlRelationMemberConflictResolver);
            getContentPane().add(spTagConflictTypes, BorderLayout.CENTER);
        } else if (relModel.getNumDecisions() > 0) {
            getContentPane().add(pnlRelationMemberConflictResolver, BorderLayout.CENTER);
        } else if (tagModel.getNumDecisions() >0) {
            getContentPane().add(pnlTagConflictResolver, BorderLayout.CENTER);
        } else {
            getContentPane().add(buildEmptyConflictsPanel(), BorderLayout.CENTER);
        }
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
        validate();
        int numTagDecisions = getTagConflictResolverModel().getNumDecisions();
        int numRelationDecisions = getRelationMemberConflictResolverModel().getNumDecisions();
        if (numTagDecisions > 0 &&  numRelationDecisions > 0) {
            spTagConflictTypes.setDividerLocation(0.5);
        }
        pnlRelationMemberConflictResolver.prepareForEditing();
    }

    protected void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            prepareGUIBeforeConflictResolutionStarts();
            new WindowGeometry(
                    getClass().getName()  + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(600,400)
                    )
            ).applySafe(this);
            setCancelled(false);
        } else {
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }


    class CancelAction extends AbstractAction {

        public CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution"));
            putValue(Action.NAME, tr("Cancel"));
            putValue(Action.SMALL_ICON, ImageProvider.get("", "cancel"));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent arg0) {
            setCancelled(true);
            setVisible(false);
        }
    }

    class ApplyAction extends AbstractAction implements PropertyChangeListener {

        public ApplyAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts"));
            putValue(Action.NAME, tr("Apply"));
            putValue(Action.SMALL_ICON, ImageProvider.get("ok"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }

        protected void updateEnabledState() {
            setEnabled(
                    pnlTagConflictResolver.getModel().getNumConflicts() == 0
                    && pnlRelationMemberConflictResolver.getModel().getNumConflicts() == 0
            );
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(TagConflictResolverModel.NUM_CONFLICTS_PROP)) {
                updateEnabledState();
            }
            if (evt.getPropertyName().equals(RelationMemberConflictResolverModel.NUM_CONFLICTS_PROP)) {
                updateEnabledState();
            }
        }
    }

    class AdjustDividerLocationAction extends WindowAdapter {
        @Override
        public void windowOpened(WindowEvent e) {
            int numTagDecisions = getTagConflictResolverModel().getNumDecisions();
            int numRelationDecisions = getRelationMemberConflictResolverModel().getNumDecisions();
            if (numTagDecisions > 0 &&  numRelationDecisions > 0) {
                spTagConflictTypes.setDividerLocation(0.5);
            }
        }
    }
}
