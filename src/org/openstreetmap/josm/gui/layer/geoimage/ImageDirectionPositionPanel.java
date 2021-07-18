// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.gpx.GpxImageDirectionPositionSettings;
import org.openstreetmap.josm.tools.GBC;

/**
 * Panel allowing user to enter {@link GpxImageDirectionPositionSettings}.
 * @since 18061
 */
public class ImageDirectionPositionPanel extends JPanel {

    private final JCheckBox cChangeImageDirection = new JCheckBox();
    private final JSpinner sOffsetDegrees = new JSpinner(new SpinnerNumberModel(0, -360, 360, 1));

    private final JSpinner sX = new JSpinner(new SpinnerNumberModel(0.0, -50.0, 50.0, 0.1));
    private final JSpinner sY = new JSpinner(new SpinnerNumberModel(0.0, -50.0, 50.0, 0.1));
    private final JSpinner sZ = new JSpinner(new SpinnerNumberModel(0.0, -20.0, 20.0, 0.1));

    /**
     * Constructs a new {@code ImageMetadataModificationPanel}
     * @param changeDirectionText the text displayed for the change image direction combobox
     */
    protected ImageDirectionPositionPanel(String changeDirectionText) {
        super(new GridBagLayout());

        cChangeImageDirection.setText(changeDirectionText);
        add(cChangeImageDirection, GBC.eol().insets(0, 0, 0, 5));
        cChangeImageDirection.addActionListener(e -> sOffsetDegrees.setEnabled(!sOffsetDegrees.isEnabled()));
        addSetting(tr("Offset angle in degrees:"), sOffsetDegrees);
        sOffsetDegrees.setEnabled(false);

        add(new JSeparator(SwingConstants.HORIZONTAL),
                GBC.eol().fill(GBC.HORIZONTAL).insets(0, 12, 0, 12));

        add(new JLabel(tr("Shift image relative to the direction (in meters)")),
                GBC.eol().insets(0, 0, 0, 5));
        addSetting(tr("X:"), sX);
        addSetting(tr("Y:"), sY);
        addSetting(tr("Elevation:"), sZ);
    }

    /**
     * Returns a new {@code ImageMetadataModificationPanel} in a GPX trace context.
     * @return a new {@code ImageMetadataModificationPanel} in a GPX trace context
     */
    public static ImageDirectionPositionPanel forGpxTrace() {
        return new ImageDirectionPositionPanel(tr("Set image direction towards the next GPX waypoint"));
    }

    /**
     * Returns a new {@code ImageMetadataModificationPanel} in an image sequence context.
     * @return a new {@code ImageMetadataModificationPanel} in an image sequence context
     */
    public static ImageDirectionPositionPanel forImageSequence() {
        return new ImageDirectionPositionPanel(tr("Set image direction towards the next one"));
    }

    protected void addSetting(String text, JComponent component) {
        add(new JLabel(text, JLabel.RIGHT), GBC.std().insets(15, 0, 5, 5).fill(GBC.HORIZONTAL).weight(0, 0));
        add(component, GBC.std().fill(GBC.HORIZONTAL));
        add(GBC.glue(1, 0), GBC.eol().fill(GBC.HORIZONTAL).weight(1, 0));
    }

    /**
     * Returns the settings set by user.
     * @return the settings set by user
     */
    public GpxImageDirectionPositionSettings getSettings() {
        return new GpxImageDirectionPositionSettings(
                cChangeImageDirection.isSelected(),
                (Integer) sOffsetDegrees.getValue(),
                (Double) sX.getValue(),
                (Double) sY.getValue(),
                (Double) sZ.getValue());
    }

    /**
     * Adds a focus listener on all spinners of this panel.
     * @param focusListener focus listener to add
     */
    public void addFocusListenerOnComponent(FocusListener focusListener) {
        sOffsetDegrees.addFocusListener(focusListener);
        sX.addFocusListener(focusListener);
        sY.addFocusListener(focusListener);
        sZ.addFocusListener(focusListener);
    }

    /**
     * Adds a change listener on all checkboxes and spinners of this panel.
     * @param listener change listener to add
     */
    public void addChangeListenerOnComponents(ChangeListener listener) {
        cChangeImageDirection.addChangeListener(listener);
        sOffsetDegrees.addChangeListener(listener);
        sX.addChangeListener(listener);
        sY.addChangeListener(listener);
        sZ.addChangeListener(listener);
    }
}
