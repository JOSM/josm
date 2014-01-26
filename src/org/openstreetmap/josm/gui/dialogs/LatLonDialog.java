// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.WindowGeometry;

public class LatLonDialog extends ExtendedDialog {
    private static final Color BG_COLOR_ERROR = new Color(255,224,224);

    public JTabbedPane tabs;
    private JosmTextField tfLatLon, tfEastNorth;
    private LatLon latLonCoordinates;
    private EastNorth eastNorthCoordinates;

    private static final double ZERO = 0.0;
    private static final String DEG = "\u00B0";
    private static final String MIN = "\u2032";
    private static final String SEC = "\u2033";

    private static final char N_TR = LatLon.NORTH.charAt(0);
    private static final char S_TR = LatLon.SOUTH.charAt(0);
    private static final char E_TR = LatLon.EAST.charAt(0);
    private static final char W_TR = LatLon.WEST.charAt(0);

    private static final Pattern p = Pattern.compile(
            "([+|-]?\\d+[.,]\\d+)|"             // (1)
            + "([+|-]?\\d+)|"                   // (2)
            + "("+DEG+"|o|deg)|"                // (3)
            + "('|"+MIN+"|min)|"                // (4)
            + "(\"|"+SEC+"|sec)|"               // (5)
            + "(,|;)|"                          // (6)
            + "([NSEW"+N_TR+S_TR+E_TR+W_TR+"])|"// (7)
            + "\\s+|"
            + "(.+)");

    protected JPanel buildLatLon() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        pnl.add(new JLabel(tr("Coordinates:")), GBC.std().insets(0,10,5,0));
        tfLatLon = new JosmTextField(24);
        pnl.add(tfLatLon, GBC.eol().insets(0,10,0,0).fill(GBC.HORIZONTAL).weight(1.0, 0.0));

        pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0,5,0,5));

        pnl.add(new HtmlPanel(
                tr("Enter the coordinates for the new node.<br/>You can separate longitude and latitude with space, comma or semicolon.<br/>" +
                        "Use positive numbers or N, E characters to indicate North or East cardinal direction.<br/>" +
                        "For South and West cardinal directions you can use either negative numbers or S, W characters.<br/>" +
                        "Coordinate value can be in one of three formats:<ul>" +
                        "<li><i>degrees</i><tt>&deg;</tt></li>" +
                        "<li><i>degrees</i><tt>&deg;</tt> <i>minutes</i><tt>&#39;</tt></li>" +
                        "<li><i>degrees</i><tt>&deg;</tt> <i>minutes</i><tt>&#39;</tt> <i>seconds</i><tt>&quot</tt></li>" +
                        "</ul>" +
                        "Symbols <tt>&deg;</tt>, <tt>&#39;</tt>, <tt>&prime;</tt>, <tt>&quot;</tt>, <tt>&Prime;</tt> are optional.<br/><br/>" +
                        "Some examples:<ul>{0}</ul>", 
                        "<li>49.29918&deg; 19.24788&deg;</li>" +
                        "<li>N 49.29918 E 19.24788</li>" +
                        "<li>W 49&deg;29.918&#39; S 19&deg;24.788&#39;</li>" +
                        "<li>N 49&deg;29&#39;04&quot; E 19&deg;24&#39;43&quot;</li>" +
                        "<li>49.29918 N, 19.24788 E</li>" +
                        "<li>49&deg;29&#39;21&quot; N 19&deg;24&#39;38&quot; E</li>" +
                        "<li>49 29 51, 19 24 18</li>" +
                        "<li>49 29, 19 24</li>" +
                        "<li>E 49 29, N 19 24</li>" +
                        "<li>49&deg; 29; 19&deg; 24</li>" +
                        "<li>N 49&deg; 29, W 19&deg; 24</li>" +
                        "<li>49&deg; 29.5 S, 19&deg; 24.6 E</li>" +
                        "<li>N 49 29.918 E 19 15.88</li>" +
                        "<li>49 29.4 19 24.5</li>" +
                        "<li>-49 29.4 N -19 24.5 W</li>" +
                        "<li>48 deg 42&#39; 52.13\" N, 21 deg 11&#39; 47.60\" E</li>")),
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
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        pnl.add(new JLabel(tr("Projected coordinates:")), GBC.std().insets(0,10,5,0));
        tfEastNorth = new JosmTextField(24);

        pnl.add(tfEastNorth, GBC.eol().insets(0,10,0,0).fill(GBC.HORIZONTAL).weight(1.0, 0.0));

        pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0,5,0,5));

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
        tabs.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                switch (tabs.getModel().getSelectedIndex()) {
                    case 0: parseLatLonUserInput(); break;
                    case 1: parseEastNorthUserInput(); break;
                    default: throw new AssertionError();
                }
            }
        });
        setContent(tabs, false);
    }

    public LatLonDialog(Component parent, String title, String help) {
        super(parent, title, new String[] { tr("Ok"), tr("Cancel") });
        setButtonIcons(new String[] { "ok", "cancel" });
        configureContextsensitiveHelp(help, true);

        build();
        setCoordinates(null);
    }

    public boolean isLatLon() {
        return tabs.getModel().getSelectedIndex() == 0;
    }

    public void setCoordinates(LatLon ll) {
        if (ll == null) {
            ll = new LatLon(0,0);
        }
        this.latLonCoordinates = ll;
        tfLatLon.setText(ll.latToString(CoordinateFormat.getDefaultFormat()) + " " + ll.lonToString(CoordinateFormat.getDefaultFormat()));
        EastNorth en = Main.getProjection().latlon2eastNorth(ll);
        tfEastNorth.setText(en.east()+" "+en.north());
        setOkEnabled(true);
    }

    public LatLon getCoordinates() {
        if (isLatLon()) {
            return latLonCoordinates;
        } else {
            if (eastNorthCoordinates == null) return null;
            return Main.getProjection().eastNorth2latlon(eastNorthCoordinates);
        }
    }

    public LatLon getLatLonCoordinates() {
        return latLonCoordinates;
    }

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

    protected Double parseDoubleFromUserInput(String input) {
        if (input == null) return null;
        // remove white space and an optional degree symbol
        //
        input = input.trim();
        input = input.replaceAll(DEG, "");

        // try to parse using the current locale
        //
        NumberFormat f = NumberFormat.getNumberInstance();
        Number n=null;
        ParsePosition pp = new ParsePosition(0);
        n = f.parse(input,pp);
        if (pp.getErrorIndex() >= 0 || pp.getIndex()<input.length()) {
            // fall back - try to parse with the english locale
            //
            pp = new ParsePosition(0);
            f = NumberFormat.getNumberInstance(Locale.ENGLISH);
            n = f.parse(input, pp);
            if (pp.getErrorIndex() >= 0 || pp.getIndex()<input.length())
                return null;
        }
        return n== null ? null : n.doubleValue();
    }

    protected void parseLatLonUserInput() {
        LatLon latLon;
        try {
            latLon = parseLatLon(tfLatLon.getText());
            if (!LatLon.isValidLat(latLon.lat()) || !LatLon.isValidLon(latLon.lon())) {
                latLon = null;
            }
        } catch (IllegalArgumentException e) {
            latLon = null;
        }
        if (latLon == null) {
            setErrorFeedback(tfLatLon, tr("Please enter a GPS coordinates"));
            latLonCoordinates = null;
            setOkEnabled(false);
        } else {
            clearErrorFeedback(tfLatLon,tr("Please enter a GPS coordinates"));
            latLonCoordinates = latLon;
            setOkEnabled(true);
        }
    }

    protected void parseEastNorthUserInput() {
        EastNorth en;
        try {
            en = parseEastNorth(tfEastNorth.getText());
        } catch (IllegalArgumentException e) {
            en = null;
        }
        if (en == null) {
            setErrorFeedback(tfEastNorth, tr("Please enter a Easting and Northing"));
            latLonCoordinates = null;
            setOkEnabled(false);
        } else {
            clearErrorFeedback(tfEastNorth,tr("Please enter a Easting and Northing"));
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
        if (visible) {
            WindowGeometry.centerInWindow(Main.parent, getSize()).applySafe(this);
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
                JosmTextField tf = (JosmTextField)c;
                tf.selectAll();
            }
        }
        @Override
        public void focusLost(FocusEvent e) {}
    }

    public static LatLon parseLatLon(final String coord) {
        final Matcher m = p.matcher(coord);

        final StringBuilder sb = new StringBuilder();
        final List<Object> list = new ArrayList<Object>();

        while (m.find()) {
            if (m.group(1) != null) {
                sb.append('R');     // floating point number
                list.add(Double.parseDouble(m.group(1).replace(',', '.')));
            } else if (m.group(2) != null) {
                sb.append('Z');     // integer number
                list.add(Double.parseDouble(m.group(2)));
            } else if (m.group(3) != null) {
                sb.append('o');     // degree sign
            } else if (m.group(4) != null) {
                sb.append('\'');    // seconds sign
            } else if (m.group(5) != null) {
                sb.append('"');     // minutes sign
            } else if (m.group(6) != null) {
                sb.append(',');     // separator
            } else if (m.group(7) != null) {
                sb.append("x");     // cardinal direction
                String c = m.group(7).toUpperCase();
                if (c.equals("N") || c.equals("S") || c.equals("E") || c.equals("W")) {
                    list.add(c);
                } else {
                    list.add(c.replace(N_TR, 'N').replace(S_TR, 'S')
                            .replace(E_TR, 'E').replace(W_TR, 'W'));
                }
            } else if (m.group(8) != null) {
                throw new IllegalArgumentException("invalid token: " + m.group(8));
            }
        }

        final String pattern = sb.toString();

        final Object[] params = list.toArray();
        final LatLonHolder latLon = new LatLonHolder();

        if (pattern.matches("Ro?,?Ro?")) {
            setLatLonObj(latLon,
                    params[0], ZERO, ZERO, "N",
                    params[1], ZERO, ZERO, "E");
        } else if (pattern.matches("xRo?,?xRo?")) {
            setLatLonObj(latLon,
                    params[1], ZERO, ZERO, params[0],
                    params[3], ZERO, ZERO, params[2]);
        } else if (pattern.matches("Ro?x,?Ro?x")) {
            setLatLonObj(latLon,
                    params[0], ZERO, ZERO, params[1],
                    params[2], ZERO, ZERO, params[3]);
        } else if (pattern.matches("Zo[RZ]'?,?Zo[RZ]'?|Z[RZ],?Z[RZ]")) {
            setLatLonObj(latLon,
                    params[0], params[1], ZERO, "N",
                    params[2], params[3], ZERO, "E");
        } else if (pattern.matches("xZo[RZ]'?,?xZo[RZ]'?|xZo?[RZ],?xZo?[RZ]")) {
            setLatLonObj(latLon,
                    params[1], params[2], ZERO, params[0],
                    params[4], params[5], ZERO, params[3]);
        } else if (pattern.matches("Zo[RZ]'?x,?Zo[RZ]'?x|Zo?[RZ]x,?Zo?[RZ]x")) {
            setLatLonObj(latLon,
                    params[0], params[1], ZERO, params[2],
                    params[3], params[4], ZERO, params[5]);
        } else if (pattern.matches("ZoZ'[RZ]\"?x,?ZoZ'[RZ]\"?x|ZZ[RZ]x,?ZZ[RZ]x")) {
            setLatLonObj(latLon,
                    params[0], params[1], params[2], params[3],
                    params[4], params[5], params[6], params[7]);
        } else if (pattern.matches("xZoZ'[RZ]\"?,?xZoZ'[RZ]\"?|xZZ[RZ],?xZZ[RZ]")) {
            setLatLonObj(latLon,
                    params[1], params[2], params[3], params[0],
                    params[5], params[6], params[7], params[4]);
        } else if (pattern.matches("ZZ[RZ],?ZZ[RZ]")) {
            setLatLonObj(latLon,
                    params[0], params[1], params[2], "N",
                    params[3], params[4], params[5], "E");
        } else {
            throw new IllegalArgumentException("invalid format: " + pattern);
        }

        return new LatLon(latLon.lat, latLon.lon);
    }

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

    private static class LatLonHolder {
        double lat, lon;
    }

    private static void setLatLonObj(final LatLonHolder latLon,
            final Object coord1deg, final Object coord1min, final Object coord1sec, final Object card1,
            final Object coord2deg, final Object coord2min, final Object coord2sec, final Object card2) {

        setLatLon(latLon,
                (Double) coord1deg, (Double) coord1min, (Double) coord1sec, (String) card1,
                (Double) coord2deg, (Double) coord2min, (Double) coord2sec, (String) card2);
    }

    private static void setLatLon(final LatLonHolder latLon,
            final double coord1deg, final double coord1min, final double coord1sec, final String card1,
            final double coord2deg, final double coord2min, final double coord2sec, final String card2) {

        setLatLon(latLon, coord1deg, coord1min, coord1sec, card1);
        setLatLon(latLon, coord2deg, coord2min, coord2sec, card2);
    }

    private static void setLatLon(final LatLonHolder latLon, final double coordDeg, final double coordMin, final double coordSec, final String card) {
        if (coordDeg < -180 || coordDeg > 180 || coordMin < 0 || coordMin >= 60 || coordSec < 0 || coordSec > 60) {
            throw new IllegalArgumentException("out of range");
        }

        double coord = (coordDeg < 0 ? -1 : 1) * (Math.abs(coordDeg) + coordMin / 60 + coordSec / 3600);
        coord = card.equals("N") || card.equals("E") ? coord : -coord;
        if (card.equals("N") || card.equals("S")) {
            latLon.lat = coord;
        } else {
            latLon.lon = coord;
        }
    }

    public String getLatLonText() {
        return tfLatLon.getText();
    }

    public void setLatLonText(String text) {
        tfLatLon.setText(text);
    }

    public String getEastNorthText() {
        return tfEastNorth.getText();
    }

    public void setEastNorthText(String text) {
        tfEastNorth.setText(text);
    }

}
