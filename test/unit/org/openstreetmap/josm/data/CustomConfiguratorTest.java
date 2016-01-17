// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests for class {@link CustomConfigurator}.
 */
public class CustomConfiguratorTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test method for {@link CustomConfigurator#log}.
     */
    @Test
    public void testLog() {
        assertEquals("", CustomConfigurator.getLog());
        CustomConfigurator.log("test");
        assertEquals("test\n", CustomConfigurator.getLog());
        CustomConfigurator.log("%d\n", 100);
        assertEquals("test\n100\n", CustomConfigurator.getLog());
        CustomConfigurator.log("test");
        assertEquals("test\n100\ntest\n", CustomConfigurator.getLog());
    }

    /**
     * Test method for {@link CustomConfigurator#exportPreferencesKeysToFile}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testExportPreferencesKeysToFile() throws IOException {
        File tmp = File.createTempFile("josm.testExportPreferencesKeysToFile.lorem_ipsum", ".xml");

        Main.pref.putCollection("lorem_ipsum", Arrays.asList(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                "Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor.",
                "Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi.",
                "Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat.",
                "Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim.",
                "Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue.",
                "Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales.",
                "Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh.",
                "Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit."));
        CustomConfigurator.exportPreferencesKeysToFile(tmp.getAbsolutePath(), false, "lorem_ipsum");
        String xml = Utils.join("\n", Files.readAllLines(tmp.toPath(), StandardCharsets.UTF_8));
        assertTrue(xml.contains("<preferences operation=\"replace\">"));
        for (String entry : Main.pref.getCollection("lorem_ipsum")) {
            assertTrue(entry + "\nnot found in:\n" + xml, xml.contains(entry));
        }

        Main.pref.putCollection("test", Arrays.asList("11111111", "2222222", "333333333"));
        CustomConfigurator.exportPreferencesKeysByPatternToFile(tmp.getAbsolutePath(), true, "test");
        xml = Utils.join("\n", Files.readAllLines(tmp.toPath(), StandardCharsets.UTF_8));
        assertTrue(xml.contains("<preferences operation=\"append\">"));
        for (String entry : Main.pref.getCollection("test")) {
            assertTrue(entry + "\nnot found in:\n" + xml, xml.contains(entry));
        }

        Utils.deleteFile(tmp);
    }
}
