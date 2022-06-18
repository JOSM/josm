// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

@BasicPreferences
class UploadTextComponentValidatorTest {
    /**
     * Unit test of {@link UploadTextComponentValidator.UploadCommentValidator}
     */
    @Test
    void testUploadCommentValidator() {
        JTextField textField = new JTextField();
        JLabel feedback = new JLabel();
        new UploadTextComponentValidator.UploadCommentValidator(textField, feedback);
        assertThat(feedback.getText(), containsString("Your upload comment is <i>empty</i>, or <i>very short</i>"));
        textField.setText("a comment long enough");
        assertThat(feedback.getText(), containsString("Thank you for providing a changeset comment"));
        textField.setText("a");
        assertThat(feedback.getText(), containsString("Your upload comment is <i>empty</i>, or <i>very short</i>"));
    }

    /**
     * Unit test of {@link UploadTextComponentValidator.UploadSourceValidator}
     */
    @Test
    void testUploadSourceValidator() {
        JTextField textField = new JTextField();
        JLabel feedback = new JLabel();
        new UploadTextComponentValidator.UploadSourceValidator(textField, feedback);
        assertThat(feedback.getText(), containsString("You did not specify a source for your changes"));
        textField.setText("a comment long enough");
        assertThat(feedback.getText(), containsString("Thank you for providing the data source"));
    }

    static Stream<Arguments> testUploadWithMandatoryTerm() {
        return Stream.of(Arguments.of("upload.comment.mandatory-terms", "Thank you for providing a changeset comment",
                    (BiFunction<JTextField, JLabel, ? extends UploadTextComponentValidator>)
                            UploadTextComponentValidator.UploadCommentValidator::new),
                Arguments.of("upload.source.mandatory-terms", "Thank you for providing the data source",
                    (BiFunction<JTextField, JLabel, ? extends UploadTextComponentValidator>)
                            UploadTextComponentValidator.UploadSourceValidator::new)
        );
    }

    /**
     * Unit test of {@link UploadTextComponentValidator.UploadCommentValidator} and
     * {@link UploadTextComponentValidator.UploadSourceValidator} with mandatory terms
     */
    @BasicPreferences
    @ParameterizedTest
    @MethodSource
    void testUploadWithMandatoryTerm(String confPref, String expectedText,
            BiFunction<JTextField, JLabel, ? extends UploadTextComponentValidator> validatorSupplier) {
        Config.getPref().putList(confPref, Arrays.asList("myrequired", "xyz"));
        JTextField textField = new JTextField("");
        JLabel feedback = new JLabel();

        validatorSupplier.apply(textField, feedback);

        // A too-short string should fail validation
        textField.setText("");
        assertThat(feedback.getText(), containsString("The following required terms are missing: [myrequired, xyz]"));

        // A long enough string without the mandatory terms should claim that the required terms are missing
        textField.setText("a string long enough but missing the mandatory term");
        assertThat(feedback.getText(), containsString("The following required terms are missing: [myrequired, xyz]"));

        // A valid string should pass
        textField.setText("a string long enough with the mandatory term #myrequired #xyz");
        assertThat(feedback.getText(), containsString(expectedText));
    }
}
