// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.io.AbstractReader;
import org.openstreetmap.josm.io.OsmServerReadPostprocessor;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * Cleanup plugins if they've been loaded
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(Plugins.PluginExtension.class)
public @interface Plugins {
    /**
     * The extension to clean up after plugin installs
     */
    class PluginExtension implements AfterEachCallback {

        @SuppressWarnings("unchecked")
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            // We want to clean up as much as possible using "standard" methods
            for (PluginInformation plugin : PluginHandler.getPlugins()) {
                Object root = PluginHandler.getPlugin(plugin.name);
                if (root instanceof Destroyable) {
                    ((Destroyable) root).destroy();
                    PluginHandler.removePlugins(Collections.singletonList(plugin));
                }
            }
            final Field pluginListField = PluginHandler.class.getDeclaredField("pluginList");
            final Field classLoadersField = PluginHandler.class.getDeclaredField("classLoaders");
            final Field postprocessorsField = AbstractReader.class.getDeclaredField("postprocessors");
            org.openstreetmap.josm.tools.ReflectionUtils.setObjectsAccessible(classLoadersField, postprocessorsField,
                    pluginListField);
            ((List<?>) pluginListField.get(null)).clear();
            ((Map<?, ?>) classLoadersField.get(null)).clear();
            // Needed due to SDS
            final Object postprocessors = postprocessorsField.get(null);
            if (postprocessors instanceof Collection) {
                for (OsmServerReadPostprocessor pp : new ArrayList<>((Collection<OsmServerReadPostprocessor>) postprocessors)) {
                    AbstractReader.deregisterPostprocessor(pp);
                }
            }
        }
    }
}
