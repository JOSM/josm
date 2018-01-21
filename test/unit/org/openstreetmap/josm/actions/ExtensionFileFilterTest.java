// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.ExtensionFileFilter.AddArchiveExtension;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link ExtensionFileFilter}.
 */
public class ExtensionFileFilterTest {

    private static void test(String extensions, String defaultExtension, String description, boolean addArchiveExtensionsToDescription,
            String expectedExtensions, String expectedDescription) {
        ExtensionFileFilter ext = ExtensionFileFilter.newFilterWithArchiveExtensions(
                extensions, defaultExtension, description, addArchiveExtensionsToDescription);
        assertEquals(expectedExtensions, ext.getExtensions());
        assertEquals(defaultExtension, ext.getDefaultExtension());
        assertEquals(expectedDescription, ext.getDescription());
    }

    /**
     * Unit test of method {@link ExtensionFileFilter#newFilterWithArchiveExtensions}.
     */
    @Test
    public void testNewFilterWithArchiveExtensions() {
        test("ext1", "ext1", "description", true,
                "ext1,ext1.gz,ext1.bz,ext1.bz2,ext1.xz,ext1.zip",
                "description (*.ext1, *.ext1.gz, *.ext1.bz, *.ext1.bz2, *.ext1.xz, *.ext1.zip)");
        test("ext1", "ext1", "description", false,
                "ext1,ext1.gz,ext1.bz,ext1.bz2,ext1.xz,ext1.zip", "description (*.ext1)");
        test("ext1,ext2", "ext1", "description", true,
                "ext1,ext1.gz,ext1.bz,ext1.bz2,ext1.xz,ext1.zip,ext2,ext2.gz,ext2.bz,ext2.bz2,ext2.xz,ext2.zip",
                "description (*.ext1, *.ext1.gz, *.ext1.bz, *.ext1.bz2, *.ext1.xz, *.ext1.zip, " +
                             "*.ext2, *.ext2.gz, *.ext2.bz, *.ext2.bz2, *.ext2.xz, *.ext2.zip)");
        test("ext1,ext2", "ext1", "description", false,
                "ext1,ext1.gz,ext1.bz,ext1.bz2,ext1.xz,ext1.zip,ext2,ext2.gz,ext2.bz,ext2.bz2,ext2.xz,ext2.zip",
                "description (*.ext1, *.ext2)");
    }

    /**
     * Unit test of methods {@link ExtensionFileFilter#equals} and {@link ExtensionFileFilter#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ExtensionFileFilter.class).usingGetClass()
            .verify();
    }

    /**
     * Unit test of {@link AddArchiveExtension} enum.
     */
    @Test
    public void testEnumAddArchiveExtension() {
        TestUtils.superficialEnumCodeCoverage(AddArchiveExtension.class);
    }
}
