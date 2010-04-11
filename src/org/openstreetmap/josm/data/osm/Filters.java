package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchAction.Function;

/**
 *
 * @author Petr_Dlouhý
 */
public class Filters extends AbstractTableModel{

    public int disabledCount, hiddenCount;

    public Filters(){
        loadPrefs();
    }

    private List<Filter> filters = new LinkedList<Filter>();

    public void executeFilters(){
        Collection<OsmPrimitive> seld = new LinkedList<OsmPrimitive> ();
        Collection<OsmPrimitive> self = new LinkedList<OsmPrimitive> ();
        if(Main.main.getCurrentDataSet() == null)return;
        Main.main.getCurrentDataSet().setFiltered();
        Main.main.getCurrentDataSet().setDisabled();
        for (Filter flt : filters){
            if(flt.enable){
                SearchAction.getSelection(flt, seld, new Function(){
                    public Boolean isSomething(OsmPrimitive o){
                        return o.isDisabled();
                    }
                });
                if(flt.hide) {
                    SearchAction.getSelection(flt, self, new Function(){
                        public Boolean isSomething(OsmPrimitive o){
                            return o.isFiltered();
                        }
                    });
                }
            }
        }
        disabledCount = seld.size() - self.size();
        hiddenCount = self.size();
        Main.main.getCurrentDataSet().setFiltered(self);
        Main.main.getCurrentDataSet().setDisabled(seld);
        Main.map.mapView.repaint();
    }

    public void clearFilterFlags() {
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds != null) {
            ds.setFiltered();
            ds.setDisabled();
        }
        disabledCount = 0;
        hiddenCount = 0;
        Main.map.mapView.repaint();
    }


    private void loadPrefs() {
        Map<String,String> prefs = Main.pref.getAllPrefix("filters.filter");
        for (String value : prefs.values()) {
            filters.add(new Filter(value));
        }
    }

    private void savePrefs(){
        Map<String,String> prefs = Main.pref.getAllPrefix("filters.filter");
        for (String key : prefs.keySet()) {
            String[] sts = key.split("\\.");
            if (sts.length != 3)throw new Error("Incompatible filter preferences");
            Main.pref.put("filters.filter." + sts[2], null);
        }

        int i = 0;
        for (Filter flt : filters){
            Main.pref.put("filters.filter." + i++, flt.getPrefString());
        }
    }

    private void savePref(int i){
        if(i >= filters.size()) {
            Main.pref.put("filters.filter." + i, null);
        } else {
            Main.pref.put("filters.filter." + i, filters.get(i).getPrefString());
        }
    }

    public void addFilter(Filter f){
        filters.add(f);
        savePref(filters.size()-1);
        executeFilters();
        fireTableRowsInserted(filters.size()-1, filters.size()-1);
    }

    public void moveDownFilter(int i){
        if(i >= filters.size()-1) return;
        filters.add(i+1, filters.remove(i));
        savePref(i);
        savePref(i+1);
        executeFilters();
        fireTableRowsUpdated(i, i+1);
    }

    public void moveUpFilter(int i){
        if(i == 0) return;
        filters.add(i-1, filters.remove(i));
        savePref(i);
        savePref(i-1);
        executeFilters();
        fireTableRowsUpdated(i-1, i);
    }

    public void removeFilter(int i){
        filters.remove(i);
        savePrefs();
        executeFilters();
        fireTableRowsDeleted(i, i);
    }

    public void setFilter(int i, Filter f){
        filters.set(i, f);
        savePref(i);
        executeFilters();
        fireTableRowsUpdated(i, i);
    }

    public Filter getFilter(int i){
        return filters.get(i);
    }

    public int getRowCount(){
        return filters.size();
    }

    public int getColumnCount(){
        return 6;
    }

    @Override
    public String getColumnName(int column){
        String[] names = { /* translators notes must be in front */
                /* column header: enable filter */             trc("filter","E"),
                /* column header: hide filter */               trc("filter", "H"),
                /* column header: filter text */               trc("filter", "Text"),
                /* column header: apply filter for children */ trc("filter", "C"),
                /* column header: inverted filter */           trc("filter", "I"),
                /* column header: filter mode */               trc("filter", "M")
        };
        return names[column];
    }

    @Override
    public Class<?> getColumnClass(int column){
        Class<?>[] classes = { Boolean.class, Boolean.class, String.class, Boolean.class, Boolean.class, String.class };
        return classes[column];
    }

    public boolean isCellEnabled(int row, int column){
        if(!filters.get(row).enable && column!=0) return false;
        return true;
    }

    @Override
    public boolean isCellEditable(int row, int column){
        if(!filters.get(row).enable && column!=0) return false;
        if(column < 5)return true;
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column){
        Filter f = filters.get(row);
        switch(column){
        case 0:
            f.enable = (Boolean)aValue;
            savePref(row);
            executeFilters();
            fireTableRowsUpdated(row, row);
            break;
        case 1:
            f.hide = (Boolean)aValue;
            savePref(row);
            executeFilters();
            break;
        case 2:
            f.text = (String)aValue;
            savePref(row);
            break;
        case 3:
            f.applyForChildren = (Boolean)aValue;
            savePref(row);
            executeFilters();
            break;
        case 4:
            f.inverted = (Boolean)aValue;
            savePref(row);
            executeFilters();
            break;
        }
        if(column!=0) {
            fireTableCellUpdated(row, column);
        }
    }

    public Object getValueAt(int row, int column){
        Filter f = filters.get(row);
        switch(column){
        case 0: return f.enable;
        case 1: return f.hide;
        case 2: return f.text;
        case 3: return f.applyForChildren;
        case 4: return f.inverted;
        case 5:
            switch(f.mode){ /* translators notes must be in front */
            case replace:      /* filter mode: replace */      return trc("filter", "R");
            case add:          /* filter mode: add */          return trc("filter", "A");
            case remove:       /* filter mode: remove */       return trc("filter", "D");
            case in_selection: /* filter mode: in selection */ return trc("filter", "F");
            }
        }
        return null;
    }

    private static class OSDLabel extends JLabel {
        public OSDLabel(String text) {
            super(text);
            setOpaque(true);
            setForeground(Color.black);
            setBackground(new Color(0,0,0,0));
            setFont(getFont().deriveFont(Font.PLAIN));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        }

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(new Color(255, 255, 255, 140));
            g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 10, 10);
            super.paintComponent(g);
        }
    }

    private OSDLabel lblOSD = new OSDLabel("");

    public void drawOSDText(Graphics2D g) {
        String message = "<html>"+tr("<h2>Filter active</h2>");

        if (disabledCount == 0 && hiddenCount == 0)
            return;

        if (hiddenCount != 0) {
            message += tr("<p><b>{0}</b> objects hidden", hiddenCount);
        }

        if (hiddenCount != 0 && disabledCount != 0) {
            message += "<br>";
        }

        if (disabledCount != 0) {
            message += tr("<b>{0}</b> objects disabled", disabledCount);
        }

        message += tr("</p><p>Close the filter dialog to see all objects.<p></html>");

        lblOSD.setText(message);
        lblOSD.setSize(lblOSD.getPreferredSize());

        int dx = Main.map.mapView.getWidth() - lblOSD.getPreferredSize().width - 15;
        int dy = 15;
        g.translate(dx, dy);
        lblOSD.paintComponent(g);
        g.translate(-dx, -dy);
    }
}
