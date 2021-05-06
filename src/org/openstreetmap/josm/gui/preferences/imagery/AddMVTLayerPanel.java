// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * A panel for adding Mapbox Vector Tile layers
 * @author Taylor Smock
 * @since 17862
 */
public class AddMVTLayerPanel extends AddImageryPanel {
    private final JosmTextField mvtZoom = new JosmTextField();
    private final JosmTextArea mvtUrl = new JosmTextArea(3, 40).transferFocusOnTab();

    /**
     * Constructs a new {@code AddMVTLayerPanel}.
     */
    public AddMVTLayerPanel() {

        add(new JLabel(tr("{0} Make sure OSM has the permission to use this service", "1.")), GBC.eol());
        add(new JLabel(tr("{0} Enter URL (may be a style sheet url)", "2.")), GBC.eol());
        add(new JLabel("<html>" + Utils.joinAsHtmlUnorderedList(Arrays.asList(
                tr("{0} is replaced by tile zoom level, also supported:<br>" +
                        "offsets to the zoom level: {1} or {2}<br>" +
                        "reversed zoom level: {3}", "{zoom}", "{zoom+1}", "{zoom-1}", "{19-zoom}"),
                tr("{0} is replaced by X-coordinate of the tile", "{x}"),
                tr("{0} is replaced by Y-coordinate of the tile", "{y}"),
                tr("{0} is replaced by a random selection from the given comma separated list, e.g. {1}", "{switch:...}", "{switch:a,b,c}")
        )) + "</html>"), GBC.eol().fill());

        final KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                mvtUrl.setText(buildMvtUrl());
            }
        };

        add(rawUrl, GBC.eop().fill());
        rawUrl.setLineWrap(true);
        rawUrl.addKeyListener(keyAdapter);

        add(new JLabel(tr("{0} Enter maximum zoom (optional)", "3.")), GBC.eol());
        mvtZoom.addKeyListener(keyAdapter);
        add(mvtZoom, GBC.eop().fill());

        add(new JLabel(tr("{0} Edit generated {1} URL (optional)", "4.", "MVT")), GBC.eol());
        add(mvtUrl, GBC.eop().fill());
        mvtUrl.setLineWrap(true);

        add(new JLabel(tr("{0} Enter name for this layer", "5.")), GBC.eol());
        add(name, GBC.eop().fill());

        registerValidableComponent(mvtUrl);
    }

    private String buildMvtUrl() {
        StringBuilder a = new StringBuilder("mvt");
        String z = sanitize(mvtZoom.getText());
        if (!z.isEmpty()) {
            a.append('[').append(z).append(']');
        }
        a.append(':').append(sanitize(getImageryRawUrl(), ImageryType.MVT));
        return a.toString();
    }

    @Override
    public ImageryInfo getImageryInfo() {
        final ImageryInfo generated = new ImageryInfo(getImageryName(), getMvtUrl());
        generated.setImageryType(ImageryType.MVT);
        return generated;
    }

    protected final String getMvtUrl() {
        return sanitize(mvtUrl.getText());
    }

    @Override
    protected boolean isImageryValid() {
        return !getImageryName().isEmpty() && !getMvtUrl().isEmpty();
    }
}
