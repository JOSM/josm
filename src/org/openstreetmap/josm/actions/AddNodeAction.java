// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
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
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This action displays a dialog where the user can enter a latitude and longitude,
 * and when ok is pressed, a new node is created at the specified position.
 */
public final class AddNodeAction extends JosmAction {
    //static private final Logger logger = Logger.getLogger(AddNodeAction.class.getName());

    public AddNodeAction() {
        super(tr("Add Node..."), "addnode", tr("Add a node by entering latitude and longitude."),
                Shortcut.registerShortcut("addnode", tr("Edit: {0}", tr("Add Node...")), KeyEvent.VK_D, Shortcut.GROUP_EDIT,
                        Shortcut.SHIFT_DEFAULT), true);
        putValue("help", ht("/Action/AddNode"));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        LatLonDialog dialog = new LatLonDialog(Main.parent);
        dialog.setVisible(true);
        if (dialog.isCanceled())
            return;

        LatLon coordinates = dialog.getCoordinates();
        if (coordinates == null)
            return;
        Node nnew = new Node(coordinates);

        // add the node
        Main.main.undoRedo.add(new AddCommand(nnew));
        getCurrentDataSet().setSelected(nnew);
        Main.map.mapView.repaint();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

    static private class LatLonDialog extends JDialog {
        private static final Color BG_COLOR_ERROR = new Color(255,224,224);

        private JTextField tfLat;
        private JTextField tfLon;
        private boolean canceled = false;
        private LatLon coordinates;
        private OKAction actOK;
        private CancelAction actCancel;

        protected JPanel buildInputForm() {
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            pnl.add(new JLabel("<html>"+
                    tr("Enter the coordinates for the new node.") +
                    "<br>" + tr("Use decimal degrees.") +
                    "<br>" + tr("Negative values denote Western/Southern hemisphere.")),
                    GBC.eol());

            pnl.add(new JLabel(tr("Latitude")), GBC.std().insets(0,10,5,0));
            tfLat = new JTextField(12);
            pnl.add(tfLat, GBC.eol().insets(0,10,0,0));
            pnl.add(new JLabel(tr("Longitude")), GBC.std().insets(0,0,5,10));
            tfLon = new JTextField(12);
            pnl.add(tfLon, GBC.eol().insets(0,0,0,10));

            // parse and verify input on the fly
            //
            LatLonInputVerifier inputVerifier = new LatLonInputVerifier();
            tfLat.getDocument().addDocumentListener(inputVerifier);
            tfLon.getDocument().addDocumentListener(inputVerifier);

            // select the text in the field on focus
            //
            TextFieldFocusHandler focusHandler = new TextFieldFocusHandler();
            tfLat.addFocusListener(focusHandler);
            tfLon.addFocusListener(focusHandler);
            return pnl;
        }

        protected JPanel buildButtonRow() {
            JPanel pnl = new JPanel(new FlowLayout());

            SideButton btn;
            pnl.add(btn = new SideButton(actOK = new OKAction()));
            makeButtonRespondToEnter(btn);
            pnl.add(btn = new SideButton(actCancel = new CancelAction()));
            makeButtonRespondToEnter(btn);
            pnl.add(new SideButton(new ContextSensitiveHelpAction(ht("/Action/AddNode"))));
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
            HelpUtil.setHelpContext(getRootPane(), ht("/Action/AddNode"));
        }

        public LatLonDialog(Component parent) {
            super(JOptionPane.getFrameForComponent(parent), true /* modal */);
            setTitle(tr("Add Node..."));
            build();
            addWindowListener(new WindowEventHandler());
            setCoordinates(null);
        }

        public void setCoordinates(LatLon coordinates) {
            if (coordinates == null) {
                coordinates = new LatLon(0,0);
            }
            this.coordinates = coordinates;
            tfLat.setText(coordinates.latToString(CoordinateFormat.DECIMAL_DEGREES));
            tfLon.setText(coordinates.lonToString(CoordinateFormat.DECIMAL_DEGREES));
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
            input = input.replaceAll("\u00B0", ""); // the degree symbol

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

        protected Double parseLatFromUserInput() {
            Double d = parseDoubleFromUserInput(tfLat.getText());
            if (d == null || ! LatLon.isValidLat(d)) {
                setErrorFeedback(tfLat, tr("Please enter a valid latitude in the range -90..90"));
                return null;
            } else {
                clearErrorFeedback(tfLat,tr("Please enter a latitude in the range -90..90"));
            }
            return d;
        }

        protected Double parseLonFromUserInput() {
            Double d = parseDoubleFromUserInput(tfLon.getText());
            if (d == null || ! LatLon.isValidLon(d)) {
                setErrorFeedback(tfLon, tr("Please enter a valid longitude in the range -180..180"));
                return null;
            } else {
                clearErrorFeedback(tfLon,tr("Please enter a longitude in the range -180..180"));
            }
            return d;
        }

        protected void parseUserInput() {
            Double lat = parseLatFromUserInput();
            Double lon = parseLonFromUserInput();
            if (lat == null || lon == null) {
                coordinates = null;
                actOK.setEnabled(false);
            } else {
                coordinates = new LatLon(lat,lon);
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
                putValue(SHORT_DESCRIPTION, tr("Close the dialog, don't create a new node"));
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
                tfLat.requestFocusInWindow();
            }
        }
    }
}
