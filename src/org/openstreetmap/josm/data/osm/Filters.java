package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.tools.Property;

/**
 *
 * @author Petr_Dlouhý
 */
public class Filters extends AbstractTableModel {

    // number of primitives that are disabled but not hidden
    public int disabledCount;
    // number of primitives that are disabled and hidden
    public int disabledAndHiddenCount;

    public Filters() {
        loadPrefs();
    }

    private List<Filter> filters = new LinkedList<Filter>();

    /**
     * Apply the filters to the primitives of the data set.
     *
     * There are certain rules to ensure that a way is not displayed "naked"
     * without its nodes (1) and on the other hand to avoid hiding a way but
     * leaving its nodes visible as a cloud of points (2).
     *
     * In normal (non-inverted) mode only problem (2) is relevant.
     * Untagged child nodes of filtered ways that are not used by other
     * unfiltered ways are filtered as well.
     *
     * If a filter applies explicitly to a node, (2) is ignored and it
     * is filtered in any case.
     *
     * In inverted mode usually only problem (1) is relevant.
     * If the inverted filter applies explicitly to a node, this no longer
     * means it is filtered in any case:
     * E.g. the filter [searchtext="highway=footway", inverted=true] displays
     * the footways only. But that does not mean, the nodes of the footway
     * (which do not have the highway tag) should be filtered as well.
     *
     * So first the Filter is applied for ways and relations. Then to nodes
     * (but hides them only if they are not used by any unfiltered way).
     */
    public void executeFilters(){
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds == null)
            return;

        final Collection<OsmPrimitive> all = ds.allNonDeletedCompletePrimitives();
        // temporary set to collect the primitives returned by the search engine
        final Collection<OsmPrimitive> collect = new HashSet<OsmPrimitive>();

        // an auxiliary property to collect the results of the search engine
        class CollectProperty implements Property<OsmPrimitive,Boolean> {
            boolean collectValue;
            boolean hidden;

            /**
             * Depending on the parameters, there are 4 different instances
             * of this class.
             *
             * @param collectValue
             *          If true: collect only those primitives that are added
             *              by the search engine.
             *          If false: Collect only those primitives that are removed
             *              by the search engine.
             * @param hidden Whether the property refers to primitives that
             *          are disabled and hidden or to primitives
             *          that are disabled only.
             */
            public CollectProperty(boolean collectValue, boolean hidden) {
                this.collectValue = collectValue;
                this.hidden = hidden;
            }

            public Boolean get(OsmPrimitive osm) {
                if (hidden)
                    return osm.isDisabledAndHidden();
                else
                    return osm.isDisabled();
            }

            public void set(OsmPrimitive osm, Boolean value) {
                if (collectValue == value.booleanValue()) {
                    collect.add(osm);
                }
            }
        }

        clearFilterFlags();

        for (Filter flt : filters){
            if (flt.enable) {
                collect.clear();
                // Decide, whether primitives are collected that are added to the current
                // selection or those that are removed from the current selection
                boolean collectValue = flt.mode == SearchAction.SearchMode.replace || flt.mode == SearchAction.SearchMode.add;
                Property<OsmPrimitive,Boolean> collectProp = new CollectProperty(collectValue, flt.hiding);

                SearchAction.getSelection(flt, all, collectProp);

                switch (flt.mode) {
                    case replace:
                        for (OsmPrimitive osm : all) {
                            osm.unsetDisabledState();
                        }
                    case add:
                        if (!flt.inverted) {
                            for (OsmPrimitive osm : collect) {
                                osm.setDisabledState(flt.hiding);
                            }

                            // Find child nodes of hidden ways and add them to the hidden nodes
                            for (OsmPrimitive osm : collect) {
                                if (osm instanceof Way) {
                                    nodes:
                                    for (Node n : ((Way)osm).getNodes()) {
                                        // if node is already disabled, there is nothing to do
                                        if (n.isDisabledAndHidden() || (!flt.hiding && n.isDisabled()))
                                            continue;

                                        // if the node is tagged, don't disable it
                                        if (n.isTagged())
                                            continue;

                                        // if the node has undisabled parent ways, don't disable it
                                        for (OsmPrimitive ref : n.getReferrers()) {
                                            if (ref instanceof Way) {
                                                if (!ref.isDisabled())
                                                    continue nodes;
                                                if (flt.hiding && !ref.isDisabledAndHidden())
                                                    continue nodes;
                                            }
                                        }
                                        n.setDisabledState(flt.hiding);
                                    }
                                }
                            }
                        } else { // inverted filter in add mode
                            // update flags, except for nodes
                            for (OsmPrimitive osm : collect) {
                                if (!(osm instanceof Node)) {
                                    osm.setDisabledState(flt.hiding);
                                }
                            }

                            // update flags for nodes
                            nodes:
                            for (OsmPrimitive osm : collect) {
                                if (osm instanceof Node) {
                                    // if node is already disabled, there is nothing to do
                                    if (osm.isDisabledAndHidden() || (!flt.hiding && osm.isDisabled()))
                                        continue;

                                    // if the node has undisabled parent ways, don't disable it
                                    for (OsmPrimitive ref : osm.getReferrers()) {
                                        if (ref instanceof Way) {
                                            if (!ref.isDisabled())
                                                continue nodes;
                                            if (flt.hiding && !ref.isDisabledAndHidden())
                                                continue nodes;
                                        }
                                    }
                                    osm.setDisabledState(flt.hiding);
                                }
                            }
                        }
                        break;
                    case remove:
                    case in_selection:
                        if (!flt.inverted) {
                            // make the described primitive undisabled again
                            for (OsmPrimitive osm : collect) {
                                osm.unsetDisabledState();
                            }

                            // Undisable the child nodes of undisabled ways
                            for (OsmPrimitive osm : collect) {
                                if (osm instanceof Way) {
                                    for (Node n : ((Way) osm).getNodes()) {
                                        n.unsetDisabledState();
                                    }
                                }
                            }
                        } else { // inverted filter in remove mode
                            // make the described primitive undisabled again
                            for (OsmPrimitive osm : collect) {
                                osm.unsetDisabledState();
                            }

                            // Undisable the child nodes of undisabled ways
                            for (OsmPrimitive osm : collect) {
                                if (osm instanceof Way) {
                                    for (Node n : ((Way) osm).getNodes()) {
                                        n.unsetDisabledState();
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }

        disabledCount = 0;
        disabledAndHiddenCount = 0;
        // collect disabled and selected the primitives
        final Collection<OsmPrimitive> deselect = new HashSet<OsmPrimitive>();
        for (OsmPrimitive osm : all) {
            if (osm.isDisabled()) {
                disabledCount++;
                if (osm.isSelected()) {
                    deselect.add(osm);
                }
                if (osm.isDisabledAndHidden()) {
                    disabledAndHiddenCount++;
                }
            }
        }
        disabledCount -= disabledAndHiddenCount;
        if (!deselect.isEmpty()) {
            ds.clearSelection(deselect);
        }

        Main.map.mapView.repaint();
    }

    public void clearFilterFlags() {
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds != null) {
            for (OsmPrimitive osm : ds.allPrimitives()) {
                osm.unsetDisabledState();
            }
        }
        disabledCount = 0;
        disabledAndHiddenCount = 0;
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
        return 5;
    }

    @Override
    public String getColumnName(int column){
        String[] names = { /* translators notes must be in front */
                /* column header: enable filter */             trc("filter","E"),
                /* column header: hide filter */               trc("filter", "H"),
                /* column header: filter text */               trc("filter", "Text"),
                /* column header: inverted filter */           trc("filter", "I"),
                /* column header: filter mode */               trc("filter", "M")
        };
        return names[column];
    }

    @Override
    public Class<?> getColumnClass(int column){
        Class<?>[] classes = { Boolean.class, Boolean.class, String.class, Boolean.class, String.class };
        return classes[column];
    }

    public boolean isCellEnabled(int row, int column){
        if(!filters.get(row).enable && column!=0) return false;
        return true;
    }

    @Override
    public boolean isCellEditable(int row, int column){
        if(!filters.get(row).enable && column!=0) return false;
        if(column < 4)return true;
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
            f.hiding = (Boolean)aValue;
            savePref(row);
            executeFilters();
            break;
        case 2:
            f.text = (String)aValue;
            savePref(row);
            break;
        case 3:
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
        case 1: return f.hiding;
        case 2: return f.text;
        case 3: return f.inverted;
        case 4:
            switch(f.mode){ /* translators notes must be in front */
            case replace:      /* filter mode: replace */      return trc("filter", "R");
            case add:          /* filter mode: add */          return trc("filter", "A");
            case remove:       /* filter mode: remove */       return trc("filter", "D");
            case in_selection: /* filter mode: in selection */ return trc("filter", "F");
            }
        }
        return null;
    }

    /**
     * On screen display label
     */
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

        if (disabledCount == 0 && disabledAndHiddenCount == 0)
            return;

        if (disabledAndHiddenCount != 0) {
            message += tr("<p><b>{0}</b> objects hidden", disabledAndHiddenCount);
        }

        if (disabledAndHiddenCount != 0 && disabledCount != 0) {
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
