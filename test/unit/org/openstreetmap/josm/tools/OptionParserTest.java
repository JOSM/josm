// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.OptionParser.OptionCount;
import org.openstreetmap.josm.tools.OptionParser.OptionParseException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test for {@link OptionParser}
 * @author Michael Zangl
 */
public class OptionParserTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().i18n();

    // A reason for moving to jupiter...
    @Test
    public void testEmptyParserRejectsLongopt() {
        Exception e = assertThrows(OptionParseException.class, () ->
                new OptionParser("test").parseOptions(Arrays.asList("--long")));
        assertEquals("test: unrecognized option '--long'", e.getMessage());
    }

    @Test
    public void testEmptyParserRejectsShortopt() {
        Exception e = assertThrows(OptionParseException.class, () ->
                new OptionParser("test").parseOptions(Arrays.asList("-s")));
        assertEquals("test: unrecognized option '-s'", e.getMessage());
    }

    @Test
    public void testParserRejectsWrongShortopt() {
        Exception e = assertThrows(OptionParseException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "t")
                .parseOptions(Arrays.asList("-s")));
        assertEquals("test: unrecognized option '-s'", e.getMessage());
    }

    @Test
    public void testParserRejectsWrongLongopt() {
        Exception e = assertThrows(OptionParseException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop).parseOptions(Arrays.asList("--wrong")));
        assertEquals("test: unrecognized option '--wrong'", e.getMessage());
    }

    @Test
    public void testParserOption() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test", "arg"));
        assertEquals("arg", argFound.get());
    }

    @Test
    public void testParserOptionFailsIfMissing() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test2", "arg")));
        assertEquals("test: unrecognized option '--test2'", e.getMessage());
    }

    @Test
    public void testParserOptionFailsIfMissingArgument() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test2", "--other")));
        assertEquals("test: unrecognized option '--test2'", e.getMessage());
    }

    @Test
    public void testParserOptionFailsIfMissing2() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--", "--test", "arg")));
        assertEquals("test: option '--test' is required", e.getMessage());
    }

    @Test
    public void testParserOptionFailsIfTwice() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test", "arg", "--test", "arg")));
        assertEquals("test: option '--test' may not appear multiple times", e.getMessage());
    }

    @Test
    public void testParserOptionFailsIfTwiceForAlias() {
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addShortAlias("test", "t");

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test", "arg", "-t", "arg")));
        assertEquals("test: option '-t' may not appear multiple times", e.getMessage());
    }

    @Test
    public void testOptionalOptionFailsIfTwice() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop);
        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test", "--test")));
        assertEquals("test: option '--test' may not appear multiple times", e.getMessage());
    }

    @Test
    public void testOptionalOptionFailsIfTwiceForAlias() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop)
                .addShortAlias("test", "t");
        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("-t", "-t")));
        assertEquals("test: option '-t' may not appear multiple times", e.getMessage());
    }

    @Test
    public void testOptionalOptionFailsIfTwiceForAlias2() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop)
                .addShortAlias("test", "t");
        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("-tt")));
        assertEquals("test: option '-t' may not appear multiple times", e.getMessage());
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

    @Test
    public void testLongArgumentsMissingOption() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop);

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test")));
        assertEquals("test: option '--test' requires an argument", e.getMessage());
    }

    @Test
    public void testLongArgumentsMissingOption2() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop);

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test", "--", "x")));
        assertEquals("test: option '--test' requires an argument", e.getMessage());
    }

    @Test
    public void testShortArgumentsMissingOption() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop)
                .addShortAlias("test", "t");

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("-t")));
        assertEquals("test: option '-t' requires an argument", e.getMessage());
    }

    @Test
    public void testShortArgumentsMissingOption2() {
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop)
                .addShortAlias("test", "t");

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("-t", "--", "x")));
        assertEquals("test: option '-t' requires an argument", e.getMessage());
    }

    @Test
    public void testLongFlagHasOption() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop);

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--test=arg")));
        assertEquals("test: option '--test' does not allow an argument", e.getMessage());
    }

    @Test
    public void testShortFlagHasOption() {
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop)
                .addShortAlias("test", "t");

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("-t=arg")));
        assertEquals("test: option '-t' does not allow an argument", e.getMessage());
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

    @Test
    public void testAmbiguousAlternatives() {
        AtomicReference<String> argFound = new AtomicReference<>();
        AtomicBoolean usedFlag = new AtomicBoolean();
        AtomicBoolean unusedFlag = new AtomicBoolean();

        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addFlagParameter("flag", () -> usedFlag.set(true))
                .addFlagParameter("fleg", () -> unusedFlag.set(true));

        Exception e = assertThrows(OptionParseException.class, () -> parser.parseOptions(Arrays.asList("--te=arg", "--fl")));
        assertEquals("test: option '--fl' is ambiguous", e.getMessage());
    }

    @Test
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

        List<String> remaining = parser.parseOptions(Arrays.asList("-ft", "arg", "x"));

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

    @Test
    public void testIllegalOptionName() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("", this::nop));
        assertEquals("Illegal option name: ''", e.getMessage());
    }

    @Test
    public void testIllegalOptionName2() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("-", this::nop));
        assertEquals("Illegal option name: '-'", e.getMessage());
    }

    @Test
    public void testIllegalOptionName3() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("-test", this::nop));
        assertEquals("Illegal option name: '-test'", e.getMessage());
    }

    @Test
    public void testIllegalOptionName4() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("$", this::nop));
        assertEquals("Illegal option name: '$'", e.getMessage());
    }

    @Test
    public void testDuplicateOptionName() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop).addFlagParameter("test", this::nop));
        assertEquals("The option '--test' is already registered", e.getMessage());
    }

    @Test
    public void testDuplicateOptionName2() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop)
            .addArgumentParameter("test", OptionCount.OPTIONAL, this::nop));
        assertEquals("The option '--test' is already registered", e.getMessage());
    }

    @Test
    public void testInvalidShortAlias() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "$"));
        assertEquals("Short name '$' must be one character", e.getMessage());
    }

    @Test
    public void testInvalidShortAlias2() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", ""));
        assertEquals("Short name '' must be one character", e.getMessage());
    }

    @Test
    public void testInvalidShortAlias3() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "xx"));
        assertEquals("Short name 'xx' must be one character", e.getMessage());
    }

    @Test
    public void testDuplicateShortAlias() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop)
        .addFlagParameter("test2", this::nop)
        .addShortAlias("test", "t")
        .addShortAlias("test2", "t"));
        assertEquals("Short name 't' is already used", e.getMessage());
    }

    @Test
    public void testInvalidShortNoLong() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test2", "t"));
        assertEquals("No long definition for test2 was defined. " +
                "Define the long definition first before creating a short definition for it.", e.getMessage());
    }

    private void nop() {
        // nop
    }

    private void nop(String arg) {
        // nop
    }
}
