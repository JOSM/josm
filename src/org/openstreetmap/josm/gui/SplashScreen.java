// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressRenderer;
import org.openstreetmap.josm.gui.progress.SwingRenderingProgressMonitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Show a splash screen so the user knows what is happening during startup.
 *
 */
public class SplashScreen extends JFrame {

    private SplashScreenProgressRenderer progressRenderer;
    private SwingRenderingProgressMonitor progressMonitor;

    public SplashScreen() {
        super();
        setUndecorated(true);

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
        progressRenderer = new SplashScreenProgressRenderer();
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 5, 10, 5);
        innerContentPane.add(progressRenderer, gbc);
        progressMonitor = new SwingRenderingProgressMonitor(progressRenderer);

        pack();

        // Center the splash screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = contentPane.getPreferredSize();
        setLocation(screenSize.width / 2 - (labelSize.width / 2),
                screenSize.height / 2 - (labelSize.height / 2));

        // Add ability to hide splash screen by clicking it
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                setVisible(false);
            }
        });
    }

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    static private class SplashScreenProgressRenderer extends JPanel implements ProgressRenderer {
        private JLabel lblTaskTitle;
        private JLabel lblCustomText;
        private JProgressBar progressBar;

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            gc.insets = new Insets(5,0,0,5);
            add(lblTaskTitle = new JLabel(""), gc);

            gc.gridx = 0;
            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            gc.insets = new Insets(5,0,0,5);
            add(lblCustomText = new JLabel(""), gc);

            gc.gridx = 0;
            gc.gridy = 2;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            gc.insets = new Insets(5,0,0,5);
            add(progressBar = new JProgressBar(JProgressBar.HORIZONTAL), gc);
        }

        public SplashScreenProgressRenderer() {
            build();
        }

        public void setCustomText(String message) {
            lblCustomText.setText(message);
            repaint();
        }

        public void setIndeterminate(boolean indeterminate) {
            progressBar.setIndeterminate(indeterminate);
            repaint();
        }

        public void setMaximum(int maximum) {
            progressBar.setMaximum(maximum);
            repaint();
        }

        public void setTaskTitle(String taskTitle) {
            lblTaskTitle.setText(taskTitle);
            repaint();
        }

        public void setValue(int value) {
            progressBar.setValue(value);
            repaint();
        }
    }
}
