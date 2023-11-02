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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
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
@ExtendWith(FakeImagery.FakeImageryExtension.class)
public @interface FakeImagery {
    /**
     * This is a stop-gap for <a href="https://github.com/wiremock/wiremock/pull/1981">WireMock #1981</a>.
     * We just wrap everything.
     */
    class FakeImageryExtension implements ParameterResolver,
            BeforeEachCallback,
            BeforeAllCallback,
            AfterEachCallback,
            AfterAllCallback {

        @Override
        public void afterAll(ExtensionContext extensionContext) throws Exception {
            getActualExtension(extensionContext).afterAll(extensionContext);
        }

        @Override
        public void afterEach(ExtensionContext extensionContext) throws Exception {
            final FakeImageryWireMockExtension extension = getActualExtension(extensionContext);
            extension.afterEach(extensionContext);
            extension.onAfterEach(extensionContext, getWireMockRuntimeInfo(extensionContext));
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) throws Exception {
            getActualExtension(extensionContext).beforeAll(extensionContext);
        }

        @Override
        public void beforeEach(ExtensionContext extensionContext) throws Exception {
            final FakeImageryWireMockExtension extension = getActualExtension(extensionContext);
            extension.beforeEach(extensionContext);
            extension.onBeforeEach(extensionContext, getWireMockRuntimeInfo(extensionContext));
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            if (parameterContext.getParameter().getType().equals(FakeImageryWireMockExtension.class)) {
                return true;
            }
            return getActualExtension(extensionContext).supportsParameter(parameterContext, extensionContext);
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            if (parameterContext.getParameter().getType().equals(FakeImageryWireMockExtension.class)) {
                return getActualExtension(extensionContext);
            }
            return getActualExtension(extensionContext).resolveParameter(parameterContext, extensionContext);
        }

        private static FakeImageryWireMockExtension getActualExtension(ExtensionContext extensionContext) {
            return FakeImageryWireMockExtension.getStore(extensionContext)
                    .getOrComputeIfAbsent(FakeImageryWireMockExtension.class, ignored -> new FakeImageryWireMockExtension(),
                            FakeImageryWireMockExtension.class);
        }

        private static WireMockRuntimeInfo getWireMockRuntimeInfo(ExtensionContext extensionContext) {
            return FakeImageryWireMockExtension.getStore(extensionContext)
                    .getOrComputeIfAbsent(WireMockRuntimeInfo.class, ignored -> getActualExtension(extensionContext).getRuntimeInfo(),
                            WireMockRuntimeInfo.class);

        }
    }

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

        protected void onBeforeEach(ExtensionContext extensionContext, WireMockRuntimeInfo wireMockRuntimeInfo) {
            super.onBeforeEach(wireMockRuntimeInfo);
            final ExtensionContext.Store store = getStore(extensionContext);
            registerLayers(store, wireMockRuntimeInfo);
            for (TileSourceRule.ConstSource source : this.sources) {
                this.stubFor(source.getMappingBuilder().willReturn(source.getResponseDefinitionBuilder()));
            }
        }

        protected void onAfterEach(ExtensionContext extensionContext, WireMockRuntimeInfo wireMockRuntimeInfo) {
            super.onAfterEach(wireMockRuntimeInfo);
            final ExtensionContext.Store store = getStore(extensionContext);
            unregisterLayers(store);
        }

        private void registerLayers(ExtensionContext.Store store, WireMockRuntimeInfo wireMockRuntimeInfo) {
            if (this.clearSlippyMapSources) {
                try {
                    @SuppressWarnings("unchecked")
                    List<SlippyMapBBoxChooser.TileSourceProvider> slippyMapProviders =
                            (List<SlippyMapBBoxChooser.TileSourceProvider>) getPrivateStaticField(
                                    SlippyMapBBoxChooser.class,
                                    "providers"
                            );
                    // pop this off the beginning of the list, keep for later
                    SlippyMapBBoxChooser.TileSourceProvider slippyMapDefaultProvider = slippyMapProviders.remove(0);
                    store.put("slippyMapProviders", slippyMapProviders);
                    store.put("slippyMapDefaultProvider", slippyMapDefaultProvider);
                } catch (ReflectiveOperationException e) {
                    Logging.warn("Failed to remove default SlippyMapBBoxChooser TileSourceProvider");
                }
            }

            if (this.clearLayerList) {
                store.put("originalImageryInfoList", ImageryLayerInfo.instance.getLayers());
                ImageryLayerInfo.instance.clear();
            }
            if (this.registerInLayerList) {
                for (TileSourceRule.ConstSource source : this.sources) {
                    ImageryLayerInfo.addLayer(source.getImageryInfo(wireMockRuntimeInfo.getHttpPort()));
                }
            }
        }

        private void unregisterLayers(ExtensionContext.Store store) {
            @SuppressWarnings("unchecked")
            final List<SlippyMapBBoxChooser.TileSourceProvider> slippyMapProviders =
                    (List<SlippyMapBBoxChooser.TileSourceProvider>) store.get("slippyMapProviders", List.class);
            SlippyMapBBoxChooser.TileSourceProvider slippyMapDefaultProvider =
                    store.get("slippyMapDefaultProvider", JosmMapViewer.TileSourceProvider.class);
            @SuppressWarnings("unchecked")
            List<ImageryInfo> originalImageryInfoList = (List<ImageryInfo>) store.get("originalImageryInfoList", List.class);
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
