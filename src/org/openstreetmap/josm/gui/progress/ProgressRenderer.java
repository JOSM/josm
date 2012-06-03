// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

/**
 * Swing components can implement this interface and use a {@link SwingRenderingProgressMonitor}
 * to render progress information.
 *
 */
public interface ProgressRenderer {
    void setTaskTitle(String taskTitle);
    void setCustomText(String message);
    void setIndeterminate(boolean indeterminate);
    void setMaximum(int maximum);
    void setValue(int value);
}
