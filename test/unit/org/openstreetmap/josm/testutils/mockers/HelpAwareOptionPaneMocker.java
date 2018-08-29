// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import static org.junit.Assert.fail;

import java.awt.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.tools.Logging;

import mockit.Mock;

/**
 * MockUp for {@link HelpAwareOptionPane} allowing a test to pre-seed uses of
 * {@link HelpAwareOptionPane} with mock "responses". This works best with
 * calls to {@link HelpAwareOptionPane#showOptionDialog(Component, Object, String, int, Icon,
 * HelpAwareOptionPane.ButtonSpec[], HelpAwareOptionPane.ButtonSpec, String)} which use simple string-based
 * {@code msg} parameters. In such a case, responses can be defined through a mapping from content
 * {@link String}s to button indexes ({@link Integer}s) or button labels ({@link String}s). Example:
 *
 * <pre>
 *      new HelpAwareOptionPaneMocker(ImmutableMap.&lt;String, Object&gt;builder()
 *          .put("Do you want to save foo bar?", 2)
 *          .put("Please restart JOSM to activate the downloaded plugins.", "OK")
 *          .build()
 *      );
 * </pre>
 *
 * Testing examples with more complicated contents would require overriding
 * {@link #getStringFromMessage(Object)} or even {@link #getMockResultForMessage(Object)} with custom
 * logic. The class is implemented as a number of small methods with the main aim being to allow overriding
 * of only the parts necessary for a particular case.
 *
 * The default {@link #getMockResultForMessage(Object)} will raise an
 * {@link AssertionError} on an {@link #showOptionDialog(Component, Object, String,
 * int, Icon, HelpAwareOptionPane.ButtonSpec[], HelpAwareOptionPane.ButtonSpec, String)}
 * activation without a matching mapping entry or if the named button doesn't exist.
 *
 * The public {@link #getMockResultMap()} method returns the modifiable result map to allow for situations
 * where the desired result might need to be changed mid-test.
 */
public class HelpAwareOptionPaneMocker extends BaseDialogMockUp<HelpAwareOptionPane> {
    /**
     * Construct a {@link HelpAwareOptionPaneMocker} with an empty {@link #mockResultMap}.
     */
    public HelpAwareOptionPaneMocker() {
        this(null);
    }

    /**
     * Construct an {@link HelpAwareOptionPane} with the provided {@link #mockResultMap}.
     * @param mockResultMap mapping of {@link HelpAwareOptionPane} {@code msg} string to
     *      result button label or integer index.
     */
    public HelpAwareOptionPaneMocker(
        final Map<String, Object> mockResultMap
    ) {
        super(mockResultMap);
    }

    protected String getStringFromMessage(final Object message) {
        return message.toString();
    }

    protected Object getMockResultForMessage(final Object message) {
        final String messageString = this.getStringFromMessage(message);
        if (!this.getMockResultMap().containsKey(messageString)) {
            fail("Unexpected HelpAwareOptionPane message string: " + messageString);
        }
        return this.getMockResultMap().get(messageString);
    }

    protected int getButtonPositionFromLabel(
        final HelpAwareOptionPane.ButtonSpec[] options,
        final String label
    ) {
        if (options == null) {
            if (!label.equals("OK")) {
                fail(String.format(
                    "Only valid result for HelpAwareOptionPane with options = null is \"OK\": received %s",
                    label
                ));
            }
            return JOptionPane.OK_OPTION;
        } else {
            final OptionalInt optIndex = IntStream.range(0, options.length)
                .filter(i -> options[i].text.equals(label))
                .findFirst();
            if (!optIndex.isPresent()) {
                fail(String.format(
                    "Unable to find button labeled \"%s\". Instead found %s",
                    label,
                    Arrays.toString(Arrays.stream(options).map((buttonSpec) -> buttonSpec.text).toArray())
                ));
            }
            return optIndex.getAsInt();
        }
    }

    protected Object[] getInvocationLogEntry(
        final Object msg,
        final String title,
        final Integer messageType,
        final Icon icon,
        final HelpAwareOptionPane.ButtonSpec[] options,
        final HelpAwareOptionPane.ButtonSpec defaultOption,
        final String helpTopic,
        final Integer mockResult
    ) {
        return new Object[] {
            mockResult,
            this.getStringFromMessage(msg),
            title
        };
    }

    @Mock
    protected int showOptionDialog(
        final Component parentComponent,
        final Object msg,
        final String title,
        final int messageType,
        final Icon icon,
        final HelpAwareOptionPane.ButtonSpec[] options,
        final HelpAwareOptionPane.ButtonSpec defaultOption,
        final String helpTopic
    ) {
        try {
            final Object result = this.getMockResultForMessage(msg);

            if (result == null) {
                fail(
                    "Invalid result for HelpAwareOptionPane: null (HelpAwareOptionPane returns"
                    + "JOptionPane.OK_OPTION for closed windows if that was the intent)"
                );
            }

            Integer retval = null;
            if (result instanceof String) {
                retval = this.getButtonPositionFromLabel(options, (String) result);
            } else if (result instanceof Integer) {
                retval = (Integer) result;
            } else {
                throw new IllegalArgumentException(
                    "HelpAwareOptionPane message mapped to unsupported type of Object: " + result
                );
            }

            // check the returned integer for validity
            if (retval < 0) {
                fail(String.format(
                    "Invalid result for HelpAwareOptionPane: %s (HelpAwareOptionPane returns "
                    + "JOptionPane.OK_OPTION for closed windows if that was the intent)",
                    retval
                ));
            } else if (retval > (options == null ? 0 : options.length-1)) {
                fail(String.format(
                    "Invalid result for HelpAwareOptionPane: %s (in call with options = %s)",
                    retval,
                    Arrays.asList(options)
                ));
            }

            Logging.info(
                "{0} answering {1} to HelpAwareOptionPane with message {2}",
                this.getClass().getName(),
                retval,
                this.getStringFromMessage(msg)
            );

            this.getInvocationLogInternal().add(this.getInvocationLogEntry(
                msg,
                title,
                messageType,
                icon,
                options,
                defaultOption,
                helpTopic,
                retval
            ));

            return retval;
        } catch (AssertionError e) {
            // in case this exception gets ignored by the calling thread we want to signify this failure
            // in the invocation log. it's hard to know what to add to the log in these cases as it's
            // probably unsafe to call getInvocationLogEntry, so add the exception on its own.
            this.getInvocationLogInternal().add(new Object[] {e});
            throw e;
        }
    }
}
