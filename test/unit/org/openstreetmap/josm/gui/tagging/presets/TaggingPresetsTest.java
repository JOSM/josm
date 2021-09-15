// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.tools.Logging;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link TaggingPresets} class.
 */
public class TaggingPresetsTest {
    /**
     * Tests that {@code TaggingPresets} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(TaggingPresets.class);
    }

    /**
     * Wait for asynchronous icon loading
     * @param presets presets collection
     */
    public static void waitForIconLoading(Collection<TaggingPreset> presets) {
        presets.parallelStream().map(TaggingPreset::getIconLoadingTask).filter(Objects::nonNull).forEach(t -> {
            try {
                t.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Logging.error(e);
            }
        });
    }
}
