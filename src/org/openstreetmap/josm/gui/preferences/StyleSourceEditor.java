// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
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
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.xml.sax.SAXException;

public class StyleSourceEditor extends JPanel {
    private JTable tblActiveStyles;
    private ActiveStylesModel activeStylesModel;
    private JList lstAvailableStyles;
    private AvailableStylesListModel availableStylesModel;
    private JTable tblIconPaths = null;
    private IconPathTableModel iconPathsModel;
    private String pref;
    private String iconpref;
    private boolean stylesInitiallyLoaded;
    private String availableStylesUrl;

    /**
     *
     * @param stylesPreferencesKey the preferences key with the list of active style sources (filenames and URLs)
     * @param iconsPreferenceKey the preference key with the list of icon sources (can be null)
     * @param availableStylesUrl the URL to the list of available style sources
     */
    public StyleSourceEditor(String stylesPreferencesKey, String iconsPreferenceKey, final String availableStylesUrl) {

        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        tblActiveStyles = new JTable(activeStylesModel = new ActiveStylesModel(selectionModel));
        tblActiveStyles.putClientProperty("terminateEditOnFocusLost", true);
        tblActiveStyles.setSelectionModel(selectionModel);
        tblActiveStyles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblActiveStyles.setTableHeader(null);
        tblActiveStyles.getColumnModel().getColumn(0).setCellEditor(new FileOrUrlCellEditor(true));
        tblActiveStyles.setRowHeight(20);
        activeStylesModel.setActiveStyles(Main.pref.getCollection(stylesPreferencesKey, null));

        selectionModel = new DefaultListSelectionModel();
        lstAvailableStyles = new JList(availableStylesModel =new AvailableStylesListModel(selectionModel));
        lstAvailableStyles.setSelectionModel(selectionModel);
        lstAvailableStyles.setCellRenderer(new StyleSourceCellRenderer());
        //availableStylesModel.setStyleSources(reloadAvailableStyles(availableStylesUrl));
        this.availableStylesUrl = availableStylesUrl;

        this.pref = stylesPreferencesKey;
        this.iconpref = iconsPreferenceKey;

        JButton iconadd = null;
        JButton iconedit = null;
        JButton icondelete = null;

        if (iconsPreferenceKey != null) {
            selectionModel = new DefaultListSelectionModel();
            tblIconPaths = new JTable(iconPathsModel = new IconPathTableModel(selectionModel));
            tblIconPaths.setSelectionModel(selectionModel);
            tblIconPaths.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tblIconPaths.setTableHeader(null);
            tblIconPaths.getColumnModel().getColumn(0).setCellEditor(new FileOrUrlCellEditor(false));
            tblIconPaths.setRowHeight(20);
            iconPathsModel.setIconPaths(Main.pref.getCollection(iconsPreferenceKey, null));

            iconadd = new JButton(new NewIconPathAction());

            EditIconPathAction editIconPathAction = new EditIconPathAction();
            tblIconPaths.getSelectionModel().addListSelectionListener(editIconPathAction);
            iconedit = new JButton(editIconPathAction);

            RemoveIconPathAction removeIconPathAction = new RemoveIconPathAction();
            tblIconPaths.getSelectionModel().addListSelectionListener(removeIconPathAction);
            icondelete = new JButton(removeIconPathAction);
            tblIconPaths.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "delete");
            tblIconPaths.getActionMap().put("delete", removeIconPathAction);
        }

        JButton add = new JButton(new NewActiveStyleAction());

        EditActiveStyleAction editActiveStyleAction = new EditActiveStyleAction();
        tblActiveStyles.getSelectionModel().addListSelectionListener(editActiveStyleAction);
        JButton edit = new JButton(editActiveStyleAction);

        RemoveActiveStylesAction removeActiveStylesAction = new RemoveActiveStylesAction();
        tblActiveStyles.getSelectionModel().addListSelectionListener(removeActiveStylesAction);
        tblActiveStyles.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "delete");
        tblActiveStyles.getActionMap().put("delete", removeActiveStylesAction);
        JButton delete = new JButton(removeActiveStylesAction);

        ActivateStylesAction activateStylesAction = new ActivateStylesAction();
        lstAvailableStyles.addListSelectionListener(activateStylesAction);
        JButton copy = new JButton(activateStylesAction);

        JButton update = new JButton(new ReloadStylesAction(availableStylesUrl));

        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setLayout(new GridBagLayout());
        add(new JLabel(tr("Active styles")), GBC.eol().insets(5, 5, 5, 0));
        JScrollPane sp;
        add(sp = new JScrollPane(tblActiveStyles), GBC.eol().insets(5, 0, 5, 0).fill(GBC.BOTH));
        sp.setColumnHeaderView(null);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        add(buttonPanel, GBC.eol().insets(5, 0, 5, 5).fill(GBC.HORIZONTAL));
        buttonPanel.add(add, GBC.std().insets(0, 5, 0, 0));
        buttonPanel.add(edit, GBC.std().insets(5, 5, 5, 0));
        buttonPanel.add(delete, GBC.std().insets(0, 5, 5, 0));
        buttonPanel.add(copy, GBC.std().insets(0, 5, 5, 0));
        add(new JLabel(tr("Available styles (from {0})", availableStylesUrl)), GBC.eol().insets(5, 5, 5, 0));
        add(new JScrollPane(lstAvailableStyles), GBC.eol().insets(5, 0, 5, 0).fill(GBC.BOTH));
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        add(buttonPanel, GBC.eol().insets(5, 0, 5, 5).fill(GBC.HORIZONTAL));
        buttonPanel.add(update, GBC.std().insets(0, 5, 0, 0));
        if (tblIconPaths != null) {
            add(new JLabel(tr("Icon paths")), GBC.eol().insets(5, -5, 5, 0));
            add(sp = new JScrollPane(tblIconPaths), GBC.eol().insets(5, 0, 5, 0).fill(GBC.BOTH));
            sp.setColumnHeaderView(null);
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            add(buttonPanel, GBC.eol().insets(5, 0, 5, 5).fill(GBC.HORIZONTAL));
            buttonPanel.add(iconadd);
            buttonPanel.add(iconedit);
            buttonPanel.add(icondelete);
        }
    }

    public boolean hasActiveStylesChanged() {
        return !activeStylesModel.getStyles().equals(Main.pref.getCollection(pref, Collections.<String>emptyList()));
    }

    public Collection<String> getActiveStyles() {
        return activeStylesModel.getStyles();
    }

    public void removeSource(String source) {
        activeStylesModel.remove(source);
    }

    public boolean finish() {
        boolean changed = false;
        List<String> activeStyles = activeStylesModel.getStyles();

        if (activeStyles.size() > 0) {
            if (Main.pref.putCollection(pref, activeStyles)) {
                changed = true;
            }
        } else if (Main.pref.putCollection(pref, null)) {
            changed = true;
        }

        if (tblIconPaths != null) {
            List<String> iconPaths = iconPathsModel.getIconPaths();

            if (!iconPaths.isEmpty()) {
                if (Main.pref.putCollection(iconpref, iconPaths)) {
                    changed = true;
                }
            } else if (Main.pref.putCollection(iconpref, null)) {
                changed = true;
            }
        }
        return changed;
    }

    protected void reloadAvailableStyles(String url) {
        Main.worker.submit(new StyleSourceLoader(url));
    }

    public void initiallyLoadAvailableStyles() {
        if (!stylesInitiallyLoaded) {
            reloadAvailableStyles(this.availableStylesUrl);
        }
        stylesInitiallyLoaded = true;
    }

    static class AvailableStylesListModel extends DefaultListModel {
        private ArrayList<StyleSourceInfo> data;
        private DefaultListSelectionModel selectionModel;

        public AvailableStylesListModel(DefaultListSelectionModel selectionModel) {
            data = new ArrayList<StyleSourceInfo>();
            this.selectionModel = selectionModel;
        }

        public void setStyleSources(List<StyleSourceInfo> styleSources) {
            data.clear();
            if (styleSources != null) {
                data.addAll(styleSources);
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
            Iterator<StyleSourceInfo> it = data.iterator();
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

        public List<StyleSourceInfo> getSelected() {
            ArrayList<StyleSourceInfo> ret = new ArrayList<StyleSourceInfo>();
            for(int i=0; i<data.size();i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    ret.add(data.get(i));
                }
            }
            return ret;
        }
    }

    static class ActiveStylesModel extends AbstractTableModel {
        private ArrayList<String> data;
        private DefaultListSelectionModel selectionModel;

        public ActiveStylesModel(DefaultListSelectionModel selectionModel) {
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
            updateStyle(rowIndex, (String)aValue);
        }

        public void setActiveStyles(Collection<String> styles) {
            data.clear();
            if (styles !=null) {
                data.addAll(styles);
            }
            sort();
            fireTableDataChanged();
        }

        public void addStyle(String style) {
            if (style == null) return;
            data.add(style);
            sort();
            fireTableDataChanged();
            int idx = data.indexOf(style);
            if (idx >= 0) {
                selectionModel.setSelectionInterval(idx, idx);
            }
        }

        public void updateStyle(int pos, String style) {
            if (style == null) return;
            if (pos < 0 || pos >= getRowCount()) return;
            data.set(pos, style);
            sort();
            fireTableDataChanged();
            int idx = data.indexOf(style);
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
        }

        public void remove(String source) {
            data.remove(source);
            fireTableDataChanged();
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

        public void addStylesFromSources(List<StyleSourceInfo> sources) {
            if (sources == null) return;
            for (StyleSourceInfo info: sources) {
                data.add(info.url);
            }
            sort();
            fireTableDataChanged();
            selectionModel.clearSelection();
            for (StyleSourceInfo info: sources) {
                int pos = data.indexOf(info.url);
                if (pos >=0) {
                    selectionModel.addSelectionInterval(pos, pos);
                }
            }
        }

        public List<String> getStyles() {
            return new ArrayList<String>(data);
        }

        public String getStyle(int pos) {
            return data.get(pos);
        }
    }

    public static class StyleSourceInfo {
        String version;
        String name;
        String url;
        String author;
        String link;
        String description;
        String shortdescription;

        public StyleSourceInfo(String name, String url) {
            this.name = name;
            this.url = url;
            version = author = link = description = shortdescription = null;
        }

        public String getName() {
            return shortdescription == null ? name : shortdescription;
        }

        public String getTooltip() {
            String s = tr("Short Description: {0}", getName()) + "<br>" + tr("URL: {0}", url);
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
            return getName() + " (" + url + ")";
        }
    }

    class NewActiveStyleAction extends AbstractAction {
        public NewActiveStyleAction() {
            putValue(NAME, tr("New"));
            putValue(SHORT_DESCRIPTION, tr("Add a filename or an URL of an active style"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
        }

        public void actionPerformed(ActionEvent e) {
            activeStylesModel.addStyle("");
            tblActiveStyles.requestFocusInWindow();
            tblActiveStyles.editCellAt(activeStylesModel.getRowCount()-1, 0);
        }
    }

    class RemoveActiveStylesAction extends AbstractAction implements ListSelectionListener {

        public RemoveActiveStylesAction() {
            putValue(NAME, tr("Remove"));
            putValue(SHORT_DESCRIPTION, tr("Remove the selected styles from the list of active styles"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(tblActiveStyles.getSelectedRowCount() > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            activeStylesModel.removeSelected();
        }
    }

    class EditActiveStyleAction extends AbstractAction implements ListSelectionListener {
        public EditActiveStyleAction() {
            putValue(NAME, tr("Edit"));
            putValue(SHORT_DESCRIPTION, tr("Edit the filename or URL for the selected active style"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(tblActiveStyles.getSelectedRowCount() == 1);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            int pos = tblActiveStyles.getSelectedRow();
            tblActiveStyles.editCellAt(pos, 0);
        }
    }

    class ActivateStylesAction extends AbstractAction implements ListSelectionListener {
        public ActivateStylesAction() {
            putValue(NAME, tr("Activate"));
            putValue(SHORT_DESCRIPTION, tr("Add the selected available styles to the list of active styles"));
            putValue(SMALL_ICON, ImageProvider.get("preferences", "activatestyle"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(lstAvailableStyles.getSelectedIndices().length > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            List<StyleSourceInfo> styleSources = availableStylesModel.getSelected();
            activeStylesModel.addStylesFromSources(styleSources);
        }
    }

    class ReloadStylesAction extends AbstractAction {
        private String url;
        public ReloadStylesAction(String url) {
            putValue(NAME, tr("Reload"));
            putValue(SHORT_DESCRIPTION, tr("Reloads the list of available styles from ''{0}''", url));
            putValue(SMALL_ICON, ImageProvider.get("download"));
            this.url = url;
        }

        public void actionPerformed(ActionEvent e) {
            MirroredInputStream.cleanup(url);
            reloadAvailableStyles(url);
        }
    }

    static class IconPathTableModel extends AbstractTableModel {
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

        public void setIconPaths(Collection<String> styles) {
            data.clear();
            if (styles !=null) {
                data.addAll(styles);
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

    static class StyleSourceCellRenderer extends JLabel implements ListCellRenderer {
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
            setOpaque(true);
            setToolTipText(((StyleSourceInfo) value).getTooltip());
            return this;
        }
    }

    class StyleSourceLoader extends PleaseWaitRunnable {
        private String url;
        private BufferedReader reader;
        private boolean canceled;

        public StyleSourceLoader(String url) {
            super(tr("Loading style sources from ''{0}''", url));
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
            String msg = tr("<html>Failed to load the list of style sources from<br>"
                    + "''{0}''.<br>"
                    + "<br>"
                    + "Details (untranslated):<br>{1}</html>",
                    url, emsg
            );

            HelpAwareOptionPane.showOptionDialog(
                    Main.parent,
                    msg,
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE,
                    ht("Preferences/Styles#FailedToLoadStyleSources")
            );
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            LinkedList<StyleSourceInfo> styles = new LinkedList<StyleSourceInfo>();
            String lang = LanguageInfo.getLanguageCodeXML();
            try {
                MirroredInputStream stream = new MirroredInputStream(url);
                InputStreamReader r;
                try {
                    r = new InputStreamReader(stream, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    r = new InputStreamReader(stream);
                }
                BufferedReader reader = new BufferedReader(r);

                String line;
                StyleSourceInfo last = null;

                while ((line = reader.readLine()) != null && !canceled) {
                    if (line.trim().equals("")) {
                        continue; // skip empty lines
                    }
                    if (line.startsWith("\t")) {
                        Matcher m = Pattern.compile("^\t([^:]+): *(.+)$").matcher(line);
                        if (! m.matches()) {
                            System.err.println(tr("Warning: illegal format of entry in style list ''{0}''. Got ''{1}''", url, line));
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
                            } else if ("shortdescription".equals(key) && last.shortdescription == null) {
                                last.shortdescription = value;
                            } else if ((lang + "author").equals(key)) {
                                last.author = value;
                            } else if ((lang + "link").equals(key)) {
                                last.link = value;
                            } else if ((lang + "description").equals(key)) {
                                last.description = value;
                            } else if ((lang + "shortdescription").equals(key)) {
                                last.shortdescription = value;
                            }
                        }
                    } else {
                        last = null;
                        Matcher m = Pattern.compile("^(.+);(.+)$").matcher(line);
                        if (m.matches()) {
                            styles.add(last = new StyleSourceInfo(m.group(1), m.group(2)));
                        } else {
                            System.err.println(tr("Warning: illegal format of entry in style list ''{0}''. Got ''{1}''", url, line));
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
            availableStylesModel.setStyleSources(styles);
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
                int ret = fc.showOpenDialog(JOptionPane.getFrameForComponent(StyleSourceEditor.this));
                if (ret != JFileChooser.APPROVE_OPTION)
                    return;
                tfFileName.setText(fc.getSelectedFile().toString());
            }
        }
    }

}
