// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Show a splash screen so the user knows what is happening during startup.
 *
 * @author cbrill
 */
public class SplashScreen extends JWindow {

    private JLabel status;
    private boolean visible;

    private Runnable closerRunner;

    public SplashScreen(boolean visible) {
        super();
        this.visible=visible;

        if (!visible)
            return;

        // Add a nice border to the main splash screen
        JPanel contentPane = (JPanel)this.getContentPane();
        Border margin = new EtchedBorder(1, Color.white, Color.gray);
        contentPane.setBorder(margin);

        // Add a margin from the border to the content
        JPanel innerContentPane = new JPanel();
        innerContentPane.setBorder(new EmptyBorder(10, 10, 2, 10));
        contentPane.add(innerContentPane);
        innerContentPane.setLayout(new GridBagLayout());

        // Add the logo
        JLabel logo = new JLabel(ImageProvider.get("logo.png"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridheight = 2;
        innerContentPane.add(logo, gbc);

        // Add the name of this application
        JLabel caption = new JLabel("JOSM - " + tr("Java OpenStreetMap Editor"));
        caption.setFont(new Font("Helvetica", Font.BOLD, 20));
        gbc.gridheight = 1;
        gbc.gridx = 1;
        gbc.insets = new Insets(30, 0, 0, 0);
        innerContentPane.add(caption, gbc);

        // Add the version number
        JLabel version = new JLabel(tr("Version {0}", Version.getInstance().getVersionString()));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        innerContentPane.add(version, gbc);

        // Add a separator to the status text
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 0, 5, 0);
        innerContentPane.add(separator, gbc);

        // Add a status message
        status = new JLabel();
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        innerContentPane.add(status, gbc);
        setStatus(tr("Initializing"));

        pack();

        // Center the splash screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = contentPane.getPreferredSize();
        setLocation(screenSize.width / 2 - (labelSize.width / 2),
                screenSize.height / 2 - (labelSize.height / 2));

        // Method to close the splash screen when being clicked or when closeSplash is called
        closerRunner = new Runnable() {
            public void run() {
                setVisible(false);
                dispose();
            }
        };

        // Add ability to hide splash screen by clicking it
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                try {
                    closerRunner.run();
                } catch (Exception e) {
                    e.printStackTrace();
                    // can catch InvocationTargetException
                    // can catch InterruptedException
                }
            }
        });

        // Hide splashscreen when other window is created
        Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.WINDOW_EVENT_MASK);

        setVisible(true);
    }

    private AWTEventListener awtListener = new AWTEventListener() {
        public void eventDispatched(AWTEvent event) {
            if (event.getSource() != SplashScreen.this) {
                closeSplash();
            }
        }
    };

    /**
     * This method sets the status message. It should be called prior to
     * actually doing the action.
     *
     * @param message
     *            the message to be displayed
     */
    public void setStatus(String message) {
        if (!visible)
            return;
        status.setText(message + "...");
    }

    /**
     * Closes the splashscreen. Call once you are done starting.
     */
    public void closeSplash() {
        if (!visible)
            return;
        Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
        try {
            SwingUtilities.invokeLater(closerRunner);
        } catch (Exception e) {
            e.printStackTrace();
            // can catch InvocationTargetException
            // can catch InterruptedException
        }
    }
}
