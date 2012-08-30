// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.lang.reflect.InvocationTargetException;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * basic gui utils
 */
public class GuiHelper {
    /**
     * disable / enable a component and all its child components
     */
    public static void setEnabledRec(Container root, boolean enabled) {
        root.setEnabled(enabled);
        Component[] children = root.getComponents();
        for (Component child : children) {
            if(child instanceof Container) {
                setEnabledRec((Container) child, enabled);
            } else {
                child.setEnabled(enabled);
            }
        }
    }

    public static void runInEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public static void runInEDTAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * returns true if the user wants to cancel, false if they
     * want to continue
     */
    public static final boolean warnUser(String title, String content, ImageIcon baseActionIcon, String continueToolTip) {
        ExtendedDialog dlg = new ExtendedDialog(Main.parent,
                title, new String[] {tr("Cancel"), tr("Continue")});
        dlg.setContent(content);
        dlg.setButtonIcons(new Icon[] {
                ImageProvider.get("cancel"),
                ImageProvider.overlay(
                        ImageProvider.get("upload"),
                        new ImageIcon(ImageProvider.get("warning-small").getImage().getScaledInstance(10 , 10, Image.SCALE_SMOOTH)),
                        ImageProvider.OverlayPosition.SOUTHEAST)});
        dlg.setToolTipTexts(new String[] {
                tr("Cancel"),
                continueToolTip});
        dlg.setIcon(JOptionPane.WARNING_MESSAGE);
        dlg.setCancelButton(1);
        return dlg.showDialog().getValue() != 2;
    }
    
    /**
     * Replies the disabled (grayed) version of the specified image.
     * @param image The image to disable
     * @return The disabled (grayed) version of the specified image, brightened by 20%.
     * @since 5484
     */
    public static final Image getDisabledImage(Image image) {
        return Toolkit.getDefaultToolkit().createImage(
                new FilteredImageSource(image.getSource(), new GrayFilter(true, 20)));
    }

    /**
     * Replies the disabled (grayed) version of the specified icon.
     * @param icon The icon to disable
     * @return The disabled (grayed) version of the specified icon, brightened by 20%.
     * @since 5484
     */
    public static final ImageIcon getDisabledIcon(ImageIcon icon) {
        return new ImageIcon(getDisabledImage(icon.getImage()));
    }
}
