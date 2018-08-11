// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint.relations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * A memory cache for {@link Multipolygon} objects.
 * @since 4623
 */
public final class MultipolygonCache implements DataSetListener, LayerChangeListener, ProjectionChangeListener, DataSelectionListener {

    private static final MultipolygonCache INSTANCE = new MultipolygonCache();

    private final Map<DataSet, Map<Relation, Multipolygon>> cache = new ConcurrentHashMap<>(); // see ticket 11833

    private final Collection<PolyData> selectedPolyData = new ArrayList<>();

    private MultipolygonCache() {
        ProjectionRegistry.addProjectionChangeListener(this);
        SelectionEventManager.getInstance().addSelectionListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
    }

    /**
     * Replies the unique instance.
     * @return the unique instance
     */
    public static MultipolygonCache getInstance() {
        return INSTANCE;
    }

    /**
     * Gets a multipolygon from cache.
     * @param r The multipolygon relation
     * @return A multipolygon object for the given relation, or {@code null}
     * @since 11779
     */
    public Multipolygon get(Relation r) {
        return get(r, false);
    }

    /**
     * Gets a multipolygon from cache.
     * @param r The multipolygon relation
     * @param forceRefresh if {@code true}, a new object will be created even of present in cache
     * @return A multipolygon object for the given relation, or {@code null}
     * @since 11779
     */
    public Multipolygon get(Relation r, boolean forceRefresh) {
        Multipolygon multipolygon = null;
        if (r != null && r.getDataSet() != null) {
            Map<Relation, Multipolygon> map2 = cache.get(r.getDataSet());
            if (map2 == null) {
                map2 = new ConcurrentHashMap<>();
                cache.put(r.getDataSet(), map2);
            }
            multipolygon = map2.get(r);
            if (multipolygon == null || forceRefresh) {
                multipolygon = new Multipolygon(r);
                map2.put(r, multipolygon);
                synchronized (this) {
                    for (PolyData pd : multipolygon.getCombinedPolygons()) {
                        if (pd.isSelected()) {
                            selectedPolyData.add(pd);
                        }
                    }
                }
            }
        }
        return multipolygon;
    }

    /**
     * Clears the cache for the given dataset.
     * @param ds the data set
     */
    public void clear(DataSet ds) {
        Map<Relation, Multipolygon> map2 = cache.remove(ds);
        if (map2 != null) {
            map2.clear();
        }
    }

    /**
     * Clears the whole cache.
     */
    public void clear() {
        cache.clear();
    }

    private Collection<Map<Relation, Multipolygon>> getMapsFor(DataSet ds) {
        List<Map<Relation, Multipolygon>> result = new ArrayList<>();
        Map<Relation, Multipolygon> map2 = cache.get(ds);
        if (map2 != null) {
            result.add(map2);
        }
        return result;
    }

    private static boolean isMultipolygon(OsmPrimitive p) {
        return p instanceof Relation && ((Relation) p).isMultipolygon();
    }

    private void updateMultipolygonsReferringTo(AbstractDatasetChangedEvent event) {
        updateMultipolygonsReferringTo(event, event.getPrimitives(), event.getDataset());
    }

    private void updateMultipolygonsReferringTo(
            final AbstractDatasetChangedEvent event, Collection<? extends OsmPrimitive> primitives, DataSet ds) {
        updateMultipolygonsReferringTo(event, primitives, ds, null);
    }

    private Collection<Map<Relation, Multipolygon>> updateMultipolygonsReferringTo(
            AbstractDatasetChangedEvent event, Collection<? extends OsmPrimitive> primitives,
            DataSet ds, Collection<Map<Relation, Multipolygon>> initialMaps) {
        Collection<Map<Relation, Multipolygon>> maps = initialMaps;
        if (primitives != null) {
            for (OsmPrimitive p : primitives) {
                if (isMultipolygon(p)) {
                    if (maps == null) {
                        maps = getMapsFor(ds);
                    }
                    processEvent(event, (Relation) p, maps);

                } else if (p instanceof Way && p.getDataSet() != null) {
                    for (OsmPrimitive ref : p.getReferrers()) {
                        if (isMultipolygon(ref)) {
                            if (maps == null) {
                                maps = getMapsFor(ds);
                            }
                            processEvent(event, (Relation) ref, maps);
                        }
                    }
                } else if (p instanceof Node && p.getDataSet() != null) {
                    maps = updateMultipolygonsReferringTo(event, p.getReferrers(), ds, maps);
                }
            }
        }
        return maps;
    }

    private static void processEvent(AbstractDatasetChangedEvent event, Relation r, Collection<Map<Relation, Multipolygon>> maps) {
        if (event instanceof NodeMovedEvent || event instanceof WayNodesChangedEvent) {
            dispatchEvent(event, r, maps);
        } else if (event instanceof PrimitivesRemovedEvent) {
            if (event.getPrimitives().contains(r)) {
                removeMultipolygonFrom(r, maps);
            }
        } else {
            // Default (non-optimal) action: remove multipolygon from cache
            removeMultipolygonFrom(r, maps);
        }
    }

    private static void dispatchEvent(AbstractDatasetChangedEvent event, Relation r, Collection<Map<Relation, Multipolygon>> maps) {
        for (Map<Relation, Multipolygon> map : maps) {
            Multipolygon m = map.get(r);
            if (m != null) {
                for (PolyData pd : m.getCombinedPolygons()) {
                    if (event instanceof NodeMovedEvent) {
                        pd.nodeMoved((NodeMovedEvent) event);
                    } else if (event instanceof WayNodesChangedEvent) {
                        final boolean oldClosedStatus = pd.isClosed();
                        pd.wayNodesChanged((WayNodesChangedEvent) event);
                        if (pd.isClosed() != oldClosedStatus) {
                            removeMultipolygonFrom(r, maps); // see ticket #13591
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void removeMultipolygonFrom(Relation r, Collection<Map<Relation, Multipolygon>> maps) {
        for (Map<Relation, Multipolygon> map : maps) {
            map.remove(r);
        }
        // Erase style cache for polygon members
        for (OsmPrimitive member : r.getMemberPrimitivesList()) {
            member.clearCachedStyle();
        }
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        // Do nothing
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        updateMultipolygonsReferringTo(event);
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        updateMultipolygonsReferringTo(event);
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        updateMultipolygonsReferringTo(event);
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        updateMultipolygonsReferringTo(event);
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        updateMultipolygonsReferringTo(event);
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // Do nothing
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        // Do not call updateMultipolygonsReferringTo as getPrimitives()
        // can return all the data set primitives for this event
        Collection<Map<Relation, Multipolygon>> maps = null;
        for (OsmPrimitive p : event.getPrimitives()) {
            if (isMultipolygon(p)) {
                if (maps == null) {
                    maps = getMapsFor(event.getDataset());
                }
                for (Map<Relation, Multipolygon> map : maps) {
                    // DataChangedEvent is sent after downloading incomplete members (see #7131),
                    // without having received RelationMembersChangedEvent or PrimitivesAddedEvent
                    // OR when undoing a move of a large number of nodes (see #7195),
                    // without having received NodeMovedEvent
                    // This ensures concerned multipolygons will be correctly redrawn
                    map.remove(p);
                }
            }
        }
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        // Do nothing
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.getRemovedLayer() instanceof OsmDataLayer) {
            clear(((OsmDataLayer) e.getRemovedLayer()).data);
        }
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        clear();
    }

    @Override
    public synchronized void selectionChanged(SelectionChangeEvent event) {

        for (Iterator<PolyData> it = selectedPolyData.iterator(); it.hasNext();) {
            it.next().setSelected(false);
            it.remove();
        }

        DataSet ds = null;
        Collection<Map<Relation, Multipolygon>> maps = null;
        for (OsmPrimitive p : event.getSelection()) {
            if (p instanceof Way && p.getDataSet() != null) {
                if (ds == null) {
                    ds = p.getDataSet();
                }
                for (OsmPrimitive ref : p.getReferrers()) {
                    if (isMultipolygon(ref)) {
                        if (maps == null) {
                            maps = getMapsFor(ds);
                        }
                        for (Map<Relation, Multipolygon> map : maps) {
                            Multipolygon multipolygon = map.get(ref);
                            if (multipolygon != null) {
                                for (PolyData pd : multipolygon.getCombinedPolygons()) {
                                    if (pd.getWayIds().contains(p.getUniqueId())) {
                                        pd.setSelected(true);
                                        selectedPolyData.add(pd);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
