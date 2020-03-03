// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This is an utility class that allows you to generate OSM test data.
 * @author Michael Zangl
 */
public final class OsmDataGenerator {
    private static final int DEFAULT_KEY_VALUE_RATIO = 3;
    private static final int DEFAULT_NODE_COUNT = 1000;
    private static final String DATA_DIR = "nodist/data" + File.separator + "osmfiles";

    private OsmDataGenerator() {
        // private constructor for utility classes
    }

    /**
     * This is a list of randomly generated strings.
     * <p>
     * It provides methods to get them both interned and uninterned.
     *
     * @author Michael Zangl
     */
    public static final class RandomStringList {
        private final Random random;
        private final String[] strings;
        private final String[] interned;

        /**
         * Creates a new, random List of Strings.
         * @param seed The seed to use.
         * @param size The size of the list.
         */
        public RandomStringList(int seed, int size) {
            random = new SecureRandom();
            random.setSeed(seed);
            strings = new String[size];
            interned = new String[size];
            for (int i = 0; i < size; i++) {
                strings[i] = randomString();
                interned[i] = strings[i].intern();
            }
        }

        protected String randomString() {
            return RandomStringUtils.random(12, 0, 0, true, true, null, random);
        }

        /**
         * Gets a String that was not interned.
         * @return The String.
         */
        public String get() {
            int n = random.nextInt(strings.length);
            return strings[n];
        }

        /**
         * Gets a String that was interned.
         * @return The String.
         */
        public String getInterned() {
            int n = random.nextInt(interned.length);
            return interned[n];
        }
    }

    /**
     * A generator that generates test data by filling a data set.
     * @author Michael Zangl
     */
    public abstract static class DataGenerator {
        private String datasetName;
        protected final Random random;
        private DataSet ds;

        /**
         * Create a new generator.
         * @param datasetName The name for the generator. Only used for human readability.
         */
        protected DataGenerator(String datasetName) {
            this.datasetName = datasetName;
            this.random = new SecureRandom();
        }

        /**
         * Generates the data set. If this method is called twice, the same dataset is returned.
         * @return The generated data set.
         */
        public DataSet generateDataSet() {
            ensureInitialized();
            return ds;
        }

        protected void ensureInitialized() {
            if (ds == null) {
                ds = new DataSet();
                fillData(ds);
            }
        }

        protected abstract void fillData(DataSet ds);

        /**
         * Create a random node and add it to the dataset.
         * @param ds the data set
         * @return a random node
         */
        protected Node createRandomNode(DataSet ds) {
            Node node = new Node();
            node.setEastNorth(new EastNorth(random.nextDouble(), random.nextDouble()));
            ds.addPrimitive(node);
            return node;
        }

        /**
         * Gets a file path where this data could be stored.
         * @return A file path.
         */
        public File getFile() {
            return new File(DATA_DIR, datasetName + ".osm");
        }

        /**
         * Creates a new {@link OsmDataLayer} that uses the underlying dataset of this generator.
         * @return A new data layer.
         */
        public OsmDataLayer createDataLayer() {
            return new OsmDataLayer(generateDataSet(), datasetName, getFile());
        }

        @Override
        public String toString() {
            return "DataGenerator [datasetName=" + datasetName + ']';
        }
    }

    /**
     * A data generator that generates a bunch of random nodes.
     * @author Michael Zangl
     */
    public static class NodeDataGenerator extends DataGenerator {
        protected final ArrayList<Node> nodes = new ArrayList<>();
        private final int nodeCount;

        protected NodeDataGenerator(String datasetName, int nodeCount) {
            super(datasetName);
            this.nodeCount = nodeCount;
        }

        @Override
        public void fillData(DataSet ds) {
            for (int i = 0; i < nodeCount; i++) {
                nodes.add(createRandomNode(ds));
            }
        }

        /**
         * Gets a random node of this dataset.
         * @return A random node.
         */
        public Node randomNode() {
            ensureInitialized();
            return nodes.get(random.nextInt(nodes.size()));
        }
    }

    /**
     * A data generator that generates a bunch of random nodes and fills them with keys/values.
     * @author Michael Zangl
     */
    public static final class KeyValueDataGenerator extends NodeDataGenerator {

        private static final int VALUE_COUNT = 200;
        private static final int KEY_COUNT = 150;
        private final double tagNodeRation;
        private RandomStringList keys;
        private RandomStringList values;

        private KeyValueDataGenerator(String datasetName, int nodeCount, double tagNodeRation) {
            super(datasetName, nodeCount);
            this.tagNodeRation = tagNodeRation;
        }

        @Override
        public void fillData(DataSet ds) {
            super.fillData(ds);
            keys = new RandomStringList(random.nextInt(), KEY_COUNT);
            values = new RandomStringList(random.nextInt(), VALUE_COUNT);

            double tags = nodes.size() * tagNodeRation;
            for (int i = 0; i < tags; i++) {
                String key = keys.get();
                String value = values.get();
                nodes.get(random.nextInt(nodes.size())).put(key, value);
            }
        }

        /**
         * Gets the values that were used to fill the tags.
         * @return The list of strings.
         */
        public RandomStringList randomValues() {
            ensureInitialized();
            return values;
        }

        /**
         * Gets the list of keys that was used to fill the tags.
         * @return The list of strings.
         */
        public RandomStringList randomKeys() {
            ensureInitialized();
            return keys;
        }

        /**
         * Gets a random value that was used to fill the tags.
         * @return A random String probably used in as value somewhere. Not interned.
         */
        public String randomValue() {
            return randomValues().get();
        }

        /**
         * Gets a random key that was used to fill the tags.
         * @return A random String probably used in as key somewhere. Not interned.
         */
        public String randomKey() {
            return randomKeys().get();
        }
    }

    /**
     * Generate a generator that creates some nodes and adds random keys and values to it.
     * @return The generator
     */
    public static KeyValueDataGenerator getKeyValue() {
        return getKeyValue(DEFAULT_KEY_VALUE_RATIO);
    }

    /**
     * Generate a generator that creates some nodes and adds random keys and values to it.
     * @param tagNodeRation How many tags to add per node (on average).
     * @return The generator
     */
    public static KeyValueDataGenerator getKeyValue(double tagNodeRation) {
        return getKeyValue(DEFAULT_NODE_COUNT, tagNodeRation);
    }

    /**
     * Generate a generator that creates some nodes and adds random keys and values to it.
     * @param nodeCount The number of nodes the dataset should contain.
     * @param tagNodeRation How many tags to add per node (on average).
     * @return The generator
     */
    public static KeyValueDataGenerator getKeyValue(int nodeCount, double tagNodeRation) {
        return new KeyValueDataGenerator("key-value", nodeCount, tagNodeRation);
    }

    /**
     * Create a generator that generates a bunch of nodes.
     * @return The generator
     */
    public static DataGenerator getNodes() {
        return new NodeDataGenerator("nodes", DEFAULT_NODE_COUNT);
    }
}
