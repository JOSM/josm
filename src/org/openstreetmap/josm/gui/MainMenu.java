// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AddNodeAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.CloseChangesetAction;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.CreateCircleAction;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.DistributeAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DownloadReferrersAction;
import org.openstreetmap.josm.actions.DuplicateAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.actions.HistoryInfoAction;
import org.openstreetmap.josm.actions.InfoAction;
import org.openstreetmap.josm.actions.JoinNodeWayAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.actions.MergeSelectionAction;
import org.openstreetmap.josm.actions.MirrorAction;
import org.openstreetmap.josm.actions.NewAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.OpenLocationAction;
import org.openstreetmap.josm.actions.OrthogonalizeAction;
import org.openstreetmap.josm.actions.PasteAction;
import org.openstreetmap.josm.actions.PasteTagsAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.ToggleGPXLinesAction;
import org.openstreetmap.josm.actions.UnGlueAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UnselectAllAction;
import org.openstreetmap.josm.actions.UpdateDataAction;
import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.actions.UploadSelectionAction;
import org.openstreetmap.josm.actions.WireframeToggleAction;
import org.openstreetmap.josm.actions.ZoomInAction;
import org.openstreetmap.josm.actions.ZoomOutAction;
import org.openstreetmap.josm.actions.audio.AudioBackAction;
import org.openstreetmap.josm.actions.audio.AudioFasterAction;
import org.openstreetmap.josm.actions.audio.AudioFwdAction;
import org.openstreetmap.josm.actions.audio.AudioNextAction;
import org.openstreetmap.josm.actions.audio.AudioPlayPauseAction;
import org.openstreetmap.josm.actions.audio.AudioPrevAction;
import org.openstreetmap.josm.actions.audio.AudioSlowerAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.Shortcut;
/**
 * This is the JOSM main menu bar. It is overwritten to initialize itself and provide all menu
 * entries as member variables (sort of collect them).
 *
 * It also provides possibilities to attach new menu entries (used by plugins).
 *
 * @author Immanuel.Scholz
 */
public class MainMenu extends JMenuBar {

    /* File menu */
    public final NewAction newAction = new NewAction();
    public final OpenFileAction openFile = new OpenFileAction();
    public final OpenLocationAction openLocation = new OpenLocationAction();
    public final JosmAction save = new SaveAction();
    public final JosmAction saveAs = new SaveAsAction();
    public final JosmAction gpxExport = new GpxExportAction();
    public final DownloadAction download = new DownloadAction();
    public final DownloadReferrersAction downloadReferrers = new DownloadReferrersAction();
    public final CloseChangesetAction closeChangesetAction = new CloseChangesetAction();
    public final JosmAction update = new UpdateDataAction();
    public final JosmAction updateSelection = new UpdateSelectionAction();
    public final JosmAction upload = new UploadAction();
    public final JosmAction uploadSelection = new UploadSelectionAction();
    public final JosmAction exit = new ExitAction();

    /* Edit menu */
    public final UndoAction undo = new UndoAction();
    public final RedoAction redo = new RedoAction();
    public final JosmAction copy = new CopyAction();
    public final JosmAction paste = new PasteAction();
    public final JosmAction pasteTags = new PasteTagsAction(copy);
    public final JosmAction duplicate = new DuplicateAction();
    public final JosmAction delete = new DeleteAction();
    public final JosmAction merge = new MergeLayerAction();
    public final JosmAction mergeSelected = new MergeSelectionAction();
    public final JosmAction selectAll = new SelectAllAction();
    public final JosmAction unselectAll = new UnselectAllAction();
    /* crashes when loading data, if using JosmAction for search */
    public final JosmAction search = new SearchAction();
    public final JosmAction preferences = new PreferencesAction();

    /* View menu */
    public final JosmAction toggleGPXLines = new ToggleGPXLinesAction();

    /* Tools menu */
    public final JosmAction splitWay = new SplitWayAction();
    public final JosmAction combineWay = new CombineWayAction();
    public final JosmAction reverseWay = new ReverseWayAction();
    public final JosmAction alignInCircle = new AlignInCircleAction();
    public final JosmAction alignInLine = new AlignInLineAction();
    public final JosmAction distribute = new DistributeAction();
    public final OrthogonalizeAction ortho = new OrthogonalizeAction();
    public final JosmAction orthoUndo = ortho.new Undo();  // action is not shown in the menu. Only triggered by shortcut
    public final JosmAction mirror = new MirrorAction();
    public final AddNodeAction addnode = new AddNodeAction();
    public final JosmAction createCircle = new CreateCircleAction();
    public final JosmAction mergeNodes = new MergeNodesAction();
    public final JosmAction joinNodeWay = new JoinNodeWayAction();
    public final JosmAction unglueNodes = new UnGlueAction();
    public final InfoAction info = new InfoAction();
    public final HistoryInfoAction historyinfo = new HistoryInfoAction();

    /* Audio menu */
    public final JosmAction audioPlayPause = new AudioPlayPauseAction();
    public final JosmAction audioNext = new AudioNextAction();
    public final JosmAction audioPrev = new AudioPrevAction();
    public final JosmAction audioFwd = new AudioFwdAction();
    public final JosmAction audioBack = new AudioBackAction();
    public final JosmAction audioFaster = new AudioFasterAction();
    public final JosmAction audioSlower = new AudioSlowerAction();

    /* Help menu */
    public final HelpAction help = new HelpAction();
    public final JosmAction about = new AboutAction();
    public final JosmAction statusreport = new ShowStatusReportAction();

    public final JMenu fileMenu = addMenu(marktr("File"), KeyEvent.VK_F, 0, ht("/Menu/File"));
    public final JMenu editMenu = addMenu(marktr("Edit"), KeyEvent.VK_E, 1, ht("/Menu/Edit"));
    public final JMenu viewMenu = addMenu(marktr("View"), KeyEvent.VK_V, 2, ht("/Menu/View"));
    public final JMenu toolsMenu = addMenu(marktr("Tools"), KeyEvent.VK_T, 3, ht("/Menu/Tools"));
    public final JMenu presetsMenu = addMenu(marktr("Presets"), KeyEvent.VK_P, 4, ht("/Menu/Presets"));
    public JMenu audioMenu = null;
    public final JMenu helpMenu = addMenu(marktr("Help"), KeyEvent.VK_H, 5, ht("/Menu/Help"));
    public final int defaultMenuPos = 5;

    /**
     * Add a JosmAction to a menu.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu.
     */
    public static JMenuItem add(JMenu menu, JosmAction action) {
        JMenuItem menuitem = null;
        if (!action.getShortcut().getAutomatic()) {
            menuitem = menu.add(action);
            KeyStroke ks = action.getShortcut().getKeyStroke();
            if (ks != null) {
                menuitem.setAccelerator(ks);
            }
        }
        return menuitem;
    }

    public JMenu addMenu(String name, int mnemonicKey, int position, String relativeHelpTopic) {
        JMenu menu = new JMenu(tr(name));
        Shortcut.registerShortcut("menu:" + name, tr("Menu: {0}", tr(name)), mnemonicKey,
                Shortcut.GROUP_MNEMONIC).setMnemonic(menu);
        add(menu, position);
        menu.putClientProperty("help", relativeHelpTopic);
        return menu;
    }

    public MainMenu() {
        JMenuItem current;

        add(fileMenu, newAction);
        add(fileMenu, openFile);
        add(fileMenu, openLocation);
        fileMenu.addSeparator();
        add(fileMenu, save);
        add(fileMenu, saveAs);
        add(fileMenu, gpxExport);
        fileMenu.addSeparator();
        add(fileMenu, download);
        add(fileMenu, downloadReferrers);
        add(fileMenu, update);
        add(fileMenu, updateSelection);
        fileMenu.addSeparator();
        add(fileMenu, upload);
        add(fileMenu, uploadSelection);
        fileMenu.addSeparator();
        add(fileMenu, closeChangesetAction);
        fileMenu.addSeparator();
        add(fileMenu, exit);

        add(editMenu, undo);
        add(editMenu, redo);
        editMenu.addSeparator();
        add(editMenu, copy);
        add(editMenu, paste);
        add(editMenu, pasteTags);
        add(editMenu, duplicate);
        add(editMenu, delete);
        editMenu.addSeparator();
        add(editMenu,merge);
        add(editMenu,mergeSelected);
        editMenu.addSeparator();
        add(editMenu, selectAll);
        add(editMenu, unselectAll);
        editMenu.addSeparator();
        add(editMenu, search);
        editMenu.addSeparator();
        add(editMenu, preferences);

        // -- wireframe toggle action
        WireframeToggleAction wireFrameToggleAction = new WireframeToggleAction();
        final JCheckBoxMenuItem wireframe = new JCheckBoxMenuItem(wireFrameToggleAction);
        viewMenu.add(wireframe);
        wireframe.setAccelerator(wireFrameToggleAction.getShortcut().getKeyStroke());
        wireFrameToggleAction.addButtonModel(wireframe.getModel());

        viewMenu.addSeparator();
        add(viewMenu, new ZoomInAction());
        add(viewMenu, new ZoomOutAction());
        viewMenu.addSeparator();
        for (String mode : AutoScaleAction.modes) {
            JosmAction autoScaleAction = new AutoScaleAction(mode);
            add(viewMenu, autoScaleAction);
        }

        //
        // Full Screen action
        //
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (Main.platform instanceof PlatformHookUnixoid && gd.isFullScreenSupported()) {
            final JCheckBoxMenuItem fullscreen = new JCheckBoxMenuItem(tr("Full Screen"));
            fullscreen.setSelected(Main.pref.getBoolean("draw.fullscreen", false));
            fullscreen.setAccelerator(Shortcut.registerShortcut("menu:view:fullscreen", tr("Toggle Full Screen view"),
                    KeyEvent.VK_F11, Shortcut.GROUP_DIRECT).getKeyStroke());

            fullscreen.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    Main.pref.put("draw.fullscreen", fullscreen.isSelected());

                    if (Main.pref.getBoolean("draw.fullscreen")) {
                        Frame frame = (Frame)Main.parent;
                        gd.setFullScreenWindow(frame);
                    } else {
                        gd.setFullScreenWindow(null);
                    }
                }
            });
            viewMenu.addSeparator();
            viewMenu.add(fullscreen);
        }

        add(toolsMenu, splitWay);
        add(toolsMenu, combineWay);
        toolsMenu.addSeparator();
        add(toolsMenu, reverseWay);
        toolsMenu.addSeparator();
        add(toolsMenu, alignInCircle);
        add(toolsMenu, alignInLine);
        add(toolsMenu, distribute);
        add(toolsMenu, ortho);
        add(toolsMenu, mirror);
        toolsMenu.addSeparator();
        add(toolsMenu, addnode);
        add(toolsMenu, createCircle);
        toolsMenu.addSeparator();
        add(toolsMenu, mergeNodes);
        add(toolsMenu, joinNodeWay);
        add(toolsMenu, unglueNodes);
        toolsMenu.addSeparator();
        add(toolsMenu, info);
        add(toolsMenu, historyinfo);

        if (!Main.pref.getBoolean("audio.menuinvisible", false)) {
            audioMenu = addMenu(marktr("Audio"), KeyEvent.VK_A, 5, ht("/Menu/Audio"));
            add(audioMenu, audioPlayPause);
            add(audioMenu, audioNext);
            add(audioMenu, audioPrev);
            add(audioMenu, audioFwd);
            add(audioMenu, audioBack);
            add(audioMenu, audioSlower);
            add(audioMenu, audioFaster);
        }

        helpMenu.add(statusreport);

        current = helpMenu.add(help); // FIXME why is help not a JosmAction?
        current.setAccelerator(Shortcut.registerShortcut("system:help", tr("Help"), KeyEvent.VK_F1,
                Shortcut.GROUP_DIRECT).getKeyStroke());
        add(helpMenu, about);

        new PresetsMenuEnabler(presetsMenu).refreshEnabled();
    }

    class PresetsMenuEnabler implements LayerChangeListener {
        private JMenu presetsMenu;
        public PresetsMenuEnabler(JMenu presetsMenu) {
            Layer.listeners.add(this);
            this.presetsMenu = presetsMenu;
        }
        /**
         * Refreshes the enabled state
         *
         */
        protected void refreshEnabled() {
            presetsMenu.setEnabled(Main.map != null
                    && Main.map.mapView !=null
                    && Main.map.mapView.getEditLayer() != null
            );
        }

        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            refreshEnabled();
        }

        public void layerAdded(Layer newLayer) {
            refreshEnabled();
        }

        public void layerRemoved(Layer oldLayer) {
            refreshEnabled();
        }
    }
}
