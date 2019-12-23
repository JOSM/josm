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
 * There is a new data source
 *
 * @author Taylor Smock
 * @since 15609
 */
public class DataSourceAddedEvent extends AbstractDataSourceChangeEvent {
    private Set<DataSource> current;
    private Set<DataSource> removed;
    private final Set<DataSource> added;

    /**
     * Create a Data Source change event
     *
     * @param source         The DataSet that is originating the change
     * @param old            The previous set of DataSources
     * @param newDataSources The data sources that are being added
     */
    public DataSourceAddedEvent(DataSet source, Set<DataSource> old, Stream<DataSource> newDataSources) {
        super(source, old);
        CheckParameterUtil.ensureParameterNotNull(newDataSources, "newDataSources");
        this.added = newDataSources.collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<DataSource> getDataSources() {
        if (current == null) {
            current = new LinkedHashSet<>(getOldDataSources());
            current.addAll(added);
        }
        return current;
    }

    @Override
    public synchronized Set<DataSource> getRemoved() {
        if (removed == null) {
            removed = getOldDataSources().stream().filter(s -> !getDataSources().contains(s))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return removed;
    }

    @Override
    public synchronized Set<DataSource> getAdded() {
        return added;
    }

    @Override
    public String toString() {
        return "DataSourceAddedEvent [current=" + current + ", removed=" + removed + ", added=" + added + ']';
    }
}

