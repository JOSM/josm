// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Set;

import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * The base class for data source change events
 *
 * @author Taylor Smock
 * @since 15609
 */
public abstract class AbstractDataSourceChangeEvent implements DataSourceChangeEvent {

    private DataSet source;
    private Set<DataSource> old;

    /**
     * Create a Data Source change event
     *
     * @param source The DataSet that is originating the change
     * @param old    The previous set of DataSources
     */
    public AbstractDataSourceChangeEvent(DataSet source, Set<DataSource> old) {
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        CheckParameterUtil.ensureParameterNotNull(old, "old");
        this.source = source;
        this.old = old;
    }

    @Override
    public Set<DataSource> getOldDataSources() {
        return old;
    }

    @Override
    public DataSet getSource() {
        return source;
    }
}

