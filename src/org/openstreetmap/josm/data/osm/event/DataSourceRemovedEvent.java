// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.AbstractDataSourceChangeEvent;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A data source was removed
 *
 * @author Taylor Smock
 * @since 15609
 */
public class DataSourceRemovedEvent extends AbstractDataSourceChangeEvent {
    private Set<DataSource> current;
    private final Set<DataSource> removed;
    private Set<DataSource> added;

    /**
     * Create a Data Source change event
     *
     * @param source             The DataSet that is originating the change
     * @param old                The previous set of DataSources
     * @param removedDataSources The data sources that are being removed
     */
    public DataSourceRemovedEvent(DataSet source, Set<DataSource> old, Stream<DataSource> removedDataSources) {
        super(source, old);
        CheckParameterUtil.ensureParameterNotNull(removedDataSources, "removedDataSources");
        this.removed = removedDataSources.collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<DataSource> getDataSources() {
        if (current == null) {
            current = getOldDataSources().stream().filter(s -> !removed.contains(s))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return current;
    }

    @Override
    public synchronized Set<DataSource> getRemoved() {
        return removed;
    }

    @Override
    public synchronized Set<DataSource> getAdded() {
        if (added == null) {
            added = getDataSources().stream().filter(s -> !getOldDataSources().contains(s))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return added;
    }

    @Override
    public String toString() {
        return "DataSourceAddedEvent [current=" + current + ", removed=" + removed + ", added=" + added + ']';
    }
}

