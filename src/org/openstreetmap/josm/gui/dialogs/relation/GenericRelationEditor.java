// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.ScrollViewport;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AddSelectedAfterSelection;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AddSelectedAtEndAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AddSelectedAtStartAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AddSelectedBeforeSelection;
import org.openstreetmap.josm.gui.dialogs.relation.actions.ApplyAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.CancelAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.CopyMembersAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.DeleteCurrentRelationAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.DownloadIncompleteMembersAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.DuplicateRelationAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.EditAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionGroup;
import org.openstreetmap.josm.gui.dialogs.relation.actions.MoveDownAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.MoveUpAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.OKAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.PasteMembersAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.RefreshAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.RemoveAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.RemoveSelectedAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.ReverseAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.SelectAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.SelectPrimitivesForSelectedMembersAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.SelectedMembersForSelectionAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.SetRoleAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.SortAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.SortBelowAction;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * This dialog is for editing relations.
 * @since 343
 */
public class GenericRelationEditor extends RelationEditor {
    /** the tag table and its model */
    private final TagEditorPanel tagEditorPanel;
    private final ReferringRelationsBrowser referrerBrowser;
    private final ReferringRelationsBrowserModel referrerModel;

    /** the member table and its model */
    private final MemberTable memberTable;
    private final MemberTableModel memberTableModel;

    /** the selection table and its model */
    private final SelectionTable selectionTable;
    private final SelectionTableModel selectionTableModel;

    private final AutoCompletingTextField tfRole;

    /**
     * the menu item in the windows menu. Required to properly hide on dialog close.
     */
    private JMenuItem windowMenuItem;
    /**
     * The toolbar with the buttons on the left
     */
    private final LeftButtonToolbar leftButtonToolbar;
    /**
     * Action for performing the {@link RefreshAction}
     */
    private final RefreshAction refreshAction;
    /**
     * Action for performing the {@link ApplyAction}
     */
    private final ApplyAction applyAction;
    /**
     * Action for performing the {@link SelectAction}
     */
    private final SelectAction selectAction;
    /**
     * Action for performing the {@link DuplicateRelationAction}
     */
    private final DuplicateRelationAction duplicateAction;
    /**
     * Action for performing the {@link DeleteCurrentRelationAction}
     */
    private final DeleteCurrentRelationAction deleteAction;
    /**
     * Action for performing the {@link OKAction}
     */
    private final OKAction okAction;
    /**
     * Action for performing the {@link CancelAction}
     */
    private final CancelAction cancelAction;
    /**
     * A list of listeners that need to be notified on clipboard content changes.
     */
    private final ArrayList<FlavorListener> clipboardListeners = new ArrayList<>();

    /**
     * Creates a new relation editor for the given relation. The relation will be saved if the user
     * selects "ok" in the editor.
     *
     * If no relation is given, will create an editor for a new relation.
     *
     * @param layer the {@link OsmDataLayer} the new or edited relation belongs to
     * @param relation relation to edit, or null to create a new one.
     * @param selectedMembers a collection of members which shall be selected initially
     */
    public GenericRelationEditor(OsmDataLayer layer, Relation relation, Collection<RelationMember> selectedMembers) {
        super(layer, relation);

        setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(Main.parent, new Dimension(700, 650)));

        final TaggingPresetHandler presetHandler = new TaggingPresetHandler() {

            @Override
            public void updateTags(List<Tag> tags) {
                tagEditorPanel.getModel().updateTags(tags);
            }

            @Override
            public Collection<OsmPrimitive> getSelection() {
                Relation relation = new Relation();
                tagEditorPanel.getModel().applyToPrimitive(relation);
                return Collections.<OsmPrimitive>singletonList(relation);
            }
        };

        // init the various models
        //
        memberTableModel = new MemberTableModel(relation, getLayer(), presetHandler);
        memberTableModel.register();
        selectionTableModel = new SelectionTableModel(getLayer());
        selectionTableModel.register();
        referrerModel = new ReferringRelationsBrowserModel(relation);

        tagEditorPanel = new TagEditorPanel(relation, presetHandler);
        populateModels(relation);
        tagEditorPanel.getModel().ensureOneTag();

        // setting up the member table
        memberTable = new MemberTable(getLayer(), getRelation(), memberTableModel);
        memberTable.addMouseListener(new MemberTableDblClickAdapter());
        memberTableModel.addMemberModelListener(memberTable);

        MemberRoleCellEditor ce = (MemberRoleCellEditor) memberTable.getColumnModel().getColumn(0).getCellEditor();
        selectionTable = new SelectionTable(selectionTableModel, memberTableModel);
        selectionTable.setRowHeight(ce.getEditor().getPreferredSize().height);

        leftButtonToolbar = new LeftButtonToolbar(new RelationEditorActionAccess());
        tfRole = buildRoleTextField(this);

        JSplitPane pane = buildSplitPane(
                buildTagEditorPanel(tagEditorPanel),
                buildMemberEditorPanel(leftButtonToolbar, new RelationEditorActionAccess()),
                this);
        pane.setPreferredSize(new Dimension(100, 100));

        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(pane, BorderLayout.CENTER);
        pnl.setBorder(BorderFactory.createRaisedBevelBorder());

        getContentPane().setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add(tr("Tags and Members"), pnl);
        referrerBrowser = new ReferringRelationsBrowser(getLayer(), referrerModel);
        tabbedPane.add(tr("Parent Relations"), referrerBrowser);
        tabbedPane.add(tr("Child Relations"), new ChildRelationBrowser(getLayer(), relation));
        tabbedPane.addChangeListener(e -> {
            JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
            int index = sourceTabbedPane.getSelectedIndex();
            String title = sourceTabbedPane.getTitleAt(index);
            if (title.equals(tr("Parent Relations"))) {
                referrerBrowser.init();
            }
        });

        IRelationEditorActionAccess actionAccess = new RelationEditorActionAccess();

        refreshAction = new RefreshAction(actionAccess);
        applyAction = new ApplyAction(actionAccess);
        selectAction = new SelectAction(actionAccess);
        duplicateAction = new DuplicateRelationAction(actionAccess);
        deleteAction = new DeleteCurrentRelationAction(actionAccess);
        addPropertyChangeListener(deleteAction);

        okAction = new OKAction(actionAccess);
        cancelAction = new CancelAction(actionAccess);

        getContentPane().add(buildToolBar(refreshAction, applyAction, selectAction, duplicateAction, deleteAction), BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(buildOkCancelButtonPanel(okAction, cancelAction), BorderLayout.SOUTH);

        setSize(findMaxDialogSize());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                        cleanSelfReferences(memberTableModel, getRelation());
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        cancel();
                    }
                }
        );
        // CHECKSTYLE.OFF: LineLength
        registerCopyPasteAction(tagEditorPanel.getPasteAction(), "PASTE_TAGS",
                Shortcut.registerShortcut("system:pastestyle", tr("Edit: {0}", tr("Paste Tags")), KeyEvent.VK_V, Shortcut.CTRL_SHIFT).getKeyStroke(),
                getRootPane(), memberTable, selectionTable);
        // CHECKSTYLE.ON: LineLength

        KeyStroke key = Shortcut.getPasteKeyStroke();
        if (key != null) {
            // handle uncommon situation, that user has no keystroke assigned to paste
            registerCopyPasteAction(new PasteMembersAction(actionAccess) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    super.actionPerformed(e);
                    tfRole.requestFocusInWindow();
                }
            }, "PASTE_MEMBERS", key, getRootPane(), memberTable, selectionTable);
        }
        key = Shortcut.getCopyKeyStroke();
        if (key != null) {
            // handle uncommon situation, that user has no keystroke assigned to copy
            registerCopyPasteAction(new CopyMembersAction(actionAccess),
                    "COPY_MEMBERS", key, getRootPane(), memberTable, selectionTable);
        }
        tagEditorPanel.setNextFocusComponent(memberTable);
        selectionTable.setFocusable(false);
        memberTableModel.setSelectedMembers(selectedMembers);
        HelpUtil.setHelpContext(getRootPane(), ht("/Dialog/RelationEditor"));
    }

    @Override
    public void reloadDataFromRelation() {
        setRelation(getRelation());
        populateModels(getRelation());
        refreshAction.updateEnabledState();
    }

    private void populateModels(Relation relation) {
        if (relation != null) {
            tagEditorPanel.getModel().initFromPrimitive(relation);
            memberTableModel.populate(relation);
            if (!getLayer().data.getRelations().contains(relation)) {
                // treat it as a new relation if it doesn't exist in the data set yet.
                setRelation(null);
            }
        } else {
            tagEditorPanel.getModel().clear();
            memberTableModel.populate(null);
        }
    }

    /**
     * Apply changes.
     * @see ApplyAction
     */
    public void apply() {
        applyAction.actionPerformed(null);
    }

    /**
     * Select relation.
     * @see SelectAction
     * @since 12933
     */
    public void select() {
        selectAction.actionPerformed(null);
    }

    /**
     * Cancel changes.
     * @see CancelAction
     */
    public void cancel() {
        cancelAction.actionPerformed(null);
    }

    /**
     * Creates the toolbar
     * @param actions relation toolbar actions
     * @return the toolbar
     * @since 12933
     */
    protected static JToolBar buildToolBar(AbstractRelationEditorAction... actions) {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        for (AbstractRelationEditorAction action : actions) {
            tb.add(action);
        }
        return tb;
    }

    /**
     * builds the panel with the OK and the Cancel button
     * @param okAction OK action
     * @param cancelAction Cancel action
     *
     * @return the panel with the OK and the Cancel button
     */
    protected static JPanel buildOkCancelButtonPanel(OKAction okAction, CancelAction cancelAction) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnl.add(new JButton(okAction));
        pnl.add(new JButton(cancelAction));
        pnl.add(new JButton(new ContextSensitiveHelpAction(ht("/Dialog/RelationEditor"))));
        return pnl;
    }

    /**
     * builds the panel with the tag editor
     * @param tagEditorPanel tag editor panel
     *
     * @return the panel with the tag editor
     */
    protected static JPanel buildTagEditorPanel(TagEditorPanel tagEditorPanel) {
        JPanel pnl = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(new JLabel(tr("Tags")), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(tagEditorPanel, gc);
        return pnl;
    }

    /**
     * builds the role text field
     * @param re relation editor
     * @return the role text field
     */
    protected static AutoCompletingTextField buildRoleTextField(final IRelationEditor re) {
        final AutoCompletingTextField tfRole = new AutoCompletingTextField(10);
        tfRole.setToolTipText(tr("Enter a role and apply it to the selected relation members"));
        tfRole.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tfRole.selectAll();
            }
        });
        tfRole.setAutoCompletionList(new AutoCompletionList());
        tfRole.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        AutoCompletionList list = tfRole.getAutoCompletionList();
                        if (list != null) {
                            list.clear();
                            AutoCompletionManager.of(re.getLayer().data).populateWithMemberRoles(list, re.getRelation());
                        }
                    }
                }
        );
        tfRole.setText(Config.getPref().get("relation.editor.generic.lastrole", ""));
        return tfRole;
    }

    /**
     * builds the panel for the relation member editor
     * @param leftButtonToolbar left button toolbar
     * @param editorAccess The relation editor
     *
     * @return the panel for the relation member editor
     */
    protected static JPanel buildMemberEditorPanel(
            LeftButtonToolbar leftButtonToolbar, IRelationEditorActionAccess editorAccess) {
        final JPanel pnl = new JPanel(new GridBagLayout());
        final JScrollPane scrollPane = new JScrollPane(editorAccess.getMemberTable());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(new JLabel(tr("Members")), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridheight = 2;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.VERTICAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        pnl.add(new ScrollViewport(leftButtonToolbar, ScrollViewport.VERTICAL_DIRECTION), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.6;
        gc.weighty = 1.0;
        pnl.add(scrollPane, gc);

        // --- role editing
        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p3.add(new JLabel(tr("Apply Role:")));
        p3.add(editorAccess.getTextFieldRole());
        SetRoleAction setRoleAction = new SetRoleAction(editorAccess);
        editorAccess.getMemberTableModel().getSelectionModel().addListSelectionListener(setRoleAction);
        editorAccess.getTextFieldRole().getDocument().addDocumentListener(setRoleAction);
        editorAccess.getTextFieldRole().addActionListener(setRoleAction);
        editorAccess.getMemberTableModel().getSelectionModel().addListSelectionListener(
                e -> editorAccess.getTextFieldRole().setEnabled(editorAccess.getMemberTable().getSelectedRowCount() > 0)
        );
        editorAccess.getTextFieldRole().setEnabled(editorAccess.getMemberTable().getSelectedRowCount() > 0);
        JButton btnApply = new JButton(setRoleAction);
        btnApply.setPreferredSize(new Dimension(20, 20));
        btnApply.setText("");
        p3.add(btnApply);

        gc.gridx = 1;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.LAST_LINE_START;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(p3, gc);

        JPanel pnl2 = new JPanel(new GridBagLayout());

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 1;
        gc.gridwidth = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl2.add(new JLabel(tr("Selection")), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.VERTICAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        pnl2.add(new ScrollViewport(buildSelectionControlButtonToolbar(editorAccess),
                ScrollViewport.VERTICAL_DIRECTION), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        pnl2.add(buildSelectionTablePanel(editorAccess.getSelectionTable()), gc);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(pnl);
        splitPane.setRightComponent(pnl2);
        splitPane.setOneTouchExpandable(false);
        if (editorAccess.getEditor() instanceof Window) {
            ((Window) editorAccess.getEditor()).addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    // has to be called when the window is visible, otherwise no effect
                    splitPane.setDividerLocation(0.6);
                }
            });
        }

        JPanel pnl3 = new JPanel(new BorderLayout());
        pnl3.add(splitPane, BorderLayout.CENTER);

        return pnl3;
    }

    /**
     * builds the panel with the table displaying the currently selected primitives
     * @param selectionTable selection table
     *
     * @return panel with current selection
     */
    protected static JPanel buildSelectionTablePanel(SelectionTable selectionTable) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(new JScrollPane(selectionTable), BorderLayout.CENTER);
        return pnl;
    }

    /**
     * builds the {@link JSplitPane} which divides the editor in an upper and a lower half
     * @param top top panel
     * @param bottom bottom panel
     * @param re relation editor
     *
     * @return the split panel
     */
    protected static JSplitPane buildSplitPane(JPanel top, JPanel bottom, IRelationEditor re) {
        final JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pane.setTopComponent(top);
        pane.setBottomComponent(bottom);
        pane.setOneTouchExpandable(true);
        if (re instanceof Window) {
            ((Window) re).addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    // has to be called when the window is visible, otherwise no effect
                    pane.setDividerLocation(0.3);
                }
            });
        }
        return pane;
    }

    /**
     * The toolbar with the buttons on the left
     */
    static class LeftButtonToolbar extends JToolBar {
        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new {@code LeftButtonToolbar}.
         * @param editorAccess relation editor
         */
        LeftButtonToolbar(IRelationEditorActionAccess editorAccess) {
            setOrientation(JToolBar.VERTICAL);
            setFloatable(false);

            List<IRelationEditorActionGroup> groups = new ArrayList<>();
            // Move
            groups.add(buildNativeGroup(10,
                    new MoveUpAction(editorAccess, "moveUp"),
                    new MoveDownAction(editorAccess, "moveDown")
                    ));
            // Edit
            groups.add(buildNativeGroup(20,
                    new EditAction(editorAccess),
                    new RemoveAction(editorAccess, "removeSelected")
                    ));
            // Sort
            groups.add(buildNativeGroup(30,
                    new SortAction(editorAccess),
                    new SortBelowAction(editorAccess)
                    ));
            // Reverse
            groups.add(buildNativeGroup(40,
                    new ReverseAction(editorAccess)
                    ));
            // Download
            groups.add(buildNativeGroup(50,
                    new DownloadIncompleteMembersAction(editorAccess, "downloadIncomplete"),
                    new DownloadSelectedIncompleteMembersAction(editorAccess)
                    ));
            groups.addAll(RelationEditorHooks.getMemberActions());

            IRelationEditorActionGroup.fillToolbar(this, groups, editorAccess);


            InputMap inputMap = editorAccess.getMemberTable().getInputMap(MemberTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put((KeyStroke) new RemoveAction(editorAccess, "removeSelected")
                    .getValue(AbstractAction.ACCELERATOR_KEY), "removeSelected");
            inputMap.put((KeyStroke) new MoveUpAction(editorAccess, "moveUp")
                    .getValue(AbstractAction.ACCELERATOR_KEY), "moveUp");
            inputMap.put((KeyStroke) new MoveDownAction(editorAccess, "moveDown")
                    .getValue(AbstractAction.ACCELERATOR_KEY), "moveDown");
            inputMap.put((KeyStroke) new DownloadIncompleteMembersAction(
                    editorAccess, "downloadIncomplete").getValue(AbstractAction.ACCELERATOR_KEY), "downloadIncomplete");
        }
    }

    /**
     * build the toolbar with the buttons for adding or removing the current selection
     * @param editorAccess relation editor
     *
     * @return control buttons panel for selection/members
     */
    protected static JToolBar buildSelectionControlButtonToolbar(IRelationEditorActionAccess editorAccess) {
        JToolBar tb = new JToolBar(JToolBar.VERTICAL);
        tb.setFloatable(false);

        List<IRelationEditorActionGroup> groups = new ArrayList<>();
        groups.add(buildNativeGroup(10,
                new AddSelectedAtStartAction(editorAccess),
                new AddSelectedBeforeSelection(editorAccess),
                new AddSelectedAfterSelection(editorAccess),
                new AddSelectedAtEndAction(editorAccess)
                ));
        groups.add(buildNativeGroup(20,
                new SelectedMembersForSelectionAction(editorAccess),
                new SelectPrimitivesForSelectedMembersAction(editorAccess)
                ));
        groups.add(buildNativeGroup(30,
                new RemoveSelectedAction(editorAccess)
                ));
        groups.addAll(RelationEditorHooks.getSelectActions());

        IRelationEditorActionGroup.fillToolbar(tb, groups, editorAccess);
        return tb;
    }

    private static IRelationEditorActionGroup buildNativeGroup(int order, AbstractRelationEditorAction... actions) {
        return new IRelationEditorActionGroup() {
            @Override
            public int order() {
                return order;
            }

            @Override
            public List<AbstractRelationEditorAction> getActions(IRelationEditorActionAccess editorAccess) {
                return Arrays.asList(actions);
            }
        };
    }

    @Override
    protected Dimension findMaxDialogSize() {
        return new Dimension(700, 650);
    }

    @Override
    public void setVisible(boolean visible) {
        if (isVisible() == visible) {
            return;
        }
        if (visible) {
            tagEditorPanel.initAutoCompletion(getLayer());
        }
        super.setVisible(visible);
        Clipboard clipboard = ClipboardUtils.getClipboard();
        if (visible) {
            RelationDialogManager.getRelationDialogManager().positionOnScreen(this);
            if (windowMenuItem == null) {
                windowMenuItem = addToWindowMenu(this, getLayer().getName());
            }
            tagEditorPanel.requestFocusInWindow();
            for (FlavorListener listener : clipboardListeners) {
                clipboard.addFlavorListener(listener);
            }
        } else {
            // make sure all registered listeners are unregistered
            //
            memberTable.stopHighlighting();
            selectionTableModel.unregister();
            memberTableModel.unregister();
            memberTable.unregisterListeners();
            if (windowMenuItem != null) {
                MainApplication.getMenu().windowMenu.remove(windowMenuItem);
                windowMenuItem = null;
            }
            for (FlavorListener listener : clipboardListeners) {
                clipboard.removeFlavorListener(listener);
            }
            dispose();
        }
    }

    /**
     * Adds current relation editor to the windows menu (in the "volatile" group)
     * @param re relation editor
     * @param layerName layer name
     * @return created menu item
     */
    protected static JMenuItem addToWindowMenu(IRelationEditor re, String layerName) {
        Relation r = re.getRelation();
        String name = r == null ? tr("New relation") : r.getLocalName();
        JosmAction focusAction = new JosmAction(
                tr("Relation Editor: {0}", name == null && r != null ? r.getId() : name),
                "dialogs/relationlist",
                tr("Focus Relation Editor with relation ''{0}'' in layer ''{1}''", name, layerName),
                null, false, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                ((RelationEditor) getValue("relationEditor")).setVisible(true);
            }
        };
        focusAction.putValue("relationEditor", re);
        return MainMenu.add(MainApplication.getMenu().windowMenu, focusAction, MainMenu.WINDOW_MENU_GROUP.VOLATILE);
    }

    /**
     * checks whether the current relation has members referring to itself. If so,
     * warns the users and provides an option for removing these members.
     * @param memberTableModel member table model
     * @param relation relation
     */
    protected static void cleanSelfReferences(MemberTableModel memberTableModel, Relation relation) {
        List<OsmPrimitive> toCheck = new ArrayList<>();
        toCheck.add(relation);
        if (memberTableModel.hasMembersReferringTo(toCheck)) {
            int ret = ConditionalOptionPaneUtil.showOptionDialog(
                    "clean_relation_self_references",
                    Main.parent,
                    tr("<html>There is at least one member in this relation referring<br>"
                            + "to the relation itself.<br>"
                            + "This creates circular dependencies and is discouraged.<br>"
                            + "How do you want to proceed with circular dependencies?</html>"),
                            tr("Warning"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            new String[]{tr("Remove them, clean up relation"), tr("Ignore them, leave relation as is")},
                            tr("Remove them, clean up relation")
            );
            switch(ret) {
            case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.NO_OPTION:
                return;
            case JOptionPane.YES_OPTION:
                memberTableModel.removeMembersReferringTo(toCheck);
                break;
            default: // Do nothing
            }
        }
    }

    private void registerCopyPasteAction(AbstractAction action, Object actionName, KeyStroke shortcut,
            JRootPane rootPane, JTable... tables) {
        if (shortcut == null) {
            Logging.warn("No shortcut provided for the Paste action in Relation editor dialog");
        } else {
            int mods = shortcut.getModifiers();
            int code = shortcut.getKeyCode();
            if (code != KeyEvent.VK_INSERT && (mods == 0 || mods == InputEvent.SHIFT_DOWN_MASK)) {
                Logging.info(tr("Sorry, shortcut \"{0}\" can not be enabled in Relation editor dialog"), shortcut);
                return;
            }
        }
        rootPane.getActionMap().put(actionName, action);
        if (shortcut != null) {
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shortcut, actionName);
            // Assign also to JTables because they have their own Copy&Paste implementation
            // (which is disabled in this case but eats key shortcuts anyway)
            for (JTable table : tables) {
                table.getInputMap(JComponent.WHEN_FOCUSED).put(shortcut, actionName);
                table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(shortcut, actionName);
                table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shortcut, actionName);
            }
        }
        if (action instanceof FlavorListener) {
            clipboardListeners.add((FlavorListener) action);
        }
    }

    /**
     * Exception thrown when user aborts add operation.
     */
    public static class AddAbortException extends Exception {
    }

    /**
     * Asks confirmationbefore adding a primitive.
     * @param primitive primitive to add
     * @return {@code true} is user confirms the operation, {@code false} otherwise
     * @throws AddAbortException if user aborts operation
     */
    public static boolean confirmAddingPrimitive(OsmPrimitive primitive) throws AddAbortException {
        String msg = tr("<html>This relation already has one or more members referring to<br>"
                + "the object ''{0}''<br>"
                + "<br>"
                + "Do you really want to add another relation member?</html>",
                Utils.escapeReservedCharactersHTML(primitive.getDisplayName(DefaultNameFormatter.getInstance()))
            );
        int ret = ConditionalOptionPaneUtil.showOptionDialog(
                "add_primitive_to_relation",
                Main.parent,
                msg,
                tr("Multiple members referring to same object."),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                null
        );
        switch(ret) {
        case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
        case JOptionPane.YES_OPTION:
            return true;
        case JOptionPane.NO_OPTION:
        case JOptionPane.CLOSED_OPTION:
            return false;
        case JOptionPane.CANCEL_OPTION:
        default:
            throw new AddAbortException();
        }
    }

    /**
     * Warn about circular references.
     * @param primitive the concerned primitive
     */
    public static void warnOfCircularReferences(OsmPrimitive primitive) {
        String msg = tr("<html>You are trying to add a relation to itself.<br>"
                + "<br>"
                + "This creates circular references and is therefore discouraged.<br>"
                + "Skipping relation ''{0}''.</html>",
                Utils.escapeReservedCharactersHTML(primitive.getDisplayName(DefaultNameFormatter.getInstance())));
        JOptionPane.showMessageDialog(
                Main.parent,
                msg,
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Adds primitives to a given relation.
     * @param orig The relation to modify
     * @param primitivesToAdd The primitives to add as relation members
     * @return The resulting command
     * @throws IllegalArgumentException if orig is null
     */
    public static Command addPrimitivesToRelation(final Relation orig, Collection<? extends OsmPrimitive> primitivesToAdd) {
        CheckParameterUtil.ensureParameterNotNull(orig, "orig");
        try {
            final Collection<TaggingPreset> presets = TaggingPresets.getMatchingPresets(
                    EnumSet.of(TaggingPresetType.forPrimitive(orig)), orig.getKeys(), false);
            Relation relation = new Relation(orig);
            boolean modified = false;
            for (OsmPrimitive p : primitivesToAdd) {
                if (p instanceof Relation && orig.equals(p)) {
                    if (!GraphicsEnvironment.isHeadless()) {
                        warnOfCircularReferences(p);
                    }
                    continue;
                } else if (MemberTableModel.hasMembersReferringTo(relation.getMembers(), Collections.singleton(p))
                        && !confirmAddingPrimitive(p)) {
                    continue;
                }
                final Set<String> roles = findSuggestedRoles(presets, p);
                relation.addMember(new RelationMember(roles.size() == 1 ? roles.iterator().next() : "", p));
                modified = true;
            }
            return modified ? new ChangeCommand(orig, relation) : null;
        } catch (AddAbortException ign) {
            Logging.trace(ign);
            return null;
        }
    }

    protected static Set<String> findSuggestedRoles(final Collection<TaggingPreset> presets, OsmPrimitive p) {
        final Set<String> roles = new HashSet<>();
        for (TaggingPreset preset : presets) {
            String role = preset.suggestRoleForOsmPrimitive(p);
            if (role != null && !role.isEmpty()) {
                roles.add(role);
            }
        }
        return roles;
    }

    class MemberTableDblClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                new EditAction(new RelationEditorActionAccess()).actionPerformed(null);
            }
        }
    }

    private class RelationEditorActionAccess implements IRelationEditorActionAccess {

        @Override
        public MemberTable getMemberTable() {
            return memberTable;
        }

        @Override
        public MemberTableModel getMemberTableModel() {
            return memberTableModel;
        }

        @Override
        public SelectionTable getSelectionTable() {
            return selectionTable;
        }

        @Override
        public SelectionTableModel getSelectionTableModel() {
            return selectionTableModel;
        }

        @Override
        public IRelationEditor getEditor() {
            return GenericRelationEditor.this;
        }

        @Override
        public TagEditorModel getTagModel() {
            return tagEditorPanel.getModel();
        }

        @Override
        public AutoCompletingTextField getTextFieldRole() {
            return tfRole;
        }

    }
}
