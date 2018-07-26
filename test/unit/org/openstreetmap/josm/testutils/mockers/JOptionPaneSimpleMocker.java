// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import static org.junit.Assert.fail;

import java.awt.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil.MessagePanel;
import org.openstreetmap.josm.tools.Logging;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * MockUp for {@link JOptionPane} allowing a test to pre-seed uses of {@link JOptionPane}'s
 * {@code showInputDialog(...)}, {@code showMessageDialog(...)} and {@code showConfirmDialog(...)}
 * with mock "responses". This works best with calls which use simple string-based {@code message}
 * parameters. In such a case, responses can be defined through a mapping from content {@link String}s
 * to button integer codes ({@link Integer}s) in the case of {@code showConfirmDialog(...)} calls or
 * arbitrary Objects ( but probably {@link String}s) in the case of {@code showInputDialog(...)} calls.
 * {@code showMessageDialog(...)} calls' contents should be mapped to {@link JOptionPane#OK_OPTION}.
 * Example:
 *
 * <pre>
 *      new JOptionPaneSimpleMocker(ImmutableMap.of(
 *          "Number of tags to delete", "17",  // a showInputDialog(...) call
 *          "Please select the row to edit.", JOptionPane.OK_OPTION,  // a showMessageDialog(...) call
 *          "Do you want to save foo bar?", JOptionPane.CANCEL_OPTION  // a showConfirmDialog(...) call
 *      ));
 * </pre>
 *
 * Testing examples with more complicated contents would require overriding
 * {@link #getStringFromMessage(Object)} or even {@link #getMockResultForMessage(Object)} with custom logic.
 * The class is implemented as a number of small methods with the main aim being to allow overriding of
 * only the parts necessary for a particular case.
 *
 * The default {@link #getMockResultForMessage(Object)} will raise an
 * {@link junit.framework.AssertionFailedError} on an activation without a matching mapping entry or if
 * the mapped result value is invalid for the call.
 *
 * The public {@link #getMockResultMap()} method returns the modifiable result map to allow for situations
 * where the desired result might need to be changed mid-test.
 *
 * This class should also work with dialogs shown using
 * {@link org.openstreetmap.josm.gui.ConditionalOptionPaneUtil}.
 *
 * NOTE that this class does NOT handle {@code showOptionDialog(...)} calls or direct {@link JOptionPane}
 * instantiations. These are probably too flexible to be universally mocked with a "Simple" interface and
 * are probably best handled with case-specific mockers.
 */
public class JOptionPaneSimpleMocker extends BaseDialogMockUp<JOptionPane> {
    protected static final Map<Integer, int[]> optionTypePermittedResults = ImmutableMap.of(
        JOptionPane.YES_NO_OPTION, new int[] {
            JOptionPane.YES_OPTION,
            JOptionPane.NO_OPTION,
            JOptionPane.CLOSED_OPTION
        },
        JOptionPane.YES_NO_CANCEL_OPTION, new int[] {
            JOptionPane.YES_OPTION,
            JOptionPane.NO_OPTION,
            JOptionPane.CANCEL_OPTION,
            JOptionPane.CLOSED_OPTION
        },
        JOptionPane.OK_CANCEL_OPTION, new int[] {
            JOptionPane.OK_OPTION,
            JOptionPane.CANCEL_OPTION,
            JOptionPane.CLOSED_OPTION
        }
    );

    protected final MessagePanelMocker messagePanelMocker;

    /**
     * Construct a {@link JOptionPaneSimpleMocker} with an empty {@link #mockResultMap}.
     */
    public JOptionPaneSimpleMocker() {
        this(null);
    }

    /**
     * Construct an {@link JOptionPaneSimpleMocker} with the provided {@link #mockResultMap} and a
     * default {@link MessagePanelMocker}.
     * @param mockResultMap mapping of {@link JOptionPaneSimpleMocker} {@code message} string to
     *      result Object.
     */
    public JOptionPaneSimpleMocker(
        final Map<String, Object> mockResultMap
    ) {
        this(mockResultMap, null);
    }

    /**
     * Construct an {@link JOptionPaneSimpleMocker} with the provided {@link #mockResultMap} and the
     * provided {@link MessagePanelMocker} instance.
     * @param mockResultMap mapping of {@link JOptionPaneSimpleMocker} {@code message} string to
     *      result Object.
     * @param messagePanelMocker {@link MessagePanelMocker} instace to use for {@link org.openstreetmap.josm.gui.ConditionalOptionPaneUtil}
     *      message-string retrieval.
     */
    public JOptionPaneSimpleMocker(
        final Map<String, Object> mockResultMap,
        final MessagePanelMocker messagePanelMocker
    ) {
        super(mockResultMap);
        this.messagePanelMocker = messagePanelMocker != null ? messagePanelMocker : new MessagePanelMocker();
    }

    protected String getStringFromOriginalMessage(final Object originalMessage) {
        return originalMessage.toString();
    }

    protected String getStringFromMessage(final Object message) {
        final Object originalMessage = message instanceof MessagePanel ?
            this.messagePanelMocker.getOriginalMessage((MessagePanel) message) : message;
        return this.getStringFromOriginalMessage(originalMessage);
    }

    protected Object getMockResultForMessage(final Object message) {
        final String messageString = this.getStringFromMessage(message);
        if (!this.getMockResultMap().containsKey(messageString)) {
            fail("Unexpected JOptionPane message string: " + messageString);
        }
        return this.getMockResultMap().get(messageString);
    }

    protected Object[] getInvocationLogEntry(
        final Object message,
        final String title,
        final Integer optionType,
        final Integer messageType,
        final Icon icon,
        final Object[] selectionValues,
        final Object initialSelectionValue,
        final Object mockResult
    ) {
        return new Object[] {
            mockResult,
            this.getStringFromMessage(message),
            title
        };
    }

    @Mock
    protected Object showInputDialog(
        final Component parentComponent,
        final Object message,
        final String title,
        final int messageType,
        final Icon icon,
        final Object[] selectionValues,
        final Object initialSelectionValue
    ) {
        try {
            final Object result = this.getMockResultForMessage(message);
            if (selectionValues == null) {
                if (!(result instanceof String)) {
                    fail(String.format(
                        "Only valid result type for showInputDialog with null selectionValues is String: received %s",
                        result
                    ));
                }
            } else {
                if (!Arrays.asList(selectionValues).contains(result)) {
                    fail(String.format(
                        "Result for showInputDialog not present in selectionValues: %s",
                        result
                    ));
                }
            }

            Logging.info(
                "{0} answering {1} to showInputDialog with message {2}",
                this.getClass().getName(),
                result,
                this.getStringFromMessage(message)
            );

            this.getInvocationLogInternal().add(this.getInvocationLogEntry(
                message,
                title,
                null,
                messageType,
                icon,
                selectionValues,
                initialSelectionValue,
                result
            ));

            return result;
        } catch (AssertionError e) {
            // in case this exception gets ignored by the calling thread we want to signify this failure
            // in the invocation log. it's hard to know what to add to the log in these cases as it's
            // probably unsafe to call getInvocationLogEntry, so add the exception on its own.
            this.getInvocationLogInternal().add(new Object[] {e});
            throw e;
        }
    }

    @Mock
    protected void showMessageDialog(
        final Component parentComponent,
        final Object message,
        final String title,
        final int messageType,
        final Icon icon
    ) {
        try {
            // why look up a "result" for a message dialog which can only have one possible result? it's
            // a good opportunity to assert its contents
            final Object result = this.getMockResultForMessage(message);
            if (!(result instanceof Integer && (int) result == JOptionPane.OK_OPTION)) {
                fail(String.format(
                    "Only valid result for showMessageDialog is %d: received %s",
                    JOptionPane.OK_OPTION,
                    result
                ));
            }

            Logging.info(
                "{0} answering {1} to showMessageDialog with message {2}",
                this.getClass().getName(),
                result,
                this.getStringFromMessage(message)
            );

            this.getInvocationLogInternal().add(this.getInvocationLogEntry(
                message,
                title,
                null,
                messageType,
                icon,
                null,
                null,
                JOptionPane.OK_OPTION
            ));
        } catch (AssertionError e) {
            // in case this exception gets ignored by the calling thread we want to signify this failure
            // in the invocation log. it's hard to know what to add to the log in these cases as it's
            // probably unsafe to call getInvocationLogEntry, so add the exception on its own.
            this.getInvocationLogInternal().add(new Object[] {e});
            throw e;
        }
    }

    @Mock
    protected int showConfirmDialog(
        final Component parentComponent,
        final Object message,
        final String title,
        final int optionType,
        final int messageType,
        final Icon icon
    ) {
        try {
            final Object result = this.getMockResultForMessage(message);
            if (!(result instanceof Integer && Ints.contains(optionTypePermittedResults.get(optionType), (int) result))) {
                fail(String.format(
                    "Invalid result for showConfirmDialog with optionType %d: %s",
                    optionType,
                    result
                ));
            }

            Logging.info(
                "{0} answering {1} to showConfirmDialog with message {2}",
                this.getClass().getName(),
                result,
                this.getStringFromMessage(message)
            );

            this.getInvocationLogInternal().add(this.getInvocationLogEntry(
                message,
                title,
                optionType,
                messageType,
                icon,
                null,
                null,
                result
            ));

            return (int) result;
        } catch (AssertionError e) {
            // in case this exception gets ignored by the calling thread we want to signify this failure
            // in the invocation log. it's hard to know what to add to the log in these cases as it's
            // probably unsafe to call getInvocationLogEntry, so add the exception on its own.
            this.getInvocationLogInternal().add(new Object[] {e});
            throw e;
        }
    }

    /**
     * MockUp for {@link MessagePanel} to allow mocking to work with ConditionalOptionPaneUtil dialogs
     */
    public static class MessagePanelMocker extends MockUp<MessagePanel> {
        protected final Map<MessagePanel, Object> originalMessageMemo = new WeakHashMap<>();

        @Mock
        private void $init(
            final Invocation invocation,
            final Object message,
            final boolean displayImmediateOption
        ) {
            this.originalMessageMemo.put(
                (MessagePanel) invocation.getInvokedInstance(),
                message
            );
            invocation.proceed();
        }

        /**
         * Returns the original message.
         * @param instance message panel
         * @return the original message
         */
        public Object getOriginalMessage(final MessagePanel instance) {
            return this.originalMessageMemo.get(instance);
        }

        /* TODO also allow mocking of getNotShowAgain() */
    }
}
