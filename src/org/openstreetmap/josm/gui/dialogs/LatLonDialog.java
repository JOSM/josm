// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

public class LatLonDialog extends JDialog {
    private static final Color BG_COLOR_ERROR = new Color(255,224,224);

    private JTextField tfLatLon;
    private String help;
    private boolean canceled = false;
    private LatLon coordinates;
    private OKAction actOK;
    private CancelAction actCancel;

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

    protected JPanel buildInputForm() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        pnl.add(new JLabel(tr("Coordinates:")), GBC.std().insets(0,10,5,0));
        tfLatLon = new JTextField(24);
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
                		"Some examples:<ul>" +
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
                        "<li>-49 29.4 N -19 24.5 W</li></ul>" +
                        "<li>48 deg 42&#39; 52.13\" N, 21 deg 11&#39; 47.60\" E</li></ul>"
                		)),
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

    protected JPanel buildButtonRow() {
        JPanel pnl = new JPanel(new FlowLayout());

        SideButton btn;
        pnl.add(btn = new SideButton(actOK = new OKAction()));
        makeButtonRespondToEnter(btn);
        pnl.add(btn = new SideButton(actCancel = new CancelAction()));
        makeButtonRespondToEnter(btn);
        pnl.add(new SideButton(new ContextSensitiveHelpAction(help)));
        return pnl;
    }

    protected void makeButtonRespondToEnter(SideButton btn) {
        btn.setFocusable(true);
        btn.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "enter");
        btn.getActionMap().put("enter", btn.getAction());
    }

    protected void build() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildInputForm(), BorderLayout.CENTER);
        getContentPane().add(buildButtonRow(), BorderLayout.SOUTH);
        pack();

        // make dialog respond to ESCAPE
        //
        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "escape");
        getRootPane().getActionMap().put("escape", actCancel);

        // make dialog respond to F1
        //
        HelpUtil.setHelpContext(getRootPane(), help);
    }

    public LatLonDialog(Component parent, String title, String help) {
        super(JOptionPane.getFrameForComponent(parent), ModalityType.DOCUMENT_MODAL);
        this.help = help;
        setTitle(title);
        build();
        addWindowListener(new WindowEventHandler());
        setCoordinates(null);
    }

    public void setCoordinates(LatLon coordinates) {
        if (coordinates == null) {
            coordinates = new LatLon(0,0);
        }
        this.coordinates = coordinates;
        tfLatLon.setText(coordinates.latToString(CoordinateFormat.getDefaultFormat()) + " " + coordinates.lonToString(CoordinateFormat.getDefaultFormat()));
        actOK.setEnabled(true);
    }

    public LatLon getCoordinates() {
        return coordinates;
    }

    protected void setErrorFeedback(JTextField tf, String message) {
        tf.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
        tf.setToolTipText(message);
        tf.setBackground(BG_COLOR_ERROR);
    }

    protected void clearErrorFeedback(JTextField tf, String message) {
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

    protected void parseUserInput() {
        LatLon latLon;
        try {
            latLon = parse(tfLatLon.getText());
            if (!LatLon.isValidLat(latLon.lat()) || !LatLon.isValidLon(latLon.lon())) {
                latLon = null;
            }
        } catch (IllegalArgumentException e) {
            latLon = null;
        }
        if (latLon == null) {
            setErrorFeedback(tfLatLon, tr("Please enter a GPS coordinates"));
            coordinates = null;
            actOK.setEnabled(false);
        } else {
            clearErrorFeedback(tfLatLon,tr("Please enter a GPS coordinates"));
            coordinates = latLon;
            actOK.setEnabled(true);
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setCanceled(false);
            WindowGeometry.centerInWindow(Main.parent, getSize()).applySafe(this);
        }
        super.setVisible(visible);
    }

    class OKAction extends AbstractAction {
        public OKAction() {
            putValue(NAME, tr("OK"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog and create a new node"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
        }

        public void actionPerformed(ActionEvent e) {
            setCanceled(false);
            setVisible(false);
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog, do not create a new node"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
        }

        public void actionPerformed(ActionEvent e) {
            setCanceled(true);
            setVisible(false);
        }
    }

    class LatLonInputVerifier implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            parseUserInput();
        }

        public void insertUpdate(DocumentEvent e) {
            parseUserInput();
        }

        public void removeUpdate(DocumentEvent e) {
            parseUserInput();
        }
    }

    static class TextFieldFocusHandler implements FocusListener {
        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            if (c instanceof JTextField) {
                JTextField tf = (JTextField)c;
                tf.selectAll();
            }
        }
        public void focusLost(FocusEvent e) {}
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void windowOpened(WindowEvent e) {
            tfLatLon.requestFocusInWindow();
        }
    }

    private static LatLon parse(final String coord) {
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

    public String getText() {
        return tfLatLon.getText();
    }

    public void setText(String text) {
        tfLatLon.setText(text);
    }
}
