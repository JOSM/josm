// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_F10;
import static java.awt.event.KeyEvent.VK_F4;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_NUM_LOCK;
import static java.awt.event.KeyEvent.VK_PRINTSCREEN;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_TAB;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_V;
import static java.awt.event.KeyEvent.VK_X;
import static java.awt.event.KeyEvent.VK_Y;
import static java.awt.event.KeyEvent.VK_Z;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;

/**
  * {@code PlatformHook} implementation for Microsoft Windows systems.
  * @since 1023
  */
public class PlatformHookWindows extends PlatformHookUnixoid implements PlatformHook {
    
    @Override
    public void startupHook() {
        super.startupHook();
        // Invite users to install Oracle Java 7 if they are still with Sun/Oracle Java 6
        String vendor = System.getProperty("java.vendor");
        String version = System.getProperty("java.version");
        if ("Sun Microsystems Inc.".equals(vendor) && version != null && version.startsWith("1.6")) {
            askUpdateJava(version);
        }
    }

    @Override
    public void openUrl(String url) throws IOException {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
    }

    @Override
    public void initSystemShortcuts() {
        //Shortcut.registerSystemCut("system:menuexit", tr("reserved"), VK_Q, CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:duplicate", tr("reserved"), VK_D, CTRL_DOWN_MASK); // not really system, but to avoid odd results

        // Windows 7 shortcuts: http://windows.microsoft.com/en-US/windows7/Keyboard-shortcuts

        // Shortcuts with setAutomatic(): items with automatic shortcuts will not be added to the menu bar at all

        // Don't know why Ctrl-Alt-Del isn't even listed on official Microsoft support page
        Shortcut.registerSystemShortcut("system:reset", tr("reserved"), VK_DELETE, CTRL_DOWN_MASK | ALT_DOWN_MASK).setAutomatic();

        // Ease of Access keyboard shortcuts
        Shortcut.registerSystemShortcut("microsoft-reserved-01", tr("reserved"), VK_PRINTSCREEN, ALT_DOWN_MASK | SHIFT_DOWN_MASK).setAutomatic(); // Turn High Contrast on or off
        Shortcut.registerSystemShortcut("microsoft-reserved-02", tr("reserved"), VK_NUM_LOCK, ALT_DOWN_MASK | SHIFT_DOWN_MASK).setAutomatic(); // Turn Mouse Keys on or off
        //Shortcut.registerSystemCut("microsoft-reserved-03", tr("reserved"), VK_U, );// Open the Ease of Access Center (TODO: Windows-U, how to handle it in Java ?)

        // General keyboard shortcuts
        //Shortcut.registerSystemShortcut("system:help", tr("reserved"), VK_F1, 0);                            // Display Help
        Shortcut.registerSystemShortcut("system:copy", tr("reserved"), VK_C, CTRL_DOWN_MASK);                // Copy the selected item
        Shortcut.registerSystemShortcut("system:cut", tr("reserved"), VK_X, CTRL_DOWN_MASK);                 // Cut the selected item
        Shortcut.registerSystemShortcut("system:paste", tr("reserved"), VK_V, CTRL_DOWN_MASK);               // Paste the selected item
        Shortcut.registerSystemShortcut("system:undo", tr("reserved"), VK_Z, CTRL_DOWN_MASK);                // Undo an action
        Shortcut.registerSystemShortcut("system:redo", tr("reserved"), VK_Y, CTRL_DOWN_MASK);                // Redo an action
        //Shortcut.registerSystemCut("microsoft-reserved-10", tr("reserved"), VK_DELETE, 0);                  // Delete the selected item and move it to the Recycle Bin
        //Shortcut.registerSystemCut("microsoft-reserved-11", tr("reserved"), VK_DELETE, SHIFT_DOWN_MASK);    // Delete the selected item without moving it to the Recycle Bin first
        //Shortcut.registerSystemCut("system:rename", tr("reserved"), VK_F2, 0);                          // Rename the selected item
        Shortcut.registerSystemShortcut("system:movefocusright", tr("reserved"), VK_RIGHT, CTRL_DOWN_MASK);  // Move the cursor to the beginning of the next word
        Shortcut.registerSystemShortcut("system:movefocusleft", tr("reserved"), VK_LEFT, CTRL_DOWN_MASK);    // Move the cursor to the beginning of the previous word
        Shortcut.registerSystemShortcut("system:movefocusdown", tr("reserved"), VK_DOWN, CTRL_DOWN_MASK);    // Move the cursor to the beginning of the next paragraph
        Shortcut.registerSystemShortcut("system:movefocusup", tr("reserved"), VK_UP, CTRL_DOWN_MASK);        // Move the cursor to the beginning of the previous paragraph
        //Shortcut.registerSystemCut("microsoft-reserved-17", tr("reserved"), VK_RIGHT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK); // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-18", tr("reserved"), VK_LEFT, CTRL_DOWN_MASK | SHIFT_DOWN_MASK);  // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-19", tr("reserved"), VK_DOWN, CTRL_DOWN_MASK | SHIFT_DOWN_MASK);  // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-20", tr("reserved"), VK_UP, CTRL_DOWN_MASK | SHIFT_DOWN_MASK);    // Select a block of text
        //Shortcut.registerSystemCut("microsoft-reserved-21", tr("reserved"), VK_RIGHT, SHIFT_DOWN_MASK); // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-22", tr("reserved"), VK_LEFT, SHIFT_DOWN_MASK);  // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-23", tr("reserved"), VK_DOWN, SHIFT_DOWN_MASK);  // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-24", tr("reserved"), VK_UP, SHIFT_DOWN_MASK);    // Select more than one item in a window or on the desktop, or select text within a document
        //Shortcut.registerSystemCut("microsoft-reserved-25", tr("reserved"), VK_RIGHT+, CTRL_DOWN_MASK); // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-26", tr("reserved"), VK_LEFT+, CTRL_DOWN_MASK);  // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-27", tr("reserved"), VK_DOWN+, CTRL_DOWN_MASK);  // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-28", tr("reserved"), VK_UP+, CTRL_DOWN_MASK);    // Select multiple individual items in a window or on the desktop (TODO: ctrl+arrow+spacebar, how to handle it in Java ?)
        Shortcut.registerSystemShortcut("system:selectall", tr("reserved"), VK_A, CTRL_DOWN_MASK);           // Select all items in a document or window
        //Shortcut.registerSystemCut("system:search", tr("reserved"), VK_F3, 0);                          // Search for a file or folder
        Shortcut.registerSystemShortcut("microsoft-reserved-31", tr("reserved"), VK_ENTER, ALT_DOWN_MASK).setAutomatic();   // Display properties for the selected item
        Shortcut.registerSystemShortcut("system:exit", tr("reserved"), VK_F4, ALT_DOWN_MASK).setAutomatic(); // Close the active item, or exit the active program
        Shortcut.registerSystemShortcut("microsoft-reserved-33", tr("reserved"), VK_SPACE, ALT_DOWN_MASK).setAutomatic();   // Open the shortcut menu for the active window
        //Shortcut.registerSystemCut("microsoft-reserved-34", tr("reserved"), VK_F4, CTRL_DOWN_MASK);     // Close the active document (in programs that allow you to have multiple documents open simultaneously)
        Shortcut.registerSystemShortcut("microsoft-reserved-35", tr("reserved"), VK_TAB, ALT_DOWN_MASK).setAutomatic();     // Switch between open items
        Shortcut.registerSystemShortcut("microsoft-reserved-36", tr("reserved"), VK_TAB, CTRL_DOWN_MASK | ALT_DOWN_MASK).setAutomatic(); // Use the arrow keys to switch between open items
        //Shortcut.registerSystemCut("microsoft-reserved-37", tr("reserved"), VK_TAB, ); // Cycle through programs on the taskbar by using Aero Flip 3-D (TODO: Windows-Tab, how to handle it in Java ?)
        //Shortcut.registerSystemCut("microsoft-reserved-38", tr("reserved"), VK_TAB, CTRL_DOWN_MASK | ); // Use the arrow keys to cycle through programs on the taskbar by using Aero Flip 3-D (TODO: Ctrl-Windows-Tab, how to handle it in Java ?)
        Shortcut.registerSystemShortcut("microsoft-reserved-39", tr("reserved"), VK_ESCAPE, ALT_DOWN_MASK).setAutomatic();  // Cycle through items in the order in which they were opened
        //Shortcut.registerSystemCut("microsoft-reserved-40", tr("reserved"), VK_F6, 0);                  // Cycle through screen elements in a window or on the desktop
        //Shortcut.registerSystemCut("microsoft-reserved-41", tr("reserved"), VK_F4, 0);                  // Display the address bar list in Windows Explorer
        Shortcut.registerSystemShortcut("microsoft-reserved-42", tr("reserved"), VK_F10, SHIFT_DOWN_MASK);   // Display the shortcut menu for the selected item
        Shortcut.registerSystemShortcut("microsoft-reserved-43", tr("reserved"), VK_ESCAPE, CTRL_DOWN_MASK).setAutomatic(); // Open the Start menu
        //Shortcut.registerSystemShortcut("microsoft-reserved-44", tr("reserved"), VK_F10, 0);                 // Activate the menu bar in the active program
        //Shortcut.registerSystemCut("microsoft-reserved-45", tr("reserved"), VK_RIGHT, 0);               // Open the next menu to the right, or open a submenu
        //Shortcut.registerSystemCut("microsoft-reserved-46", tr("reserved"), VK_LEFT, 0);                // Open the next menu to the left, or close a submenu
        //Shortcut.registerSystemCut("microsoft-reserved-47", tr("reserved"), VK_F5, 0);                  // Refresh the active window
        //Shortcut.registerSystemCut("microsoft-reserved-48", tr("reserved"), VK_UP, ALT_DOWN_MASK);      // View the folder one level up in Windows Explorer
        //Shortcut.registerSystemCut("microsoft-reserved-49", tr("reserved"), VK_ESCAPE, 0);              // Cancel the current task
        Shortcut.registerSystemShortcut("microsoft-reserved-50", tr("reserved"), VK_ESCAPE, CTRL_DOWN_MASK | SHIFT_DOWN_MASK).setAutomatic(); // Open Task Manager
        Shortcut.registerSystemShortcut("microsoft-reserved-51", tr("reserved"), VK_SHIFT, ALT_DOWN_MASK).setAutomatic();   // Switch the input language when multiple input languages are enabled
        Shortcut.registerSystemShortcut("microsoft-reserved-52", tr("reserved"), VK_SHIFT, CTRL_DOWN_MASK).setAutomatic();  // Switch the keyboard layout when multiple keyboard layouts are enabled
        //Shortcut.registerSystemCut("microsoft-reserved-53", tr("reserved"), ); // Change the reading direction of text in right-to-left reading languages (TODO: unclear)
    }

    @Override
    public String getDefaultStyle() {
        return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    }

    @Override
    public boolean rename(File from, File to) {
        if (to.exists())
            to.delete();
        return from.renameTo(to);
    }

    @Override
    public String getOSDescription() {
        return Utils.strip(System.getProperty("os.name")) + " " +
                ((System.getenv("ProgramFiles(x86)") == null) ? "32" : "64") + "-Bit";
    }
}
