// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.data.osm.TagCollection;

public class TagConflictResolverModel extends DefaultTableModel {
    static public final String RESOLVED_COMPLETELY_PROP = TagConflictResolverModel.class.getName() + ".resolvedCompletely";

    private TagCollection tags;
    private List<String> keys;
    private HashMap<String, MultiValueResolutionDecision> decisions;
    private boolean resolvedCompletely;
    private PropertyChangeSupport support;

    public TagConflictResolverModel() {
        resolvedCompletely= false;
        support = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    protected void setResolvedCompletely(boolean resolvedCompletey) {
        boolean oldValue = this.resolvedCompletely;
        this.resolvedCompletely = resolvedCompletey;
        if (oldValue != this.resolvedCompletely) {
            support.firePropertyChange(RESOLVED_COMPLETELY_PROP, oldValue, this.resolvedCompletely);
        }
    }

    protected void refreshResolvedCompletely() {
        for (MultiValueResolutionDecision d : decisions.values()) {
            if (!d.isDecided()) {
                setResolvedCompletely(false);
                return;
            }
        }
        setResolvedCompletely(true);
    }

    protected void sort() {
        Collections.sort(
                keys,
                new Comparator<String>() {
                    public int compare(String o1, String o2) {
                        if (decisions.get(o1).isDecided() && ! decisions.get(o2).isDecided())
                            return 1;
                        else if (!decisions.get(o1).isDecided() && decisions.get(o2).isDecided())
                            return -1;
                        return o1.compareTo(o2);
                    }
                }
        );
    }

    protected void init() {
        keys.clear();
        keys.addAll(tags.getKeys());
        for(String key: tags.getKeys()) {
            MultiValueResolutionDecision decision = new MultiValueResolutionDecision(tags.getTagsFor(key));
            decisions.put(key,decision);
        }
        refreshResolvedCompletely();
    }

    public void populate(TagCollection tags) {
        if (tags == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "tags"));
        this.tags = tags;
        keys = new ArrayList<String>();
        decisions = new HashMap<String, MultiValueResolutionDecision>();
        init();
        sort();
        fireTableDataChanged();
    }


    @Override
    public int getRowCount() {
        if (keys == null) return 0;
        return keys.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return decisions.get(keys.get(row));
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 2;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        MultiValueResolutionDecision decision = decisions.get(keys.get(row));
        if (value instanceof String) {
            decision.keepOne((String)value);
        } else if (value instanceof MultiValueDecisionType) {
            MultiValueDecisionType type = (MultiValueDecisionType)value;
            switch(type) {
                case KEEP_NONE:
                    decision.keepNone();
                    break;
                case KEEP_ALL:
                    decision.keepAll();
                    break;
            }
        }
        fireTableDataChanged();
        refreshResolvedCompletely();
    }

    /**
     * Replies true if each {@see MultiValueResolutionDecision} is decided.
     * 
     * @return true if each {@see MultiValueResolutionDecision} is decided; false
     * otherwise
     */
    public boolean isResolvedCompletely() {
        return resolvedCompletely;
    }

    public TagCollection getResolution() {
        TagCollection tc = new TagCollection();
        for (String key: keys) {
            tc.add(decisions.get(key).getResolution());
        }
        return tc;
    }
}
