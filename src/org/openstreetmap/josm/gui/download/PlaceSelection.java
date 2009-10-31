package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PlaceSelection implements DownloadSelection {

    private JTextField tfSearchExpression;
    private JButton btnSearch;
    private NamedResultTableModel model;
    private JTable tblSearchResults;
    private DownloadDialog parent;
  
    protected JPanel buildSearchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        
        // the label for the search field 
        //
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx  =1.0;
        gc.insets = new Insets(5, 5, 0, 5);
        panel.add(new JLabel(tr("Enter a place name to search for:")), gc);
        
        // the search expression field
        //
        tfSearchExpression = new JTextField();
        tfSearchExpression.setToolTipText(tr("Enter a place name to search for"));
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        panel.add(tfSearchExpression,  gc);

        // the search button
        //
        SearchAction searchAction = new SearchAction();
        btnSearch = new JButton(searchAction);
        tfSearchExpression.getDocument().addDocumentListener(searchAction);
        tfSearchExpression.addActionListener(searchAction);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        panel.add(btnSearch,  gc);
   
        return panel;   
    }

    /**
     * Adds a new tab to the download dialog in JOSM.
     *
     * This method is, for all intents and purposes, the constructor for this class.
     */
    public void addGui(final DownloadDialog gui) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(buildSearchPanel(), BorderLayout.NORTH);

        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        model = new NamedResultTableModel(selectionModel);
        tblSearchResults = new JTable(model, new NamedResultTableColumnModel());
        tblSearchResults.setSelectionModel(selectionModel);
        JScrollPane scrollPane = new JScrollPane(tblSearchResults);
        scrollPane.setPreferredSize(new Dimension(200,200));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        gui.addDownloadAreaSelector(panel, tr("Areas around Places"));

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
        public int zoom;
        public int osmId;
        public OsmPrimitiveType type;
        
        public Bounds getDownloadArea() {
            double size = 180.0 / Math.pow(2, zoom);
            Bounds b = new Bounds(
                    new LatLon(lat - size / 2, lon - size), 
                    new LatLon(lat + size / 2, lon+ size)
                    );
            return b;
        }
    }
    
    
    /**
     * A very primitive parser for the name finder's output.
     * Structure of xml described here:  http://wiki.openstreetmap.org/index.php/Name_finder
     *
     */
    private class NameFinderResultParser extends DefaultHandler {
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
                    currentResult.lat = Double.parseDouble(atts.getValue("lat"));
                    currentResult.lon = Double.parseDouble(atts.getValue("lon"));
                    currentResult.zoom = Integer.parseInt(atts.getValue("zoom"));
                    currentResult.osmId = Integer.parseInt(atts.getValue("id"));
                    currentResult.type = OsmPrimitiveType.from(atts.getValue("type"));
                    data.add(currentResult);
                } else if (qName.equals("description") && (depth == 3)) {
                    description = new StringBuffer();
                } else if (qName.equals("named") && (depth == 4)) {
                    // this is a "named" place in the nearest places list.
                    String info = atts.getValue("info");
                    if ("city".equals(info) || "town".equals(info) || "village".equals(info)) {
                        currentResult.nearestPlace = atts.getValue("name");
                    }
                }
            } catch (NumberFormatException x) {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(x.getMessage(), x);
            } catch (NullPointerException x) {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(tr("Null pointer exception, possibly some missing tags."), x);
            }
        }

        /**
         * Detect ending elements.
         */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (qName.equals("searchresults")) {
            } else if (qName.equals("description") && description != null) {
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
        
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled() || tfSearchExpression.getText().trim().length() == 0)
                return;
            NameQueryTask task = new NameQueryTask(tfSearchExpression.getText());
            Main.worker.submit(task);
        }
        
        protected void updateEnabledState() {
            setEnabled(tfSearchExpression.getText().trim().length() > 0);
        }

        public void changedUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        public void insertUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        public void removeUpdate(DocumentEvent e) {
            updateEnabledState();
        }
    }
    
    
    class NameQueryTask extends PleaseWaitRunnable {
        
        private String searchExpression;
        private HttpURLConnection connection;
        private List<SearchResult> data;
        private boolean canceled = false;
        private Exception lastException;
        
        public NameQueryTask(String searchExpression) {
            super(tr("Querying name server"),false /* don't ignore exceptions */);
            this.searchExpression = searchExpression;
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
            model.setData(this.data);            
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {            
            try {
                getProgressMonitor().indeterminateSubTask(tr("Querying name server ..."));
                    URL url = new URL("http://gazetteer.openstreetmap.org/namefinder/search.xml?find="
                            +java.net.URLEncoder.encode(searchExpression, "UTF-8"));
                    synchronized(this) {
                        connection = (HttpURLConnection)url.openConnection();
                    }
                    connection.setConnectTimeout(15000);
                    InputStream inputStream = connection.getInputStream();
                    InputSource inputSource = new InputSource(new InputStreamReader(inputStream, "UTF-8"));
                    NameFinderResultParser parser = new NameFinderResultParser();
                    SAXParserFactory.newInstance().newSAXParser().parse(inputSource, parser);
                    this.data = parser.getResult();
            } catch(Exception e) {
                if (canceled) {
                    // ignore exception 
                    return;
                }
                lastException = e;
            }
        }
    }
    
    class NamedResultTableModel extends DefaultTableModel {
        private ArrayList<SearchResult> data;
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
            if (selectionModel.getMinSelectionIndex() < 0) {
                return null;
            }
            return data.get(selectionModel.getMinSelectionIndex());
        }
    }
    
    class NamedResultTableColumnModel extends DefaultTableColumnModel {
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
            col = new TableColumn(2);
            col.setHeaderValue(tr("Near"));
            col.setResizable(true);
            col.setPreferredWidth(100);
            col.setCellRenderer(renderer);
            addColumn(col);
            

            // column 3 - Zoom
            col = new TableColumn(3);
            col.setHeaderValue(tr("Zoom"));
            col.setResizable(true);
            col.setPreferredWidth(50);
            col.setCellRenderer(renderer);
            addColumn(col);
        }

        public NamedResultTableColumnModel() {
            createColumns();
        }
    }
    
    class ListSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent lse) {
            SearchResult r = null;
            try {
                r = (SearchResult) model.getValueAt(lse.getFirstIndex(), 0);
            } catch (Exception x) {
                // Ignore
            }
            if (r != null) {
                parent.boundingBoxChanged(r.getDownloadArea(), PlaceSelection.this);
            }
        }
    }
    
    class NamedResultCellRenderer extends JLabel implements TableCellRenderer {
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
            StringBuffer ret = new StringBuffer();
            StringBuffer line = new StringBuffer();
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
                    line = new StringBuffer();
                }
            }
            ret.insert(0, "<html>");
            ret.append("</html>");
            return ret.toString();
        }
        
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
                setText(Integer.toString(sr.zoom));
                break;
            }
            setToolTipText(lineWrapDescription(sr.description));
            return this;
        }
    }
}
