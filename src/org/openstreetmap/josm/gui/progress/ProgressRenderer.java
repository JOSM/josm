// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

/**
 * Swing components can implement this interface and use a {@code SwingRenderingProgressMonitor}
 * to render progress information.
 */
public interface ProgressRenderer {
    /**
     * Sets the title to display
     * @param taskTitle The title text
     */
    void setTaskTitle(String taskTitle);

    /**
     * Sets the custom text below the title
     * @param message The message
     */
    void setCustomText(String message);

    /**
     * Display the value as indeterminate value (unknown progress)
     * @param indeterminate <code>true</code> if the progress is unknown
     */
    void setIndeterminate(boolean indeterminate);

    /**
     * Sets the maximum possible progress
     * @param maximum The minimum value
     */
    void setMaximum(int maximum);

    /**
     * Sets the current progress
     * @param value The progress, in range 0...maximum
     * @see #setMaximum(int)
     */
    void setValue(int value);
}
