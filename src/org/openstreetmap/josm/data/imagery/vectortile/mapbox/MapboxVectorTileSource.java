// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.JosmTemplatedTMSTileSource;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.style.MapboxVectorStyle;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.style.Source;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.Logging;

/**
 * Tile Source handling for Mapbox Vector Tile sources
 * @author Taylor Smock
 * @since xxx
 */
public class MapboxVectorTileSource extends JosmTemplatedTMSTileSource {
    private final MapboxVectorStyle styleSource;

    /**
     * Create a new {@link MapboxVectorTileSource} from an {@link ImageryInfo}
     * @param info The info to create the source from
     */
    public MapboxVectorTileSource(ImageryInfo info) {
        super(info);
        MapboxVectorStyle mapBoxVectorStyle = null;
        try (CachedFile style = new CachedFile(info.getUrl());
          InputStream inputStream = style.getInputStream();
          JsonReader reader = Json.createReader(inputStream)) {
            JsonObject object = reader.readObject();
            // OK, we may have a stylesheet. "version", "layers", and "sources" are all required.
            if (object.containsKey("version") && object.containsKey("layers") && object.containsKey("sources")) {
                mapBoxVectorStyle = MapboxVectorStyle.getMapboxVectorStyle(info.getUrl());
            }
        } catch (IOException | JsonException e) {
            Logging.trace(e);
        }
        this.styleSource = mapBoxVectorStyle;
        if (this.styleSource != null) {
            final Source source;
            List<Source> sources = this.styleSource.getSources().keySet().stream().filter(Objects::nonNull)
              .collect(Collectors.toList());
            if (sources.size() == 1) {
                source = sources.get(0);
            } else if (!sources.isEmpty()) {
                // Ask user what source they want.
                source = GuiHelper.runInEDTAndWaitAndReturn(() -> {
                    ExtendedDialog dialog = new ExtendedDialog(MainApplication.getMainFrame(),
                      tr("Select Vector Tile Layers"), tr("Add layers"));
                    JosmComboBox<Source> comboBox = new JosmComboBox<>(sources.toArray(new Source[0]));
                    comboBox.setSelectedIndex(0);
                    dialog.setContent(comboBox);
                    dialog.showDialog();
                    return (Source) comboBox.getSelectedItem();
                });
            } else {
                // Umm. What happened? We probably have an invalid style source.
                throw new InvalidMapboxVectorTileException(tr("Cannot understand style source: {0}", info.getUrl()));
            }
            if (source != null) {
                this.name = name + ": " + source.getName();
                // There can technically be multiple URL's for this field; unfortunately, JOSM can only handle one right now.
                this.baseUrl = source.getUrls().get(0);
                this.minZoom = source.getMinZoom();
                this.maxZoom = source.getMaxZoom();
                if (source.getAttributionText() != null) {
                    this.setAttributionText(source.getAttributionText());
                }
            }
        }
    }

    /**
     * Get the style source for this Vector Tile source
     * @return The source to use for styling
     */
    public MapboxVectorStyle getStyleSource() {
        return this.styleSource;
    }
}
