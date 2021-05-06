// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.protobuf.ProtobufPacked;
import org.openstreetmap.josm.data.protobuf.ProtobufParser;
import org.openstreetmap.josm.data.protobuf.ProtobufRecord;
import org.openstreetmap.josm.tools.Utils;

/**
 * A Feature for a {@link Layer}
 *
 * @author Taylor Smock
 * @since xxx
 */
public class Feature {
    private static final byte ID_FIELD = 1;
    private static final byte TAG_FIELD = 2;
    private static final byte GEOMETRY_TYPE_FIELD = 3;
    private static final byte GEOMETRY_FIELD = 4;
    /**
     * The geometry of the feature. Required.
     */
    private final List<CommandInteger> geometry = new ArrayList<>();

    /**
     * The geometry type of the feature. Required.
     */
    private final GeometryTypes geometryType;
    /**
     * The id of the feature. Optional.
     */
    // Technically, uint64
    private final long id;
    /**
     * The tags of the feature. Optional.
     */
    private TagMap tags;
    private Geometry geometryObject;

    /**
     * Create a new Feature
     *
     * @param layer  The layer the feature is part of (required for tags)
     * @param record The record to create the feature from
     * @throws IOException - if an IO error occurs
     */
    public Feature(Layer layer, ProtobufRecord record) throws IOException {
        long tId = 0;
        GeometryTypes geometryTypeTemp = GeometryTypes.UNKNOWN;
        String key = null;
        try (ProtobufParser parser = new ProtobufParser(record.getBytes())) {
            while (parser.hasNext()) {
                try (ProtobufRecord next = new ProtobufRecord(parser)) {
                    if (next.getField() == TAG_FIELD) {
                        if (tags == null) {
                            tags = new TagMap();
                        }
                        // This is packed in v1 and v2
                        ProtobufPacked packed = new ProtobufPacked(next.getBytes());
                        for (Number number : packed.getArray()) {
                            key = parseTagValue(key, layer, number);
                        }
                    } else if (next.getField() == GEOMETRY_FIELD) {
                        // This is packed in v1 and v2
                        ProtobufPacked packed = new ProtobufPacked(next.getBytes());
                        CommandInteger currentCommand = null;
                        for (Number number : packed.getArray()) {
                            if (currentCommand != null && currentCommand.hasAllExpectedParameters()) {
                                currentCommand = null;
                            }
                            if (currentCommand == null) {
                                currentCommand = new CommandInteger(number.intValue());
                                this.geometry.add(currentCommand);
                            } else {
                                currentCommand.addParameter(ProtobufParser.decodeZigZag(number));
                            }
                        }
                        // TODO fallback to non-packed
                    } else if (next.getField() == GEOMETRY_TYPE_FIELD) {
                        geometryTypeTemp = GeometryTypes.values()[next.asUnsignedVarInt().intValue()];
                    } else if (next.getField() == ID_FIELD) {
                        tId = next.asUnsignedVarInt().longValue();
                    }
                }
            }
        }
        this.id = tId;
        this.geometryType = geometryTypeTemp;
        record.close();
    }

    /**
     * Parse a tag value
     *
     * @param key    The current key (or {@code null}, if {@code null}, the returned value will be the new key)
     * @param layer  The layer with key/value information
     * @param number The number to get the value from
     * @return The new key (if {@code null}, then a value was parsed and added to tags)
     */
    private String parseTagValue(String key, Layer layer, Number number) {
        if (key == null) {
            key = layer.getKey(number.intValue());
        } else {
            Object value = layer.getValue(number.intValue());
            if (value instanceof Double || value instanceof Float) {
                // reset grouping if the instance is a singleton
                final NumberFormat numberFormat = NumberFormat.getNumberInstance();
                final boolean grouping = numberFormat.isGroupingUsed();
                try {
                    numberFormat.setGroupingUsed(false);
                    this.tags.put(key, numberFormat.format(value));
                } finally {
                    numberFormat.setGroupingUsed(grouping);
                }
            } else {
                this.tags.put(key, Utils.intern(value.toString()));
            }
            key = null;
        }
        return key;
    }

    /**
     * Get the geometry instructions
     *
     * @return The geometry
     */
    public List<CommandInteger> getGeometry() {
        return this.geometry;
    }

    /**
     * Get the geometry type
     *
     * @return The {@link GeometryTypes}
     */
    public GeometryTypes getGeometryType() {
        return this.geometryType;
    }

    /**
     * Get the id of the object
     *
     * @return The unique id in the layer, or 0.
     */
    public long getId() {
        return this.id;
    }

    /**
     * Get the tags
     *
     * @return A tag map
     */
    public TagMap getTags() {
        return this.tags;
    }

    /**
     * Get the an object with shapes for the geometry
     * @return An object with usable geometry information
     */
    public Geometry getGeometryObject() {
        if (this.geometryObject == null) {
            this.geometryObject = new Geometry(this.getGeometryType(), this.getGeometry());
        }
        return this.geometryObject;
    }
}
