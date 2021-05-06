// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.protobuf.ProtobufParser;
import org.openstreetmap.josm.data.protobuf.ProtobufRecord;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Logging;

/**
 * A Mapbox Vector Tile Layer
 * @author Taylor Smock
 * @since xxx
 */
public final class Layer implements Destroyable {
    private static final class ValueFields<T> {
        static final ValueFields<String> STRING = new ValueFields<>(1, ProtobufRecord::asString);
        static final ValueFields<Float> FLOAT = new ValueFields<>(2, ProtobufRecord::asFloat);
        static final ValueFields<Double> DOUBLE = new ValueFields<>(3, ProtobufRecord::asDouble);
        static final ValueFields<Number> INT64 = new ValueFields<>(4, ProtobufRecord::asUnsignedVarInt);
        // This may have issues if there are actual uint_values (i.e., more than {@link Long#MAX_VALUE})
        static final ValueFields<Number> UINT64 = new ValueFields<>(5, ProtobufRecord::asUnsignedVarInt);
        static final ValueFields<Number> SINT64 = new ValueFields<>(6, ProtobufRecord::asSignedVarInt);
        static final ValueFields<Boolean> BOOL = new ValueFields<>(7, r -> r.asUnsignedVarInt().longValue() != 0);

        /**
         * A collection of methods to map a record to a type
         */
        public static final Collection<ValueFields<?>> MAPPERS =
          Collections.unmodifiableList(Arrays.asList(STRING, FLOAT, DOUBLE, INT64, UINT64, SINT64, BOOL));

        private final byte field;
        private final Function<ProtobufRecord, T> conversion;
        private ValueFields(int field, Function<ProtobufRecord, T> conversion) {
            this.field = (byte) field;
            this.conversion = conversion;
        }

        /**
         * Get the field identifier for the value
         * @return The identifier
         */
        public byte getField() {
            return this.field;
        }

        /**
         * Convert a protobuf record to a value
         * @param protobufRecord The record to convert
         * @return the converted value
         */
        public T convertValue(ProtobufRecord protobufRecord) {
            return this.conversion.apply(protobufRecord);
        }
    }

    /** The field value for a layer (in {@link ProtobufRecord#getField}) */
    public static final byte LAYER_FIELD = 3;
    private static final byte VERSION_FIELD = 15;
    private static final byte NAME_FIELD = 1;
    private static final byte FEATURE_FIELD = 2;
    private static final byte KEY_FIELD = 3;
    private static final byte VALUE_FIELD = 4;
    private static final byte EXTENT_FIELD = 5;
    /** The default extent for a vector tile */
    static final int DEFAULT_EXTENT = 4096;
    private static final byte DEFAULT_VERSION = 1;
    /** This is <i>technically</i> an integer, but there are currently only two major versions (1, 2). Required. */
    private final byte version;
    /** A unique name for the layer. This <i>must</i> be unique on a per-tile basis. Required. */
    private final String name;

    /** The extent of the tile, typically 4096. Required. */
    private final int extent;

    /** A list of unique keys. Order is important. Optional. */
    private final List<String> keyList = new ArrayList<>();
    /** A list of unique values. Order is important. Optional. */
    private final List<Object> valueList = new ArrayList<>();
    /** The actual features of this layer in this tile */
    private final List<Feature> featureCollection;

    /**
     * Create a layer from a collection of records
     * @param records The records to convert to a layer
     * @throws IOException - if an IO error occurs
     */
    public Layer(Collection<ProtobufRecord> records) throws IOException {
        // Do the unique required fields first
        Map<Integer, List<ProtobufRecord>> sorted = records.stream().collect(Collectors.groupingBy(ProtobufRecord::getField));
        this.version = sorted.getOrDefault((int) VERSION_FIELD, Collections.emptyList()).parallelStream()
          .map(ProtobufRecord::asUnsignedVarInt).map(Number::byteValue).findFirst().orElse(DEFAULT_VERSION);
        // Per spec, we cannot continue past this until we have checked the version number
        if (this.version != 1 && this.version != 2) {
            throw new IllegalArgumentException(tr("We do not understand version {0} of the vector tile specification", this.version));
        }
        this.name = sorted.getOrDefault((int) NAME_FIELD, Collections.emptyList()).parallelStream().map(ProtobufRecord::asString).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(tr("Vector tile layers must have a layer name")));
        this.extent = sorted.getOrDefault((int) EXTENT_FIELD, Collections.emptyList()).parallelStream().map(ProtobufRecord::asUnsignedVarInt)
                .map(Number::intValue).findAny().orElse(DEFAULT_EXTENT);

        sorted.getOrDefault((int) KEY_FIELD, Collections.emptyList()).parallelStream().map(ProtobufRecord::asString)
                .forEachOrdered(this.keyList::add);
        sorted.getOrDefault((int) VALUE_FIELD, Collections.emptyList()).parallelStream().map(ProtobufRecord::getBytes)
                .map(ProtobufParser::new).map(parser1 -> {
                    try {
                        return new ProtobufRecord(parser1);
                    } catch (IOException e) {
                        Logging.error(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(value -> ValueFields.MAPPERS.parallelStream()
                        .filter(v -> v.getField() == value.getField())
                        .map(v -> v.convertValue(value)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(tr("Unknown field in vector tile layer value ({0})", value.getField()))))
                .forEachOrdered(this.valueList::add);
        Collection<IOException> exceptions = new HashSet<>(0);
        this.featureCollection = sorted.getOrDefault((int) FEATURE_FIELD, Collections.emptyList()).parallelStream().map(feature -> {
            try {
                return new Feature(this, feature);
            } catch (IOException e) {
                exceptions.add(e);
            }
            return null;
        }).collect(Collectors.toList());
        if (!exceptions.isEmpty()) {
            throw exceptions.iterator().next();
        }
        // Cleanup bytes (for memory)
        for (ProtobufRecord record : records) {
            record.close();
        }
    }

    /**
     * Get all the records from a array of bytes
     * @param bytes The byte information
     * @return All the protobuf records
     * @throws IOException If there was an error reading the bytes (unlikely)
     */
    private static Collection<ProtobufRecord> getAllRecords(byte[] bytes) throws IOException {
        try (ProtobufParser parser = new ProtobufParser(bytes)) {
            return parser.allRecords();
        }
    }

    /**
     * Create a new layer
     * @param bytes The bytes that the layer comes from
     * @throws IOException - if an IO error occurs
     */
    public Layer(byte[] bytes) throws IOException {
        this(getAllRecords(bytes));
    }

    /**
     * Get the extent of the tile
     * @return The layer extent
     */
    public int getExtent() {
        return this.extent;
    }

    /**
     * Get the feature on this layer
     * @return the features
     */
    public Collection<Feature> getFeatures() {
        return Collections.unmodifiableCollection(this.featureCollection);
    }

    /**
     * Get the geometry for this layer
     * @return The geometry
     */
    public Collection<Geometry> getGeometry() {
        return getFeatures().stream().map(Feature::getGeometryObject).collect(Collectors.toList());
    }

    /**
     * Get a specified key
     * @param index The index in the key list
     * @return The actual key
     */
    public String getKey(int index) {
        return this.keyList.get(index);
    }

    /**
     * Get the name of the layer
     * @return The layer name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get a specified value
     * @param index The index in the value list
     * @return The actual value. This can be a {@link String}, {@link Boolean}, {@link Integer}, or {@link Float} value.
     */
    public Object getValue(int index) {
        return this.valueList.get(index);
    }

    /**
     * Get the Mapbox Vector Tile version specification for this layer
     * @return The version of the Mapbox Vector Tile specification
     */
    public byte getVersion() {
        return this.version;
    }

    @Override
    public void destroy() {
        this.featureCollection.clear();
        this.keyList.clear();
        this.valueList.clear();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Layer) {
            Layer o = (Layer) other;
            return this.extent == o.extent
              && this.version == o.version
              && Objects.equals(this.name, o.name)
              && Objects.equals(this.featureCollection, o.featureCollection)
              && Objects.equals(this.keyList, o.keyList)
              && Objects.equals(this.valueList, o.valueList);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.version, this.extent, this.featureCollection, this.keyList, this.valueList);
    }
}
