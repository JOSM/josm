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
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 *
 *
 */
public class BoundingBoxSelectionPanel extends JPanel {

    private JosmTextField[] tfLatLon = null;
    private final JosmTextField tfOsmUrl = new JosmTextField();

    protected void buildInputFields() {
        tfLatLon = new JosmTextField[4];
        for(int i=0; i< 4; i++) {
            tfLatLon[i] = new JosmTextField(11);
            tfLatLon[i].setMinimumSize(new Dimension(100,new JosmTextField().getMinimumSize().height));
            SelectAllOnFocusGainedDecorator.decorate(tfLatLon[i]);
        }
        LatitudeValidator.decorate(tfLatLon[0]);
        LatitudeValidator.decorate(tfLatLon[2]);
        LongitudeValidator.decorate(tfLatLon[1]);
        LongitudeValidator.decorate(tfLatLon[3]);
    }

    protected void build() {
        buildInputFields();
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        setLayout(new GridBagLayout());
        tfOsmUrl.getDocument().addDocumentListener(new OsmUrlRefresher());

        // select content on receiving focus. this seems to be the default in the
        // windows look+feel but not for others. needs invokeLater to avoid strange
        // side effects that will cancel out the newly made selection otherwise.
        tfOsmUrl.addFocusListener(new SelectAllOnFocusGainedDecorator());

        add(new JLabel(tr("Min. latitude")), GBC.std().insets(0,0,3,5));
        add(tfLatLon[0], GBC.std().insets(0,0,3,5));
        add(new JLabel(tr("Min. longitude")), GBC.std().insets(0,0,3,5));
        add(tfLatLon[1], GBC.eol());
        add(new JLabel(tr("Max. latitude")), GBC.std().insets(0,0,3,5));
        add(tfLatLon[2], GBC.std().insets(0,0,3,5));
        add(new JLabel(tr("Max. longitude")), GBC.std().insets(0,0,3,5));
        add(tfLatLon[3], GBC.eol());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 4;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(10,0,0,3);
        add(new JMultilineLabel(tr("URL from www.openstreetmap.org (you can paste a download URL here to specify a bounding box)")), gc);

        gc.gridy = 3;
        gc.insets = new Insets(3,0,0,3);
        add(tfOsmUrl, gc);
    }

    public BoundingBoxSelectionPanel() {
        build();
    }

    public void setBoundingBox(Bounds area) {
        updateBboxFields(area);
    }

    public Bounds getBoundingBox() {
        double minlon, minlat, maxlon,maxlat;
        try {
            minlat = Double.parseDouble(tfLatLon[0].getText().trim());
            minlon = Double.parseDouble(tfLatLon[1].getText().trim());
            maxlat = Double.parseDouble(tfLatLon[2].getText().trim());
            maxlon = Double.parseDouble(tfLatLon[3].getText().trim());
        } catch(NumberFormatException e) {
            return null;
        }
        if (!LatLon.isValidLon(minlon) || !LatLon.isValidLon(maxlon)
                || !LatLon.isValidLat(minlat) || ! LatLon.isValidLat(maxlat))
            return null;
        if (minlon > maxlon)
            return null;
        if (minlat > maxlat)
            return null;
        return new Bounds(minlon,minlat,maxlon,maxlat);
    }

    private boolean parseURL() {
        Bounds b = OsmUrlToBounds.parse(tfOsmUrl.getText());
        if(b == null) return false;
        updateBboxFields(b);
        return true;
    }

    private void updateBboxFields(Bounds area) {
        if (area == null) return;
        tfLatLon[0].setText(area.getMin().latToString(CoordinateFormat.DECIMAL_DEGREES));
        tfLatLon[1].setText(area.getMin().lonToString(CoordinateFormat.DECIMAL_DEGREES));
        tfLatLon[2].setText(area.getMax().latToString(CoordinateFormat.DECIMAL_DEGREES));
        tfLatLon[3].setText(area.getMax().lonToString(CoordinateFormat.DECIMAL_DEGREES));
    }

    static private class LatitudeValidator extends AbstractTextComponentValidator {

        public static void decorate(JTextComponent tc) {
            new LatitudeValidator(tc);
        }

        public LatitudeValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public void validate() {
            double value = 0;
            try {
                value = Double.parseDouble(getComponent().getText());
            } catch(NumberFormatException ex) {
                feedbackInvalid(tr("The string ''{0}'' is not a valid double value.", getComponent().getText()));
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
            double value = 0;
            try {
                value = Double.parseDouble(getComponent().getText());
            } catch(NumberFormatException ex) {
                return false;
            }
            if (!LatLon.isValidLat(value))
                return false;
            return true;
        }
    }

    static private class LongitudeValidator extends AbstractTextComponentValidator{

        public static void decorate(JTextComponent tc) {
            new LongitudeValidator(tc);
        }

        public LongitudeValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public void validate() {
            double value = 0;
            try {
                value = Double.parseDouble(getComponent().getText());
            } catch(NumberFormatException ex) {
                feedbackInvalid(tr("The string ''{0}'' is not a valid double value.", getComponent().getText()));
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
            double value = 0;
            try {
                value = Double.parseDouble(getComponent().getText());
            } catch(NumberFormatException ex) {
                return false;
            }
            if (!LatLon.isValidLon(value))
                return false;
            return true;
        }
    }

    class OsmUrlRefresher implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) { parseURL(); }
        @Override
        public void insertUpdate(DocumentEvent e) { parseURL(); }
        @Override
        public void removeUpdate(DocumentEvent e) { parseURL(); }
    }
}
