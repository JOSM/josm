// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage.viewers.projections;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * A class that holds a registry of viewers for image projections
 * @since 18246
 */
public final class ImageProjectionRegistry {
    private static final EnumMap<Projections, Class<? extends IImageViewer>> DEFAULT_VIEWERS = new EnumMap<>(Projections.class);

    // Register the default viewers
    static {
        try {
            registerViewer(Perspective.class);
            registerViewer(Equirectangular.class);
        } catch (ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private ImageProjectionRegistry() {
        // Prevent instantiations
    }

    /**
     * Register a new viewer
     * @param clazz The class to register. The class <i>must</i> have a no args constructor
     * @return {@code true} if something changed
     * @throws ReflectiveOperationException if there is no no-args constructor, or it is not visible to us.
     */
    public static boolean registerViewer(Class<? extends IImageViewer> clazz) throws ReflectiveOperationException {
        Objects.requireNonNull(clazz, "null classes are hard to instantiate");
        final IImageViewer object = clazz.getConstructor().newInstance();
        boolean changed = false;
        for (Projections projections : object.getSupportedProjections()) {
            changed = clazz.equals(DEFAULT_VIEWERS.put(projections, clazz)) || changed;
        }
        return changed;
    }

    /**
     * Remove a viewer
     * @param clazz The class to remove.
     * @return {@code true} if something changed
     */
    public static boolean removeViewer(Class<? extends IImageViewer> clazz) {
        boolean changed = false;
        for (Projections projections : DEFAULT_VIEWERS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(clazz)).map(Map.Entry::getKey)
                .collect(Collectors.toList())) {
            changed = DEFAULT_VIEWERS.remove(projections, clazz) || changed;
        }
        return changed;
    }

    /**
     * Get the viewer for a specific projection type
     * @param projection The projection to view
     * @return The class to use
     */
    public static Class<? extends IImageViewer> getViewer(Projections projection) {
        return DEFAULT_VIEWERS.getOrDefault(projection, DEFAULT_VIEWERS.getOrDefault(Projections.UNKNOWN, Perspective.class));
    }
}
