// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.Ignore;
import org.openstreetmap.josm.gui.io.UploadStrategySelectionPanel;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.tools.Logging;

@Ignore
public class UploadStrategySelectionPanelTest extends JFrame {

    protected UploadStrategySelectionPanel uploadStrategySelectionPanel;

    protected void build() {
        getContentPane().setLayout(new BorderLayout());
        uploadStrategySelectionPanel = new UploadStrategySelectionPanel();
        getContentPane().add(uploadStrategySelectionPanel, BorderLayout.CENTER);
        getContentPane().add(buildControlPanel(), BorderLayout.SOUTH);
        setSize(400, 400);
    }

    protected JPanel buildControlPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnl.add(new JLabel("Num objects:"));
        final JTextField tf;
        pnl.add(tf = new JTextField(8));
        tf.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        int n = 0;
                        try {
                            n = Integer.parseInt(tf.getText());
                        } catch (NumberFormatException e) {
                            Logging.error(e);
                            return;
                        }
                        uploadStrategySelectionPanel.setNumUploadedObjects(n);
                    }
                }
        );
        return pnl;
    }

    /**
     * Constructs a new {@code UploadStrategySelectionPanelTest}.
     */
    public UploadStrategySelectionPanelTest() {
        build();
        uploadStrategySelectionPanel.setNumUploadedObjects(51000);
    }

    public static void main(String[] args) throws OsmApiInitializationException, OsmTransferCanceledException {
        OsmApi.getOsmApi().initialize(NullProgressMonitor.INSTANCE);
        new UploadStrategySelectionPanelTest().setVisible(true);
    }
}
