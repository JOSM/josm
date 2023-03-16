// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.tools.Logging;

public class PluginServer {
    public static class RemotePlugin {
        private final File srcJar;
        private final Map<String, String> attrOverrides;
        private final String pluginName;
        private final String pluginURL;

        public RemotePlugin(
            final File srcJar
        ) {
            this(srcJar, null, null, null);
        }

        public RemotePlugin(
            final File srcJar,
            final Map<String, String> attrOverrides
        ) {
            this(srcJar, attrOverrides, null, null);
        }

        public RemotePlugin(
            final File srcJar,
            final Map<String, String> attrOverrides,
            final String pluginName
        ) {
            this(srcJar, attrOverrides, pluginName, null);
        }

        public RemotePlugin(
            final File srcJar,
            final Map<String, String> attrOverrides,
            final String pluginName,
            final String pluginURL
        ) {
            this.srcJar = srcJar;
            this.attrOverrides = attrOverrides;
            this.pluginName = pluginName;
            this.pluginURL = pluginURL;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.srcJar, this.attrOverrides, this.pluginName, this.pluginURL, this.getClass());
        }

        public String getRemotePluginsListManifestSection() {
            final Map<String, String> attrs = new HashMap<>();
            JarFile jarFile = null;

            if (srcJar != null) {
                try {
                    jarFile = new JarFile(srcJar, false);
                    jarFile.getManifest().getMainAttributes()
                            .forEach((key, value) -> attrs.put(key.toString(), value.toString()));
                } catch (IOException e) {
                    Logging.warn(
                        "Failed to open {0} as a jar file. Using empty initial manifest. Error was: {1}",
                        srcJar,
                        e
                    );
                } finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close();
                        } catch (IOException e) {
                            Logging.warn(
                                "Somehow failed to close jar file {0}. Error was: {1}",
                                srcJar,
                                e
                            );
                        }
                    }
                }
            }

            if (this.attrOverrides != null) {
                attrs.putAll(this.attrOverrides);
            }

            return attrs.entrySet().stream().filter(entry -> entry.getValue() != null).map(
                entry -> String.format("\t%s: %s\n", entry.getKey(), entry.getValue())
            ).collect(Collectors.joining());
        }

        private String getJarPathBeneathFilesDir() {
            if (this.srcJar != null) {
                final Path jarPath = this.srcJar.toPath().toAbsolutePath().normalize();
                final Path filesRootPath = new File(TestUtils.getTestDataRoot()).toPath().toAbsolutePath().resolve("__files").normalize();

                if (jarPath.startsWith(filesRootPath)) {
                    // would just use .toString() but need to force use of *forward slash* path separators on all platforms
                    final Path path = filesRootPath.relativize(jarPath);
                    return StreamSupport.stream(path.spliterator(), false)
                            .map(Path::toString)
                            .collect(Collectors.joining("/"));
                }
            }
            return null;
        }

        protected String getPluginURLPath() {
            final String jarPathBeneathFilesDir = this.getJarPathBeneathFilesDir();

            if (jarPathBeneathFilesDir != null) {
                return "/" + jarPathBeneathFilesDir;
            }

            return String.format("/%h/%s.jar", this.hashCode(), pluginName != null ? pluginName : Integer.toHexString(this.hashCode()));
        }

        public String getPluginURL(WireMockRuntimeInfo wireMock) {
            if (this.pluginURL != null) {
                return this.pluginURL;
            } else if (wireMock != null && this.getJarPathBeneathFilesDir() != null) {
                return wireMock.getHttpBaseUrl() + this.getPluginURLPath();
            }
            return "http://example.com" + this.getPluginURLPath();
        }

        public String getName() {
            if (this.pluginName != null) {
                return this.pluginName;
            } else if (this.srcJar != null) {
                return this.srcJar.getName().split("\\.", 2)[0];
            }
            return Integer.toHexString(this.hashCode());
        }

        public String getRemotePluginsListSection(WireMockRuntimeInfo wireMock) {
            return String.format(
                "%s.jar;%s\n%s",
                this.getName(),
                this.getPluginURL(wireMock),
                this.getRemotePluginsListManifestSection()
            );
        }

        public MappingBuilder getMappingBuilder() {
            final String jarPathBeneathFilesDir = this.getJarPathBeneathFilesDir();

            if (jarPathBeneathFilesDir != null) {
                return WireMock.get(WireMock.urlMatching(this.getPluginURLPath()));
            }
            return null;
        }

        public ResponseDefinitionBuilder getResponseDefinitionBuilder() {
            final String jarPathBeneathFilesDir = this.getJarPathBeneathFilesDir();

            if (jarPathBeneathFilesDir != null) {
                return WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/java-archive").withBodyFile(
                    jarPathBeneathFilesDir
                );
            }
            return null;
        }
    }

    protected final List<RemotePlugin> pluginList;

    public PluginServer(RemotePlugin... remotePlugins) {
        this.pluginList = Arrays.asList(remotePlugins);
    }

    public void applyToWireMockServer(WireMockRuntimeInfo wireMock) {
        final WireMock wireMockServer = wireMock.getWireMock();
        // first add the plugins list
        wireMockServer.register(
            WireMock.get(WireMock.urlEqualTo("/plugins")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("Content-Type", "text/plain").withBody(
                    this.pluginList.stream().map(
                        remotePlugin -> remotePlugin.getRemotePluginsListSection(wireMock)
                    ).collect(Collectors.joining())
                )
            )
        );

        // now add each file that we're able to serve
        for (final RemotePlugin remotePlugin : this.pluginList) {
            final MappingBuilder mappingBuilder = remotePlugin.getMappingBuilder();
            final ResponseDefinitionBuilder responseDefinitionBuilder = remotePlugin.getResponseDefinitionBuilder();

            if (mappingBuilder != null && responseDefinitionBuilder != null) {
                wireMockServer.register(
                    remotePlugin.getMappingBuilder().willReturn(remotePlugin.getResponseDefinitionBuilder())
                );
            }
        }
    }

    public PluginServerRule asWireMockRule() {
        return this.asWireMockRule(
            options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot()),
            true
        );
    }

    public PluginServerRule asWireMockRule(Options ruleOptions, boolean failOnUnmatchedRequests) {
        return new PluginServerRule(ruleOptions, failOnUnmatchedRequests);
    }

    public class PluginServerRule extends WireMockExtension {
        public PluginServerRule(Options ruleOptions, boolean failOnUnmatchedRequests) {
            super(extensionOptions().options(ruleOptions).failOnUnmatchedRequests(failOnUnmatchedRequests));
        }

        public PluginServer getPluginServer() {
            return PluginServer.this;
        }

        @Override
        protected void onBeforeEach(WireMockRuntimeInfo wireMockRuntimeInfo) {
            PluginServer.this.applyToWireMockServer(wireMockRuntimeInfo);
        }
    }
}
