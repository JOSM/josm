// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.gui.mappaint.StyleSetting;
import org.openstreetmap.josm.gui.mappaint.StyleSettingGuiFactory;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.loader.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.util.FileFilterAllFiles;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.gui.widgets.ScrollableTable;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageOverlay;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Dialog to configure the map painting style.
 * @since 3843
 */
public class MapPaintDialog extends ToggleDialog {

    protected ScrollableTable tblStyles;
    protected StylesModel model;
    protected final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

    protected OnOffAction onoffAction;
    protected ReloadAction reloadAction;
    protected MoveUpDownAction upAction;
    protected MoveUpDownAction downAction;
    protected JCheckBox cbWireframe;

    /**
     * Action that opens the map paint preferences.
     */
    public static final JosmAction PREFERENCE_ACTION = PreferencesAction.forPreferenceSubTab(
            tr("Map paint preferences"), null, MapPaintPreference.class, /* ICON */ "dialogs/mappaintpreference");

    /**
     * Constructs a new {@code MapPaintDialog}.
     */
    public MapPaintDialog() {
        super(tr("Map Paint Styles"), "mapstyle", tr("configure the map painting style"),
                Shortcut.registerShortcut("subwindow:mappaint", tr("Toggle: {0}", tr("MapPaint")),
                        KeyEvent.VK_M, Shortcut.ALT_SHIFT), 150, false, MapPaintPreference.class);
        build();
    }

    protected void build() {
        model = new StylesModel();

        cbWireframe = new JCheckBox();
        JLabel wfLabel = new JLabel(tr("Wireframe View"), ImageProvider.get("dialogs/mappaint", "wireframe_small"), JLabel.HORIZONTAL);
        wfLabel.setFont(wfLabel.getFont().deriveFont(Font.PLAIN));
        wfLabel.setLabelFor(cbWireframe);

        cbWireframe.setModel(new DefaultButtonModel() {
            @Override
            public void setSelected(boolean b) {
                super.setSelected(b);
                tblStyles.setEnabled(!b);
                onoffAction.updateEnabledState();
                upAction.updateEnabledState();
                downAction.updateEnabledState();
            }
        });
        cbWireframe.addActionListener(e -> MainApplication.getMenu().wireFrameToggleAction.actionPerformed(null));
        cbWireframe.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));

        tblStyles = new ScrollableTable(model);
        tblStyles.setSelectionModel(selectionModel);
        tblStyles.addMouseListener(new PopupMenuHandler());
        tblStyles.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblStyles.setBackground(UIManager.getColor("Panel.background"));
        tblStyles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblStyles.setTableHeader(null);
        tblStyles.getColumnModel().getColumn(0).setMaxWidth(1);
        tblStyles.getColumnModel().getColumn(0).setResizable(false);
        tblStyles.getColumnModel().getColumn(0).setCellRenderer(new MyCheckBoxRenderer());
        tblStyles.getColumnModel().getColumn(1).setCellRenderer(new StyleSourceRenderer());
        tblStyles.setShowGrid(false);
        tblStyles.setIntercellSpacing(new Dimension(0, 0));

        JPanel p = new JPanel(new GridBagLayout());
        p.add(cbWireframe, GBC.std(0, 0));
        p.add(wfLabel, GBC.std(1, 0).weight(1, 0));
        p.add(tblStyles, GBC.std(0, 1).span(2).fill());

        reloadAction = new ReloadAction();
        onoffAction = new OnOffAction();
        upAction = new MoveUpDownAction(false);
        downAction = new MoveUpDownAction(true);
        selectionModel.addListSelectionListener(onoffAction);
        selectionModel.addListSelectionListener(reloadAction);
        selectionModel.addListSelectionListener(upAction);
        selectionModel.addListSelectionListener(downAction);

        // Toggle style on Enter and Spacebar
        InputMapUtils.addEnterAction(tblStyles, onoffAction);
        InputMapUtils.addSpacebarAction(tblStyles, onoffAction);

        createLayout(p, true, Arrays.asList(
                new SideButton(onoffAction, false),
                new SideButton(upAction, false),
                new SideButton(downAction, false),
                new SideButton(PREFERENCE_ACTION, false)
        ));
    }

    @Override
    public void showNotify() {
        MapPaintStyles.addMapPaintSylesUpdateListener(model);
        MainApplication.getMenu().wireFrameToggleAction.addButtonModel(cbWireframe.getModel());
    }

    @Override
    public void hideNotify() {
        MainApplication.getMenu().wireFrameToggleAction.removeButtonModel(cbWireframe.getModel());
        MapPaintStyles.removeMapPaintSylesUpdateListener(model);
    }

    protected class StylesModel extends AbstractTableModel implements MapPaintSylesUpdateListener {

        private final Class<?>[] columnClasses = {Boolean.class, StyleSource.class};

        private transient List<StyleSource> data = new ArrayList<>();

        /**
         * Constructs a new {@code StylesModel}.
         */
        public StylesModel() {
            data = new ArrayList<>(MapPaintStyles.getStyles().getStyleSources());
        }

        private StyleSource getRow(int i) {
            return data.get(i);
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0)
                return getRow(row).active;
            else
                return getRow(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return columnClasses[column];
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (row < 0 || row >= getRowCount() || aValue == null)
                return;
            if (column == 0) {
                MapPaintStyles.toggleStyleActive(row);
            }
        }

        /**
         * Make sure the first of the selected entry is visible in the
         * views of this model.
         */
        public void ensureSelectedIsVisible() {
            int index = selectionModel.getMinSelectionIndex();
            if (index < 0)
                return;
            if (index >= getRowCount())
                return;
            tblStyles.scrollToVisible(index, 0);
            tblStyles.repaint();
        }

        @Override
        public void mapPaintStylesUpdated() {
            data = new ArrayList<>(MapPaintStyles.getStyles().getStyleSources());
            fireTableDataChanged();
            tblStyles.repaint();
        }

        @Override
        public void mapPaintStyleEntryUpdated(int idx) {
            data = new ArrayList<>(MapPaintStyles.getStyles().getStyleSources());
            fireTableRowsUpdated(idx, idx);
            tblStyles.repaint();
        }
    }

    private class MyCheckBoxRenderer extends JCheckBox implements TableCellRenderer {

        /**
         * Constructs a new {@code MyCheckBoxRenderer}.
         */
        MyCheckBoxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null)
                return this;
            boolean b = (Boolean) value;
            setSelected(b);
            setEnabled(!cbWireframe.isSelected());
            return this;
        }
    }

    private class StyleSourceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null)
                return this;
            StyleSource s = (StyleSource) value;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                    s.getDisplayString(), isSelected, hasFocus, row, column);
            label.setIcon(s.getIcon());
            label.setToolTipText(s.getToolTipText());
            label.setEnabled(!cbWireframe.isSelected());
            return label;
        }
    }

    protected class OnOffAction extends AbstractAction implements ListSelectionListener {
        /**
         * Constructs a new {@code OnOffAction}.
         */
        public OnOffAction() {
            putValue(NAME, tr("On/Off"));
            putValue(SHORT_DESCRIPTION, tr("Turn selected styles on or off"));
            new ImageProvider("apply").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(!cbWireframe.isSelected() && tblStyles.getSelectedRowCount() > 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] pos = tblStyles.getSelectedRows();
            MapPaintStyles.toggleStyleActive(pos);
            selectionModel.setValueIsAdjusting(true);
            selectionModel.clearSelection();
            for (int p: pos) {
                selectionModel.addSelectionInterval(p, p);
            }
            selectionModel.setValueIsAdjusting(false);
        }
    }

    /**
     * The action to move down the currently selected entries in the list.
     */
    protected class MoveUpDownAction extends AbstractAction implements ListSelectionListener {

        private final int increment;

        /**
         * Constructs a new {@code MoveUpDownAction}.
         * @param isDown {@code true} to move the entry down, {@code false} to move it up
         */
        public MoveUpDownAction(boolean isDown) {
            increment = isDown ? 1 : -1;
            putValue(NAME, isDown ? tr("Down") : tr("Up"));
            new ImageProvider("dialogs", isDown ? "down" : "up").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, isDown ? tr("Move the selected entry one row down.") : tr("Move the selected entry one row up."));
            updateEnabledState();
        }

        public void updateEnabledState() {
            int[] sel = tblStyles.getSelectedRows();
            setEnabled(!cbWireframe.isSelected() && MapPaintStyles.canMoveStyles(sel, increment));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] sel = tblStyles.getSelectedRows();
            MapPaintStyles.moveStyles(sel, increment);

            selectionModel.setValueIsAdjusting(true);
            selectionModel.clearSelection();
            for (int row: sel) {
                selectionModel.addSelectionInterval(row + increment, row + increment);
            }
            selectionModel.setValueIsAdjusting(false);
            model.ensureSelectedIsVisible();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    protected class ReloadAction extends AbstractAction implements ListSelectionListener {
        /**
         * Constructs a new {@code ReloadAction}.
         */
        public ReloadAction() {
            putValue(NAME, tr("Reload from file"));
            putValue(SHORT_DESCRIPTION, tr("reload selected styles from file"));
            new ImageProvider("dialogs", "refresh").getResource().attachImageIcon(this);
            setEnabled(getEnabledState());
        }

        protected boolean getEnabledState() {
            if (cbWireframe.isSelected())
                return false;
            int[] pos = tblStyles.getSelectedRows();
            if (pos.length == 0)
                return false;
            for (int i : pos) {
                if (!model.getRow(i).isLocal())
                    return false;
            }
            return true;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(getEnabledState());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int[] rows = tblStyles.getSelectedRows();
            MapPaintStyleLoader.reloadStyles(rows);
            MainApplication.worker.submit(() -> SwingUtilities.invokeLater(() -> {
                selectionModel.setValueIsAdjusting(true);
                selectionModel.clearSelection();
                for (int r: rows) {
                    selectionModel.addSelectionInterval(r, r);
                }
                selectionModel.setValueIsAdjusting(false);
            }));
        }
    }

    protected class SaveAsAction extends AbstractAction {

        /**
         * Constructs a new {@code SaveAsAction}.
         */
        public SaveAsAction() {
            putValue(NAME, tr("Save as..."));
            putValue(SHORT_DESCRIPTION, tr("Save a copy of this Style to file and add it to the list"));
            new ImageProvider("copy").getResource().attachImageIcon(this);
            setEnabled(tblStyles.getSelectedRows().length == 1);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int sel = tblStyles.getSelectionModel().getLeadSelectionIndex();
            if (sel < 0 || sel >= model.getRowCount())
                return;
            final StyleSource s = model.getRow(sel);

            FileChooserManager fcm = new FileChooserManager(false, "mappaint.clone-style.lastDirectory", System.getProperty("user.home"));
            String suggestion = fcm.getInitialDirectory() + File.separator + s.getFileNamePart();

            FileFilter ff;
            if (s instanceof MapCSSStyleSource) {
                ff = new ExtensionFileFilter("mapcss,css,zip", "mapcss", tr("Map paint style file (*.mapcss, *.zip)"));
            } else {
                ff = new ExtensionFileFilter("xml,zip", "xml", tr("Map paint style file (*.xml, *.zip)"));
            }
            fcm.createFileChooser(false, null, Arrays.asList(ff, FileFilterAllFiles.getInstance()), ff, JFileChooser.FILES_ONLY)
                    .getFileChooser().setSelectedFile(new File(suggestion));
            AbstractFileChooser fc = fcm.openFileChooser();
            if (fc == null)
                return;
            MainApplication.worker.submit(new SaveToFileTask(s, fc.getSelectedFile()));
        }

        private class SaveToFileTask extends PleaseWaitRunnable {
            private final StyleSource s;
            private final File file;

            private boolean canceled;
            private boolean error;

            SaveToFileTask(StyleSource s, File file) {
                super(tr("Reloading style sources"));
                this.s = s;
                this.file = file;
            }

            @Override
            protected void cancel() {
                canceled = true;
            }

            @Override
            protected void realRun() {
                getProgressMonitor().indeterminateSubTask(
                        tr("Save style ''{0}'' as ''{1}''", s.getDisplayString(), file.getPath()));
                try {
                    try (InputStream in = s.getSourceInputStream()) {
                        Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    Logging.warn(e);
                    error = true;
                }
            }

            @Override
            protected void finish() {
                SwingUtilities.invokeLater(() -> {
                    if (!error && !canceled) {
                        SourceEntry se = new SourceEntry(s);
                        se.url = file.getPath();
                        MapPaintStyles.addStyle(se);
                        tblStyles.getSelectionModel().setSelectionInterval(model.getRowCount() - 1, model.getRowCount() - 1);
                        model.ensureSelectedIsVisible();
                    }
                });
            }
        }
    }

    /**
     * Displays information about selected paint style in a new dialog.
     */
    protected class InfoAction extends AbstractAction {

        private boolean errorsTabLoaded;
        private boolean warningsTabLoaded;
        private boolean sourceTabLoaded;

        /**
         * Constructs a new {@code InfoAction}.
         */
        public InfoAction() {
            putValue(NAME, tr("Info"));
            putValue(SHORT_DESCRIPTION, tr("view meta information, error log and source definition"));
            new ImageProvider("info").getResource().attachImageIcon(this);
            setEnabled(tblStyles.getSelectedRows().length == 1);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int sel = tblStyles.getSelectionModel().getLeadSelectionIndex();
            if (sel < 0 || sel >= model.getRowCount())
                return;
            final StyleSource s = model.getRow(sel);
            ExtendedDialog info = new ExtendedDialog(Main.parent, tr("Map Style info"), tr("Close"));
            info.setPreferredSize(new Dimension(600, 400));
            info.setButtonIcons("ok");

            final JTabbedPane tabs = new JTabbedPane();

            JLabel lblInfo = new JLabel(tr("Info"));
            lblInfo.setLabelFor(tabs.add("Info", buildInfoPanel(s)));
            lblInfo.setFont(lblInfo.getFont().deriveFont(Font.PLAIN));
            tabs.setTabComponentAt(0, lblInfo);

            final JPanel pErrors = addErrorOrWarningTab(tabs, lblInfo,
                    s.getErrors(), marktr("Errors"), 1, ImageProvider.get("misc", "error"));
            final JPanel pWarnings = addErrorOrWarningTab(tabs, lblInfo,
                    s.getWarnings(), marktr("Warnings"), 2, ImageProvider.get("warning-small"));

            final JPanel pSource = new JPanel(new GridBagLayout());
            JLabel lblSource = new JLabel(tr("Source"));
            lblSource.setLabelFor(tabs.add("Source", pSource));
            lblSource.setFont(lblSource.getFont().deriveFont(Font.PLAIN));
            tabs.setTabComponentAt(3, lblSource);

            tabs.getModel().addChangeListener(e1 -> {
                if (!errorsTabLoaded && ((SingleSelectionModel) e1.getSource()).getSelectedIndex() == 1) {
                    errorsTabLoaded = true;
                    buildErrorsOrWarningPanel(s.getErrors(), pErrors);
                }
                if (!warningsTabLoaded && ((SingleSelectionModel) e1.getSource()).getSelectedIndex() == 2) {
                    warningsTabLoaded = true;
                    buildErrorsOrWarningPanel(s.getWarnings(), pWarnings);
                }
                if (!sourceTabLoaded && ((SingleSelectionModel) e1.getSource()).getSelectedIndex() == 3) {
                    sourceTabLoaded = true;
                    buildSourcePanel(s, pSource);
                }
            });
            info.setContent(tabs, false);
            info.showDialog();
        }

        private JPanel addErrorOrWarningTab(final JTabbedPane tabs, JLabel lblInfo,
                Collection<?> items, String title, int pos, ImageIcon icon) {
            final JPanel pErrors = new JPanel(new GridBagLayout());
            tabs.add(title, pErrors);
            if (items.isEmpty()) {
                JLabel lblErrors = new JLabel(tr(title));
                lblErrors.setLabelFor(pErrors);
                lblErrors.setFont(lblInfo.getFont().deriveFont(Font.PLAIN));
                lblErrors.setEnabled(false);
                tabs.setTabComponentAt(pos, lblErrors);
                tabs.setEnabledAt(pos, false);
            } else {
                JLabel lblErrors = new JLabel(tr(title), icon, JLabel.HORIZONTAL);
                lblErrors.setLabelFor(pErrors);
                tabs.setTabComponentAt(pos, lblErrors);
            }
            return pErrors;
        }

        private JPanel buildInfoPanel(StyleSource s) {
            JPanel p = new JPanel(new GridBagLayout());
            StringBuilder text = new StringBuilder("<table cellpadding=3>");
            text.append(tableRow(tr("Title:"), s.getDisplayString()));
            if (s.url.startsWith("http://") || s.url.startsWith("https://")) {
                text.append(tableRow(tr("URL:"), s.url));
            } else if (s.url.startsWith("resource://")) {
                text.append(tableRow(tr("Built-in Style, internal path:"), s.url));
            } else {
                text.append(tableRow(tr("Path:"), s.url));
            }
            if (s.icon != null) {
                text.append(tableRow(tr("Icon:"), s.icon));
            }
            if (s.getBackgroundColorOverride() != null) {
                text.append(tableRow(tr("Background:"), Utils.toString(s.getBackgroundColorOverride())));
            }
            text.append(tableRow(tr("Style is currently active?"), s.active ? tr("Yes") : tr("No")))
                .append("</table>");
            p.add(new JScrollPane(new HtmlPanel(text.toString())), GBC.eol().fill(GBC.BOTH));
            return p;
        }

        private String tableRow(String firstColumn, String secondColumn) {
            return "<tr><td><b>" + firstColumn + "</b></td><td>" + secondColumn + "</td></tr>";
        }

        private void buildSourcePanel(StyleSource s, JPanel p) {
            JosmTextArea txtSource = new JosmTextArea();
            txtSource.setFont(GuiHelper.getMonospacedFont(txtSource));
            txtSource.setEditable(false);
            p.add(new JScrollPane(txtSource), GBC.std().fill());

            try {
                InputStream is = s.getSourceInputStream();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        txtSource.append(line + '\n');
                    }
                } finally {
                    s.closeSourceInputStream(is);
                }
            } catch (IOException ex) {
                Logging.error(ex);
                txtSource.append("<ERROR: failed to read file!>");
            }
            txtSource.setCaretPosition(0);
        }

        private <T> void buildErrorsOrWarningPanel(Collection<T> items, JPanel p) {
            JosmTextArea txtErrors = new JosmTextArea();
            txtErrors.setFont(GuiHelper.getMonospacedFont(txtErrors));
            txtErrors.setEditable(false);
            p.add(new JScrollPane(txtErrors), GBC.std().fill());
            for (T t : items) {
                txtErrors.append(t.toString() + '\n');
            }
            txtErrors.setCaretPosition(0);
        }
    }

    class PopupMenuHandler extends PopupMenuLauncher {
        @Override
        public void launch(MouseEvent evt) {
            if (cbWireframe.isSelected())
                return;
            super.launch(evt);
        }

        @Override
        protected void showMenu(MouseEvent evt) {
            menu = new MapPaintPopup();
            super.showMenu(evt);
        }
    }

    /**
     * The popup menu displayed when right-clicking a map paint entry
     */
    public class MapPaintPopup extends JPopupMenu {
        /**
         * Constructs a new {@code MapPaintPopup}.
         */
        public MapPaintPopup() {
            add(reloadAction);
            add(new SaveAsAction());

            JMenu setMenu = new JMenu(tr("Style settings"));
            setMenu.setIcon(new ImageProvider("preference").setMaxSize(ImageSizes.POPUPMENU).addOverlay(
                new ImageOverlay(new ImageProvider("dialogs/mappaint", "pencil"), 0.5, 0.5, 1.0, 1.0)).get());
            setMenu.setToolTipText(tr("Customize the style"));
            add(setMenu);

            int sel = tblStyles.getSelectionModel().getLeadSelectionIndex();
            StyleSource style = null;
            if (sel >= 0 && sel < model.getRowCount()) {
                style = model.getRow(sel);
            }
            if (style == null || style.settings.isEmpty()) {
                setMenu.setEnabled(false);
            } else {
                for (StyleSetting s : style.settings) {
                    StyleSettingGuiFactory.getStyleSettingGui(s).addMenuEntry(setMenu);
                }
            }

            addSeparator();
            add(new InfoAction());
        }
    }
}
