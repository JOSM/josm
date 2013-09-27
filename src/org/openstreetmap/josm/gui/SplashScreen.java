// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedList;

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
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Show a splash screen so the user knows what is happening during startup.
 *
 */
public class SplashScreen extends JFrame {

    private SwingRenderingProgressMonitor progressMonitor;

    /**
     * Constructs a new {@code SplashScreen}.
     */
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
        gbc.insets = new Insets(0, 0, 0, 70);
        innerContentPane.add(logo, gbc);

        // Add the name of this application
        JLabel caption = new JLabel("JOSM - " + tr("Java OpenStreetMap Editor"));
        caption.setFont(GuiHelper.getTitleFont());
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
        SplashScreenProgressRenderer progressRenderer = new SplashScreenProgressRenderer();
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 10, 0);
        innerContentPane.add(progressRenderer, gbc);
        progressMonitor = new SwingRenderingProgressMonitor(progressRenderer);

        pack();

        WindowGeometry.centerOnScreen(this.getSize(), "gui.geometry").applySafe(this);

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
            gc.insets = new Insets(5,0,0,0);
            add(lblTaskTitle = new JLabel(" "), gc);

            gc.gridx = 0;
            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            gc.insets = new Insets(5,0,0,0);
            add(lblCustomText = new JLabel(" ") {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    if(d.width < 600) d.width = 600;
                    d.height *= MAX_NUMBER_OF_MESSAGES;
                    return d;
                }
            }, gc);

            gc.gridx = 0;
            gc.gridy = 2;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            gc.insets = new Insets(5,0,0,0);
            add(progressBar = new JProgressBar(JProgressBar.HORIZONTAL), gc);
        }

        public SplashScreenProgressRenderer() {
            build();
        }

        @Override
        public void setCustomText(String message) {
            if(message.isEmpty())
                message = " "; // prevent killing of additional line
            lblCustomText.setText(message);
            repaint();
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {
            progressBar.setIndeterminate(indeterminate);
            repaint();
        }

        @Override
        public void setMaximum(int maximum) {
            progressBar.setMaximum(maximum);
            repaint();
        }

        private static final int MAX_NUMBER_OF_MESSAGES = 3;
        private LinkedList<String> messages = new LinkedList<String>(Arrays.asList("", "", "")); //update when changing MAX_NUMBER_OF_MESSAGES
        private long time = System.currentTimeMillis();

        /**
         * Stores and displays the {@code MAX_NUMBER_OF_MESSAGES} most recent
         * task titles together with their execution time.
         */
        @Override
        public void setTaskTitle(String taskTitle) {

            while (messages.size() >= MAX_NUMBER_OF_MESSAGES) {
                messages.removeFirst();
            }
            long now = System.currentTimeMillis();
            String prevMessageTitle = messages.getLast();
            if (!prevMessageTitle.isEmpty()) {
                messages.removeLast();
                messages.add(tr("{0} ({1} ms)", prevMessageTitle, Long.toString(now - time)));
            }
            time = now;
            if (!taskTitle.isEmpty()) {
                messages.add(taskTitle);
            }
            StringBuilder html = new StringBuilder();
            int i = 0;
            for (String m : messages) {
                html.append("<p class=\"entry").append(++i).append("\">").append(m).append("</p>");
            }

            lblTaskTitle.setText("<html><style>"
                    + ".entry1{color:#CCCCCC;}"
                    + ".entry2{color:#999999;}"
                    + ".entry3{color:#000000;}</style>" + html + "</html>");  //update when changing MAX_NUMBER_OF_MESSAGES
            repaint();
        }

        @Override
        public void setValue(int value) {
            progressBar.setValue(value);
            repaint();
        }
    }
}
