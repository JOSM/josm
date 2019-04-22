// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.io.UploadDialog.UploadAction;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.Invocation;
import mockit.Mock;

/**
 * Unit tests of {@link UploadDialog} class.
 */
public class UploadDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    private static class MockUploadDialog extends JOptionPane implements IUploadDialog {
        private final String source;
        private final String comment;

        public int handleMissingCommentCalls;
        public int handleMissingSourceCalls;

        MockUploadDialog(final String comment, final String source) {
            this.source = source;
            this.comment = comment;
        }

        @Override
        public void rememberUserInput() {
            // Do nothing
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void handleMissingSource() {
            this.handleMissingSourceCalls += 1;
        }

        @Override
        public void handleMissingComment() {
            this.handleMissingCommentCalls += 1;
        }

        @Override
        public void handleIllegalChunkSize() {
            // Do nothing
        }

        @Override
        public UploadStrategySpecification getUploadStrategySpecification() {
            return new UploadStrategySpecification();
        }

        @Override
        public String getUploadSource() {
            return source;
        }

        @Override
        public String getUploadComment() {
            return comment;
        }

        @Override
        public Map<String, String> getTags(boolean keepEmpty) {
            return new ConcurrentHashMap<>();
        }

        @Override
        public void forceUpdateActiveField() {
            // Do nothing
        }
    }

    /**
     * Test of {@link UploadDialog.CancelAction} class.
     */
    @Test
    public void testCancelAction() {
        if (GraphicsEnvironment.isHeadless()) {
            TestUtils.assumeWorkingJMockit();
            new WindowMocker();
        }
        MockUploadDialog uploadDialog = new MockUploadDialog(null, null);
        new UploadDialog.CancelAction(uploadDialog).actionPerformed(null);
    }

    /**
     * Test of {@link UploadDialog.UploadAction} class.
     */
    @Test
    public void testUploadAction() {
        TestUtils.assumeWorkingJMockit();
        ExtendedDialogMocker edMocker = new ExtendedDialogMocker(
            ImmutableMap.<String, Object>of(
                "<html>Your upload comment is <i>empty</i>, or <i>very short</i>.<br /><br />This is "
                + "technically allowed, but please consider that many users who are<br />watching changes "
                + "in their area depend on meaningful changeset comments<br />to understand what is going "
                + "on!<br /><br />If you spend a minute now to explain your change, you will make life<br />"
                + "easier for many other mappers.</html>", "Revise",
                "<html>You did not specify a source for your changes.<br />It is technically allowed, "
                + "but this information helps<br />other users to understand the origins of the data."
                + "<br /><br />If you spend a minute now to explain your change, you will make life"
                + "<br />easier for many other mappers.</html>", "Revise"
            )
        ) {
            @Mock
            void setupDialog(Invocation invocation) throws Exception {
                if (GraphicsEnvironment.isHeadless()) {
                    final int nButtons = ((String[]) TestUtils.getPrivateField(
                            ExtendedDialog.class, invocation.getInvokedInstance(), "bTexts")).length;
                    @SuppressWarnings("unchecked")
                    final List<JButton> buttons = (List<JButton>) TestUtils.getPrivateField(
                            ExtendedDialog.class, invocation.getInvokedInstance(), "buttons");

                    for (int i = 0; i < nButtons; i++) {
                        buttons.add(new JButton());
                    }
                } else {
                    invocation.proceed();
                }
            }
        };

        MockUploadDialog uploadDialog = new MockUploadDialog("comment", "source");
        new UploadDialog.UploadAction(uploadDialog).actionPerformed(null);

        assertEquals(1, uploadDialog.handleMissingCommentCalls);
        assertEquals(0, uploadDialog.handleMissingSourceCalls);
        assertEquals(1, edMocker.getInvocationLog().size());
        Object[] invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Please revise upload comment", invocationLogEntry[2]);
        edMocker.resetInvocationLog();

        uploadDialog = new MockUploadDialog("", "source");
        new UploadDialog.UploadAction(uploadDialog).actionPerformed(null);

        assertEquals(1, uploadDialog.handleMissingCommentCalls);
        assertEquals(0, uploadDialog.handleMissingSourceCalls);
        assertEquals(1, edMocker.getInvocationLog().size());
        invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Please revise upload comment", invocationLogEntry[2]);
        edMocker.resetInvocationLog();

        uploadDialog = new MockUploadDialog("comment", "");
        new UploadDialog.UploadAction(uploadDialog).actionPerformed(null);

        assertEquals(1, uploadDialog.handleMissingCommentCalls);
        assertEquals(0, uploadDialog.handleMissingSourceCalls);
        assertEquals(1, edMocker.getInvocationLog().size());
        invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Please revise upload comment", invocationLogEntry[2]);
        edMocker.resetInvocationLog();

        uploadDialog = new MockUploadDialog("a comment long enough", "");
        new UploadDialog.UploadAction(uploadDialog).actionPerformed(null);

        assertEquals(0, uploadDialog.handleMissingCommentCalls);
        assertEquals(1, uploadDialog.handleMissingSourceCalls);
        assertEquals(1, edMocker.getInvocationLog().size());
        invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Please specify a changeset source", invocationLogEntry[2]);
        edMocker.resetInvocationLog();

        uploadDialog = new MockUploadDialog("a comment long enough", "a source long enough");
        new UploadDialog.UploadAction(uploadDialog).actionPerformed(null);

        assertEquals(0, uploadDialog.handleMissingCommentCalls);
        assertEquals(0, uploadDialog.handleMissingSourceCalls);
        assertEquals(0, edMocker.getInvocationLog().size());
    }

    /**
     * Test of {@link UploadDialog.UploadAction#isUploadCommentTooShort} method.
     */
    @Test
    public void testIsUploadCommentTooShort() {
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort(""));
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort("test"));
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort("测试"));
        assertFalse(UploadDialog.UploadAction.isUploadCommentTooShort("geometric corrections"));
        assertFalse(UploadDialog.UploadAction.isUploadCommentTooShort("几何校正"));
        // test with unassigned unicode characters ==> no unicode block
        assertTrue(UploadDialog.UploadAction.isUploadCommentTooShort("\u0860"));
    }

    private static void doTestGetLastChangesetTagFromHistory(String historyKey, Supplier<String> methodToTest, String def) {
        Config.getPref().putList(historyKey, null);
        Config.getPref().putInt(BasicUploadSettingsPanel.HISTORY_LAST_USED_KEY, 0);
        Config.getPref().putInt(BasicUploadSettingsPanel.HISTORY_MAX_AGE_KEY, 30);
        assertNull(methodToTest.get());          // age NOK (history empty)
        Config.getPref().putList(historyKey, Arrays.asList("foo"));
        assertNull(methodToTest.get());          // age NOK (history not empty)
        Config.getPref().putLong(BasicUploadSettingsPanel.HISTORY_LAST_USED_KEY, System.currentTimeMillis() / 1000);
        assertEquals("foo", methodToTest.get()); // age OK, history not empty
        Config.getPref().putList(historyKey, null);
        assertEquals(def, methodToTest.get());   // age OK, history empty
    }

    /**
     * Test of {@link UploadDialog#getLastChangesetCommentFromHistory} method.
     */
    @Test
    public void testGetLastChangesetCommentFromHistory() {
        doTestGetLastChangesetTagFromHistory(
                BasicUploadSettingsPanel.HISTORY_KEY,
                UploadDialog::getLastChangesetCommentFromHistory,
                null);
    }

    /**
     * Test of {@link UploadDialog#getLastChangesetSourceFromHistory} method.
     */
    @Test
    public void testGetLastChangesetSourceFromHistory() {
        doTestGetLastChangesetTagFromHistory(
                BasicUploadSettingsPanel.SOURCE_HISTORY_KEY,
                UploadDialog::getLastChangesetSourceFromHistory,
                BasicUploadSettingsPanel.getDefaultSources().get(0));
    }

    private static void doTestValidateUploadTag(String prefix) {
        List<String> def = Collections.emptyList();
        Config.getPref().putList(prefix + ".mandatory-terms", null);
        Config.getPref().putList(prefix + ".forbidden-terms", null);
        assertNull(UploadAction.validateUploadTag("foo", prefix, def, def));

        Config.getPref().putList(prefix + ".mandatory-terms", Arrays.asList("foo"));
        assertNull(UploadAction.validateUploadTag("foo", prefix, def, def));
        assertEquals("The following required terms are missing: [foo]",
                UploadAction.validateUploadTag("bar", prefix, def, def));

        Config.getPref().putList(prefix + ".forbidden-terms", Arrays.asList("bar"));
        assertNull(UploadAction.validateUploadTag("foo", prefix, def, def));
        assertEquals("The following forbidden terms have been found: [bar]",
                UploadAction.validateUploadTag("foobar", prefix, def, def));
        assertEquals("The following forbidden terms have been found: [bar]",
                UploadAction.validateUploadTag("FOOBAR", prefix, def, def));
    }

    /**
     * Test of {@link UploadDialog.UploadAction#validateUploadTag} method.
     */
    @Test
    public void testValidateUploadTag() {
        doTestValidateUploadTag("upload.comment");
        doTestValidateUploadTag("upload.source");
    }
}
