// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;
/**
 * Tile selector.
 *
 * Provides a tile coordinate input field.
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class TileSelection implements DownloadSelection {

    private JTextField tileX0 = new JTextField(7);
    private JTextField tileY0 = new JTextField(7);
    private JTextField tileX1 = new JTextField(7);
    private JTextField tileY1 = new JTextField(7);
    private JSpinner tileZ = new JSpinner(new SpinnerNumberModel(12, 10, 18, 1));

    public void addGui(final DownloadDialog gui) {

        JPanel smpanel = new JPanel(new GridBagLayout());
        smpanel.add(new JLabel(tr("zoom level")), GBC.std().insets(0,0,10,0));
        smpanel.add(new JLabel(tr("x from")), GBC.std().insets(10,0,5,0));
        smpanel.add(tileX0, GBC.std());
        smpanel.add(new JLabel(tr("to")), GBC.std().insets(10,0,5,0));
        smpanel.add(tileX1, GBC.eol());
        smpanel.add(tileZ, GBC.std().insets(0,0,10,0));
        smpanel.add(new JLabel(tr("y from")), GBC.std().insets(10,0,5,0));
        smpanel.add(tileY0, GBC.std());
        smpanel.add(new JLabel(tr("to")), GBC.std().insets(10,0,5,0));
        smpanel.add(tileY1, GBC.eol());

        final FocusListener dialogUpdater = new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                try {
                    int zoomlvl = (Integer) tileZ.getValue();
                    int fromx = Integer.parseInt(tileX0.getText());
                    int tox = fromx;
                    if (tileX1.getText().length()>0) {
                        tox = Integer.parseInt(tileX1.getText());
                    }
                    if (tox<fromx) { int i = fromx; fromx=tox; tox=i; }

                    int fromy = Integer.parseInt(tileY0.getText());
                    int toy = fromy;
                    if (tileY1.getText().length()>0) {
                        toy = Integer.parseInt(tileY1.getText());
                    }
                    if (toy<fromy) { int i = fromy; fromy=toy; toy=i; }

                    Bounds b = new Bounds(
                            new LatLon(tileYToLat(zoomlvl, toy + 1), tileXToLon(zoomlvl, fromx)),
                            new LatLon(tileYToLat(zoomlvl, fromy), tileXToLon(zoomlvl, tox + 1))
                            );
                    gui.boundingBoxChanged(b, TileSelection.this);
                    //repaint();
                } catch (NumberFormatException x) {
                    // ignore
                }
            }
        };

        for (JTextField f : new JTextField[] { tileX0, tileX1, tileY0, tileY1 }) {
            f.setMinimumSize(new Dimension(100,new JTextField().getMinimumSize().height));
            f.addFocusListener(dialogUpdater);
        }

        gui.addDownloadAreaSelector(smpanel, tr("Tile Numbers"));
    }

    public void setDownloadArea(Bounds area) {
        if (area == null)
            return;
        int z = ((Integer) tileZ.getValue()).intValue();
        tileX0.setText(Integer.toString(lonToTileX(z, area.getMin().lon())));
        tileX1.setText(Integer.toString(lonToTileX(z, area.getMax().lon()-.00001)));
        tileY0.setText(Integer.toString(latToTileY(z, area.getMax().lat()-.00001)));
        tileY1.setText(Integer.toString(latToTileY(z, area.getMin().lat())));
    }

    public static int latToTileY(int zoom, double lat) {
        if ((zoom < 3) || (zoom > 18)) return -1;
        double l = lat / 180 * Math.PI;
        double pf = Math.log(Math.tan(l) + (1/Math.cos(l)));
        return (int) ((1<<(zoom-1)) * (Math.PI - pf) / Math.PI);
    }

    public static int lonToTileX(int zoom, double lon) {
        if ((zoom < 3) || (zoom > 18)) return -1;
        return (int) ((1<<(zoom-3)) * (lon + 180.0) / 45.0);
    }

    public static double tileYToLat(int zoom, int y) {
        if ((zoom < 3) || (zoom > 18)) return Double.MIN_VALUE;
        return Math.atan(Math.sinh(Math.PI - (Math.PI*y / (1<<(zoom-1))))) * 180 / Math.PI;
    }

    public static double tileXToLon(int zoom, int x) {
        if ((zoom < 3) || (zoom > 18)) return Double.MIN_VALUE;
        return x * 45.0 / (1<<(zoom-3)) - 180.0;

    }
}
