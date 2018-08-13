// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Collections;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Marker representing an image. Uses a special icon, and when clicked,
 * displays an image view dialog. Re-uses some code from GeoImageLayer.
 *
 * @author Frederik Ramm
 *
 */
public class ImageMarker extends ButtonMarker {

    public URL imageUrl;

    public ImageMarker(LatLon ll, URL imageUrl, MarkerLayer parentLayer, double time, double offset) {
        super(ll, /* ICON(markers/) */ "photo", parentLayer, time, offset);
        this.imageUrl = imageUrl;
    }

    @Override public void actionPerformed(ActionEvent ev) {
        final JPanel p = new JPanel(new BorderLayout());
        final JScrollPane scroll = new JScrollPane(new JLabel(loadScaledImage(imageUrl, 580)));
        final JViewport vp = scroll.getViewport();
        p.add(scroll, BorderLayout.CENTER);

        final JToggleButton scale = new JToggleButton(ImageProvider.get("misc", "rectangle"));

        JPanel p2 = new JPanel();
        p2.add(scale);
        p.add(p2, BorderLayout.SOUTH);
        scale.addActionListener(ev1 -> {
            p.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if (scale.getModel().isSelected()) {
                ((JLabel) vp.getView()).setIcon(loadScaledImage(imageUrl, Math.max(vp.getWidth(), vp.getHeight())));
            } else {
                ((JLabel) vp.getView()).setIcon(new ImageIcon(imageUrl));
            }
            p.setCursor(Cursor.getDefaultCursor());
        });
        scale.setSelected(true);
        JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE);
        if (!GraphicsEnvironment.isHeadless()) {
            JDialog dlg = pane.createDialog(MainApplication.getMainFrame(), imageUrl.toString());
            dlg.setModal(false);
            dlg.toFront();
            dlg.setVisible(true);
        }
    }

    private static Icon loadScaledImage(URL u, int maxSize) {
        Image img = new ImageIcon(u).getImage();
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w > h) {
            h = (int) Math.round(maxSize*((double) h/w));
            w = maxSize;
        } else {
            w = (int) Math.round(maxSize*((double) w/h));
            h = maxSize;
        }
        return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }

    @Override
    public WayPoint convertToWayPoint() {
        WayPoint wpt = super.convertToWayPoint();
        GpxLink link = new GpxLink(imageUrl.toString());
        link.type = "image";
        wpt.put(GpxConstants.META_LINKS, Collections.singleton(link));
        return wpt;
    }
}
