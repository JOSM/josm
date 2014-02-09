// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AddNodeAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.ChangesetManagerToggleAction;
import org.openstreetmap.josm.actions.CloseChangesetAction;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.CopyCoordinatesAction;
import org.openstreetmap.josm.actions.CreateCircleAction;
import org.openstreetmap.josm.actions.CreateMultipolygonAction;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.DialogsToggleAction;
import org.openstreetmap.josm.actions.DistributeAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DownloadPrimitiveAction;
import org.openstreetmap.josm.actions.DownloadReferrersAction;
import org.openstreetmap.josm.actions.DuplicateAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.FollowLineAction;
import org.openstreetmap.josm.actions.FullscreenToggleAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.actions.HistoryInfoAction;
import org.openstreetmap.josm.actions.HistoryInfoWebAction;
import org.openstreetmap.josm.actions.InfoAction;
import org.openstreetmap.josm.actions.InfoWebAction;
import org.openstreetmap.josm.actions.JoinAreasAction;
import org.openstreetmap.josm.actions.JoinNodeWayAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.JumpToAction;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.actions.MergeSelectionAction;
import org.openstreetmap.josm.actions.MirrorAction;
import org.openstreetmap.josm.actions.MoveAction;
import org.openstreetmap.josm.actions.MoveNodeAction;
import org.openstreetmap.josm.actions.NewAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.OpenLocationAction;
import org.openstreetmap.josm.actions.OrthogonalizeAction;
import org.openstreetmap.josm.actions.OrthogonalizeAction.Undo;
import org.openstreetmap.josm.actions.PasteAction;
import org.openstreetmap.josm.actions.PasteTagsAction;
import org.openstreetmap.josm.actions.PreferenceToggleAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.PurgeAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.SelectNonBranchingWaySequencesAction;
import org.openstreetmap.josm.actions.SessionLoadAction;
import org.openstreetmap.josm.actions.SessionSaveAsAction;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.actions.SimplifyWayAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.ToggleGPXLinesAction;
import org.openstreetmap.josm.actions.UnGlueAction;
import org.openstreetmap.josm.actions.UnJoinNodeWayAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UnselectAllAction;
import org.openstreetmap.josm.actions.UpdateDataAction;
import org.openstreetmap.josm.actions.UpdateModifiedAction;
import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.actions.UploadSelectionAction;
import org.openstreetmap.josm.actions.ViewportFollowToggleAction;
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
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.gui.io.RecentlyOpenedFilesMenu;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.mappaint.MapPaintMenu;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPresetSearchAction;
import org.openstreetmap.josm.gui.tagging.TaggingPresetSearchPrimitiveDialog;
import org.openstreetmap.josm.tools.ImageProvider;
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
    /** File / New Layer **/
    public final NewAction newAction = new NewAction();
    /** File / Open... **/
    public final OpenFileAction openFile = new OpenFileAction();
    /** File / Open Recent &gt; **/
    public final RecentlyOpenedFilesMenu recentlyOpened = new RecentlyOpenedFilesMenu();
    /** File / Open Location... **/
    public final OpenLocationAction openLocation = new OpenLocationAction();
    /** File / Save **/
    public final SaveAction save = SaveAction.getInstance();
    /** File / Save As... **/
    public final SaveAsAction saveAs = SaveAsAction.getInstance();
    /** File / Session &gt; Load Session **/
    public SessionLoadAction sessionLoad;
    /** File / Session &gt; Save Session As... **/
    public SessionSaveAsAction sessionSaveAs;
    /** File / Export to GPX... **/
    public final GpxExportAction gpxExport = new GpxExportAction();
    /** File / Download from OSM... **/
    public final DownloadAction download = new DownloadAction();
    /** File / Download object... **/
    public final DownloadPrimitiveAction downloadPrimitive = new DownloadPrimitiveAction();
    /** File / Download parent ways/relations... **/
    public final DownloadReferrersAction downloadReferrers = new DownloadReferrersAction();
    /** File / Close open changesets... **/
    public final CloseChangesetAction closeChangesetAction = new CloseChangesetAction();
    /** File / Update data **/
    public final JosmAction update = new UpdateDataAction();
    /** File / Update selection **/
    public final JosmAction updateSelection = new UpdateSelectionAction();
    /** File / Update modified **/
    public final JosmAction updateModified = new UpdateModifiedAction();
    /** File / Upload data **/
    public final JosmAction upload = new UploadAction();
    /** File / Upload selection **/
    public final JosmAction uploadSelection = new UploadSelectionAction();
    /** File / Restart **/
    public final RestartAction restart = new RestartAction();
    /** File / Exit **/
    public final ExitAction exit = new ExitAction();

    /* Edit menu */
    /** Edit / Undo... */
    public final UndoAction undo = new UndoAction();
    /** Edit / Redo */
    public final RedoAction redo = new RedoAction();
    /** Edit / Copy */
    public final CopyAction copy = new CopyAction();
    /** Edit / Copy Coordinates */
    public final JosmAction copyCoordinates = new CopyCoordinatesAction();
    /** Edit / Paste */
    public final PasteAction paste = new PasteAction();
    /** Edit / Paste Tags */
    public final PasteTagsAction pasteTags = new PasteTagsAction();
    /** Edit / Duplicate */
    public final DuplicateAction duplicate = new DuplicateAction();
    /** Edit / Delete */
    public final DeleteAction delete = new DeleteAction();
    /** Edit / Purge... */
    public final JosmAction purge = new PurgeAction();
    /** Edit / Merge layer */
    public final MergeLayerAction merge = new MergeLayerAction();
    /** Edit / Merge selection */
    public final MergeSelectionAction mergeSelected = new MergeSelectionAction();
    /** Edit / Search... */
    public final SearchAction search = new SearchAction();
    /** Edit / Preferences */
    public final PreferencesAction preferences = new PreferencesAction();

    /* View menu */
    /** View / Wireframe View */
    public final WireframeToggleAction wireFrameToggleAction = new WireframeToggleAction();
    public final JosmAction toggleGPXLines = new ToggleGPXLinesAction();
    /** View / Advanced info */
    public final InfoAction info = new InfoAction();
    /** View / Advanced info (web) */
    public final InfoWebAction infoweb = new InfoWebAction();
    /** View / History */
    public final HistoryInfoAction historyinfo = new HistoryInfoAction();
    /** View / History (web) */
    public final HistoryInfoWebAction historyinfoweb = new HistoryInfoWebAction();
    /** View / "Zoom to"... actions */
    public final Map<String, AutoScaleAction> autoScaleActions = new HashMap<String, AutoScaleAction>();
    /** View / Jump to position */
    public final JumpToAction jumpToAct = new JumpToAction();

    /* Tools menu */
    /** Tools / Split Way */
    public final SplitWayAction splitWay = new SplitWayAction();
    /** Tools / Combine Way */
    public final CombineWayAction combineWay = new CombineWayAction();
    /** Tools / Reverse Ways */
    public final ReverseWayAction reverseWay = new ReverseWayAction();
    /** Tools / Simplify Way */
    public final SimplifyWayAction simplifyWay = new SimplifyWayAction();
    /** Tools / Align Nodes in Circle */
    public final AlignInCircleAction alignInCircle = new AlignInCircleAction();
    /** Tools / Align Nodes in Line */
    public final AlignInLineAction alignInLine = new AlignInLineAction();
    /** Tools / Distribute Nodes */
    public final DistributeAction distribute = new DistributeAction();
    /** Tools / Orthogonalize Shape */
    public final OrthogonalizeAction ortho = new OrthogonalizeAction();
    /** Orthogonalize undo. Action is not shown in the menu. Only triggered by shortcut */
    public final Undo orthoUndo = new Undo();
    /** Tools / Mirror */
    public final MirrorAction mirror = new MirrorAction();
    /** Tools / Follow line */
    public final FollowLineAction followLine = new FollowLineAction();
    /** Tools / Add Node... */
    public final AddNodeAction addNode = new AddNodeAction();
    /** Tools / Move Node... */
    public final MoveNodeAction moveNode = new MoveNodeAction();
    /** Tools / Create Circle */
    public final CreateCircleAction createCircle = new CreateCircleAction();
    /** Tools / Merge Nodes */
    public final MergeNodesAction mergeNodes = new MergeNodesAction();
    /** Tools / Join Node to Way */
    public final JoinNodeWayAction joinNodeWay = new JoinNodeWayAction();
    /** Tools / Disconnect Node from Way */
    public final UnJoinNodeWayAction unJoinNodeWay = new UnJoinNodeWayAction();
    /** Tools / Unglue Ways */
    public final UnGlueAction unglueNodes = new UnGlueAction();
    /** Tools / Join overlapping Areas */
    public final JoinAreasAction joinAreas = new JoinAreasAction();
    /** Tools / Create multipolygon */
    public final CreateMultipolygonAction createMultipolygon = new CreateMultipolygonAction(false);
    /** Tools / Update multipolygon */
    public final CreateMultipolygonAction updateMultipolygon = new CreateMultipolygonAction(true);

    /* Selection menu */
    /** Selection / Select All */
    public final SelectAllAction selectAll = new SelectAllAction();
    /** Selection / Unselect All */
    public final UnselectAllAction unselectAll = new UnselectAllAction();
    /** Selection / Non-branching way sequences */
    public final SelectNonBranchingWaySequencesAction nonBranchingWaySequences = new SelectNonBranchingWaySequencesAction();

    /* Audio menu */
    /** Audio / Play/Pause */
    public final JosmAction audioPlayPause = new AudioPlayPauseAction();
    /** Audio / Next marker */
    public final JosmAction audioNext = new AudioNextAction();
    /** Audio / Previous Marker */
    public final JosmAction audioPrev = new AudioPrevAction();
    /** Audio / Forward */
    public final JosmAction audioFwd = new AudioFwdAction();
    /** Audio / Back */
    public final JosmAction audioBack = new AudioBackAction();
    /** Audio / Faster */
    public final JosmAction audioFaster = new AudioFasterAction();
    /** Audio / Slower */
    public final JosmAction audioSlower = new AudioSlowerAction();

    /* Windows Menu */
    /** Windows / Changeset Manager */
    public final ChangesetManagerToggleAction changesetManager = new ChangesetManagerToggleAction();

    /* Help menu */
    /** Help / Help */
    public final HelpAction help = new HelpAction();
    /** Help / About */
    public final AboutAction about = new AboutAction();
    /** Help / Show Status Report */
    public final ShowStatusReportAction statusreport = new ShowStatusReportAction();

    /**
     * fileMenu contains I/O actions
     */
    public final JMenu fileMenu = addMenu(marktr("File"), KeyEvent.VK_F, 0, ht("/Menu/File"));
    /**
     * sessionMenu is a submenu of File menu containing all session actions
     */
    public final JMenu sessionMenu = new JMenu(tr("Session"));
    /**
     * editMenu contains editing actions
     */
    public final JMenu editMenu = addMenu(marktr("Edit"), KeyEvent.VK_E, 1, ht("/Menu/Edit"));
    /**
     * viewMenu contains display actions (zoom, map styles, etc.)
     */
    public final JMenu viewMenu = addMenu(marktr("View"), KeyEvent.VK_V, 2, ht("/Menu/View"));
    /**
     * toolsMenu contains different geometry manipulation actions from JOSM core (most used)
     * The plugins should use other menus
     */
    public final JMenu toolsMenu = addMenu(marktr("Tools"), KeyEvent.VK_T, 3, ht("/Menu/Tools"));
    /**
     * moreToolsMenu contains geometry-related actions from all the plugins
     * @since 6082 (moved from Utilsplugin2)
     */
    public final JMenu moreToolsMenu = addMenu(marktr("More tools"), KeyEvent.VK_M, 4, ht("/Menu/MoreTools"));
    /**
     * dataMenu contains plugin actions that are related to certain tagging schemes (addressing opening hours),
     * importing external data and using external web APIs
     * @since 6082
     */
    public final JMenu dataMenu = addMenu(marktr("Data"), KeyEvent.VK_D, 5, ht("/Menu/Data"));
    /**
     * selectionMenu contains all actions related to selecting different objects
     * @since 6082 (moved from Utilsplugin2)
     */
    public final JMenu selectionMenu = addMenu(marktr("Selection"), KeyEvent.VK_N, 6, ht("/Menu/Selection"));
    /**
     * presetsMenu contains presets actions (search, presets tree)
     */
    public final JMenu presetsMenu = addMenu(marktr("Presets"), KeyEvent.VK_P, 7, ht("/Menu/Presets"));
    /**
     * submenu in Imagery menu that contains plugin-managed additional imagery layers
     * @since 6097
     */
    public final JMenu imagerySubMenu = new JMenu(tr("More..."));
    /**
     * imageryMenu contains all imagery-related actions
     */
    public final ImageryMenu imageryMenu = addMenu(new ImageryMenu(imagerySubMenu), marktr("Imagery"), KeyEvent.VK_I, 8, ht("/Menu/Imagery"));
    /**
     * gpsMenu contains all plugin actions that are related
     * to using GPS data, including opening, uploading and real-time tracking
     * @since 6082
     */
    public final JMenu gpsMenu = addMenu(marktr("GPS"), KeyEvent.VK_G, 9, ht("/Menu/GPS"));
    /** the window menu is split into several groups. The first is for windows that can be opened from
     * this menu any time, e.g. the changeset editor. The second group is for toggle dialogs and the third
     * group is for currently open windows that cannot be toggled, e.g. relation editors. It's recommended
     * to use WINDOW_MENU_GROUP to determine the group integer.
     */
    public final JMenu windowMenu = addMenu(marktr("Windows"), KeyEvent.VK_W, 10, ht("/Menu/Windows"));
    public static enum WINDOW_MENU_GROUP { ALWAYS, TOGGLE_DIALOG, VOLATILE }

    /**
     * audioMenu contains all audio-related actions. Be careful, this menu is not guaranteed to be displayed at all
     */
    public JMenu audioMenu = null;
    /**
     * helpMenu contains JOSM general actions (Help, About, etc.)
     */
    public final JMenu helpMenu = addMenu(marktr("Help"), KeyEvent.VK_H, 11, ht("/Menu/Help"));

    private static final int defaultMenuPos = 11;

    public final JosmAction moveUpAction = new MoveAction(MoveAction.Direction.UP);
    public final JosmAction moveDownAction = new MoveAction(MoveAction.Direction.DOWN);
    public final JosmAction moveLeftAction = new MoveAction(MoveAction.Direction.LEFT);
    public final JosmAction moveRightAction = new MoveAction(MoveAction.Direction.RIGHT);

    public final TaggingPresetSearchAction presetSearchAction = new TaggingPresetSearchAction();
    public final TaggingPresetSearchPrimitiveDialog.Action presetSearchPrimitiveAction = new TaggingPresetSearchPrimitiveDialog.Action();
    public final DialogsToggleAction dialogsToggleAction = new DialogsToggleAction();
    public FullscreenToggleAction fullscreenToggleAction = null;

    /** this menu listener hides unnecessary JSeparators in a menu list but does not remove them.
     * If at a later time the separators are required, they will be made visible again. Intended
     * usage is make menus not look broken if separators are used to group the menu and some of
     * these groups are empty.
     */
    public final static MenuListener menuSeparatorHandler = new MenuListener() {
        @Override
        public void menuCanceled(MenuEvent arg0) {}
        @Override
        public void menuDeselected(MenuEvent arg0) {}
        @Override
        public void menuSelected(MenuEvent a) {
            if(!(a.getSource() instanceof JMenu))
                return;
            final JPopupMenu m = ((JMenu) a.getSource()).getPopupMenu();
            for(int i=0; i < m.getComponentCount()-1; i++) {
                if(!(m.getComponent(i) instanceof JSeparator)) {
                    continue;
                }
                // hide separator if the next menu item is one as well
                ((JSeparator) m.getComponent(i)).setVisible(!(m.getComponent(i+1) instanceof JSeparator));
            }
            // hide separator at the end of the menu
            if(m.getComponent(m.getComponentCount()-1) instanceof JSeparator) {
                ((JSeparator) m.getComponent(m.getComponentCount()-1)).setVisible(false);
            }
        }
    };

    /**
     * @since 6088
     * @return the default position of tnew top-level menus
     */
    public int getDefaultMenuPos() {
         return defaultMenuPos;
    }

    /**
     * Add a JosmAction at the end of a menu.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu.
     * @param menu the menu to add the action to
     * @param action the action that should get a menu item
     * @return the created menu item
     */
    public static JMenuItem add(JMenu menu, JosmAction action) {
        return add(menu, action, false);
    }

    /**
     * Add a JosmAction at the end of a menu.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu.
     * @param menu the menu to add the action to
     * @param action the action that should get a menu item
     * @param isExpert whether the entry should only be visible if the expert mode is activated
     * @return the created menu item
     */
    public static JMenuItem add(JMenu menu, JosmAction action, boolean isExpert) {
        return add(menu, action, isExpert, null);
    }

    /**
     * Add a JosmAction at the end of a menu.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu.
     * @param menu the menu to add the action to
     * @param action the action that should get a menu item
     * @param isExpert whether the entry should only be visible if the expert mode is activated
     * @param index  an integer specifying the position at which to add the action
     * @return the created menu item
     */
    public static JMenuItem add(JMenu menu, JosmAction action, boolean isExpert, Integer index) {
        if (action.getShortcut().getAutomatic())
            return null;
        final JMenuItem menuitem;
        if (index == null) {
            menuitem = menu.add(action);
        } else {
            menuitem = menu.insert(action, index);
        }
        if (isExpert) {
            ExpertToggleAction.addVisibilitySwitcher(menuitem);
        }
        KeyStroke ks = action.getShortcut().getKeyStroke();
        if (ks != null) {
            menuitem.setAccelerator(ks);
        }
        // some menus are hidden before they are populated with some items by plugins
        if (!menu.isVisible()) menu.setVisible(true);
        return menuitem;
    }

    /**
     * Add the JosmAction {@code actionToBeInserted} directly below {@code existingMenuEntryAction}.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu.
     * @param menu the menu to add the action to
     * @param actionToBeInserted the action that should get a menu item directly below {@code existingMenuEntryAction}
     * @param isExpert whether the entry should only be visible if the expert mode is activated
     * @param existingMenuEntryAction an action already added to the menu {@code menu}, the action {@code actionToBeInserted} is added directly below
     * @return the created menu item
     */
    public static JMenuItem addAfter(JMenu menu, JosmAction actionToBeInserted, boolean isExpert, JosmAction existingMenuEntryAction) {
        int i = 0;
        for (Component c : menu.getMenuComponents()) {
            if (c instanceof JMenuItem && ((JMenuItem) c).getAction() == existingMenuEntryAction) {
                break;
            }
            i++;
        }
        return add(menu, actionToBeInserted, isExpert, i + 1);
    }

    /**
     * Add a JosmAction to a menu.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu.
     * @param menu to add the action to
     * @param action the action that should get a menu item
     * @param group the item should be added to. Groups are split by a separator.
     *        0 is the first group, -1 will add the item to the end.
     * @return The created menu item
     */
    public static <E extends Enum<E>> JMenuItem add(JMenu menu, JosmAction action, Enum<E> group) {
        if (action.getShortcut().getAutomatic())
            return null;
        int i = getInsertionIndexForGroup(menu, group.ordinal());
        JMenuItem menuitem = (JMenuItem) menu.add(new JMenuItem(action), i);
        KeyStroke ks = action.getShortcut().getKeyStroke();
        if (ks != null) {
            menuitem.setAccelerator(ks);
        }
        return menuitem;
    }

    /**
     * Add a JosmAction to a menu and automatically prints accelerator if available.
     * Also adds a checkbox that may be toggled.
     * @param menu to add the action to
     * @param action the action that should get a menu item
     * @param group the item should be added to. Groups are split by a separator. Use
     *        one of the enums that are defined for some of the menus to tell in which
     *        group the item should go.
     * @return The created menu item
     */
    public static <E extends Enum<E>> JCheckBoxMenuItem addWithCheckbox(JMenu menu, JosmAction action, Enum<E> group) {
        int i = getInsertionIndexForGroup(menu, group.ordinal());
        final JCheckBoxMenuItem mi = (JCheckBoxMenuItem) menu.add(new JCheckBoxMenuItem(action), i);
        final KeyStroke ks = action.getShortcut().getKeyStroke();
        if (ks != null) {
            mi.setAccelerator(ks);
        }
        return mi;
    }

    /** finds the correct insertion index for a given group and adds separators if necessary */
    private static int getInsertionIndexForGroup(JMenu menu, int group) {
        if(group < 0)
            return -1;
        // look for separator that *ends* the group (or stop at end of menu)
        int i;
        for(i=0; i < menu.getItemCount() && group >= 0; i++) {
            if(menu.getItem(i) == null) {
                group--;
            }
        }
        // insert before separator that ends the group
        if(group < 0) {
            i--;
        }
        // not enough separators have been found, add them
        while(group > 0) {
            menu.addSeparator();
            group--;
            i++;
        }
        return i;
    }

    public JMenu addMenu(String name, int mnemonicKey, int position, String relativeHelpTopic) {
        final JMenu menu = new JMenu(tr(name));
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        int menuItemHeight = new JMenu().add(newAction).getPreferredSize().height;
        MenuScroller.setScrollerFor(menu, screenHeight / menuItemHeight);
        return addMenu(menu, name, mnemonicKey, position, relativeHelpTopic);
    }

    public <T extends JMenu> T addMenu(T menu, String name, int mnemonicKey, int position, String relativeHelpTopic) {
        Shortcut.registerShortcut("menu:" + name, tr("Menu: {0}", tr(name)), mnemonicKey,
                Shortcut.MNEMONIC).setMnemonic(menu);
        add(menu, position);
        menu.putClientProperty("help", relativeHelpTopic);
        return menu;
    }

    /**
     * Constructs a new {@code MainMenu}.
     */
    public MainMenu() {
        JMenuItem current;

        moreToolsMenu.setVisible(false);
        dataMenu.setVisible(false);
        gpsMenu.setVisible(false);

        add(fileMenu, newAction);
        add(fileMenu, openFile);
        fileMenu.add(recentlyOpened);
        add(fileMenu, openLocation);
        fileMenu.addSeparator();
        add(fileMenu, save);
        add(fileMenu, saveAs);
        sessionMenu.setToolTipText(tr("Save and load the current session (list of layers, etc.)"));
        sessionMenu.setIcon(ImageProvider.get("session"));
        sessionSaveAs = new SessionSaveAsAction();
        sessionLoad = new SessionLoadAction();
        add(sessionMenu, sessionSaveAs);
        add(sessionMenu, sessionLoad);
        fileMenu.add(sessionMenu);
        ExpertToggleAction.addVisibilitySwitcher(sessionMenu);
        add(fileMenu, gpxExport, true);
        fileMenu.addSeparator();
        add(fileMenu, download);
        add(fileMenu, downloadPrimitive);
        add(fileMenu, downloadReferrers);
        add(fileMenu, update);
        add(fileMenu, updateSelection);
        add(fileMenu, updateModified);
        fileMenu.addSeparator();
        add(fileMenu, upload);
        add(fileMenu, uploadSelection);
        Component sep = new JPopupMenu.Separator();
        fileMenu.add(sep);
        ExpertToggleAction.addVisibilitySwitcher(sep);
        add(fileMenu, closeChangesetAction, true);
        fileMenu.addSeparator();
        add(fileMenu, restart);
        add(fileMenu, exit);

        add(editMenu, undo);
        Main.main.undoRedo.addCommandQueueListener(undo);
        add(editMenu, redo);
        Main.main.undoRedo.addCommandQueueListener(redo);
        editMenu.addSeparator();
        add(editMenu, copy);
        add(editMenu, copyCoordinates, true);
        add(editMenu, paste);
        add(editMenu, pasteTags);
        add(editMenu, duplicate);
        add(editMenu, delete);
        add(editMenu, purge, true);
        editMenu.addSeparator();
        add(editMenu,merge);
        add(editMenu,mergeSelected);
        editMenu.addSeparator();
        add(editMenu, search);
        add(editMenu, presetSearchPrimitiveAction);
        editMenu.addSeparator();
        add(editMenu, preferences);

        // -- wireframe toggle action
        final JCheckBoxMenuItem wireframe = new JCheckBoxMenuItem(wireFrameToggleAction);
        viewMenu.add(wireframe);
        wireframe.setAccelerator(wireFrameToggleAction.getShortcut().getKeyStroke());
        wireFrameToggleAction.addButtonModel(wireframe.getModel());

        viewMenu.add(new MapPaintMenu());
        viewMenu.addSeparator();
        add(viewMenu, new ZoomInAction());
        add(viewMenu, new ZoomOutAction());
        viewMenu.addSeparator();
        for (String mode : AutoScaleAction.MODES) {
            AutoScaleAction autoScaleAction = new AutoScaleAction(mode);
            autoScaleActions.put(mode, autoScaleAction);
            add(viewMenu, autoScaleAction);
        }

        // -- viewport follow toggle action
        ViewportFollowToggleAction viewportFollowToggleAction = new ViewportFollowToggleAction();
        final JCheckBoxMenuItem vft = new JCheckBoxMenuItem(viewportFollowToggleAction);
        ExpertToggleAction.addVisibilitySwitcher(vft);
        viewMenu.add(vft);
        vft.setAccelerator(viewportFollowToggleAction.getShortcut().getKeyStroke());
        viewportFollowToggleAction.addButtonModel(vft.getModel());

        if(!Main.applet && Main.platform.canFullscreen()) {
            // -- fullscreen toggle action
            fullscreenToggleAction = new FullscreenToggleAction();
            final JCheckBoxMenuItem fullscreen = new JCheckBoxMenuItem(fullscreenToggleAction);
            viewMenu.addSeparator();
            viewMenu.add(fullscreen);
            fullscreen.setAccelerator(fullscreenToggleAction.getShortcut().getKeyStroke());
            fullscreenToggleAction.addButtonModel(fullscreen.getModel());
        }

        // -- dialogs panel toggle action
        final JCheckBoxMenuItem dialogsToggle = new JCheckBoxMenuItem(dialogsToggleAction);
        dialogsToggle.setAccelerator(dialogsToggleAction.getShortcut().getKeyStroke());
        dialogsToggleAction.addButtonModel(dialogsToggle.getModel());
        viewMenu.add(dialogsToggle);

        add(viewMenu, jumpToAct, true);
        viewMenu.addSeparator();
        add(viewMenu, info);
        add(viewMenu, infoweb);
        add(viewMenu, historyinfo);
        add(viewMenu, historyinfoweb);
        viewMenu.addSeparator();
        viewMenu.add(new PreferenceToggleAction(tr("Edit toolbar"),
                tr("Toggles the visibility of the edit toolbar (i.e., the vertical tool)"),
                "sidetoolbar.visible", true).getCheckbox());
        // -- expert mode toggle action
        final JCheckBoxMenuItem expertItem = new JCheckBoxMenuItem(ExpertToggleAction.getInstance());
        viewMenu.add(expertItem);
        ExpertToggleAction.getInstance().addButtonModel(expertItem.getModel());

        add(presetsMenu, presetSearchAction);
        add(presetsMenu, presetSearchPrimitiveAction);
        add(presetsMenu, PreferencesAction.forPreferenceSubTab(tr("Preset preferences"),
                tr("Click to open the tagging presets tab in the preferences"), TaggingPresetPreference.class));
        presetsMenu.addSeparator();

        add(imageryMenu, PreferencesAction.forPreferenceTab(tr("Imagery preferences"),
                tr("Click to open the imagery tab in the preferences"), ImageryPreference.class));

        add(selectionMenu, selectAll);
        add(selectionMenu, unselectAll);
        add(selectionMenu, nonBranchingWaySequences);

        add(toolsMenu, splitWay);
        add(toolsMenu, combineWay);
        toolsMenu.addSeparator();
        add(toolsMenu, reverseWay);
        add(toolsMenu, simplifyWay);
        toolsMenu.addSeparator();
        add(toolsMenu, alignInCircle);
        add(toolsMenu, alignInLine);
        add(toolsMenu, distribute);
        add(toolsMenu, ortho);
        add(toolsMenu, mirror, true);
        toolsMenu.addSeparator();
        add(toolsMenu, followLine, true);
        add(toolsMenu, addNode, true);
        add(toolsMenu, moveNode, true);
        add(toolsMenu, createCircle);
        toolsMenu.addSeparator();
        add(toolsMenu, mergeNodes);
        add(toolsMenu, joinNodeWay);
        add(toolsMenu, unJoinNodeWay);
        add(toolsMenu, unglueNodes);
        toolsMenu.addSeparator();
        add(toolsMenu, joinAreas);
        add(toolsMenu, createMultipolygon);
        add(toolsMenu, updateMultipolygon);

        // -- changeset manager toggle action
        final JCheckBoxMenuItem mi = MainMenu.addWithCheckbox(windowMenu, changesetManager,
                MainMenu.WINDOW_MENU_GROUP.ALWAYS);
        changesetManager.addButtonModel(mi.getModel());

        if (!Main.pref.getBoolean("audio.menuinvisible", false)) {
            showAudioMenu(true);
        }

        Main.pref.addPreferenceChangeListener(new PreferenceChangedListener() {
            @Override
            public void preferenceChanged(PreferenceChangeEvent e) {
                if (e.getKey().equals("audio.menuinvisible")) {
                    showAudioMenu(!Boolean.parseBoolean(e.getNewValue().toString()));
                }
            }
        });

        helpMenu.add(statusreport);

        current = helpMenu.add(help); // FIXME why is help not a JosmAction?
        current.setAccelerator(Shortcut.registerShortcut("system:help", tr("Help"), KeyEvent.VK_F1,
                Shortcut.DIRECT).getKeyStroke());
        add(helpMenu, about);


        windowMenu.addMenuListener(menuSeparatorHandler);

        new PresetsMenuEnabler(presetsMenu).refreshEnabled();
    }

    protected void showAudioMenu(boolean showMenu) {
        if (showMenu && audioMenu == null) {
            audioMenu = addMenu(marktr("Audio"), KeyEvent.VK_U, defaultMenuPos, ht("/Menu/Audio"));
            add(audioMenu, audioPlayPause);
            add(audioMenu, audioNext);
            add(audioMenu, audioPrev);
            add(audioMenu, audioFwd);
            add(audioMenu, audioBack);
            add(audioMenu, audioSlower);
            add(audioMenu, audioFaster);
            validate();
        } else if (!showMenu && audioMenu != null) {
            remove(audioMenu);
            audioMenu.removeAll();
            audioMenu = null;
            validate();
        }
    }

    static class PresetsMenuEnabler implements MapView.LayerChangeListener {
        private JMenu presetsMenu;
        public PresetsMenuEnabler(JMenu presetsMenu) {
            MapView.addLayerChangeListener(this);
            this.presetsMenu = presetsMenu;
        }
        /**
         * Refreshes the enabled state
         */
        protected void refreshEnabled() {
            presetsMenu.setEnabled(Main.main.hasEditLayer());
        }

        @Override
        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            refreshEnabled();
        }

        @Override
        public void layerAdded(Layer newLayer) {
            refreshEnabled();
        }

        @Override
        public void layerRemoved(Layer oldLayer) {
            refreshEnabled();
        }
    }
}
