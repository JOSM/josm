// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.xml.sax.SAXException;

public abstract class SourceEditor extends JPanel {

    final protected boolean isMapPaint;
    
    protected JTable tblActiveSources;
    protected ActiveSourcesModel activeSourcesModel;
    protected JList lstAvailableSources;
    protected AvailableSourcesListModel availableSourcesModel;
    protected JTable tblIconPaths = null;
    protected IconPathTableModel iconPathsModel;
    protected boolean sourcesInitiallyLoaded;
    protected String availableSourcesUrl;

    /**
     * constructor
     * @param isMapPaint true for MapPaintPreference subclass, false
     *  for TaggingPresetPreference subclass
     * @param availableSourcesUrl the URL to the list of available sources
     */
    public SourceEditor(final boolean isMapPaint, final String availableSourcesUrl) {

        this.isMapPaint = isMapPaint;
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        lstAvailableSources = new JList(availableSourcesModel = new AvailableSourcesListModel(selectionModel));
        lstAvailableSources.setSelectionModel(selectionModel);
        lstAvailableSources.setCellRenderer(new SourceEntryListCellRenderer());
        this.availableSourcesUrl = availableSourcesUrl;

        selectionModel = new DefaultListSelectionModel();
        tblActiveSources = new JTable(activeSourcesModel = new ActiveSourcesModel(selectionModel)) {
            // some kind of hack to prevent the table from scrolling slightly to the
            // right when clicking on the text
            public void scrollRectToVisible(Rectangle aRect) {
                super.scrollRectToVisible(new Rectangle(0, aRect.y, aRect.width, aRect.height));
            }
        };
        tblActiveSources.putClientProperty("terminateEditOnFocusLost", true);
        tblActiveSources.setSelectionModel(selectionModel);
        tblActiveSources.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblActiveSources.setShowGrid(false);
        tblActiveSources.setIntercellSpacing(new Dimension(0, 0));
        tblActiveSources.setTableHeader(null);
        tblActiveSources.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        SourceEntryTableCellRenderer sourceEntryRenderer = new SourceEntryTableCellRenderer();
        if (isMapPaint) {
            tblActiveSources.getColumnModel().getColumn(0).setMaxWidth(1);
            tblActiveSources.getColumnModel().getColumn(0).setResizable(false);
            tblActiveSources.getColumnModel().getColumn(1).setCellRenderer(sourceEntryRenderer);
        } else {
            tblActiveSources.getColumnModel().getColumn(0).setCellRenderer(sourceEntryRenderer);
        }

        activeSourcesModel.addTableModelListener(new TableModelListener() {
            // Force swing to show horizontal scrollbars for the JTable
            // Yes, this is a little ugly, but should work
            @Override
            public void tableChanged(TableModelEvent e) {
                adjustColumnWidth(tblActiveSources, isMapPaint ? 1 : 0);
            }
        });
        activeSourcesModel.setActiveSources(getInitialSourcesList());
        
        final EditActiveSourceAction editActiveSourceAction = new EditActiveSourceAction();
        tblActiveSources.getSelectionModel().addListSelectionListener(editActiveSourceAction);
        tblActiveSources.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblActiveSources.rowAtPoint(e.getPoint());
                    int col = tblActiveSources.columnAtPoint(e.getPoint());
                    if (row < 0 || row >= tblActiveSources.getRowCount())
                        return;
                    if (isMapPaint  && col != 1)
                        return;
                    editActiveSourceAction.actionPerformed(null);
                }
            }
        });

        RemoveActiveSourcesAction removeActiveSourcesAction = new RemoveActiveSourcesAction();
        tblActiveSources.getSelectionModel().addListSelectionListener(removeActiveSourcesAction);
        tblActiveSources.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "delete");
        tblActiveSources.getActionMap().put("delete", removeActiveSourcesAction);

        MoveUpDownAction moveUp = null;
        MoveUpDownAction moveDown = null;
        if (isMapPaint) {
            moveUp = new MoveUpDownAction(false);
            moveDown = new MoveUpDownAction(true);
            tblActiveSources.getSelectionModel().addListSelectionListener(moveUp);
            tblActiveSources.getSelectionModel().addListSelectionListener(moveDown);
            activeSourcesModel.addTableModelListener(moveUp);
            activeSourcesModel.addTableModelListener(moveDown);
        }

        ActivateSourcesAction activateSourcesAction = new ActivateSourcesAction();
        lstAvailableSources.addListSelectionListener(activateSourcesAction);
        JButton activate = new JButton(activateSourcesAction);

        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.gridwidth = 2;
        gbc.anchor = GBC.WEST;
        gbc.insets = new Insets(5, 11, 0, 0);

        add(new JLabel(getStr(I18nString.AVAILABLE_SOURCES)), gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(5, 0, 0, 6);

        add(new JLabel(getStr(I18nString.ACTIVE_SOURCES)), gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 0.8;
        gbc.fill = GBC.BOTH;
        gbc.anchor = GBC.CENTER;
        gbc.insets = new Insets(0, 11, 0, 0);

        JScrollPane sp1 = new JScrollPane(lstAvailableSources);
        add(sp1, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GBC.VERTICAL;
        gbc.insets = new Insets(0, 0, 0, 0);

        JToolBar middleTB = new JToolBar();
        middleTB.setFloatable(false);
        middleTB.setBorderPainted(false);
        middleTB.setOpaque(false);
        middleTB.add(Box.createHorizontalGlue());
        middleTB.add(activate);
        middleTB.add(Box.createHorizontalGlue());
        add(middleTB, gbc);

        gbc.gridx++;
        gbc.weightx = 0.5;
        gbc.fill = GBC.BOTH;

        JScrollPane sp = new JScrollPane(tblActiveSources);
        add(sp, gbc);
        sp.setColumnHeaderView(null);

        gbc.gridx++;
        gbc.weightx = 0.0;
        gbc.fill = GBC.VERTICAL;
        gbc.insets = new Insets(0, 0, 0, 6);

        JToolBar sideButtonTB = new JToolBar(JToolBar.VERTICAL);
        sideButtonTB.setFloatable(false);
        sideButtonTB.setBorderPainted(false);
        sideButtonTB.setOpaque(false);
        sideButtonTB.add(new NewActiveSourceAction());
        sideButtonTB.add(editActiveSourceAction);
        sideButtonTB.add(removeActiveSourcesAction);
        sideButtonTB.addSeparator(new Dimension(12, 30));
        if (isMapPaint) {
            sideButtonTB.add(moveUp);
            sideButtonTB.add(moveDown);
        }
        add(sideButtonTB, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.weightx = 0.5;
        gbc.fill = GBC.HORIZONTAL;
        gbc.anchor = GBC.WEST;
        gbc.insets = new Insets(0, 11, 0, 0);

        JToolBar bottomLeftTB = new JToolBar();
        bottomLeftTB.setFloatable(false);
        bottomLeftTB.setBorderPainted(false);
        bottomLeftTB.setOpaque(false);
        bottomLeftTB.add(new ReloadSourcesAction(availableSourcesUrl));
        bottomLeftTB.add(Box.createHorizontalGlue());
        add(bottomLeftTB, gbc);

        gbc.gridx = 2;
        gbc.anchor = GBC.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);

        JToolBar bottomRightTB = new JToolBar();
        bottomRightTB.setFloatable(false);
        bottomRightTB.setBorderPainted(false);
        bottomRightTB.setOpaque(false);
        bottomRightTB.add(Box.createHorizontalGlue());
        bottomRightTB.add(new JButton(new ResetAction()));
        add(bottomRightTB, gbc);

        /***
         * Icon configuration
         **/

        selectionModel = new DefaultListSelectionModel();
        tblIconPaths = new JTable(iconPathsModel = new IconPathTableModel(selectionModel));
        tblIconPaths.setSelectionModel(selectionModel);
        tblIconPaths.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblIconPaths.setTableHeader(null);
        tblIconPaths.getColumnModel().getColumn(0).setCellEditor(new FileOrUrlCellEditor(false));
        tblIconPaths.setRowHeight(20);
        tblIconPaths.putClientProperty("terminateEditOnFocusLost", true);
        iconPathsModel.setIconPaths(getInitialIconPathsList());

        EditIconPathAction editIconPathAction = new EditIconPathAction();
        tblIconPaths.getSelectionModel().addListSelectionListener(editIconPathAction);

        RemoveIconPathAction removeIconPathAction = new RemoveIconPathAction();
        tblIconPaths.getSelectionModel().addListSelectionListener(removeIconPathAction);
        tblIconPaths.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "delete");
        tblIconPaths.getActionMap().put("delete", removeIconPathAction);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 1.0;
        gbc.gridwidth = GBC.REMAINDER;
        gbc.insets = new Insets(8, 11, 8, 6);

        add(new JSeparator(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 11, 0, 6);

        add(new JLabel(tr("Icon paths:")), gbc);

        gbc.gridy++;
        gbc.weighty = 0.2;
        gbc.gridwidth = 3;
        gbc.fill = GBC.BOTH;
        gbc.insets = new Insets(0, 11, 0, 0);

        add(sp = new JScrollPane(tblIconPaths), gbc);
        sp.setColumnHeaderView(null);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.fill = GBC.VERTICAL;
        gbc.insets = new Insets(0, 0, 0, 6);

        JToolBar sideButtonTBIcons = new JToolBar(JToolBar.VERTICAL);
        sideButtonTBIcons.setFloatable(false);
        sideButtonTBIcons.setBorderPainted(false);
        sideButtonTBIcons.setOpaque(false);
        sideButtonTBIcons.add(new NewIconPathAction());
        sideButtonTBIcons.add(editIconPathAction);
        sideButtonTBIcons.add(removeIconPathAction);
        add(sideButtonTBIcons, gbc);
    }

    /**
     * Load the list of source entries that the user has configured.
     */
    abstract public Collection<? extends SourceEntry> getInitialSourcesList();

    /**
     * Load the list of configured icon paths.
     */
    abstract public Collection<String> getInitialIconPathsList();

    /**
     * Get the default list of entries (used when resetting the list).
     */
    abstract public Collection<ExtendedSourceEntry> getDefault();

    /**
     * Save the settings after user clicked "Ok".
     * @return true if restart is required
     */
    abstract public boolean finish();

    /**
     * Provide the GUI strings. (There are differences for MapPaint and Preset)
     */
    abstract protected String getStr(I18nString ident);

    /**
     * Identifiers for strings that need to be provided.
     */
    protected enum I18nString { AVAILABLE_SOURCES, ACTIVE_SOURCES, NEW_SOURCE_ENTRY_TOOLTIP, NEW_SOURCE_ENTRY,
            REMOVE_SOURCE_TOOLTIP, EDIT_SOURCE_TOOLTIP, ACTIVATE_TOOLTIP, RELOAD_ALL_AVAILABLE,
            LOADING_SOURCES_FROM, FAILED_TO_LOAD_SOURCES_FROM, FAILED_TO_LOAD_SOURCES_FROM_HELP_TOPIC,
            ILLEGAL_FORMAT_OF_ENTRY }

    /**
     * adjust the preferred width of column col to the maximum preferred width of the cells
     * requires JTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
     */
    private static void adjustColumnWidth(JTable tbl, int col) {
        int maxwidth = 0;
		for (int row=0; row<tbl.getRowCount(); row++) {
            TableCellRenderer tcr = tbl.getCellRenderer(row, col);
                Object val = tbl.getValueAt(row, col);
                Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, row, col);
                maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
		}
		tbl.getColumnModel().getColumn(col).setPreferredWidth(maxwidth);
    }

    public boolean hasActiveSourcesChanged() {
        Collection<? extends SourceEntry> prev = getInitialSourcesList();
        List<SourceEntry> cur = activeSourcesModel.getSources();
        if (prev.size() != cur.size())
            return true;
        Iterator<? extends SourceEntry> p = prev.iterator();
        Iterator<SourceEntry> c = cur.iterator();
        while (p.hasNext()) {
            SourceEntry pe = p.next();
            SourceEntry ce = c.next();
            if (!equal(pe.url, ce.url) || !equal(pe.name, ce.name) || pe.active != ce.active)
                return true;
        }
        return false;
    }

    public Collection<SourceEntry> getActiveSources() {
        return activeSourcesModel.getSources();
    }

    public void removeSources(Collection<Integer> idxs) {
        activeSourcesModel.removeIdxs(idxs);
    }

    protected void reloadAvailableSources(String url) {
        Main.worker.submit(new SourceLoader(url));
    }

    public void initiallyLoadAvailableSources() {
        if (!sourcesInitiallyLoaded) {
            reloadAvailableSources(this.availableSourcesUrl);
        }
        sourcesInitiallyLoaded = true;
    }

    protected static class AvailableSourcesListModel extends DefaultListModel {
        private ArrayList<ExtendedSourceEntry> data;
        private DefaultListSelectionModel selectionModel;

        public AvailableSourcesListModel(DefaultListSelectionModel selectionModel) {
            data = new ArrayList<ExtendedSourceEntry>();
            this.selectionModel = selectionModel;
        }

        public void setSources(List<ExtendedSourceEntry> sources) {
            data.clear();
            if (sources != null) {
                data.addAll(sources);
            }
            fireContentsChanged(this, 0, data.size());
        }

        @Override
        public Object getElementAt(int index) {
            return data.get(index);
        }

        @Override
        public int getSize() {
            if (data == null) return 0;
            return data.size();
        }

        public void deleteSelected() {
            Iterator<ExtendedSourceEntry> it = data.iterator();
            int i=0;
            while(it.hasNext()) {
                it.next();
                if (selectionModel.isSelectedIndex(i)) {
                    it.remove();
                }
                i++;
            }
            fireContentsChanged(this, 0, data.size());
        }

        public List<ExtendedSourceEntry> getSelected() {
            ArrayList<ExtendedSourceEntry> ret = new ArrayList<ExtendedSourceEntry>();
            for(int i=0; i<data.size();i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    ret.add(data.get(i));
                }
            }
            return ret;
        }
    }

    protected class ActiveSourcesModel extends AbstractTableModel {
        private List<SourceEntry> data;
        private DefaultListSelectionModel selectionModel;

        public ActiveSourcesModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
            this.data = new ArrayList<SourceEntry>();
        }

        public int getColumnCount() {
            return isMapPaint ? 2 : 1;
        }

        public int getRowCount() {
            return data == null ? 0 : data.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (isMapPaint && columnIndex == 0)
                return data.get(rowIndex).active;
            else
                return data.get(rowIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return isMapPaint && columnIndex == 0;
        }

        Class<?>[] columnClasses = {Boolean.class, SourceEntry.class};

        @Override
        public Class<?> getColumnClass(int column) {
            return isMapPaint ? columnClasses[column] : SourceEntry.class;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (row < 0 || row >= getRowCount() || aValue == null)
                return;
            if (isMapPaint && column == 0) {
                data.get(row).active = ! data.get(row).active;
            }
        }

        public void setActiveSources(Collection<? extends SourceEntry> sources) {
            data.clear();
            if (sources != null) {
                for (SourceEntry e : sources) {
                    data.add(new SourceEntry(e));
                }
            }
            fireTableDataChanged();
        }

        public void addSource(SourceEntry entry) {
            if (entry == null) return;
            data.add(entry);
            fireTableDataChanged();
            int idx = data.indexOf(entry);
            if (idx >= 0) {
                selectionModel.setSelectionInterval(idx, idx);
            }
        }

        public void removeSelected() {
            Iterator<SourceEntry> it = data.iterator();
            int i=0;
            while(it.hasNext()) {
                it.next();
                if (selectionModel.isSelectedIndex(i)) {
                    it.remove();
                }
                i++;
            }
            fireTableDataChanged();
        }

        public void removeIdxs(Collection<Integer> idxs) {
            List<SourceEntry> newData = new ArrayList<SourceEntry>();
            for (int i=0; i<data.size(); ++i) {
                if (!idxs.contains(i)) {
                    newData.add(data.get(i));
                }
            }
            data = newData;
            fireTableDataChanged();
        }

        public void addExtendedSourceEntries(List<ExtendedSourceEntry> sources) {
            if (sources == null) return;
            for (ExtendedSourceEntry info: sources) {
                data.add(new SourceEntry(info.url, info.name, info.getDisplayName(), true));
            }
            fireTableDataChanged();
            selectionModel.clearSelection();
            for (ExtendedSourceEntry info: sources) {
                int pos = data.indexOf(info);
                if (pos >=0) {
                    selectionModel.addSelectionInterval(pos, pos);
                }
            }
        }

        public List<SourceEntry> getSources() {
            return new ArrayList<SourceEntry>(data);
        }

        public boolean canMove(int i) {
            int[] sel = tblActiveSources.getSelectedRows();
            if (sel.length == 0)
                return false;
            if (i < 0) { // Up
                return sel[0] >= -i;
            } else if (i > 0) { // Down
                return sel[sel.length-1] <= getRowCount()-1 - i;
            } else
                return true;
        }

        public void move(int i) {
            if (!canMove(i)) return;
            int[] sel = tblActiveSources.getSelectedRows();
            for (int row: sel) {
                SourceEntry t1 = data.get(row);
                SourceEntry t2 = data.get(row + i);
                data.set(row, t2);
                data.set(row + i, t1);
            }
            selectionModel.clearSelection();
            for (int row: sel) {
                selectionModel.addSelectionInterval(row + i, row + i);
            }
        }
    }

    public static class ExtendedSourceEntry extends SourceEntry {
        public String simpleFileName;
        public String version;
        public String author;
        public String link;
        public String description;

        public ExtendedSourceEntry(String simpleFileName, String url) {
            super(url, null, null, true);
            this.simpleFileName = simpleFileName;
            version = author = link = description = title = null;
        }

        /**
         * @return string representation for GUI list or menu entry
         */
        public String getDisplayName() {
            return title == null ? simpleFileName : title;
        }

        public String getTooltip() {
            String s = tr("Short Description: {0}", getDisplayName()) + "<br>" + tr("URL: {0}", url);
            if (author != null) {
                s += "<br>" + tr("Author: {0}", author);
            }
            if (link != null) {
                s += "<br>" + tr("Webpage: {0}", link);
            }
            if (description != null) {
                s += "<br>" + tr("Description: {0}", description);
            }
            if (version != null) {
                s += "<br>" + tr("Version: {0}", version);
            }
            return "<html>" + s + "</html>";
        }

        @Override
        public String toString() {
            return "<html><b>" + getDisplayName() + "</b> (" + url + ")</html>";
        }
    }

    protected class EditSourceEntryDialog extends ExtendedDialog {

        private JTextField tfTitle;
        private JTextField tfURL;
        private JCheckBox cbActive;

        public EditSourceEntryDialog(Component parent, String title, SourceEntry e) {
            super(parent,
                    title,
                    new String[] {tr("Ok"), tr("Cancel")});

            JPanel p = new JPanel(new GridBagLayout());

            tfTitle = new JTextField(60);
            p.add(new JLabel(tr("Name (optional):")), GBC.std().insets(15, 0, 5, 5));
            p.add(tfTitle, GBC.eol().insets(0, 0, 5, 5));

            tfURL = new JTextField(60);
            p.add(new JLabel(tr("URL / File:")), GBC.std().insets(15, 0, 5, 0));
            p.add(tfURL, GBC.std().insets(0, 0, 5, 5));
            JButton fileChooser = new JButton(new LaunchFileChooserAction());
            fileChooser.setMargin(new Insets(0, 0, 0, 0));
            p.add(fileChooser, GBC.eol().insets(0, 0, 5, 5));

            if (e != null) {
                if (e.title != null) {
                    tfTitle.setText(e.title);
                }
                tfURL.setText(e.url);
            }

            if (isMapPaint) {
                cbActive = new JCheckBox(tr("active"), e != null ? e.active : true);
                p.add(cbActive, GBC.eol().insets(15, 0, 5, 0));
            }
            setButtonIcons(new String[] {"ok", "cancel"});
            setContent(p);
        }

        class LaunchFileChooserAction extends AbstractAction {
            public LaunchFileChooserAction() {
                putValue(SMALL_ICON, ImageProvider.get("open"));
                putValue(SHORT_DESCRIPTION, tr("Launch a file chooser to select a file"));
            }

            protected void prepareFileChooser(String url, JFileChooser fc) {
                if (url == null || url.trim().length() == 0) return;
                URL sourceUrl = null;
                try {
                    sourceUrl = new URL(url);
                } catch(MalformedURLException e) {
                    File f = new File(url);
                    if (f.isFile()) {
                        f = f.getParentFile();
                    }
                    if (f != null) {
                        fc.setCurrentDirectory(f);
                    }
                    return;
                }
                if (sourceUrl.getProtocol().startsWith("file")) {
                    File f = new File(sourceUrl.getPath());
                    if (f.isFile()) {
                        f = f.getParentFile();
                    }
                    if (f != null) {
                        fc.setCurrentDirectory(f);
                    }
                }
            }

            public void actionPerformed(ActionEvent e) {
                JFileChooser fc= new JFileChooser();
                prepareFileChooser(tfURL.getText(), fc);
                int ret = fc.showOpenDialog(JOptionPane.getFrameForComponent(SourceEditor.this));
                if (ret != JFileChooser.APPROVE_OPTION)
                    return;
                tfURL.setText(fc.getSelectedFile().toString());
            }
        }

        public String getTitle() {
            return tfTitle.getText();
        }

        public String getURL() {
            return tfURL.getText();
        }

        public boolean active() {
            if (!isMapPaint)
                throw new UnsupportedOperationException();
            return cbActive.isSelected();
        }
    }

    class NewActiveSourceAction extends AbstractAction {
        public NewActiveSourceAction() {
            putValue(NAME, tr("New"));
            putValue(SHORT_DESCRIPTION, getStr(I18nString.NEW_SOURCE_ENTRY_TOOLTIP));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
        }

        public void actionPerformed(ActionEvent evt) {
            EditSourceEntryDialog editEntryDialog = new EditSourceEntryDialog(
                    SourceEditor.this,
                    getStr(I18nString.NEW_SOURCE_ENTRY),
                    null);
            editEntryDialog.showDialog();
            if (editEntryDialog.getValue() == 1) {
                boolean active = true;
                if (isMapPaint) {
                    active = editEntryDialog.active();
                }
                activeSourcesModel.addSource(new SourceEntry(
                        editEntryDialog.getURL(),
                        null, editEntryDialog.getTitle(), active));
                activeSourcesModel.fireTableDataChanged();
            }
        }
    }

    class RemoveActiveSourcesAction extends AbstractAction implements ListSelectionListener {

        public RemoveActiveSourcesAction() {
            putValue(NAME, tr("Remove"));
            putValue(SHORT_DESCRIPTION, getStr(I18nString.REMOVE_SOURCE_TOOLTIP));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(tblActiveSources.getSelectedRowCount() > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            activeSourcesModel.removeSelected();
        }
    }

    class EditActiveSourceAction extends AbstractAction implements ListSelectionListener {
        public EditActiveSourceAction() {
            putValue(NAME, tr("Edit"));
            putValue(SHORT_DESCRIPTION, getStr(I18nString.EDIT_SOURCE_TOOLTIP));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(tblActiveSources.getSelectedRowCount() == 1);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent evt) {
            int pos = tblActiveSources.getSelectedRow();
            if (pos < 0 || pos >= tblActiveSources.getRowCount())
                return;

            SourceEntry e = (SourceEntry) activeSourcesModel.getValueAt(pos, 1);

            EditSourceEntryDialog editEntryDialog = new EditSourceEntryDialog(
                    SourceEditor.this, tr("Edit source entry:"), e);
            editEntryDialog.showDialog();
            if (editEntryDialog.getValue() == 1) {
                if (e.title != null || !equal(editEntryDialog.getTitle(), "")) {
                    e.title = editEntryDialog.getTitle();
                    if (equal(e.title, "")) {
                        e.title = null;
                    }
                }
                e.url = editEntryDialog.getURL();
                if (isMapPaint) {
                    e.active = editEntryDialog.active();
                }
                activeSourcesModel.fireTableRowsUpdated(pos, pos);
            }
        }
    }

    /**
     * The action to move the currently selected entries up or down in the list.
     */
    class MoveUpDownAction extends AbstractAction implements ListSelectionListener, TableModelListener {
        final int increment;
        public MoveUpDownAction(boolean isDown) {
            increment = isDown ? 1 : -1;
            putValue(SMALL_ICON, isDown ? ImageProvider.get("dialogs", "down") : ImageProvider.get("dialogs", "up"));
            putValue(SHORT_DESCRIPTION, isDown ? tr("Move the selected entry one row down.") : tr("Move the selected entry one row up."));
            updateEnabledState();
        }

        public void updateEnabledState() {
            setEnabled(activeSourcesModel.canMove(increment));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            activeSourcesModel.move(increment);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void tableChanged(TableModelEvent e) {
            updateEnabledState();
        }
    }

    class ActivateSourcesAction extends AbstractAction implements ListSelectionListener {
        public ActivateSourcesAction() {
            putValue(SHORT_DESCRIPTION, getStr(I18nString.ACTIVATE_TOOLTIP));
            putValue(SMALL_ICON, ImageProvider.get("preferences", "activate-right"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(lstAvailableSources.getSelectedIndices().length > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            List<ExtendedSourceEntry> sources = availableSourcesModel.getSelected();
            activeSourcesModel.addExtendedSourceEntries(sources);
        }
    }

    class ResetAction extends AbstractAction {

        public ResetAction() {
            putValue(NAME, tr("Reset"));
            putValue(SHORT_DESCRIPTION, tr("Reset to default"));
            putValue(SMALL_ICON, ImageProvider.get("preferences", "reset"));
        }

        public void actionPerformed(ActionEvent e) {
            activeSourcesModel.setActiveSources(getDefault());
        }
    }

    class ReloadSourcesAction extends AbstractAction {
        private String url;
        public ReloadSourcesAction(String url) {
            putValue(NAME, tr("Reload"));
            putValue(SHORT_DESCRIPTION, tr(getStr(I18nString.RELOAD_ALL_AVAILABLE), url));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
            this.url = url;
        }

        public void actionPerformed(ActionEvent e) {
            MirroredInputStream.cleanup(url);
            reloadAvailableSources(url);
        }
    }

    protected static class IconPathTableModel extends AbstractTableModel {
        private ArrayList<String> data;
        private DefaultListSelectionModel selectionModel;

        public IconPathTableModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
            this.data = new ArrayList<String>();
        }

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return data == null ? 0 : data.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            updatePath(rowIndex, (String)aValue);
        }

        public void setIconPaths(Collection<String> paths) {
            data.clear();
            if (paths !=null) {
                data.addAll(paths);
            }
            sort();
            fireTableDataChanged();
        }

        public void addPath(String path) {
            if (path == null) return;
            data.add(path);
            sort();
            fireTableDataChanged();
            int idx = data.indexOf(path);
            if (idx >= 0) {
                selectionModel.setSelectionInterval(idx, idx);
            }
        }

        public void updatePath(int pos, String path) {
            if (path == null) return;
            if (pos < 0 || pos >= getRowCount()) return;
            data.set(pos, path);
            sort();
            fireTableDataChanged();
            int idx = data.indexOf(path);
            if (idx >= 0) {
                selectionModel.setSelectionInterval(idx, idx);
            }
        }

        public void removeSelected() {
            Iterator<String> it = data.iterator();
            int i=0;
            while(it.hasNext()) {
                it.next();
                if (selectionModel.isSelectedIndex(i)) {
                    it.remove();
                }
                i++;
            }
            fireTableDataChanged();
            selectionModel.clearSelection();
        }

        protected void sort() {
            Collections.sort(
                    data,
                    new Comparator<String>() {
                        public int compare(String o1, String o2) {
                            if (o1.equals("") && o2.equals(""))
                                return 0;
                            if (o1.equals("")) return 1;
                            if (o2.equals("")) return -1;
                            return o1.compareTo(o2);
                        }
                    }
            );
        }

        public List<String> getIconPaths() {
            return new ArrayList<String>(data);
        }
    }

    class NewIconPathAction extends AbstractAction {
        public NewIconPathAction() {
            putValue(NAME, tr("New"));
            putValue(SHORT_DESCRIPTION, tr("Add a new icon path"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
        }

        public void actionPerformed(ActionEvent e) {
            iconPathsModel.addPath("");
            tblIconPaths.editCellAt(iconPathsModel.getRowCount() -1,0);
        }
    }

    class RemoveIconPathAction extends AbstractAction implements ListSelectionListener {
        public RemoveIconPathAction() {
            putValue(NAME, tr("Remove"));
            putValue(SHORT_DESCRIPTION, tr("Remove the selected icon paths"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(tblIconPaths.getSelectedRowCount() > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            iconPathsModel.removeSelected();
        }
    }

    class EditIconPathAction extends AbstractAction implements ListSelectionListener {
        public EditIconPathAction() {
            putValue(NAME, tr("Edit"));
            putValue(SHORT_DESCRIPTION, tr("Edit the selected icon path"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(tblIconPaths.getSelectedRowCount() == 1);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            int row = tblIconPaths.getSelectedRow();
            tblIconPaths.editCellAt(row, 0);
        }
    }

    static class SourceEntryListCellRenderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            String s = value.toString();
            setText(s);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setFont(getFont().deriveFont(Font.PLAIN));
            setOpaque(true);
            setToolTipText(((ExtendedSourceEntry) value).getTooltip());
            return this;
        }
    }

    class SourceLoader extends PleaseWaitRunnable {
        private String url;
        private BufferedReader reader;
        private boolean canceled;

        public SourceLoader(String url) {
            super(tr(getStr(I18nString.LOADING_SOURCES_FROM), url));
            this.url = url;
        }

        @Override
        protected void cancel() {
            canceled = true;
            if (reader!= null) {
                try {
                    reader.close();
                } catch(IOException e) {
                    // ignore
                }
            }
        }

        @Override
        protected void finish() {}

        protected void warn(Exception e) {
            String emsg = e.getMessage() != null ? e.getMessage() : e.toString();
            emsg = emsg.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            String msg = tr(getStr(I18nString.FAILED_TO_LOAD_SOURCES_FROM), url, emsg);

            HelpAwareOptionPane.showOptionDialog(
                    Main.parent,
                    msg,
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE,
                    ht(getStr(I18nString.FAILED_TO_LOAD_SOURCES_FROM_HELP_TOPIC))
            );
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            LinkedList<ExtendedSourceEntry> sources = new LinkedList<ExtendedSourceEntry>();
            String lang = LanguageInfo.getLanguageCodeXML();
            try {
                sources.addAll(getDefault());
                MirroredInputStream stream = new MirroredInputStream(url);
                InputStreamReader r;
                try {
                    r = new InputStreamReader(stream, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    r = new InputStreamReader(stream);
                }
                reader = new BufferedReader(r);

                String line;
                ExtendedSourceEntry last = null;

                while ((line = reader.readLine()) != null && !canceled) {
                    if (line.trim().equals("")) {
                        continue; // skip empty lines
                    }
                    if (line.startsWith("\t")) {
                        Matcher m = Pattern.compile("^\t([^:]+): *(.+)$").matcher(line);
                        if (! m.matches()) {
                            System.err.println(tr(getStr(I18nString.ILLEGAL_FORMAT_OF_ENTRY), url, line));
                            continue;
                        }
                        if (last != null) {
                            String key = m.group(1);
                            String value = m.group(2);
                            if ("author".equals(key) && last.author == null) {
                                last.author = value;
                            } else if ("version".equals(key)) {
                                last.version = value;
                            } else if ("link".equals(key) && last.link == null) {
                                last.link = value;
                            } else if ("description".equals(key) && last.description == null) {
                                last.description = value;
                            } else if ((lang + "shortdescription").equals(key) && last.title == null) {
                                last.title = value;
                            } else if ("shortdescription".equals(key) && last.title == null) {
                                last.title = value;
                            } else if ((lang + "title").equals(key) && last.title == null) {
                                last.title = value;
                            } else if ("title".equals(key) && last.title == null) {
                                last.title = value;
                            } else if ("name".equals(key) && last.name == null) {
                                last.name = value;
                            } else if ((lang + "author").equals(key)) {
                                last.author = value;
                            } else if ((lang + "link").equals(key)) {
                                last.link = value;
                            } else if ((lang + "description").equals(key)) {
                                last.description = value;
                            }
                        }
                    } else {
                        last = null;
                        Matcher m = Pattern.compile("^(.+);(.+)$").matcher(line);
                        if (m.matches()) {
                            sources.add(last = new ExtendedSourceEntry(m.group(1), m.group(2)));
                        } else {
                            System.err.println(tr(getStr(I18nString.ILLEGAL_FORMAT_OF_ENTRY), url, line));
                        }
                    }
                }
            } catch (Exception e) {
                if (canceled)
                    // ignore the exception and return
                    return;
                OsmTransferException ex = new OsmTransferException(e);
                ex.setUrl(url);
                warn(ex);
                return;
            }
            availableSourcesModel.setSources(sources);
        }
    }
    
    class SourceEntryTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            SourceEntry se = (SourceEntry) value;
            JLabel label = (JLabel)super.getTableCellRendererComponent(table,
                    fromSourceEntry(se), isSelected, hasFocus, row, column);
            return label;
        }

        private String fromSourceEntry(SourceEntry entry) {
            if (entry == null)
                return null;
            StringBuilder s = new StringBuilder("<html><b>");
            if (entry.title != null) {
                s.append(entry.title).append("</b> (");
            }
            s.append(entry.url);
            if (entry.title != null) {
                s.append(")");
            }
            s.append("</html>");
            return s.toString();
        }
    }

    class FileOrUrlCellEditor extends JPanel implements TableCellEditor {
        private JTextField tfFileName;
        private CopyOnWriteArrayList<CellEditorListener> listeners;
        private String value;
        private JFileChooser fileChooser;
        private boolean isFile;

        protected JFileChooser getFileChooser() {
            if (fileChooser == null) {
                this.fileChooser = new JFileChooser();
                if(!isFile) {
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }
            }
            return fileChooser;
        }

        /**
         * build the GUI
         */
        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(tfFileName = new JTextField(), gc);

            gc.gridx = 1;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 0.0;
            gc.weighty = 1.0;
            add(new JButton(new LaunchFileChooserAction()));

            tfFileName.addFocusListener(
                    new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            tfFileName.selectAll();
                        }
                    }
            );
        }

        public FileOrUrlCellEditor(boolean isFile) {
            this.isFile = isFile;
            listeners = new CopyOnWriteArrayList<CellEditorListener>();
            build();
        }

        public void addCellEditorListener(CellEditorListener l) {
            if (l != null) {
                listeners.addIfAbsent(l);
            }
        }

        protected void fireEditingCanceled() {
            for (CellEditorListener l: listeners) {
                l.editingCanceled(new ChangeEvent(this));
            }
        }

        protected void fireEditingStopped() {
            for (CellEditorListener l: listeners) {
                l.editingStopped(new ChangeEvent(this));
            }
        }

        public void cancelCellEditing() {
            fireEditingCanceled();
        }

        public Object getCellEditorValue() {
            return value;
        }

        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent)
                return ((MouseEvent)anEvent).getClickCount() >= 2;
                return true;
        }

        public void removeCellEditorListener(CellEditorListener l) {
            listeners.remove(l);
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        public boolean stopCellEditing() {
            value = tfFileName.getText();
            fireEditingStopped();
            return true;
        }

        public void setInitialValue(String initialValue) {
            this.value = initialValue;
            if (initialValue == null) {
                this.tfFileName.setText("");
            } else {
                this.tfFileName.setText(initialValue);
            }
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            setInitialValue((String)value);
            tfFileName.selectAll();
            return this;
        }

        class LaunchFileChooserAction extends AbstractAction {
            public LaunchFileChooserAction() {
                putValue(NAME, "...");
                putValue(SHORT_DESCRIPTION, tr("Launch a file chooser to select a file"));
            }

            protected void prepareFileChooser(String url, JFileChooser fc) {
                if (url == null || url.trim().length() == 0) return;
                URL sourceUrl = null;
                try {
                    sourceUrl = new URL(url);
                } catch(MalformedURLException e) {
                    File f = new File(url);
                    if (f.isFile()) {
                        f = f.getParentFile();
                    }
                    if (f != null) {
                        fc.setCurrentDirectory(f);
                    }
                    return;
                }
                if (sourceUrl.getProtocol().startsWith("file")) {
                    File f = new File(sourceUrl.getPath());
                    if (f.isFile()) {
                        f = f.getParentFile();
                    }
                    if (f != null) {
                        fc.setCurrentDirectory(f);
                    }
                }
            }

            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = getFileChooser();
                prepareFileChooser(tfFileName.getText(), fc);
                int ret = fc.showOpenDialog(JOptionPane.getFrameForComponent(SourceEditor.this));
                if (ret != JFileChooser.APPROVE_OPTION)
                    return;
                tfFileName.setText(fc.getSelectedFile().toString());
            }
        }
    }


    /**
     * Convert mappaint and preset source preferences from a simple list to
     * array with one line for each source entry.
     *
     * MapPaint:
     *
     *    Old format
     *      key: mappaint.style.sources
     *      value: list of "<name>=<url>" pairs. The "<name>=" part is optional.
     *          The style is always active.
     *      default: empty list
     *
     *      key: mappaint.style.enable-defaults
     *      value: if true, the default style "resource://data/elemstyles.xml" should
     *          be loaded.
     *      default: true
     *
     *    New format
     *      key: mappaint.style.sources-list
     *      value: each line is a list with entries: url, name, shortdescription, active
     *      default:
     *          One line: "resource://data/elemstyles.xml", "standard", tr("Internal Style"), true
     *
     * Tagging Preset:
     *
     *      the same, but "name" and "active" are not needed and omitted
     *
     */
    abstract public static class SourcePrefMigration {

        private final String oldPref;
        private final String oldPrefEnableDefaults;
        private final String pref;

        public SourcePrefMigration(String oldPref, String oldPrefEnableDefaults, String pref) {
            this.oldPref = oldPref;
            this.oldPrefEnableDefaults = oldPrefEnableDefaults;
            this.pref = pref;
        }

        abstract public Collection<ExtendedSourceEntry> getDefault();

        abstract public Collection<String> serialize(SourceEntry entry);

        abstract public SourceEntry deserialize(List<String> entryStr);

        public List<SourceEntry> get() {
            List<SourceEntry> entries = readNewFormatImpl();
            if (entries == null) {

                entries = readOldFormat();
                put(entries);
                return entries;
            }
            return entries;
        }

        public boolean put(Collection<? extends SourceEntry> entries) {
            boolean changed = false;
            if (entries.isEmpty()) {
                changed |= Main.pref.put(pref + "._empty_", true);
                changed |= Main.pref.putArray(pref, null);
            } else {
                Collection<Collection<String>> setting = new ArrayList<Collection<String>>();
                for (SourceEntry e : entries) {
                    setting.add(serialize(e));
                }
                changed |= Main.pref.put(pref + "._empty_", null);
                changed |= Main.pref.putArray(pref, setting);
            }
            return changed;
        }

        public List<SourceEntry> readOldFormat() {
            List<SourceEntry> result = new ArrayList<SourceEntry>();
            if (Main.pref.getBoolean(oldPrefEnableDefaults, true)) {
                result.addAll(getDefault());
            }

            List<String> lines = new LinkedList<String>(Main.pref.getCollection(oldPref));
            for (String line : lines) {
                String[] a = null;
                if (line.indexOf("=") >= 0) {
                    a = line.split("=", 2);
                } else {
                    a = new String[] { null, line };
                }
                result.add(new SourceEntry(a[1], a[0], null, true));
            }

            return result;
        }

        public Collection<? extends SourceEntry> readNewFormat() {
            List<SourceEntry> entries = readNewFormatImpl();
            if (entries == null) {
                return getDefault();
            }
            return entries;
        }

        private List<SourceEntry> readNewFormatImpl() {
            List<SourceEntry> entries = new ArrayList<SourceEntry>();
            Collection<Collection<String>> mappaintSrc = Main.pref.getArray(pref, null);
            if (mappaintSrc == null || mappaintSrc.isEmpty()) {
                if (Main.pref.getBoolean(pref + "._empty_", false)) {
                    return Collections.<SourceEntry>emptyList();
                }
                return null;
            }

            for (Collection<String> sourcePref : mappaintSrc) {
                SourceEntry e = deserialize(new ArrayList<String>(sourcePref));
                if (e != null) {
                    entries.add(e);
                }
            }
            return entries;
        }
    }

}
