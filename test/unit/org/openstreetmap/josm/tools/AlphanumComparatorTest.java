// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AlphanumComparator}.
 */
class AlphanumComparatorTest {
    @AfterEach
    void teardown() {
        AlphanumComparator.useFastASCIISort = true;
    }

    /**
     * Test numeric strings.
     */
    @Test
    void testNumeric() {
        List<String> lst = Arrays.asList("1", "20", "-1", "00999", "100");
        lst.sort(AlphanumComparator.getInstance());
        assertEquals(Arrays.asList("-1", "1", "20", "100", "00999"), lst);
    }

    /**
     * Test mixed character strings.
     */
    @Test
    void testMixed() {
        List<String> lst = Arrays.asList("b1", "b20", "a5", "a00999", "a100");
        lst.sort(AlphanumComparator.getInstance());
        assertEquals(Arrays.asList("a5", "a100", "a00999", "b1", "b20"), lst);
    }

    private static Stream<String[]> testNonRegression23471Arguments() {
        List<String> testStrings = Arrays.asList(
                "AMEN",
                "Ameriabank",
                "America First Credit Union",
                "BAC Credomatic",
                "BADR Banque",
                "BAI",
                "Banca Popolare di Cividale",
                "Banca Popolare di Sondrio",
                "Banca Sella",
                "Banca Transilvania",
                "Bancaribe",
                "BancaStato",
                "Banco Agrario",
                "Banco AV Villas",
                "Banco Azteca",
                "Banco Bicentenario",
                "Banco BISA",
                "Banco BMG",
                "Banco BPI (Portugal)",
                "Banco BPM",
                "Banco Caja Social",
                "Banco Ciudad",
                "Banco Continental (Paraguay)",
                "Banco di Sardegna"
        );
        List<String> testChars = new ArrayList<>(AlphanumComparator.ASCII_SORT_ORDER.length());
        for (char c : AlphanumComparator.ASCII_SORT_ORDER.toCharArray()) {
            testChars.add(Character.toString(c));
        }
        BiFunction<List<String>, String, List<String>> subList = (list, string) -> list.subList(list.indexOf(string), list.size());
        return Stream.concat(
                testStrings.stream().flatMap(first -> subList.apply(testStrings, first).stream().map(second -> new String[]{first, second})),
                testChars.stream().flatMap(first -> subList.apply(testChars, first).stream().map(second -> new String[]{first, second}))
        );
    }

    /**
     * Non-regression test for #23471
     * This ensures that the comparison contract holds.
     * There are ~5300 combinations run in <1s (as of 2024-02-14).
     */
    @Test
    void testNonRegression23471() {
        assertAll(testNonRegression23471Arguments().map(strings -> () -> testNonRegression23471(strings[0], strings[1])));
    }

    private static void testNonRegression23471(String first, String second) {
        AlphanumComparator.useFastASCIISort = true;
        final AlphanumComparator instance = AlphanumComparator.getInstance();
        assertEquals(-instance.compare(first, second), instance.compare(second, first));
        // Ensure that the fast sort is equivalent to the slow sort
        AlphanumComparator.useFastASCIISort = false;
        final int slowFirstSecond = instance.compare(first, second);
        final int slowSecondFirst = instance.compare(second, first);
        AlphanumComparator.useFastASCIISort = true;
        final int fastFirstSecond = instance.compare(first, second);
        final int fastSecondFirst = instance.compare(second, first);
        assertEquals(slowFirstSecond, fastFirstSecond);
        assertEquals(slowSecondFirst, fastSecondFirst);

        final Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        // Check against the collator instance
        assertEquals(Utils.clamp(collator.compare(first, second), -1, 1),
                Utils.clamp(instance.compare(first, second), -1, 1));
        assertEquals(Utils.clamp(collator.compare(second, first), -1, 1),
                Utils.clamp(instance.compare(second, first), -1, 1));
    }

}
