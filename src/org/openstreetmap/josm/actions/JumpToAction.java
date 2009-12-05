// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.Shortcut;

public class JumpToAction extends JosmAction implements MouseListener {
    public JumpToAction() {
        super(tr("Jump To Position"), null, tr("Opens a dialog that allows to jump to a specific location"), Shortcut.registerShortcut("tools:jumpto", tr("Tool: {0}", tr("Jump To Position")),
        KeyEvent.VK_G, Shortcut.GROUP_HOTKEY), true);
    }

    private JTextField url = new JTextField();
    private JTextField lat = new JTextField();
    private JTextField lon = new JTextField();
    private JTextField zm = new JTextField();

    private double zoomFactor = 0;
    public void showJumpToDialog() {
        MapView mv = Main.map.mapView;
        if(mv == null)
            return;
        LatLon curPos=mv.getProjection().eastNorth2latlon(mv.getCenter());
        lat.setText(java.lang.Double.toString(curPos.lat()));
        lon.setText(java.lang.Double.toString(curPos.lon()));

        double dist = mv.getDist100Pixel();
        zoomFactor = 1/dist;

        zm.setText(java.lang.Long.toString(Math.round(dist*100)/100));
        updateUrl(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("<html>"
                              + tr("Enter Lat/Lon to jump to position.")
                              + "<br>"
                              + tr("You can also paste an URL from www.openstreetmap.org")
                              + "<br>"
                              + "</html>"),
                  BorderLayout.NORTH);

        class osmURLListener implements DocumentListener {
            public void changedUpdate(DocumentEvent e) { parseURL(); }
            public void insertUpdate(DocumentEvent e) { parseURL(); }
            public void removeUpdate(DocumentEvent e) { parseURL(); }
        }

        class osmLonLatListener implements DocumentListener {
            public void changedUpdate(DocumentEvent e) { updateUrl(false); }
            public void insertUpdate(DocumentEvent e) { updateUrl(false); }
            public void removeUpdate(DocumentEvent e) { updateUrl(false); }
        }

        osmLonLatListener x=new osmLonLatListener();
        lat.getDocument().addDocumentListener(x);
        lon.getDocument().addDocumentListener(x);
        zm.getDocument().addDocumentListener(x);
        url.getDocument().addDocumentListener(new osmURLListener());

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
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(Main.parent, tr("Could not parse Latitude, Longitude or Zoom. Please check."), tr("Unable to parse Lon/Lat"), JOptionPane.ERROR_MESSAGE);
            }
        }

        mv.zoomToFactor(mv.getProjection().latlon2eastNorth(ll), zoomFactor * zoomLvl);
    }

    private void parseURL() {
        if(!url.hasFocus()) return;
        Bounds b = OsmUrlToBounds.parse(url.getText());
        if (b != null) {
            lat.setText(Double.toString((b.getMin().lat() + b.getMax().lat())/2));
            lon.setText(Double.toString((b.getMin().lon() + b.getMax().lon())/2));

            int zoomLvl = 16;
            String[] args = url.getText().substring(url.getText().indexOf('?')+1).split("&");
            for (String arg : args) {
                int eq = arg.indexOf('=');
                if (eq == -1 || !arg.substring(0, eq).equalsIgnoreCase("zoom")) continue;

                zoomLvl = Integer.parseInt(arg.substring(eq + 1));
                break;
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

            int decimals = (int) Math.pow(10, (zoomLvl / 3));
            dlat = Math.round(dlat * decimals);
            dlat /= decimals;
            dlon = Math.round(dlon * decimals);
            dlon /= decimals;
            url.setText("http://www.openstreetmap.org/?lat="+dlat+"&lon="+dlon+"&zoom="+zoomLvl);
        } catch (NumberFormatException x) {}
    }

    public void actionPerformed(ActionEvent e) {
        showJumpToDialog();
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {
        showJumpToDialog();
    }
}
