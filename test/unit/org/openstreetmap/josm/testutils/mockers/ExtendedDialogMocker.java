// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import static org.junit.Assert.fail;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.Logging;

import mockit.Deencapsulation;
import mockit.Invocation;
import mockit.Mock;
import mockit.internal.reflection.FieldReflection;

/**
 * MockUp for {@link ExtendedDialog} allowing a test to pre-seed uses of {@link ExtendedDialog}
 * with mock "responses". This works best with {@link ExtendedDialog}s which have their contents set
 * through {@link ExtendedDialog#setContent(String)} as simple strings. In such a case, responses can
 * be defined through a mapping from content {@link String}s to button indexes ({@link Integer}s) or
 * button labels ({@link String}s). Example:
 *
 * <pre>
 *      new ExtendedDialogMocker(ImmutableMap.&lt;String, Object&gt;builder()
 *          .put("JOSM version 8,001 required for plugin baz_plugin.", "Download Plugin")
 *          .put("JOSM version 7,001 required for plugin dummy_plugin.", "Cancel")
 *          .put("Are you sure you want to do foo bar?", ExtendedDialog.DialogClosedOtherwise)
 *          .build()
 *      );
 * </pre>
 *
 * Testing examples with more complicated contents would require overriding
 * {@link #getString(ExtendedDialog)} or even {@link #getMockResult(ExtendedDialog)} with custom logic.
 * The class is implemented as a number of small methods with the main aim being to allow overriding of
 * only the parts necessary for a particular case.
 *
 * The default {@link #getMockResult(ExtendedDialog)} will raise an
 * {@link AssertionError} on an {@link ExtendedDialog} activation without a
 * matching mapping entry or if the named button doesn't exist.
 *
 * The public {@link #getMockResultMap()} method returns the modifiable result map to allow for situations
 * where the desired result might need to be changed mid-test.
 */
public class ExtendedDialogMocker extends BaseDialogMockUp<ExtendedDialog> {
    /**
     * Because we're unable to add fields to the mocked class, we need to use this external global
     * mapping to be able to keep a note of the most recently set simple String contents of each
     * {@link ExtendedDialog} instance - {@link ExtendedDialog} doesn't store this information
     * itself, instead converting it directly into the embedded {@link Component}.
     */
    protected final Map<ExtendedDialog, String> simpleStringContentMemo = new WeakHashMap<>();

    /**
     * Construct an {@link ExtendedDialogMocker} with an empty {@link #mockResultMap}.
     */
    public ExtendedDialogMocker() {
        this(null);
    }

    /**
     * Construct an {@link ExtendedDialogMocker} with the provided {@link #mockResultMap}.
     * @param mockResultMap mapping of {@link ExtendedDialog} string contents to
     *      result button label or integer index.
     */
    public ExtendedDialogMocker(final Map<String, Object> mockResultMap) {
        super(mockResultMap);
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
    }

    protected int getButtonPositionFromLabel(final ExtendedDialog instance, final String label) {
        final String[] bTexts = Deencapsulation.getField(instance, "bTexts");
        final int position = Arrays.asList(bTexts).indexOf(label);
        if (position == -1) {
            fail("Unable to find button labeled \"" + label + "\". Instead found: " + Arrays.toString(bTexts));
        }
        return position;
    }

    protected String getString(final ExtendedDialog instance) {
        return Optional.ofNullable(this.simpleStringContentMemo.get(instance))
            .orElseGet(() -> instance.toString());
    }

    protected int getMockResult(final ExtendedDialog instance) {
        final String stringContent = this.getString(instance);
        final Object result = this.getMockResultMap().get(stringContent);

        if (result == null) {
            fail(
                "Unexpected ExtendedDialog content: " + stringContent
            );
        } else if (result instanceof Integer) {
            return (Integer) result;
        } else if (result instanceof String) {
            // buttons are numbered with 1-based indexing
            return 1 + this.getButtonPositionFromLabel(instance, (String) result);
        }

        throw new IllegalArgumentException(
            "ExtendedDialog contents mapped to unsupported type of Object: " + result
        );
    }

    /**
     * Target for overriding, similar to {@link #getMockResult} except with the implication it will only
     * be invoked once per dialog display, therefore ideal opportunity to perform any mutating actions,
     * e.g. making a selection on a widget.
     * @param instance dialog instance
     */
    protected void act(final ExtendedDialog instance) {
        // Override in sub-classes
    }

    protected Object[] getInvocationLogEntry(final ExtendedDialog instance, final int mockResult) {
        return new Object[] {
            mockResult,
            this.getString(instance),
            instance.getTitle()
        };
    }

    /**
     * A convenience method to access {@link ExtendedDialog#content} without exception-catching boilerplate
     * @param instance dialog instance
     * @return dialog content component
     */
    protected Component getContent(final ExtendedDialog instance) {
        try {
            return (Component) TestUtils.getPrivateField(instance, "content");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private void setupDialog(final Invocation invocation) {
        if (!GraphicsEnvironment.isHeadless()) {
            invocation.proceed();
        }
        // else do nothing - WindowMocker-ed Windows doesn't work well enough for some of the
        // component constructions
    }

    @Mock
    private void setVisible(final Invocation invocation, final boolean value) {
        if (value == true) {
            try {
                final ExtendedDialog instance = invocation.getInvokedInstance();
                this.act(instance);
                final int mockResult = this.getMockResult(instance);
                // TODO check validity of mockResult?
                FieldReflection.setField(instance.getClass(), instance, "result", mockResult);
                Logging.info(
                    "{0} answering {1} to ExtendedDialog with content {2}",
                    this.getClass().getName(),
                    mockResult,
                    this.getString(instance)
                );
                this.getInvocationLogInternal().add(this.getInvocationLogEntry(instance, mockResult));
            } catch (AssertionError e) {
                // in case this exception gets ignored by the calling thread we want to signify this failure
                // in the invocation log. it's hard to know what to add to the log in these cases as it's
                // probably unsafe to call getInvocationLogEntry, so add the exception on its own.
                this.getInvocationLogInternal().add(new Object[] {e});
                throw e;
            }
        }
    }

    @Mock
    private ExtendedDialog setContent(final Invocation invocation, final String message) {
        final ExtendedDialog retval = invocation.proceed(message);
        // must set this *after* the regular invocation else that will fall through to
        // setContent(Component, boolean) which would overwrite it (with null)
        this.simpleStringContentMemo.put((ExtendedDialog) invocation.getInvokedInstance(), message);
        return retval;
    }

    @Mock
    private ExtendedDialog setContent(final Invocation invocation, final Component content, final boolean placeContentInScrollPane) {
        this.simpleStringContentMemo.put((ExtendedDialog) invocation.getInvokedInstance(), null);
        return invocation.proceed(content, placeContentInScrollPane);
    }
}
