package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ConflictAddCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionCache;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.io.OsmServerBackreferenceReader;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This dialog is for editing relations.
 *
 */
public class GenericRelationEditor extends RelationEditor  {

    static private final Logger logger = Logger.getLogger(GenericRelationEditor.class.getName());

    /** the tag table and its model */
    private TagEditorPanel tagEditorPanel;
    private ReferringRelationsBrowser referrerBrowser;
    private ReferringRelationsBrowserModel referrerModel;

    /** the member table */
    private MemberTable memberTable;
    private MemberTableModel memberTableModel;

    /** the model for the selection table */
    private SelectionTableModel selectionTableModel;

    private AutoCompletingTextField tfRole;

    /**
     * Creates a new relation editor for the given relation. The relation will be saved if the user
     * selects "ok" in the editor.
     *
     * If no relation is given, will create an editor for a new relation.
     *
     * @param layer the {@see OsmDataLayer} the new or edited relation belongs to
     * @param relation relation to edit, or null to create a new one.
     * @param selectedMembers a collection of members which shall be selected initially
     */
    public GenericRelationEditor(OsmDataLayer layer, Relation relation, Collection<RelationMember> selectedMembers) {
        super(layer, relation, selectedMembers);


        // initialize the autocompletion infrastructure
        //
        AutoCompletionCache.getCacheForLayer(getLayer()).initFromDataSet();

        // init the various models
        //
        memberTableModel = new MemberTableModel(getLayer());
        selectionTableModel = new SelectionTableModel(getLayer());
        referrerModel = new ReferringRelationsBrowserModel(relation);

        tagEditorPanel = new TagEditorPanel();
        // populate the models
        //
        if (relation != null) {
            tagEditorPanel.getModel().initFromPrimitive(relation);
            //this.tagEditorModel.initFromPrimitive(relation);
            this.memberTableModel.populate(relation);
            if (!getLayer().data.getRelations().contains(relation)) {
                // treat it as a new relation if it doesn't exist in the
                // data set yet.
                setRelation(null);
            }
        } else {
            tagEditorPanel.getModel().clear();
            this.memberTableModel.populate(null);
        }
        tagEditorPanel.getModel().ensureOneTag();

        JSplitPane pane = buildSplitPane();
        pane.setPreferredSize(new Dimension(100, 100));

        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(pane, BorderLayout.CENTER);
        pnl.setBorder(BorderFactory.createRaisedBevelBorder());

        getContentPane().setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add(tr("Tags and Members"), pnl);
        referrerBrowser = new ReferringRelationsBrowser(getLayer(), referrerModel, this);
        tabbedPane.add(tr("Parent Relations"), referrerBrowser);
        tabbedPane.add(tr("Child Relations"), new ChildRelationBrowser(getLayer(), relation));
        tabbedPane.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
                        int index = sourceTabbedPane.getSelectedIndex();
                        String title = sourceTabbedPane.getTitleAt(index);
                        if (title.equals(tr("Parent Relations"))) {
                            referrerBrowser.init();
                        }
                    }
                }
        );

        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(buildOkCancelButtonPanel(), BorderLayout.SOUTH);

        setSize(findMaxDialogSize());

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                        cleanSelfReferences();
                    }
                }
        );

        memberTableModel.setSelectedMembers(selectedMembers);
        HelpUtil.setHelpContext(getRootPane(),ht("/Dialog/RelationEditor"));
    }

    /**
     * builds the panel with the OK and the Cancel button
     *
     * @return the panel with the OK and the Cancel button
     */
    protected JPanel buildOkCancelButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        pnl.add(new SideButton(new OKAction()));
        pnl.add(new SideButton(new CancelAction()));
        pnl.add(new SideButton(new ContextSensitiveHelpAction(ht("/Dialog/RelationEditor"))));
        return pnl;
    }

    /**
     * builds the panel with the tag editor
     *
     * @return the panel with the tag editor
     */
    protected JPanel buildTagEditorPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());

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
     * builds the panel for the relation member editor
     *
     * @return the panel for the relation member editor
     */
    protected JPanel buildMemberEditorPanel() {
        final JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        // setting up the member table
        memberTable = new MemberTable(getLayer(),memberTableModel);
        memberTable.addMouseListener(new MemberTableDblClickAdapter());
        memberTableModel.addMemberModelListener(memberTable);

        final JScrollPane scrollPane = new JScrollPane(memberTable);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 1;
        gc.gridwidth = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(new JLabel(tr("Members")), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.VERTICAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        pnl.add(buildLeftButtonPanel(), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.6;
        gc.weighty = 1.0;
        pnl.add(scrollPane, gc);

        JPanel pnl2 = new JPanel();
        pnl2.setLayout(new GridBagLayout());

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
        pnl2.add(buildSelectionControlButtonPanel(), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        pnl2.add(buildSelectionTablePanel(), gc);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(pnl);
        splitPane.setRightComponent(pnl2);
        splitPane.setOneTouchExpandable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // has to be called when the window is visible, otherwise
                // no effect
                splitPane.setDividerLocation(0.6);
            }
        });


        JPanel pnl3 = new JPanel();
        pnl3.setLayout(new BorderLayout());
        pnl3.add(splitPane, BorderLayout.CENTER);
        pnl3.add(buildButtonPanel(), BorderLayout.SOUTH);
        return pnl3;
    }

    /**
     * builds the panel with the table displaying the currently selected primitives
     *
     * @return
     */
    protected JPanel buildSelectionTablePanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        JTable tbl = new JTable(selectionTableModel, new SelectionTableColumnModel(memberTableModel));
        tbl.setEnabled(false);
        JScrollPane pane = new JScrollPane(tbl);
        pnl.add(pane, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * builds the {@see JSplitPane} which divides the editor in an upper and a lower half
     *
     * @return the split panel
     */
    protected JSplitPane buildSplitPane() {
        final JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pane.setTopComponent(buildTagEditorPanel());
        pane.setBottomComponent(buildMemberEditorPanel());
        pane.setOneTouchExpandable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // has to be called when the window is visible, otherwise
                // no effect
                pane.setDividerLocation(0.3);
            }
        });
        return pane;
    }

    /**
     * build the panel with the buttons on the left
     *
     * @return
     */
    protected JPanel buildLeftButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        gc.insets = new Insets(0, 5, 0, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;

        // -----
        gc.gridy = 0;
        MoveUpAction moveUpAction = new MoveUpAction();
        memberTableModel.getSelectionModel().addListSelectionListener(moveUpAction);
        pnl.add(new JButton(moveUpAction), gc);

        // -----
        gc.gridy = 1;
        MoveDownAction moveDownAction = new MoveDownAction();
        memberTableModel.getSelectionModel().addListSelectionListener(moveDownAction);
        pnl.add(new JButton(moveDownAction), gc);

        // -- edit action
        gc.gridy = 2;
        EditAction editAction = new EditAction();
        memberTableModel.getSelectionModel().addListSelectionListener(editAction);
        pnl.add(new JButton(editAction),gc);

        // ------
        gc.gridy = 3;
        RemoveAction removeSelectedAction = new RemoveAction();
        memberTable.getSelectionModel().addListSelectionListener(removeSelectedAction);
        pnl.add(new JButton(removeSelectedAction), gc);


        // ------
        gc.gridy = 4;
        SelectPrimitivesForSelectedMembersAction selectAction = new SelectPrimitivesForSelectedMembersAction();
        memberTable.getSelectionModel().addListSelectionListener(selectAction);
        pnl.add(new JButton(selectAction), gc);

        // ------
        gc.gridy = 5;
        SortAction sortAction = new SortAction();
        pnl.add(new JButton(sortAction), gc);

        // ------
        // just grab the remaining space
        gc.gridy = 6;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        pnl.add(new JPanel(), gc);
        return pnl;
    }

    /**
     * build the panel with the buttons for adding or removing the current selection
     *
     * @return
     */
    protected JPanel buildSelectionControlButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        gc.insets = new Insets(0, 5, 0, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        AddSelectedAtEndAction addSelectedAtEndAction = new AddSelectedAtEndAction();
        selectionTableModel.addTableModelListener(addSelectedAtEndAction);
        pnl.add(new JButton(addSelectedAtEndAction), gc);

        // -----
        gc.gridy = 1;
        SelectedMembersForSelectionAction selectMembersForSelectionAction = new SelectedMembersForSelectionAction();
        selectionTableModel.addTableModelListener(selectMembersForSelectionAction);
        memberTableModel.addTableModelListener(selectMembersForSelectionAction);
        pnl.add(new JButton(selectMembersForSelectionAction), gc);

        // -----
        gc.gridy = 2;
        RemoveSelectedAction removeSelectedAction = new RemoveSelectedAction();
        selectionTableModel.addTableModelListener(removeSelectedAction);
        pnl.add(new JButton(removeSelectedAction), gc);

        // ------
        // just grab the remaining space
        gc.gridy = 3;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        pnl.add(new JPanel(), gc);

        // -----
        gc.gridy = 4;
        gc.weighty = 0.0;
        AddSelectedAtStartAction addSelectionAction = new AddSelectedAtStartAction();
        selectionTableModel.addTableModelListener(addSelectionAction);
        pnl.add(new JButton(addSelectionAction), gc);

        // -----
        gc.gridy = 5;
        AddSelectedBeforeSelection addSelectedBeforeSelectionAction = new AddSelectedBeforeSelection();
        selectionTableModel.addTableModelListener(addSelectedBeforeSelectionAction);
        memberTableModel.getSelectionModel().addListSelectionListener(addSelectedBeforeSelectionAction);
        pnl.add(new JButton(addSelectedBeforeSelectionAction), gc);

        // -----
        gc.gridy = 6;
        AddSelectedAfterSelection addSelectedAfterSelectionAction = new AddSelectedAfterSelection();
        selectionTableModel.addTableModelListener(addSelectedAfterSelectionAction);
        memberTableModel.getSelectionModel().addListSelectionListener(addSelectedAfterSelectionAction);
        pnl.add(new JButton(addSelectedAfterSelectionAction), gc);

        return pnl;
    }

    /**
     * Creates the buttons for the basic editing layout
     * @return {@see JPanel} with basic buttons
     */
    protected JPanel buildButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // --- download members
        buttonPanel.add(new SideButton(new DownlaodAction()));

        // --- role editing
        buttonPanel.add(new JLabel(tr("Role:")));
        tfRole = new AutoCompletingTextField(10);
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
                        AutoCompletionCache.getCacheForLayer(Main.main.getEditLayer()).populateWithMemberRoles(list);
                    }
                }
        );

        buttonPanel.add(tfRole);
        SetRoleAction setRoleAction = new SetRoleAction();
        memberTableModel.getSelectionModel().addListSelectionListener(setRoleAction);
        buttonPanel.add(new SideButton(setRoleAction));
        tfRole.getDocument().addDocumentListener(setRoleAction);
        tfRole.addActionListener(setRoleAction);

        // --- copy relation action
        buttonPanel.add(new SideButton(new DuplicateRelationAction()));

        // --- apply relation action
        buttonPanel.add(new SideButton(new ApplyAction()));

        // --- delete relation action
        buttonPanel.add(new SideButton(new DeleteCurrentRelationAction()));
        return buttonPanel;
    }

    @Override
    protected Dimension findMaxDialogSize() {
        // FIXME: Make it remember dialog size
        return new Dimension(700, 500);
    }

    @Override
    public void dispose() {
        selectionTableModel.unregister();
        DataSet.selListeners.remove(memberTableModel);
        super.dispose();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            tagEditorPanel.initAutoCompletion(Main.main.getEditLayer());
        }
        super.setVisible(visible);
        if (!visible) {
            dispose();
        }
    }

    /**
     * checks whether the current relation has members referring to itself. If so,
     * warns the users and provides an option for removing these members.
     *
     */
    protected void cleanSelfReferences() {
        ArrayList<OsmPrimitive> toCheck = new ArrayList<OsmPrimitive>();
        toCheck.add(getRelation());
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
            case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION: return;
            case JOptionPane.CLOSED_OPTION: return;
            case JOptionPane.NO_OPTION: return;
            case JOptionPane.YES_OPTION:
                memberTableModel.removeMembersReferringTo(toCheck);
                break;
            }
        }
    }

    class AddAbortException extends Exception  {
    }

    abstract class  AddFromSelectionAction extends AbstractAction {
        protected boolean isPotentialDuplicate(OsmPrimitive primitive) {
            return memberTableModel.hasMembersReferringTo(Collections.singleton(primitive));
        }

        protected boolean confirmAddingPrimtive(OsmPrimitive primitive)  throws AddAbortException {
            String msg = tr("<html>This relation already has one or more members referring to<br>"
                    + "the primitive ''{0}''<br>"
                    + "<br>"
                    + "Do you really want to add another relation member?</html>",
                    primitive.getDisplayName(DefaultNameFormatter.getInstance())
            );
            int ret = ConditionalOptionPaneUtil.showOptionDialog(
                    "add_primitive_to_relation",
                    Main.parent,
                    msg,
                    tr("Multiple members referring to same primitive"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    null
            );
            switch(ret) {
            case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION : return true;
            case JOptionPane.YES_OPTION: return true;
            case JOptionPane.NO_OPTION: return false;
            case JOptionPane.CLOSED_OPTION: return false;
            case JOptionPane.CANCEL_OPTION: throw new AddAbortException();
            }
            // should not happen
            return false;
        }

        protected void warnOfCircularReferences(OsmPrimitive primitive) {
            String msg = tr("<html>You are trying to add a relation to itself.<br>"
                    + "<br>"
                    + "This creates circular references and is therefore discouraged.<br>"
                    + "Skipping relation ''{0}''.</html>",
                    primitive.getDisplayName(DefaultNameFormatter.getInstance())
            );
            JOptionPane.showMessageDialog(
                    Main.parent,
                    msg,
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
        }

        protected List<OsmPrimitive> filterConfirmedPrimitives(List<OsmPrimitive> primitives) throws AddAbortException {
            if (primitives == null || primitives.isEmpty())
                return primitives;
            ArrayList<OsmPrimitive> ret = new ArrayList<OsmPrimitive>();
            Iterator<OsmPrimitive> it = primitives.iterator();
            while(it.hasNext()) {
                OsmPrimitive primitive = it.next();
                if (primitive instanceof Relation && getRelation() != null && getRelation().equals(primitive)) {
                    warnOfCircularReferences(primitive);
                    continue;
                }
                if (isPotentialDuplicate(primitive))  {
                    if (confirmAddingPrimtive(primitive)) {
                        ret.add(primitive);
                    }
                    continue;
                } else {
                    ret.add(primitive);
                }
            }
            return ret;
        }
    }

    class AddSelectedAtStartAction extends AddFromSelectionAction implements TableModelListener {
        public AddSelectedAtStartAction() {
            putValue(SHORT_DESCRIPTION,
                    tr("Add all primitives selected in the current dataset before the first member"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/conflict", "copystartright"));
            // putValue(NAME, tr("Add Selected"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(selectionTableModel.getRowCount() > 0 && memberTableModel.getRowCount() > 0);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                List<OsmPrimitive> toAdd = filterConfirmedPrimitives(selectionTableModel.getSelection());
                memberTableModel.addMembersAtBeginning(toAdd);
            } catch(AddAbortException ex) {
                // do nothing
            }
        }

        public void tableChanged(TableModelEvent e) {
            refreshEnabled();
        }
    }

    class AddSelectedAtEndAction extends AddFromSelectionAction implements TableModelListener {
        public AddSelectedAtEndAction() {
            putValue(SHORT_DESCRIPTION, tr("Add all primitives selected in the current dataset after the last member"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/conflict", "copyendright"));
            // putValue(NAME, tr("Add Selected"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(selectionTableModel.getRowCount() > 0);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                List<OsmPrimitive> toAdd = filterConfirmedPrimitives(selectionTableModel.getSelection());
                memberTableModel.addMembersAtEnd(toAdd);
            } catch(AddAbortException ex) {
                // do nothing
            }
        }

        public void tableChanged(TableModelEvent e) {
            refreshEnabled();
        }
    }

    class AddSelectedBeforeSelection extends AddFromSelectionAction implements TableModelListener, ListSelectionListener {
        public AddSelectedBeforeSelection() {
            putValue(SHORT_DESCRIPTION,
                    tr("Add all primitives selected in the current dataset before the first selected member"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/conflict", "copybeforecurrentright"));
            // putValue(NAME, tr("Add Selected"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(selectionTableModel.getRowCount() > 0
                    && memberTableModel.getSelectionModel().getMinSelectionIndex() >= 0);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                List<OsmPrimitive> toAdd = filterConfirmedPrimitives(selectionTableModel.getSelection());
                memberTableModel.addMembersBeforeIdx(toAdd, memberTableModel
                        .getSelectionModel().getMinSelectionIndex());
            } catch(AddAbortException ex) {
                // do nothing
            }


        }

        public void tableChanged(TableModelEvent e) {
            refreshEnabled();
        }

        public void valueChanged(ListSelectionEvent e) {
            refreshEnabled();
        }
    }

    class AddSelectedAfterSelection extends AddFromSelectionAction implements TableModelListener, ListSelectionListener {
        public AddSelectedAfterSelection() {
            putValue(SHORT_DESCRIPTION,
                    tr("Add all primitives selected in the current dataset after the last selected member"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/conflict", "copyaftercurrentright"));
            // putValue(NAME, tr("Add Selected"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(selectionTableModel.getRowCount() > 0
                    && memberTableModel.getSelectionModel().getMinSelectionIndex() >= 0);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                List<OsmPrimitive> toAdd = filterConfirmedPrimitives(selectionTableModel.getSelection());
                memberTableModel.addMembersAfterIdx(toAdd, memberTableModel
                        .getSelectionModel().getMaxSelectionIndex());
            } catch(AddAbortException ex) {
                // do nothing
            }
        }

        public void tableChanged(TableModelEvent e) {
            refreshEnabled();
        }

        public void valueChanged(ListSelectionEvent e) {
            refreshEnabled();
        }
    }

    class RemoveSelectedAction extends AbstractAction implements TableModelListener {
        public RemoveSelectedAction() {
            putValue(SHORT_DESCRIPTION, tr("Remove all members referring to one of the selected primitives"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "deletemembers"));
            // putValue(NAME, tr("Remove Selected"));
            Shortcut.registerShortcut("relationeditor:removeselected", tr("Relation Editor: Remove Selected"),
                    KeyEvent.VK_S, Shortcut.GROUP_MNEMONIC);

            updateEnabledState();
        }

        protected void updateEnabledState() {
            DataSet ds = getLayer().data;
            if (ds == null || ds.getSelected().isEmpty()) {
                setEnabled(false);
                return;
            }
            // only enable the action if we have members referring to the
            // selected primitives
            //
            setEnabled(memberTableModel.hasMembersReferringTo(ds.getSelected()));
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.removeMembersReferringTo(selectionTableModel.getSelection());
        }

        public void tableChanged(TableModelEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Selects  members in the relation editor which refer to primitives in the current
     * selection of the context layer.
     *
     */
    class SelectedMembersForSelectionAction extends AbstractAction implements TableModelListener {
        public SelectedMembersForSelectionAction() {
            putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to primitives in the current selection"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "selectmembers"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            boolean enabled = selectionTableModel.getRowCount() > 0
            &&  !memberTableModel.getChildPrimitives(getLayer().data.getSelected()).isEmpty();

            if (enabled) {
                putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to {0} primitives in the current selection",memberTableModel.getChildPrimitives(getLayer().data.getSelected()).size()));
            } else {
                putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to primitives in the current selection"));
            }
            setEnabled(enabled);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.selectMembersReferringTo(getLayer().data.getSelected());
        }

        public void tableChanged(TableModelEvent e) {
            updateEnabledState();

        }
    }

    /**
     * Selects primitives in the layer this editor belongs to. The selected primitives are
     * equal to the set of primitives the currently selected relation members refer to.
     *
     */
    class SelectPrimitivesForSelectedMembersAction extends AbstractAction implements ListSelectionListener {
        public SelectPrimitivesForSelectedMembersAction() {
            putValue(SHORT_DESCRIPTION, tr("Select primitives for selected relation members"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "selectprimitives"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(memberTable.getSelectedRowCount() > 0);
        }

        public void actionPerformed(ActionEvent e) {
            getLayer().data.setSelected(memberTableModel.getSelectedChildPrimitives());
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class SortAction extends AbstractAction {
        public SortAction() {
            putValue(SHORT_DESCRIPTION, tr("Sort the relation members"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "sort"));
            // putValue(NAME, tr("Sort"));
            Shortcut.registerShortcut("relationeditor:sort", tr("Relation Editor: Sort"), KeyEvent.VK_T,
                    Shortcut.GROUP_MNEMONIC);
            //setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.sort();
        }
    }

    class MoveUpAction extends AbstractAction implements ListSelectionListener {
        public MoveUpAction() {
            putValue(SHORT_DESCRIPTION, tr("Move the currently selected members up"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "moveup"));
            // putValue(NAME, tr("Move Up"));
            Shortcut.registerShortcut("relationeditor:moveup", tr("Relation Editor: Move Up"), KeyEvent.VK_N,
                    Shortcut.GROUP_MNEMONIC);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.moveUp(memberTable.getSelectedRows());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(memberTableModel.canMoveUp(memberTable.getSelectedRows()));
        }
    }

    class MoveDownAction extends AbstractAction implements ListSelectionListener {
        public MoveDownAction() {
            putValue(SHORT_DESCRIPTION, tr("Move the currently selected members down"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "movedown"));
            // putValue(NAME, tr("Move Down"));
            Shortcut.registerShortcut("relationeditor:moveup", tr("Relation Editor: Move Down"), KeyEvent.VK_J,
                    Shortcut.GROUP_MNEMONIC);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.moveDown(memberTable.getSelectedRows());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(memberTableModel.canMoveDown(memberTable.getSelectedRows()));
        }
    }

    class RemoveAction extends AbstractAction implements ListSelectionListener {
        public RemoveAction() {
            putValue(SHORT_DESCRIPTION, tr("Remove the currently selected members from this relation"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "remove"));
            // putValue(NAME, tr("Remove"));
            Shortcut.registerShortcut("relationeditor:remove", tr("Relation Editor: Remove"), KeyEvent.VK_J,
                    Shortcut.GROUP_MNEMONIC);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            memberTableModel.remove(memberTable.getSelectedRows());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(memberTableModel.canRemove(memberTable.getSelectedRows()));
        }
    }

    class DeleteCurrentRelationAction extends AbstractAction {
        public DeleteCurrentRelationAction() {
            putValue(SHORT_DESCRIPTION, tr("Delete the currently edited relation"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            putValue(NAME, tr("Delete"));
            updateEnabledState();
        }

        public void run() {
            Relation toDelete = getRelation();
            if (toDelete == null)
                return;
            org.openstreetmap.josm.actions.mapmode.DeleteAction.deleteRelation(
                    getLayer(),
                    toDelete
            );
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }

        protected void updateEnabledState() {
            setEnabled(getRelation() != null);
        }
    }

    abstract class SavingAction extends AbstractAction {
        /**
         * apply updates to a new relation
         */
        protected void applyNewRelation() {
            // If the user wanted to create a new relation, but hasn't added any members or
            // tags, don't add an empty relation
            if (memberTableModel.getRowCount() == 0 && tagEditorPanel.getModel().getKeys().isEmpty())
                return;
            Relation newRelation = new Relation();
            tagEditorPanel.getModel().applyToPrimitive(newRelation);
            memberTableModel.applyToRelation(newRelation);
            Main.main.undoRedo.add(new AddCommand(getLayer(),newRelation));

            // make sure everybody is notified about the changes
            //
            getLayer().data.fireSelectionChanged();
            getLayer().fireDataChange();
            GenericRelationEditor.this.setRelation(newRelation);
            RelationDialogManager.getRelationDialogManager().updateContext(
                    getLayer(),
                    getRelation(),
                    GenericRelationEditor.this
            );
        }

        /**
         * Apply the updates for an existing relation which has not been changed
         * outside of the relation editor.
         *
         */
        protected void applyExistingConflictingRelation() {
            Relation editedRelation = new Relation(getRelation());
            tagEditorPanel.getModel().applyToPrimitive(editedRelation);
            memberTableModel.applyToRelation(editedRelation);
            Conflict<Relation> conflict = new Conflict<Relation>(getRelation(), editedRelation);
            Main.main.undoRedo.add(new ConflictAddCommand(getLayer(),conflict));
        }

        /**
         * Apply the updates for an existing relation which has been changed
         * outside of the relation editor.
         *
         */
        protected void applyExistingNonConflictingRelation() {
            Relation editedRelation = new Relation(getRelation());
            tagEditorPanel.getModel().applyToPrimitive(editedRelation);
            memberTableModel.applyToRelation(editedRelation);
            Main.main.undoRedo.add(new ChangeCommand(getRelation(), editedRelation));
            getLayer().data.fireSelectionChanged();
            getLayer().fireDataChange();
            // this will refresh the snapshot and update the dialog title
            //
            setRelation(getRelation());
        }

        protected boolean confirmClosingBecauseOfDirtyState() {
            ButtonSpec [] options = new ButtonSpec[] {
                    new ButtonSpec(
                            tr("Yes, create a conflict and close"),
                            ImageProvider.get("ok"),
                            tr("Click to create a conflict and close this relation editor") ,
                            null /* no specific help topic */
                    ),
                    new ButtonSpec(
                            tr("No, continue editing"),
                            ImageProvider.get("cancel"),
                            tr("Click to to return to the relation editor and to resume relation editing") ,
                            null /* no specific help topic */
                    )
            };

            int ret = HelpAwareOptionPane.showOptionDialog(
                    Main.parent,
                    tr("<html>This relation has been changed outside of the editor.<br>"
                            + "You can't apply your changes and continue editing.<br>"
                            + "<br>"
                            + "Do you want to create a conflict and close the editor?</html>"),
                            tr("Conflict in data"),
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            options,
                            options[0], // OK is default
                            "/Dialog/RelationEditor#RelationChangedOutsideOfEditor"
            );
            return ret == 0;
        }

        protected void warnDoubleConflict() {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Layer ''{0}'' already has a conflict for primitive<br>"
                            + "''{1}''.<br>"
                            + "Please resolve this conflict first, then try again.</html>",
                            getLayer().getName(),
                            getRelation().getDisplayName(DefaultNameFormatter.getInstance())
                    ),
                    tr("Double conflict"),
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    class ApplyAction extends SavingAction {
        public ApplyAction() {
            putValue(SHORT_DESCRIPTION, tr("Apply the current updates"));
            putValue(SMALL_ICON, ImageProvider.get("save"));
            putValue(NAME, tr("Apply"));
            setEnabled(true);
        }

        public void run() {
            if (getRelation() == null) {
                applyNewRelation();
            } else if (!memberTableModel.hasSameMembersAs(getRelationSnapshot())
                    || tagEditorPanel.getModel().isDirty()) {
                if (isDirtyRelation()) {
                    if (confirmClosingBecauseOfDirtyState()) {
                        if (getLayer().getConflicts().hasConflictForMy(getRelation())) {
                            warnDoubleConflict();
                            return;
                        }
                        applyExistingConflictingRelation();
                        setVisible(false);
                    }
                } else {
                    applyExistingNonConflictingRelation();
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }
    }

    class OKAction extends SavingAction {
        public OKAction() {
            putValue(SHORT_DESCRIPTION, tr("Apply the updates and close the dialog"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(NAME, tr("OK"));
            setEnabled(true);
        }

        public void run() {
            if (getRelation() == null) {
                applyNewRelation();
            } else if (!memberTableModel.hasSameMembersAs(getRelationSnapshot())
                    || tagEditorPanel.getModel().isDirty()) {
                if (isDirtyRelation()) {
                    if (confirmClosingBecauseOfDirtyState()) {
                        if (getLayer().getConflicts().hasConflictForMy(getRelation())) {
                            warnDoubleConflict();
                            return;
                        }
                        applyExistingConflictingRelation();
                    } else
                        return;
                } else {
                    applyExistingNonConflictingRelation();
                }
            }
            setVisible(false);
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(SHORT_DESCRIPTION, tr("Cancel the updates and close the dialog"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(NAME, tr("Cancel"));

            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
            getRootPane().getActionMap().put("ESCAPE", this);
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }

    class AddTagAction extends AbstractAction {
        public AddTagAction() {
            putValue(SHORT_DESCRIPTION, tr("Add an empty tag"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
            // putValue(NAME, tr("Cancel"));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) {
            tagEditorPanel.getModel().appendNewTag();
        }
    }

    class DownlaodAction extends AbstractAction {
        public DownlaodAction() {
            putValue(SHORT_DESCRIPTION, tr("Download all incomplete ways and nodes in relation"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "downloadincomplete"));
            putValue(NAME, tr("Download Members"));
            Shortcut.registerShortcut("relationeditor:downloadincomplete", tr("Relation Editor: Download Members"),
                    KeyEvent.VK_K, Shortcut.GROUP_MNEMONIC);
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            Main.worker.submit(new DownloadTask(
                    Collections.singletonList(getRelation()),
                    getLayer(),
                    memberTableModel,
                    GenericRelationEditor.this)
            );
        }

        protected void updateEnabledState() {
            setEnabled(getRelation() != null && !getRelation().isNew());
        }
    }

    class SetRoleAction extends AbstractAction implements ListSelectionListener, DocumentListener {
        public SetRoleAction() {
            putValue(SHORT_DESCRIPTION, tr("Sets a role for the selected members"));
            // FIXME: find better icon
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(NAME, tr("Apply Role"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(memberTable.getSelectedRowCount() > 0);
        }

        protected boolean isEmptyRole() {
            return tfRole.getText() == null || tfRole.getText().trim().equals("");
        }

        protected boolean confirmSettingEmptyRole(int onNumMembers) {
            String message = tr("<html>You are setting an empty role on {0} primitives.<br>"
                    + "This is equal to deleting the roles of these primitives.<br>"
                    + "Do you really want to apply the new role?</html>", onNumMembers);
            String [] options = new String[] {
                    tr("Yes, apply it"),
                    tr("No, don't apply")
            };
            int ret = ConditionalOptionPaneUtil.showOptionDialog(
                    "relation_editor.confirm_applying_empty_role",
                    Main.parent,
                    message,
                    tr("Confirm empty role"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    options,
                    options[0]
            );
            switch(ret) {
            case JOptionPane.YES_OPTION: return true;
            case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION: return true;
            default:
                return false;
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (isEmptyRole()) {
                if (! confirmSettingEmptyRole(memberTable.getSelectedRowCount()))
                    return;
            }
            memberTableModel.updateRole(memberTable.getSelectedRows(), tfRole.getText());
        }

        public void valueChanged(ListSelectionEvent e) {
            refreshEnabled();
        }

        public void changedUpdate(DocumentEvent e) {
            refreshEnabled();
        }

        public void insertUpdate(DocumentEvent e) {
            refreshEnabled();
        }

        public void removeUpdate(DocumentEvent e) {
            refreshEnabled();
        }
    }

    /**
     * Creates a new relation with a copy of the current editor state
     *
     */
    class DuplicateRelationAction extends AbstractAction {
        public DuplicateRelationAction() {
            putValue(SHORT_DESCRIPTION, tr("Create a copy of this relation and open it in another editor window"));
            // FIXME provide an icon
            putValue(SMALL_ICON, ImageProvider.get("duplicate"));
            putValue(NAME, tr("Duplicate"));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) {
            Relation copy = new Relation();
            tagEditorPanel.getModel().applyToPrimitive(copy);
            memberTableModel.applyToRelation(copy);
            RelationEditor editor = RelationEditor.getEditor(getLayer(), copy, memberTableModel.getSelectedMembers());
            editor.setVisible(true);
        }
    }

    /**
     * Action for editing the currently selected relation
     *
     *
     */
    class EditAction extends AbstractAction implements ListSelectionListener {
        public EditAction() {
            putValue(SHORT_DESCRIPTION, tr("Edit the relation the currently selected relation member refers to"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            //putValue(NAME, tr("Edit"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            setEnabled(memberTable.getSelectedRowCount() == 1
                    && memberTableModel.isEditableRelation(memberTable.getSelectedRow()));
        }

        protected Collection<RelationMember> getMembersForCurrentSelection(Relation r) {
            Collection<RelationMember> members = new HashSet<RelationMember>();
            Collection<OsmPrimitive> selection = getLayer().data.getSelected();
            for (RelationMember member: r.getMembers()) {
                if (selection.contains(member.getMember())) {
                    members.add(member);
                }
            }
            return members;
        }

        public void run() {
            int idx = memberTable.getSelectedRow();
            if (idx < 0)
                return;
            OsmPrimitive primitive = memberTableModel.getReferredPrimitive(idx);
            if (!(primitive instanceof Relation))
                return;
            Relation r = (Relation) primitive;
            if (r.incomplete)
                return;

            RelationEditor editor = RelationEditor.getEditor(getLayer(), r, getMembersForCurrentSelection(r));
            editor.setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            run();
        }

        public void valueChanged(ListSelectionEvent e) {
            refreshEnabled();
        }
    }

    class MemberTableDblClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                new EditAction().run();
            }
        }
    }

    /**
     * The asynchronous task for downloading relation members.
     *
     */
    public static class DownloadTask extends PleaseWaitRunnable {
        private boolean cancelled;
        private Exception lastException;
        private List<Relation> relations;
        private OsmDataLayer curLayer;
        private MemberTableModel memberTableModel;
        private OsmServerObjectReader objectReader;
        private OsmServerBackreferenceReader parentReader;

        public DownloadTask(List<Relation> relations, OsmDataLayer curLayer, MemberTableModel memberTableModel, Dialog parent) {
            super(tr("Download relation members"), new PleaseWaitProgressMonitor(parent), false /* don't ignore exception */);
            this.relations = relations;
            this.curLayer = curLayer;
            this.memberTableModel = memberTableModel;
        }

        public DownloadTask(List<Relation> relations, OsmDataLayer curLayer, MemberTableModel memberTableModel) {
            super(tr("Download relation members"), new PleaseWaitProgressMonitor(), false /* don't ignore exception */);
            this.relations = relations;
            this.curLayer = curLayer;
            this.memberTableModel = memberTableModel;
        }

        @Override
        protected void cancel() {
            cancelled = true;
            synchronized(this) {
                if (objectReader != null) {
                    objectReader.cancel();
                } else if (parentReader != null) {
                    parentReader.cancel();
                }
            }
        }

        @Override
        protected void finish() {
            Main.map.repaint();
            if (cancelled)
                return;
            if (memberTableModel != null) {
                memberTableModel.fireTableDataChanged();
            }
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                for (Relation relation : relations) {
                    // download the relation
                    //
                    progressMonitor.indeterminateSubTask(tr("Downloading relation ''{0}''", relation.getDisplayName(DefaultNameFormatter.getInstance())));
                    synchronized(this) {
                        if (cancelled) return;
                        objectReader = new OsmServerObjectReader(relation.getId(), OsmPrimitiveType.RELATION, true /* full download */);
                    }
                    final DataSet dataSet = objectReader.parseOsm(progressMonitor
                            .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    if (dataSet == null)
                        return;
                    synchronized (this) {
                        if (cancelled) return;
                        objectReader = null;
                    }

                    // download referring objects of the downloaded member objects
                    //
                    // asked for in #3999, but uncommented for the time being. Could be used
                    // later, perhaps if user explicity requests so (for instance by checking
                    // a checkbox)
                    //                    for (OsmPrimitive p: relation.getMemberPrimitives()) {
                    //                        synchronized(this) {
                    //                            if (cancelled) return;
                    //                            parentReader = new OsmServerBackreferenceReader(p);
                    //                        }
                    //                        DataSet parents = parentReader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
                    //                        synchronized(this) {
                    //                            if (cancelled) return;
                    //                            parentReader = null;
                    //                        }
                    //                        DataSetMerger merger = new DataSetMerger(dataSet, parents);
                    //                        merger.merge();
                    //                    }
                    //                    if (cancelled) return;

                    // has to run on the EDT because mergeFrom may trigger events
                    // which update the UI
                    //
                    SwingUtilities.invokeAndWait(
                            new Runnable() {
                                public void run() {
                                    curLayer.mergeFrom(dataSet);
                                    curLayer.fireDataChange();
                                    curLayer.onPostDownloadFromServer();
                                }
                            }
                    );
                }
            } catch (Exception e) {
                if (cancelled) {
                    System.out.println(tr("Warning: ignoring exception because task is cancelled. Exception: {0}", e
                            .toString()));
                    return;
                }
                lastException = e;
            }
        }
    }
}
