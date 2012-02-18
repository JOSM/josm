// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.tools.GBC;

public abstract class DefaultTabPreferenceSetting extends DefaultPreferenceSetting implements TabPreferenceSetting {

    private final String iconName;
    private final String description;
    private final String title;
    
    public DefaultTabPreferenceSetting() {
        this(null, null, null);
    }

    public DefaultTabPreferenceSetting(String iconName, String title, String description) {
        this(iconName, title, description, false);
    }

    public DefaultTabPreferenceSetting(String iconName, String title, String description, boolean isExpert) {
        super(isExpert);
        this.iconName = iconName;
        this.description = description;
        this.title = title;
    }

    @Override
    public String getIconName() {
        return iconName;
    }

    @Override
    public String getTooltip() {
        if (getDescription() != null) {
            return "<html>"+getDescription()+"</html>";
        } else {
            return null;
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getTitle() {
        return title;
    }
    
    protected final void createPreferenceTabWithScrollPane(PreferenceTabbedPane gui, JPanel panel) {
        GBC a = GBC.eol().insets(-5,0,0,0);
        a.anchor = GBC.EAST;
        
        JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        JPanel tab = gui.createPreferenceTab(this);
        tab.add(scrollPane, GBC.eol().fill(GBC.BOTH));
        tab.add(GBC.glue(0,10), a);
    }
}
