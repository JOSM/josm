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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PlaceSelection implements DownloadSelection {
    private static final String HISTORY_KEY = "download.places.history";

    private HistoryComboBox cbSearchExpression;
    private JButton btnSearch;
    private NamedResultTableModel model;
    private NamedResultTableColumnModel columnmodel;
    private JTable tblSearchResults;
    private DownloadDialog parent;
    private final static Server[] servers = new Server[]{
        new Server("Nominatim","http://nominatim.openstreetmap.org/search?format=xml&q=",tr("Class Type"),tr("Bounds")),
        //new Server("Namefinder","http://gazetteer.openstreetmap.org/namefinder/search.xml?find=",tr("Near"),trc("placeselection", "Zoom"))
    };
    private final JosmComboBox server = new JosmComboBox(servers);

    private static class Server {
        public String name;
        public String url;
        public String thirdcol;
        public String fourthcol;
        @Override
        public String toString() {
            return name;
        }
        public Server(String n, String u, String t, String f) {
            name = n;
            url = u;
            thirdcol = t;
            fourthcol = f;
        }
    }

    protected JPanel buildSearchPanel() {
        JPanel lpanel = new JPanel();
        lpanel.setLayout(new GridLayout(2,2));
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        lpanel.add(new JLabel(tr("Choose the server for searching:")));
        lpanel.add(server);
        String s = Main.pref.get("namefinder.server", servers[0].name);
        for (int i = 0; i < servers.length; ++i) {
            if (servers[i].name.equals(s)) {
                server.setSelectedIndex(i);
            }
        }
        lpanel.add(new JLabel(tr("Enter a place name to search for:")));

        cbSearchExpression = new HistoryComboBox();
        cbSearchExpression.setToolTipText(tr("Enter a place name to search for"));
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        Collections.reverse(cmtHistory);
        cbSearchExpression.setPossibleItems(cmtHistory);
        lpanel.add(cbSearchExpression);

        panel.add(lpanel, GBC.std().fill(GBC.HORIZONTAL).insets(5, 5, 0, 5));
        SearchAction searchAction = new SearchAction();
        btnSearch = new JButton(searchAction);
        ((JTextField)cbSearchExpression.getEditor().getEditorComponent()).getDocument().addDocumentListener(searchAction);
        ((JTextField)cbSearchExpression.getEditor().getEditorComponent()).addActionListener(searchAction);

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
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(buildSearchPanel(), BorderLayout.NORTH);

        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        model = new NamedResultTableModel(selectionModel);
        columnmodel = new NamedResultTableColumnModel();
        tblSearchResults = new JTable(model, columnmodel);
        tblSearchResults.setSelectionModel(selectionModel);
        JScrollPane scrollPane = new JScrollPane(tblSearchResults);
        scrollPane.setPreferredSize(new Dimension(200,200));
        panel.add(scrollPane, BorderLayout.CENTER);

        gui.addDownloadAreaSelector(panel, tr("Areas around places"));

        scrollPane.setPreferredSize(scrollPane.getPreferredSize());
        tblSearchResults.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSearchResults.getSelectionModel().addListSelectionListener(new ListSelectionHandler());
        tblSearchResults.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    SearchResult sr = model.getSelectedSearchResult();
                    if (sr == null) return;
                    parent.startDownload(sr.getDownloadArea());
                }
            }
        });
        parent = gui;
    }

    @Override
    public void setDownloadArea(Bounds area) {
        tblSearchResults.clearSelection();
    }

    /**
     * Data storage for search results.
     */
    static private class SearchResult {
        public String name;
        public String info;
        public String nearestPlace;
        public String description;
        public double lat;
        public double lon;
        public int zoom = 0;
        public Bounds bounds = null;

        public Bounds getDownloadArea() {
            return bounds != null ? bounds : OsmUrlToBounds.positionToBounds(lat, lon, zoom);
        }
    }

    /**
     * A very primitive parser for the name finder's output.
     * Structure of xml described here:  http://wiki.openstreetmap.org/index.php/Name_finder
     *
     */
    private static class NameFinderResultParser extends DefaultHandler {
        private SearchResult currentResult = null;
        private StringBuffer description = null;
        private int depth = 0;
        private List<SearchResult> data = new LinkedList<SearchResult>();

        /**
         * Detect starting elements.
         *
         */
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
            depth++;
            try {
                if (qName.equals("searchresults")) {
                    // do nothing
                } else if (qName.equals("named") && (depth == 2)) {
                    currentResult = new PlaceSelection.SearchResult();
                    currentResult.name = atts.getValue("name");
                    currentResult.info = atts.getValue("info");
                    if(currentResult.info != null) {
                        currentResult.info = tr(currentResult.info);
                    }
                    currentResult.lat = Double.parseDouble(atts.getValue("lat"));
                    currentResult.lon = Double.parseDouble(atts.getValue("lon"));
                    currentResult.zoom = Integer.parseInt(atts.getValue("zoom"));
                    data.add(currentResult);
                } else if (qName.equals("description") && (depth == 3)) {
                    description = new StringBuffer();
                } else if (qName.equals("named") && (depth == 4)) {
                    // this is a "named" place in the nearest places list.
                    String info = atts.getValue("info");
                    if ("city".equals(info) || "town".equals(info) || "village".equals(info)) {
                        currentResult.nearestPlace = atts.getValue("name");
                    }
                } else if (qName.equals("place") && atts.getValue("lat") != null) {
                    currentResult = new PlaceSelection.SearchResult();
                    currentResult.name = atts.getValue("display_name");
                    currentResult.description = currentResult.name;
                    currentResult.info = atts.getValue("class");
                    if (currentResult.info != null) {
                        currentResult.info = tr(currentResult.info);
                    }
                    currentResult.nearestPlace = tr(atts.getValue("type"));
                    currentResult.lat = Double.parseDouble(atts.getValue("lat"));
                    currentResult.lon = Double.parseDouble(atts.getValue("lon"));
                    String[] bbox = atts.getValue("boundingbox").split(",");
                    currentResult.bounds = new Bounds(
                            Double.parseDouble(bbox[0]), Double.parseDouble(bbox[2]),
                            Double.parseDouble(bbox[1]), Double.parseDouble(bbox[3]));
                    data.add(currentResult);
                }
            } catch (NumberFormatException x) {
                Main.error(x); // SAXException does not chain correctly
                throw new SAXException(x.getMessage(), x);
            } catch (NullPointerException x) {
                Main.error(x); // SAXException does not chain correctly
                throw new SAXException(tr("Null pointer exception, possibly some missing tags."), x);
            }
        }

        /**
         * Detect ending elements.
         */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (qName.equals("description") && description != null) {
                currentResult.description = description.toString();
                description = null;
            }
            depth--;
        }

        /**
         * Read characters for description.
         */
        @Override
        public void characters(char[] data, int start, int length) throws org.xml.sax.SAXException {
            if (description != null) {
                description.append(data, start, length);
            }
        }

        public List<SearchResult> getResult() {
            return data;
        }
    }

    class SearchAction extends AbstractAction implements DocumentListener {

        public SearchAction() {
            putValue(NAME, tr("Search ..."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs","search"));
            putValue(SHORT_DESCRIPTION, tr("Click to start searching for places"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled() || cbSearchExpression.getText().trim().length() == 0)
                return;
            cbSearchExpression.addCurrentItemToHistory();
            Main.pref.putCollection(HISTORY_KEY, cbSearchExpression.getHistory());
            NameQueryTask task = new NameQueryTask(cbSearchExpression.getText());
            Main.worker.submit(task);
        }

        protected void updateEnabledState() {
            setEnabled(cbSearchExpression.getText().trim().length() > 0);
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

        private String searchExpression;
        private HttpURLConnection connection;
        private List<SearchResult> data;
        private boolean canceled = false;
        private Server useserver;
        private Exception lastException;

        public NameQueryTask(String searchExpression) {
            super(tr("Querying name server"),false /* don't ignore exceptions */);
            this.searchExpression = searchExpression;
            useserver = (Server)server.getSelectedItem();
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
            String urlString = useserver.url+java.net.URLEncoder.encode(searchExpression, "UTF-8");

            try {
                getProgressMonitor().indeterminateSubTask(tr("Querying name server ..."));
                URL url = new URL(urlString);
                synchronized(this) {
                    connection = Utils.openHttpConnection(url);
                }
                connection.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);
                InputStream inputStream = connection.getInputStream();
                InputSource inputSource = new InputSource(new InputStreamReader(inputStream, Utils.UTF_8));
                NameFinderResultParser parser = new NameFinderResultParser();
                SAXParserFactory.newInstance().newSAXParser().parse(inputSource, parser);
                this.data = parser.getResult();
            } catch(Exception e) {
                if (canceled)
                    // ignore exception
                    return;
                OsmTransferException ex = new OsmTransferException(e);
                ex.setUrl(urlString);
                lastException = ex;
            }
        }
    }

    static class NamedResultTableModel extends DefaultTableModel {
        private List<SearchResult> data;
        private ListSelectionModel selectionModel;

        public NamedResultTableModel(ListSelectionModel selectionModel) {
            data = new ArrayList<SearchResult>();
            this.selectionModel = selectionModel;
        }
        @Override
        public int getRowCount() {
            if (data == null) return 0;
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (data == null) return null;
            return data.get(row);
        }

        public void setData(List<SearchResult> data) {
            if (data == null) {
                this.data.clear();
            } else {
                this.data  =new ArrayList<SearchResult>(data);
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
        TableColumn col3 = null;
        TableColumn col4 = null;
        protected void createColumns() {
            TableColumn col = null;
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
            col3.setHeaderValue(servers[0].thirdcol);
            col3.setResizable(true);
            col3.setPreferredWidth(100);
            col3.setCellRenderer(renderer);
            addColumn(col3);

            // column 3 - Zoom
            col4 = new TableColumn(3);
            col4.setHeaderValue(servers[0].fourthcol);
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

        public NamedResultTableColumnModel() {
            createColumns();
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

        public NamedResultCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
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
            while(tok.hasMoreElements()) {
                String t = tok.nextToken();
                if (line.length() == 0) {
                    line.append(t);
                } else if (line.length() < 80) {
                    line.append(" ").append(t);
                } else {
                    line.append(" ").append(t).append("<br>");
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

            if (value == null) return this;
            SearchResult sr = (SearchResult) value;
            switch(column) {
            case 0:
                setText(sr.name);
                break;
            case 1:
                setText(sr.info);
                break;
            case 2:
                setText(sr.nearestPlace);
                break;
            case 3:
                if(sr.bounds != null) {
                    setText(sr.bounds.toShortString(new DecimalFormat("0.000")));
                } else {
                    setText(sr.zoom != 0 ? Integer.toString(sr.zoom) : tr("unknown"));
                }
                break;
            }
            setToolTipText(lineWrapDescription(sr.description));
            return this;
        }
    }
}
