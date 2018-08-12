// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * Action to rename an specific layer. Provides the option to rename the
 * file, this layer was loaded from as well (if it was loaded from a file).
 *
 * @author Imi
 */
public class RenameLayerAction extends AbstractAction {

    private final File file;
    private final transient Layer layer;

    /**
     * Constructs a new {@code RenameLayerAction}.
     * @param file The file of the original location of this layer.
     *      If null, no possibility to "rename the file as well" is provided.
     * @param layer layer to rename
     */
    public RenameLayerAction(File file, Layer layer) {
        super(tr("Rename layer"));
        new ImageProvider("dialogs", "edit").getResource().attachImageIcon(this, true);
        this.file = file;
        this.layer = layer;
        this.putValue("help", ht("/Action/RenameLayer"));
    }

    static class InitialValueOptionPane extends JOptionPane {
        InitialValueOptionPane(Box panel, JosmTextField initial) {
            super(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, null, initial);
        }

        @Override
        public void selectInitialValue() {
            JosmTextField initial = (JosmTextField) getInitialValue();
            initial.requestFocusInWindow();
            initial.selectAll();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Box panel = Box.createVerticalBox();
        final JosmTextField name = new JosmTextField(layer.getName());
        panel.add(name);
        JCheckBox filerename = new JCheckBox(tr("Also rename the file"));
        panel.add(filerename);
        filerename.setEnabled(file != null);
        if (filerename.isEnabled()) {
            filerename.setSelected(Config.getPref().getBoolean("layer.rename-file", true));
        }

        final JOptionPane optionPane = new InitialValueOptionPane(panel, name);
        final JDialog dlg = optionPane.createDialog(Main.parent, tr("Rename layer"));
        dlg.setModalityType(ModalityType.DOCUMENT_MODAL);
        dlg.setVisible(true);

        Object answer = optionPane.getValue();
        if (answer == null || answer == JOptionPane.UNINITIALIZED_VALUE ||
                (answer instanceof Integer && (Integer) answer != JOptionPane.OK_OPTION))
            return;

        String nameText = name.getText();
        if (filerename.isEnabled()) {
            Config.getPref().putBoolean("layer.rename-file", filerename.isSelected());
            if (filerename.isSelected()) {
                String newname = nameText;
                if (newname.indexOf('/') == -1 && newname.indexOf('\\') == -1) {
                    newname = file.getParent() + File.separator + newname;
                }
                String oldname = file.getName();
                if (name.getText().indexOf('.') == -1 && oldname.indexOf('.') >= 0) {
                    newname += oldname.substring(oldname.lastIndexOf('.'));
                }
                File newFile = new File(newname);
                if (!SaveActionBase.confirmOverwrite(newFile))
                    return;
                if (PlatformManager.getPlatform().rename(file, newFile)) {
                    layer.setAssociatedFile(newFile);
                    if (!layer.isRenamed()) {
                        nameText = newFile.getName();
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Could not rename file ''{0}''", file.getPath()),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            }
        }
        layer.rename(nameText);
        Main.parent.repaint();
    }
}
