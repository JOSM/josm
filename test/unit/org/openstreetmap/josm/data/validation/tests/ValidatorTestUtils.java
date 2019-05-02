// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Utilities for validator unit tests.
 */
public final class ValidatorTestUtils {

    private ValidatorTestUtils() {
        // Hide default constructor for utilities classes
    }

    static <T extends OsmPrimitive> void testSampleFile(String sampleFile,
            Function<DataSet, Iterable<T>> provider, Predicate<String> namePredicate,
            Test... tests) throws Exception {
        try (InputStream is = Files.newInputStream(Paths.get(sampleFile))) {
            for (T t: provider.apply(OsmReader.parseDataSet(is, null))) {
                String name = DefaultNameFormatter.getInstance().format(t);
                List<TestError> errors = new ArrayList<>();
                for (Test test : tests) {
                    test.initialize();
                    test.startTest(null);
                    test.visit(Collections.singleton(t));
                    test.endTest();
                    errors.addAll(test.getErrors());
                }
                String codes = t.get("josm_error_codes");
                if (codes != null) {
                    Set<Integer> expectedCodes = new TreeSet<>();
                    if (!"none".equals(codes)) {
                        for (String code : codes.split(",")) {
                            expectedCodes.add(Integer.parseInt(code));
                        }
                    }
                    Set<Integer> actualCodes = new TreeSet<>();
                    for (TestError error : errors) {
                        Integer code = error.getCode();
                        assertTrue(name + " does not expect JOSM error code " + code + ": " + error.getDescription(),
                                expectedCodes.contains(code));
                        actualCodes.add(code);
                    }
                    assertEquals(name + " " + expectedCodes + " => " + actualCodes,
                            expectedCodes.size(), actualCodes.size());
                } else if (t.hasKey("name") && namePredicate != null && namePredicate.test(t.getName())) {
                    fail(name + " lacks josm_error_codes tag");
                } else if (t.hasKey("name") && name.startsWith("OK") && !errors.isEmpty()) {
                    fail(name + "has unexpected error(s) ");
                }
            }
        }
    }
}
