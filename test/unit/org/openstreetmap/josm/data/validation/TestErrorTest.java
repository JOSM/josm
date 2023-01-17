// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.tests.InternetTags;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test class for {@link TestError}
 */
@BasicPreferences
class TestErrorTest {
    static Stream<Arguments> testCodeCompatibility() {
        return Stream.of(Arguments.of(InternetTags.class, 3301, 1166507262, false, Collections.singletonList(TestUtils.newNode("url=invalid"))),
                Arguments.of(InternetTags.class, 3301, 1166507262, true, Collections.singletonList(TestUtils.newNode("url=invalid"))));
    }

    /**
     * See #18230/#21423: Keep error codes unique
     *
     * @param testClass The test class to use
     * @param originalCode The expected error code (original)
     * @param expectedCode The expected error code (new, should be {@code testClass.getName().hashCode()})
     * @param switchOver {@code true} if the new code should be saved instead of the original code
     * @param primitiveCollection The primitives to run the test on
     * @throws ReflectiveOperationException If the test class could not be instantiated (no-op constructor)
     */
    @ParameterizedTest
    @MethodSource
    void testCodeCompatibility(Class<? extends Test> testClass, int originalCode, int expectedCode,
                               boolean switchOver, List<OsmPrimitive> primitiveCollection) throws ReflectiveOperationException {
        // Ensure that this test always works
        TestError.setUpdateErrorCodes(switchOver);
        assertEquals(expectedCode, testClass.getName().hashCode());
        // Run the test
        final Test test = testClass.getConstructor().newInstance();
        test.startTest(NullProgressMonitor.INSTANCE);
        test.visit(primitiveCollection);
        test.endTest();
        assertFalse(test.getErrors().isEmpty());
        assertEquals(1, test.getErrors().size());
        final TestError testError = test.getErrors().get(0);
        final String ignoreGroup = testError.getIgnoreGroup();
        final String ignoreSubGroup = testError.getIgnoreSubGroup();
        if (primitiveCollection.size() == 1 && primitiveCollection.get(0).isNew()) {
            assertNull(testError.getIgnoreState());
            primitiveCollection.get(0).setOsmId(1, 1);
        }
        final String ignoreState = testError.getIgnoreState();
        final String startUniqueCode = expectedCode + "_";
        assertAll(() -> assertTrue(ignoreGroup.startsWith(startUniqueCode + originalCode)),
                () -> assertTrue(ignoreSubGroup.startsWith(startUniqueCode + originalCode)),
                () -> assertTrue(ignoreState.startsWith(startUniqueCode + originalCode)));
        for (String ignore : Arrays.asList(ignoreGroup, ignoreSubGroup, ignoreState)) {
            OsmValidator.clearIgnoredErrors();
            final String oldIgnore = ignore.replace(startUniqueCode, "");
            OsmValidator.addIgnoredError(oldIgnore);
            // Add the ignored error
            assertTrue(testError.updateIgnored());
            assertAll(() -> assertEquals(switchOver, OsmValidator.hasIgnoredError(ignore)),
                    () -> assertNotEquals(switchOver, OsmValidator.hasIgnoredError(oldIgnore)));

            OsmValidator.clearIgnoredErrors();
            OsmValidator.addIgnoredError(ignore);
            // Add the ignored error
            assertTrue(testError.updateIgnored());
            assertAll(() -> assertTrue(OsmValidator.hasIgnoredError(ignore)),
                    () -> assertFalse(OsmValidator.hasIgnoredError(oldIgnore)));
        }
    }
}
