// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.util.List;

import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Predicates;

/**
 * This class defines a position to insert a given layer in the list of layers.
 * @author Michael Zangl
 * @since 10008
 */
public abstract class LayerPositionStrategy {

    /**
     * always inserts at the front of the stack.
     */
    public static final LayerPositionStrategy IN_FRONT = new LayerPositionStrategy() {
        @Override
        public int getPosition(LayerManager manager) {
            return 0;
        }
    };

    /**
     * A GPX layer is added below the lowest data layer.
     */
    public static final LayerPositionStrategy AFTER_LAST_DATA_LAYER = afterLast(new Predicate<Layer>() {
        @Override
        public boolean evaluate(Layer object) {
            return object instanceof OsmDataLayer || object instanceof ValidatorLayer;
        }
    });

    /**
     * A normal layer is added after all validation layers.
     */
    public static final LayerPositionStrategy AFTER_LAST_VALIDATION_LAYER = afterLast(new Predicate<Layer>() {
        @Override
        public boolean evaluate(Layer object) {
            return object instanceof ValidatorLayer;
        }
    });

    /**
     * The default for background layers: They are added before the first background layer in the list.
     * If there is none, they are added at the end of the list.
     */
    public static final LayerPositionStrategy BEFORE_FIRST_BACKGROUND_LAYER = inFrontOfFirst(new Predicate<Layer>() {
        @Override
        public boolean evaluate(Layer object) {
            return object.isBackgroundLayer();
        }
    });

    /**
     * Gets a {@link LayerPositionStrategy} that inserts this layer in front of a given layer
     * @param other The layer before which to insert this layer
     * @return The strategy
     */
    public static LayerPositionStrategy inFrontOf(Layer other) {
        return inFrontOfFirst(Predicates.equalTo(other));
    }

    /**
     * Gets a {@link LayerPositionStrategy} that inserts the layer in front of the first layer that matches a condition.
     * @param what The condition to match.
     * @return The strategy.
     */
    public static LayerPositionStrategy inFrontOfFirst(final Predicate<Layer> what) {
        return new LayerPositionStrategy() {
            @Override
            public int getPosition(LayerManager manager) {
                List<Layer> layers = manager.getLayers();
                for (int i = 0; i < layers.size(); i++) {
                    if (what.evaluate(layers.get(i))) {
                        return i;
                    }
                }
                return layers.size();
            }
        };
    }

    /**
     * Creates a strategy that places the layer after the last layer of a given kind or at the beginning of the list if no such layer exists.
     * @param what what to search for
     * @return The strategy.
     */
    public static LayerPositionStrategy afterLast(final Predicate<Layer> what) {
        return new LayerPositionStrategy() {
            @Override
            public int getPosition(LayerManager manager) {
                List<Layer> layers = manager.getLayers();
                for (int i = layers.size() - 1; i >= 0; i--) {
                    if (what.evaluate(layers.get(i))) {
                        return i + 1;
                    }
                }
                return 0;
            }
        };
    }

    /**
     * Gets the position where the layer should be inserted
     * @param manager The layer manager to insert the layer in.
     * @return The position in the range 0...layers.size
     */
    public abstract int getPosition(LayerManager manager);
}
