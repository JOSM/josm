// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

public interface StackableProgress {
    public void setChildProgress(double value);
    public void setSubTaskName(String value);
}
