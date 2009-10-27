package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PlaceSelection implements DownloadSelection {

    private JTextField searchTerm = new JTextField();
    private JButton submitSearch = new JButton(tr("Search..."));
    private DefaultTableModel searchResults = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private JTable searchResultDisplay = new JTable(searchResults);
    private boolean updatingSelf;

    /**
     * Data storage for search results.
     */
    class SearchResult
    {
        public String name;
        public String type;
        public String nearestPlace;
        public String description;
        public double lat;
        public double lon;
        public int zoom;
    }

    /**
     * A very primitive parser for the name finder's output.
     * Structure of xml described here:  http://wiki.openstreetmap.org/index.php/Name_finder
     *
     */
    private class Parser extends DefaultHandler
    {
        private SearchResult currentResult = null;
        private StringBuffer description = null;
        private int depth = 0;
        /**
         * Detect starting elements.
         *
         */
        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
        {
            depth++;
            try
            {
                if (qName.equals("searchresults"))
                {
                    searchResults.setRowCount(0);
                }
                else if (qName.equals("named") && (depth == 2))
                {
                    currentResult = new PlaceSelection.SearchResult();
                    currentResult.name = atts.getValue("name");
                    currentResult.type = atts.getValue("info");
                    currentResult.lat = Double.parseDouble(atts.getValue("lat"));
                    currentResult.lon = Double.parseDouble(atts.getValue("lon"));
                    currentResult.zoom = Integer.parseInt(atts.getValue("zoom"));
                    searchResults.addRow(new Object[] { currentResult, currentResult, currentResult, currentResult });
                }
                else if (qName.equals("description") && (depth == 3))
                {
                    description = new StringBuffer();
                }
                else if (qName.equals("named") && (depth == 4))
                {
                    // this is a "named" place in the nearest places list.
                    String info = atts.getValue("info");
                    if ("city".equals(info) || "town".equals(info) || "village".equals(info)) {
                        currentResult.nearestPlace = atts.getValue("name");
                    }
                }
            }
            catch (NumberFormatException x)
            {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(x.getMessage(), x);
            }
            catch (NullPointerException x)
            {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(tr("Null pointer exception, possibly some missing tags."), x);
            }
        }
        /**
         * Detect ending elements.
         */
        @Override public void endElement(String namespaceURI, String localName, String qName) throws SAXException
        {

            if (qName.equals("searchresults"))
            {
            }
            else if (qName.equals("description") && description != null)
            {
                currentResult.description = description.toString();
                description = null;
            }
            depth--;

        }
        /**
         * Read characters for description.
         */
        @Override public void characters(char[] data, int start, int length) throws org.xml.sax.SAXException
        {
            if (description != null)
            {
                description.append(data, start, length);
            }
        }
    }

    /**
     * This queries David Earl's server. Needless to say, stuff should be configurable, and
     * error handling improved.
     */
    public void queryServer(final JComponent component)
    {
        final Cursor oldCursor = component.getCursor();

        // had to put this in a thread as it wouldn't update the cursor properly before.
        Runnable r = new Runnable() {
            public void run() {
                try
                {
                    String searchtext = searchTerm.getText();
                    if(searchtext.length()==0)
                    {
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("Please enter a search string"),
                                tr("Information"),
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                    else
                    {
                        component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        component.repaint();
                        URL url = new URL("http://gazetteer.openstreetmap.org/namefinder/search.xml?find="
                                +java.net.URLEncoder.encode(searchTerm.getText(), "UTF-8"));
                        HttpURLConnection activeConnection = (HttpURLConnection)url.openConnection();
                        //System.out.println("got return: "+activeConnection.getResponseCode());
                        activeConnection.setConnectTimeout(15000);
                        InputStream inputStream = activeConnection.getInputStream();
                        InputSource inputSource = new InputSource(new InputStreamReader(inputStream, "UTF-8"));
                        SAXParserFactory.newInstance().newSAXParser().parse(inputSource, new Parser());
                    }
                }
                catch (Exception x)
                {
                    x.printStackTrace();
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Cannot read place search results from server"),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                component.setCursor(oldCursor);
            }
        };
        new Thread(r).start();
    }

    /**
     * Adds a new tab to the download dialog in JOSM.
     *
     * This method is, for all intents and purposes, the constructor for this class.
     */
    public void addGui(final DownloadDialog gui) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        // this is manually tuned so that it looks nice on a GNOME
        // desktop - maybe needs some cross platform proofing.
        panel.add(new JLabel(tr("Enter a place name to search for:")), GBC.eol().insets(5, 5, 5, 5));
        panel.add(searchTerm, GBC.std().fill(GBC.HORIZONTAL).insets(5, 0, 5, 4));
        panel.add(submitSearch, GBC.eol().insets(5, 0, 5, 5));
        Dimension btnSize = submitSearch.getPreferredSize();
        btnSize.setSize(btnSize.width, btnSize.height * 0.8);
        submitSearch.setPreferredSize(btnSize);

        GBC c = GBC.std().fill().insets(5, 0, 5, 5);
        c.gridwidth = 2;
        JScrollPane scrollPane = new JScrollPane(searchResultDisplay);
        scrollPane.setPreferredSize(new Dimension(200,200));
        panel.add(scrollPane, c);
        gui.addDownloadAreaSelector(panel, tr("Places"));

        scrollPane.setPreferredSize(scrollPane.getPreferredSize());

        // when the button is clicked
        submitSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                queryServer(gui);
            }
        });

        searchTerm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                queryServer(gui);
            }
        });

        searchResults.addColumn(tr("name"));
        searchResults.addColumn(tr("type"));
        searchResults.addColumn(tr("near"));
        searchResults.addColumn(tr("zoom"));

        // TODO - this is probably not the coolest way to set relative sizes?
        searchResultDisplay.getColumn(tr("name")).setPreferredWidth(200);
        searchResultDisplay.getColumn(tr("type")).setPreferredWidth(100);
        searchResultDisplay.getColumn(tr("near")).setPreferredWidth(100);
        searchResultDisplay.getColumn(tr("zoom")).setPreferredWidth(50);
        searchResultDisplay.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // display search results in a table. for simplicity, the table contains
        // the same SearchResult object in each of the four columns, but it is rendered
        // differently depending on the column.
        searchResultDisplay.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    SearchResult sr = (SearchResult) value;
                    switch(column) {
                    case 0:
                        setText(sr.name);
                        break;
                    case 1:
                        setText(sr.type);
                        break;
                    case 2:
                        setText(sr.nearestPlace);
                        break;
                    case 3:
                        setText(Integer.toString(sr.zoom));
                        break;
                    }
                    setToolTipText("<html>"+((SearchResult)value).description+"</html>");
                }
                return this;
            }
        });

        // if item is selected in list, notify dialog
        searchResultDisplay.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent lse) {
                if (lse.getValueIsAdjusting()) return;
                SearchResult r = null;
                try
                {
                    r = (SearchResult) searchResults.getValueAt(lse.getFirstIndex(), 0);
                }
                catch (Exception x)
                {
                    // Ignore
                }
                if (r != null)
                {
                    double size = 180.0 / Math.pow(2, r.zoom);
                    Bounds b = new Bounds(
                        new LatLon(
                            r.lat - size / 2,
                            r.lat + size / 2
                         ),
                         new LatLon(
                            r.lon - size,
                            r.lon + size
                         )
                    );
                    updatingSelf = true;
                    gui.boundingBoxChanged(b,null);
                    updatingSelf = false;
                }
            }
        });

        // TODO - we'd like to finish the download dialog upon double-click but
        // don't know how to bypass the JOptionPane in which the whole thing is
        // displayed.
        searchResultDisplay.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    if (searchResultDisplay.getSelectionModel().getMinSelectionIndex() > -1) {
                        // add sensible action here.
                    }
                }
            }
        });

    }

    // if bounding box selected on other tab, de-select item
    public void boundingBoxChanged(DownloadDialog gui) {
        if (!updatingSelf) {
            searchResultDisplay.clearSelection();
        }
    }
}
