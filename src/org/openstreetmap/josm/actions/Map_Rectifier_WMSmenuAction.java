// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UrlLabel;

public class Map_Rectifier_WMSmenuAction extends JosmAction {
    /**
     * Class that bundles all required information of a rectifier service
     */
    public static class RectifierService {
        private final String name;
        private final String url;
        private final String wmsUrl;
        private final Pattern urlRegEx;
        private final Pattern idValidator;
        public JRadioButton btn;
        /**
         * @param name: Name of the rectifing service
         * @param url: URL to the service where users can register, upload, etc.
         * @param wmsUrl: URL to the WMS server where JOSM will grab the images. Insert __s__ where the ID should be placed
         * @param urlRegEx: a regular expression that determines if a given URL is one of the service and returns the WMS id if so
         * @param idValidator: regular expression that checks if a given ID is syntactically valid
         */
        public RectifierService(String name, String url, String wmsUrl, String urlRegEx, String idValidator) {
            this.name = name;
            this.url = url;
            this.wmsUrl = wmsUrl;
            this.urlRegEx = Pattern.compile(urlRegEx);
            this.idValidator = Pattern.compile(idValidator);
        }

        public boolean isSelected() {
            return btn.isSelected();
        }
    }

    /**
     * List of available rectifier services. May be extended from the outside
     */
    public ArrayList<RectifierService> services = new ArrayList<RectifierService>();

    public Map_Rectifier_WMSmenuAction() {
        super(tr("Rectified Image..."),
                "OLmarker",
                tr("Download Rectified Images From Various Services"),
                Shortcut.registerShortcut("wms:rectimg",
                        tr("WMS: {0}", tr("Rectified Image...")),
                        KeyEvent.VK_R,
                        Shortcut.GROUP_NONE),
                        true
        );

        // Add default services
        services.add(
                new RectifierService("Metacarta Map Rectifier",
                        "http://labs.metacarta.com/rectifier/",
                        "http://labs.metacarta.com/rectifier/wms.cgi?id=__s__&srs=EPSG:4326"
                        + "&Service=WMS&Version=1.1.0&Request=GetMap&format=image/png&",
                        // This matches more than the "classic" WMS link, so users can pretty much
                        // copy any link as long as it includes the ID
                        "labs\\.metacarta\\.com/(?:.*?)(?:/|=)([0-9]+)(?:\\?|/|\\.|$)",
                "^[0-9]+$")
        );
        services.add(
                // TODO: Change all links to mapwarper.net once the project has moved.
                // The RegEx already matches the new URL and old URLs will be forwarded
                // to make the transition as smooth as possible for the users
                new RectifierService("Geothings Map Warper",
                        "http://warper.geothings.net/",
                        "http://warper.geothings.net/maps/wms/__s__?request=GetMap&version=1.1.1"
                        + "&styles=&format=image/png&srs=epsg:4326&exceptions=application/vnd.ogc.se_inimage&",
                        // This matches more than the "classic" WMS link, so users can pretty much
                        // copy any link as long as it includes the ID
                        "(?:mapwarper\\.net|warper\\.geothings\\.net)/(?:.*?)/([0-9]+)(?:\\?|/|\\.|$)",
                "^[0-9]+$")
        );

        // This service serves the purpose of "just this once" without forcing the user
        // to commit the link to the preferences

        // Clipboard content gets trimmed, so matching whitespace only ensures that this
        // service will never be selected automatically.
        services.add(new RectifierService(tr("Custom WMS Link"), "", "", "^\\s+$", ""));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel(tr("Supported Rectifier Services:")), GBC.eol());

        JTextField tfWmsUrl = new JTextField(30);

        String clip = getClipboardContents();
        ButtonGroup group = new ButtonGroup();

        JRadioButton firstBtn = null;
        for(RectifierService s : services) {
            JRadioButton serviceBtn = new JRadioButton(s.name);
            if(firstBtn == null) {
                firstBtn = serviceBtn;
            }
            // Checks clipboard contents against current service if no match has been found yet.
            // If the contents match, they will be inserted into the text field and the corresponding
            // service will be pre-selected.
            if(!clip.equals("") && tfWmsUrl.getText().equals("")
                    && (s.urlRegEx.matcher(clip).find() || s.idValidator.matcher(clip).matches())) {
                serviceBtn.setSelected(true);
                tfWmsUrl.setText(clip);
            }
            s.btn = serviceBtn;
            group.add(serviceBtn);
            if(!s.url.equals("")) {
                panel.add(serviceBtn, GBC.std());
                panel.add(new UrlLabel(s.url, tr("Visit Homepage")), GBC.eol().anchor(GridBagConstraints.EAST));
            } else {
                panel.add(serviceBtn, GBC.eol().anchor(GridBagConstraints.WEST));
            }
        }

        // Fallback in case no match was found
        if(tfWmsUrl.getText().equals("") && firstBtn != null) {
            firstBtn.setSelected(true);
        }

        panel.add(new JLabel(tr("WMS URL or Image ID:")), GBC.eol());
        panel.add(tfWmsUrl, GBC.eol().fill(GridBagConstraints.HORIZONTAL));

        ExtendedDialog diag = new ExtendedDialog(Main.parent,
                tr("Add Rectified Image"),

                new String[] {tr("Add Rectified Image"), tr("Cancel")});
        diag.setContent(panel);
        diag.setButtonIcons(new String[] {"OLmarker.png", "cancel.png"});

        // This repeatedly shows the dialog in case there has been an error.
        // The loop is break;-ed if the users cancels
        outer: while(true) {
            diag.showDialog();
            int answer = diag.getValue();
            // Break loop when the user cancels
            if(answer != 1) {
                break;
            }

            String text = tfWmsUrl.getText().trim();
            // Loop all services until we find the selected one
            for(RectifierService s : services) {
                if(!s.isSelected()) {
                    continue;
                }

                // We've reached the custom WMS URL service
                // Just set the URL and hope everything works out
                if(s.wmsUrl.equals("")) {
                    addWMSLayer(s.name + " (" + text + ")", text);
                    break outer;
                }

                // First try to match if the entered string as an URL
                Matcher m = s.urlRegEx.matcher(text);
                if(m.find()) {
                    String id = m.group(1);
                    String newURL = s.wmsUrl.replaceAll("__s__", id);
                    String title = s.name + " (" + id + ")";
                    addWMSLayer(title, newURL);
                    break outer;
                }
                // If not, look if it's a valid ID for the selected service
                if(s.idValidator.matcher(text).matches()) {
                    String newURL = s.wmsUrl.replaceAll("__s__", text);
                    String title = s.name + " (" + text + ")";
                    addWMSLayer(title, newURL);
                    break outer;
                }

                // We've found the selected service, but the entered string isn't suitable for
                // it. So quit checking the other radio buttons
                break;
            }

            // and display an error message. The while(true) ensures that the dialog pops up again
            JOptionPane.showMessageDialog(Main.parent,
                    tr("Couldn't match the entered link or id to the selected service. Please try again."),
                    tr("No valid WMS URL or id"),
                    JOptionPane.ERROR_MESSAGE);
            diag.setVisible(true);
        }
    }

    /**
     * Adds a WMS Layer with given title and URL
     * @param title: Name of the layer as it will shop up in the layer manager
     * @param url: URL to the WMS server
     */
    private void addWMSLayer(String title, String url) {
        Main.main.addLayer(new WMSLayer(new ImageryInfo(title, url)));
    }

    /**
     * Helper function that extracts a String from the Clipboard if available.
     * Returns an empty String otherwise
     * @return String Clipboard contents if available
     */
    private String getClipboardContents() {
        String result = "";
        Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

        if(contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor))
            return "";

        try {
            result = (String)contents.getTransferData(DataFlavor.stringFlavor);
        } catch(Exception ex) {
            return "";
        }
        return result.trim();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null && Main.map.mapView != null && !Main.map.mapView.getAllLayers().isEmpty());
    }
}
