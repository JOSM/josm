// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * <p>MapRendererFactory manages a list of map renderer classes and associated
 * meta data (display name, description).</p>
 *
 * <p>Plugins can implement and supply their own map renderers.</p>
 * <strong>Sample code in a plugin</strong>
 * <pre>
 * public class MyMapRenderer extends AbstractMapRenderer {
 *    // ....
 * }
 *
 * // to be called when the plugin is created
 * MapRendererFactory factory = MapRendererFactory.getInstance();
 * factory.register(MyMapRenderer.class, "My map renderer", "This is is a fast map renderer");
 * factory.activate(MyMapRenderer.class);
 *
 * </pre>
 * @since 4087
 */
public final class MapRendererFactory {

    /** preference key for the renderer class name. Default: class name for {@link StyledMapRenderer}
     *
     */
    public static final String PREF_KEY_RENDERER_CLASS_NAME = "mappaint.renderer-class-name";

    /**
     * An exception thrown while creating a map renderer
     */
    public static class MapRendererFactoryException extends RuntimeException {

        /**
         * Create a new {@link MapRendererFactoryException}
         * @param message The message
         * @param cause The cause
         */
        public MapRendererFactoryException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Create a new {@link MapRendererFactoryException}
         * @param message The message
         */
        public MapRendererFactoryException(String message) {
            super(message);
        }

        /**
         * Create a new {@link MapRendererFactoryException}
         * @param cause The cause
         */
        public MapRendererFactoryException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * A description of a possible renderer for the map
     */
    public static class Descriptor {
        private final Class<? extends AbstractMapRenderer> renderer;
        private final String displayName;
        private final String description;

        /**
         * Creates a new map renderer description
         * @param renderer The renderer
         * @param displayName The display name for the renderer
         * @param description The longer description that should be displayed to the user.
         */
        public Descriptor(Class<? extends AbstractMapRenderer> renderer, String displayName, String description) {
            this.renderer = renderer;
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Get the class of the renderer
         * @return The class
         */
        public Class<? extends AbstractMapRenderer> getRenderer() {
            return renderer;
        }

        /**
         * Get the display name
         * @return The name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Get the description
         * @return The description
         */
        public String getDescription() {
            return description;
        }
    }

    private static MapRendererFactory instance;

    /**
     * Replies the unique instance
     * @return instance of map rending class
     */
    public static synchronized MapRendererFactory getInstance() {
        if (instance == null) {
            instance = new MapRendererFactory();
        }
        return instance;
    }

    private static Class<?> loadRendererClass(String className) {
        for (ClassLoader cl : PluginHandler.getResourceClassLoaders()) {
            try {
                return Class.forName(className, true, cl);
            } catch (final NoClassDefFoundError | ClassNotFoundException e) {
                Logging.trace(e);
            }
        }
        Logging.error(tr("Failed to load map renderer class ''{0}''. The class wasn''t found.", className));
        return null;
    }

    private final List<Descriptor> descriptors = new ArrayList<>();
    private Class<? extends AbstractMapRenderer> activeRenderer;

    private MapRendererFactory() {
        registerDefaultRenderers();
        String rendererClassName = Config.getPref().get(PREF_KEY_RENDERER_CLASS_NAME, null);
        if (rendererClassName != null) {
            activateMapRenderer(rendererClassName);
        } else {
            activateDefault();
        }
    }

    private void activateMapRenderer(String rendererClassName) {
        Class<?> c = loadRendererClass(rendererClassName);
        if (c == null) {
            Logging.error(tr("Can''t activate map renderer class ''{0}'', because the class wasn''t found.", rendererClassName));
            Logging.error(tr("Activating the standard map renderer instead."));
            activateDefault();
        } else if (!AbstractMapRenderer.class.isAssignableFrom(c)) {
            Logging.error(tr("Can''t activate map renderer class ''{0}'', because it isn''t a subclass of ''{1}''.",
                    rendererClassName, AbstractMapRenderer.class.getName()));
            Logging.error(tr("Activating the standard map renderer instead."));
            activateDefault();
        } else {
            Class<? extends AbstractMapRenderer> renderer = c.asSubclass(AbstractMapRenderer.class);
            if (!isRegistered(renderer)) {
                Logging.error(tr("Can''t activate map renderer class ''{0}'', because it isn''t registered as map renderer.",
                        rendererClassName));
                Logging.error(tr("Activating the standard map renderer instead."));
                activateDefault();
            } else {
                activate(renderer);
            }
        }
    }

    private void registerDefaultRenderers() {
        register(
                WireframeMapRenderer.class,
                tr("Wireframe Map Renderer"),
                tr("Renders the map as simple wire frame.")
        );
        register(
                StyledMapRenderer.class,
                tr("Styled Map Renderer"),
                tr("Renders the map using style rules in a set of style sheets.")
        );
    }

    /**
     * <p>Replies true, if {@code Renderer} is already a registered map renderer class.</p>
     *
     * @param renderer the map renderer class. Must not be null.
     * @return true, if {@code Renderer} is already a registered map renderer class
     * @throws IllegalArgumentException if {@code renderer} is null
     */
    public boolean isRegistered(Class<? extends AbstractMapRenderer> renderer) {
        CheckParameterUtil.ensureParameterNotNull(renderer);
        return descriptors.stream().anyMatch(d -> d.getRenderer().equals(renderer));
    }

    /**
     * <p>Registers a map renderer class.</p>
     *
     * @param renderer the map renderer class. Must not be null.
     * @param displayName the display name to be displayed in UIs (i.e. in the preference dialog)
     * @param description the description
     * @throws IllegalArgumentException if {@code renderer} is null
     * @throws IllegalStateException if {@code renderer} is already registered
     */
    public void register(Class<? extends AbstractMapRenderer> renderer, String displayName, String description) {
        CheckParameterUtil.ensureParameterNotNull(renderer);
        if (isRegistered(renderer))
            throw new IllegalStateException(
                    // no I18n - this is a technical message
                    MessageFormat.format("Class ''{0}'' already registered a renderer", renderer.getName())
            );
        Descriptor d = new Descriptor(renderer, displayName, description);
        descriptors.add(d);
    }

    /**
     * <p>Unregisters a map renderer class.</p>
     *
     * <p>If the respective class is also the active renderer, the renderer is reset
     * to the default renderer.</p>
     *
     * @param renderer the map renderer class. Must not be null.
     *
     */
    public void unregister(Class<? extends AbstractMapRenderer> renderer) {
        if (renderer == null) return;
        if (!isRegistered(renderer)) return;
        Iterator<Descriptor> it = descriptors.iterator();
        while (it.hasNext()) {
            Descriptor d = it.next();
            if (d.getRenderer().equals(renderer)) {
                it.remove();
                break;
            }
        }
        if (activeRenderer != null && activeRenderer.equals(renderer)) {
            activateDefault();
        }
    }

    /**
     * <p>Activates a map renderer class.</p>
     *
     * <p>The renderer class must already be registered.</p>
     *
     * @param renderer the map renderer class. Must not be null.
     * @throws IllegalArgumentException if {@code renderer} is null
     * @throws IllegalStateException if {@code renderer} isn't registered yet
     */
    public void activate(Class<? extends AbstractMapRenderer> renderer) {
        CheckParameterUtil.ensureParameterNotNull(renderer);
        if (!isRegistered(renderer))
            throw new IllegalStateException(
                    // no I18n required
                    MessageFormat.format("Class ''{0}'' not registered as renderer. Can''t activate it.", renderer.getName())
            );
        this.activeRenderer = renderer;
        Config.getPref().put(PREF_KEY_RENDERER_CLASS_NAME, activeRenderer.getName());

    }

    /**
     * <p>Activates the default map renderer.</p>
     *
     * @throws IllegalStateException if the default renderer {@link StyledMapRenderer} isn't registered
     */
    public void activateDefault() {
        Class<? extends AbstractMapRenderer> defaultRenderer = StyledMapRenderer.class;
        if (!isRegistered(defaultRenderer))
            throw new IllegalStateException(
                    MessageFormat.format("Class ''{0}'' not registered as renderer. Can''t activate default renderer.",
                            defaultRenderer.getName())
            );
        activate(defaultRenderer);
    }

    /**
     * <p>Creates an instance of the currently active renderer.</p>
     * @param g Graphics
     * @param viewport Navigatable component
     * @param isInactiveMode {@code true} if the paint visitor shall render OSM objects such that they look inactive
     * @return an instance of the currently active renderer
     *
     * @throws MapRendererFactoryException if creating an instance fails
     * @see AbstractMapRenderer#AbstractMapRenderer(Graphics2D, NavigatableComponent, boolean)
     */
    public AbstractMapRenderer createActiveRenderer(Graphics2D g, NavigatableComponent viewport, boolean isInactiveMode) {
        try {
            Constructor<?> c = activeRenderer.getConstructor(Graphics2D.class, NavigatableComponent.class, boolean.class);
            return AbstractMapRenderer.class.cast(c.newInstance(g, viewport, isInactiveMode));
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new MapRendererFactoryException(e);
        }
    }

    /**
     * <p>Replies the (unmodifiable) list of map renderer descriptors.</p>
     *
     * @return the descriptors
     */
    public List<Descriptor> getMapRendererDescriptors() {
        return Collections.unmodifiableList(descriptors);
    }

    /**
     * <p>Replies true, if currently the wireframe map renderer is active. Otherwise, false.</p>
     *
     * <p>There is a specific method for {@link WireframeMapRenderer} for legacy support.
     * Until 03/2011 there were only two possible map renderers in JOSM: the wireframe
     * renderer and the styled renderer. For the time being there are still UI elements
     * (menu entries, etc.) which toggle between these two renderers only.</p>
     *
     * @return true, if currently the wireframe map renderer is active. Otherwise, false
     */
    public boolean isWireframeMapRendererActive() {
        return WireframeMapRenderer.class.equals(activeRenderer);
    }
}
