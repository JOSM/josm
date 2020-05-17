// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * A {@link JPopupMenu} that can stay open on all platforms when containing {@code StayOpen*} items.
 * @since 15492
 */
public class StayOpenPopupMenu extends JPopupMenu {

    private static final String MOUSE_GRABBER_KEY = "javax.swing.plaf.basic.BasicPopupMenuUI.MouseGrabber";

    /**
     * Special mask for the UngrabEvent events, in addition to the public masks defined in AWTEvent.
     */
    private static final int GRAB_EVENT_MASK = 0x80000000;

    /**
     * Constructs a new {@code StayOpenPopupMenu}.
     */
    public StayOpenPopupMenu() {
    }

    /**
     * Constructs a new {@code StayOpenPopupMenu} with the specified title.
     * @param label  the string that a UI may use to display as a title for the popup menu.
     */
    public StayOpenPopupMenu(String label) {
        super(label);
    }

    @Override
    public void setVisible(boolean b) {
        // macOS triggers a spurious UngrabEvent that is catched by BasicPopupMenuUI.MouseGrabber
        // and makes the popup menu disappear. Probably related to https://bugs.openjdk.java.net/browse/JDK-8225698
        if (PlatformManager.isPlatformOsx()) {
            try {
                Class<?> appContextClass = Class.forName("sun.awt.AppContext");
                Field tableField = appContextClass.getDeclaredField("table");
                ReflectionUtils.setObjectsAccessible(tableField);
                Object mouseGrabber = ((Map<?, ?>) tableField.get(appContextClass.getMethod("getAppContext").invoke(appContextClass)))
                        .entrySet().stream()
                        .filter(e -> MOUSE_GRABBER_KEY.equals(Objects.toString(e.getKey())))
                        .map(Entry::getValue)
                        .findFirst().orElse(null);
                final ChangeListener changeListener = (ChangeListener) mouseGrabber;
                final AWTEventListener awtEventListener = (AWTEventListener) mouseGrabber;
                final MenuSelectionManager msm = MenuSelectionManager.defaultManager();
                final Toolkit tk = Toolkit.getDefaultToolkit();
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    if (b)
                        msm.removeChangeListener(changeListener);
                    else
                        msm.addChangeListener(changeListener);
                    tk.removeAWTEventListener(awtEventListener);
                    tk.addAWTEventListener(awtEventListener,
                            AWTEvent.MOUSE_EVENT_MASK |
                            AWTEvent.MOUSE_MOTION_EVENT_MASK |
                            AWTEvent.MOUSE_WHEEL_EVENT_MASK |
                            AWTEvent.WINDOW_EVENT_MASK | (b ? 0 : GRAB_EVENT_MASK));
                    return null;
                });
            } catch (ReflectiveOperationException | RuntimeException e) {
                Logging.error(e);
            }
        }
        super.setVisible(b);
    }
}
