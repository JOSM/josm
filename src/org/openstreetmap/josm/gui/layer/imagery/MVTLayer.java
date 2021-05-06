// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.Layer;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTFile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTTile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTTile.TileListener;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorCachedTileLoader;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorTileSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.AbstractMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.vector.VectorDataSet;
import org.openstreetmap.josm.data.vector.VectorNode;
import org.openstreetmap.josm.data.vector.VectorPrimitive;
import org.openstreetmap.josm.data.vector.VectorRelation;
import org.openstreetmap.josm.data.vector.VectorWay;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractCachedTileSourceLayer;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;

/**
 * A layer for Mapbox Vector Tiles
 * @author Taylor Smock
 * @since 17862
 */
public class MVTLayer extends AbstractCachedTileSourceLayer<MapboxVectorTileSource> implements TileListener {
    private static final String CACHE_REGION_NAME = "MVT";
    // Just to avoid allocating a bunch of 0 length action arrays
    private static final Action[] EMPTY_ACTIONS = new Action[0];
    private final Map<String, Boolean> layerNames = new HashMap<>();
    private final VectorDataSet dataSet = new VectorDataSet();

    /**
     * Creates an instance of an MVT layer
     *
     * @param info ImageryInfo describing the layer
     */
    public MVTLayer(ImageryInfo info) {
        super(info);
    }

    @Override
    protected Class<? extends TileLoader> getTileLoaderClass() {
        return MapboxVectorCachedTileLoader.class;
    }

    @Override
    protected String getCacheName() {
        return CACHE_REGION_NAME;
    }

    @Override
    public Collection<String> getNativeProjections() {
        // Mapbox Vector Tiles <i>specifically</i> only support EPSG:3857
        // ("it is exclusively geared towards square pixel tiles in {link to EPSG:3857}").
        return Collections.singleton(MVTFile.DEFAULT_PROJECTION);
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        this.dataSet.setZoom(this.getZoomLevel());
        AbstractMapRenderer painter = MapRendererFactory.getInstance().createActiveRenderer(g, mv, false);
        painter.enableSlowOperations(mv.getMapMover() == null || !mv.getMapMover().movementInProgress()
          || !OsmDataLayer.PROPERTY_HIDE_LABELS_WHILE_DRAGGING.get());
        // Set the painter to use our custom style sheet
        if (painter instanceof StyledMapRenderer && this.dataSet.getStyles() != null) {
            ((StyledMapRenderer) painter).setStyles(this.dataSet.getStyles());
        }
        painter.render(this.dataSet, false, box);
    }

    @Override
    protected MapboxVectorTileSource getTileSource() {
        MapboxVectorTileSource source = new MapboxVectorTileSource(this.info);
        this.info.setAttribution(source);
        if (source.getStyleSource() != null) {
            List<ElemStyles> styles = source.getStyleSource().getSources().entrySet().stream()
              .filter(entry -> entry.getKey() == null || entry.getKey().getUrls().contains(source.getBaseUrl()))
              .map(Map.Entry::getValue).collect(Collectors.toList());
            // load the style sources
            styles.stream().map(ElemStyles::getStyleSources).flatMap(Collection::stream).forEach(StyleSource::loadStyleSource);
            this.dataSet.setStyles(styles);
            this.setName(source.getName());
        }
        return source;
    }

    @Override
    public Tile createTile(MapboxVectorTileSource source, int x, int y, int zoom) {
        final MVTTile tile = new MVTTile(source, x, y, zoom);
        tile.addTileLoaderFinisher(this);
        return tile;
    }

    @Override
    public Action[] getMenuEntries() {
        ArrayList<Action> actions = new ArrayList<>(Arrays.asList(super.getMenuEntries()));
        // Add separator between Info and the layers
        actions.add(SeparatorLayerAction.INSTANCE);
        if (ExpertToggleAction.isExpert()) {
            for (Map.Entry<String, Boolean> layerConfig : layerNames.entrySet()) {
                actions.add(new EnableLayerAction(layerConfig.getKey(), () -> layerNames.computeIfAbsent(layerConfig.getKey(), key -> true),
                        layer -> {
                            layerNames.compute(layer, (key, value) -> Boolean.FALSE.equals(value));
                            this.dataSet.setInvisibleLayers(layerNames.entrySet().stream()
                                    .filter(entry -> Boolean.FALSE.equals(entry.getValue()))
                                    .map(Map.Entry::getKey).collect(Collectors.toList()));
                            this.invalidate();
                        }));
            }
            // Add separator between layers and convert action
            actions.add(SeparatorLayerAction.INSTANCE);
            actions.add(new ConvertLayerAction(this));
        }
        return actions.toArray(EMPTY_ACTIONS);
    }

    /**
     * Get the data set for this layer
     */
    public VectorDataSet getData() {
        return this.dataSet;
    }
    
    private static class ConvertLayerAction extends AbstractAction implements LayerAction {
        private final MVTLayer layer;

        ConvertLayerAction(MVTLayer layer) {
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LayerManager manager = MainApplication.getLayerManager();
            VectorDataSet dataSet = layer.getData();
            DataSet osmData = new DataSet();
            // Add nodes first, map is to ensure we can map new nodes to vector nodes
            Map<VectorNode, Node> nodeMap = new HashMap<>(dataSet.getNodes().size());
            for (VectorNode vectorNode : dataSet.getNodes()) {
                Node newNode = new Node(vectorNode.getCoor());
                if (vectorNode.isTagged()) {
                    vectorNode.getInterestingTags().forEach(newNode::put);
                    newNode.put("layer", vectorNode.getLayer());
                    newNode.put("id", Long.toString(vectorNode.getId()));
                }
                nodeMap.put(vectorNode, newNode);
            }
            // Add ways next
            Map<VectorWay, Way> wayMap = new HashMap<>(dataSet.getWays().size());
            for (VectorWay vectorWay : dataSet.getWays()) {
                Way newWay = new Way();
                List<Node> nodes = vectorWay.getNodes().stream().map(nodeMap::get).filter(Objects::nonNull).collect(Collectors.toList());
                newWay.setNodes(nodes);
                if (vectorWay.isTagged()) {
                    vectorWay.getInterestingTags().forEach(newWay::put);
                    newWay.put("layer", vectorWay.getLayer());
                    newWay.put("id", Long.toString(vectorWay.getId()));
                }
                wayMap.put(vectorWay, newWay);
            }

            // Finally, add Relations
            Map<VectorRelation, Relation> relationMap = new HashMap<>(dataSet.getRelations().size());
            for (VectorRelation vectorRelation : dataSet.getRelations()) {
                Relation newRelation = new Relation();
                if (vectorRelation.isTagged()) {
                    vectorRelation.getInterestingTags().forEach(newRelation::put);
                    newRelation.put("layer", vectorRelation.getLayer());
                    newRelation.put("id", Long.toString(vectorRelation.getId()));
                }
                List<RelationMember> members = vectorRelation.getMembers().stream().map(member -> {
                    final OsmPrimitive primitive;
                    final VectorPrimitive vectorPrimitive = member.getMember();
                    if (vectorPrimitive instanceof VectorNode) {
                        primitive = nodeMap.get(vectorPrimitive);
                    } else if (vectorPrimitive instanceof VectorWay) {
                        primitive = wayMap.get(vectorPrimitive);
                    } else if (vectorPrimitive instanceof VectorRelation) {
                        // Hopefully, relations are encountered in order...
                        primitive = relationMap.get(vectorPrimitive);
                    } else {
                        primitive = null;
                    }
                    if (primitive == null) return null;
                    return new RelationMember(member.getRole(), primitive);
                }).filter(Objects::nonNull).collect(Collectors.toList());
                newRelation.setMembers(members);
                relationMap.put(vectorRelation, newRelation);
            }
            try {
                osmData.beginUpdate();
                nodeMap.values().forEach(osmData::addPrimitive);
                wayMap.values().forEach(osmData::addPrimitive);
                relationMap.values().forEach(osmData::addPrimitive);
            } finally {
                osmData.endUpdate();
            }
            manager.addLayer(new OsmDataLayer(osmData, this.layer.getName(), null));
            manager.removeLayer(this.layer);
        }

        @Override
        public boolean supportLayers(List<org.openstreetmap.josm.gui.layer.Layer> layers) {
            return layers.stream().allMatch(MVTLayer.class::isInstance);
        }

        @Override
        public Component createMenuComponent() {
            JMenuItem menuItem = new JMenuItem(tr("Convert to OSM Data"));
            menuItem.addActionListener(this);
            return menuItem;
        }
    }

    private static class EnableLayerAction extends AbstractAction implements LayerAction {
        private final String layer;
        private final Consumer<String> consumer;
        private final BooleanSupplier state;

        EnableLayerAction(String layer, BooleanSupplier state, Consumer<String> consumer) {
            super(tr("Toggle layer {0}", layer));
            this.layer = layer;
            this.consumer = consumer;
            this.state = state;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            consumer.accept(layer);
        }

        @Override
        public boolean supportLayers(List<org.openstreetmap.josm.gui.layer.Layer> layers) {
            return layers.stream().allMatch(MVTLayer.class::isInstance);
        }

        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
            item.setSelected(this.state.getAsBoolean());
            return item;
        }
    }

    @Override
    public void finishedLoading(MVTTile tile) {
        for (Layer layer : tile.getLayers()) {
            this.layerNames.putIfAbsent(layer.getName(), true);
        }
        this.dataSet.addTileData(tile);
    }
}
