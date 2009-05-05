package org.openstreetmap.josm.gui.historycombobox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StringUtils {
    public static List<String> stringToList(String string, String delim) {
        List<String> list = new ArrayList<String>();
        if(string != null && delim != null) {
            String[] s = string.split(delim);
            for (String str : s) {
                list.add(str);
            }
        }
        return list;
    }
    
    public static String listToString(List<String> list, String delim) {
        if(list != null && list.size() > 0) {
            Iterator<String> iter = list.iterator();
            StringBuilder sb = new StringBuilder(iter.next());
            while(iter.hasNext()) {
                sb.append(delim).append(iter.next());
            }
            return sb.toString();
        }
        return "";
    }
}
