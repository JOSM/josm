// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.google.common.io.ByteStreams;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link PluginDownloadTask}.
 */
public class PluginDownloadTaskTest extends AbstractDownloadTaskTestParent {
    protected String pluginPath;

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().assumeRevision(
        "Revision: 8000\n"
    ).preferences();

    @Override
    protected String getRemoteContentType() {
        return "application/java-archive";
    }

    @Override
    protected String getRemoteFile() {
        return this.pluginPath;
    }

    /**
     * Test download task when updating a plugin that we already have in our plugins directory
     * and the downloaded file is valid
     * @throws Exception if an error occurs
     */
    @Test
    public void testUpdatePluginValid() throws Exception {
        this.pluginPath = "plugin/dummy_plugin.v31772.jar";
        this.mockHttp();

        final File srcPluginFile = new File(
            new File(TestUtils.getTestDataRoot()), "__files/" + this.pluginPath
        );
        final File pluginDir = Preferences.main().getPluginsDirectory();
        final File pluginFile = new File(pluginDir, "dummy_plugin.jar");
        final File pluginFileNew = new File(pluginDir, "dummy_plugin.jar.new");

        // put existing "plugin file" in place
        pluginFile.getParentFile().mkdirs();
        final byte[] existingPluginContents = "Existing plugin contents 123".getBytes();
        try (FileOutputStream existingPluginOutputStream = new FileOutputStream(pluginFile)) {
            existingPluginOutputStream.write(existingPluginContents);
        }

        // get PluginInformation from jar file
        final PluginInformation pluginInformation = new PluginInformation(srcPluginFile, "dummy_plugin");
        // ...and grafting on the downloadlink
        pluginInformation.downloadlink = this.getRemoteFileUrl();

        final PluginDownloadTask pluginDownloadTask = new PluginDownloadTask(
            NullProgressMonitor.INSTANCE,
            Collections.singletonList(pluginInformation),
            null
        );
        pluginDownloadTask.run();

        // the ".jar.new" file should have been deleted
        assertFalse(pluginFileNew.exists());
        // the ".jar" file should still exist and its contents should equal those that were served to the task
        TestUtils.assertFileContentsEqual(pluginFile, srcPluginFile);
    }

    /**
     * Test download task when updating a plugin that we already have in our plugins directory
     * and the downloaded file is not a valid jar file.
     * @throws Exception if an error occurs
     */
    @Test
    public void testUpdatePluginCorrupt() throws Exception {
        this.pluginPath = "plugin/corrupted_plugin.jar";
        this.mockHttp();

        final File srcPluginFile = new File(
            new File(TestUtils.getTestDataRoot()), "__files/" + this.pluginPath
        );
        final File pluginDir = Preferences.main().getPluginsDirectory();
        final File pluginFile = new File(pluginDir, "corrupted_plugin.jar");
        final File pluginFileNew = new File(pluginDir, "corrupted_plugin.jar.new");
        // have to store this manifest externally as it clearly can't be read from our corrupted plugin
        final File pluginManifest = new File(TestUtils.getTestDataRoot(), "plugin/corrupted_plugin.MANIFEST.MF");

        pluginFile.getParentFile().mkdirs();
        final byte[] existingPluginContents = "Existing plugin contents 123".getBytes();
        try (FileOutputStream existingPluginOutputStream = new FileOutputStream(pluginFile)) {
            existingPluginOutputStream.write(existingPluginContents);
        }

        try (FileInputStream manifestInputStream = new FileInputStream(pluginManifest)) {
            final PluginInformation pluginInformation = new PluginInformation(
                manifestInputStream,
                "corrupted_plugin",
                this.getRemoteFileUrl()
            );
            final PluginDownloadTask pluginDownloadTask = new PluginDownloadTask(
                NullProgressMonitor.INSTANCE,
                Collections.singletonList(pluginInformation),
                null
            );
            pluginDownloadTask.run();
        }

        // assert that the "corrupt" jar file made it through in tact
        TestUtils.assertFileContentsEqual(pluginFileNew, srcPluginFile);
        // the ".jar" file should still exist
        assertTrue(pluginFile.exists());
        try (
            FileInputStream pluginDirPluginStream = new FileInputStream(pluginFile);
        ) {
            // the ".jar" file's contents should be as before
            assertArrayEquals(
                existingPluginContents,
                ByteStreams.toByteArray(pluginDirPluginStream)
            );
        }
    }
}
