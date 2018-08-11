// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * A dialog that lets the user add a node at the coordinates he enters.
 */
public class LatLonDialog extends ExtendedDialog {
    private static final Color BG_COLOR_ERROR = new Color(255, 224, 224);

    /**
     * The tabs that define the coordinate mode.
     */
    public JTabbedPane tabs;
    private JosmTextField tfLatLon, tfEastNorth;
    private LatLon latLonCoordinates;
    private EastNorth eastNorthCoordinates;

    protected JPanel buildLatLon() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        pnl.add(new JLabel(tr("Coordinates:")), GBC.std().insets(0, 10, 5, 0));
        tfLatLon = new JosmTextField(24);
        pnl.add(tfLatLon, GBC.eol().insets(0, 10, 0, 0).fill(GBC.HORIZONTAL).weight(1.0, 0.0));

        pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 5));

        pnl.add(new HtmlPanel(
                Utils.join("<br/>", Arrays.asList(
                        tr("Enter the coordinates for the new node."),
                        tr("You can separate longitude and latitude with space, comma or semicolon."),
                        tr("Use positive numbers or N, E characters to indicate North or East cardinal direction."),
                        tr("For South and West cardinal directions you can use either negative numbers or S, W characters."),
                        tr("Coordinate value can be in one of three formats:")
                      )) +
                Utils.joinAsHtmlUnorderedList(Arrays.asList(
                        tr("<i>degrees</i><tt>&deg;</tt>"),
                        tr("<i>degrees</i><tt>&deg;</tt> <i>minutes</i><tt>&#39;</tt>"),
                        tr("<i>degrees</i><tt>&deg;</tt> <i>minutes</i><tt>&#39;</tt> <i>seconds</i><tt>&quot</tt>")
                      )) +
                Utils.join("<br/><br/>", Arrays.asList(
                        tr("Symbols <tt>&deg;</tt>, <tt>&#39;</tt>, <tt>&prime;</tt>, <tt>&quot;</tt>, <tt>&Prime;</tt> are optional."),
                        tr("You can also use the syntax <tt>lat=\"...\" lon=\"...\"</tt> or <tt>lat=''...'' lon=''...''</tt>."),
                        tr("Some examples:")
                      )) +
                "<table><tr><td>" +
                Utils.joinAsHtmlUnorderedList(Arrays.asList(
                        "49.29918 19.24788",
                        "49.29918, 19.24788",
                        "49.29918&deg; 19.24788&deg;",
                        "N 49.29918 E 19.24788",
                        "W 49&deg;29.918&#39; S 19&deg;24.788&#39;",
                        "N 49&deg;29&#39;04&quot; E 19&deg;24&#39;43&quot;",
                        "49.29918 N, 19.24788 E",
                        "49&deg;29&#39;21&quot; N 19&deg;24&#39;38&quot; E",
                        "49 29 51, 19 24 18",
                        "49 29, 19 24"
                      )) +
                "</td><td>" +
                Utils.joinAsHtmlUnorderedList(Arrays.asList(
                        "E 49 29, N 19 24",
                        "49&deg; 29; 19&deg; 24",
                        "N 49&deg; 29, W 19&deg; 24",
                        "49&deg; 29.5 S, 19&deg; 24.6 E",
                        "N 49 29.918 E 19 15.88",
                        "49 29.4 19 24.5",
                        "-49 29.4 N -19 24.5 W",
                        "48 deg 42&#39; 52.13\" N, 21 deg 11&#39; 47.60\" E",
                        "lat=\"49.29918\" lon=\"19.24788\"",
                        "lat='49.29918' lon='19.24788'"
                    )) +
                "</td></tr></table>"),
                GBC.eol().fill().weight(1.0, 1.0));

        // parse and verify input on the fly
        //
        LatLonInputVerifier inputVerifier = new LatLonInputVerifier();
        tfLatLon.getDocument().addDocumentListener(inputVerifier);

        // select the text in the field on focus
        //
        TextFieldFocusHandler focusHandler = new TextFieldFocusHandler();
        tfLatLon.addFocusListener(focusHandler);
        return pnl;
    }

    private JPanel buildEastNorth() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        pnl.add(new JLabel(tr("Projected coordinates:")), GBC.std().insets(0, 10, 5, 0));
        tfEastNorth = new JosmTextField(24);

        pnl.add(tfEastNorth, GBC.eol().insets(0, 10, 0, 0).fill(GBC.HORIZONTAL).weight(1.0, 0.0));

        pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 5));

        pnl.add(new HtmlPanel(
                tr("Enter easting and northing (x and y) separated by space, comma or semicolon.")),
                GBC.eol().fill(GBC.HORIZONTAL));

        pnl.add(GBC.glue(1, 1), GBC.eol().fill().weight(1.0, 1.0));

        EastNorthInputVerifier inputVerifier = new EastNorthInputVerifier();
        tfEastNorth.getDocument().addDocumentListener(inputVerifier);

        TextFieldFocusHandler focusHandler = new TextFieldFocusHandler();
        tfEastNorth.addFocusListener(focusHandler);

        return pnl;
    }

    protected void build() {
        tabs = new JTabbedPane();
        tabs.addTab(tr("Lat/Lon"), buildLatLon());
        tabs.addTab(tr("East/North"), buildEastNorth());
        tabs.getModel().addChangeListener(e -> {
            switch (tabs.getModel().getSelectedIndex()) {
                case 0: parseLatLonUserInput(); break;
                case 1: parseEastNorthUserInput(); break;
                default: throw new AssertionError();
            }
        });
        setContent(tabs, false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                tfLatLon.requestFocusInWindow();
            }
        });
    }

    /**
     * Creates a new {@link LatLonDialog}
     * @param parent The parent
     * @param title The title of this dialog
     * @param help The help text to use
     */
    public LatLonDialog(Component parent, String title, String help) {
        super(parent, title, tr("Ok"), tr("Cancel"));
        setButtonIcons("ok", "cancel");
        configureContextsensitiveHelp(help, true);

        build();
        setCoordinates(null);
    }

    /**
     * Check if lat/lon mode is active
     * @return <code>true</code> iff the user selects lat/lon coordinates
     */
    public boolean isLatLon() {
        return tabs.getModel().getSelectedIndex() == 0;
    }

    /**
     * Sets the coordinate fields to the given coordinates
     * @param ll The lat/lon coordinates
     */
    public void setCoordinates(LatLon ll) {
        LatLon llc = Optional.ofNullable(ll).orElse(LatLon.ZERO);
        tfLatLon.setText(CoordinateFormatManager.getDefaultFormat().latToString(llc) + ' ' +
                         CoordinateFormatManager.getDefaultFormat().lonToString(llc));
        EastNorth en = ProjectionRegistry.getProjection().latlon2eastNorth(llc);
        tfEastNorth.setText(Double.toString(en.east()) + ' ' + Double.toString(en.north()));
        // Both latLonCoordinates and eastNorthCoordinates may have been reset to null if ll is out of the world
        latLonCoordinates = llc;
        eastNorthCoordinates = en;
        setOkEnabled(true);
    }

    /**
     * Gets the coordinates that are entered by the user.
     * @return The coordinates
     */
    public LatLon getCoordinates() {
        if (isLatLon()) {
            return latLonCoordinates;
        } else {
            if (eastNorthCoordinates == null) return null;
            return ProjectionRegistry.getProjection().eastNorth2latlon(eastNorthCoordinates);
        }
    }

    /**
     * Gets the coordinates that are entered in the lat/lon field
     * @return The lat/lon coordinates
     */
    public LatLon getLatLonCoordinates() {
        return latLonCoordinates;
    }

    /**
     * Gets the coordinates that are entered in the east/north field
     * @return The east/north coordinates
     */
    public EastNorth getEastNorthCoordinates() {
        return eastNorthCoordinates;
    }

    protected void setErrorFeedback(JosmTextField tf, String message) {
        tf.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
        tf.setToolTipText(message);
        tf.setBackground(BG_COLOR_ERROR);
    }

    protected void clearErrorFeedback(JosmTextField tf, String message) {
        tf.setBorder(UIManager.getBorder("TextField.border"));
        tf.setToolTipText(message);
        tf.setBackground(UIManager.getColor("TextField.background"));
    }

    protected void parseLatLonUserInput() {
        LatLon latLon;
        try {
            latLon = LatLonParser.parse(tfLatLon.getText());
            if (!LatLon.isValidLat(latLon.lat()) || !LatLon.isValidLon(latLon.lon())) {
                latLon = null;
            }
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            latLon = null;
        }
        if (latLon == null) {
            setErrorFeedback(tfLatLon, tr("Please enter a GPS coordinates"));
            latLonCoordinates = null;
            setOkEnabled(false);
        } else {
            clearErrorFeedback(tfLatLon, tr("Please enter a GPS coordinates"));
            latLonCoordinates = latLon;
            setOkEnabled(true);
        }
    }

    protected void parseEastNorthUserInput() {
        EastNorth en;
        try {
            en = parseEastNorth(tfEastNorth.getText());
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            en = null;
        }
        if (en == null) {
            setErrorFeedback(tfEastNorth, tr("Please enter a Easting and Northing"));
            latLonCoordinates = null;
            setOkEnabled(false);
        } else {
            clearErrorFeedback(tfEastNorth, tr("Please enter a Easting and Northing"));
            eastNorthCoordinates = en;
            setOkEnabled(true);
        }
    }

    private void setOkEnabled(boolean b) {
        if (buttons != null && !buttons.isEmpty()) {
            buttons.get(0).setEnabled(b);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        final String preferenceKey = getClass().getName() + ".geometry";
        if (visible) {
            new WindowGeometry(
                    preferenceKey,
                    WindowGeometry.centerInWindow(getParent(), getSize())
            ).applySafe(this);
        } else {
            new WindowGeometry(this).remember(preferenceKey);
        }
        super.setVisible(visible);
    }

    class LatLonInputVerifier implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            parseLatLonUserInput();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            parseLatLonUserInput();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            parseLatLonUserInput();
        }
    }

    class EastNorthInputVerifier implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            parseEastNorthUserInput();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            parseEastNorthUserInput();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            parseEastNorthUserInput();
        }
    }

    static class TextFieldFocusHandler implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            if (c instanceof JosmTextField) {
                JosmTextField tf = (JosmTextField) c;
                tf.selectAll();
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            // Not used
        }
    }

    /**
     * Parses a east/north coordinate string
     * @param s The coordinates. Dot has to be used as decimal separator, as comma can be used to delimit values
     * @return The east/north coordinates or <code>null</code> on error.
     */
    public static EastNorth parseEastNorth(String s) {
        String[] en = s.split("[;, ]+");
        if (en.length != 2) return null;
        try {
            double east = Double.parseDouble(en[0]);
            double north = Double.parseDouble(en[1]);
            return new EastNorth(east, north);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * Gets the text entered in the lat/lon text field.
     * @return The text the user entered
     */
    public String getLatLonText() {
        return tfLatLon.getText();
    }

    /**
     * Set the text in the lat/lon text field.
     * @param text The new text
     */
    public void setLatLonText(String text) {
        tfLatLon.setText(text);
    }

    /**
     * Gets the text entered in the east/north text field.
     * @return The text the user entered
     */
    public String getEastNorthText() {
        return tfEastNorth.getText();
    }

    /**
     * Set the text in the east/north text field.
     * @param text The new text
     */
    public void setEastNorthText(String text) {
        tfEastNorth.setText(text);
    }
}
