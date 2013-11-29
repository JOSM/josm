// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintStyleLoader;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.util.FileFilterAllFiles;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JFileChooserManager;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

public class MapPaintDialog extends ToggleDialog implements Main.WindowSwitchListener {

    protected StylesTable tblStyles;
    protected StylesModel model;
    protected DefaultListSelectionModel selectionModel;

    protected OnOffAction onoffAction;
    protected ReloadAction reloadAction;
    protected MoveUpDownAction upAction;
    protected MoveUpDownAction downAction;
    protected JCheckBox cbWireframe;

    public static final JosmAction PREFERENCE_ACTION = PreferencesAction.forPreferenceSubTab(
            tr("Map paint preferences"), null, MapPaintPreference.class);

    /**
     * Constructs a new {@code MapPaintDialog}.
     */
    public MapPaintDialog() {
        super(tr("Map Paint Styles"), "mapstyle", tr("configure the map painting style"),
                Shortcut.registerShortcut("subwindow:mappaint", tr("Toggle: {0}", tr("MapPaint")),
                        KeyEvent.VK_M, Shortcut.ALT_SHIFT), 150);
        build();
    }

    protected void build() {
        model = new StylesModel();

        cbWireframe = new JCheckBox();
        JLabel wfLabel = new JLabel(tr("Wireframe View"), ImageProvider.get("dialogs/mappaint", "wireframe_small"), JLabel.HORIZONTAL);
        wfLabel.setFont(wfLabel.getFont().deriveFont(Font.PLAIN));

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
        cbWireframe.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.main.menu.wireFrameToggleAction.actionPerformed(null);
            }
        });
        cbWireframe.setBorder(new EmptyBorder(new Insets(1,1,1,1)));

        tblStyles = new StylesTable(model);
        tblStyles.setSelectionModel(selectionModel= new DefaultListSelectionModel());
        tblStyles.addMouseListener(new PopupMenuHandler());
        tblStyles.putClientProperty("terminateEditOnFocusLost", true);
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

    protected static class StylesTable extends JTable {

        public StylesTable(TableModel dm) {
            super(dm);
        }

        public void scrollToVisible(int row, int col) {
            if (!(getParent() instanceof JViewport))
                return;
            JViewport viewport = (JViewport) getParent();
            Rectangle rect = getCellRect(row, col, true);
            Point pt = viewport.getViewPosition();
            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
            viewport.scrollRectToVisible(rect);
        }
    }

    @Override
    public void toOtherApplication() {
        // nothing
    }

    @Override
    public void fromOtherApplication() {
        // Reload local styles when they have been changed in an external editor.
        // Checks file modification time.
        List<StyleSource> toReload = new ArrayList<StyleSource>();
        for (StyleSource s : MapPaintStyles.getStyles().getStyleSources()) {
            if (s.isLocal()) {
                File f = new File(s.url);
                long mtime = f.lastModified();
                if (mtime > s.getLastMTime()) {
                    toReload.add(s);
                    s.setLastMTime(mtime);
                }
            }
        }
        if (!toReload.isEmpty()) {
            Main.info(trn("Reloading {0} map style.", "Reloading {0} map styles.", toReload.size(), toReload.size()));
            Main.worker.submit(new MapPaintStyleLoader(toReload));
        }
    }

    @Override
    public void showNotify() {
        MapPaintStyles.addMapPaintSylesUpdateListener(model);
        Main.main.menu.wireFrameToggleAction.addButtonModel(cbWireframe.getModel());
        if (Main.pref.getBoolean("mappaint.auto_reload_local_styles", true)) {
            Main.addWindowSwitchListener(this);
        }
    }

    @Override
    public void hideNotify() {
        Main.main.menu.wireFrameToggleAction.removeButtonModel(cbWireframe.getModel());
        MapPaintStyles.removeMapPaintSylesUpdateListener(model);
        if (Main.pref.getBoolean("mappaint.auto_reload_local_styles", true)) {
            Main.removeWindowSwitchListener(this);
        }
    }

    protected class StylesModel extends AbstractTableModel implements MapPaintSylesUpdateListener {

        List<StyleSource> data = new ArrayList<StyleSource>();

        public StylesModel() {
            data = new ArrayList<StyleSource>(MapPaintStyles.getStyles().getStyleSources());
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

        Class<?>[] columnClasses = {Boolean.class, StyleSource.class};

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
            if (index < 0) return;
            if (index >= getRowCount()) return;
            tblStyles.scrollToVisible(index, 0);
            tblStyles.repaint();
        }

        /**
         * MapPaintSylesUpdateListener interface
         */

        @Override
        public void mapPaintStylesUpdated() {
            data = new ArrayList<StyleSource>(MapPaintStyles.getStyles().getStyleSources());
            fireTableDataChanged();
            tblStyles.repaint();
        }

        @Override
        public void mapPaintStyleEntryUpdated(int idx) {
            data = new ArrayList<StyleSource>(MapPaintStyles.getStyles().getStyleSources());
            fireTableRowsUpdated(idx, idx);
            tblStyles.repaint();
        }
    }

    private class MyCheckBoxRenderer extends JCheckBox implements TableCellRenderer {

        public MyCheckBoxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int column) {
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
            JLabel label = (JLabel)super.getTableCellRendererComponent(table,
                    s.getDisplayString(), isSelected, hasFocus, row, column);
            label.setIcon(s.getIcon());
            label.setToolTipText(s.getToolTipText());
            label.setEnabled(!cbWireframe.isSelected());
            return label;
        }
    }

    protected class OnOffAction extends AbstractAction implements ListSelectionListener {
        public OnOffAction() {
            putValue(NAME, tr("On/Off"));
            putValue(SHORT_DESCRIPTION, tr("Turn selected styles on or off"));
            putValue(SMALL_ICON, ImageProvider.get("apply"));
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
            selectionModel.clearSelection();
            for (int p: pos) {
                selectionModel.addSelectionInterval(p, p);
            }
        }
    }

    /**
     * The action to move down the currently selected entries in the list.
     */
    protected class MoveUpDownAction extends AbstractAction implements ListSelectionListener {

        final int increment;

        public MoveUpDownAction(boolean isDown) {
            increment = isDown ? 1 : -1;
            putValue(NAME, isDown?tr("Down"):tr("Up"));
            putValue(SMALL_ICON, isDown ? ImageProvider.get("dialogs", "down") : ImageProvider.get("dialogs", "up"));
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

            selectionModel.clearSelection();
            for (int row: sel) {
                selectionModel.addSelectionInterval(row + increment, row + increment);
            }
            model.ensureSelectedIsVisible();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    protected class ReloadAction extends AbstractAction implements ListSelectionListener {
        public ReloadAction() {
            putValue(NAME, tr("Reload from file"));
            putValue(SHORT_DESCRIPTION, tr("reload selected styles from file"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
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
            MapPaintStyles.reloadStyles(rows);
            Main.worker.submit(new Runnable() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            selectionModel.clearSelection();
                            for (int r: rows) {
                                selectionModel.addSelectionInterval(r, r);
                            }
                        }
                    });

                }
            });
        }
    }

    protected class SaveAsAction extends AbstractAction {

        public SaveAsAction() {
            putValue(NAME, tr("Save as..."));
            putValue(SHORT_DESCRIPTION, tr("Save a copy of this Style to file and add it to the list"));
            putValue(SMALL_ICON, ImageProvider.get("copy"));
            setEnabled(tblStyles.getSelectedRows().length == 1);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int sel = tblStyles.getSelectionModel().getLeadSelectionIndex();
            if (sel < 0 || sel >= model.getRowCount())
                return;
            final StyleSource s = model.getRow(sel);

            JFileChooserManager fcm = new JFileChooserManager(false, "mappaint.clone-style.lastDirectory", System.getProperty("user.home"));
            String suggestion = fcm.getInitialDirectory() + File.separator + s.getFileNamePart();

            FileFilter ff;
            if (s instanceof MapCSSStyleSource) {
                ff = new ExtensionFileFilter("mapcss,css,zip", "mapcss", tr("Map paint style file (*.mapcss, *.zip)"));
            } else {
                ff = new ExtensionFileFilter("xml,zip", "xml", tr("Map paint style file (*.xml, *.zip)"));
            }
            fcm.createFileChooser(false, null, Arrays.asList(ff, FileFilterAllFiles.getInstance()), ff, JFileChooser.FILES_ONLY)
                    .getFileChooser().setSelectedFile(new File(suggestion));
            JFileChooser fc = fcm.openFileChooser();
            if (fc == null)
                return;
            Main.worker.submit(new SaveToFileTask(s, fc.getSelectedFile()));
        }

        private class SaveToFileTask extends PleaseWaitRunnable {
            private StyleSource s;
            private File file;

            private boolean canceled;
            private boolean error;

            public SaveToFileTask(StyleSource s, File file) {
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
                BufferedInputStream bis = null;
                BufferedOutputStream bos = null;
                try {
                    InputStream in = s.getSourceInputStream();
                    try {
                        bis = new BufferedInputStream(in);
                        bos = new BufferedOutputStream(new FileOutputStream(file));
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = bis.read(buffer)) > -1 && !canceled) {
                            bos.write(buffer, 0, length);
                        }
                    } finally {
                        s.closeSourceInputStream(in);
                    }
                } catch (IOException e) {
                    error = true;
                } finally {
                    Utils.close(bis);
                    Utils.close(bos);
                }
            }

            @Override
            protected void finish() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!error && !canceled) {
                            SourceEntry se = new SourceEntry(s);
                            se.url = file.getPath();
                            MapPaintStyles.addStyle(se);
                            tblStyles.getSelectionModel().setSelectionInterval(model.getRowCount() - 1 , model.getRowCount() - 1);
                            model.ensureSelectedIsVisible();
                        }
                    }
                });
            }
        }
    }

    protected class InfoAction extends AbstractAction {

        boolean errorsTabLoaded;
        boolean sourceTabLoaded;

        public InfoAction() {
            putValue(NAME, tr("Info"));
            putValue(SHORT_DESCRIPTION, tr("view meta information, error log and source definition"));
            putValue(SMALL_ICON, ImageProvider.get("info"));
            setEnabled(tblStyles.getSelectedRows().length == 1);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int sel = tblStyles.getSelectionModel().getLeadSelectionIndex();
            if (sel < 0 || sel >= model.getRowCount())
                return;
            final StyleSource s = model.getRow(sel);
            ExtendedDialog info = new ExtendedDialog(Main.parent, tr("Map Style info"), new String[] {tr("Close")});
            info.setPreferredSize(new Dimension(600, 400));
            info.setButtonIcons(new String[] {"ok.png"});

            final JTabbedPane tabs = new JTabbedPane();

            tabs.add("Info", buildInfoPanel(s));
            JLabel lblInfo = new JLabel(tr("Info"));
            lblInfo.setFont(lblInfo.getFont().deriveFont(Font.PLAIN));
            tabs.setTabComponentAt(0, lblInfo);

            final JPanel pErrors = new JPanel(new GridBagLayout());
            tabs.add("Errors", pErrors);
            JLabel lblErrors;
            if (s.getErrors().isEmpty()) {
                lblErrors = new JLabel(tr("Errors"));
                lblErrors.setFont(lblInfo.getFont().deriveFont(Font.PLAIN));
                lblErrors.setEnabled(false);
                tabs.setTabComponentAt(1, lblErrors);
                tabs.setEnabledAt(1, false);
            } else {
                lblErrors = new JLabel(tr("Errors"), ImageProvider.get("misc", "error"), JLabel.HORIZONTAL);
                tabs.setTabComponentAt(1, lblErrors);
            }

            final JPanel pSource = new JPanel(new GridBagLayout());
            tabs.addTab("Source", pSource);
            JLabel lblSource = new JLabel(tr("Source"));
            lblSource.setFont(lblSource.getFont().deriveFont(Font.PLAIN));
            tabs.setTabComponentAt(2, lblSource);

            tabs.getModel().addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (!errorsTabLoaded && ((SingleSelectionModel) e.getSource()).getSelectedIndex() == 1) {
                        errorsTabLoaded = true;
                        buildErrorsPanel(s, pErrors);
                    }
                    if (!sourceTabLoaded && ((SingleSelectionModel) e.getSource()).getSelectedIndex() == 2) {
                        sourceTabLoaded = true;
                        buildSourcePanel(s, pSource);
                    }
                }
            });
            info.setContent(tabs, false);
            info.showDialog();
        }

        private JPanel buildInfoPanel(StyleSource s) {
            JPanel p = new JPanel(new GridBagLayout());
            StringBuilder text = new StringBuilder("<table cellpadding=3>");
            text.append(tableRow(tr("Title:"), s.getDisplayString()));
            if (s.url.startsWith("http://")) {
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
            text.append(tableRow(tr("Style is currently active?"), s.active ? tr("Yes") : tr("No")));
            text.append("</table>");
            p.add(new JScrollPane(new HtmlPanel(text.toString())), GBC.eol().fill(GBC.BOTH));
            return p;
        }

        private String tableRow(String firstColumn, String secondColumn) {
            return "<tr><td><b>" + firstColumn + "</b></td><td>" + secondColumn + "</td></tr>";
        }

        private void buildSourcePanel(StyleSource s, JPanel p) {
            JosmTextArea txtSource = new JosmTextArea();
            txtSource.setFont(new Font("Monospaced", txtSource.getFont().getStyle(), txtSource.getFont().getSize()));
            txtSource.setEditable(false);
            p.add(new JScrollPane(txtSource), GBC.std().fill());

            try {
                InputStream is = s.getSourceInputStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            txtSource.append(line + "\n");
                        }
                    } finally {
                        reader.close();
                    }
                } finally {
                    s.closeSourceInputStream(is);
                }
            } catch (IOException ex) {
                txtSource.append("<ERROR: failed to read file!>");
            }
        }

        private void buildErrorsPanel(StyleSource s, JPanel p) {
            JosmTextArea txtErrors = new JosmTextArea();
            txtErrors.setFont(new Font("Monospaced", txtErrors.getFont().getStyle(), txtErrors.getFont().getSize()));
            txtErrors.setEditable(false);
            p.add(new JScrollPane(txtErrors), GBC.std().fill());
            for (Throwable t : s.getErrors()) {
                txtErrors.append(t.toString() + "\n");
            }
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
            addSeparator();
            add(new InfoAction());
        }
    }
}
