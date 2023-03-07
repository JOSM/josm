// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

/**
 * Test class for {@link ImageViewerDialog}
 */
class ImageViewerDialogTest {
    @RegisterExtension
    static JOSMTestRules rule = new JOSMTestRules().main().projection();

    @TempDir
    Path tempDirectory;

    private ImageViewerDialog dialog;
    @BeforeEach
    void setup() {
        this.dialog = ImageViewerDialog.getInstance();
        this.dialog.displayImages(null);
    }

    /**
     * Generate a number of image entries
     * @param numberOfEntries The number of image entries to generate
     * @return The generated entries
     * @throws IOException If a file could not be copied to a temp directory
     */
    protected List<? extends IImageEntry<?>> generateImageEntries(int numberOfEntries) throws IOException {
        final List<ImageEntry> images = new ArrayList<>(numberOfEntries);
        for (int i = 0; i < 5; i++) {
            // This copy is important -- we do *not* want to delete files in the test resources directory, and tests might perform file deletion
            Files.copy(new File(TestUtils.getRegressionDataFile(11685, "2015-11-08_15-33-27-Xiaomi_YI-Y0030832.jpg")).toPath(),
                    tempDirectory.resolve(i + ".jpg"));
            images.add(new ImageEntry(tempDirectory.resolve(i + ".jpg").toFile()));
        }
        MainApplication.getLayerManager().addLayer(new GeoImageLayer(new ArrayList<>(images), null, "testImageDeletion"));
        return images;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testImageDeletion(boolean permanentDeletion) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Config.getPref().putBoolean("message.geoimage.deleteimagefromdisk", false);
        Config.getPref().putInt("message.geoimage.deleteimagefromdisk.value", 2);
        new ExtendedDialogMocker(); // Just needed to avoid a headless exception.
        final Action deleteAction;
        if (permanentDeletion) {
            deleteAction = ((JButton) ((JPanel) ((JPanel) ((JPanel) this.dialog.getComponent(1)).getComponent(1))
                    .getComponent(0)).getComponent(9)).getAction();
            assertEquals("ImageRemoveFromDiskAction", deleteAction.getClass().getSimpleName());
        } else {
            deleteAction = ((JButton) ((JPanel) ((JPanel) ((JPanel) this.dialog.getComponent(1)).getComponent(1))
                    .getComponent(0)).getComponent(8)).getAction();
            assertEquals("ImageRemoveAction", deleteAction.getClass().getSimpleName());
        }
        assertNull(ImageViewerDialog.getCurrentImage());
        final List<? extends IImageEntry<?>> images = generateImageEntries(5);
        for (int i = 0; i < 5; i++) {
            assertSame(images.get(i), ImageViewerDialog.getCurrentImage());
            deleteAction.actionPerformed(null);
            assertNotEquals(permanentDeletion, images.get(i).getFile().exists());
            MainApplication.worker.submit(() -> { /* Sync thread */ }).get(1, TimeUnit.SECONDS);
        }
        assertNull(ImageViewerDialog.getCurrentImage());
    }
}
