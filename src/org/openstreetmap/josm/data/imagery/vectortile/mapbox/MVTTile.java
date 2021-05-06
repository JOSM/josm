// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.IQuadBucketType;
import org.openstreetmap.josm.data.imagery.vectortile.VectorTile;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.protobuf.ProtobufParser;
import org.openstreetmap.josm.data.protobuf.ProtobufRecord;
import org.openstreetmap.josm.data.vector.VectorDataStore;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class for Mapbox Vector Tiles
 *
 * @author Taylor Smock
 * @since xxx
 */
public class MVTTile extends Tile implements VectorTile, IQuadBucketType {
    private final ListenerList<TileListener> listenerList = ListenerList.create();
    private Collection<Layer> layers;
    private int extent = Layer.DEFAULT_EXTENT;
    static final BufferedImage CLEAR_LOADED = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
    private BBox bbox;
    private VectorDataStore vectorDataStore;

    /**
     * Create a new Tile
     * @param source The source of the tile
     * @param xtile The x coordinate for the tile
     * @param ytile The y coordinate for the tile
     * @param zoom The zoom for the tile
     */
    public MVTTile(TileSource source, int xtile, int ytile, int zoom) {
        super(source, xtile, ytile, zoom);
    }

    @Override
    public void loadImage(final InputStream inputStream) throws IOException {
        if (this.image == null || this.image == Tile.LOADING_IMAGE || this.image == Tile.ERROR_IMAGE) {
            this.initLoading();
            ProtobufParser parser = new ProtobufParser(inputStream);
            Collection<ProtobufRecord> protobufRecords = parser.allRecords();
            this.layers = new HashSet<>();
            this.layers = protobufRecords.stream().map(protoBufRecord -> {
                Layer mvtLayer = null;
                if (protoBufRecord.getField() == Layer.LAYER_FIELD) {
                    try (ProtobufParser tParser = new ProtobufParser(protoBufRecord.getBytes())) {
                        mvtLayer = new Layer(tParser.allRecords());
                    } catch (IOException e) {
                        Logging.error(e);
                    } finally {
                        // Cleanup bytes
                        protoBufRecord.close();
                    }
                }
                return mvtLayer;
            }).collect(Collectors.toCollection(HashSet::new));
            this.extent = layers.stream().map(Layer::getExtent).max(Integer::compare).orElse(Layer.DEFAULT_EXTENT);
            if (this.getData() != null) {
                this.finishLoading();
                this.listenerList.fireEvent(event -> event.finishedLoading(this));
                // Ensure that we don't keep the loading image around
                this.image = CLEAR_LOADED;
                // Cleanup as much as possible -- layers will still exist, but only base information (like name, extent) will remain.
                // Called last just in case the listeners need the layers.
                this.layers.forEach(Layer::destroy);
            }
        }
    }

    @Override
    public Collection<Layer> getLayers() {
        return this.layers;
    }

    @Override
    public int getExtent() {
        return this.extent;
    }

    /**
     * Add a tile loader finisher listener
     *
     * @param listener The listener to add
     */
    public void addTileLoaderFinisher(TileListener listener) {
        // Add as weak listeners since we don't want to keep unnecessary references.
        this.listenerList.addWeakListener(listener);
    }

    @Override
    public BBox getBBox() {
        if (this.bbox == null) {
            final ICoordinate upperLeft = this.getTileSource().tileXYToLatLon(this);
            final ICoordinate lowerRight = this.getTileSource()
                    .tileXYToLatLon(this.getXtile() + 1, this.getYtile() + 1, this.getZoom());
            BBox newBBox = new BBox(upperLeft.getLon(), upperLeft.getLat(), lowerRight.getLon(), lowerRight.getLat());
            this.bbox = newBBox.toImmutable();
        }
        return this.bbox;
    }

    /**
     * Get the datastore for this tile
     * @return The data
     */
    public VectorDataStore getData() {
        if (this.vectorDataStore == null) {
            VectorDataStore newDataStore = new VectorDataStore();
            newDataStore.addDataTile(this);
            this.vectorDataStore = newDataStore;
        }
        return this.vectorDataStore;
    }

    /**
     * A class that can be notified that a tile has finished loading
     *
     * @author Taylor Smock
     */
    public interface TileListener {
        /**
         * Called when the MVTTile is finished loading
         *
         * @param tile The tile that finished loading
         */
        void finishedLoading(MVTTile tile);
    }

    /**
     * A class used to set the layers that an MVTTile will show.
     *
     * @author Taylor Smock
     */
    public interface LayerShower {
        /**
         * Get a list of layers to show
         *
         * @return A list of layer names
         */
        List<String> layersToShow();
    }
}
