// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.actions.ValidateAction;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;


/**
 * A management window for the validator's ignorelist
 * @author Taylor Smock
 * @since 14828
 */
public class ValidatorListManagementDialog extends ExtendedDialog {
    enum BUTTONS {
        OK(0, tr("OK"), new ImageProvider("ok")),
        CANCEL(1, tr("Cancel"), new ImageProvider("cancel"));

        private int index;
        private String name;
        private ImageIcon icon;

        BUTTONS(int index, String name, ImageProvider image) {
            this.index = index;
            this.name = name;
            Dimension dim = new Dimension();
            ImageIcon sizeto = new ImageProvider("ok").getResource().getImageIcon();
            dim.setSize(-1, sizeto.getIconHeight());
            this.icon = image.getResource().getImageIcon(dim);
        }

        public ImageIcon getImageIcon() {
            return icon;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }

    private static final String[] BUTTON_TEXTS = {BUTTONS.OK.getName(), BUTTONS.CANCEL.getName()};

    private static final ImageIcon[] BUTTON_IMAGES = {BUTTONS.OK.getImageIcon(), BUTTONS.CANCEL.getImageIcon()};

    private final JPanel panel = new JPanel(new GridBagLayout());

    private final JTree ignoreErrors;

    private final String type;

    /**
     * Create a new {@link ValidatorListManagementDialog}
     * @param type The type of list to create (first letter may or may not be
     * capitalized, it is put into all lowercase after building the title)
     */
    public ValidatorListManagementDialog(String type) {
        super(MainApplication.getMainFrame(), tr("Validator {0} List Management", type), BUTTON_TEXTS, false);
        this.type = type.toLowerCase(Locale.ENGLISH);
        setButtonIcons(BUTTON_IMAGES);

        ignoreErrors = buildList();
        JScrollPane scroll = GuiHelper.embedInVerticalScrollPane(ignoreErrors);

        panel.add(scroll, GBC.eol().fill(GBC.BOTH).anchor(GBC.CENTER));
        setContent(panel);
        setDefaultButton(1);
        setupDialog();
        setModal(true);
        showDialog();
    }

    @Override
    public void buttonAction(int buttonIndex, ActionEvent evt) {
        // Currently OK/Cancel buttons do nothing
        final int answer;
        if (buttonIndex == BUTTONS.OK.getIndex()) {
            Map<String, String> errors = OsmValidator.getIgnoredErrors();
            Map<String, String> tree = OsmValidator.buildIgnore(ignoreErrors);
            if (!errors.equals(tree)) {
                answer = rerunValidatorPrompt();
                if (answer == JOptionPane.YES_OPTION || answer == JOptionPane.NO_OPTION) {
                    OsmValidator.resetErrorList();
                    tree.forEach(OsmValidator::addIgnoredError);
                    OsmValidator.saveIgnoredErrors();
                    OsmValidator.initialize();
                }
            }
            dispose();
        } else {
            super.buttonAction(buttonIndex, evt);
        }
    }

    /**
     * Build a JTree with a list
     * @return &lt;type&gt;list as a {@code JTree}
     */
    public JTree buildList() {
        JTree tree;

        if ("ignore".equals(type)) {
            tree = OsmValidator.buildJTreeList();
        } else {
            Logging.error(tr("Cannot understand the following type: {0}", type));
            return null;
        }
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                process(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                process(e);
            }

            private void process(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath[] paths = tree.getSelectionPaths();
                    if (paths == null) return;
                    Rectangle bounds = tree.getUI().getPathBounds(tree, paths[0]);
                    if (bounds != null) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem delete = new JMenuItem(new AbstractAction(tr("Don''t ignore")) {
                            @Override
                            public void actionPerformed(ActionEvent e1) {
                                deleteAction(tree, paths);
                            }
                        });
                        menu.add(delete);
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        tree.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                // Do nothing
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // Do nothing
            }

            @Override
            public void keyReleased(KeyEvent e) {
                TreePath[] paths = tree.getSelectionPaths();
                if (e.getKeyCode() == KeyEvent.VK_DELETE && paths != null) {
                    deleteAction(tree, paths);
                }
            }
        });
        return tree;
    }

    private static void deleteAction(JTree tree, TreePath[] paths) {
        for (TreePath path : paths) {
            tree.clearSelection();
            tree.addSelectionPath(path);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            node.removeAllChildren();
            while (node.getChildCount() == 0) {
                node.removeFromParent();
                node = parent;
                if (parent == null || parent.isRoot()) break;
                parent = (DefaultMutableTreeNode) node.getParent();
            }
        }
        tree.updateUI();
    }


    /**
     * Prompt to rerun the validator when the ignore list changes
     * @return {@code JOptionPane.YES_OPTION}, {@code JOptionPane.NO_OPTION},
     *  or {@code JOptionPane.CANCEL_OPTION}
     */
    public int rerunValidatorPrompt() {
        MapFrame map = MainApplication.getMap();
        List<TestError> errors = map.validatorDialog.tree.getErrors();
        ValidateAction validateAction = ValidatorDialog.validateAction;
        if (!validateAction.isEnabled() || errors == null || errors.isEmpty()) return JOptionPane.NO_OPTION;
        final int answer = ConditionalOptionPaneUtil.showOptionDialog(
                "rerun_validation_when_ignorelist_changed",
                MainApplication.getMainFrame(),
                tr("{0}Should the validation be rerun?{1}", "<hmtl><h3>", "</h3></html>"),
                tr("Ignored error filter changed"),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                null);
        if (answer == JOptionPane.YES_OPTION) {
            validateAction.doValidate(true);
        }
        return answer;
    }
}
