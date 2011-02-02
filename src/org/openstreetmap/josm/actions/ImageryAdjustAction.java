// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;


public class ImageryAdjustAction extends MapMode implements MouseListener, MouseMotionListener, AWTEventListener{
    static ImageryOffsetDialog offsetDialog;
    static Cursor cursor = ImageProvider.getCursor("normal", "move");

    double oldDx, oldDy;
    boolean mouseDown;
    EastNorth prevEastNorth;
    private ImageryLayer layer;
    private MapMode oldMapMode;

    public ImageryAdjustAction(ImageryLayer layer) {
        super(tr("New offset"), "adjustimg",
                tr("Adjust the position of this imagery layer"), Main.map,
                cursor);
        this.layer = layer;
    }

    @Override public void enterMode() {
        super.enterMode();
        if (layer == null)
            return;
        if (!layer.isVisible()) {
            layer.setVisible(true);
        }
        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        oldDx = layer.getDx();
        oldDy = layer.getDy();
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
        }
        offsetDialog = new ImageryOffsetDialog();
        offsetDialog.setVisible(true);
    }

    @Override public void exitMode() {
        super.exitMode();
        if (offsetDialog != null) {
            layer.setOffset(oldDx, oldDy);
            offsetDialog.setVisible(false);
            offsetDialog = null;
        }
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
        }
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (!(event instanceof KeyEvent)) return;
        if (event.getID() != KeyEvent.KEY_PRESSED) return;
        if (layer == null) return;
        KeyEvent kev = (KeyEvent)event;
        double dx = 0, dy = 0;
        switch (kev.getKeyCode()) {
        case KeyEvent.VK_UP : dy = +1; break;
        case KeyEvent.VK_DOWN : dy = -1; break;
        case KeyEvent.VK_LEFT : dx = -1; break;
        case KeyEvent.VK_RIGHT : dx = +1; break;
        }
        if (dx != 0 || dy != 0) {
            double ppd = layer.getPPD();
            layer.displace(dx / ppd, dy / ppd);
            if (offsetDialog != null) {
                offsetDialog.updateOffset();
            }
            kev.consume();
            Main.map.repaint();
        }
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (layer.isVisible()) {
            prevEastNorth=Main.map.mapView.getEastNorth(e.getX(),e.getY());
            Main.map.mapView.setCursor
            (Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        if (layer == null || prevEastNorth == null) return;
        EastNorth eastNorth =
            Main.map.mapView.getEastNorth(e.getX(),e.getY());
        double dx = layer.getDx()+eastNorth.east()-prevEastNorth.east();
        double dy = layer.getDy()+eastNorth.north()-prevEastNorth.north();
        layer.setOffset(dx, dy);
        if (offsetDialog != null) {
            offsetDialog.updateOffset();
        }
        Main.map.repaint();
        prevEastNorth = eastNorth;
    }

    @Override public void mouseReleased(MouseEvent e) {
        Main.map.mapView.repaint();
        Main.map.mapView.setCursor(Cursor.getDefaultCursor());
        prevEastNorth = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (offsetDialog != null || layer == null || Main.map == null)
            return;
        oldMapMode = Main.map.mapMode;
        layer.enableOffsetServer(false);
        super.actionPerformed(e);
    }

    class ImageryOffsetDialog extends ExtendedDialog implements PropertyChangeListener {
        public final JFormattedTextField easting = new JFormattedTextField(new DecimalFormat("0.00000E0"));
        public final JFormattedTextField northing = new JFormattedTextField(new DecimalFormat("0.00000E0"));
        JTextField tBookmarkName = new JTextField();
        private boolean ignoreListener;
        public ImageryOffsetDialog() {
            super(Main.parent,
                    tr("Adjust imagery offset"),
                    new String[] { tr("OK"),tr("Cancel") },
                    false);
            setButtonIcons(new String[] { "ok", "cancel" });
            contentInsets = new Insets(10, 15, 5, 15);
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.add(new JMultilineLabel(tr("Use arrow keys or drag the imagery layer with mouse to adjust the imagery offset.\n" +
                    "You can also enter east and north offset in the {0} coordinates.\n" +
                    "If you want to save the offset as bookmark, enter the bookmark name below",Main.proj.toString())), GBC.eop());
            pnl.add(new JLabel(tr("Easting") + ": "),GBC.std());
            pnl.add(easting,GBC.std().fill(GBC.HORIZONTAL).insets(0, 0, 5, 0));
            pnl.add(new JLabel(tr("Northing") + ": "),GBC.std());
            pnl.add(northing,GBC.eol().fill(GBC.HORIZONTAL));
            pnl.add(new JLabel(tr("Bookmark name: ")),GBC.eol().insets(0,5,0,0));
            pnl.add(tBookmarkName,GBC.eol().fill(GBC.HORIZONTAL));
            easting.setColumns(8);
            northing.setColumns(8);
            easting.setValue(layer.getDx());
            northing.setValue(layer.getDy());
            easting.addPropertyChangeListener("value",this);
            northing.addPropertyChangeListener("value",this);
            setContent(pnl);
            setupDialog();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ignoreListener) return;
            layer.setOffset(((Number)easting.getValue()).doubleValue(), ((Number)northing.getValue()).doubleValue());
            Main.map.repaint();
        }

        public void updateOffset() {
            ignoreListener = true;
            easting.setValue(layer.getDx());
            northing.setValue(layer.getDy());
            ignoreListener = false;
        }

        private boolean confirmOverwriteBookmark() {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Overwrite"),
                    new String[] {tr("Overwrite"), tr("Cancel")}
            ) {{
                contentInsets = new Insets(10, 15, 10, 15);
            }};
            dialog.setContent(tr("Offset bookmark already exists. Overwrite?"));
            dialog.setButtonIcons(new String[] {"ok.png", "cancel.png"});
            dialog.setupDialog();
            dialog.setVisible(true);
            return dialog.getValue() == 1;
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            if (buttonIndex == 0 && tBookmarkName.getText() != null && !"".equals(tBookmarkName.getText()) &&
                    OffsetBookmark.getBookmarkByName(layer, tBookmarkName.getText()) != null) {
                if (!confirmOverwriteBookmark()) return;
            }
            super.buttonAction(buttonIndex, evt);
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) return;
            offsetDialog = null;
            if (getValue() != 1) {
                layer.setOffset(oldDx, oldDy);
            } else if (tBookmarkName.getText() != null && !"".equals(tBookmarkName.getText())) {
                OffsetBookmark.bookmarkOffset(tBookmarkName.getText(), layer);
            }
            Main.main.menu.imageryMenu.refreshOffsetMenu();
            if (Main.map == null) return;
            if (oldMapMode != null) {
                Main.map.selectMapMode(oldMapMode);
                oldMapMode = null;
            } else {
                Main.map.selectSelectTool(false);
            }
        }
    }
}
