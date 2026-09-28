// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Add primitives to a data layer.
 * @since 2305
 */
public class AddPrimitivesCommand extends Command {

    private List<PrimitiveData> primitiveData;
    private Collection<PrimitiveData> toSelect;
    private List<PrimitiveData> preExistingData;

    // only filled on undo
    private List<OsmPrimitive> createdPrimitives;

    /**
     * Constructs a new {@code AddPrimitivesCommand} to add data to the given data set.
     * @param data The OSM primitives data to add. Must not be {@code null}
     * @param toSelect The OSM primitives to select at the end. Can be {@code null}
     * @param ds The target data set. Must not be {@code null}
     * @since 12718
     */
    public AddPrimitivesCommand(List<PrimitiveData> data, List<PrimitiveData> toSelect, DataSet ds) {
        super(ds);
        init(data, toSelect);
    }

    /**
     * Constructs a new {@code AddPrimitivesCommand} to add data to the given data set.
     * @param data The OSM primitives data to add and select. Must not be {@code null}
     * @param ds The target data set. Must not be {@code null}
     * @since 12726
     */
    public AddPrimitivesCommand(List<PrimitiveData> data, DataSet ds) {
        this(data, data, ds);
    }

    private void init(List<PrimitiveData> data, List<PrimitiveData> toSelect) {
        CheckParameterUtil.ensureParameterNotNull(data, "data");
        this.primitiveData = new ArrayList<>(data);
        if (toSelect == data) {
            this.toSelect = this.primitiveData;
        } else if (toSelect != null) {
            this.toSelect = new ArrayList<>(toSelect);
        }
    }

    @Override
    public boolean executeCommand() {
        DataSet ds = getAffectedDataSet();
        ds.update(() -> this.executeRealCommand(ds));
        if (toSelect != null) {
            ds.setSelected(toSelect.stream().map(ds::getPrimitiveById).collect(Collectors.toList()));
        }
        return true;
    }

    private void executeRealCommand(DataSet ds) {
        if (createdPrimitives == null) { // first time execution
            List<OsmPrimitive> newPrimitives = new ArrayList<>(primitiveData.size());
            preExistingData = new ArrayList<>();

            for (PrimitiveData pd : primitiveData) {
                OsmPrimitive primitive = ds.getPrimitiveById(pd);
                boolean created = primitive == null;
                if (primitive == null) {
                    primitive = pd.getType().newInstance(pd.getUniqueId(), true);
                } else {
                    preExistingData.add(primitive.save());
                }
                if (pd instanceof NodeData) { // Load nodes immediately because they can't be added to dataset without coordinates
                    primitive.load(pd);
                }
                if (created) {
                    ds.addPrimitive(primitive);
                }
                newPrimitives.add(primitive);
            }

            // Then load ways and relations
            for (int i = 0; i < newPrimitives.size(); i++) {
                if (!(newPrimitives.get(i) instanceof Node)) {
                    newPrimitives.get(i).load(primitiveData.get(i));
                }
            }
            newPrimitives.forEach(p -> p.setModified(true));
        } else { // redo
            // When redoing this command, we have to add the same objects, otherwise
            // a subsequent command (e.g. MoveCommand) cannot be redone.
            for (OsmPrimitive osm : createdPrimitives) {
                if (preExistingData.stream().anyMatch(pd -> pd.getPrimitiveId().equals(osm.getPrimitiveId()))) {
                    Optional<PrimitiveData> o = primitiveData.stream()
                            .filter(pd -> pd.getPrimitiveId().equals(osm.getPrimitiveId())).findAny();
                    o.ifPresent(osm::load);
                } else {
                    ds.addPrimitive(osm);
                }
            }
        }
    }

    @Override public void undoCommand() {
        DataSet ds = getAffectedDataSet();
        if (createdPrimitives == null) {
            createdPrimitives = new ArrayList<>(primitiveData.size());
            for (PrimitiveData pd : primitiveData) {
                OsmPrimitive p = ds.getPrimitiveById(pd);
                createdPrimitives.add(p);
            }
            createdPrimitives = PurgeCommand.topoSort(createdPrimitives);
        }
        // reversed order, see #14620
        final List<PrimitiveId> toRemove = new ArrayList<>(this.createdPrimitives.size());
        ds.update(() -> {
                    for (int i = createdPrimitives.size() - 1; i >= 0; i--) {
                        OsmPrimitive osm = createdPrimitives.get(i);
                        Optional<PrimitiveData> previous = preExistingData.stream()
                                .filter(pd -> pd.getPrimitiveId().equals(osm.getPrimitiveId())).findAny();
                        if (previous.isPresent()) {
                            osm.load(previous.get());
                        } else {
                            toRemove.add(osm);
                        }
                    }
                });
        ds.removePrimitives(toRemove);
    }

    @Override
    public String getDescriptionText() {
        int size = primitiveData != null ? primitiveData.size() : createdPrimitives.size();
        return trn("Added {0} object", "Added {0} objects", size, size);
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        // Does nothing because we don't want to create OsmPrimitives.
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        if (createdPrimitives != null)
            return createdPrimitives;

        return primitiveData.stream()
                .map(d -> Objects.requireNonNull(getAffectedDataSet().getPrimitiveById(d), () -> "No primitive found for " + d))
                .collect(Collectors.toSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), primitiveData, toSelect, preExistingData, createdPrimitives);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        AddPrimitivesCommand that = (AddPrimitivesCommand) obj;
        return Objects.equals(primitiveData, that.primitiveData) &&
               Objects.equals(toSelect, that.toSelect) &&
               Objects.equals(preExistingData, that.preExistingData) &&
               Objects.equals(createdPrimitives, that.createdPrimitives);
    }
}
