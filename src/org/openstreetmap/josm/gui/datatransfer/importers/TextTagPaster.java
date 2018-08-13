// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.LanguageInfo.LocaleType;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.TextTagParser;
import org.openstreetmap.josm.tools.TextTagParser.TagWarningCallback;

/**
 * This transfer support allows us to import tags from the text that was copied to the clipboard.
 * @author Michael Zangl
 * @since 10604
 */
public final class TextTagPaster extends AbstractTagPaster {
    private static final String HELP = ht("/Action/PasteTags");

    /**
     * Create a new {@link TextTagPaster}
     */
    public TextTagPaster() {
        super(DataFlavor.stringFlavor);
    }

    @Override
    public boolean supports(TransferSupport support) {
        try {
            return super.supports(support) && containsValidTags(support);
        } catch (UnsupportedFlavorException | IOException e) {
            Logging.warn(e);
            return false;
        }
    }

    private boolean containsValidTags(TransferSupport support) throws UnsupportedFlavorException, IOException {
        return !getTagsImpl(support).isEmpty();
    }

    @Override
    protected Map<String, String> getTags(TransferSupport support) throws UnsupportedFlavorException, IOException {
        Map<String, String> tags = getTagsImpl(support);
        if (tags.isEmpty()) {
            showBadBufferMessage(HELP);
            throw new IOException("Invalid tags to paste.");
        }
        if (!TextTagParser.validateTags(tags, TextTagPaster::warning)) {
            throw new IOException("Tags to paste are not valid.");
        }
        return tags;
    }

    private Map<String, String> getTagsImpl(TransferSupport support) throws UnsupportedFlavorException, IOException {
        return TextTagParser.readTagsFromText((String) support.getTransferable().getTransferData(df));
    }

    /**
     * Default {@link TagWarningCallback} implementation.
     * Displays a warning about a problematic tag and ask user what to do about it.
     * @param text Message to display
     * @param data Tag key and/or value
     * @param code to use with {@code ExtendedDialog#toggleEnable(String)}
     * @return 1 to validate and display next warnings if any, 2 to cancel operation, 3 to clear buffer, 4 to paste tags
     * @since 12683
     */
    public static int warning(String text, String data, String code) {
        ExtendedDialog ed = new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    tr("Do you want to paste these tags?"),
                    tr("Ok"), tr("Cancel"), tr("Clear buffer"), tr("Ignore warnings"));
        ed.setButtonIcons("ok", "cancel", "dialogs/delete", "pastetags");
        ed.setContent("<html><b>"+text + "</b><br/><br/><div width=\"300px\">"+XmlWriter.encode(data, true)+"</html>");
        ed.setDefaultButton(2);
        ed.setCancelButton(2);
        ed.setIcon(JOptionPane.WARNING_MESSAGE);
        ed.toggleEnable(code);
        ed.showDialog();
        int r = ed.getValue();
        if (r == 0) r = 2;
        // clean clipboard if user asked
        if (r == 3) ClipboardUtils.copyString("");
        return r;
    }

    /**
     * Shows message that the buffer can not be pasted, allowing user to clean the buffer
     * @param helpTopic the help topic of the parent action
     * TODO: Replace by proper HelpAwareOptionPane instead of self-made help link
     */
    public static void showBadBufferMessage(String helpTopic) {
        String msg = tr("<html><p> Sorry, it is impossible to paste tags from buffer. It does not contain any JOSM object"
            + " or suitable text. </p></html>");
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(msg), GBC.eop());
        String helpUrl = HelpUtil.getHelpTopicUrl(HelpUtil.buildAbsoluteHelpTopic(helpTopic, LocaleType.DEFAULT));
        if (helpUrl != null) {
            p.add(new UrlLabel(helpUrl), GBC.eop());
        }

        ExtendedDialog ed = new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    tr("Warning"),
                    tr("Ok"), tr("Clear buffer"))
            .setButtonIcons("ok", "dialogs/delete")
            .setContent(p)
            .setDefaultButton(1)
            .setCancelButton(1)
            .setIcon(JOptionPane.WARNING_MESSAGE)
            .toggleEnable("tags.paste.cleanbadbuffer");

        ed.showDialog();

        // clean clipboard if user asked
        if (ed.getValue() == 2) ClipboardUtils.copyString("");
    }
}
