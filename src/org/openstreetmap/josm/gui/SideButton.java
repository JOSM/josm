// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageResource;

/**
 * Button that is usually used in toggle dialogs.
 * @since 744
 */
public class SideButton extends JButton implements Destroyable {

    private transient PropertyChangeListener propertyChangeListener;

    /**
     * Constructs a new {@code SideButton}.
     * @param action action used to specify the new button
     * an icon must be provided with {@link ImageResource#attachImageIcon(AbstractAction, boolean)}
     * @throws IllegalArgumentException if no icon provided
     * @since 744
     */
    public SideButton(Action action) {
        super(action);
        ImageResource icon = (ImageResource) action.getValue("ImageResource");
        if (icon != null) {
            setIcon(icon.getImageIconBounded(
                ImageProvider.ImageSizes.SIDEBUTTON.getImageDimension()));
        } else {
            throw new IllegalArgumentException("No icon provided");
        }
        doStyle();
    }

    /**
     * Constructs a new {@code SideButton}.
     * @param action action used to specify the new button
     * @param usename use action name
     * @since 2710
     */
    public SideButton(Action action, boolean usename) {
        this(action);
        if (!usename) {
            setText(null);
        }
    }

    /**
     * Constructs a new {@code SideButton}.
     * @param action action used to specify the new button
     * @param imagename image name in "dialogs" directory
     * @since 2747
     */
    public SideButton(Action action, String imagename) {
        super(action);
        setIcon(ImageProvider.get("dialogs", imagename, ImageProvider.ImageSizes.SIDEBUTTON));
        doStyle();
    }

    /**
     * Do the style settings for the side button layout
     */
    private void doStyle() {
        setLayout(new BorderLayout());
        setIconTextGap(2);
        setMargin(new Insets(0, 0, 0, 0));
    }

    /**
     * Create the arrow for opening a drop-down menu
     * @param listener listener to use for button actions (e.g. pressing)
     * @return the created button
     * @since 9668
     */
    public BasicArrowButton createArrow(ActionListener listener) {
        setMargin(new Insets(0, 0, 0, 0));
        BasicArrowButton arrowButton = new BasicArrowButton(SwingConstants.SOUTH, null, null, Color.BLACK, null);
        arrowButton.setBorder(BorderFactory.createEmptyBorder());
        add(arrowButton, BorderLayout.EAST);
        arrowButton.addActionListener(listener);
        return arrowButton;
    }

    @Override
    public void destroy() {
        Action action = getAction();
        if (action instanceof Destroyable) {
            ((Destroyable) action).destroy();
        }
        if (action != null) {
            if (propertyChangeListener != null) {
                action.removePropertyChangeListener(propertyChangeListener);
            }
            setAction(null);
        }
    }
}
