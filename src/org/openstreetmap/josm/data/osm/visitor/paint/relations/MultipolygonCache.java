package org.openstreetmap.josm.data.osm.visitor.paint.relations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
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
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/*
 * A memory cache for Multipolygon objects.
 * 
 */
public class MultipolygonCache implements DataSetListener, LayerChangeListener, ZoomChangeListener, ProjectionChangeListener {

    private static final MultipolygonCache instance = new MultipolygonCache(); 
    
    private final Map<NavigatableComponent, Map<DataSet, Map<Relation, Multipolygon>>> cache;
    
    private MultipolygonCache() {
        this.cache = new HashMap<NavigatableComponent, Map<DataSet, Map<Relation,Multipolygon>>>();
        Main.addProjectionChangeListener(this);
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
                map2.put(r, multipolygon = new Multipolygon(nc, r));
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
    
    private static final void removeMultipolygonFrom(Relation mp, Collection<Map<Relation, Multipolygon>> maps) {
        for (Map<Relation, Multipolygon> map : maps) {
            map.remove(mp);
        }
    }

    private final void removeMultipolygonsReferringTo(AbstractDatasetChangedEvent event) {
        removeMultipolygonsReferringTo(event.getPrimitives(), event.getDataset());
    }

    private final void removeMultipolygonsReferringTo(Collection<? extends OsmPrimitive> primitives, DataSet ds) {
        removeMultipolygonsReferringTo(primitives, ds, null);
    }
    
    private final Collection<Map<Relation, Multipolygon>> removeMultipolygonsReferringTo(
            Collection<? extends OsmPrimitive> primitives, DataSet ds, Collection<Map<Relation, Multipolygon>> initialMaps) {
        Collection<Map<Relation, Multipolygon>> maps = initialMaps;
        if (primitives != null) {
            for (OsmPrimitive p : primitives) {
                if (isMultipolygon(p)) {
                    if (maps == null) {
                        maps = getMapsFor(ds);
                    }
                    removeMultipolygonFrom((Relation) p, maps);
                    
                } else if (p instanceof Way && p.getDataSet() != null) {
                    for (OsmPrimitive ref : p.getReferrers()) {
                        if (isMultipolygon(ref)) {
                            if (maps == null) {
                                maps = getMapsFor(ds);
                            }
                            removeMultipolygonFrom((Relation) ref, maps);
                        }
                    }
                } else if (p instanceof Node && p.getDataSet() != null) {
                    maps = removeMultipolygonsReferringTo(p.getReferrers(), ds, maps);
                }
            }
        }
        return maps;
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        // Do nothing
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        removeMultipolygonsReferringTo(event);
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        removeMultipolygonsReferringTo(event);
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        removeMultipolygonsReferringTo(event);
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        removeMultipolygonsReferringTo(event);
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        removeMultipolygonsReferringTo(event);
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        removeMultipolygonsReferringTo(event);
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        removeMultipolygonsReferringTo(event);
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
    public void zoomChanged(/*NavigatableComponent source*/) {
        // TODO Change zoomChanged() method to add a "NavigatableComponent source" argument ? (this method is however used at least by one plugin)
        //clear(source);
        clear();
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        clear();
    }
}