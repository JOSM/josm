// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Bounding box selector.
 *
 * Provides max/min lat/lon input fields as well as the "URL from www.openstreetmap.org" text field.
 *
 * @author Frederik Ramm
 *
 */
public class BoundingBoxSelection implements DownloadSelection {

    private JosmTextField[] latlon;
    private final JosmTextArea tfOsmUrl = new JosmTextArea();
    private final JosmTextArea showUrl = new JosmTextArea();
    private DownloadDialog parent;

    protected void registerBoundingBoxBuilder() {
        BoundingBoxBuilder bboxbuilder = new BoundingBoxBuilder();
        for (JosmTextField ll : latlon) {
            ll.addFocusListener(bboxbuilder);
            ll.addActionListener(bboxbuilder);
        }
    }

    protected void buildDownloadAreaInputFields() {
        latlon = new JosmTextField[4];
        for (int i = 0; i < 4; i++) {
            latlon[i] = new JosmTextField(11);
            latlon[i].setMinimumSize(new Dimension(100, new JosmTextField().getMinimumSize().height));
            latlon[i].addFocusListener(new SelectAllOnFocusHandler(latlon[i]));
        }
        LatValueChecker latChecker = new LatValueChecker(latlon[0]);
        latlon[0].addFocusListener(latChecker);
        latlon[0].addActionListener(latChecker);

        latChecker = new LatValueChecker(latlon[2]);
        latlon[2].addFocusListener(latChecker);
        latlon[2].addActionListener(latChecker);

        LonValueChecker lonChecker = new LonValueChecker(latlon[1]);
        latlon[1].addFocusListener(lonChecker);
        latlon[1].addActionListener(lonChecker);

        lonChecker = new LonValueChecker(latlon[3]);
        latlon[3].addFocusListener(lonChecker);
        latlon[3].addActionListener(lonChecker);

        registerBoundingBoxBuilder();
    }

    @Override
    public void addGui(final DownloadDialog gui) {
        buildDownloadAreaInputFields();
        final JPanel dlg = new JPanel(new GridBagLayout());

        tfOsmUrl.getDocument().addDocumentListener(new OsmUrlRefresher());

        // select content on receiving focus. this seems to be the default in the
        // windows look+feel but not for others. needs invokeLater to avoid strange
        // side effects that will cancel out the newly made selection otherwise.
        tfOsmUrl.addFocusListener(new SelectAllOnFocusHandler(tfOsmUrl));
        tfOsmUrl.setLineWrap(true);
        tfOsmUrl.setBorder(latlon[0].getBorder());

        dlg.add(new JLabel(tr("min lat")), GBC.std().insets(10, 20, 5, 0));
        dlg.add(latlon[0], GBC.std().insets(0, 20, 0, 0));
        dlg.add(new JLabel(tr("min lon")), GBC.std().insets(10, 20, 5, 0));
        dlg.add(latlon[1], GBC.eol().insets(0, 20, 0, 0));
        dlg.add(new JLabel(tr("max lat")), GBC.std().insets(10, 0, 5, 0));
        dlg.add(latlon[2], GBC.std());
        dlg.add(new JLabel(tr("max lon")), GBC.std().insets(10, 0, 5, 0));
        dlg.add(latlon[3], GBC.eol());

        final JButton btnClear = new JButton(tr("Clear textarea"));
        btnClear.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                tfOsmUrl.setText("");
            }
        });
        dlg.add(btnClear, GBC.eol().insets(10, 20, 0, 0));

        dlg.add(new JLabel(tr("URL from www.openstreetmap.org (you can paste an URL here to download the area)")),
                GBC.eol().insets(10, 5, 5, 0));
        dlg.add(tfOsmUrl, GBC.eop().insets(10, 0, 5, 0).fill());
        dlg.add(showUrl, GBC.eop().insets(10, 0, 5, 5));
        showUrl.setEditable(false);
        showUrl.setBackground(dlg.getBackground());
        showUrl.addFocusListener(new SelectAllOnFocusHandler(showUrl));

        if (gui != null)
            gui.addDownloadAreaSelector(dlg, tr("Bounding Box"));
        this.parent = gui;
    }

    @Override
    public void setDownloadArea(Bounds area) {
        updateBboxFields(area);
        updateUrl(area);
    }

    /**
     * Replies the download area.
     * @return The download area
     */
    public Bounds getDownloadArea() {
        double[] values = new double[4];
        for (int i = 0; i < 4; i++) {
            try {
                values[i] = Double.parseDouble(latlon[i].getText());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (!LatLon.isValidLat(values[0]) || !LatLon.isValidLon(values[1]))
            return null;
        if (!LatLon.isValidLat(values[2]) || !LatLon.isValidLon(values[3]))
            return null;
        return new Bounds(values);
    }

    private boolean parseURL(DownloadDialog gui) {
        Bounds b = OsmUrlToBounds.parse(tfOsmUrl.getText());
        if (b == null) return false;
        gui.boundingBoxChanged(b, this);
        updateBboxFields(b);
        updateUrl(b);
        return true;
    }

    private void updateBboxFields(Bounds area) {
        if (area == null) return;
        latlon[0].setText(DecimalDegreesCoordinateFormat.INSTANCE.latToString(area.getMin()));
        latlon[1].setText(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(area.getMin()));
        latlon[2].setText(DecimalDegreesCoordinateFormat.INSTANCE.latToString(area.getMax()));
        latlon[3].setText(DecimalDegreesCoordinateFormat.INSTANCE.lonToString(area.getMax()));
        for (JosmTextField tf: latlon) {
            resetErrorMessage(tf);
        }
    }

    private void updateUrl(Bounds area) {
        if (area == null) return;
        showUrl.setText(OsmUrlToBounds.getURL(area));
    }

    private final Border errorBorder = BorderFactory.createLineBorder(Color.RED, 1);

    protected void setErrorMessage(JosmTextField tf, String msg) {
        tf.setBorder(errorBorder);
        tf.setToolTipText(msg);
    }

    protected void resetErrorMessage(JosmTextField tf) {
        tf.setBorder(UIManager.getBorder("TextField.border"));
        tf.setToolTipText(null);
    }

    class LatValueChecker extends FocusAdapter implements ActionListener {
        private final JosmTextField tfLatValue;

        LatValueChecker(JosmTextField tfLatValue) {
            this.tfLatValue = tfLatValue;
        }

        protected void check() {
            double value = 0;
            try {
                value = Double.parseDouble(tfLatValue.getText());
            } catch (NumberFormatException ex) {
                setErrorMessage(tfLatValue, tr("The string ''{0}'' is not a valid double value.", tfLatValue.getText()));
                return;
            }
            if (!LatLon.isValidLat(value)) {
                setErrorMessage(tfLatValue, tr("Value for latitude in range [-90,90] required.", tfLatValue.getText()));
                return;
            }
            resetErrorMessage(tfLatValue);
        }

        @Override
        public void focusLost(FocusEvent e) {
            check();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            check();
        }
    }

    class LonValueChecker extends FocusAdapter implements ActionListener {
        private final JosmTextField tfLonValue;

        LonValueChecker(JosmTextField tfLonValue) {
            this.tfLonValue = tfLonValue;
        }

        protected void check() {
            double value = 0;
            try {
                value = Double.parseDouble(tfLonValue.getText());
            } catch (NumberFormatException ex) {
                setErrorMessage(tfLonValue, tr("The string ''{0}'' is not a valid double value.", tfLonValue.getText()));
                return;
            }
            if (!LatLon.isValidLon(value)) {
                setErrorMessage(tfLonValue, tr("Value for longitude in range [-180,180] required.", tfLonValue.getText()));
                return;
            }
            resetErrorMessage(tfLonValue);
        }

        @Override
        public void focusLost(FocusEvent e) {
            check();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            check();
        }
    }

    static class SelectAllOnFocusHandler extends FocusAdapter {
        private final JTextComponent tfTarget;

        SelectAllOnFocusHandler(JTextComponent tfTarget) {
            this.tfTarget = tfTarget;
        }

        @Override
        public void focusGained(FocusEvent e) {
            tfTarget.selectAll();
        }
    }

    class OsmUrlRefresher implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            parseURL(parent);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            parseURL(parent);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            parseURL(parent);
        }
    }

    class BoundingBoxBuilder extends FocusAdapter implements ActionListener {
        protected Bounds build() {
            double minlon, minlat, maxlon, maxlat;
            try {
                minlat = Double.parseDouble(latlon[0].getText().trim());
                minlon = Double.parseDouble(latlon[1].getText().trim());
                maxlat = Double.parseDouble(latlon[2].getText().trim());
                maxlon = Double.parseDouble(latlon[3].getText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
            if (!LatLon.isValidLon(minlon) || !LatLon.isValidLon(maxlon)
                    || !LatLon.isValidLat(minlat) || !LatLon.isValidLat(maxlat))
                return null;
            if (minlon > maxlon)
                return null;
            if (minlat > maxlat)
                return null;
            return new Bounds(minlat, minlon, maxlat, maxlon);
        }

        protected void refreshBounds() {
            Bounds b = build();
            parent.boundingBoxChanged(b, BoundingBoxSelection.this);
        }

        @Override
        public void focusLost(FocusEvent e) {
            refreshBounds();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            refreshBounds();
        }
    }
}
