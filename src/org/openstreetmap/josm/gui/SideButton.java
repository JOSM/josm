// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageResource;
import org.openstreetmap.josm.tools.Logging;

/**
 * Button that is usually used in toggle dialogs.
 * @since 744
 */
public class SideButton extends JButton implements Destroyable {

    private transient PropertyChangeListener propertyChangeListener;

    /**
     * Constructs a new {@code SideButton}.
     * @param action action used to specify the new button
     * @since 744
     */
    public SideButton(Action action) {
        super(action);
        ImageResource icon = (ImageResource) action.getValue("ImageResource");
        if (icon != null) {
            setIcon(icon.getImageIconBounded(
                ImageProvider.ImageSizes.SIDEBUTTON.getImageDimension()));
        } else if (getIcon() != null) { /* TODO: remove when calling code is fixed, replace by exception */
            Logging.warn("Old style SideButton usage for action " + action);
            fixIcon(action);
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
     * Fix icon size
     * @param action the action
     * @deprecated This method is old style and will be removed together with the removal
     * of old constructor code
     */
    @Deprecated
    private void fixIcon(Action action) {
        // need to listen for changes, so that putValue() that are called after the
        // SideButton is constructed get the proper icon size
        if (action != null) {
            propertyChangeListener = evt -> {
                if (Action.SMALL_ICON.equals(evt.getPropertyName())) {
                    fixIcon(null);
                }
            };
            action.addPropertyChangeListener(propertyChangeListener);
        }
        int iconHeight = ImageProvider.ImageSizes.SIDEBUTTON.getImageDimension().height;
        Icon i = getIcon();
        if (i instanceof ImageIcon && i.getIconHeight() != iconHeight) {
            Image im = ((ImageIcon) i).getImage();
            int newWidth = im.getWidth(null) * iconHeight / im.getHeight(null);
            ImageIcon icon = new ImageIcon(im.getScaledInstance(newWidth, iconHeight, Image.SCALE_SMOOTH));
            setIcon(icon);
        }
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
