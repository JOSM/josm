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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.OptionParser.OptionCount;
import org.openstreetmap.josm.tools.OptionParser.OptionParseException;

/**
 * Test for {@link OptionParser}
 * @author Michael Zangl
 */
public class OptionParserTest {

    /**
     * Rule used for tests throwing exceptions.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().i18n();

    // A reason for moving to jupiter...
    @Test
    public void testEmptyParserRejectsLongopt() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: unrecognized option '--long'");
        new OptionParser("test").parseOptions(Arrays.asList("--long"));
    }

    @Test
    public void testEmptyParserRejectsShortopt() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: unrecognized option '-s'");
        new OptionParser("test").parseOptions(Arrays.asList("-s"));
    }

    @Test
    public void testParserRejectsWrongShortopt() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: unrecognized option '-s'");
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "t")
                .parseOptions(Arrays.asList("-s"));
    }

    @Test
    public void testParserRejectsWrongLongopt() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: unrecognized option '--wrong'");
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

    @Test
    public void testParserOptionFailsIfMissing() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: unrecognized option '--test2'");
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test2", "arg"));
    }

    @Test
    public void testParserOptionFailsIfMissingArgument() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: unrecognized option '--test2'");
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test2", "--other"));
    }

    @Test
    public void testParserOptionFailsIfMissing2() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '--test' is required");
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--", "--test", "arg"));
    }

    @Test
    public void testParserOptionFailsIfTwice() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '--test' may not appear multiple times");
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set);

        parser.parseOptions(Arrays.asList("--test", "arg", "--test", "arg"));
    }

    @Test
    public void testParserOptionFailsIfTwiceForAlias() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '-t' may not appear multiple times");
        AtomicReference<String> argFound = new AtomicReference<>();
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addShortAlias("test", "t");

        parser.parseOptions(Arrays.asList("--test", "arg", "-t", "arg"));
    }

    @Test
    public void testOptionalOptionFailsIfTwice() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '--test' may not appear multiple times");
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop);
        parser.parseOptions(Arrays.asList("--test", "--test"));
    }

    @Test
    public void testOptionalOptionFailsIfTwiceForAlias() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '-t' may not appear multiple times");
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop)
                .addShortAlias("test", "t");
        parser.parseOptions(Arrays.asList("-t", "-t"));
    }

    @Test
    public void testOptionalOptionFailsIfTwiceForAlias2() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '-t' may not appear multiple times");
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

    @Test
    public void testLongArgumentsMissingOption() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '--test' requires an argument");
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop);

        parser.parseOptions(Arrays.asList("--test"));
    }

    @Test
    public void testLongArgumentsMissingOption2() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '--test' requires an argument");
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop);

        parser.parseOptions(Arrays.asList("--test", "--", "x"));
    }

    @Test
    public void testShortArgumentsMissingOption() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '-t' requires an argument");
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop)
                .addShortAlias("test", "t");

        parser.parseOptions(Arrays.asList("-t"));
    }

    @Test
    public void testShortArgumentsMissingOption2() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '-t' requires an argument");
        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, this::nop)
                .addShortAlias("test", "t");

        parser.parseOptions(Arrays.asList("-t", "--", "x"));
    }

    @Test
    public void testLongFlagHasOption() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '--test' does not allow an argument");
        OptionParser parser = new OptionParser("test")
                .addFlagParameter("test", this::nop);

        parser.parseOptions(Arrays.asList("--test=arg"));
    }

    @Test
    public void testShortFlagHasOption() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '-t' does not allow an argument");
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

    @Test
    public void testAmbiguousAlternatives() {
        thrown.expect(OptionParseException.class);
        thrown.expectMessage("test: option '--fl' is ambiguous");
        AtomicReference<String> argFound = new AtomicReference<>();
        AtomicBoolean usedFlag = new AtomicBoolean();
        AtomicBoolean unusedFlag = new AtomicBoolean();

        OptionParser parser = new OptionParser("test")
                .addArgumentParameter("test", OptionCount.REQUIRED, argFound::set)
                .addFlagParameter("flag", () -> usedFlag.set(true))
                .addFlagParameter("fleg", () -> unusedFlag.set(true));

        parser.parseOptions(Arrays.asList("--te=arg", "--fl"));
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
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Illegal option name: ''");
        new OptionParser("test").addFlagParameter("", this::nop);
    }

    @Test
    public void testIllegalOptionName2() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Illegal option name: '-'");
        new OptionParser("test").addFlagParameter("-", this::nop);
    }

    @Test
    public void testIllegalOptionName3() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Illegal option name: '-test'");
        new OptionParser("test").addFlagParameter("-test", this::nop);
    }

    @Test
    public void testIllegalOptionName4() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Illegal option name: '$'");
        new OptionParser("test").addFlagParameter("$", this::nop);
    }

    @Test
    public void testDuplicateOptionName() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The option '--test' is already registered");
        new OptionParser("test").addFlagParameter("test", this::nop).addFlagParameter("test", this::nop);
    }

    @Test
    public void testDuplicateOptionName2() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The option '--test' is already registered");
        new OptionParser("test").addFlagParameter("test", this::nop)
            .addArgumentParameter("test", OptionCount.OPTIONAL, this::nop);
    }

    @Test
    public void testInvalidShortAlias() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Short name '$' must be one character");
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "$");
    }

    @Test
    public void testInvalidShortAlias2() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Short name '' must be one character");
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "");
    }

    @Test
    public void testInvalidShortAlias3() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Short name 'xx' must be one character");
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test", "xx");
    }

    @Test
    public void testDuplicateShortAlias() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Short name 't' is already used");
        new OptionParser("test").addFlagParameter("test", this::nop)
        .addFlagParameter("test2", this::nop)
        .addShortAlias("test", "t")
        .addShortAlias("test2", "t");
    }

    @Test
    public void testInvalidShortNoLong() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No long definition for test2 was defined. " +
                "Define the long definition first before creating a short definition for it.");
        new OptionParser("test").addFlagParameter("test", this::nop).addShortAlias("test2", "t");
    }

    private void nop() {
        // nop
    }

    private void nop(String arg) {
        // nop
    }
}
