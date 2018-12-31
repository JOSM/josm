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
import java.util.Formatter;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Adjust the position of an imagery layer.
 * @since 3715
 */
public class ImageryAdjustAction extends MapMode implements AWTEventListener {
    private static volatile ImageryOffsetDialog offsetDialog;
    private static Cursor cursor = ImageProvider.getCursor("normal", "move");

    private OffsetBookmark old;
    private OffsetBookmark tempOffset;
    private EastNorth prevEastNorth;
    private transient AbstractTileSourceLayer<?> layer;
    private MapMode oldMapMode;
    private boolean exitingMode;
    private boolean restoreOldMode;

    /**
     * Constructs a new {@code ImageryAdjustAction} for the given layer.
     * @param layer The imagery layer
     */
    public ImageryAdjustAction(AbstractTileSourceLayer<?> layer) {
        super(tr("New offset"), "adjustimg", tr("Adjust the position of this imagery layer"), cursor);
        putValue("toolbar", Boolean.FALSE);
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
        old = layer.getDisplaySettings().getOffsetBookmark();
        EastNorth curOff = old == null ? EastNorth.ZERO : old.getDisplacement(ProjectionRegistry.getProjection());
        LatLon center;
        if (MainApplication.isDisplayingMapView()) {
            center = ProjectionRegistry.getProjection().eastNorth2latlon(MainApplication.getMap().mapView.getCenter());
        } else {
            center = LatLon.ZERO;
        }
        tempOffset = new OffsetBookmark(
                ProjectionRegistry.getProjection().toCode(),
                layer.getInfo().getId(),
                layer.getInfo().getName(),
                null,
                curOff, center);
        layer.getDisplaySettings().setOffsetBookmark(tempOffset);
        addListeners();
        showOffsetDialog(new ImageryOffsetDialog());
    }

    private static void showOffsetDialog(ImageryOffsetDialog dlg) {
        offsetDialog = dlg;
        offsetDialog.setVisible(true);
    }

    private static void hideOffsetDialog() {
        offsetDialog.setVisible(false);
        offsetDialog = null;
    }

    protected void addListeners() {
        MapView mapView = MainApplication.getMap().mapView;
        mapView.addMouseListener(this);
        mapView.addMouseMotionListener(this);
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            Logging.error(ex);
        }
    }

    @Override
    public void exitMode() {
        // do not restore old mode here - this is called when the new mode is already known.
        restoreOldMode = false;
        doExitMode();
    }

    private void exitModeAndRestoreOldMode() {
        restoreOldMode = true;
        doExitMode();
        restoreOldMode = false;
    }

    private void doExitMode() {
        exitingMode = true;
        try {
            super.exitMode();
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
        }
        if (offsetDialog != null) {
            if (layer != null) {
                layer.getDisplaySettings().setOffsetBookmark(old);
            }
            hideOffsetDialog();
        }
        removeListeners();
        exitingMode = false;
    }

    protected void removeListeners() {
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
            Logging.error(ex);
        }
        if (MainApplication.isDisplayingMapView()) {
            MapFrame map = MainApplication.getMap();
            map.mapView.removeMouseMotionListener(this);
            map.mapView.removeMouseListener(this);
        }
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (!(event instanceof KeyEvent)
          || (event.getID() != KeyEvent.KEY_PRESSED)
          || (layer == null)
          || (offsetDialog != null && offsetDialog.areFieldsInFocus())) {
            return;
        }
        KeyEvent kev = (KeyEvent) event;
        int dx = 0;
        int dy = 0;
        switch (kev.getKeyCode()) {
        case KeyEvent.VK_UP : dy = +1; break;
        case KeyEvent.VK_DOWN : dy = -1; break;
        case KeyEvent.VK_LEFT : dx = -1; break;
        case KeyEvent.VK_RIGHT : dx = +1; break;
        case KeyEvent.VK_ESCAPE:
            if (offsetDialog != null) {
                restoreOldMode = true;
                offsetDialog.setVisible(false);
                return;
            }
            break;
        default: // Do nothing
        }
        if (dx != 0 || dy != 0) {
            double ppd = layer.getPPD();
            EastNorth d = tempOffset.getDisplacement().add(new EastNorth(dx / ppd, dy / ppd));
            tempOffset.setDisplacement(d);
            layer.getDisplaySettings().setOffsetBookmark(tempOffset);
            if (offsetDialog != null) {
                offsetDialog.updateOffset();
            }
            if (Logging.isDebugEnabled()) {
                Logging.debug("{0} consuming event {1}", getClass().getName(), kev);
            }
            kev.consume();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (layer.isVisible()) {
            requestFocusInMapView();
            MapView mapView = MainApplication.getMap().mapView;
            prevEastNorth = mapView.getEastNorth(e.getX(), e.getY());
            mapView.setNewCursor(Cursor.MOVE_CURSOR, this);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (layer == null || prevEastNorth == null) return;
        EastNorth eastNorth = MainApplication.getMap().mapView.getEastNorth(e.getX(), e.getY());
        EastNorth d = tempOffset.getDisplacement().add(eastNorth).subtract(prevEastNorth);
        tempOffset.setDisplacement(d);
        layer.getDisplaySettings().setOffsetBookmark(tempOffset);
        if (offsetDialog != null) {
            offsetDialog.updateOffset();
        }
        prevEastNorth = eastNorth;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        MapView mapView = MainApplication.getMap().mapView;
        mapView.repaint();
        mapView.resetCursor(this);
        prevEastNorth = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MapFrame map = MainApplication.getMap();
        if (offsetDialog != null || layer == null || map == null)
            return;
        oldMapMode = map.mapMode;
        super.actionPerformed(e);
    }

    private static final class ConfirmOverwriteBookmarkDialog extends ExtendedDialog {
        ConfirmOverwriteBookmarkDialog() {
            super(MainApplication.getMainFrame(), tr("Overwrite"), new String[] {tr("Overwrite"), tr("Cancel")});
            contentInsets = new Insets(10, 15, 10, 15);
            setContent(tr("Offset bookmark already exists. Overwrite?"));
            setButtonIcons("ok", "cancel");
        }
    }

    private class ImageryOffsetDialog extends ExtendedDialog implements FocusListener {
        private final JosmTextField tOffset = new JosmTextField();
        private final JosmTextField tBookmarkName = new JosmTextField();
        private boolean ignoreListener;

        /**
         * Constructs a new {@code ImageryOffsetDialog}.
         */
        ImageryOffsetDialog() {
            super(MainApplication.getMainFrame(),
                    tr("Adjust imagery offset"),
                    new String[] {tr("OK"), tr("Cancel")},
                    false, false); // Do not dispose on close, so HIDE_ON_CLOSE remains the default behaviour and setVisible is called
            setButtonIcons("ok", "cancel");
            contentInsets = new Insets(10, 15, 5, 15);
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.add(new JMultilineLabel(tr("Use arrow keys or drag the imagery layer with mouse to adjust the imagery offset.\n" +
                    "You can also enter east and north offset in the {0} coordinates.\n" +
                    "If you want to save the offset as bookmark, enter the bookmark name below",
                    ProjectionRegistry.getProjection().toString())), GBC.eop());
            pnl.add(new JLabel(tr("Offset:")), GBC.std());
            pnl.add(tOffset, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 5));
            pnl.add(new JLabel(tr("Bookmark name: ")), GBC.std());
            pnl.add(tBookmarkName, GBC.eol().fill(GBC.HORIZONTAL));
            tOffset.setColumns(16);
            updateOffsetIntl();
            tOffset.addFocusListener(this);
            setContent(pnl);
            setupDialog();
            setRememberWindowGeometry(getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(MainApplication.getMainFrame(), getSize()));
        }

        private boolean areFieldsInFocus() {
            return tOffset.hasFocus();
        }

        @Override
        public void focusGained(FocusEvent e) {
            // Do nothing
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (ignoreListener) return;
            String ostr = tOffset.getText();
            int semicolon = ostr.indexOf(';');
            if (layer != null && semicolon >= 0 && semicolon + 1 < ostr.length()) {
                try {
                    String easting = ostr.substring(0, semicolon).trim();
                    String northing = ostr.substring(semicolon + 1).trim();
                    double dx = JosmDecimalFormatSymbolsProvider.parseDouble(easting);
                    double dy = JosmDecimalFormatSymbolsProvider.parseDouble(northing);
                    tempOffset.setDisplacement(new EastNorth(dx, dy));
                    layer.getDisplaySettings().setOffsetBookmark(tempOffset);
                } catch (NumberFormatException nfe) {
                    // we repaint offset numbers in any case
                    Logging.trace(nfe);
                }
            }
            updateOffsetIntl();
            if (layer != null) {
                layer.invalidate();
            }
        }

        private void updateOffset() {
            ignoreListener = true;
            updateOffsetIntl();
            ignoreListener = false;
        }

        private void updateOffsetIntl() {
            if (layer != null) {
                // Support projections with very small numbers (e.g. 4326)
                int precision = ProjectionRegistry.getProjection().getDefaultZoomInPPD() >= 1.0 ? 2 : 7;
                // US locale to force decimal separator to be '.'
                try (Formatter us = new Formatter(Locale.US)) {
                    EastNorth displacement = layer.getDisplaySettings().getDisplacement();
                    tOffset.setText(us.format(new StringBuilder()
                        .append("%1.").append(precision).append("f; %1.").append(precision).append('f').toString(),
                        displacement.east(), displacement.north()).toString());
                }
            }
        }

        private boolean confirmOverwriteBookmark() {
            return new ConfirmOverwriteBookmarkDialog().showDialog().getValue() == 1;
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            restoreOldMode = true;
            if (buttonIndex == 0 && tBookmarkName.getText() != null && !tBookmarkName.getText().isEmpty() &&
                    OffsetBookmark.getBookmarkByName(layer, tBookmarkName.getText()) != null &&
                    !confirmOverwriteBookmark()) {
                return;
            }
            super.buttonAction(buttonIndex, evt);
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible)
                return;
            ignoreListener = true;
            offsetDialog = null;
            if (layer != null) {
                if (getValue() != 1) {
                    layer.getDisplaySettings().setOffsetBookmark(old);
                } else if (tBookmarkName.getText() != null && !tBookmarkName.getText().isEmpty()) {
                    OffsetBookmark.bookmarkOffset(tBookmarkName.getText(), layer);
                }
            }
            MainApplication.getMenu().imageryMenu.refreshOffsetMenu();
            restoreMapModeState();
        }

        private void restoreMapModeState() {
            MapFrame map = MainApplication.getMap();
            if (map == null)
                return;
            if (oldMapMode != null) {
                if (restoreOldMode || (!exitingMode && getValue() == ExtendedDialog.DialogClosedOtherwise)) {
                    map.selectMapMode(oldMapMode);
                }
                oldMapMode = null;
            } else if (!exitingMode && !map.selectSelectTool(false)) {
                exitModeAndRestoreOldMode();
                map.mapMode = null;
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
