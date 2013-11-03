// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint.relations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
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
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/*
 * A memory cache for Multipolygon objects.
 * 
 */
public final class MultipolygonCache implements DataSetListener, LayerChangeListener, ProjectionChangeListener, SelectionChangedListener {

    private static final MultipolygonCache instance = new MultipolygonCache(); 
    
    private final Map<NavigatableComponent, Map<DataSet, Map<Relation, Multipolygon>>> cache;
    
    private final Collection<PolyData> selectedPolyData;
    
    private MultipolygonCache() {
        this.cache = new HashMap<NavigatableComponent, Map<DataSet, Map<Relation, Multipolygon>>>();
        this.selectedPolyData = new ArrayList<Multipolygon.PolyData>();
        Main.addProjectionChangeListener(this);
        DataSet.addSelectionListener(this);
        MapView.addLayerChangeListener(this);
    }

    public static final MultipolygonCache getInstance() {
        return instance;
    }

    public final Multipolygon get(NavigatableComponent nc, Relation r) {
        return get(nc, r, false);
    }

    public final Multipolygon get(NavigatableComponent nc, Relation r, boolean forceRefresh) {
        Multipolygon multipolygon = null;
        if (nc != null && r != null) {
            Map<DataSet, Map<Relation, Multipolygon>> map1 = cache.get(nc);
            if (map1 == null) {
                cache.put(nc, map1 = new HashMap<DataSet, Map<Relation, Multipolygon>>());
            }
            Map<Relation, Multipolygon> map2 = map1.get(r.getDataSet());
            if (map2 == null) {
                map1.put(r.getDataSet(), map2 = new HashMap<Relation, Multipolygon>());
            }
            multipolygon = map2.get(r);
            if (multipolygon == null || forceRefresh) {
                map2.put(r, multipolygon = new Multipolygon(r));
                for (PolyData pd : multipolygon.getCombinedPolygons()) {
                    if (pd.selected) {
                        selectedPolyData.add(pd);
                    }
                }
            }
        }
        return multipolygon;
    }
    
    public final void clear(NavigatableComponent nc) {
        Map<DataSet, Map<Relation, Multipolygon>> map = cache.remove(nc);
        if (map != null) {
            map.clear();
            map = null;
        }
    }

    public final void clear(DataSet ds) {
        for (Map<DataSet, Map<Relation, Multipolygon>> map1 : cache.values()) {
            Map<Relation, Multipolygon> map2 = map1.remove(ds);
            if (map2 != null) {
                map2.clear();
                map2 = null;
            }
        }
    }

    public final void clear() {
        cache.clear();
    }
    
    private final Collection<Map<Relation, Multipolygon>> getMapsFor(DataSet ds) {
        List<Map<Relation, Multipolygon>> result = new ArrayList<Map<Relation, Multipolygon>>();
        for (Map<DataSet, Map<Relation, Multipolygon>> map : cache.values()) {
            Map<Relation, Multipolygon> map2 = map.get(ds);
            if (map2 != null) {
                result.add(map2);
            }
        }
        return result;
    }
    
    private static final boolean isMultipolygon(OsmPrimitive p) {
        return p instanceof Relation && ((Relation) p).isMultipolygon();
    }
    
    private final void updateMultipolygonsReferringTo(AbstractDatasetChangedEvent event) {
        updateMultipolygonsReferringTo(event, event.getPrimitives(), event.getDataset());
    }

    private final void updateMultipolygonsReferringTo(
            final AbstractDatasetChangedEvent event, Collection<? extends OsmPrimitive> primitives, DataSet ds) {
        updateMultipolygonsReferringTo(event, primitives, ds, null);
    }
    
    private final Collection<Map<Relation, Multipolygon>> updateMultipolygonsReferringTo(
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
    
    private final void processEvent(AbstractDatasetChangedEvent event, Relation r, Collection<Map<Relation, Multipolygon>> maps) {
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
    
    private final void dispatchEvent(AbstractDatasetChangedEvent event, Relation r, Collection<Map<Relation, Multipolygon>> maps) {
        for (Map<Relation, Multipolygon> map : maps) {
            Multipolygon m = map.get(r);
            if (m != null) {
                for (PolyData pd : m.getCombinedPolygons()) {
                    if (event instanceof NodeMovedEvent) {
                        pd.nodeMoved((NodeMovedEvent) event);
                    } else if (event instanceof WayNodesChangedEvent) {
                        pd.wayNodesChanged((WayNodesChangedEvent)event);
                    }
                }
            }
        }
    }
    
    private final void removeMultipolygonFrom(Relation r, Collection<Map<Relation, Multipolygon>> maps) {
        for (Map<Relation, Multipolygon> map : maps) {
            map.remove(r);
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
        // Do nothing
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
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        // Do nothing
    }

    @Override
    public void layerAdded(Layer newLayer) {
        // Do nothing
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof OsmDataLayer) {
            clear(((OsmDataLayer) oldLayer).data);
        }
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        clear();
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        
        for (Iterator<PolyData> it = selectedPolyData.iterator(); it.hasNext();) {
            it.next().selected = false;
            it.remove();
        }
        
        DataSet ds = null;
        Collection<Map<Relation, Multipolygon>> maps = null;
        for (OsmPrimitive p : newSelection) {
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
                                        pd.selected = true;
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