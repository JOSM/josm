package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import javax.swing.table.AbstractTableModel;

import java.util.LinkedList;
import java.util.List;
import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchAction.Function;
import org.openstreetmap.josm.actions.search.SearchAction;

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
   public void filter(){
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

   private void loadPrefs(){
      Map<String,String> prefs = Main.pref.getAllPrefix("filters.filter");
      for (String value : prefs.values()) {
         Filter filter = new Filter(value);
         if(filter!=null)
            filters.add(filter);
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
      if(i >= filters.size())
         Main.pref.put("filters.filter." + i, null);
      else
         Main.pref.put("filters.filter." + i, filters.get(i).getPrefString());
   }

   public void addFilter(Filter f){
      filters.add(f);
      savePref(filters.size()-1);
      filter();
      fireTableRowsInserted(filters.size()-1, filters.size()-1);
   }

   public void moveDownFilter(int i){
      if(i >= filters.size()-1) return;
      filters.add(i+1, filters.remove(i));
      savePref(i);
      savePref(i+1);
      filter();
      fireTableRowsUpdated(i, i+1);
   }

   public void moveUpFilter(int i){
      if(i == 0) return;
      filters.add(i-1, filters.remove(i));
      savePref(i);
      savePref(i-1);
      filter();
      fireTableRowsUpdated(i-1, i);
   }

   public void removeFilter(int i){
      filters.remove(i);
      savePrefs();
      filter();
      fireTableRowsDeleted(i, i);
   }

   public void setFilter(int i, Filter f){
      filters.set(i, f);
      savePref(i);
      filter();
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

   public String getColumnName(int column){
      String[] names = { /* translators notes must be in front */
          /* column header: enable filter */             trc("filter","E"),
          /* column header: hide filter */               tr("H"),
          /* column header: filter text */               tr("Text"),
          /* column header: apply filter for children */ tr("C"),
          /* column header: inverted filter */           tr("I"),
          /* column header: filter mode */               tr("M")
      };
      return names[column];
   }

   public Class getColumnClass(int column){
      Class[] classes = { Boolean.class, Boolean.class, String.class, Boolean.class, Boolean.class, String.class };
      return classes[column];
   }

   public boolean isCellEnabled(int row, int column){
      if(!filters.get(row).enable && column!=0) return false;
      return true;
   }

   public boolean isCellEditable(int row, int column){
      if(!filters.get(row).enable && column!=0) return false;
      if(column < 5)return true;
      return false;
   }

   public void setValueAt(Object aValue, int row, int column){
      Filter f = filters.get(row);
      switch(column){
         case 0: f.enable = (Boolean)aValue;
                 savePref(row);
                 filter();
                 fireTableRowsUpdated(row, row);
                 break;
         case 1: f.hide = (Boolean)aValue;
                 savePref(row);
                 filter();
                 break;
         case 2: f.text = (String)aValue;
                 savePref(row);
                 break;
         case 3: f.applyForChildren = (Boolean)aValue;
                 savePref(row);
                 filter();
                 break;
         case 4: f.inverted = (Boolean)aValue;
                 savePref(row);
                 filter();
                 break;
      }
      if(column!=0)fireTableCellUpdated(row, column);
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
                    case replace:      /* filter mode: replace */      return tr("R");
                    case add:          /* filter mode: add */          return tr("A");
                    case remove:       /* filter mode: remove */       return tr("D");
                    case in_selection: /* filter mode: in selection */ return tr("F");
                 }
      }
      return null;
   }
}
