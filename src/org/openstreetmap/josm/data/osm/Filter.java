package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;

/**
 *
 * @author Petr_Dlouhý
 */
public class Filter extends SearchSetting {
   private final String version = "1";
   public Boolean filtered = false;
   public Boolean disabled = true;
   public Boolean inverted = false;
   public Boolean applyForChildren = true;
   public Filter() {
       super("", SearchMode.add, false, false);
   }
   public Filter(String text, SearchMode mode, boolean caseSensitive, boolean regexSearch) {
       super(text, mode, caseSensitive, regexSearch);
   }

   public Filter(String prefText){
      super("", SearchMode.add, false, false);
      String[] prfs = prefText.split(";");
      if(prfs.length != 10 && !prfs[0].equals(version))
         throw new Error("Incompatible filter preferences");
      text = prfs[1];
      if(prfs[2].equals("replace")) mode = SearchMode.replace;
      if(prfs[2].equals("add")) mode = SearchMode.add;
      if(prfs[2].equals("remove")) mode = SearchMode.remove;
      if(prfs[2].equals("in_selection")) mode = SearchMode.in_selection;
      caseSensitive = Boolean.parseBoolean(prfs[3]);
      regexSearch = Boolean.parseBoolean(prfs[4]);
      filtered = Boolean.parseBoolean(prfs[6]);
      disabled = Boolean.parseBoolean(prfs[7]);
      inverted = Boolean.parseBoolean(prfs[8]);
      applyForChildren = Boolean.parseBoolean(prfs[9]);

   }  

   public String getPrefString(){
      return version + ";" + 
          text + ";" + mode + ";" + caseSensitive + ";" + regexSearch + ";" +
          "legacy" + ";" + filtered + ";" + disabled + ";" + 
          inverted + ";" + applyForChildren;
   }
}
