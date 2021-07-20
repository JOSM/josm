// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openstreetmap.josm.TestUtils.getPrivateStaticField;

import java.awt.Color;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.testutils.annotations.fake_imagery.ColorSource;
import org.openstreetmap.josm.testutils.annotations.fake_imagery.ConstSource;
import org.openstreetmap.josm.tools.Logging;
import org.opentest4j.AssertionFailedError;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * An annotation for fake imagery
 * Please note that this currently only supports single-color tile sources.
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@BasicWiremock
@ExtendWith(FakeImagery.FakeImageryExtension.class)
@ExtendWith(FakeImagery.FakeImageryParameterResolver.class)
public @interface FakeImagery {
    /**
     * The options to use for FakeImagery
     * @return The options
     */
    Options[] options() default {Options.CLEAR_LAYER_LIST, Options.CLEAR_SLIPPY_MAP_SOURCES, Options.REGISTER_IN_LAYER_LIST};

    /**
     * The color tile sources to use for FakeImagery
     * @return The simple color tile sources
     */
    ColorTileSource[] colorTileSources() default {@ColorTileSource(colors = Colors.WHITE, name = "White Tiles"),
        @ColorTileSource(colors = Colors.BLACK, name = "Black Tiles"),
        @ColorTileSource(colors = Colors.MAGENTA, name = "Magenta Tiles"), @ColorTileSource(colors = Colors.GREEN, name = "Green Tiles")};

    /**
     * The more complex tilesources to use. The classes <i>must</i> have a constructor with no arguments
     * @return The tile sources to instantiate
     */
    Class<? extends ConstSource>[] tileSources() default {};

    /**
     * Define a simple tile source (only single color supported at this time)
     * @author Taylor Smock
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.LOCAL_VARIABLE)
    @interface ColorTileSource {
        /**
         * Pixel dimension of tiles (usually 256)
         * @return The expected size for the tiles
         */
        int size() default 256;

        /**
         * Color for these tiles
         * @return The Color enum (sorry, you can't make your own arbitrary colors)
         */
        Colors colors() default Colors.BLACK;

        /**
         * text label/name for this source if displayed in JOSM menus
         * @return The name for the layer/menu entry
         */
        String name();
    }

    /**
     * The options that can be used for FakeImagery
     * @author Taylor Smock
     *
     */
    enum Options {
        /**
         * whether to clear ImageryLayerInfo's layer list of any pre-existing entries
         */
        CLEAR_LAYER_LIST,
        /**
         * whether to clear SlippyMapBBoxChooser's stubborn fallback Mapnik ColorTileSource
         */
        CLEAR_SLIPPY_MAP_SOURCES,
        /**
         * whether to add sources to ImageryLayerInfo's layer list
         */
        REGISTER_IN_LAYER_LIST
    }

    /**
     * An enum for colors (currently only uses static colors from {@link Color})
     */
    enum Colors {
        BLACK(Color.BLACK),
        BLUE(Color.BLUE),
        CYAN(Color.CYAN),
        DARK_GRAY(Color.DARK_GRAY),
        GRAY(Color.GRAY),
        GREEN(Color.GREEN),
        LIGHT_GRAY(Color.LIGHT_GRAY),
        MAGENTA(Color.MAGENTA),
        ORANGE(Color.ORANGE),
        PINK(Color.PINK),
        RED(Color.RED),
        WHITE(Color.WHITE),
        YELLOW(Color.YELLOW);

        private final Color color;
        Colors(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return this.color;
        }
    }

    /**
     * A class to specifically mock Imagery calls
     */
    class FakeImageryExtension extends BasicWiremock.WireMockExtension {
        static final String SLIPPY_MAP_PROVIDERS = "slippyMapProviders";
        static final String SLIPPY_MAP_DEFAULT_PROVIDER = "slippyMapDefaultProvider";
        static final String ORIGINAL_IMAGERY_INFO_LIST = "originalImageryInfoList";

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            try {
                super.afterEach(context);
            } finally {
                cleanup(context);
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            if (context.getStore(ExtensionContext.Namespace.create(BasicPreferences.BasicPreferencesExtension.class))
                    .get("preferences") == null) {
                fail("FakeImageryExtension requires preferences (try @BasicPreferences)");
            }
            super.beforeAll(context);

            // Get the wiremock server
            final WireMockServer wireMockServer = getWiremock(context);

            // set up a stub target for the early request hack
            wireMockServer.stubFor(WireMock.get(
                    WireMock.urlMatching("/_poke")
            ).willReturn(WireMock.aResponse().withStatus(200).withBody("ow.")));
            runServer(wireMockServer);
        }

        @Override
        public void beforeEach(final ExtensionContext context) throws Exception {
            super.beforeEach(context);
            final FakeImagery fakeImagery = AnnotationSupport.findAnnotation(context.getElement(), FakeImagery.class)
                    .orElseGet(() -> AnnotationSupport.findAnnotation(context.getRequiredTestClass(), FakeImagery.class)
                    .orElseThrow((Supplier<AssertionFailedError>) () -> fail(context.getDisplayName() + " missing @FakeImagery annotation")));
            final Set<Options> options = EnumSet.copyOf(Arrays.asList(fakeImagery.options()));
            final ColorTileSource[] colorTileSources = fakeImagery.colorTileSources();
            final Class<? extends ConstSource>[] additionalTileSources = fakeImagery.tileSources();

            // Get the wiremock server
            final WireMockServer wireMockServer = getWiremock(context);

            final List<ConstSource> constSourceList = new ArrayList<>(colorTileSources.length + additionalTileSources.length);
            for (ColorTileSource source : colorTileSources) {
                final ColorSource colorSource = new ColorSource(source);
                constSourceList.add(colorSource);
                wireMockServer.stubFor(colorSource.getMappingBuilder().willReturn(colorSource.getResponseDefinitionBuilder()));
            }

            for (Class<? extends ConstSource> constSourceClazz : additionalTileSources) {
                final ConstSource constSource = ReflectionUtils.newInstance(constSourceClazz);
                constSourceList.add(constSource);
                wireMockServer.stubFor(constSource.getMappingBuilder().willReturn(constSource.getResponseDefinitionBuilder()));
            }

            registerLayers(context, options, constSourceList, wireMockServer);
        }

        /**
         * A hack to circumvent a WireMock bug concerning delayed server startup. sending an early request
         * to the mock server seems to prompt it to start earlier (though this request itself is not
         * expected to succeed). See <a href="https://github.com/tomakehurst/wiremock/issues/97">WireMock Issue #97</a>
         */
        private static void runServer(WireMockServer wireMockServer) {
            try {
                new java.net.URL(wireMockServer.url("/_poke")).getContent();
            } catch (IOException e) {
                Logging.trace(e);
            }
        }

        private static void registerLayers(final ExtensionContext context, final Set<Options> options, final List<ConstSource> sourcesList,
                final WireMockServer wireMockServer) {
            if (options.contains(Options.REGISTER_IN_LAYER_LIST) || options.contains(Options.CLEAR_LAYER_LIST)) {
                ExtensionContext.Store store = context.getStore(
                        ExtensionContext.Namespace.create(FakeImageryExtension.class));
                if (options.contains(Options.CLEAR_SLIPPY_MAP_SOURCES)) {
                    try {
                        final List<SlippyMapBBoxChooser.TileSourceProvider> slippyMapProviders =
                                (List<SlippyMapBBoxChooser.TileSourceProvider>)
                                        getPrivateStaticField(SlippyMapBBoxChooser.class, "providers");
                        // pop this off the beginning of the list, keep for later
                        final SlippyMapBBoxChooser.TileSourceProvider slippyMapDefaultProvider = slippyMapProviders.remove(0);
                        store.put(SLIPPY_MAP_PROVIDERS, slippyMapProviders);
                        store.put(SLIPPY_MAP_DEFAULT_PROVIDER, slippyMapDefaultProvider);
                    } catch (ReflectiveOperationException e) {
                        Logging.warn("Failed to remove default SlippyMapBBoxChooser TileSourceProvider");
                    }
                }

                if (options.contains(Options.CLEAR_LAYER_LIST)) {
                    final List<ImageryInfo> originalImageryInfoList = ImageryLayerInfo.instance.getLayers();
                    store.put(ORIGINAL_IMAGERY_INFO_LIST, originalImageryInfoList);
                    ImageryLayerInfo.instance.clear();
                }
                if (options.contains(Options.REGISTER_IN_LAYER_LIST)) {
                    for (ConstSource source : sourcesList) {
                        ImageryLayerInfo.addLayer(source.getImageryInfo(wireMockServer));
                    }
                }
                store.put(ConstSource.class, sourcesList);
            }
        }

        /**
         * Cleanup the environment
         * @param context The context with the original values
         */
        private static void cleanup(final ExtensionContext context) {
            final ExtensionContext.Store store = context.getStore(
                    ExtensionContext.Namespace.create(FakeImageryExtension.class));
            final List<SlippyMapBBoxChooser.TileSourceProvider> slippyMapProviders = (List<SlippyMapBBoxChooser.TileSourceProvider>)
                    store.remove(SLIPPY_MAP_PROVIDERS, List.class);
            final SlippyMapBBoxChooser.TileSourceProvider slippyMapDefaultProvider = store.remove(SLIPPY_MAP_DEFAULT_PROVIDER,
                    SlippyMapBBoxChooser.TileSourceProvider.class);
            final List<ImageryInfo> originalImageryInfoList = (List<ImageryInfo>) store.remove(ORIGINAL_IMAGERY_INFO_LIST, List.class);
            // clean up to original state
            if (slippyMapDefaultProvider != null && slippyMapProviders != null) {
                slippyMapProviders.add(0, slippyMapDefaultProvider);
            }
            if (originalImageryInfoList != null) {
                ImageryLayerInfo.instance.clear();
                ImageryLayerInfo.addLayers(originalImageryInfoList);
            }
        }
    }

    /**
     * A parameter resolver for FakeImagery
     */
    class FakeImageryParameterResolver implements ParameterResolver {
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            ExtensionContext.Store store = extensionContext.getStore(
                    ExtensionContext.Namespace.create(FakeImageryExtension.class));
            if (store.get(ConstSource.class) != null && parameterContext.isAnnotated(FakeImagery.class)
                    && List.class.isAssignableFrom(parameterContext.getParameter().getType())) {
                 Optional<Method> optionalMethod = ReflectionUtils.findMethod(parameterContext.getParameter().getParameterizedType().getClass(),
                         "getActualTypeArguments");
                 if (!optionalMethod.isPresent() || !optionalMethod.get().getReturnType().isAssignableFrom(Type[].class)) {
                     // We can't check the type. Therefore, we must hope that everything goes well.
                     return true;
                 }
                 Type[] types = (Type[]) ReflectionUtils.invokeMethod(optionalMethod.get(),
                         parameterContext.getParameter().getParameterizedType());
                 return types.length == 1 && ConstSource.class.isAssignableFrom((Class<?>) types[0]);
            }
            return false;
        }

        @Override
        public List<ConstSource> resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            ExtensionContext.Store store = extensionContext.getStore(
                    ExtensionContext.Namespace.create(FakeImageryExtension.class));
            return (List<ConstSource>) store.get(ConstSource.class, List.class);
        }
    }
}
