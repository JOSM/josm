// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.IQuadBucketType;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.vectortile.VectorTile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.Feature;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.GeometryTypes;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.Layer;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.UniqueIdGenerator;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * A data store for Vector Data sets
 * @author Taylor Smock
 * @since 17862
 */
public class VectorDataStore extends DataStore<VectorPrimitive, VectorNode, VectorWay, VectorRelation> implements Destroyable {
    private static final String JOSM_MERGE_TYPE_KEY = "josm_merge_type";
    private static final String ORIGINAL_ID = "original_id";
    private static final String MULTIPOLYGON_TYPE = "multipolygon";
    private static final String RELATION_TYPE = "type";

    @Override
    protected void addPrimitive(VectorPrimitive primitive) {
        // The field is uint64, so we can use negative numbers to indicate that it is a "generated" object (e.g., nodes for ways)
        if (primitive.getUniqueId() == 0) {
            final UniqueIdGenerator generator = primitive.getIdGenerator();
            long id;
            do {
                id = generator.generateUniqueId();
            } while (this.primitivesMap.containsKey(new SimplePrimitiveId(id, primitive.getType())));
            primitive.setId(primitive.getIdGenerator().generateUniqueId());
        }
        if (primitive instanceof VectorRelation && !primitive.isMultipolygon()) {
            primitive = mergeWays((VectorRelation) primitive);
        }
        final VectorPrimitive alreadyAdded = this.primitivesMap.get(primitive.getPrimitiveId());
        final VectorRelation mergedRelation = (VectorRelation) this.primitivesMap
          .get(new SimplePrimitiveId(primitive.getPrimitiveId().getUniqueId(),
            OsmPrimitiveType.RELATION));
        if (alreadyAdded == null || alreadyAdded.equals(primitive)) {
            super.addPrimitive(primitive);
        } else if (mergedRelation != null && mergedRelation.get(JOSM_MERGE_TYPE_KEY) != null) {
            mergedRelation.addRelationMember(new VectorRelationMember("", primitive));
            super.addPrimitive(primitive);
            // Check that all primitives can be merged
            if (mergedRelation.getMemberPrimitivesList().stream().allMatch(IWay.class::isInstance)) {
                // This pretty much does the "right" thing
                this.mergeWays(mergedRelation);
            } else if (!(primitive instanceof IWay)) {
                // Can't merge, ever (one of the childs is a node/relation)
                mergedRelation.remove(JOSM_MERGE_TYPE_KEY);
            }
        } else if (mergedRelation != null && primitive instanceof IRelation) {
            // Just add to the relation
            ((VectorRelation) primitive).getMembers().forEach(mergedRelation::addRelationMember);
        } else if (alreadyAdded instanceof VectorWay && primitive instanceof VectorWay) {
            final VectorRelation temporaryRelation =
              mergedRelation == null ? new VectorRelation(primitive.getLayer()) : mergedRelation;
            if (mergedRelation == null) {
                temporaryRelation.put(JOSM_MERGE_TYPE_KEY, "merge");
                temporaryRelation.addRelationMember(new VectorRelationMember("", alreadyAdded));
            }
            temporaryRelation.addRelationMember(new VectorRelationMember("", primitive));
            super.addPrimitive(primitive);
            super.addPrimitive(temporaryRelation);
        }
    }

    private VectorPrimitive mergeWays(VectorRelation relation) {
        List<VectorRelationMember> members = RelationSorter.sortMembersByConnectivity(relation.getMembers());
        Collection<VectorWay> relationWayList = members.stream().map(VectorRelationMember::getMember)
          .filter(VectorWay.class::isInstance)
          .map(VectorWay.class::cast).collect(toCollection(ArrayList::new));
        // Only support way-only relations
        if (relationWayList.size() != relation.getMemberPrimitivesList().size()) {
            return relation;
        }
        List<VectorWay> wayList = new ArrayList<>(relation.getMembersCount());
        // Assume that the order may not be correct, worst case O(n), best case O(n/2)
        // Assume that the ways were drawn in order
        final int maxIteration = relationWayList.size();
        int iteration = 0;
        while (iteration < maxIteration && wayList.size() < relationWayList.size()) {
            for (VectorWay way : relationWayList) {
                if (wayList.isEmpty()) {
                    wayList.add(way);
                    continue;
                }
                // Check first/last ways (last first, since the list *should* be sorted)
                if (canMergeWays(wayList.get(wayList.size() - 1), way, false)) {
                    wayList.add(way);
                } else if (canMergeWays(wayList.get(0), way, false)) {
                    wayList.add(0, way);
                }
            }
            iteration++;
            relationWayList.removeIf(wayList::contains);
        }
        return relation;
    }

    private static <N extends INode, W extends IWay<N>> boolean canMergeWays(W old, W toAdd, boolean allowReverse) {
        final List<N> nodes = new ArrayList<>(old.getNodes());
        boolean added = true;
        if (allowReverse && old.firstNode().equals(toAdd.firstNode())) {
            // old <-|-> new becomes old ->|-> new
            Collections.reverse(nodes);
            nodes.addAll(toAdd.getNodes());
        } else if (old.firstNode().equals(toAdd.lastNode())) {
            // old <-|<- new, so we prepend the new nodes in order
            nodes.addAll(0, toAdd.getNodes());
        } else if (old.lastNode().equals(toAdd.firstNode())) {
            // old ->|-> new, we just add it
            nodes.addAll(toAdd.getNodes());
        } else if (allowReverse && old.lastNode().equals(toAdd.lastNode())) {
            // old ->|<- new, we need to reverse new
            final List<N> toAddNodes = new ArrayList<>(toAdd.getNodes());
            Collections.reverse(toAddNodes);
            nodes.addAll(toAddNodes);
        } else {
            added = false;
        }
        if (added) {
            // This is (technically) always correct
            old.setNodes(nodes);
        }
        return added;
    }

    private synchronized <T extends Tile & VectorTile> VectorNode pointToNode(T tile, Layer layer,
      Collection<VectorPrimitive> featureObjects, int x, int y) {
        final BBox tileBbox;
        if (tile instanceof IQuadBucketType) {
            tileBbox = ((IQuadBucketType) tile).getBBox();
        } else {
            final ICoordinate upperLeft = tile.getTileSource().tileXYToLatLon(tile);
            final ICoordinate lowerRight = tile.getTileSource()
                    .tileXYToLatLon(tile.getXtile() + 1, tile.getYtile() + 1, tile.getZoom());

            tileBbox = new BBox(upperLeft.getLon(), upperLeft.getLat(), lowerRight.getLon(), lowerRight.getLat());
        }
        final int layerExtent = layer.getExtent();
        final LatLon coords = new LatLon(
                tileBbox.getMaxLat() - (tileBbox.getMaxLat() - tileBbox.getMinLat()) * y / layerExtent,
                tileBbox.getMinLon() + (tileBbox.getMaxLon() - tileBbox.getMinLon()) * x / layerExtent
        );
        final Collection<VectorNode> nodes = this.store
          .searchNodes(new BBox(coords.lon(), coords.lat(), VectorDataSet.DUPE_NODE_DISTANCE));
        final VectorNode node;
        if (!nodes.isEmpty()) {
            final VectorNode first = nodes.iterator().next();
            if (first.isDisabled() || !first.isVisible()) {
                // Only replace nodes that are not visible
                node = new VectorNode(layer.getName());
                node.setCoor(node.getCoor());
                first.getReferrers(true).forEach(primitive -> {
                    if (primitive instanceof VectorWay) {
                        List<VectorNode> nodeList = new ArrayList<>(((VectorWay) primitive).getNodes());
                        nodeList.replaceAll(vnode -> vnode.equals(first) ? node : vnode);
                        ((VectorWay) primitive).setNodes(nodeList);
                    } else if (primitive instanceof VectorRelation) {
                        List<VectorRelationMember> members = new ArrayList<>(((VectorRelation) primitive).getMembers());
                        members.replaceAll(member ->
                          member.getMember().equals(first) ? new VectorRelationMember(member.getRole(), node) : member);
                        ((VectorRelation) primitive).setMembers(members);
                    }
                });
                this.removePrimitive(first);
            } else {
                node = first;
            }
        } else {
            node = new VectorNode(layer.getName());
        }
        node.setCoor(coords);
        featureObjects.add(node);
        return node;
    }

    private <T extends Tile & VectorTile> List<VectorWay> pathToWay(T tile, Layer layer,
      Collection<VectorPrimitive> featureObjects, Path2D shape) {
        final PathIterator pathIterator = shape.getPathIterator(null);
        final List<VectorWay> ways = new ArrayList<>(
                Utils.filteredCollection(pathIteratorToObjects(tile, layer, featureObjects, pathIterator), VectorWay.class));
        // These nodes technically do not exist, so we shouldn't show them
        for (VectorWay way : ways) {
            for (VectorNode node : way.getNodes()) {
                if (!node.hasKeys() && node.getReferrers(true).size() == 1 && node.getId() <= 0) {
                    node.setDisabled(true);
                    node.setVisible(false);
                }
            }
        }
        return ways;
    }

    private <T extends Tile & VectorTile> List<VectorPrimitive> pathIteratorToObjects(T tile, Layer layer,
      Collection<VectorPrimitive> featureObjects, PathIterator pathIterator) {
        final List<VectorNode> nodes = new ArrayList<>();
        final double[] coords = new double[6];
        final List<VectorPrimitive> ways = new ArrayList<>();
        do {
            final int type = pathIterator.currentSegment(coords);
            pathIterator.next();
            if ((PathIterator.SEG_MOVETO == type || PathIterator.SEG_CLOSE == type) && !nodes.isEmpty()) {
                if (PathIterator.SEG_CLOSE == type) {
                    nodes.add(nodes.get(0));
                }
                // New line
                if (!nodes.isEmpty()) {
                    final VectorWay way = new VectorWay(layer.getName());
                    way.setNodes(nodes);
                    featureObjects.add(way);
                    ways.add(way);
                }
                nodes.clear();
            }
            if (PathIterator.SEG_MOVETO == type || PathIterator.SEG_LINETO == type) {
                final VectorNode node = pointToNode(tile, layer, featureObjects, (int) coords[0], (int) coords[1]);
                nodes.add(node);
            } else if (PathIterator.SEG_CLOSE != type) {
                // Vector Tiles only have MoveTo, LineTo, and ClosePath. Anything else is not supported at this time.
                throw new UnsupportedOperationException();
            }
        } while (!pathIterator.isDone());
        if (!nodes.isEmpty()) {
            final VectorWay way = new VectorWay(layer.getName());
            way.setNodes(nodes);
            featureObjects.add(way);
            ways.add(way);
        }
        return ways;
    }

    private <T extends Tile & VectorTile> VectorRelation areaToRelation(T tile, Layer layer,
      Collection<VectorPrimitive> featureObjects, Area area) {
        VectorRelation vectorRelation = new VectorRelation(layer.getName());
        for (VectorPrimitive member : pathIteratorToObjects(tile, layer, featureObjects, area.getPathIterator(null))) {
            final String role;
            if (member instanceof VectorWay && ((VectorWay) member).isClosed()) {
                role = Geometry.isClockwise(((VectorWay) member).getNodes()) ? "outer" : "inner";
            } else {
                role = "";
            }
            vectorRelation.addRelationMember(new VectorRelationMember(role, member));
        }
        return vectorRelation;
    }

    /**
     * Add the information from a tile to this object
     * @param tile The tile to add
     * @param <T> The tile type
     */
    public <T extends Tile & VectorTile> void addDataTile(T tile) {
        // Using a map reduces the cost of addFeatureData from 2,715,158,632 bytes to 235,042,184 bytes (-91.3%)
        // This was somewhat variant, with some runs being closer to ~560 MB (still -80%).
        for (Layer layer : tile.getLayers()) {
            Map<GeometryTypes, List<Feature>> grouped = layer.getFeatures().stream().collect(Collectors.groupingBy(Feature::getGeometryType));
            // Unknown -> Point -> LineString -> Polygon
            for (GeometryTypes type : GeometryTypes.values()) {
                if (grouped.containsKey(type)) {
                    addFeatureData(tile, layer, grouped.get(type));
                }
            }
        }
        // Replace original_ids with the same object (reduce memory usage)
        // Strings aren't interned automatically in some GC implementations
        Collection<IPrimitive> primitives = this.getAllPrimitives().stream().filter(p -> p.hasKey(ORIGINAL_ID)).collect(toList());
        List<String> toReplace = primitives.stream().map(p -> p.get(ORIGINAL_ID)).filter(Objects::nonNull).collect(toList());
        primitives.stream().filter(p -> toReplace.contains(p.get(ORIGINAL_ID)))
                .forEach(p -> p.put(ORIGINAL_ID, toReplace.stream().filter(shared -> shared.equals(p.get(ORIGINAL_ID)))
                        .findAny().orElse(null)));
    }

    private <T extends Tile & VectorTile> void addFeatureData(T tile, Layer layer, Collection<Feature> features) {
        for (Feature feature : features) {
            try {
                addFeatureData(tile, layer, feature);
            } catch (IllegalArgumentException e) {
                Logging.error("Cannot add vector data for feature {0} of tile {1}: {2}", feature, tile, e.getMessage());
                Logging.error(e);
            }
        }
    }

    private <T extends Tile & VectorTile> void addFeatureData(T tile, Layer layer, Feature feature) {
        // This will typically be larger than primaryFeatureObjects, but this at least avoids quite a few ArrayList#grow calls
        List<VectorPrimitive> featureObjects = new ArrayList<>(feature.getGeometryObject().getShapes().size());
        List<VectorPrimitive> primaryFeatureObjects = new ArrayList<>(feature.getGeometryObject().getShapes().size());
        for (Shape shape : feature.getGeometryObject().getShapes()) {
            primaryFeatureObjects.add(shapeToPrimaryFeatureObject(tile, layer, shape, featureObjects));
        }
        final VectorPrimitive primitive;
        if (primaryFeatureObjects.size() == 1) {
            primitive = primaryFeatureObjects.get(0);
            if (primitive instanceof IRelation && !primitive.isMultipolygon()) {
                primitive.put(JOSM_MERGE_TYPE_KEY, "merge");
            }
        } else if (!primaryFeatureObjects.isEmpty()) {
            VectorRelation relation = new VectorRelation(layer.getName());
            List<VectorRelationMember> members = new ArrayList<>(primaryFeatureObjects.size());
            for (VectorPrimitive prim : primaryFeatureObjects) {
                members.add(new VectorRelationMember("", prim));
            }
            relation.setMembers(members);
            primitive = relation;
        } else {
            return;
        }
        primitive.setId(feature.getId());
        // Version 1 <i>and</i> 2 <i>do not guarantee</i> that non-zero ids are unique
        // We depend upon unique ids in the data store
        if (feature.getId() != 0 && this.primitivesMap.containsKey(primitive.getPrimitiveId())) {
            // This, unfortunately, makes a new string
            primitive.put(ORIGINAL_ID, Long.toString(feature.getId()));
            primitive.setId(primitive.getIdGenerator().generateUniqueId());
        }
        if (feature.getTags() != null) {
            primitive.putAll(feature.getTags());
        }
        for (VectorPrimitive prim : featureObjects) {
            this.addPrimitive(prim);
        }
        for (VectorPrimitive prim : primaryFeatureObjects) {
            this.addPrimitive(prim);
        }
        try {
            this.addPrimitive(primitive);
        } catch (JosmRuntimeException e) {
            Logging.error("{0}/{1}/{2}: {3}", tile.getZoom(), tile.getXtile(), tile.getYtile(), primitive.get("key"));
            throw e;
        }
    }

    private <T extends Tile & VectorTile> VectorPrimitive shapeToPrimaryFeatureObject(
            T tile, Layer layer, Shape shape, List<VectorPrimitive> featureObjects) {
        final VectorPrimitive primitive;
        if (shape instanceof Ellipse2D) {
            primitive = pointToNode(tile, layer, featureObjects,
                    (int) ((Ellipse2D) shape).getCenterX(), (int) ((Ellipse2D) shape).getCenterY());
        } else if (shape instanceof Path2D) {
            primitive = pathToWay(tile, layer, featureObjects, (Path2D) shape).stream().findFirst().orElse(null);
        } else if (shape instanceof Area) {
            primitive = areaToRelation(tile, layer, featureObjects, (Area) shape);
            primitive.put(RELATION_TYPE, MULTIPOLYGON_TYPE);
        } else {
            // We shouldn't hit this, but just in case
            throw new UnsupportedOperationException(Objects.toString(shape));
        }
        return primitive;
    }

    @Override
    public void destroy() {
        this.addedTiles.forEach(tile -> tile.setLoaded(false));
        this.addedTiles.forEach(tile -> tile.setImage(null));
        this.addedTiles.clear();
        this.store.clear();
        this.allPrimitives.clear();
        this.primitivesMap.clear();
    }
}
