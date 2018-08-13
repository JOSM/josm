// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.AutoAdjustingSplitPane;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * This dialog helps to resolve conflicts occurring when ways are combined or
 * nodes are merged.
 *
 * Usage: {@link #launchIfNecessary} followed by {@link #buildResolutionCommands}.
 *
 * Prior to {@link #launchIfNecessary}, the following usage sequence was needed:
 *
 * The dialog uses two models: one  for resolving tag conflicts, the other
 * for resolving conflicts in relation memberships. For both models there are accessors,
 * i.e {@link #getTagConflictResolverModel()} and {@link #getRelationMemberConflictResolverModel()}.
 *
 * Models have to be <strong>populated</strong> before the dialog is launched. Example:
 * <pre>
 *    CombinePrimitiveResolverDialog dialog = new CombinePrimitiveResolverDialog(MainApplication.getMainFrame());
 *    dialog.getTagConflictResolverModel().populate(aTagCollection);
 *    dialog.getRelationMemberConflictResolverModel().populate(aRelationLinkCollection);
 *    dialog.prepareDefaultDecisions();
 * </pre>
 *
 * You should also set the target primitive which other primitives (ways or nodes) are
 * merged to, see {@link #setTargetPrimitive(OsmPrimitive)}.
 *
 * After the dialog is closed use {@link #isApplied()} to check whether the dialog has been
 * applied. If it was applied you may build a collection of {@link Command} objects
 * which reflect the conflict resolution decisions the user made in the dialog:
 * see {@link #buildResolutionCommands()}
 */
public class CombinePrimitiveResolverDialog extends JDialog {

    private AutoAdjustingSplitPane spTagConflictTypes;
    private final TagConflictResolverModel modelTagConflictResolver;
    protected TagConflictResolver pnlTagConflictResolver;
    private final RelationMemberConflictResolverModel modelRelConflictResolver;
    protected RelationMemberConflictResolver pnlRelationMemberConflictResolver;
    private final CombinePrimitiveResolver primitiveResolver;
    private boolean applied;
    private JPanel pnlButtons;
    protected transient OsmPrimitive targetPrimitive;

    /** the private help action */
    private ContextSensitiveHelpAction helpAction;
    /** the apply button */
    private JButton btnApply;

    /**
     * Constructs a new {@code CombinePrimitiveResolverDialog}.
     * @param parent The parent component in which this dialog will be displayed.
     */
    public CombinePrimitiveResolverDialog(Component parent) {
        this(parent, new TagConflictResolverModel(), new RelationMemberConflictResolverModel());
    }

    /**
     * Constructs a new {@code CombinePrimitiveResolverDialog}.
     * @param parent The parent component in which this dialog will be displayed.
     * @param tagModel tag conflict resolver model
     * @param relModel relation member conflict resolver model
     * @since 11772
     */
    public CombinePrimitiveResolverDialog(Component parent,
            TagConflictResolverModel tagModel, RelationMemberConflictResolverModel relModel) {
        super(GuiHelper.getFrameForComponent(parent), ModalityType.DOCUMENT_MODAL);
        this.modelTagConflictResolver = tagModel;
        this.modelRelConflictResolver = relModel;
        this.primitiveResolver = new CombinePrimitiveResolver(tagModel, relModel);
        build();
    }

    /**
     * Replies the target primitive the collection of primitives is merged or combined to.
     *
     * @return the target primitive
     * @since 11772 (naming)
     */
    public OsmPrimitive getTargetPrimitive() {
        return targetPrimitive;
    }

    /**
     * Sets the primitive the collection of primitives is merged or combined to.
     *
     * @param primitive the target primitive
     */
    public void setTargetPrimitive(final OsmPrimitive primitive) {
        setTargetPrimitive(primitive, true);
    }

    /**
     * Sets the primitive the collection of primitives is merged or combined to.
     *
     * @param primitive the target primitive
     * @param updateTitle {@code true} to call {@link #updateTitle} in EDT (can be a slow operation)
     * @since 11626
     */
    private void setTargetPrimitive(final OsmPrimitive primitive, boolean updateTitle) {
        this.targetPrimitive = primitive;
        if (updateTitle) {
            GuiHelper.runInEDTAndWait(this::updateTitle);
        }
    }

    /**
     * Updates the dialog title.
     */
    protected void updateTitle() {
        if (targetPrimitive == null) {
            setTitle(tr("Conflicts when combining primitives"));
            return;
        }
        if (targetPrimitive instanceof Way) {
            setTitle(tr("Conflicts when combining ways - combined way is ''{0}''", targetPrimitive
                    .getDisplayName(DefaultNameFormatter.getInstance())));
            helpAction.setHelpTopic(ht("/Action/CombineWay#ResolvingConflicts"));
            getRootPane().putClientProperty("help", ht("/Action/CombineWay#ResolvingConflicts"));
            pnlRelationMemberConflictResolver.initForWayCombining();
        } else if (targetPrimitive instanceof Node) {
            setTitle(tr("Conflicts when merging nodes - target node is ''{0}''", targetPrimitive
                    .getDisplayName(DefaultNameFormatter.getInstance())));
            helpAction.setHelpTopic(ht("/Action/MergeNodes#ResolvingConflicts"));
            getRootPane().putClientProperty("help", ht("/Action/MergeNodes#ResolvingConflicts"));
            pnlRelationMemberConflictResolver.initForNodeMerging();
        }
    }

    /**
     * Builds the components.
     */
    protected final void build() {
        getContentPane().setLayout(new BorderLayout());
        updateTitle();
        spTagConflictTypes = new AutoAdjustingSplitPane(JSplitPane.VERTICAL_SPLIT);
        spTagConflictTypes.setTopComponent(buildTagConflictResolverPanel());
        spTagConflictTypes.setBottomComponent(buildRelationMemberConflictResolverPanel());
        pnlButtons = buildButtonPanel();
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
        addWindowListener(new AdjustDividerLocationAction());
        HelpUtil.setHelpContext(getRootPane(), ht("/"));
        InputMapUtils.addEscapeAction(getRootPane(), new CancelAction());
    }

    /**
     * Builds the tag conflict resolver panel.
     * @return the tag conflict resolver panel
     */
    protected JPanel buildTagConflictResolverPanel() {
        pnlTagConflictResolver = new TagConflictResolver(modelTagConflictResolver);
        return pnlTagConflictResolver;
    }

    /**
     * Builds the relation member conflict resolver panel.
     * @return the relation member conflict resolver panel
     */
    protected JPanel buildRelationMemberConflictResolverPanel() {
        pnlRelationMemberConflictResolver = new RelationMemberConflictResolver(modelRelConflictResolver);
        return pnlRelationMemberConflictResolver;
    }

    /**
     * Builds the "Apply" action.
     * @return the "Apply" action
     */
    protected ApplyAction buildApplyAction() {
        return new ApplyAction();
    }

    /**
     * Builds the button panel.
     * @return the button panel
     */
    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // -- apply button
        ApplyAction applyAction = buildApplyAction();
        modelTagConflictResolver.addPropertyChangeListener(applyAction);
        modelRelConflictResolver.addPropertyChangeListener(applyAction);
        btnApply = new JButton(applyAction);
        btnApply.setFocusable(true);
        pnl.add(btnApply);

        // -- cancel button
        CancelAction cancelAction = new CancelAction();
        pnl.add(new JButton(cancelAction));

        // -- help button
        helpAction = new ContextSensitiveHelpAction();
        pnl.add(new JButton(helpAction));

        return pnl;
    }

    /**
     * Replies the tag conflict resolver model.
     * @return The tag conflict resolver model.
     */
    public TagConflictResolverModel getTagConflictResolverModel() {
        return modelTagConflictResolver;
    }

    /**
     * Replies the relation membership conflict resolver model.
     * @return The relation membership conflict resolver model.
     */
    public RelationMemberConflictResolverModel getRelationMemberConflictResolverModel() {
        return modelRelConflictResolver;
    }

    /**
     * Replies true if all tag and relation member conflicts have been decided.
     *
     * @return true if all tag and relation member conflicts have been decided; false otherwise
     */
    public boolean isResolvedCompletely() {
        return modelTagConflictResolver.isResolvedCompletely()
            && modelRelConflictResolver.isResolvedCompletely();
    }

    /**
     * Builds the list of tag change commands.
     * @param primitive target primitive
     * @param tc all resolutions
     * @return the list of tag change commands
     */
    protected List<Command> buildTagChangeCommand(OsmPrimitive primitive, TagCollection tc) {
        return primitiveResolver.buildTagChangeCommand(primitive, tc);
    }

    /**
     * Replies the list of {@link Command commands} needed to apply resolution choices.
     * @return The list of {@link Command commands} needed to apply resolution choices.
     */
    public List<Command> buildResolutionCommands() {
        List<Command> cmds = primitiveResolver.buildResolutionCommands(targetPrimitive);
        Command cmd = pnlRelationMemberConflictResolver.buildTagApplyCommands(modelRelConflictResolver
                .getModifiedRelations(targetPrimitive));
        if (cmd != null) {
            cmds.add(cmd);
        }
        return cmds;
    }

    /**
     * Prepares the default decisions for populated tag and relation membership conflicts.
     */
    public void prepareDefaultDecisions() {
        prepareDefaultDecisions(true);
    }

    /**
     * Prepares the default decisions for populated tag and relation membership conflicts.
     * @param fireEvent {@code true} to call {@code fireTableDataChanged} (can be a slow operation)
     * @since 11626
     */
    private void prepareDefaultDecisions(boolean fireEvent) {
        modelTagConflictResolver.prepareDefaultTagDecisions(fireEvent);
        modelRelConflictResolver.prepareDefaultRelationDecisions(fireEvent);
    }

    /**
     * Builds empty conflicts panel.
     * @return empty conflicts panel
     */
    protected JPanel buildEmptyConflictsPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(new JLabel(tr("No conflicts to resolve")));
        return pnl;
    }

    /**
     * Prepares GUI before conflict resolution starts.
     */
    protected void prepareGUIBeforeConflictResolutionStarts() {
        getContentPane().removeAll();

        if (modelRelConflictResolver.getNumDecisions() > 0 && modelTagConflictResolver.getNumDecisions() > 0) {
            // display both, the dialog for resolving relation conflicts and for resolving tag conflicts
            spTagConflictTypes.setTopComponent(pnlTagConflictResolver);
            spTagConflictTypes.setBottomComponent(pnlRelationMemberConflictResolver);
            getContentPane().add(spTagConflictTypes, BorderLayout.CENTER);
        } else if (modelRelConflictResolver.getNumDecisions() > 0) {
            // relation conflicts only
            getContentPane().add(pnlRelationMemberConflictResolver, BorderLayout.CENTER);
        } else if (modelTagConflictResolver.getNumDecisions() > 0) {
            // tag conflicts only
            getContentPane().add(pnlTagConflictResolver, BorderLayout.CENTER);
        } else {
            getContentPane().add(buildEmptyConflictsPanel(), BorderLayout.CENTER);
        }

        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
        validate();
        adjustDividerLocation();
        pnlRelationMemberConflictResolver.prepareForEditing();
    }

    /**
     * Sets whether this dialog has been closed with "Apply".
     * @param applied {@code true} if this dialog has been closed with "Apply"
     */
    protected void setApplied(boolean applied) {
        this.applied = applied;
    }

    /**
     * Determines if this dialog has been closed with "Apply".
     * @return true if this dialog has been closed with "Apply", false otherwise.
     */
    public boolean isApplied() {
        return applied;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            prepareGUIBeforeConflictResolutionStarts();
            setMinimumSize(new Dimension(400, 400));
            new WindowGeometry(getClass().getName() + ".geometry", WindowGeometry.centerInWindow(MainApplication.getMainFrame(),
                    new Dimension(800, 600))).applySafe(this);
            setApplied(false);
            btnApply.requestFocusInWindow();
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Cancel action.
     */
    protected class CancelAction extends AbstractAction {

        /**
         * Constructs a new {@code CancelAction}.
         */
        public CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution"));
            putValue(Action.NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }
    }

    /**
     * Apply action.
     */
    protected class ApplyAction extends AbstractAction implements PropertyChangeListener {

        /**
         * Constructs a new {@code ApplyAction}.
         */
        public ApplyAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts"));
            putValue(Action.NAME, tr("Apply"));
            new ImageProvider("ok").getResource().attachImageIcon(this);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setApplied(true);
            setVisible(false);
            pnlTagConflictResolver.rememberPreferences();
        }

        /**
         * Updates enabled state.
         */
        protected final void updateEnabledState() {
            setEnabled(modelTagConflictResolver.isResolvedCompletely()
                    && modelRelConflictResolver.isResolvedCompletely());
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(TagConflictResolverModel.NUM_CONFLICTS_PROP)) {
                updateEnabledState();
            }
            if (evt.getPropertyName().equals(RelationMemberConflictResolverModel.NUM_CONFLICTS_PROP)) {
                updateEnabledState();
            }
        }
    }

    private void adjustDividerLocation() {
        int numTagDecisions = modelTagConflictResolver.getNumDecisions();
        int numRelationDecisions = modelRelConflictResolver.getNumDecisions();
        if (numTagDecisions > 0 && numRelationDecisions > 0) {
            double nTop = 1.0 + numTagDecisions;
            double nBottom = 2.5 + numRelationDecisions;
            spTagConflictTypes.setDividerLocation(nTop/(nTop+nBottom));
        }
    }

    class AdjustDividerLocationAction extends WindowAdapter {
        @Override
        public void windowOpened(WindowEvent e) {
            adjustDividerLocation();
        }
    }

    /**
     * Replies the list of {@link Command commands} needed to resolve specified conflicts,
     * by displaying if necessary a {@link CombinePrimitiveResolverDialog} to the user.
     * This dialog will allow the user to choose conflict resolution actions.
     *
     * Non-expert users are informed first of the meaning of these operations, allowing them to cancel.
     *
     * @param tagsOfPrimitives The tag collection of the primitives to be combined.
     *                         Should generally be equal to {@code TagCollection.unionOfAllPrimitives(primitives)}
     * @param primitives The primitives to be combined
     * @param targetPrimitives The primitives the collection of primitives are merged or combined to.
     * @return The list of {@link Command commands} needed to apply resolution actions.
     * @throws UserCancelException If the user cancelled a dialog.
     */
    public static List<Command> launchIfNecessary(
            final TagCollection tagsOfPrimitives,
            final Collection<? extends OsmPrimitive> primitives,
            final Collection<? extends OsmPrimitive> targetPrimitives) throws UserCancelException {

        CheckParameterUtil.ensureParameterNotNull(tagsOfPrimitives, "tagsOfPrimitives");
        CheckParameterUtil.ensureParameterNotNull(primitives, "primitives");
        CheckParameterUtil.ensureParameterNotNull(targetPrimitives, "targetPrimitives");

        final TagCollection completeWayTags = new TagCollection(tagsOfPrimitives);
        TagConflictResolutionUtil.applyAutomaticTagConflictResolution(completeWayTags);
        TagConflictResolutionUtil.normalizeTagCollectionBeforeEditing(completeWayTags, primitives);
        final TagCollection tagsToEdit = new TagCollection(completeWayTags);
        TagConflictResolutionUtil.completeTagCollectionForEditing(tagsToEdit);

        final Set<Relation> parentRelations = OsmPrimitive.getParentRelations(primitives);

        // Show information dialogs about conflicts to non-experts
        if (!ExpertToggleAction.isExpert()) {
            // Tag conflicts
            if (!completeWayTags.isApplicableToPrimitive()) {
                informAboutTagConflicts(primitives, completeWayTags);
            }
            // Relation membership conflicts
            if (!parentRelations.isEmpty()) {
                informAboutRelationMembershipConflicts(primitives, parentRelations);
            }
        }

        final List<Command> cmds = new LinkedList<>();

        final TagConflictResolverModel tagModel = new TagConflictResolverModel();
        final RelationMemberConflictResolverModel relModel = new RelationMemberConflictResolverModel();

        tagModel.populate(tagsToEdit, completeWayTags.getKeysWithMultipleValues(), false);
        relModel.populate(parentRelations, primitives, false);
        tagModel.prepareDefaultTagDecisions(false);
        relModel.prepareDefaultRelationDecisions(false);

        if (tagModel.isResolvedCompletely() && relModel.isResolvedCompletely()) {
            // Build commands without need of dialog
            CombinePrimitiveResolver resolver = new CombinePrimitiveResolver(tagModel, relModel);
            for (OsmPrimitive i : targetPrimitives) {
                cmds.addAll(resolver.buildResolutionCommands(i));
            }
        } else if (!GraphicsEnvironment.isHeadless()) {
            UserCancelException canceled = GuiHelper.runInEDTAndWaitAndReturn(() -> {
                // Build conflict resolution dialog
                final CombinePrimitiveResolverDialog dialog = new CombinePrimitiveResolverDialog(
                        MainApplication.getMainFrame(), tagModel, relModel);

                // Ensure a proper title is displayed instead of a previous target (fix #7925)
                if (targetPrimitives.size() == 1) {
                    dialog.setTargetPrimitive(targetPrimitives.iterator().next(), false);
                } else {
                    dialog.setTargetPrimitive(null, false);
                }

                // Resolve tag conflicts
                GuiHelper.runInEDTAndWait(() -> {
                    tagModel.fireTableDataChanged();
                    relModel.fireTableDataChanged();
                    dialog.updateTitle();
                });
                dialog.setVisible(true);
                if (!dialog.isApplied()) {
                    return new UserCancelException();
                }

                // Build commands
                for (OsmPrimitive i : targetPrimitives) {
                    dialog.setTargetPrimitive(i, false);
                    cmds.addAll(dialog.buildResolutionCommands());
                }
                return null;
            });
            if (canceled != null) {
                throw canceled;
            }
        }
        return cmds;
    }

    /**
     * Inform a non-expert user about what relation membership conflict resolution means.
     * @param primitives The primitives to be combined
     * @param parentRelations The parent relations of the primitives
     * @throws UserCancelException If the user cancels the dialog.
     */
    protected static void informAboutRelationMembershipConflicts(
            final Collection<? extends OsmPrimitive> primitives,
            final Set<Relation> parentRelations) throws UserCancelException {
        /* I18n: object count < 2 is not possible */
        String msg = trn("You are about to combine {1} object, "
                + "which is part of {0} relation:<br/>{2}"
                + "Combining these objects may break this relation. If you are unsure, please cancel this operation.<br/>"
                + "If you want to continue, you are shown a dialog to decide how to adapt the relation.<br/><br/>"
                + "Do you want to continue?",
                "You are about to combine {1} objects, "
                + "which are part of {0} relations:<br/>{2}"
                + "Combining these objects may break these relations. If you are unsure, please cancel this operation.<br/>"
                + "If you want to continue, you are shown a dialog to decide how to adapt the relations.<br/><br/>"
                + "Do you want to continue?",
                parentRelations.size(), parentRelations.size(), primitives.size(),
                DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(parentRelations, 20));

        if (!ConditionalOptionPaneUtil.showConfirmationDialog(
                "combine_tags",
                MainApplication.getMainFrame(),
                "<html>" + msg + "</html>",
                tr("Combine confirmation"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_OPTION)) {
            throw new UserCancelException();
        }
    }

    /**
     * Inform a non-expert user about what tag conflict resolution means.
     * @param primitives The primitives to be combined
     * @param normalizedTags The normalized tag collection of the primitives to be combined
     * @throws UserCancelException If the user cancels the dialog.
     */
    protected static void informAboutTagConflicts(
            final Collection<? extends OsmPrimitive> primitives,
            final TagCollection normalizedTags) throws UserCancelException {
        String conflicts = normalizedTags.getKeysWithMultipleValues().stream().map(
                key -> getKeyDescription(key, normalizedTags)).collect(StreamUtils.toHtmlList());
        String msg = /* for correct i18n of plural forms - see #9110 */ trn("You are about to combine {0} objects, "
                + "but the following tags are used conflictingly:<br/>{1}"
                + "If these objects are combined, the resulting object may have unwanted tags.<br/>"
                + "If you want to continue, you are shown a dialog to fix the conflicting tags.<br/><br/>"
                + "Do you want to continue?", "You are about to combine {0} objects, "
                + "but the following tags are used conflictingly:<br/>{1}"
                + "If these objects are combined, the resulting object may have unwanted tags.<br/>"
                + "If you want to continue, you are shown a dialog to fix the conflicting tags.<br/><br/>"
                + "Do you want to continue?",
                primitives.size(), primitives.size(), conflicts);

        if (!ConditionalOptionPaneUtil.showConfirmationDialog(
                "combine_tags",
                MainApplication.getMainFrame(),
                "<html>" + msg + "</html>",
                tr("Combine confirmation"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_OPTION)) {
            throw new UserCancelException();
        }
    }

    private static String getKeyDescription(String key, TagCollection normalizedTags) {
        String values = normalizedTags.getValues(key)
                .stream()
                .map(x -> (x == null || x.isEmpty()) ? tr("<i>missing</i>") : x)
                .collect(Collectors.joining(tr(", ")));
        return tr("{0} ({1})", key, values);
    }
}
