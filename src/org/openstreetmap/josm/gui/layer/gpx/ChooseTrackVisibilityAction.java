package org.openstreetmap.josm.gui.layer.gpx;

import org.openstreetmap.josm.gui.layer.GpxLayer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WindowGeometry;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

/**
 * allows the user to choose which of the downloaded tracks should be displayed.
 * they can be chosen from the gpx layer context menu.
 */
public class ChooseTrackVisibilityAction extends AbstractAction {
    private final GpxLayer layer;
 
    JTable table;
    JDateWithSlider dateFrom = new JDateWithSlider(tr("From"));
    JDateWithSlider dateTo = new JDateWithSlider(tr("To"));
    JCheckBox checkBox  = new JCheckBox(tr("No timestamp"));
    private boolean showNoTimestamp;
    
    public ChooseTrackVisibilityAction(final GpxLayer layer) {
        super(tr("Choose visible tracks"), ImageProvider.get("dialogs/filter"));
        this.layer = layer;
        putValue("help", ht("/Action/ChooseTrackVisibility"));
    }

    /**
     * gathers all available data for the tracks and returns them as array of arrays
     * in the expected column order  */
    private Object[][] buildTableContents() {
        Object[][] tracks = new Object[layer.data.tracks.size()][5];
        int i = 0;
        for (GpxTrack trk : layer.data.tracks) {
            Map<String, Object> attr = trk.getAttributes();
            String name = (String) (attr.containsKey("name") ? attr.get("name") : "");
            String desc = (String) (attr.containsKey("desc") ? attr.get("desc") : "");
            String time = GpxLayer.getTimespanForTrack(trk);
            String length = NavigatableComponent.getSystemOfMeasurement().getDistText(trk.length());
            String url = (String) (attr.containsKey("url") ? attr.get("url") : "");
            tracks[i] = new String[]{name, desc, time, length, url};
            i++;
        }
        return tracks;
    }

    /**
     * Builds an non-editable table whose 5th column will open a browser when double clicked.
     * The table will fill its parent. */
    private JTable buildTable(String[] headers, Object[][] content) {
        final JTable t = new JTable(content, headers) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    jc.setToolTipText((String) getValueAt(row, col));
                }
                return c;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int colIndex) {
                return false;
            }
        };
        // default column widths
        t.getColumnModel().getColumn(0).setPreferredWidth(220);
        t.getColumnModel().getColumn(1).setPreferredWidth(300);
        t.getColumnModel().getColumn(2).setPreferredWidth(200);
        t.getColumnModel().getColumn(3).setPreferredWidth(50);
        t.getColumnModel().getColumn(4).setPreferredWidth(100);
        // make the link clickable
        final MouseListener urlOpener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                JTable t = (JTable) e.getSource();
                int col = t.convertColumnIndexToModel(t.columnAtPoint(e.getPoint()));
                if (col != 4) {
                    return;
                }
                int row = t.rowAtPoint(e.getPoint());
                String url = (String) t.getValueAt(row, col);
                if (url == null || url.isEmpty()) {
                    return;
                }
                OpenBrowser.displayUrl(url);
            }
        };
        t.addMouseListener(urlOpener);
        t.setFillsViewportHeight(true);
        return t;
    }
    
    boolean noUpdates=false;
    
    private void filterTracksByDate() {
        layer.filterTracksByDate(dateFrom.getDate(), dateTo.getDate(), checkBox.isSelected());
    }
    
    /** selects all rows (=tracks) in the table that are currently visible */
    private void selectVisibleTracksInTable() {
        // don't select any tracks if the layer is not visible
        if (!layer.isVisible()) {
            return;
        }
        ListSelectionModel s = table.getSelectionModel();
        s.clearSelection();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            if (layer.trackVisibility[i]) {
                s.addSelectionInterval(i, i);
            }
        }
    }

    /** listens to selection changes in the table and redraws the map */
    private void listenToSelectionChanges() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!(e.getSource() instanceof ListSelectionModel)) {
                    return;
                }
                if (!noUpdates) updateVisibilityFromTable();
            }
        });
    }
    
    private void updateVisibilityFromTable() {
        ListSelectionModel s = (ListSelectionModel) table.getSelectionModel();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            layer.trackVisibility[i] = s.isSelectedIndex(i);
            System.out.printf("changed %d:=%s", i, ""+layer.trackVisibility[i]);
        }
        Main.map.mapView.preferenceChanged(null);
        Main.map.repaint(100);
    }
    
    private static final String PREF_DATE0 = "gpx.traces.showzerotimestamp";
    private static final String PREF_DATE1 = "gpx.traces.mintime";
    private static final String PREF_DATE2 = "gpx.traces.maxtime";
    
    @Override
    public void actionPerformed(ActionEvent arg0) {
        final JPanel msg = new JPanel(new GridBagLayout());
        
        final Date startTime, endTime;
        Date[] bounds = layer.getMinMaxTimeForAllTracks();
        
        startTime = (bounds==null) ? new GregorianCalendar(2000, 1, 1).getTime():bounds[0];
        endTime = (bounds==null) ? new Date() : bounds[2];

        long d1 = Main.pref.getLong(PREF_DATE1, 0);
        if (d1==0) d1=new GregorianCalendar(2000, 1, 1).getTime().getTime();
        long d2 = Main.pref.getLong(PREF_DATE2, 0);
        if (d2==0) d2=System.currentTimeMillis();
        
        dateFrom.setValue(new Date(d1)); 
        dateTo.setValue(new Date(d2)); 
        dateFrom.setRange(startTime, endTime); 
        dateTo.setRange(startTime, endTime); 
        checkBox.setSelected(Main.pref.getBoolean(PREF_DATE0, true));
        
        JButton selectDate = new JButton();
        msg.add(selectDate, GBC.std().grid(1,1).insets(0, 0, 20, 0));
        msg.add(checkBox, GBC.std().grid(2,1).insets(0, 0, 20, 0));
        msg.add(dateFrom, GBC.std().grid(3,1).fill(GBC.HORIZONTAL));
        msg.add(dateTo, GBC.eol().grid(4,1).fill(GBC.HORIZONTAL));
        msg.add(new JLabel(tr("<html>Select all tracks that you want to be displayed. You can drag select a " + "range of tracks or use CTRL+Click to select specific ones. The map is updated live in the " + "background. Open the URLs by double clicking them.</html>")), GBC.eol().fill(GBC.HORIZONTAL));
        // build table
        final boolean[] trackVisibilityBackup = layer.trackVisibility.clone();
        final String[] headers = {tr("Name"), tr("Description"), tr("Timespan"), tr("Length"), tr("URL")};
        table = buildTable(headers, buildTableContents());
        selectVisibleTracksInTable();
        listenToSelectionChanges();
        // make the table scrollable
        JScrollPane scrollPane = new JScrollPane(table);
        msg.add(scrollPane, GBC.eol().fill(GBC.BOTH));
        
        selectDate.setAction(new AbstractAction(tr("Select by date")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.pref.putLong(PREF_DATE1, dateFrom.getDate().getTime());
                Main.pref.putLong(PREF_DATE2, dateTo.getDate().getTime());
                Main.pref.put(PREF_DATE0, checkBox.isSelected());
                noUpdates = true;
                filterTracksByDate();
                selectVisibleTracksInTable();
                noUpdates = false;
                updateVisibilityFromTable();
            }
        });
        
        // build dialog
        ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Set track visibility for {0}", layer.getName()), new String[]{tr("Show all"), tr("Show selected only"), tr("Cancel")});
        ed.setButtonIcons(new String[]{"dialogs/layerlist/eye", "dialogs/filter", "cancel"});
        ed.setContent(msg, false);
        ed.setDefaultButton(2);
        ed.setCancelButton(3);
        ed.configureContextsensitiveHelp("/Action/ChooseTrackVisibility", true);
        ed.setRememberWindowGeometry(getClass().getName() + ".geometry", WindowGeometry.centerInWindow(Main.parent, new Dimension(1000, 500)));
        ed.showDialog();
        int v = ed.getValue();
        // cancel for unknown buttons and copy back original settings
        if (v != 1 && v != 2) {
            for (int i = 0; i < layer.trackVisibility.length; i++) {
                layer.trackVisibility[i] = trackVisibilityBackup[i];
            }
            Main.map.repaint();
            return;
        }
        // set visibility (1 = show all, 2 = filter). If no tracks are selected
        // set all of them visible and...
        ListSelectionModel s = table.getSelectionModel();
        final boolean all = v == 1 || s.isSelectionEmpty();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            layer.trackVisibility[i] = all || s.isSelectedIndex(i);
        }
        // ...sync with layer visibility instead to avoid having two ways to hide everything
        layer.setVisible(v == 1 || !s.isSelectionEmpty());
        Main.map.repaint();
    }
    
    
    public static class JDateWithSlider extends JPanel {
        private JSpinner spinner;
        private JSlider slider;
        private Date dateMin;
        private Date dateMax;
        private static final int MAX_SLIDER=300;

        public JDateWithSlider(String msg) {
            super(new GridBagLayout());
            spinner = new JSpinner( new SpinnerDateModel() );
            String pattern = ((SimpleDateFormat)DateFormat.getDateInstance()).toPattern();
            JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spinner,pattern);
            spinner.setEditor(timeEditor);
            slider = new JSlider(0,MAX_SLIDER);
            slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    spinner.setValue(dateFromInt(slider.getValue()));
                }
            });
            add(new JLabel(msg),GBC.std());
            add(spinner,GBC.std().insets(10,0,0,0));
            add(slider,GBC.eol().insets(10,0,0,0).fill(GBC.HORIZONTAL));
            
            dateMin = new Date(0); dateMax =new Date();
        }

        private Date dateFromInt(int value) {
            double k = 1.0*value/MAX_SLIDER;
            return new Date((long)(dateMax.getTime()*k+ dateMin.getTime()*(1-k)));
        }
        private int intFromDate(Date date) {
            return (int)(300.0*(date.getTime()-dateMin.getTime()) /
                    (dateMax.getTime()-dateMin.getTime()));
        }

        private void setRange(Date dateMin, Date dateMax) {
            this.dateMin = dateMin;
            this.dateMax = dateMax;
        }
        
        private void setValue(Date date) {
            spinner.setValue(date);
        }

        private Date getDate() {
            return (Date) spinner.getValue();
        }
    }
   
}
