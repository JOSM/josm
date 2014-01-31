// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;

/**
 * Fixes defective data entries for all modified objects before upload
 */
public class FixDataHook implements UploadHook {

    /**
     * List of checks to run on data
     */
    private List<FixData> deprecated = new LinkedList<FixData>();

    /**
     * Constructor for data initialization
     */
    public FixDataHook () {
        deprecated.add(new FixDataSpace());
        deprecated.add(new FixDataKey("color",            "colour"));
        deprecated.add(new FixDataTag("highway", "ford",  "ford",    "yes"));
        deprecated.add(new FixDataTag("oneway",  "false", "oneway",  "no"));
        deprecated.add(new FixDataTag("oneway",  "0",     "oneway",  "no"));
        deprecated.add(new FixDataTag("oneway",  "true",  "oneway",  "yes"));
        deprecated.add(new FixDataTag("oneway",  "1",     "oneway",  "yes"));
        deprecated.add(new FixDataTag("highway", "stile", "barrier", "stile"));
        deprecated.add(new FixData() {
            @Override
            public boolean fixKeys(Map<String, String> keys, OsmPrimitive osm) {
                if(osm instanceof Relation && "multipolygon".equals(keys.get("type")) && "administrative".equals(keys.get("boundary"))) {
                    keys.put("type", "boundary");
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Common set of commands for data fixing
     */
    public interface FixData {
        /**
         * Checks if data needs to be fixed and change keys
         *
         * @param keys list of keys to be modified
         * @param osm the object for type validation, don't use keys of it!
         * @return <code>true</code> if keys have been modified
         */
        public boolean fixKeys(Map<String, String> keys, OsmPrimitive osm);
    }

    /**
     * Data fix to remove spaces at begin or end of tags
     */
    public static class FixDataSpace implements FixData {
        @Override
        public boolean fixKeys(Map<String, String> keys, OsmPrimitive osm) {
            Map<String, String> newKeys = new HashMap<String, String>(keys);
            for (Entry<String, String> e : keys.entrySet()) {
                String v = Tag.removeWhiteSpaces(e.getValue());
                String k = Tag.removeWhiteSpaces(e.getKey());
                if(!e.getKey().equals(k)) {
                    boolean drop = k.isEmpty() || v.isEmpty();
                    if(drop || !keys.containsKey(k)) {
                        newKeys.remove(e.getKey());
                        if(!drop)
                            newKeys.put(k, v);
                    }
                } else if(!e.getValue().equals(v)) {
                    if(v.isEmpty())
                        newKeys.remove(k);
                    else
                        newKeys.put(k, v);
                }
            }
            boolean changed = !keys.equals(newKeys);
            if (changed) {
                keys.clear();
                keys.putAll(newKeys);
            }
            return changed;
        }
    }

    /**
     * Data fix to cleanup wrong spelled keys
     */
    public static class FixDataKey implements FixData {
        /** key of wrong data */
        String oldKey;
        /** key of correct data */
        String newKey;

        /**
         * Setup key check for wrong spelled keys
         *
         * @param oldKey wrong spelled key
         * @param newKey correct replacement
         */
        public FixDataKey(String oldKey, String newKey) {
            this.oldKey = oldKey;
            this.newKey = newKey;
        }

        @Override
        public boolean fixKeys(Map<String, String> keys, OsmPrimitive osm) {
            if(keys.containsKey(oldKey) && !keys.containsKey(newKey)) {
                keys.put(newKey, keys.get(oldKey));
                keys.remove(oldKey);
                return true;
            }
            return false;
        }
    }

    /**
     * Data fix to cleanup wrong spelled tags
     */
    public static class FixDataTag implements FixData {
        /** key of wrong data */
        String oldKey;
        /** value of wrong data */
        String oldValue;
        /** key of correct data */
        String newKey;
        /** value of correct data */
        String newValue;

        /**
         * Setup key check for wrong spelled keys
         *
         * @param oldKey wrong or old key
         * @param oldValue wrong or old value
         * @param newKey correct key replacement
         * @param newValue correct value replacement
         */
        public FixDataTag(String oldKey, String oldValue, String newKey, String newValue) {
            this.oldKey = oldKey;
            this.oldValue = oldValue;
            this.newKey = newKey;
            this.newValue = newValue;
        }

        @Override
        public boolean fixKeys(Map<String, String> keys, OsmPrimitive osm) {
            if(oldValue.equals(keys.get(oldKey)) && (newKey.equals(oldKey) || !keys.containsKey(newKey))) {
                keys.put(newKey, newValue);
                if(!newKey.equals(oldKey))
                    keys.remove(oldKey);
                return true;
            }
            return false;
        }
    }

    /**
     * Checks the upload for deprecated or wrong tags.
     * @param apiDataSet the data to upload
     */
    @Override
    public boolean checkUpload(APIDataSet apiDataSet) {
        if(!Main.pref.getBoolean("fix.data.on.upload", true))
            return true;

        List<OsmPrimitive> objectsToUpload = apiDataSet.getPrimitives();
        Collection<Command> cmds = new LinkedList<Command>();

        for (OsmPrimitive osm : objectsToUpload) {
            Map<String, String> keys = osm.getKeys();
            if(!keys.isEmpty()) {
                boolean modified = false;
                for (FixData fix : deprecated) {
                    if(fix.fixKeys(keys, osm))
                        modified = true;
                }
                if(modified)
                    cmds.add(new ChangePropertyCommand(Collections.singleton(osm), new HashMap<String, String>(keys)));
            }
        }

        if(!cmds.isEmpty())
            Main.main.undoRedo.add(new SequenceCommand(tr("Fix deprecated tags"), cmds));
        return true;
    }
}
