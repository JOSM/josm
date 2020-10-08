// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.tools.LanguageInfo.LocaleType;
import org.openstreetmap.josm.tools.PlatformHook;
import org.openstreetmap.josm.tools.PlatformManager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;

/**
 * Unit tests of {@link HelpBrowser} class.
 */
class HelpBrowserTest {

    static final String URL_1 = "https://josm.openstreetmap.de/wiki/Help";
    static final String URL_2 = "https://josm.openstreetmap.de/wiki/Introduction";
    static final String URL_3 = "https://josm.openstreetmap.de/javadoc";

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    static JOSMTestRules test = new JOSMTestRules().preferences().https();

    static IHelpBrowser newHelpBrowser() {
        return new IHelpBrowser() {

            private final HelpBrowserHistory history = new HelpBrowserHistory(this);
            private String url;

            @Override
            public void openUrl(String url) {
                history.setCurrentUrl(url);
                this.url = url;
            }

            @Override
            public void openHelpTopic(String relativeHelpTopic) {
                openUrl(HelpUtil.getHelpTopicUrl(HelpUtil.buildAbsoluteHelpTopic(relativeHelpTopic, LocaleType.ENGLISH)));
            }

            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public void setUrl(String url) {
                this.url = url;
            }

            @Override
            public HelpBrowserHistory getHistory() {
                return history;
            }
        };
    }

    /**
     * Unit test of {@link HelpBrowser.BackAction} and {@link HelpBrowser.ForwardAction} classes.
     */
    @Test
    void testBackAndForwardActions() {
        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_1);
        assertEquals(URL_1, browser.getUrl());
        browser.openUrl(URL_2);
        assertEquals(URL_2, browser.getUrl());
        new HelpBrowser.BackAction(browser).actionPerformed(null);
        assertEquals(URL_1, browser.getUrl());
        new HelpBrowser.ForwardAction(browser).actionPerformed(null);
        assertEquals(URL_2, browser.getUrl());
    }

    /**
     * Unit test of {@link HelpBrowser.HomeAction} class.
     */
    @Test
    void testHomeAction() {
        IHelpBrowser browser = newHelpBrowser();
        assertNull(browser.getUrl());
        new HelpBrowser.HomeAction(browser).actionPerformed(null);
        assertEquals(URL_1, browser.getUrl());
    }

    /**
     * Unit test of {@link HelpBrowser.EditAction} class handling a null url.
     * @param mockPlatformHook platform hook mock
     * @param platformManager {@link PlatformManager} mock
     * @throws Exception  in case of error
     */
    @Test
    void testEditActionNull(@Injectable final PlatformHook mockPlatformHook, @Mocked final PlatformManager platformManager) throws Exception {
        TestUtils.assumeWorkingJMockit();
        new Expectations() {{
            PlatformManager.getPlatform(); result = mockPlatformHook; minTimes = 0;
            // should not be called
            mockPlatformHook.openUrl((String) any); times = 0;
        }};

        IHelpBrowser browser = newHelpBrowser();
        assertNull(browser.getUrl());
        new HelpBrowser.EditAction(browser).actionPerformed(null);
    }

    /**
     * Unit test of {@link HelpBrowser.EditAction} class handling an "internal" url.
     * @param mockPlatformHook platform hook mock
     * @param platformManager {@link PlatformManager} mock
     * @throws Exception  in case of error
     */
    @Test
    void testEditActionInternal(@Injectable final PlatformHook mockPlatformHook,
                                @Mocked final PlatformManager platformManager) throws Exception {
        TestUtils.assumeWorkingJMockit();
        new Expectations() {{
            PlatformManager.getPlatform(); result = mockPlatformHook;
            mockPlatformHook.openUrl(URL_2 + "?action=edit"); result = null; times = 1;
        }};

        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_2);
        assertEquals(URL_2, browser.getUrl());
        new HelpBrowser.EditAction(browser).actionPerformed(null);
    }

    /**
     * Unit test of {@link HelpBrowser.EditAction} class handling an "external" url.
     * @param mockPlatformHook platform hook mock
     * @param platformManager {@link PlatformManager} mock
     * @throws Exception  in case of error
     */
    @Test
    void testEditActionExternal(@Injectable final PlatformHook mockPlatformHook,
                                @Mocked final PlatformManager platformManager) throws Exception {
        TestUtils.assumeWorkingJMockit();
        new Expectations() {{
            PlatformManager.getPlatform(); result = mockPlatformHook; minTimes = 0;
            // should not be called
            mockPlatformHook.openUrl((String) any); times = 0;
        }};
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            Collections.singletonMap(
                "<html>The current URL <tt>https://josm.openstreetmap.de/javadoc</tt><br>is an external "
                + "URL. Editing is only possible for help topics<br>on the help server "
                + "<tt>https://josm.openstreetmap.de</tt>.</html>",
                JOptionPane.OK_OPTION
            )
        );

        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_3);
        assertEquals(URL_3, browser.getUrl());
        new HelpBrowser.EditAction(browser).actionPerformed(null);

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link HelpBrowser.OpenInBrowserAction} class.
     * @param mockPlatformHook platform hook moc
     * @param platformManager {@link PlatformManager} mock
     * @throws Exception  in case of error
     */
    @Test
    void testOpenInBrowserAction(@Injectable final PlatformHook mockPlatformHook,
                                 @Mocked final PlatformManager platformManager) throws Exception {
        TestUtils.assumeWorkingJMockit();
        new Expectations() {{
            PlatformManager.getPlatform(); result = mockPlatformHook;
            mockPlatformHook.openUrl(URL_1); result = null; times = 1;
        }};

        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_1);
        assertEquals(URL_1, browser.getUrl());
        new HelpBrowser.OpenInBrowserAction(browser).actionPerformed(null);
    }

    /**
     * Unit test of {@link HelpBrowser.ReloadAction} class.
     */
    @Test
    void testReloadAction() {
        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_1);
        assertEquals(URL_1, browser.getUrl());
        new HelpBrowser.ReloadAction(browser).actionPerformed(null);
        assertEquals(URL_1, browser.getUrl());
    }
}
