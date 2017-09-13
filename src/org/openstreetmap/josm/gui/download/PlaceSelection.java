// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.io.NameFinder;
import org.openstreetmap.josm.io.NameFinder.SearchResult;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Place selector.
 * @since 1329
 */
public class PlaceSelection implements DownloadSelection {
    private static final String HISTORY_KEY = "download.places.history";

    private HistoryComboBox cbSearchExpression;
    private NamedResultTableModel model;
    private NamedResultTableColumnModel columnmodel;
    private JTable tblSearchResults;
    private DownloadDialog parent;
    private static final Server[] SERVERS = new Server[] {
        new Server("Nominatim", NameFinder.NOMINATIM_URL, tr("Class Type"), tr("Bounds"))
    };
    private final JosmComboBox<Server> server = new JosmComboBox<>(SERVERS);

    private static class Server {
        public final String name;
        public final String url;
        public final String thirdcol;
        public final String fourthcol;

        Server(String n, String u, String t, String f) {
            name = n;
            url = u;
            thirdcol = t;
            fourthcol = f;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    protected JPanel buildSearchPanel() {
        JPanel lpanel = new JPanel(new GridLayout(2, 2));
        JPanel panel = new JPanel(new GridBagLayout());

        lpanel.add(new JLabel(tr("Choose the server for searching:")));
        lpanel.add(server);
        String s = Main.pref.get("namefinder.server", SERVERS[0].name);
        for (int i = 0; i < SERVERS.length; ++i) {
            if (SERVERS[i].name.equals(s)) {
                server.setSelectedIndex(i);
            }
        }
        lpanel.add(new JLabel(tr("Enter a place name to search for:")));

        cbSearchExpression = new HistoryComboBox();
        cbSearchExpression.setToolTipText(tr("Enter a place name to search for"));
        List<String> cmtHistory = new LinkedList<>(Main.pref.getList(HISTORY_KEY, new LinkedList<String>()));
        Collections.reverse(cmtHistory);
        cbSearchExpression.setPossibleItems(cmtHistory);
        lpanel.add(cbSearchExpression);

        panel.add(lpanel, GBC.std().fill(GBC.HORIZONTAL).insets(5, 5, 0, 5));
        SearchAction searchAction = new SearchAction();
        JButton btnSearch = new JButton(searchAction);
        cbSearchExpression.getEditorComponent().getDocument().addDocumentListener(searchAction);
        cbSearchExpression.getEditorComponent().addActionListener(searchAction);

        panel.add(btnSearch, GBC.eol().insets(5, 5, 0, 5));

        return panel;
    }

    /**
     * Adds a new tab to the download dialog in JOSM.
     *
     * This method is, for all intents and purposes, the constructor for this class.
     */
    @Override
    public void addGui(final DownloadDialog gui) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buildSearchPanel(), BorderLayout.NORTH);

        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        model = new NamedResultTableModel(selectionModel);
        columnmodel = new NamedResultTableColumnModel();
        tblSearchResults = new JTable(model, columnmodel);
        tblSearchResults.setSelectionModel(selectionModel);
        JScrollPane scrollPane = new JScrollPane(tblSearchResults);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        if (gui != null)
            gui.addDownloadAreaSelector(panel, tr("Areas around places"));

        scrollPane.setPreferredSize(scrollPane.getPreferredSize());
        tblSearchResults.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSearchResults.getSelectionModel().addListSelectionListener(new ListSelectionHandler());
        tblSearchResults.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    SearchResult sr = model.getSelectedSearchResult();
                    if (sr != null) {
                        parent.startDownload(sr.getDownloadArea());
                    }
                }
            }
        });
        parent = gui;
    }

    @Override
    public void setDownloadArea(Bounds area) {
        tblSearchResults.clearSelection();
    }

    class SearchAction extends AbstractAction implements DocumentListener {

        SearchAction() {
            putValue(NAME, tr("Search ..."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "search"));
            putValue(SHORT_DESCRIPTION, tr("Click to start searching for places"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled() || cbSearchExpression.getText().trim().isEmpty())
                return;
            cbSearchExpression.addCurrentItemToHistory();
            Main.pref.putList(HISTORY_KEY, cbSearchExpression.getHistory());
            NameQueryTask task = new NameQueryTask(cbSearchExpression.getText());
            MainApplication.worker.submit(task);
        }

        protected final void updateEnabledState() {
            setEnabled(!cbSearchExpression.getText().trim().isEmpty());
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateEnabledState();
        }
    }

    class NameQueryTask extends PleaseWaitRunnable {

        private final String searchExpression;
        private HttpClient connection;
        private List<SearchResult> data;
        private boolean canceled;
        private final Server useserver;
        private Exception lastException;

        NameQueryTask(String searchExpression) {
            super(tr("Querying name server"), false /* don't ignore exceptions */);
            this.searchExpression = searchExpression;
            useserver = (Server) server.getSelectedItem();
            Main.pref.put("namefinder.server", useserver.name);
        }

        @Override
        protected void cancel() {
            this.canceled = true;
            synchronized (this) {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void finish() {
            if (canceled)
                return;
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
                return;
            }
            columnmodel.setHeadlines(useserver.thirdcol, useserver.fourthcol);
            model.setData(this.data);
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            String urlString = useserver.url+Utils.encodeUrl(searchExpression);

            try {
                getProgressMonitor().indeterminateSubTask(tr("Querying name server ..."));
                URL url = new URL(urlString);
                synchronized (this) {
                    connection = HttpClient.create(url);
                    connection.connect();
                }
                try (Reader reader = connection.getResponse().getContentReader()) {
                    data = NameFinder.parseSearchResults(reader);
                }
            } catch (SAXParseException e) {
                if (!canceled) {
                    // Nominatim sometimes returns garbage, see #5934, #10643
                    Logging.log(Logging.LEVEL_WARN, tr("Error occured with query ''{0}'': ''{1}''", urlString, e.getMessage()), e);
                    GuiHelper.runInEDTAndWait(() -> HelpAwareOptionPane.showOptionDialog(
                            Main.parent,
                            tr("Name server returned invalid data. Please try again."),
                            tr("Bad response"),
                            JOptionPane.WARNING_MESSAGE, null
                    ));
                }
            } catch (IOException | ParserConfigurationException e) {
                if (!canceled) {
                    OsmTransferException ex = new OsmTransferException(e);
                    ex.setUrl(urlString);
                    lastException = ex;
                }
            }
        }
    }

    static class NamedResultTableModel extends DefaultTableModel {
        private transient List<SearchResult> data;
        private final transient ListSelectionModel selectionModel;

        NamedResultTableModel(ListSelectionModel selectionModel) {
            data = new ArrayList<>();
            this.selectionModel = selectionModel;
        }

        @Override
        public int getRowCount() {
            return data != null ? data.size() : 0;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return data != null ? data.get(row) : null;
        }

        public void setData(List<SearchResult> data) {
            if (data == null) {
                this.data.clear();
            } else {
                this.data = new ArrayList<>(data);
            }
            fireTableDataChanged();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public SearchResult getSelectedSearchResult() {
            if (selectionModel.getMinSelectionIndex() < 0)
                return null;
            return data.get(selectionModel.getMinSelectionIndex());
        }
    }

    static class NamedResultTableColumnModel extends DefaultTableColumnModel {
        private TableColumn col3;
        private TableColumn col4;

        NamedResultTableColumnModel() {
            createColumns();
        }

        protected final void createColumns() {
            TableColumn col;
            NamedResultCellRenderer renderer = new NamedResultCellRenderer();

            // column 0 - Name
            col = new TableColumn(0);
            col.setHeaderValue(tr("Name"));
            col.setResizable(true);
            col.setPreferredWidth(200);
            col.setCellRenderer(renderer);
            addColumn(col);

            // column 1 - Version
            col = new TableColumn(1);
            col.setHeaderValue(tr("Type"));
            col.setResizable(true);
            col.setPreferredWidth(100);
            col.setCellRenderer(renderer);
            addColumn(col);

            // column 2 - Near
            col3 = new TableColumn(2);
            col3.setHeaderValue(SERVERS[0].thirdcol);
            col3.setResizable(true);
            col3.setPreferredWidth(100);
            col3.setCellRenderer(renderer);
            addColumn(col3);

            // column 3 - Zoom
            col4 = new TableColumn(3);
            col4.setHeaderValue(SERVERS[0].fourthcol);
            col4.setResizable(true);
            col4.setPreferredWidth(50);
            col4.setCellRenderer(renderer);
            addColumn(col4);
        }

        public void setHeadlines(String third, String fourth) {
            col3.setHeaderValue(third);
            col4.setHeaderValue(fourth);
            fireColumnMarginChanged();
        }
    }

    class ListSelectionHandler implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent lse) {
            SearchResult r = model.getSelectedSearchResult();
            if (r != null) {
                parent.boundingBoxChanged(r.getDownloadArea(), PlaceSelection.this);
            }
        }
    }

    static class NamedResultCellRenderer extends JLabel implements TableCellRenderer {

        /**
         * Constructs a new {@code NamedResultCellRenderer}.
         */
        NamedResultCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        protected void reset() {
            setText("");
            setIcon(null);
        }

        protected void renderColor(boolean selected) {
            if (selected) {
                setForeground(UIManager.getColor("Table.selectionForeground"));
                setBackground(UIManager.getColor("Table.selectionBackground"));
            } else {
                setForeground(UIManager.getColor("Table.foreground"));
                setBackground(UIManager.getColor("Table.background"));
            }
        }

        protected String lineWrapDescription(String description) {
            StringBuilder ret = new StringBuilder();
            StringBuilder line = new StringBuilder();
            StringTokenizer tok = new StringTokenizer(description, " ");
            while (tok.hasMoreElements()) {
                String t = tok.nextToken();
                if (line.length() == 0) {
                    line.append(t);
                } else if (line.length() < 80) {
                    line.append(' ').append(t);
                } else {
                    line.append(' ').append(t).append("<br>");
                    ret.append(line);
                    line = new StringBuilder();
                }
            }
            ret.insert(0, "<html>");
            ret.append("</html>");
            return ret.toString();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            reset();
            renderColor(isSelected);

            if (value == null)
                return this;
            SearchResult sr = (SearchResult) value;
            switch(column) {
            case 0:
                setText(sr.getName());
                break;
            case 1:
                setText(sr.getInfo());
                break;
            case 2:
                setText(sr.getNearestPlace());
                break;
            case 3:
                if (sr.getBounds() != null) {
                    setText(sr.getBounds().toShortString(new DecimalFormat("0.000")));
                } else {
                    setText(sr.getZoom() != 0 ? Integer.toString(sr.getZoom()) : tr("unknown"));
                }
                break;
            default: // Do nothing
            }
            setToolTipText(lineWrapDescription(sr.getDescription()));
            return this;
        }
    }
}
