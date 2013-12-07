// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Allows to jump to a specific location.
 * @since 2575
 */
public class JumpToAction extends JosmAction {
    
    /**
     * Constructs a new {@code JumpToAction}.
     */
    public JumpToAction() {
        super(tr("Jump To Position"), (Icon) null, tr("Opens a dialog that allows to jump to a specific location"), 
                Shortcut.registerShortcut("tools:jumpto", tr("Tool: {0}", tr("Jump To Position")),
                        KeyEvent.VK_J, Shortcut.CTRL), true, "action/jumpto", true);
    }

    private final JosmTextField url = new JosmTextField();
    private final JosmTextField lat = new JosmTextField();
    private final JosmTextField lon = new JosmTextField();
    private final JosmTextField zm = new JosmTextField();

    /**
     * Displays the "Jump to" dialog.
     */
    public void showJumpToDialog() {
        if (!Main.isDisplayingMapView()) {
            return;
        }
        MapView mv = Main.map.mapView;
        LatLon curPos = mv.getProjection().eastNorth2latlon(mv.getCenter());
        lat.setText(Double.toString(curPos.lat()));
        lon.setText(Double.toString(curPos.lon()));

        double dist = mv.getDist100Pixel();
        double zoomFactor = 1/dist;

        zm.setText(Long.toString(Math.round(dist*100)/100));
        updateUrl(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("<html>"
                              + tr("Enter Lat/Lon to jump to position.")
                              + "<br>"
                              + tr("You can also paste an URL from www.openstreetmap.org")
                              + "<br>"
                              + "</html>"),
                  BorderLayout.NORTH);

        class OsmURLListener implements DocumentListener {
            @Override public void changedUpdate(DocumentEvent e) { parseURL(); }
            @Override public void insertUpdate(DocumentEvent e) { parseURL(); }
            @Override public void removeUpdate(DocumentEvent e) { parseURL(); }
        }

        class OsmLonLatListener implements DocumentListener {
            @Override public void changedUpdate(DocumentEvent e) { updateUrl(false); }
            @Override public void insertUpdate(DocumentEvent e) { updateUrl(false); }
            @Override public void removeUpdate(DocumentEvent e) { updateUrl(false); }
        }

        OsmLonLatListener x = new OsmLonLatListener();
        lat.getDocument().addDocumentListener(x);
        lon.getDocument().addDocumentListener(x);
        zm.getDocument().addDocumentListener(x);
        url.getDocument().addDocumentListener(new OsmURLListener());

        JPanel p = new JPanel(new GridBagLayout());
        panel.add(p, BorderLayout.NORTH);

        p.add(new JLabel(tr("Latitude")), GBC.eol());
        p.add(lat, GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Longitude")), GBC.eol());
        p.add(lon, GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Zoom (in metres)")), GBC.eol());
        p.add(zm, GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("URL")), GBC.eol());
        p.add(url, GBC.eol().fill(GBC.HORIZONTAL));

        Object[] buttons = { tr("Jump there"), tr("Cancel") };
        LatLon ll = null;
        double zoomLvl = 100;
        while(ll == null) {
            int option = JOptionPane.showOptionDialog(
                            Main.parent,
                            panel,
                            tr("Jump to Position"),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            buttons,
                            buttons[0]);

            if (option != JOptionPane.OK_OPTION) return;
            try {
                zoomLvl = Double.parseDouble(zm.getText());
                ll = new LatLon(Double.parseDouble(lat.getText()), Double.parseDouble(lon.getText()));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(Main.parent, tr("Could not parse Latitude, Longitude or Zoom. Please check."), tr("Unable to parse Lon/Lat"), JOptionPane.ERROR_MESSAGE);
            }
        }

        mv.zoomToFactor(mv.getProjection().latlon2eastNorth(ll), zoomFactor * zoomLvl);
    }

    private void parseURL() {
        if (!url.hasFocus()) return;
        String urlText = url.getText();
        Bounds b = OsmUrlToBounds.parse(urlText);
        if (b != null) {
            lat.setText(Double.toString((b.getMinLat() + b.getMaxLat())/2));
            lon.setText(Double.toString((b.getMinLon() + b.getMaxLon())/2));

            int zoomLvl = 16;
            int hashIndex = urlText.indexOf("#map");
            if (hashIndex >= 0) {
                zoomLvl = Integer.parseInt(urlText.substring(hashIndex+5, urlText.indexOf('/', hashIndex)));
            } else {
                String[] args = urlText.substring(urlText.indexOf('?')+1).split("&");
                for (String arg : args) {
                    int eq = arg.indexOf('=');
                    if (eq == -1 || !arg.substring(0, eq).equalsIgnoreCase("zoom")) continue;
    
                    zoomLvl = Integer.parseInt(arg.substring(eq + 1));
                    break;
                }
            }

            // 10 000 000 = 10 000 * 1000 = World * (km -> m)
            zm.setText(Double.toString(Math.round(10000000 * Math.pow(2, (-1) * zoomLvl))));
        }
    }

    private void updateUrl(boolean force) {
        if(!lat.hasFocus() && !lon.hasFocus() && !zm.hasFocus() && !force) return;
        try {
            double dlat = Double.parseDouble(lat.getText());
            double dlon = Double.parseDouble(lon.getText());
            double m = Double.parseDouble(zm.getText());
            // Inverse function to the one above. 18 is the current maximum zoom
            // available on standard renderers, so choose this is in case m
            // should be zero
            int zoomLvl = 18;
            if(m > 0)
                zoomLvl = (int)Math.round((-1) * Math.log(m/10000000)/Math.log(2));

            url.setText(OsmUrlToBounds.getURL(dlat, dlon, zoomLvl));
        } catch (NumberFormatException x) {
            Main.debug(x.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        showJumpToDialog();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.isDisplayingMapView());
    }

    @Override
    protected void installAdapters() {
        super.installAdapters();
        // make this action listen to mapframe change events
        Main.addMapFrameListener(new MapFrameListener() {
            @Override
            public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
                updateEnabledState();
            }
        });
    }
}
