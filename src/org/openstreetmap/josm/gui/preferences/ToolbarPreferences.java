// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.MenuElement;
import javax.swing.TransferHandler;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.actions.ActionParameter;
import org.openstreetmap.josm.actions.AdaptableAction;
import org.openstreetmap.josm.actions.AddImageryLayerAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.ParameterizedAction;
import org.openstreetmap.josm.actions.ParameterizedActionDecorator;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Toolbar preferences.
 * @since 172
 */
public class ToolbarPreferences implements PreferenceSettingFactory {

    private static final String EMPTY_TOOLBAR_MARKER = "<!-empty-!>";

    /**
     * The prefix for imagery toolbar entries.
     * @since 11657
     */
    public static final String IMAGERY_PREFIX = "imagery_";

    /**
     * Action definition.
     */
    public static class ActionDefinition {
        private final Action action;
        private String name = "";
        private String icon = "";
        private ImageIcon ico;
        private final Map<String, Object> parameters = new ConcurrentHashMap<>();

        /**
         * Constructs a new {@code ActionDefinition}.
         * @param action action
         */
        public ActionDefinition(Action action) {
            this.action = action;
        }

        /**
         * Returns action parameters.
         * @return action parameters
         */
        public Map<String, Object> getParameters() {
            return parameters;
        }

        /**
         * Returns {@link ParameterizedActionDecorator}, if applicable.
         * @return {@link ParameterizedActionDecorator}, if applicable
         */
        public Action getParametrizedAction() {
            if (getAction() instanceof ParameterizedAction)
                return new ParameterizedActionDecorator((ParameterizedAction) getAction(), parameters);
            else
                return getAction();
        }

        /**
         * Returns action.
         * @return action
         */
        public Action getAction() {
            return action;
        }

        /**
         * Returns action name.
         * @return action name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns action display name.
         * @return action display name
         */
        public String getDisplayName() {
            return name.isEmpty() ? (String) action.getValue(Action.NAME) : name;
        }

        /**
         * Returns display tooltip.
         * @return display tooltip
         */
        public String getDisplayTooltip() {
            if (!name.isEmpty())
                return name;

            Object tt = action.getValue(TaggingPreset.OPTIONAL_TOOLTIP_TEXT);
            if (tt != null)
                return (String) tt;

            return (String) action.getValue(Action.SHORT_DESCRIPTION);
        }

        /**
         * Returns display icon.
         * @return display icon
         */
        public Icon getDisplayIcon() {
            if (ico != null)
                return ico;
            return (Icon) Optional.ofNullable(action.getValue(Action.LARGE_ICON_KEY)).orElseGet(() -> action.getValue(Action.SMALL_ICON));
        }

        /**
         * Sets action name.
         * @param name action name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns icon name.
         * @return icon name
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Sets icon name.
         * @param icon icon name
         */
        public void setIcon(String icon) {
            this.icon = icon;
            ico = ImageProvider.getIfAvailable("", icon);
        }

        /**
         * Determines if this a separator.
         * @return {@code true} if this a separator
         */
        public boolean isSeparator() {
            return action == null;
        }

        /**
         * Returns a new separator.
         * @return new separator
         */
        public static ActionDefinition getSeparator() {
            return new ActionDefinition(null);
        }

        /**
         * Determines if this action has parameters.
         * @return {@code true} if this action has parameters
         */
        public boolean hasParameters() {
            if (!(getAction() instanceof ParameterizedAction)) return false;
            for (Object o: parameters.values()) {
                if (o != null) return true;
            }
            return false;
        }
    }

    public static class ActionParser {
        private final Map<String, Action> actions;
        private final StringBuilder result = new StringBuilder();
        private int index;
        private char[] s;

        /**
         * Constructs a new {@code ActionParser}.
         * @param actions actions map - can be null
         */
        public ActionParser(Map<String, Action> actions) {
            this.actions = actions;
        }

        private String readTillChar(char ch1, char ch2) {
            result.setLength(0);
            while (index < s.length && s[index] != ch1 && s[index] != ch2) {
                if (s[index] == '\\') {
                    index++;
                    if (index >= s.length) {
                        break;
                    }
                }
                result.append(s[index]);
                index++;
            }
            return result.toString();
        }

        private void skip(char ch) {
            if (index < s.length && s[index] == ch) {
                index++;
            }
        }

        /**
         * Loads the action definition from its toolbar name.
         * @param actionName action toolbar name
         * @return action definition or null
         */
        public ActionDefinition loadAction(String actionName) {
            index = 0;
            this.s = actionName.toCharArray();

            String name = readTillChar('(', '{');
            Action action = actions.get(name);

            if (action == null && name.startsWith(IMAGERY_PREFIX)) {
                String imageryName = name.substring(IMAGERY_PREFIX.length());
                for (ImageryInfo i : ImageryLayerInfo.instance.getDefaultLayers()) {
                    if (imageryName.equalsIgnoreCase(i.getName())) {
                        action = new AddImageryLayerAction(i);
                        break;
                    }
                }
            }

            if (action == null)
                return null;

            ActionDefinition result = new ActionDefinition(action);

            if (action instanceof ParameterizedAction) {
                skip('(');

                ParameterizedAction parametrizedAction = (ParameterizedAction) action;
                Map<String, ActionParameter<?>> actionParams = new ConcurrentHashMap<>();
                for (ActionParameter<?> param: parametrizedAction.getActionParameters()) {
                    actionParams.put(param.getName(), param);
                }

                while (index < s.length && s[index] != ')') {
                    String paramName = readTillChar('=', '=');
                    skip('=');
                    String paramValue = readTillChar(',', ')');
                    if (!paramName.isEmpty() && !paramValue.isEmpty()) {
                        ActionParameter<?> actionParam = actionParams.get(paramName);
                        if (actionParam != null) {
                            result.getParameters().put(paramName, actionParam.readFromString(paramValue));
                        }
                    }
                    skip(',');
                }
                skip(')');
            }
            if (action instanceof AdaptableAction) {
                skip('{');

                while (index < s.length && s[index] != '}') {
                    String paramName = readTillChar('=', '=');
                    skip('=');
                    String paramValue = readTillChar(',', '}');
                    if ("icon".equals(paramName) && !paramValue.isEmpty()) {
                        result.setIcon(paramValue);
                    } else if ("name".equals(paramName) && !paramValue.isEmpty()) {
                        result.setName(paramValue);
                    }
                    skip(',');
                }
                skip('}');
            }

            return result;
        }

        private void escape(String s) {
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '\\' || ch == '(' || ch == '{' || ch == ',' || ch == ')' || ch == '}' || ch == '=') {
                    result.append('\\');
                    result.append(ch);
                } else {
                    result.append(ch);
                }
            }
        }

        @SuppressWarnings("unchecked")
        public String saveAction(ActionDefinition action) {
            result.setLength(0);

            String val = (String) action.getAction().getValue("toolbar");
            if (val == null)
                return null;
            escape(val);
            if (action.getAction() instanceof ParameterizedAction) {
                result.append('(');
                List<ActionParameter<?>> params = ((ParameterizedAction) action.getAction()).getActionParameters();
                for (int i = 0; i < params.size(); i++) {
                    ActionParameter<Object> param = (ActionParameter<Object>) params.get(i);
                    escape(param.getName());
                    result.append('=');
                    Object value = action.getParameters().get(param.getName());
                    if (value != null) {
                        escape(param.writeToString(value));
                    }
                    if (i < params.size() - 1) {
                        result.append(',');
                    } else {
                        result.append(')');
                    }
                }
            }
            if (action.getAction() instanceof AdaptableAction) {
                boolean first = true;
                String tmp = action.getName();
                if (!tmp.isEmpty()) {
                    result.append(first ? "{" : ",");
                    result.append("name=");
                    escape(tmp);
                    first = false;
                }
                tmp = action.getIcon();
                if (!tmp.isEmpty()) {
                    result.append(first ? "{" : ",");
                    result.append("icon=");
                    escape(tmp);
                    first = false;
                }
                if (!first) {
                    result.append('}');
            }
            }

            return result.toString();
        }
    }

    private static class ActionParametersTableModel extends AbstractTableModel {

        private transient ActionDefinition currentAction = ActionDefinition.getSeparator();

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            int adaptable = (currentAction.getAction() instanceof AdaptableAction) ? 2 : 0;
            if (currentAction.isSeparator() || !(currentAction.getAction() instanceof ParameterizedAction))
                return adaptable;
            ParameterizedAction pa = (ParameterizedAction) currentAction.getAction();
            return pa.getActionParameters().size() + adaptable;
        }

        @SuppressWarnings("unchecked")
        private ActionParameter<Object> getParam(int index) {
            ParameterizedAction pa = (ParameterizedAction) currentAction.getAction();
            return (ActionParameter<Object>) pa.getActionParameters().get(index);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (currentAction.getAction() instanceof AdaptableAction) {
                if (rowIndex < 2) {
                    switch (columnIndex) {
                    case 0:
                        return rowIndex == 0 ? tr("Tooltip") : tr("Icon");
                    case 1:
                        return rowIndex == 0 ? currentAction.getName() : currentAction.getIcon();
                    default:
                        return null;
                    }
                } else {
                    rowIndex -= 2;
                }
            }
            ActionParameter<Object> param = getParam(rowIndex);
            switch (columnIndex) {
            case 0:
                return param.getName();
            case 1:
                return param.writeToString(currentAction.getParameters().get(param.getName()));
            default:
                return null;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String val = (String) aValue;
            int paramIndex = rowIndex;

            if (currentAction.getAction() instanceof AdaptableAction) {
                if (rowIndex == 0) {
                     currentAction.setName(val);
                     return;
                } else if (rowIndex == 1) {
                     currentAction.setIcon(val);
                     return;
                } else {
                    paramIndex -= 2;
                }
            }
            ActionParameter<Object> param = getParam(paramIndex);

            if (param != null && !val.isEmpty()) {
                currentAction.getParameters().put(param.getName(), param.readFromString((String) aValue));
            }
        }

        public void setCurrentAction(ActionDefinition currentAction) {
            this.currentAction = currentAction;
            fireTableDataChanged();
        }
    }

    private class ToolbarPopupMenu extends JPopupMenu {
        private transient ActionDefinition act;

        private void setActionAndAdapt(ActionDefinition action) {
            this.act = action;
            doNotHide.setSelected(Config.getPref().getBoolean("toolbar.always-visible", true));
            remove.setVisible(act != null);
            shortcutEdit.setVisible(act != null);
        }

        private final JMenuItem remove = new JMenuItem(new AbstractAction(tr("Remove from toolbar")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> t = new LinkedList<>(getToolString());
                ActionParser parser = new ActionParser(null);
                // get text definition of current action
                String res = parser.saveAction(act);
                // remove the button from toolbar preferences
                t.remove(res);
                Config.getPref().putList("toolbar", t);
                MainApplication.getToolbar().refreshToolbarControl();
            }
        });

        private final JMenuItem configure = new JMenuItem(new AbstractAction(tr("Configure toolbar")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PreferenceDialog p = new PreferenceDialog(MainApplication.getMainFrame());
                p.selectPreferencesTabByName("toolbar");
                p.setVisible(true);
            }
        });

        private final JMenuItem shortcutEdit = new JMenuItem(new AbstractAction(tr("Edit shortcut")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PreferenceDialog p = new PreferenceDialog(MainApplication.getMainFrame());
                p.getTabbedPane().getShortcutPreference().setDefaultFilter(act.getDisplayName());
                p.selectPreferencesTabByName("shortcuts");
                p.setVisible(true);
                // refresh toolbar to try using changed shortcuts without restart
                MainApplication.getToolbar().refreshToolbarControl();
            }
        });

        private final JCheckBoxMenuItem doNotHide = new JCheckBoxMenuItem(new AbstractAction(tr("Do not hide toolbar and menu")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
                Config.getPref().putBoolean("toolbar.always-visible", sel);
                Config.getPref().putBoolean("menu.always-visible", sel);
            }
        });

        {
            addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    setActionAndAdapt(buttonActions.get(
                            ((JPopupMenu) e.getSource()).getInvoker()
                    ));
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    // Do nothing
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    // Do nothing
                }
            });
            add(remove);
            add(configure);
            add(shortcutEdit);
            add(doNotHide);
        }
    }

    private final ToolbarPopupMenu popupMenu = new ToolbarPopupMenu();

    /**
     * Key: Registered name (property "toolbar" of action).
     * Value: The action to execute.
     */
    private final Map<String, Action> actions = new ConcurrentHashMap<>();
    private final Map<String, Action> regactions = new ConcurrentHashMap<>();

    private final DefaultMutableTreeNode rootActionsNode = new DefaultMutableTreeNode(tr("Actions"));

    public final JToolBar control = new JToolBar();
    private final Map<Object, ActionDefinition> buttonActions = new ConcurrentHashMap<>(30);

    @Override
    public PreferenceSetting createPreferenceSetting() {
        return new Settings(rootActionsNode);
    }

    /**
     * Toolbar preferences settings.
     */
    public class Settings extends DefaultTabPreferenceSetting {

        private final class SelectedListTransferHandler extends TransferHandler {
            @Override
            @SuppressWarnings("unchecked")
            protected Transferable createTransferable(JComponent c) {
                List<ActionDefinition> actions = new ArrayList<>();
                for (ActionDefinition o: ((JList<ActionDefinition>) c).getSelectedValuesList()) {
                    actions.add(o);
                }
                return new ActionTransferable(actions);
            }

            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            @Override
            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                for (DataFlavor f : transferFlavors) {
                    if (ACTION_FLAVOR.equals(f))
                        return true;
                }
                return false;
            }

            @Override
            public void exportAsDrag(JComponent comp, InputEvent e, int action) {
                super.exportAsDrag(comp, e, action);
                movingComponent = "list";
            }

            @Override
            public boolean importData(JComponent comp, Transferable t) {
                try {
                    int dropIndex = selectedList.locationToIndex(selectedList.getMousePosition(true));
                    @SuppressWarnings("unchecked")
                    List<ActionDefinition> draggedData = (List<ActionDefinition>) t.getTransferData(ACTION_FLAVOR);

                    Object leadItem = dropIndex >= 0 ? selected.elementAt(dropIndex) : null;
                    int dataLength = draggedData.size();

                    if (leadItem != null) {
                        for (Object o: draggedData) {
                            if (leadItem.equals(o))
                                return false;
                        }
                    }

                    int dragLeadIndex = -1;
                    boolean localDrop = "list".equals(movingComponent);

                    if (localDrop) {
                        dragLeadIndex = selected.indexOf(draggedData.get(0));
                        for (Object o: draggedData) {
                            selected.removeElement(o);
                        }
                    }
                    int[] indices = new int[dataLength];

                    if (localDrop) {
                        int adjustedLeadIndex = selected.indexOf(leadItem);
                        int insertionAdjustment = dragLeadIndex <= adjustedLeadIndex ? 1 : 0;
                        for (int i = 0; i < dataLength; i++) {
                            selected.insertElementAt(draggedData.get(i), adjustedLeadIndex + insertionAdjustment + i);
                            indices[i] = adjustedLeadIndex + insertionAdjustment + i;
                        }
                    } else {
                        for (int i = 0; i < dataLength; i++) {
                            selected.add(dropIndex, draggedData.get(i));
                            indices[i] = dropIndex + i;
                        }
                    }
                    selectedList.clearSelection();
                    selectedList.setSelectedIndices(indices);
                    movingComponent = "";
                    return true;
                } catch (IOException | UnsupportedFlavorException e) {
                    Logging.error(e);
                }
                return false;
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action) {
                if ("list".equals(movingComponent)) {
                    try {
                        List<?> draggedData = (List<?>) data.getTransferData(ACTION_FLAVOR);
                        boolean localDrop = selected.contains(draggedData.get(0));
                        if (localDrop) {
                            int[] indices = selectedList.getSelectedIndices();
                            Arrays.sort(indices);
                            for (int i = indices.length - 1; i >= 0; i--) {
                                selected.remove(indices[i]);
                            }
                        }
                    } catch (IOException | UnsupportedFlavorException e) {
                        Logging.error(e);
                    }
                    movingComponent = "";
                }
            }
        }

        private final class Move implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ("<".equals(e.getActionCommand()) && actionsTree.getSelectionCount() > 0) {

                    int leadItem = selected.getSize();
                    if (selectedList.getSelectedIndex() != -1) {
                        int[] indices = selectedList.getSelectedIndices();
                        leadItem = indices[indices.length - 1];
                    }
                    for (TreePath selectedAction : actionsTree.getSelectionPaths()) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedAction.getLastPathComponent();
                        if (node.getUserObject() == null) {
                            selected.add(leadItem++, ActionDefinition.getSeparator());
                        } else if (node.getUserObject() instanceof Action) {
                            selected.add(leadItem++, new ActionDefinition((Action) node.getUserObject()));
                        }
                    }
                } else if (">".equals(e.getActionCommand()) && selectedList.getSelectedIndex() != -1) {
                    while (selectedList.getSelectedIndex() != -1) {
                        selected.remove(selectedList.getSelectedIndex());
                    }
                } else if ("up".equals(e.getActionCommand())) {
                    int i = selectedList.getSelectedIndex();
                    ActionDefinition o = selected.get(i);
                    if (i != 0) {
                        selected.remove(i);
                        selected.add(i-1, o);
                        selectedList.setSelectedIndex(i-1);
                    }
                } else if ("down".equals(e.getActionCommand())) {
                    int i = selectedList.getSelectedIndex();
                    ActionDefinition o = selected.get(i);
                    if (i != selected.size()-1) {
                        selected.remove(i);
                        selected.add(i+1, o);
                        selectedList.setSelectedIndex(i+1);
                    }
                }
            }
        }

        private class ActionTransferable implements Transferable {

            private final DataFlavor[] flavors = new DataFlavor[] {ACTION_FLAVOR};

            private final List<ActionDefinition> actions;

            ActionTransferable(List<ActionDefinition> actions) {
                this.actions = actions;
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                return actions;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return flavors;
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavors[0] == flavor;
            }
        }

        private final Move moveAction = new Move();

        private final DefaultListModel<ActionDefinition> selected = new DefaultListModel<>();
        private final JList<ActionDefinition> selectedList = new JList<>(selected);

        private final DefaultTreeModel actionsTreeModel;
        private final JTree actionsTree;

        private final ActionParametersTableModel actionParametersModel = new ActionParametersTableModel();
        private final JTable actionParametersTable = new JTable(actionParametersModel);
        private JPanel actionParametersPanel;

        private final JButton upButton = createButton("up");
        private final JButton downButton = createButton("down");
        private final JButton removeButton = createButton(">");
        private final JButton addButton = createButton("<");

        private String movingComponent;

        /**
         * Constructs a new {@code Settings}.
         * @param rootActionsNode root actions node
         */
        public Settings(DefaultMutableTreeNode rootActionsNode) {
            super(/* ICON(preferences/) */ "toolbar", tr("Toolbar customization"), tr("Customize the elements on the toolbar."));
            actionsTreeModel = new DefaultTreeModel(rootActionsNode);
            actionsTree = new JTree(actionsTreeModel);
        }

        private JButton createButton(String name) {
            JButton b = new JButton();
            if ("up".equals(name)) {
                b.setIcon(ImageProvider.get("dialogs", "up", ImageSizes.SMALLICON));
            } else if ("down".equals(name)) {
                b.setIcon(ImageProvider.get("dialogs", "down", ImageSizes.SMALLICON));
            } else {
                b.setText(name);
            }
            b.addActionListener(moveAction);
            b.setActionCommand(name);
            return b;
        }

        private void updateEnabledState() {
            int index = selectedList.getSelectedIndex();
            upButton.setEnabled(index > 0);
            downButton.setEnabled(index != -1 && index < selectedList.getModel().getSize() - 1);
            removeButton.setEnabled(index != -1);
            addButton.setEnabled(actionsTree.getSelectionCount() > 0);
        }

        @Override
        public void addGui(PreferenceTabbedPane gui) {
            actionsTree.setCellRenderer(new DefaultTreeCellRenderer() {
                @Override
                public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                        boolean leaf, int row, boolean hasFocus) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    JLabel comp = (JLabel) super.getTreeCellRendererComponent(
                            tree, value, sel, expanded, leaf, row, hasFocus);
                    if (node.getUserObject() == null) {
                        comp.setText(tr("Separator"));
                        comp.setIcon(ImageProvider.get("preferences/separator"));
                    } else if (node.getUserObject() instanceof Action) {
                        Action action = (Action) node.getUserObject();
                        comp.setText((String) action.getValue(Action.NAME));
                        comp.setIcon((Icon) action.getValue(Action.SMALL_ICON));
                    }
                    return comp;
                }
            });

            ListCellRenderer<ActionDefinition> renderer = new ListCellRenderer<ActionDefinition>() {
                private final DefaultListCellRenderer def = new DefaultListCellRenderer();
                @Override
                public Component getListCellRendererComponent(JList<? extends ActionDefinition> list,
                        ActionDefinition action, int index, boolean isSelected, boolean cellHasFocus) {
                    String s;
                    Icon i;
                    if (!action.isSeparator()) {
                        s = action.getDisplayName();
                        i = action.getDisplayIcon();
                    } else {
                        i = ImageProvider.get("preferences/separator");
                        s = tr("Separator");
                    }
                    JLabel l = (JLabel) def.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
                    l.setIcon(i);
                    return l;
                }
            };
            selectedList.setCellRenderer(renderer);
            selectedList.addListSelectionListener(e -> {
                boolean sel = selectedList.getSelectedIndex() != -1;
                if (sel) {
                    actionsTree.clearSelection();
                    ActionDefinition action = selected.get(selectedList.getSelectedIndex());
                    actionParametersModel.setCurrentAction(action);
                    actionParametersPanel.setVisible(actionParametersModel.getRowCount() > 0);
                }
                updateEnabledState();
            });

            if (!GraphicsEnvironment.isHeadless()) {
                selectedList.setDragEnabled(true);
            }
            selectedList.setTransferHandler(new SelectedListTransferHandler());

            actionsTree.setTransferHandler(new TransferHandler() {
                private static final long serialVersionUID = 1L;

                @Override
                public int getSourceActions(JComponent c) {
                    return TransferHandler.MOVE;
                }

                @Override
                protected Transferable createTransferable(JComponent c) {
                    TreePath[] paths = actionsTree.getSelectionPaths();
                    List<ActionDefinition> dragActions = new ArrayList<>();
                    for (TreePath path : paths) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object obj = node.getUserObject();
                        if (obj == null) {
                            dragActions.add(ActionDefinition.getSeparator());
                        } else if (obj instanceof Action) {
                            dragActions.add(new ActionDefinition((Action) obj));
                        }
                    }
                    return new ActionTransferable(dragActions);
                }
            });
            if (!GraphicsEnvironment.isHeadless()) {
                actionsTree.setDragEnabled(true);
            }
            actionsTree.getSelectionModel().addTreeSelectionListener(e -> updateEnabledState());

            final JPanel left = new JPanel(new GridBagLayout());
            left.add(new JLabel(tr("Toolbar")), GBC.eol());
            left.add(new JScrollPane(selectedList), GBC.std().fill(GBC.BOTH));

            final JPanel right = new JPanel(new GridBagLayout());
            right.add(new JLabel(tr("Available")), GBC.eol());
            right.add(new JScrollPane(actionsTree), GBC.eol().fill(GBC.BOTH));

            final JPanel buttons = new JPanel(new GridLayout(6, 1));
            buttons.add(upButton);
            buttons.add(addButton);
            buttons.add(removeButton);
            buttons.add(downButton);
            updateEnabledState();

            final JPanel p = new JPanel();
            p.setLayout(new LayoutManager() {
                @Override
                public void addLayoutComponent(String name, Component comp) {
                    // Do nothing
                }

                @Override
                public void removeLayoutComponent(Component comp) {
                    // Do nothing
                }

                @Override
                public Dimension minimumLayoutSize(Container parent) {
                    Dimension l = left.getMinimumSize();
                    Dimension r = right.getMinimumSize();
                    Dimension b = buttons.getMinimumSize();
                    return new Dimension(l.width+b.width+10+r.width, l.height+b.height+10+r.height);
                }

                @Override
                public Dimension preferredLayoutSize(Container parent) {
                    Dimension l = new Dimension(200, 200);
                    Dimension r = new Dimension(200, 200);
                    return new Dimension(l.width+r.width+10+buttons.getPreferredSize().width, Math.max(l.height, r.height));
                }

                @Override
                public void layoutContainer(Container parent) {
                    Dimension d = p.getSize();
                    Dimension b = buttons.getPreferredSize();
                    int width = (d.width-10-b.width)/2;
                    left.setBounds(new Rectangle(0, 0, width, d.height));
                    right.setBounds(new Rectangle(width+10+b.width, 0, width, d.height));
                    buttons.setBounds(new Rectangle(width+5, d.height/2-b.height/2, b.width, b.height));
                }
            });
            p.add(left);
            p.add(buttons);
            p.add(right);

            actionParametersPanel = new JPanel(new GridBagLayout());
            actionParametersPanel.add(new JLabel(tr("Action parameters")), GBC.eol().insets(0, 10, 0, 20));
            actionParametersTable.getColumnModel().getColumn(0).setHeaderValue(tr("Parameter name"));
            actionParametersTable.getColumnModel().getColumn(1).setHeaderValue(tr("Parameter value"));
            actionParametersPanel.add(actionParametersTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
            actionParametersPanel.add(actionParametersTable, GBC.eol().fill(GBC.BOTH).insets(0, 0, 0, 10));
            actionParametersPanel.setVisible(false);

            JPanel panel = gui.createPreferenceTab(this);
            panel.add(p, GBC.eol().fill(GBC.BOTH));
            panel.add(actionParametersPanel, GBC.eol().fill(GBC.HORIZONTAL));
            selected.removeAllElements();
            for (ActionDefinition actionDefinition: getDefinedActions()) {
                selected.addElement(actionDefinition);
            }
            actionsTreeModel.reload();
        }

        @Override
        public boolean ok() {
            List<String> t = new LinkedList<>();
            ActionParser parser = new ActionParser(null);
            for (int i = 0; i < selected.size(); ++i) {
                ActionDefinition action = selected.get(i);
                if (action.isSeparator()) {
                    t.add("|");
                } else {
                    String res = parser.saveAction(action);
                    if (res != null) {
                        t.add(res);
                }
            }
            }
            if (t.isEmpty()) {
                t = Collections.singletonList(EMPTY_TOOLBAR_MARKER);
            }
            Config.getPref().putList("toolbar", t);
            MainApplication.getToolbar().refreshToolbarControl();
            return false;
        }

        @Override
        public String getHelpContext() {
            return HelpUtil.ht("/Preferences/Toolbar");
        }
    }

    /**
     * Constructs a new {@code ToolbarPreferences}.
     */
    public ToolbarPreferences() {
        GuiHelper.runInEDTAndWait(() -> {
            control.setFloatable(false);
            control.setComponentPopupMenu(popupMenu);
        });
        Config.getPref().addPreferenceChangeListener(e -> {
            if ("toolbar.visible".equals(e.getKey())) {
                refreshToolbarControl();
            }
        });
    }

    private void loadAction(DefaultMutableTreeNode node, MenuElement menu) {
        Object userObject = null;
        MenuElement menuElement = menu;
        if (menu.getSubElements().length > 0 &&
                menu.getSubElements()[0] instanceof JPopupMenu) {
            menuElement = menu.getSubElements()[0];
        }
        for (MenuElement item : menuElement.getSubElements()) {
            if (item instanceof JMenuItem) {
                JMenuItem menuItem = (JMenuItem) item;
                if (menuItem.getAction() != null) {
                    Action action = menuItem.getAction();
                    userObject = action;
                    Object tb = action.getValue("toolbar");
                    if (tb == null) {
                        Logging.info(tr("Toolbar action without name: {0}",
                        action.getClass().getName()));
                        continue;
                    } else if (!(tb instanceof String)) {
                        if (!(tb instanceof Boolean) || (Boolean) tb) {
                            Logging.info(tr("Strange toolbar value: {0}",
                            action.getClass().getName()));
                        }
                        continue;
                    } else {
                        String toolbar = (String) tb;
                        Action r = actions.get(toolbar);
                        if (r != null && r != action && !toolbar.startsWith(IMAGERY_PREFIX)) {
                            Logging.info(tr("Toolbar action {0} overwritten: {1} gets {2}",
                            toolbar, r.getClass().getName(), action.getClass().getName()));
                        }
                        actions.put(toolbar, action);
                    }
                } else {
                    userObject = menuItem.getText();
                }
            }
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(userObject);
            node.add(newNode);
            loadAction(newNode, item);
        }
    }

    private void loadActions() {
        rootActionsNode.removeAllChildren();
        loadAction(rootActionsNode, MainApplication.getMenu());
        for (Map.Entry<String, Action> a : regactions.entrySet()) {
            if (actions.get(a.getKey()) == null) {
                rootActionsNode.add(new DefaultMutableTreeNode(a.getValue()));
            }
        }
        rootActionsNode.add(new DefaultMutableTreeNode(null));
    }

    private static final String[] deftoolbar = {"open", "save", "download", "upload", "|",
    "undo", "redo", "|", "dialogs/search", "preference", "|", "splitway", "combineway",
    "wayflip", "|", "imagery-offset", "|", "tagginggroup_Highways/Streets",
    "tagginggroup_Highways/Ways", "tagginggroup_Highways/Waypoints",
    "tagginggroup_Highways/Barriers", "|", "tagginggroup_Transport/Car",
    "tagginggroup_Transport/Public Transport", "|", "tagginggroup_Facilities/Tourism",
    "tagginggroup_Facilities/Food+Drinks", "|", "tagginggroup_Man Made/Historic Places", "|",
    "tagginggroup_Man Made/Man Made"};

    public static Collection<String> getToolString() {

        Collection<String> toolStr = Config.getPref().getList("toolbar", Arrays.asList(deftoolbar));
        if (toolStr == null || toolStr.isEmpty()) {
            toolStr = Arrays.asList(deftoolbar);
        }
        return toolStr;
    }

    private Collection<ActionDefinition> getDefinedActions() {
        loadActions();

        Map<String, Action> allActions = new ConcurrentHashMap<>(regactions);
        allActions.putAll(actions);
        ActionParser actionParser = new ActionParser(allActions);

        Collection<ActionDefinition> result = new ArrayList<>();

        for (String s : getToolString()) {
            if ("|".equals(s)) {
                result.add(ActionDefinition.getSeparator());
            } else {
                ActionDefinition a = actionParser.loadAction(s);
                if (a != null) {
                    result.add(a);
                } else {
                    Logging.info("Could not load tool definition "+s);
                }
            }
        }

        return result;
    }

    /**
     * Registers an action to the toolbar preferences.
     * @param action Action to register
     * @return The parameter (for better chaining)
     */
    public Action register(Action action) {
        String toolbar = (String) action.getValue("toolbar");
        if (toolbar == null) {
            Logging.info(tr("Registered toolbar action without name: {0}",
                action.getClass().getName()));
        } else {
            Action r = regactions.get(toolbar);
            if (r != null) {
                Logging.info(tr("Registered toolbar action {0} overwritten: {1} gets {2}",
                    toolbar, r.getClass().getName(), action.getClass().getName()));
            }
        }
        if (toolbar != null) {
            regactions.put(toolbar, action);
        }
        return action;
    }

    /**
     * Unregisters an action from the toolbar preferences.
     * @param action Action to unregister
     * @return The removed action, or null
     * @since 11654
     */
    public Action unregister(Action action) {
        Object toolbar = action.getValue("toolbar");
        if (toolbar instanceof String) {
            return regactions.remove(toolbar);
        }
        return null;
    }

    /**
     * Parse the toolbar preference setting and construct the toolbar GUI control.
     *
     * Call this, if anything has changed in the toolbar settings and you want to refresh
     * the toolbar content (e.g. after registering actions in a plugin)
     */
    public void refreshToolbarControl() {
        control.removeAll();
        buttonActions.clear();
        boolean unregisterTab = Shortcut.findShortcut(KeyEvent.VK_TAB, 0).isPresent();

        for (ActionDefinition action : getDefinedActions()) {
            if (action.isSeparator()) {
                control.addSeparator();
            } else {
                final JButton b = addButtonAndShortcut(action);
                buttonActions.put(b, action);

                Icon i = action.getDisplayIcon();
                if (i != null) {
                    b.setIcon(i);
                    Dimension s = b.getPreferredSize();
                    /* make squared toolbar icons */
                    if (s.width < s.height) {
                        s.width = s.height;
                        b.setMinimumSize(s);
                        b.setMaximumSize(s);
                    } else if (s.height < s.width) {
                        s.height = s.width;
                        b.setMinimumSize(s);
                        b.setMaximumSize(s);
                    }
                } else {
                    // hide action text if an icon is set later (necessary for delayed/background image loading)
                    action.getParametrizedAction().addPropertyChangeListener(evt -> {
                        if (Action.SMALL_ICON.equals(evt.getPropertyName())) {
                            b.setHideActionText(evt.getNewValue() != null);
                        }
                    });
                }
                b.setInheritsPopupMenu(true);
                b.setFocusTraversalKeysEnabled(!unregisterTab);
            }
        }

        boolean visible = Config.getPref().getBoolean("toolbar.visible", true);

        control.setFocusTraversalKeysEnabled(!unregisterTab);
        control.setVisible(visible && control.getComponentCount() != 0);
        control.repaint();
    }

    /**
     * The method to add custom button on toolbar like search or preset buttons
     * @param definitionText toolbar definition text to describe the new button,
     * must be carefully generated by using {@link ActionParser}
     * @param preferredIndex place to put the new button, give -1 for the end of toolbar
     * @param removeIfExists if true and the button already exists, remove it
     */
    public void addCustomButton(String definitionText, int preferredIndex, boolean removeIfExists) {
        List<String> t = new LinkedList<>(getToolString());
        if (t.contains(definitionText)) {
            if (!removeIfExists) return; // do nothing
            t.remove(definitionText);
        } else {
            if (preferredIndex >= 0 && preferredIndex < t.size()) {
                t.add(preferredIndex, definitionText); // add to specified place
            } else {
                t.add(definitionText); // add to the end
            }
        }
        Config.getPref().putList("toolbar", t);
        MainApplication.getToolbar().refreshToolbarControl();
    }

    private JButton addButtonAndShortcut(ActionDefinition action) {
        Action act = action.getParametrizedAction();
        JButton b = control.add(act);

        Shortcut sc = null;
        if (action.getAction() instanceof JosmAction) {
            sc = ((JosmAction) action.getAction()).getShortcut();
            if (sc.getAssignedKey() == KeyEvent.CHAR_UNDEFINED) {
                sc = null;
        }
        }

        long paramCode = 0;
        if (action.hasParameters()) {
            paramCode = action.parameters.hashCode();
        }

        String tt = Optional.ofNullable(action.getDisplayTooltip()).orElse("");

        if (sc == null || paramCode != 0) {
            String name = Optional.ofNullable((String) action.getAction().getValue("toolbar")).orElseGet(action::getDisplayName);
            if (paramCode != 0) {
                name = name+paramCode;
            }
            String desc = action.getDisplayName() + ((paramCode == 0) ? "" : action.parameters.toString());
            sc = Shortcut.registerShortcut("toolbar:"+name, tr("Toolbar: {0}", desc),
                KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
            MainApplication.unregisterShortcut(sc);
            MainApplication.registerActionShortcut(act, sc);

            // add shortcut info to the tooltip if needed
            if (sc.isAssignedUser()) {
                if (tt.startsWith("<html>") && tt.endsWith("</html>")) {
                    tt = tt.substring(6, tt.length()-6);
                }
                tt = PlatformManager.getPlatform().makeTooltip(tt, sc);
            }
        }

        if (!tt.isEmpty()) {
            b.setToolTipText(tt);
        }
        return b;
    }

    private static final DataFlavor ACTION_FLAVOR = new DataFlavor(ActionDefinition.class, "ActionItem");
}
