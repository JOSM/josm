// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * A panel that allows the user to input the coordinates of a lat/lon box
 */
public class BoundingBoxSelectionPanel extends JPanel {

    private JosmTextField[] tfLatLon;
    private final JosmTextField tfOsmUrl = new JosmTextField();

    protected void buildInputFields() {
        tfLatLon = new JosmTextField[4];
        for (int i = 0; i < 4; i++) {
            tfLatLon[i] = new JosmTextField(11);
            tfLatLon[i].setMinimumSize(new Dimension(100, new JosmTextField().getMinimumSize().height));
            SelectAllOnFocusGainedDecorator.decorate(tfLatLon[i]);
        }
        LatitudeValidator.decorate(tfLatLon[0]);
        LatitudeValidator.decorate(tfLatLon[2]);
        LongitudeValidator.decorate(tfLatLon[1]);
        LongitudeValidator.decorate(tfLatLon[3]);
    }

    protected final void build() {
        buildInputFields();
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new GridBagLayout());
        tfOsmUrl.getDocument().addDocumentListener(new OsmUrlRefresher());

        // select content on receiving focus. this seems to be the default in the
        // windows look+feel but not for others. needs invokeLater to avoid strange
        // side effects that will cancel out the newly made selection otherwise.
        tfOsmUrl.addFocusListener(new SelectAllOnFocusGainedDecorator());

        add(new JLabel(tr("Min. latitude")), GBC.std().insets(0, 0, 3, 5));
        add(tfLatLon[0], GBC.std().insets(0, 0, 3, 5));
        add(new JLabel(tr("Min. longitude")), GBC.std().insets(0, 0, 3, 5));
        add(tfLatLon[1], GBC.eol());
        add(new JLabel(tr("Max. latitude")), GBC.std().insets(0, 0, 3, 5));
        add(tfLatLon[2], GBC.std().insets(0, 0, 3, 5));
        add(new JLabel(tr("Max. longitude")), GBC.std().insets(0, 0, 3, 5));
        add(tfLatLon[3], GBC.eol());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 4;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(10, 0, 0, 3);
        add(new JMultilineLabel(tr("URL from www.openstreetmap.org (you can paste a download URL here to specify a bounding box)")), gc);

        gc.gridy = 3;
        gc.insets = new Insets(3, 0, 0, 3);
        add(tfOsmUrl, gc);
    }

    /**
     * Constructs a new {@code BoundingBoxSelectionPanel}.
     */
    public BoundingBoxSelectionPanel() {
        build();
    }

    /**
     * Sets the bounding box to the given area
     * @param area The new input values
     */
    public void setBoundingBox(Bounds area) {
        updateBboxFields(area);
    }

    /**
     * Get the bounding box the user selected
     * @return The box or <code>null</code> if no valid data was input.
     */
    public Bounds getBoundingBox() {
        double minlon, minlat, maxlon, maxlat;
        try {
            minlat = JosmDecimalFormatSymbolsProvider.parseDouble(tfLatLon[0].getText().trim());
            minlon = JosmDecimalFormatSymbolsProvider.parseDouble(tfLatLon[1].getText().trim());
            maxlat = JosmDecimalFormatSymbolsProvider.parseDouble(tfLatLon[2].getText().trim());
            maxlon = JosmDecimalFormatSymbolsProvider.parseDouble(tfLatLon[3].getText().trim());
        } catch (NumberFormatException e) {
            Logging.trace(e);
            return null;
        }
        if (!LatLon.isValidLon(minlon) || !LatLon.isValidLon(maxlon)
         || !LatLon.isValidLat(minlat) || !LatLon.isValidLat(maxlat))
            return null;
        if (minlon > maxlon)
            return null;
        if (minlat > maxlat)
            return null;
        return new Bounds(minlon, minlat, maxlon, maxlat);
    }

    private boolean parseURL() {
        Bounds b = OsmUrlToBounds.parse(tfOsmUrl.getText());
        if (b == null) return false;
        updateBboxFields(b);
        return true;
    }

    private void updateBboxFields(Bounds area) {
        if (area == null) return;
        tfLatLon[0].setText(DecimalDegreesCoordinateFormat.INSTANCE.latToString(area.getMin()));
        tfLatLon[1].setText(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(area.getMin()));
        tfLatLon[2].setText(DecimalDegreesCoordinateFormat.INSTANCE.latToString(area.getMax()));
        tfLatLon[3].setText(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(area.getMax()));
    }

    private static class LatitudeValidator extends AbstractTextComponentValidator {

        public static void decorate(JTextComponent tc) {
            new LatitudeValidator(tc);
        }

        LatitudeValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public void validate() {
            double value = 0;
            try {
                value = JosmDecimalFormatSymbolsProvider.parseDouble(getComponent().getText());
            } catch (NumberFormatException ex) {
                feedbackInvalid(tr("The string ''{0}'' is not a valid double value.", getComponent().getText()));
                Logging.trace(ex);
                return;
            }
            if (!LatLon.isValidLat(value)) {
                feedbackInvalid(tr("Value for latitude in range [-90,90] required.", getComponent().getText()));
                return;
            }
            feedbackValid("");
        }

        @Override
        public boolean isValid() {
            try {
                return LatLon.isValidLat(JosmDecimalFormatSymbolsProvider.parseDouble(getComponent().getText()));
            } catch (NumberFormatException ex) {
                Logging.trace(ex);
                return false;
            }
        }
    }

    private static class LongitudeValidator extends AbstractTextComponentValidator {

        public static void decorate(JTextComponent tc) {
            new LongitudeValidator(tc);
        }

        LongitudeValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public void validate() {
            double value = 0;
            try {
                value = JosmDecimalFormatSymbolsProvider.parseDouble(getComponent().getText());
            } catch (NumberFormatException ex) {
                feedbackInvalid(tr("The string ''{0}'' is not a valid double value.", getComponent().getText()));
                Logging.trace(ex);
                return;
            }
            if (!LatLon.isValidLon(value)) {
                feedbackInvalid(tr("Value for longitude in range [-180,180] required.", getComponent().getText()));
                return;
            }
            feedbackValid("");
        }

        @Override
        public boolean isValid() {
            try {
                return LatLon.isValidLon(JosmDecimalFormatSymbolsProvider.parseDouble(getComponent().getText()));
            } catch (NumberFormatException ex) {
                Logging.trace(ex);
                return false;
            }
        }
    }

    class OsmUrlRefresher implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            parseURL();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            parseURL();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            parseURL();
        }
    }
}
