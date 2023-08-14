// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.support.ReflectionSupport;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.io.NameFinder;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.BugReportMock;
import org.openstreetmap.josm.testutils.mockers.ImageProviderMock;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests of {@link PlaceSelection} class.
 */
@BasicPreferences
@WireMockTest
class PlaceSelectionTest {
    /**
     * Test for {@link PlaceSelection#PlaceSelection}.
     */
    @Test
    void testBookmarkSelection() {
        PlaceSelection sel = new PlaceSelection();
        sel.addGui(null);
        sel.setDownloadArea(null);
        sel.setDownloadArea(new Bounds(0, 0, 1, 1));
    }

    static Stream<Arguments> testTicket23117() {
        return Stream.of(Arguments.of(SAXParseException.class, (Supplier<Throwable>) () -> new SAXParseException("", "", "", 0, 0)),
                Arguments.of(IOException.class, (Supplier<Throwable>) IOException::new),
                Arguments.of(ParserConfigurationException.class, (Supplier<Throwable>) ParserConfigurationException::new));
    }

    /**
     * This checks to make certain that an exception thrown in parsing does not cause a {@link RuntimeException}.
     * @param expectedThrowable The expected throwable class, mostly used to indicate which throwable was under test
     * @param exceptionSupplier The supplier for the throwable
     * @param wireMockRuntimeInfo The wiremock to avoid network requests
     */
    @ParameterizedTest
    @MethodSource
    void testTicket23117(Class<? extends Throwable> expectedThrowable, Supplier<Throwable> exceptionSupplier,
                         WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        TestUtils.assumeWorkingJMockit();
        NameFinder.NOMINATIM_URL_PROP.put(wireMockRuntimeInfo.getHttpBaseUrl() + '/');
        // We just want to return something quickly so we can get to the place where we throw an exception in a mock.
        wireMockRuntimeInfo.getWireMock().register(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse()));
        new ImageProviderMock();
        final NameFinderMock nameFinderMock = new NameFinderMock(exceptionSupplier);
        final BugReportMock bugReportMock = new BugReportMock();
        final PlaceSelection placeSelection = new PlaceSelection();
        // We need to call addGui prior to buildSearchPanel -- we really want the panel, but it needs objects initialized
        // in addGui
        placeSelection.addGui(null);
        final JPanel searchPanel = placeSelection.buildSearchPanel();
        final HistoryComboBox cbSearchExpression = (HistoryComboBox) ((JPanel) searchPanel.getComponent(0)).getComponent(3);
        final PlaceSelection.SearchAction search = (PlaceSelection.SearchAction) ((JButton) searchPanel.getComponent(1)).getAction();
        final PlaceSelection.NamedResultTableModel model = (PlaceSelection.NamedResultTableModel)
                ReflectionSupport.tryToReadFieldValue(PlaceSelection.class.getDeclaredField("model"), placeSelection).get();

        // This is needed in order to enable the action
        cbSearchExpression.setText("Somewhere, Podunk");
        search.updateState();
        search.actionPerformed(null);
        // Sync threads
        final AtomicBoolean threadSync = new AtomicBoolean();
        MainApplication.worker.execute(() -> GuiHelper.runInEDTAndWaitWithException(() -> threadSync.set(true)));
        Awaitility.await().untilTrue(threadSync);
        model.addData(Collections.singletonList(new NameFinder.SearchResult()));
        assertEquals(1, nameFinderMock.calledTimes);
        // Call the search again -- this is needed since we need to be in the "Search more..." code paths
        search.updateState();
        search.actionPerformed(null);
        // Sync threads again
        threadSync.set(false);
        MainApplication.worker.execute(() -> GuiHelper.runInEDTAndWaitWithException(() -> threadSync.set(true)));
        Awaitility.await().untilTrue(threadSync);
        assertEquals(2, nameFinderMock.calledTimes);
        if (bugReportMock.throwable() != null) {
            assertInstanceOf(expectedThrowable, bugReportMock.throwable().getCause(),
                    "Some explanations will cause a bug report window. " +
                            "In those cases, the expected throwable should be the same class as the cause.");
        }
    }

    private static class NameFinderMock extends MockUp<NameFinder> {
        private final Supplier<Throwable> exceptionSupplier;
        int calledTimes;
        NameFinderMock(Supplier<Throwable> exceptionSupplier) {
            this.exceptionSupplier = exceptionSupplier;
        }

        @Mock
        public List<NameFinder.SearchResult> parseSearchResults(Reader reader) throws IOException, ParserConfigurationException, SAXException {
            this.calledTimes++;
            final Throwable throwable = this.exceptionSupplier.get();
            if (throwable instanceof IOException) {
                throw (IOException) throwable;
            } else if (throwable instanceof ParserConfigurationException) {
                throw (ParserConfigurationException) throwable;
            } else if (throwable instanceof SAXException) {
                throw (SAXException) throwable;
            }
            fail("This method does not throw the specified throwable", throwable);
            return Collections.emptyList();
        }
    }
}
