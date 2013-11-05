// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
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

/**
 * Button that is usually used in toggle dialogs
 */
public class SideButton extends JButton implements Destroyable {
    private final static int iconHeight = 20;

    private PropertyChangeListener propertyChangeListener;

    public SideButton(Action action) {
        super(action);
        fixIcon(action);
        doStyle();
    }

    public SideButton(Action action, boolean usename) {
        super(action);
        if(!usename) {
            setText(null);
            fixIcon(action);
            doStyle();
        }
    }

    public SideButton(Action action, String imagename) {
        super(action);
        setIcon(makeIcon(imagename));
        doStyle();
    }

    private void fixIcon(Action action) {
        // need to listen for changes, so that putValue() that are called after the
        // SideButton is constructed get the proper icon size
        if (action != null) {
            action.addPropertyChangeListener(propertyChangeListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName() == javax.swing.Action.SMALL_ICON) {
                        fixIcon(null);
                    }
                }
            });
        }
        Icon i = getIcon();
        if (i instanceof ImageIcon && i.getIconHeight() != iconHeight) {
            setIcon(getScaledImage(((ImageIcon) i).getImage()));
        }
    }

    /** scales the given image proportionally so that the height is "iconHeight" **/
    private static ImageIcon getScaledImage(Image im) {
        int newWidth = im.getWidth(null) *  iconHeight / im.getHeight(null);
        return new ImageIcon(im.getScaledInstance(newWidth, iconHeight, Image.SCALE_SMOOTH));
    }

    public static ImageIcon makeIcon(String imagename) {
        Image im = ImageProvider.get("dialogs", imagename).getImage();
        return getScaledImage(im);
    }

    private void doStyle() {
        setLayout(new BorderLayout());
        setIconTextGap(2);
        setMargin(new Insets(-1,0,-1,0));
    }

    public void createArrow(ActionListener listener) {
        setMargin(new Insets(0,0,0,0));
        BasicArrowButton arrowButton = new BasicArrowButton(SwingConstants.SOUTH, null, null, Color.BLACK, null);
        arrowButton.setBorder(BorderFactory.createEmptyBorder());
        add(arrowButton, BorderLayout.EAST);
        arrowButton.addActionListener(listener);
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
