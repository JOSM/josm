// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.NameFinder;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Allows to jump to a specific location.
 * @since 2575
 */
public class JumpToAction extends JosmAction {

    private final JosmTextField url = new JosmTextField();
    private final JosmTextField place = new JosmTextField();
    private final JosmTextField lat = new JosmTextField();
    private final JosmTextField lon = new JosmTextField();
    private final JosmTextField zm = new JosmTextField();

    /**
     * Constructs a new {@code JumpToAction}.
     */
    public JumpToAction() {
        super(tr("Jump to Position"), (ImageProvider) null, tr("Opens a dialog that allows to jump to a specific location"),
                Shortcut.registerShortcut("tools:jumpto", tr("Tool: {0}", tr("Jump to Position")),
                        KeyEvent.VK_J, Shortcut.CTRL), true, "action/jumpto", false);
        // make this action listen to mapframe change events
        MainApplication.addMapFrameListener((o, n) -> updateEnabledState());

        setHelpId(ht("/Action/JumpToPosition"));
    }

    static class JumpToPositionDialog extends ExtendedDialog {
        JumpToPositionDialog(String[] buttons, JPanel panel) {
            super(MainApplication.getMainFrame(), tr("Jump to Position"), buttons);
            setButtonIcons("ok", "cancel");
            configureContextsensitiveHelp(ht("/Action/JumpToPosition"), true);
            setContent(panel);
            setCancelButton(2);
        }
    }

    class OsmURLListener implements DocumentListener {
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

    class OsmLonLatListener implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            updateUrl(false);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateUrl(false);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateUrl(false);
        }
    }

    /**
     * Displays the "Jump to" dialog.
     */
    public void showJumpToDialog() {
        if (!MainApplication.isDisplayingMapView()) {
            return;
        }
        MapView mv = MainApplication.getMap().mapView;

        final Optional<Bounds> boundsFromClipboard = Optional
                .ofNullable(ClipboardUtils.getClipboardStringContent())
                .map(OsmUrlToBounds::parse);
        if (boundsFromClipboard.isPresent() && Config.getPref().getBoolean("jumpto.use.clipboard", true)) {
            setBounds(boundsFromClipboard.get());
            place.setText("");
        } else {
            setBounds(mv.getState().getViewArea().getCornerBounds());
        }
        updateUrl(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("<html>"
                              + tr("Enter Lat/Lon to jump to position.")
                              + "<br>"
                              + tr("You can also paste an URL from www.openstreetmap.org")
                              + "<br>"
                              + "</html>"),
                  BorderLayout.NORTH);

        OsmLonLatListener x = new OsmLonLatListener();
        lat.getDocument().addDocumentListener(x);
        lon.getDocument().addDocumentListener(x);
        zm.getDocument().addDocumentListener(x);
        url.getDocument().addDocumentListener(new OsmURLListener());

        SelectAllOnFocusGainedDecorator.decorate(place);
        SelectAllOnFocusGainedDecorator.decorate(lat);
        SelectAllOnFocusGainedDecorator.decorate(lon);
        SelectAllOnFocusGainedDecorator.decorate(zm);
        SelectAllOnFocusGainedDecorator.decorate(url);

        JPanel p = new JPanel(new GridBagLayout());
        panel.add(p, BorderLayout.NORTH);

        p.add(new JLabel(tr("Enter a place name to search for")), GBC.eol());
        p.add(place, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(3, 5, 3, 5));

        p.add(new JLabel(tr("Latitude")), GBC.eol());
        p.add(lat, GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Longitude")), GBC.eol());
        p.add(lon, GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Zoom (in metres)")), GBC.eol());
        p.add(zm, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(3, 5, 3, 5));

        p.add(new JLabel(tr("URL")), GBC.eol());
        p.add(url, GBC.eol().fill(GBC.HORIZONTAL));

        String[] buttons = {tr("Jump there"), tr("Cancel")};
        LatLon ll = null;
        double zoomLvl = 100;
        while (ll == null) {
            final int option = new JumpToPositionDialog(buttons, panel).showDialog().getValue();

            if (option != 1) return;
            if (place.hasFocus() && !place.getText().trim().isEmpty()) {
                try {
                    List<NameFinder.SearchResult> searchResults = NameFinder.queryNominatim(place.getText());
                    if (!searchResults.isEmpty()) {
                        NameFinder.SearchResult searchResult = searchResults.get(0);
                        new Notification(tr("Jumping to: {0}", searchResult.getName()))
                                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                                .show();
                        mv.zoomTo(searchResult.getBounds());
                    }
                    return;
                } catch (IOException | RuntimeException ex) {
                    Logging.warn(ex);
                }
            }
            try {
                zoomLvl = Double.parseDouble(zm.getText());
                ll = new LatLon(Double.parseDouble(lat.getText()), Double.parseDouble(lon.getText()));
            } catch (NumberFormatException ex) {
                try {
                    ll = LatLonParser.parse(lat.getText() + "; " + lon.getText());
                } catch (IllegalArgumentException ex2) {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            tr("Could not parse Latitude, Longitude or Zoom. Please check."),
                            tr("Unable to parse Lon/Lat"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        double zoomFactor = 1/ mv.getDist100Pixel();
        mv.zoomToFactor(mv.getProjection().latlon2eastNorth(ll), zoomFactor * zoomLvl);
    }

    private void parseURL() {
        if (!url.hasFocus()) return;
        String urlText = url.getText();
        Bounds b = OsmUrlToBounds.parse(urlText);
        setBounds(b);
    }

    private void setBounds(Bounds b) {
        if (b != null) {
            final LatLon center = b.getCenter();
            lat.setText(Double.toString(center.lat()));
            lon.setText(Double.toString(center.lon()));
            zm.setText(Double.toString(OsmUrlToBounds.getZoom(b)));
        }
    }

    private void updateUrl(boolean force) {
        if (!lat.hasFocus() && !lon.hasFocus() && !zm.hasFocus() && !force) return;
        try {
            double dlat = Double.parseDouble(lat.getText());
            double dlon = Double.parseDouble(lon.getText());
            double zoomLvl = Double.parseDouble(zm.getText());
            url.setText(OsmUrlToBounds.getURL(dlat, dlon, (int) zoomLvl));
        } catch (NumberFormatException e) {
            Logging.debug(e.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        showJumpToDialog();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(MainApplication.isDisplayingMapView());
    }
}
