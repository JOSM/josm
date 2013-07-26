// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences.shortcut;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

public class ShortcutPreference extends DefaultTabPreferenceSetting {

    private String defaultFilter;

    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ShortcutPreference();
        }
    }

    private ShortcutPreference() {
        // icon source: http://www.iconfinder.net/index.php?q=key&page=icondetails&iconid=8553&size=128&q=key&s12=on&s16=on&s22=on&s32=on&s48=on&s64=on&s128=on
        // icon licence: GPL
        // icon designer: Paolino, http://www.paolinoland.it/
        // icon original filename: keyboard.png
        // icon original size: 128x128
        // modifications: icon was cropped, then resized
        super("shortcuts", tr("Keyboard Shortcuts"), tr("Changing keyboard shortcuts manually."));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel p = gui.createPreferenceTab(this);

        PrefJPanel prefpanel = new PrefJPanel(new scListModel());
        p.add(prefpanel, GBC.eol().fill(GBC.BOTH));
        if (defaultFilter!=null) prefpanel.filter(defaultFilter);
    }

    @Override
    public boolean ok() {
        return Shortcut.savePrefs();
    }

    public void setDefaultFilter(String substring) {
        defaultFilter = substring;
    }

    // Maybe move this to prefPanel? There's no need for it to be here.
    private static class scListModel extends AbstractTableModel {
        private String[] columnNames = new String[]{tr("Action"), tr("Shortcut")};
        private List<Shortcut> data;

        public scListModel() {
            data = Shortcut.listAll();
        }
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        @Override
        public int getRowCount() {
            return data.size();
        }
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }
        @Override
        public Object getValueAt(int row, int col) {
            return (col==0)?  data.get(row).getLongText() : data.get(row);
        }
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }
}
