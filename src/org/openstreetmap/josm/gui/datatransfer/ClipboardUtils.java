// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a utility class that provides methods useful for general data transfer support.
 *
 * @author Michael Zangl
 * @since 10604
 */
public final class ClipboardUtils {
    private static final class DoNothingClipboardOwner implements ClipboardOwner {
        @Override
        public void lostOwnership(Clipboard clpbrd, Transferable t) {
            // Do nothing
        }
    }

    private static Clipboard clipboard;

    private ClipboardUtils() {
        // Hide default constructor for utility classes
    }

    /**
     * This method should be used from all of JOSM to access the clipboard.
     * <p>
     * It will default to the system clipboard except for cases where that clipboard is not accessible.
     * @return A clipboard.
     * @see #getClipboardContent()
     */
    public static synchronized Clipboard getClipboard() {
        // Might be unsupported in some more cases, we need a fake clipboard then.
        if (clipboard == null) {
            try {
                clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (HeadlessException | SecurityException e) {
                Logging.warn("Using fake clipboard.", e);
                clipboard = new Clipboard("fake");
            }
        }
        return clipboard;
    }

    /**
     * Gets the singleton instance of the system selection as a <code>Clipboard</code> object.
     * This allows an application to read and modify the current, system-wide selection.
     * @return the system selection as a <code>Clipboard</code>, or <code>null</code> if the native platform does not
     *         support a system selection <code>Clipboard</code> or if GraphicsEnvironment.isHeadless() returns true
     * @see Toolkit#getSystemSelection
     */
    public static Clipboard getSystemSelection() {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        } else {
            return Toolkit.getDefaultToolkit().getSystemSelection();
        }
    }

    /**
     * Gets the clipboard content as string.
     * @return the content if available, <code>null</code> otherwise.
     */
    public static String getClipboardStringContent() {
        try {
            Transferable t = getClipboardContent();
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) t.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Logging.error(ex);
        }
        return null;
    }

    /**
     * Extracts clipboard content as {@code Transferable} object. Using this method avoids some problems on some platforms.
     * @return The content or <code>null</code> if it is not available
     */
    public static synchronized Transferable getClipboardContent() {
        return getClipboardContent(getClipboard());
    }

    /**
     * Extracts clipboard content as {@code Transferable} object. Using this method avoids some problems on some platforms.
     * @param clipboard clipboard from which contents are retrieved
     * @return clipboard contents if available, {@code null} otherwise.
     */
    public static Transferable getClipboardContent(Clipboard clipboard) {
        Transferable t = null;
        for (int tries = 0; t == null && tries < 10; tries++) {
            try {
                t = clipboard.getContents(null);
            } catch (IllegalStateException e) {
                // Clipboard currently unavailable.
                // On some platforms, the system clipboard is unavailable while it is accessed by another application.
                Logging.trace("Clipboard unavailable.", e);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Logging.log(Logging.LEVEL_WARN, "InterruptedException in " + Utils.class.getSimpleName()
                            + " while getting clipboard content", ex);
                    Thread.currentThread().interrupt();
                }
            } catch (NullPointerException e) { // NOPMD
                // JDK-6322854: On Linux/X11, NPE can happen for unknown reasons, on all versions of Java
                Logging.error(e);
            }
        }
        return t;
    }

    /**
     * Copy the given string to the clipboard.
     * @param s The string to copy.
     * @return True if the  copy was successful
     */
    public static boolean copyString(String s) {
        return copy(new StringSelection(s));
    }

    /**
     * Copies the given transferable to the clipboard. Handles state problems that occur on some platforms.
     * @param transferable The transferable to copy.
     * @return True if the copy was successful
     */
    public static boolean copy(final Transferable transferable) {
        return GuiHelper.runInEDTAndWaitAndReturn(() -> {
            try {
                getClipboard().setContents(transferable, new DoNothingClipboardOwner());
                return Boolean.TRUE;
            } catch (IllegalStateException ex) {
                Logging.error(ex);
                return Boolean.FALSE;
            }
        });
    }

    /**
     * Returns a new {@link DataFlavor} for the given class and human-readable name.
     * @param c class
     * @param humanPresentableName the human-readable string used to identify this flavor
     * @return a new {@link DataFlavor} for the given class and human-readable name
     * @since 10801
     */
    public static DataFlavor newDataFlavor(Class<?> c, String humanPresentableName) {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + c.getName(),
                    humanPresentableName, c.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
