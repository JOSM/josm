// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link UploadDialogModel} class.
 */
@Main
class UploadDialogModelTest {
    /**
     * Test of {@link UploadDialogModel}.
     */
    @Test
    void testUploadDialogModel() {
        assertDoesNotThrow(UploadDialogModel::new);
    }

    @Test
    void testFindHashTags() {
        UploadDialogModel model = new UploadDialogModel();

        assertNull(model.findHashTags(" "));
        assertNull(model.findHashTags(" #"));
        assertNull(model.findHashTags(" # "));
        assertNull(model.findHashTags(" https://example.com/#map "));
        assertEquals("#59606086", model.findHashTags("#59606086"));
        assertEquals("#foo", model.findHashTags(" #foo "));
        assertEquals("#foo;#bar", model.findHashTags(" #foo #bar baz"));
        assertEquals("#foo;#bar", model.findHashTags(" #foo, #bar, baz"));
        assertEquals("#foo;#bar", model.findHashTags(" #foo; #bar; baz"));
        assertEquals("#hotosm-project-4773;#DRONEBIRD;#OsakaQuake2018;#AOYAMAVISION",
            model.findHashTags("#hotosm-project-4773 #DRONEBIRD #OsakaQuake2018 #AOYAMAVISION"));
    }

    @Test
    void testCommentWithHashtags() {
        UploadDialogModel model = new UploadDialogModel();
        model.add("comment", "comment with a #hashtag");
        assertEquals("#hashtag", model.getValue("hashtags"));
    }

    @Test
    void testGetCommentWithDataSetHashTag() {
        assertEquals("", UploadDialogModel.addHashTagsFromDataSet(null, null));
        DataSet ds = new DataSet();
        assertEquals("foo", UploadDialogModel.addHashTagsFromDataSet("foo", ds));
        ds.getChangeSetTags().put("hashtags", "bar");
        assertEquals("foo #bar", UploadDialogModel.addHashTagsFromDataSet("foo", ds));
        ds.getChangeSetTags().put("hashtags", "bar;baz;#bar");
        assertEquals("foo #bar #baz", UploadDialogModel.addHashTagsFromDataSet("foo", ds));
    }

    @Test
    void testNonRegression23153() {
        final DataSet dataSet = new DataSet();
        dataSet.getChangeSetTags().put("hashtags", "duplicate");
        final UploadDialogModel model = new UploadDialogModel();
        final String hashTags = model.findHashTags("#duplicate #duplicate hashtag");
        assertEquals("#duplicate", hashTags);
        final String commentHashtags = UploadDialogModel.addHashTagsFromDataSet("There should be no " + hashTags, dataSet);
        assertEquals("There should be no #duplicate", commentHashtags);
    }

}
