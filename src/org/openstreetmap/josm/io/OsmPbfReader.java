// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.CountingInputStream;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.data.osm.pbf.Blob;
import org.openstreetmap.josm.data.osm.pbf.BlobHeader;
import org.openstreetmap.josm.data.osm.pbf.HeaderBlock;
import org.openstreetmap.josm.data.osm.pbf.Info;
import org.openstreetmap.josm.data.protobuf.ProtobufPacked;
import org.openstreetmap.josm.data.protobuf.ProtobufParser;
import org.openstreetmap.josm.data.protobuf.ProtobufRecord;
import org.openstreetmap.josm.data.protobuf.WireType;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;

/**
 * Read OSM data from an OSM PBF file
 * @since 18695
 */
public final class OsmPbfReader extends AbstractReader {
    private static final long[] EMPTY_LONG = new long[0];
    /**
     * Nano degrees
     */
    private static final double NANO_DEGREES = 1e-9;
    /**
     * The maximum BlobHeader size. BlobHeaders should (but not must) be less than half this
     */
    private static final int MAX_BLOBHEADER_SIZE = 64 * 1024;
    /**
     * The maximum Blob size. Blobs should (but not must) be less than half this
     */
    private static final int MAX_BLOB_SIZE = 32 * 1024 * 1024;

    private OsmPbfReader() {
        // Hide constructor
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source          the source input stream. Must not be null.
     * @param progressMonitor the progress monitor. If null, {@link NullProgressMonitor#INSTANCE} is assumed
     * @return the dataset with the parsed data
     * @throws IllegalDataException     if an error was found while parsing the data from the source
     * @throws IllegalArgumentException if source is null
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new OsmPbfReader().doParseDataSet(source, progressMonitor);
    }

    @Override
    protected DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return doParseDataSet(source, progressMonitor, this::parse);
    }

    private void parse(InputStream source) throws IllegalDataException, IOException {
        final CountingInputStream inputStream;
        if (source.markSupported()) {
            inputStream = new CountingInputStream(source);
        } else {
            inputStream = new CountingInputStream(new BufferedInputStream(source));
        }
        try (ProtobufParser parser = new ProtobufParser(inputStream)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            HeaderBlock headerBlock = null;
            BlobHeader blobHeader = null;
            while (parser.hasNext() && !this.cancel) {
                if (blobHeader == null) {
                    blobHeader = parseBlobHeader(inputStream, baos, parser);
                } else if ("OSMHeader".equals(blobHeader.type())) {
                    if (headerBlock != null) {
                        throw new IllegalDataException("Too many header blocks in protobuf");
                    }
                    // OSM PBF is fun -- it has *nested* pbf data
                    Blob blob = parseBlob(blobHeader, inputStream, parser, baos);
                    headerBlock = parseHeaderBlock(blob, baos);
                    checkRequiredFeatures(headerBlock);
                    blobHeader = null;
                } else if ("OSMData".equals(blobHeader.type())) {
                    if (headerBlock == null) {
                        throw new IllegalStateException("A header block must occur before the first data block");
                    }
                    Blob blob = parseBlob(blobHeader, inputStream, parser, baos);
                    parseDataBlock(baos, headerBlock, blob);
                    blobHeader = null;
                } // Other software *may* extend the FileBlocks (from just "OSMHeader" and "OSMData"), so don't throw an error.
            }
        }
    }

    /**
     * Parse a blob header
     *
     * @param cis    A counting stream to ensure we don't read too much data
     * @param baos   A reusable stream
     * @param parser The parser to read from
     * @return The BlobHeader message
     * @throws IOException          if one of the streams has an issue
     * @throws IllegalDataException If the OSM PBF is (probably) corrupted
     */
    @Nonnull
    private static BlobHeader parseBlobHeader(CountingInputStream cis, ByteArrayOutputStream baos, ProtobufParser parser)
            throws IOException, IllegalDataException {
        String type = null;
        byte[] indexData = null;
        int datasize = Integer.MIN_VALUE;
        int length = 0;
        long start = cis.getBytesRead();
        while (parser.hasNext() && (length == 0 || cis.getBytesRead() - start < length)) {
            final ProtobufRecord current = new ProtobufRecord(baos, parser);
            switch (current.getField()) {
                case 1:
                    type = current.asString();
                    break;
                case 2:
                    indexData = current.getBytes();
                    break;
                case 3:
                    datasize = current.asUnsignedVarInt().intValue();
                    break;
                default:
                    start = cis.getBytesRead();
                    length += current.asUnsignedVarInt().intValue();
                    if (length > MAX_BLOBHEADER_SIZE) { // There is a hard limit of 64 KiB for the BlobHeader. It *should* be less than 32 KiB.
                        throw new IllegalDataException("OSM PBF BlobHeader is too large. PBF is probably corrupted. (" +
                                Utils.getSizeString(MAX_BLOBHEADER_SIZE, Locale.ENGLISH) + " < " + Utils.getSizeString(length, Locale.ENGLISH));
                    }
            }
        }
        if (type == null || Integer.MIN_VALUE == datasize) {
            throw new IllegalDataException("OSM PBF BlobHeader could not be read. PBF is probably corrupted.");
        } else if (datasize > MAX_BLOB_SIZE) { // There is a hard limit of 32 MiB for the blob size. It *should* be less than 16 MiB.
            throw new IllegalDataException("OSM PBF Blob size is too large. PBF is probably corrupted. ("
                    + Utils.getSizeString(MAX_BLOB_SIZE, Locale.ENGLISH) + " < " + Utils.getSizeString(datasize, Locale.ENGLISH));
        }
        return new BlobHeader(type, indexData, datasize);
    }

    /**
     * Parse a blob from the PBF file
     *
     * @param header The header with the blob information (most critically, the length of the blob)
     * @param cis    Used to ensure we don't read too much data
     * @param parser The parser to read records from
     * @param baos   The reusable output stream
     * @return The blob to use elsewhere
     * @throws IOException If one of the streams has an issue
     */
    @Nonnull
    private static Blob parseBlob(BlobHeader header, CountingInputStream cis, ProtobufParser parser, ByteArrayOutputStream baos)
            throws IOException {
        long start = cis.getBytesRead();
        int size = Integer.MIN_VALUE;
        Blob.CompressionType type = null;
        ProtobufRecord current = null;
        while (parser.hasNext() && cis.getBytesRead() - start < header.dataSize()) {
            current = new ProtobufRecord(baos, parser);
            switch (current.getField()) {
                case 1:
                    type = Blob.CompressionType.raw;
                    break;
                case 2:
                    size = current.asUnsignedVarInt().intValue();
                    break;
                case 3:
                    type = Blob.CompressionType.zlib;
                    break;
                case 4:
                    type = Blob.CompressionType.lzma;
                    break;
                case 5:
                    type = Blob.CompressionType.bzip2;
                    break;
                case 6:
                    type = Blob.CompressionType.lz4;
                    break;
                case 7:
                    type = Blob.CompressionType.zstd;
                    break;
                default:
                    throw new IllegalStateException("Unknown compression type: " + current.getField());
            }
        }
        if (type == null) {
            throw new IllegalStateException("Compression type not found, pbf may be malformed");
        }
        return new Blob(size, type, current.getBytes());
    }

    /**
     * Parse a header block. This assumes that the parser has hit a string with the text "OSMHeader".
     *
     * @param blob The blob with the header block data
     * @param baos The reusable output stream to use
     * @return The parsed HeaderBlock
     * @throws IOException if one of the {@link InputStream}s has a problem
     */
    @Nonnull
    private static HeaderBlock parseHeaderBlock(Blob blob, ByteArrayOutputStream baos) throws IOException {
        try (InputStream blobInput = blob.inputStream();
             ProtobufParser parser = new ProtobufParser(blobInput)) {
            BBox bbox = null;
            List<String> required = new ArrayList<>();
            List<String> optional = new ArrayList<>();
            String program = null;
            String source = null;
            Long osmosisReplicationTimestamp = null;
            Long osmosisReplicationSequenceNumber = null;
            String osmosisReplicationBaseUrl = null;
            while (parser.hasNext()) {
                final ProtobufRecord current = new ProtobufRecord(baos, parser);
                switch (current.getField()) {
                    case 1: // bbox
                        bbox = parseBBox(baos, current);
                        break;
                    case 4: // repeated required features
                        required.add(current.asString());
                        break;
                    case 5: // repeated optional features
                        optional.add(current.asString());
                        break;
                    case 16: // writing program
                        program = current.asString();
                        break;
                    case 17: // source
                        source = current.asString();
                        break;
                    case 32: // osmosis replication timestamp
                        osmosisReplicationTimestamp = current.asSignedVarInt().longValue();
                        break;
                    case 33: // osmosis replication sequence number
                        osmosisReplicationSequenceNumber = current.asSignedVarInt().longValue();
                        break;
                    case 34: // osmosis replication base url
                        osmosisReplicationBaseUrl = current.asString();
                        break;
                    default: // fall through -- unknown header block field
                }
            }
            return new HeaderBlock(bbox, required.toArray(new String[0]), optional.toArray(new String[0]), program,
                    source, osmosisReplicationTimestamp, osmosisReplicationSequenceNumber, osmosisReplicationBaseUrl);
        }
    }

    /**
     * Ensure that we support all the required features in the PBF
     *
     * @param headerBlock The HeaderBlock to check
     * @throws IllegalDataException If there exists at least one feature that we do not support
     */
    private static void checkRequiredFeatures(HeaderBlock headerBlock) throws IllegalDataException {
        Set<String> supportedFeatures = new HashSet<>(Arrays.asList("OsmSchema-V0.6", "DenseNodes", "HistoricalInformation"));
        for (String requiredFeature : headerBlock.requiredFeatures()) {
            if (!supportedFeatures.contains(requiredFeature)) {
                throw new IllegalDataException("PBF Parser: Unknown required feature " + requiredFeature);
            }
        }
    }

    /**
     * Parse a data blob (should be "OSMData")
     *
     * @param baos        The reusable stream
     * @param headerBlock The header block with data source information
     * @param blob        The blob to read OSM data from
     * @throws IOException          if we don't support the compression type
     * @throws IllegalDataException If an invalid OSM primitive was read
     */
    private void parseDataBlock(ByteArrayOutputStream baos, HeaderBlock headerBlock, Blob blob) throws IOException, IllegalDataException {
        String[] stringTable = null; // field 1, note that stringTable[0] is a delimiter, so it is always blank and unused
        // field 2 -- we cannot parse these live just in case the following fields come later
        List<ProtobufRecord> primitiveGroups = new ArrayList<>();
        int granularity = 100; // field 17
        long latOffset = 0; // field 19
        long lonOffset = 0; // field 20
        int dateGranularity = 1000; // field 18, default is milliseconds since the 1970 epoch
        try (InputStream inputStream = blob.inputStream();
             ProtobufParser parser = new ProtobufParser(inputStream)) {
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1:
                        stringTable = parseStringTable(baos, protobufRecord.getBytes());
                        break;
                    case 2:
                        primitiveGroups.add(protobufRecord);
                        break;
                    case 17:
                        granularity = protobufRecord.asUnsignedVarInt().intValue();
                        break;
                    case 18:
                        dateGranularity = protobufRecord.asUnsignedVarInt().intValue();
                        break;
                    case 19:
                        latOffset = protobufRecord.asUnsignedVarInt().longValue();
                        break;
                    case 20:
                        lonOffset = protobufRecord.asUnsignedVarInt().longValue();
                        break;
                    default: // Pass, since someone might have extended the format
                }
            }
        }
        final PrimitiveBlockRecord primitiveBlockRecord = new PrimitiveBlockRecord(stringTable, granularity, latOffset, lonOffset,
                dateGranularity);
        final DataSet ds = getDataSet();
        if (!primitiveGroups.isEmpty() && headerBlock.bbox() != null) {
            try {
                ds.beginUpdate();
                ds.addDataSource(new DataSource(new Bounds((LatLon) headerBlock.bbox().getMin(), (LatLon) headerBlock.bbox().getMax()),
                        headerBlock.source()));
            } finally {
                ds.endUpdate();
            }
        }
        for (ProtobufRecord primitiveGroup : primitiveGroups) {
            try {
                ds.beginUpdate();
                parsePrimitiveGroup(baos, primitiveGroup.getBytes(), primitiveBlockRecord);
            } finally {
                ds.endUpdate();
            }
        }
    }

    /**
     * This parses a bbox from a record (HeaderBBox message)
     *
     * @param baos    The reusable {@link ByteArrayOutputStream} to avoid unnecessary allocations
     * @param current The current record
     * @return The <i>immutable</i> bbox, or {@code null}
     * @throws IOException If something happens with the {@link InputStream}s (probably won't happen)
     */
    @Nullable
    private static BBox parseBBox(ByteArrayOutputStream baos, ProtobufRecord current) throws IOException {
        try (ByteArrayInputStream bboxInputStream = new ByteArrayInputStream(current.getBytes());
             ProtobufParser bboxParser = new ProtobufParser(bboxInputStream)) {
            double left = Double.NaN;
            double right = Double.NaN;
            double top = Double.NaN;
            double bottom = Double.NaN;
            while (bboxParser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, bboxParser);
                if (protobufRecord.getType() == WireType.VARINT) {
                    double value = protobufRecord.asSignedVarInt().longValue() * NANO_DEGREES;
                    switch (protobufRecord.getField()) {
                        case 1:
                            left = value;
                            break;
                        case 2:
                            right = value;
                            break;
                        case 3:
                            top = value;
                            break;
                        case 4:
                            bottom = value;
                            break;
                        default: // Fall through -- someone might have extended the format
                    }
                }
            }
            if (!Double.isNaN(left) && !Double.isNaN(top) && !Double.isNaN(right) && !Double.isNaN(bottom)) {
                return new BBox(left, top, right, bottom).toImmutable();
            }
        }
        return null;
    }

    /**
     * Parse the string table
     *
     * @param baos  The reusable stream
     * @param bytes The message bytes
     * @return The parsed table (reminder: index 0 is empty, note that all strings are already interned by {@link String#intern()})
     * @throws IOException if something happened while reading a {@link ByteArrayInputStream}
     */
    @Nonnull
    private static String[] parseStringTable(ByteArrayOutputStream baos, byte[] bytes) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(is)) {
            List<String> list = new ArrayList<>();
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                if (protobufRecord.getField() == 1) {
                    list.add(protobufRecord.asString().intern()); // field is technically repeated bytes
                }
            }
            return list.toArray(new String[0]);
        }
    }

    /**
     * Parse a PrimitiveGroup. Note: this parsing implementation doesn't check and make certain that all primitives in the group are the same
     * type.
     *
     * @param baos                 The reusable stream
     * @param bytes                The bytes to decode
     * @param primitiveBlockRecord The record to use for creating the primitives
     * @throws IllegalDataException if one of the primitive records was invalid
     * @throws IOException          if something happened while reading a {@link ByteArrayInputStream}
     */
    private void parsePrimitiveGroup(ByteArrayOutputStream baos, byte[] bytes, PrimitiveBlockRecord primitiveBlockRecord)
            throws IllegalDataException, IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(bais)) {
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1: // Nodes, repeated
                        parseNode(baos, protobufRecord.getBytes(), primitiveBlockRecord);
                        break;
                    case 2: // Dense nodes, not repeated
                        parseDenseNodes(baos, protobufRecord.getBytes(), primitiveBlockRecord);
                        break;
                    case 3: // Ways, repeated
                        parseWay(baos, protobufRecord.getBytes(), primitiveBlockRecord);
                        break;
                    case 4: // relations, repeated
                        parseRelation(baos, protobufRecord.getBytes(), primitiveBlockRecord);
                        break;
                    case 5: // Changesets, repeated
                        // Skip -- we don't have a good way to store changeset information in JOSM
                    default: // OSM PBF could be extended
                }
            }
        }
    }

    /**
     * Parse a singular node
     *
     * @param baos                 The reusable stream
     * @param bytes                The bytes to decode
     * @param primitiveBlockRecord The record to use (mostly for tags and lat/lon calculations)
     * @throws IllegalDataException if the PBF did not provide all the data necessary for node creation
     * @throws IOException          if something happened while reading a {@link ByteArrayInputStream}
     */
    private void parseNode(ByteArrayOutputStream baos, byte[] bytes, PrimitiveBlockRecord primitiveBlockRecord)
            throws IllegalDataException, IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(bais)) {
            long id = Long.MIN_VALUE;
            List<String> keys = new ArrayList<>();
            List<String> values = new ArrayList<>();
            Info info = null;
            long lat = Long.MIN_VALUE;
            long lon = Long.MIN_VALUE;
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1:
                        id = protobufRecord.asSignedVarInt().intValue();
                        break;
                    case 2:
                        for (long number : new ProtobufPacked(protobufRecord.getBytes()).getArray()) {
                            keys.add(primitiveBlockRecord.stringTable[(int) number]);
                        }
                        break;
                    case 3:
                        for (long number : new ProtobufPacked(protobufRecord.getBytes()).getArray()) {
                            values.add(primitiveBlockRecord.stringTable[(int) number]);
                        }
                        break;
                    case 4:
                        info = parseInfo(baos, protobufRecord.getBytes());
                        break;
                    case 8:
                        lat = protobufRecord.asSignedVarInt().longValue();
                        break;
                    case 9:
                        lon = protobufRecord.asSignedVarInt().longValue();
                        break;
                    default: // Fall through -- PBF could be extended (unlikely)
                }
            }
            if (id == Long.MIN_VALUE || lat == Long.MIN_VALUE || lon == Long.MIN_VALUE) {
                throw new IllegalDataException("OSM PBF did not provide all the required node information");
            }
            NodeData node = new NodeData(id);
            node.setCoor(calculateLatLon(primitiveBlockRecord, lat, lon));
            addTags(node, keys, values);
            if (info != null) {
                setOsmPrimitiveData(primitiveBlockRecord, node, info);
            }
            buildPrimitive(node);
        }
    }

    /**
     * Parse dense nodes from a record
     *
     * @param baos                 The reusable output stream
     * @param bytes                The bytes for the dense node
     * @param primitiveBlockRecord Used for data that is common between several different objects.
     * @throws IllegalDataException if the nodes could not be parsed, or one of the nodes would be malformed
     * @throws IOException          if something happened while reading a {@link ByteArrayInputStream}
     */
    private void parseDenseNodes(ByteArrayOutputStream baos, byte[] bytes, PrimitiveBlockRecord primitiveBlockRecord)
            throws IllegalDataException, IOException {
        long[] ids = EMPTY_LONG;
        long[] lats = EMPTY_LONG;
        long[] lons = EMPTY_LONG;
        long[] keyVals = EMPTY_LONG; // technically can be int
        Info[] denseInfo = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(bais)) {
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1: // packed node ids, DELTA encoded
                        long[] tids = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        ids = joinArrays(ids, tids);
                        break;
                    case 5: // DenseInfo
                        denseInfo = parseDenseInfo(baos, protobufRecord.getBytes()); // not repeated or packed
                        break;
                    case 8: // packed lat, DELTA encoded
                        long[] tlats = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        lats = joinArrays(lats, tlats);
                        break;
                    case 9: // packed lon, DELTA encoded
                        long[] tlons = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        lons = joinArrays(lons, tlons);
                        break;
                    case 10: // key_val mappings, packed. '0' used as separator between nodes
                        long[] tkeyVal = new ProtobufPacked(protobufRecord.getBytes()).getArray();
                        keyVals = joinArrays(keyVals, tkeyVal);
                        break;
                    default: // Someone might have extended the PBF format
                }
            }
        }
        int keyValIndex = 0; // This index must not reset between nodes, and must always increment
        if (ids.length == lats.length && lats.length == lons.length && (denseInfo == null || denseInfo.length == lons.length)) {
            long id = 0;
            long lat = 0;
            long lon = 0;
            for (int i = 0; i < ids.length; i++) {
                final NodeData node;
                if (denseInfo != null) {
                    Info info = denseInfo[i];
                    id += ids[i];
                    node = new NodeData(id);
                    setOsmPrimitiveData(primitiveBlockRecord, node, info);
                } else {
                    node = new NodeData(ids[i]);
                }
                lat += lats[i];
                lon += lons[i];
                // Not very efficient when Node doesn't store the LatLon. Hopefully not too much of an issue
                node.setCoor(calculateLatLon(primitiveBlockRecord, lat, lon));
                String key = null;
                while (keyValIndex < keyVals.length) {
                    int stringIndex = (int) keyVals[keyValIndex];
                    // StringTable[0] is always an empty string, and acts as a separator between the tags of different nodes here
                    if (stringIndex != 0) {
                        if (key == null) {
                            key = primitiveBlockRecord.stringTable[stringIndex];
                        } else {
                            node.put(key, primitiveBlockRecord.stringTable[stringIndex]);
                            key = null;
                        }
                        keyValIndex++;
                    } else {
                        keyValIndex++;
                        break;
                    }
                }
                // Just add the nodes as we make them -- avoid creating another list that expands every time we parse a node
                buildPrimitive(node);
            }
        } else {
            throw new IllegalDataException("OSM PBF has mismatched DenseNode lengths");
        }
    }

    /**
     * Parse a way from the PBF
     *
     * @param baos                 The reusable stream
     * @param bytes                The bytes for the way
     * @param primitiveBlockRecord Used for common information, like tags
     * @throws IllegalDataException if an invalid way could have been created
     * @throws IOException          if something happened while reading a {@link ByteArrayInputStream}
     */
    private void parseWay(ByteArrayOutputStream baos, byte[] bytes, PrimitiveBlockRecord primitiveBlockRecord)
            throws IllegalDataException, IOException {
        long id = Long.MIN_VALUE;
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        Info info = null;
        long[] refs = EMPTY_LONG; // DELTA encoded
        // We don't do live drawing, so we don't care about lats and lons (we essentially throw them away with the current parser)
        // This is for the optional feature "LocationsOnWays"
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(bais)) {
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1:
                        id = protobufRecord.asUnsignedVarInt().longValue();
                        break;
                    case 2:
                        for (long number : new ProtobufPacked(protobufRecord.getBytes()).getArray()) {
                            keys.add(primitiveBlockRecord.stringTable[(int) number]);
                        }
                        break;
                    case 3:
                        for (long number : new ProtobufPacked(protobufRecord.getBytes()).getArray()) {
                            values.add(primitiveBlockRecord.stringTable[(int) number]);
                        }
                        break;
                    case 4:
                        info = parseInfo(baos, protobufRecord.getBytes());
                        break;
                    case 8:
                        long[] tRefs = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        refs = joinArrays(refs, tRefs);
                        break;
                    // case 9 and 10 are for "LocationsOnWays" -- this is only usable if we can create the way geometry directly
                    // if this is ever supported, lats = joinArrays(lats, decodePackedSInt64(...))
                    default: // PBF could be expanded by other people
                }
            }
        }
        if (refs.length == 0 || id == Long.MIN_VALUE) {
            throw new IllegalDataException("A way with either no id or no nodes was found");
        }
        WayData wayData = new WayData(id);
        List<Long> nodeIds = new ArrayList<>(refs.length);
        long ref = 0;
        for (long tRef : refs) {
            ref += tRef;
            nodeIds.add(ref);
        }
        this.ways.put(wayData.getUniqueId(), nodeIds);
        addTags(wayData, keys, values);
        if (info != null) {
            setOsmPrimitiveData(primitiveBlockRecord, wayData, info);
        }
        buildPrimitive(wayData);
    }

    /**
     * Parse a relation from a PBF
     *
     * @param baos                 The reusable stream
     * @param bytes                The bytes to use
     * @param primitiveBlockRecord Mostly used for tags
     * @throws IllegalDataException if the PBF had a bad relation definition
     * @throws IOException          if something happened while reading a {@link ByteArrayInputStream}
     */
    private void parseRelation(ByteArrayOutputStream baos, byte[] bytes, PrimitiveBlockRecord primitiveBlockRecord)
            throws IllegalDataException, IOException {
        long id = Long.MIN_VALUE;
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        Info info = null;
        long[] rolesStringId = EMPTY_LONG; // Technically int
        long[] memids = EMPTY_LONG;
        long[] types = EMPTY_LONG; // Technically an enum
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(bais)) {
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1:
                        id = protobufRecord.asUnsignedVarInt().longValue();
                        break;
                    case 2:
                        for (long number : new ProtobufPacked(protobufRecord.getBytes()).getArray()) {
                            keys.add(primitiveBlockRecord.stringTable[(int) number]);
                        }
                        break;
                    case 3:
                        for (long number : new ProtobufPacked(protobufRecord.getBytes()).getArray()) {
                            values.add(primitiveBlockRecord.stringTable[(int) number]);
                        }
                        break;
                    case 4:
                        info = parseInfo(baos, protobufRecord.getBytes());
                        break;
                    case 8:
                        long[] tRoles = new ProtobufPacked(protobufRecord.getBytes()).getArray();
                        rolesStringId = joinArrays(rolesStringId, tRoles);
                        break;
                    case 9:
                        long[] tMemids = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        memids = joinArrays(memids, tMemids);
                        break;
                    case 10:
                        long[] tTypes = new ProtobufPacked(protobufRecord.getBytes()).getArray();
                        types = joinArrays(types, tTypes);
                        break;
                    default: // Fall through for PBF extensions
                }
            }
        }
        if (keys.size() != values.size() || rolesStringId.length != memids.length || memids.length != types.length || id == Long.MIN_VALUE) {
            throw new IllegalDataException("OSM PBF contains a bad relation definition");
        }
        RelationData data = new RelationData(id);
        if (info != null) {
            setOsmPrimitiveData(primitiveBlockRecord, data, info);
        }
        addTags(data, keys, values);
        OsmPrimitiveType[] valueTypes = OsmPrimitiveType.values();
        List<RelationMemberData> members = new ArrayList<>(rolesStringId.length);
        long memberId = 0;
        for (int i = 0; i < rolesStringId.length; i++) {
            String role = primitiveBlockRecord.stringTable[(int) rolesStringId[i]];
            memberId += memids[i];
            OsmPrimitiveType type = valueTypes[(int) types[i]];
            members.add(new RelationMemberData(role, type, memberId));
        }
        this.relations.put(data.getUniqueId(), members);
        buildPrimitive(data);
    }

    /**
     * Parse info for an object
     *
     * @param baos  The reusable stream to use
     * @param bytes The bytes to decode
     * @return The info for an object
     * @throws IOException if something happened while reading a {@link ByteArrayInputStream}
     */
    @Nonnull
    private static Info parseInfo(ByteArrayOutputStream baos, byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(bais)) {
            int version = -1;
            Long timestamp = null;
            Long changeset = null;
            Integer uid = null;
            Integer userSid = null;
            boolean visible = true;
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1:
                        version = protobufRecord.asUnsignedVarInt().intValue();
                        break;
                    case 2:
                        timestamp = protobufRecord.asUnsignedVarInt().longValue();
                        break;
                    case 3:
                        changeset = protobufRecord.asUnsignedVarInt().longValue();
                        break;
                    case 4:
                        uid = protobufRecord.asUnsignedVarInt().intValue();
                        break;
                    case 5:
                        userSid = protobufRecord.asUnsignedVarInt().intValue();
                        break;
                    case 6:
                        visible = protobufRecord.asUnsignedVarInt().byteValue() == 0;
                        break;
                    default: // Fall through, since the PBF format could be extended
                }
            }
            return new Info(version, timestamp, changeset, uid, userSid, visible);
        }
    }

    /**
     * Calculate the actual lat lon
     *
     * @param primitiveBlockRecord The record with offset and granularity data
     * @param lat                  The latitude from the PBF
     * @param lon                  The longitude from the PBF
     * @return The actual {@link LatLon}, accounting for PBF offset and granularity changes
     */
    @Nonnull
    private static LatLon calculateLatLon(PrimitiveBlockRecord primitiveBlockRecord, long lat, long lon) {
        return new LatLon(NANO_DEGREES * (primitiveBlockRecord.latOffset + (primitiveBlockRecord.granularity * lat)),
                NANO_DEGREES * (primitiveBlockRecord.lonOffset + (primitiveBlockRecord.granularity * lon)));
    }

    /**
     * Add a set of tags to a primitive
     *
     * @param primitive The primitive to add tags to
     * @param keys      The keys (must match the size of the values)
     * @param values    The values (must match the size of the keys)
     */
    private static void addTags(Tagged primitive, List<String> keys, List<String> values) {
        if (keys.isEmpty()) {
            return;
        }
        Map<String, String> tagMap = new HashMap<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            tagMap.put(keys.get(i), values.get(i));
        }
        primitive.putAll(tagMap);
    }

    /**
     * Set the primitive data for an object
     *
     * @param primitiveBlockRecord The record with data for the current primitive (currently uses {@link PrimitiveBlockRecord#stringTable} and
     *                             {@link PrimitiveBlockRecord#dateGranularity}).
     * @param primitive            The primitive to add the information to
     * @param info                 The specific info for the primitive
     */
    private static void setOsmPrimitiveData(PrimitiveBlockRecord primitiveBlockRecord, PrimitiveData primitive, Info info) {
        primitive.setVisible(info.isVisible());
        if (info.timestamp() != null) {
            primitive.setRawTimestamp(Math.toIntExact(info.timestamp() * primitiveBlockRecord.dateGranularity / 1000));
        }
        if (info.uid() != null && info.userSid() != null) {
            primitive.setUser(User.createOsmUser(info.uid(), primitiveBlockRecord.stringTable[info.userSid()]));
        } else if (info.uid() != null) {
            primitive.setUser(User.getById(info.uid()));
        }
        if (info.version() > 0) {
            primitive.setVersion(info.version());
        }
        if (info.changeset() != null) {
            primitive.setChangesetId(Math.toIntExact(info.changeset()));
        }
    }

    /**
     * Convert an array of numbers to an array of longs, decoded from uint (zig zag decoded)
     *
     * @param numbers The numbers to convert
     * @return The long array (the same array that was passed in)
     */
    @Nonnull
    private static long[] decodePackedSInt64(long[] numbers) {
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = ProtobufParser.decodeZigZag(numbers[i]);
        }
        return numbers;
    }

    /**
     * Join two different arrays
     *
     * @param array1 The first array
     * @param array2 The second array
     * @return The joined arrays -- may return one of the original arrays, if the other is empty
     */
    @Nonnull
    private static long[] joinArrays(long[] array1, long[] array2) {
        if (array1.length == 0) {
            return array2;
        }
        if (array2.length == 0) {
            return array1;
        }
        long[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    /**
     * Parse dense info
     *
     * @param baos  The reusable stream
     * @param bytes The bytes to decode
     * @return The dense info array
     * @throws IllegalDataException If the data has mismatched array lengths
     * @throws IOException          if something happened while reading a {@link ByteArrayInputStream}
     */
    @Nonnull
    private static Info[] parseDenseInfo(ByteArrayOutputStream baos, byte[] bytes) throws IllegalDataException, IOException {
        long[] version = EMPTY_LONG; // technically ints
        long[] timestamp = EMPTY_LONG;
        long[] changeset = EMPTY_LONG;
        long[] uid = EMPTY_LONG; // technically int
        long[] userSid = EMPTY_LONG; // technically int
        long[] visible = EMPTY_LONG; // optional, true if not set, technically booleans
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ProtobufParser parser = new ProtobufParser(bais)) {
            while (parser.hasNext()) {
                ProtobufRecord protobufRecord = new ProtobufRecord(baos, parser);
                switch (protobufRecord.getField()) {
                    case 1:
                        long[] tVersion = new ProtobufPacked(protobufRecord.getBytes()).getArray();
                        version = joinArrays(version, tVersion);
                        break;
                    case 2:
                        long[] tTimestamp = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        timestamp = joinArrays(timestamp, tTimestamp);
                        break;
                    case 3:
                        long[] tChangeset = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        changeset = joinArrays(changeset, tChangeset);
                        break;
                    case 4:
                        long[] tUid = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        uid = joinArrays(uid, tUid);
                        break;
                    case 5:
                        long[] tUserSid = decodePackedSInt64(new ProtobufPacked(protobufRecord.getBytes()).getArray());
                        userSid = joinArrays(userSid, tUserSid);
                        break;
                    case 6:
                        long[] tVisible = new ProtobufPacked(protobufRecord.getBytes()).getArray();
                        visible = joinArrays(visible, tVisible);
                        break;
                    default: // Fall through
                }
            }
        }
        if (version.length == timestamp.length && timestamp.length == changeset.length && changeset.length == uid.length &&
                uid.length == userSid.length && (visible == EMPTY_LONG || visible.length == userSid.length)) {
            Info[] infos = new Info[version.length];
            long lastTimestamp = 0; // delta encoded
            long lastChangeset = 0; // delta encoded
            long lastUid = 0; // delta encoded,
            long lastUserSid = 0; // delta encoded, string id for username
            for (int i = 0; i < version.length; i++) {
                lastTimestamp += timestamp[i];
                lastChangeset += changeset[i];
                lastUid += uid[i];
                lastUserSid += userSid[i];
                infos[i] = new Info((int) version[i], lastTimestamp, lastChangeset, (int) lastUid, (int) lastUserSid,
                        visible == EMPTY_LONG || visible[i] == 1);
            }
            return infos;
        }
        throw new IllegalDataException("OSM PBF has mismatched DenseInfo lengths");
    }

    /**
     * A record class for passing PrimitiveBlock information to the PrimitiveGroup parser
     */
    private static final class PrimitiveBlockRecord {
        private final String[] stringTable;
        private final int granularity;
        private final long latOffset;
        private final long lonOffset;
        private final int dateGranularity;

        /**
         * Create a new record
         *
         * @param stringTable     The string table (reminder: 0 index is empty, as it is used by DenseNode to separate node tags)
         * @param granularity     units of nanodegrees, used to store coordinates
         * @param latOffset       offset value between the output coordinates and the granularity grid in units of nanodegrees
         * @param lonOffset       offset value between the output coordinates and the granularity grid in units of nanodegrees
         * @param dateGranularity Granularity of dates, normally represented in units of milliseconds since the 1970 epoch
         */
        PrimitiveBlockRecord(String[] stringTable, int granularity, long latOffset, long lonOffset,
                             int dateGranularity) {
            this.stringTable = stringTable;
            this.granularity = granularity;
            this.latOffset = latOffset;
            this.lonOffset = lonOffset;
            this.dateGranularity = dateGranularity;
        }

    }
}
