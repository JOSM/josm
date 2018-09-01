// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.openstreetmap.josm.gui.io.SaveLayersModel.Mode;
import org.openstreetmap.josm.gui.progress.ProgressRenderer;

/**
 * A {@link ProgressRenderer} used for upload/save action in the {@link SaveLayersDialog}.
 */
class UploadAndSaveProgressRenderer extends JPanel implements ProgressRenderer, PropertyChangeListener {

    private final JLabel lblTaskTitle = new JLabel("");
    private final JLabel lblCustomText = new JLabel("");
    private final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);

    /**
     * Constructs a new {@code UploadAndSaveProgressRenderer}.
     */
    UploadAndSaveProgressRenderer() {
        build();
        // initially not visible
        setVisible(false);
    }

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(5, 0, 0, 5);
        add(lblTaskTitle, gc);
        lblTaskTitle.setLabelFor(lblCustomText);

        gc.gridy = 1;
        add(lblCustomText, gc);
        lblCustomText.setLabelFor(progressBar);

        gc.gridy = 2;
        add(progressBar, gc);
    }

    @Override
    public void setCustomText(String message) {
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

    @Override
    public void setTaskTitle(String taskTitle) {
        lblTaskTitle.setText(taskTitle);
        repaint();
    }

    @Override
    public void setValue(int value) {
        progressBar.setValue(value);
        repaint();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(SaveLayersModel.MODE_PROP)) {
            setVisible(Mode.UPLOADING_AND_SAVING == evt.getNewValue());
        }
        getParent().validate();
    }
}
