// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.openstreetmap.josm.TestUtils.getPrivateStaticField;

import java.awt.Color;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.gui.bbox.JosmMapViewer;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.TileSourceRule;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Replace imagery sources with a default set of mock tile sources.
 * @author Taylor Smock
 * @since 18893
 * @see JOSMTestRules#fakeImagery()
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@BasicPreferences
@ExtendWith(FakeImagery.FakeImageryWireMockExtension.class)
public @interface FakeImagery {
    /**
     * A wiremock extension for fake imagery
     */
    class FakeImageryWireMockExtension extends WireMockExtension {

        private final boolean clearLayerList;
        private final boolean clearSlippyMapSources;
        private final boolean registerInLayerList;
        private final List<TileSourceRule.ConstSource> sources;

        static ExtensionContext.Store getStore(ExtensionContext extensionContext) {
            return extensionContext.getStore(ExtensionContext.Namespace.create(FakeImageryWireMockExtension.class));
        }

        /**
         * See {@link FakeImageryWireMockExtension#FakeImageryWireMockExtension(boolean, boolean, boolean, TileSourceRule.ConstSource...)}.
         * This provides tile sources for that are white, black, magenta, or green.
         */
        FakeImageryWireMockExtension() {
            this(
                    true,
                    true,
                    true,
                    new TileSourceRule.ColorSource(Color.WHITE, "White Tiles", 256),
                    new TileSourceRule.ColorSource(Color.BLACK, "Black Tiles", 256),
                    new TileSourceRule.ColorSource(Color.MAGENTA, "Magenta Tiles", 256),
                    new TileSourceRule.ColorSource(Color.GREEN, "Green Tiles", 256)
            );
        }

        /**
         * Construct a FakeImageryWireMockExtension for use with a JUnit test.
         * <p>
         * This is hidden for now, since all internal used {@link JOSMTestRules#fakeImagery()} instead of
         * {@link JOSMTestRules#fakeImagery(TileSourceRule)}. Before making this public, we'll probably want to move
         * {@link TileSourceRule.ConstSource} and it's subclasses around.
         * <p>
         * The three boolean parameters control whether to perform various steps registering the tile sources with parts
         * of JOSM's internals as part of the setup process. It is advised to only enable any of these if it can be ensured
         * that this rule will have its setup routine executed *after* the relevant parts of JOSM have been set up, e.g.
         * when handled by {@link FakeImagery}.
         *
         * @param clearLayerList whether to clear ImageryLayerInfo's layer list of any pre-existing entries
         * @param clearSlippyMapSources whether to clear SlippyMapBBoxChooser's stubborn fallback Mapnik TileSource
         * @param registerInLayerList whether to add sources to ImageryLayerInfo's layer list
         * @param sources tile sources to serve from this mock server
         */
        private FakeImageryWireMockExtension(boolean clearLayerList, boolean clearSlippyMapSources, boolean registerInLayerList,
                                             TileSourceRule.ConstSource... sources) {
            super(WireMockExtension.extensionOptions());
            this.clearLayerList = clearLayerList;
            this.clearSlippyMapSources = clearSlippyMapSources;
            this.registerInLayerList = registerInLayerList;
            this.sources = Collections.unmodifiableList(Arrays.asList(sources.clone()));
        }

        /**
         * Get the tile sources served by this {@link FakeImageryWireMockExtension}.
         *
         * @return an unmodifiable list of the tile sources served by this {@link FakeImageryWireMockExtension}
         */
        public List<TileSourceRule.ConstSource> getSourcesList() {
            return this.sources;
        }

        @Override
        protected void onBeforeEach(ExtensionContext extensionContext, WireMockRuntimeInfo wireMockRuntimeInfo) {
            super.onBeforeEach(wireMockRuntimeInfo);
            final ExtensionContext.Store store = getStore(extensionContext);
            registerLayers(store, wireMockRuntimeInfo);
            for (TileSourceRule.ConstSource source : this.sources) {
                this.stubFor(source.getMappingBuilder().willReturn(source.getResponseDefinitionBuilder()));
            }
        }

        @Override
        protected void onAfterEach(ExtensionContext extensionContext, WireMockRuntimeInfo wireMockRuntimeInfo) {
            try {
                super.onAfterEach(wireMockRuntimeInfo);
            } finally {
                final ExtensionContext.Store store = getStore(extensionContext);
                unregisterLayers(store);
            }
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            if (parameterContext.getParameter().getType().equals(FakeImageryWireMockExtension.class)) {
                return true;
            }
            return super.supportsParameter(parameterContext, extensionContext);
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            if (parameterContext.getParameter().getType().equals(FakeImageryWireMockExtension.class)) {
                return this;
            }
            return super.resolveParameter(parameterContext, extensionContext);
        }

        private void registerLayers(ExtensionContext.Store store, WireMockRuntimeInfo wireMockRuntimeInfo) {
            if (this.clearSlippyMapSources) {
                try {
                    @SuppressWarnings("unchecked")
                    List<JosmMapViewer.TileSourceProvider> slippyMapProviders =
                            (List<JosmMapViewer.TileSourceProvider>) getPrivateStaticField(
                                    SlippyMapBBoxChooser.class,
                                    "providers"
                            );
                    // pop this off the beginning of the list, keep for later
                    JosmMapViewer.TileSourceProvider slippyMapDefaultProvider = slippyMapProviders.remove(0);
                    store.put("slippyMapProviders", slippyMapProviders);
                    store.put("slippyMapDefaultProvider", slippyMapDefaultProvider);
                } catch (ReflectiveOperationException e) {
                    Logging.warn("Failed to remove default SlippyMapBBoxChooser TileSourceProvider");
                    Logging.trace(e);
                }
            }

            if (this.clearLayerList) {
                store.put("originalImageryInfoList", List.copyOf(ImageryLayerInfo.instance.getLayers()));
                ImageryLayerInfo.instance.clear();
            }
            if (this.registerInLayerList) {
                for (TileSourceRule.ConstSource source : this.sources) {
                    ImageryLayerInfo.addLayer(source.getImageryInfo(wireMockRuntimeInfo.getHttpPort()));
                }
            }
        }

        private static void unregisterLayers(ExtensionContext.Store store) {
            @SuppressWarnings("unchecked")
            final List<JosmMapViewer.TileSourceProvider> slippyMapProviders = store.get("slippyMapProviders", List.class);
            JosmMapViewer.TileSourceProvider slippyMapDefaultProvider =
                    store.get("slippyMapDefaultProvider", JosmMapViewer.TileSourceProvider.class);
            @SuppressWarnings("unchecked")
            List<ImageryInfo> originalImageryInfoList = store.get("originalImageryInfoList", List.class);
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
}
