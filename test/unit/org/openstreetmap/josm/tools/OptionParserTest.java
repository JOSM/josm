// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.openstreetmap.josm.tools.OptionParser.OptionCount;
import org.openstreetmap.josm.tools.OptionParser.OptionParseException;

/**
 * Test for {@link OptionParser}
 * @author Michael Zangl
 */
public class OptionParserTest {

    // A reason for moving to jupiter...
    @Test(expected = OptionParseException.class)
    public void testEmptyParserRejectsLongopt() {
        new OptionParser("test").parseOptions(Arrays.asList("--long"));
    }

    @Test(expected = OptionParseException.class)
    public void testEmptyParserRejectsShortopt() {
        new OptionParser("test").parseOptions(Arrays.asList("-s"));
    }

    @Test(expected = OptionParseException.class)
    public void testParserRejectsWrongShortopt() {
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "t")
                .parseOptions(Arrays.asList("-s"));
    }

    @Test(expected = OptionParseException.class)
    public void testParserRejectsWrongLongopt() {
        new OptionParser("test").addFlagParameter("test", this::nop).parseOptions(Arrays.asList("--wrong"));
    }

    @Test
    public void testParserOption() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test", "arg"));
        assertEquals("arg", argFound.get());
    }

    @Test(expected = OptionParseException.class)
    public void testParserOptionFailsIfMissing() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test2", "arg"));
    }

    @Test(expected = OptionParseException.class)
    public void testParserOptionFailsIfMissingArgument() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test2", "--other"));
    }

    @Test(expected = OptionParseException.class)
    public void testParserOptionFailsIfMissing2() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--", "--test", "arg"));
    }

    @Test(expected = OptionParseException.class)
    public void testParserOptionFailsIfTwice() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test", "arg", "--test", "arg"));
    }

    @Test(expected = OptionParseException.class)
    public void testParserOptionFailsIfTwiceForAlias() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addShortAlias("test", "t");

        parser.parseOptions(Arrays.asList("--test", "arg", "-t", "arg"));
    }

    @Test(expected = OptionParseException.class)
    public void testOptionalOptionFailsIfTwice() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop);
        parser.parseOptions(Arrays.asList("--test", "--test"));
    }

    @Test(expected = OptionParseException.class)
    public void testOptionalOptionFailsIfTwiceForAlias() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop)
                .addShortAlias("test", "t");
        parser.parseOptions(Arrays.asList("-t", "-t"));
    }

    @Test(expected = OptionParseException.class)
    public void testOptionalOptionFailsIfTwiceForAlias2() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop)
                .addShortAlias("test", "t");
        parser.parseOptions(Arrays.asList("-tt"));
    }

    @Test
    public void testLongArgumentsUsingEqualSign() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        List<String> remaining = parser.parseOptions(Arrays.asList("--test=arg", "value"));

        assertEquals(Arrays.asList("value"), remaining);
        assertEquals("arg", argFound.get());

        remaining = parser.parseOptions(Arrays.asList("--test=", "value"));

        assertEquals(Arrays.asList("value"), remaining);
        assertEquals("", argFound.get());

        remaining = parser.parseOptions(Arrays.asList("--test=with space and=equals", "value"));

        assertEquals(Arrays.asList("value"), remaining);
        assertEquals("with space and=equals", argFound.get());
    }

    @Test(expected = OptionParseException.class)
    public void testLongArgumentsMissingOption() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop);

        parser.parseOptions(Arrays.asList("--test"));
    }

    @Test(expected = OptionParseException.class)
    public void testLongArgumentsMissingOption2() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop);

        parser.parseOptions(Arrays.asList("--test", "--", "x"));
    }

    @Test(expected = OptionParseException.class)
    public void testShortArgumentsMissingOption() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop)
                .addShortAlias("test", "t");

        parser.parseOptions(Arrays.asList("-t"));
    }

    @Test(expected = OptionParseException.class)
    public void testShortArgumentsMissingOption2() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop)
                .addShortAlias("test", "t");

        parser.parseOptions(Arrays.asList("-t", "--", "x"));
    }

    @Test(expected = OptionParseException.class)
    public void testLongFlagHasOption() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop);

        parser.parseOptions(Arrays.asList("--test=arg"));
    }

    @Test(expected = OptionParseException.class)
    public void testShortFlagHasOption() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop)
                .addShortAlias("test", "t");

        parser.parseOptions(Arrays.asList("-t=arg"));
    }

    @Test
    public void testShortArgumentsUsingEqualSign() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addShortAlias("test", "t");

        List<String> remaining = parser.parseOptions(Arrays.asList("-t=arg", "value"));

        assertEquals(Arrays.asList("value"), remaining);
        assertEquals("arg", argFound.get());
    }

    @Test
    public void testMultipleArguments() {
        AtomicReference<String> argFound = new AtomicReference<>();
        List<String> multiFound = new ArrayList<>();
        AtomicBoolean usedFlag = new AtomicBoolean();
        AtomicBoolean unusedFlag = new AtomicBoolean();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addShortAlias("test", "t")
                .addArgumentParameter("multi", OptionCount.MULTIPLE, multiFound::add)
                .addFlagParameter("flag", () -> usedFlag.set(true))
                .addFlagParameter("flag2", () -> unusedFlag.set(true));

        List<String> remaining = parser.parseOptions(Arrays.asList(
                "-t=arg", "--multi", "m1", "x1", "--flag", "--multi", "m2", "x2", "--", "x3", "--x4", "x5"));

        assertEquals(Arrays.asList("x1", "x2", "x3", "--x4", "x5"), remaining);
        assertEquals("arg", argFound.get());
        assertEquals(Arrays.asList("m1", "m2"), multiFound);
        assertTrue(usedFlag.get());
        assertFalse(unusedFlag.get());
    }

    @Test
    public void testUseAlternatives() {
        AtomicReference<String> argFound = new AtomicReference<>();
        AtomicBoolean usedFlag = new AtomicBoolean();
        AtomicBoolean unusedFlag = new AtomicBoolean();

        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addFlagParameter("flag", () -> usedFlag.set(true))
                .addFlagParameter("fleg", () -> unusedFlag.set(true));

        List<String> remaining = parser.parseOptions(Arrays.asList("--te=arg", "--fla"));

        assertEquals(Arrays.asList(), remaining);
        assertEquals("arg", argFound.get());
        assertTrue(usedFlag.get());
        assertFalse(unusedFlag.get());
    }

    @Test(expected = OptionParseException.class)
    public void testAmbiguousAlternatives() {
        AtomicReference<String> argFound = new AtomicReference<>();
        AtomicBoolean usedFlag = new AtomicBoolean();
        AtomicBoolean unusedFlag = new AtomicBoolean();

        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addFlagParameter("flag", () -> usedFlag.set(true))
                .addFlagParameter("fleg", () -> unusedFlag.set(true));

        parser.parseOptions(Arrays.asList("--te=arg", "--fl"));
    }

    @Test(expected = OptionParseException.class)
    public void testMultipleShort() {
        AtomicReference<String> argFound = new AtomicReference<>();
        AtomicBoolean usedFlag = new AtomicBoolean();
        AtomicBoolean unusedFlag = new AtomicBoolean();

        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addShortAlias("test", "t")
                .addFlagParameter("flag", () -> usedFlag.set(true))
                .addShortAlias("flag", "f")
                .addFlagParameter("fleg", () -> unusedFlag.set(true));

        List<String> remaining = parser.parseOptions(Arrays.asList("-ft=arg", "x"));

        assertEquals(Arrays.asList("x"), remaining);
        assertEquals("arg", argFound.get());
        assertTrue(usedFlag.get());
        assertFalse(unusedFlag.get());

        remaining = parser.parseOptions(Arrays.asList("-ft", "arg", "x"));

        assertEquals(Arrays.asList("x"), remaining);
        assertEquals("arg", argFound.get());
        assertTrue(usedFlag.get());
        assertFalse(unusedFlag.get());

        remaining = parser.parseOptions(Arrays.asList("-f", "-t=arg", "x"));

        assertEquals(Arrays.asList("x"), remaining);
        assertEquals("arg", argFound.get());
        assertTrue(usedFlag.get());
        assertFalse(unusedFlag.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalOptionName() {
        new OptionParser("test").addFlagParameter("", this::nop);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalOptionName2() {
        new OptionParser("test").addFlagParameter("-", this::nop);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalOptionName3() {
        new OptionParser("test").addFlagParameter("-test", this::nop);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalOptionName4() {
        new OptionParser("test").addFlagParameter("$", this::nop);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateOptionName() {
        new OptionParser("test").addFlagParameter("test", this::nop).addFlagParameter("test", this::nop);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateOptionName2() {
        new OptionParser("test").addFlagParameter("test", this::nop)
            .addArgumentParameter("test", OptionCount.OPTIONAL, this::nop);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidShortAlias() {
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "$");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidShortAlias2() {
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidShortAlias3() {
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "xx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateShortAlias() {
        new OptionParser("test").addFlagParameter("test", this::nop)
        .addFlagParameter("test2", this::nop)
        .addShortAlias("test", "t")
        .addShortAlias("test2", "t");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidShortNoLong() {
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test2", "t");
    }

    private void nop() {
        // nop
    }

    private void nop(String arg) {
        // nop
    }
}
