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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
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

    /**
     * Constructs a new {@code ImageryAdjustAction} for the given layer.
     * @param layer The imagery layer
     */
    public ImageryAdjustAction(ImageryLayer layer) {
        super(tr("New offset"), "adjustimg",
                tr("Adjust the position of this imagery layer"), Main.map,
                cursor);
        putValue("toolbar", false);
        this.layer = layer;
    }

    @Override
    public void enterMode() {
        super.enterMode();
        if (layer == null)
            return;
        if (!layer.isVisible()) {
            layer.setVisible(true);
        }
        oldDx = layer.getDx();
        oldDy = layer.getDy();
        addListeners();
        offsetDialog = new ImageryOffsetDialog();
        offsetDialog.setVisible(true);
    }

    protected void addListeners() {
        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            Main.error(ex);
        }
    }

    @Override
    public void exitMode() {
        super.exitMode();
        if (offsetDialog != null) {
            layer.setOffset(oldDx, oldDy);
            offsetDialog.setVisible(false);
            offsetDialog = null;
        }
        removeListeners();
    }

    protected void removeListeners() {
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
            Main.error(ex);
        }
        if (Main.isDisplayingMapView()) {
            Main.map.mapView.removeMouseMotionListener(this);
            Main.map.mapView.removeMouseListener(this);
        }
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (!(event instanceof KeyEvent)) return;
        if (event.getID() != KeyEvent.KEY_PRESSED) return;
        if (layer == null) return;
        if (offsetDialog != null && offsetDialog.areFieldsInFocus()) return;
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

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (layer.isVisible()) {
            requestFocusInMapView();
            prevEastNorth=Main.map.mapView.getEastNorth(e.getX(),e.getY());
            Main.map.mapView.setNewCursor(Cursor.MOVE_CURSOR, this);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
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

    @Override
    public void mouseReleased(MouseEvent e) {
        Main.map.mapView.repaint();
        Main.map.mapView.resetCursor(this);
        prevEastNorth = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (offsetDialog != null || layer == null || Main.map == null)
            return;
        oldMapMode = Main.map.mapMode;
        super.actionPerformed(e);
    }

    class ImageryOffsetDialog extends ExtendedDialog implements FocusListener {
        public final JosmTextField tOffset = new JosmTextField();
        JosmTextField tBookmarkName = new JosmTextField();
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
                    "If you want to save the offset as bookmark, enter the bookmark name below",Main.getProjection().toString())), GBC.eop());
            pnl.add(new JLabel(tr("Offset: ")),GBC.std());
            pnl.add(tOffset,GBC.eol().fill(GBC.HORIZONTAL).insets(0,0,0,5));
            pnl.add(new JLabel(tr("Bookmark name: ")),GBC.std());
            pnl.add(tBookmarkName,GBC.eol().fill(GBC.HORIZONTAL));
            tOffset.setColumns(16);
            updateOffsetIntl();
            tOffset.addFocusListener(this);
            setContent(pnl);
            setupDialog();
        }

        public boolean areFieldsInFocus() {
            return tOffset.hasFocus();
        }

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (ignoreListener) return;
            String ostr = tOffset.getText();
            int semicolon = ostr.indexOf(';');
            if( semicolon >= 0 && semicolon + 1 < ostr.length() ) {
                try {
                    // here we assume that Double.parseDouble() needs '.' as a decimal separator
                    String easting = ostr.substring(0, semicolon).trim().replace(',', '.');
                    String northing = ostr.substring(semicolon + 1).trim().replace(',', '.');
                    double dx = Double.parseDouble(easting);
                    double dy = Double.parseDouble(northing);
                    layer.setOffset(dx, dy);
                } catch (NumberFormatException nfe) {
                    // we repaint offset numbers in any case
                }
            }
            updateOffsetIntl();
            if (Main.isDisplayingMapView()) {
                Main.map.repaint();
            }
        }

        public void updateOffset() {
            ignoreListener = true;
            updateOffsetIntl();
            ignoreListener = false;
        }

        public void updateOffsetIntl() {
            // Support projections with very small numbers (e.g. 4326)
            int precision = Main.getProjection().getDefaultZoomInPPD() >= 1.0 ? 2 : 7;
            // US locale to force decimal separator to be '.'
            tOffset.setText(new java.util.Formatter(java.util.Locale.US).format(
                    "%1." + precision + "f; %1." + precision + "f",
                    layer.getDx(), layer.getDy()).toString());
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
            if (buttonIndex == 0 && tBookmarkName.getText() != null && !tBookmarkName.getText().isEmpty() &&
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
            } else if (tBookmarkName.getText() != null && !tBookmarkName.getText().isEmpty()) {
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

    @Override
    public void destroy() {
        super.destroy();
        removeListeners();
        this.layer = null;
        this.oldMapMode = null;
    }
}
