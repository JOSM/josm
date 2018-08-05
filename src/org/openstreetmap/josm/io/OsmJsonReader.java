// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;

/**
 * Parser for the Osm API (JSON output). Read from an input stream and construct a dataset out of it.
 *
 * For each json element, there is a dedicated method.
 * @since 14086
 */
public class OsmJsonReader extends AbstractReader {

    protected JsonParser parser;

    /**
     * constructor (for private and subclasses use only)
     *
     * @see #parseDataSet(InputStream, ProgressMonitor)
     */
    protected OsmJsonReader() {
        // Restricts visibility
    }

    protected void setParser(JsonParser parser) {
        this.parser = parser;
    }

    protected void parse() throws IllegalDataException {
        while (parser.hasNext()) {
            Event event = parser.next();
            if (event == Event.START_OBJECT) {
                parseRoot(parser.getObject());
            }
        }
        parser.close();
    }

    private void parseRoot(JsonObject object) throws IllegalDataException {
        parseVersion(object.get("version").toString());
        parseDownloadPolicy("download", object.getString("download", null));
        parseUploadPolicy("upload", object.getString("upload", null));
        parseLocked(object.getString("locked", null));
        parseElements(object.getJsonArray("elements"));
    }

    private void parseElements(JsonArray jsonArray) throws IllegalDataException {
        for (JsonValue value : jsonArray) {
            if (value instanceof JsonObject) {
                JsonObject item = (JsonObject) value;
                switch (item.getString("type")) {
                case "node":
                    parseNode(item);
                    break;
                case "way":
                    parseWay(item);
                    break;
                case "relation":
                    parseRelation(item);
                    break;
                default:
                    parseUnknown(item);
                }
            } else {
                throw new IllegalDataException("Unexpected JSON item: " + value);
            }
        }
    }

    /**
     * Read out the common attributes and put them into current OsmPrimitive.
     * @param item current JSON object
     * @param current primitive to update
     * @throws IllegalDataException if there is an error processing the underlying JSON source
     */
    private void readCommon(JsonObject item, PrimitiveData current) throws IllegalDataException {
        try {
            parseId(current, item.getJsonNumber("id").longValue());
            parseTimestamp(current, item.getString("timestamp", null));
            JsonNumber uid = item.getJsonNumber("uid");
            if (uid != null) {
                parseUser(current, item.getString("user", null), uid.longValue());
            }
            parseVisible(current, item.getString("visible", null));
            JsonNumber version = item.getJsonNumber("version");
            if (version != null) {
                parseVersion(current, version.intValue());
            }
            parseAction(current, item.getString("action", null));
            JsonNumber changeset = item.getJsonNumber("changeset");
            if (changeset != null) {
                parseChangeset(current, changeset.intValue());
            }
        } catch (UncheckedParseException e) {
            throw new IllegalDataException(e);
        }
    }

    private void readTags(JsonObject item, Tagged t) {
        JsonObject tags = item.getJsonObject("tags");
        if (tags != null) {
            for (Entry<String, JsonValue> entry : tags.entrySet()) {
                t.put(entry.getKey(), ((JsonString) entry.getValue()).getString());
            }
        }
    }

    private void parseNode(JsonObject item) throws IllegalDataException {
        parseNode(item.getJsonNumber("lat").doubleValue(),
                  item.getJsonNumber("lon").doubleValue(), nd -> readCommon(item, nd), n -> readTags(item, n));
    }

    private void parseWay(JsonObject item) throws IllegalDataException {
        parseWay(wd -> readCommon(item, wd), (w, nodeIds) -> readWayNodesAndTags(item, w, nodeIds));
    }

    private void readWayNodesAndTags(JsonObject item, Way w, Collection<Long> nodeIds) {
        for (JsonValue v : item.getJsonArray("nodes")) {
            nodeIds.add(((JsonNumber) v).longValue());
        }
        readTags(item, w);
    }

    private void parseRelation(JsonObject item) throws IllegalDataException {
        parseRelation(rd -> readCommon(item, rd), (r, members) -> readRelationMembersAndTags(item, r, members));
    }

    private void readRelationMembersAndTags(JsonObject item, Relation r, Collection<RelationMemberData> members)
            throws IllegalDataException {
        for (JsonValue v : item.getJsonArray("members")) {
            JsonObject o = v.asJsonObject();
            members.add(parseRelationMember(r, ((JsonNumber) o.get("ref")).longValue(), o.getString("type"), o.getString("role")));
        }
        readTags(item, r);
    }

    protected void parseUnknown(JsonObject element, boolean printWarning) {
        if (printWarning) {
            Logging.info(tr("Undefined element ''{0}'' found in input stream. Skipping.", element));
        }
    }

    private void parseUnknown(JsonObject element) {
        parseUnknown(element, true);
    }

    @Override
    protected DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return doParseDataSet(source, progressMonitor, ir -> {
            setParser(Json.createParser(ir));
            parse();
        });
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source the source input stream. Must not be null.
     * @param progressMonitor the progress monitor. If null, {@link NullProgressMonitor#INSTANCE} is assumed
     *
     * @return the dataset with the parsed data
     * @throws IllegalDataException if an error was found while parsing the data from the source
     * @throws IllegalArgumentException if source is null
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new OsmJsonReader().doParseDataSet(source, progressMonitor);
    }
}
