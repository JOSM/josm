// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

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
}
