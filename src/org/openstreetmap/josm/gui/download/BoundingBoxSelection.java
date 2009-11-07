// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Bounding box selector.
 *
 * Provides max/min lat/lon input fields as well as the "URL from www.openstreetmap.org" text field.
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class BoundingBoxSelection implements DownloadSelection {

    private JTextField[] latlon = null;
    private final JTextArea tfOsmUrl = new JTextArea();
    private final JTextArea showUrl = new JTextArea();
    private DownloadDialog parent;


    protected void buildDownloadAreaInputFields() {
        latlon = new JTextField[4];
        for(int i=0; i< 4; i++) {
            latlon[i] = new JTextField(11);
            latlon[i].setMinimumSize(new Dimension(100,new JTextField().getMinimumSize().height));
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
    }

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

        dlg.add(new JLabel(tr("min lat")), GBC.std().insets(10,20,5,0));
        dlg.add(latlon[0], GBC.std().insets(0,20,0,0));
        dlg.add(new JLabel(tr("min lon")), GBC.std().insets(10,20,5,0));
        dlg.add(latlon[1], GBC.eol().insets(0,20,0,0));
        dlg.add(new JLabel(tr("max lat")), GBC.std().insets(10,0,5,0));
        dlg.add(latlon[2], GBC.std());
        dlg.add(new JLabel(tr("max lon")), GBC.std().insets(10,0,5,0));
        dlg.add(latlon[3], GBC.eol());

        dlg.add(new JLabel(tr("URL from www.openstreetmap.org (you can paste an URL here to download the area)")), GBC.eol().insets(10,20,5,0));
        dlg.add(tfOsmUrl, GBC.eop().insets(10,0,5,0).fill());
        tfOsmUrl.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        checkPopup(e);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        checkPopup(e);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        checkPopup(e);
                    }

                    private void checkPopup(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            OsmUrlPopup popup = new OsmUrlPopup();
                            popup.show(tfOsmUrl, e.getX(), e.getY());
                        }
                    }
                }
        );
        dlg.add(showUrl, GBC.eop().insets(10,0,5,5));
        showUrl.setEditable(false);
        showUrl.setBackground(dlg.getBackground());
        showUrl.addFocusListener(new SelectAllOnFocusHandler(showUrl));

        gui.addDownloadAreaSelector(dlg, tr("Bounding Box"));
        this.parent = gui;
    }


    public void setDownloadArea(Bounds area) {
        updateBboxFields(area);
        updateUrl(area);
    }

    public Bounds getDownloadArea() {
        double[] values = new double[4];
        for (int i=0; i < 4; i++) {
            try {
                values[i] = Double.parseDouble(latlon[i].getText());
            } catch(NumberFormatException x) {
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
        if(b == null) return false;
        gui.boundingBoxChanged(b,BoundingBoxSelection.this);
        updateBboxFields(b);
        updateUrl(b);
        return true;
    }

    private void updateBboxFields(Bounds area) {
        if (area == null) return;
        latlon[0].setText(Double.toString(area.getMin().lat()));
        latlon[1].setText(Double.toString(area.getMin().lon()));
        latlon[2].setText(Double.toString(area.getMax().lat()));
        latlon[3].setText(Double.toString(area.getMax().lon()));
        for (JTextField f : latlon) {
            f.setCaretPosition(0);
        }
    }

    private void updateUrl(Bounds area) {
        if (area == null) return;
        showUrl.setText(OsmUrlToBounds.getURL(area));
    }


    class LatValueChecker extends FocusAdapter implements ActionListener{
        private JTextField tfLatValue;

        private Border errorBorder = BorderFactory.createLineBorder(Color.RED, 1);
        protected void setErrorMessage(String msg) {
            if (msg != null) {
                tfLatValue.setBorder(errorBorder);
                tfLatValue.setToolTipText(msg);
            } else {
                tfLatValue.setBorder(UIManager.getBorder("TextField.border"));
                tfLatValue.setToolTipText("");
            }
        }

        public LatValueChecker(JTextField tfLatValue) {
            this.tfLatValue = tfLatValue;
        }

        protected void check() {
            double value = 0;
            try {
                value = Double.parseDouble(tfLatValue.getText());
            } catch(NumberFormatException ex) {
                setErrorMessage(tr("The string ''{0}'' isn''t a valid double value.", tfLatValue.getText()));
                return;
            }
            if (!LatLon.isValidLat(value)) {
                setErrorMessage(tr("Value for latitude in range [-90,90] required.", tfLatValue.getText()));
                return;
            }
            setErrorMessage(null);
        }

        @Override
        public void focusLost(FocusEvent e) {
            check();
        }

        public void actionPerformed(ActionEvent e) {
            check();
        }
    }

    class LonValueChecker extends FocusAdapter implements ActionListener {
        private JTextField tfLonValue;
        private Border errorBorder = BorderFactory.createLineBorder(Color.RED, 1);
        protected void setErrorMessage(String msg) {
            if (msg != null) {
                tfLonValue.setBorder(errorBorder);
                tfLonValue.setToolTipText(msg);
            } else {
                tfLonValue.setBorder(UIManager.getBorder("TextField.border"));
                tfLonValue.setToolTipText("");
            }
        }

        public LonValueChecker(JTextField tfLonValue) {
            this.tfLonValue = tfLonValue;
        }

        protected void check() {
            double value = 0;
            try {
                value = Double.parseDouble(tfLonValue.getText());
            } catch(NumberFormatException ex) {
                setErrorMessage(tr("The string ''{0}'' isn''t a valid double value.", tfLonValue.getText()));
                return;
            }
            if (!LatLon.isValidLon(value)) {
                setErrorMessage(tr("Value for longitude in range [-180,180] required.", tfLonValue.getText()));
                return;
            }
            setErrorMessage(null);
        }

        @Override
        public void focusLost(FocusEvent e) {
            check();
        }

        public void actionPerformed(ActionEvent e) {
            check();
        }
    }

    class SelectAllOnFocusHandler extends FocusAdapter {
        private JTextComponent tfTarget;
        public SelectAllOnFocusHandler(JTextComponent tfTarget) {
            this.tfTarget = tfTarget;
        }

        @Override
        public void focusGained(FocusEvent e) {
            tfTarget.selectAll();
        }
    }

    class OsmUrlRefresher implements DocumentListener {
        public void changedUpdate(DocumentEvent e) { parseURL(parent); }
        public void insertUpdate(DocumentEvent e) { parseURL(parent); }
        public void removeUpdate(DocumentEvent e) { parseURL(parent); }
    }

    class PasteUrlAction extends AbstractAction implements FlavorListener {

        public PasteUrlAction() {
            putValue(NAME, tr("Paste"));
            putValue(SMALL_ICON, ImageProvider.get("paste"));
            putValue(SHORT_DESCRIPTION, tr("Paste URL from clipboard"));
            Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(this);
        }

        protected String getClipboardContent() {
            Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            try {
                if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String text = (String)t.getTransferData(DataFlavor.stringFlavor);
                    return text;
                }
            } catch (UnsupportedFlavorException ex) {
                ex.printStackTrace();
                return null;
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
            return null;
        }

        public void actionPerformed(ActionEvent e) {
            String content = getClipboardContent();
            if (content != null) {
                tfOsmUrl.setText(content);
            }
        }

        protected void updateEnabledState() {
            setEnabled(getClipboardContent() != null);
        }

        public void flavorsChanged(FlavorEvent e) {
            updateEnabledState();
        }
    }

    class OsmUrlPopup extends JPopupMenu {
        public OsmUrlPopup() {
            add(new PasteUrlAction());
        }
    }
}
