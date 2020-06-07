// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AddNodeAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.AutoScaleAction.AutoScaleMode;
import org.openstreetmap.josm.actions.ChangesetManagerToggleAction;
import org.openstreetmap.josm.actions.CloseChangesetAction;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.CopyCoordinatesAction;
import org.openstreetmap.josm.actions.CreateCircleAction;
import org.openstreetmap.josm.actions.CreateMultipolygonAction;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.DeleteLayerAction;
import org.openstreetmap.josm.actions.DialogsToggleAction;
import org.openstreetmap.josm.actions.DistributeAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DownloadAlongWayAction;
import org.openstreetmap.josm.actions.DownloadNotesInViewAction;
import org.openstreetmap.josm.actions.DownloadOsmInViewAction;
import org.openstreetmap.josm.actions.DownloadPrimitiveAction;
import org.openstreetmap.josm.actions.DownloadReferrersAction;
import org.openstreetmap.josm.actions.DrawBoundariesOfDownloadedDataAction;
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
import org.openstreetmap.josm.actions.PasteAtSourcePositionAction;
import org.openstreetmap.josm.actions.PasteTagsAction;
import org.openstreetmap.josm.actions.PreferenceToggleAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.PurgeAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReorderImageryLayersAction;
import org.openstreetmap.josm.actions.ReportBugAction;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SearchNotesDownloadAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.SelectNonBranchingWaySequencesAction;
import org.openstreetmap.josm.actions.SessionSaveAsAction;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.actions.SimplifyWayAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.TaggingPresetSearchAction;
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
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.gui.dialogs.MenuItemSearchDialog;
import org.openstreetmap.josm.gui.io.RecentlyOpenedFilesMenu;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.mappaint.MapPaintMenu;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.PlatformManager;
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

    /**
     * The possible item groups of the Windows menu.
     * @see MainMenu#addWithCheckbox
     */
    public enum WINDOW_MENU_GROUP {
        /** Entries always displayed, at the top */
        ALWAYS,
        /** Entries displayed only for visible toggle dialogs */
        TOGGLE_DIALOG,
        /** Volatile entries displayed at the end */
        VOLATILE
    }

    /* File menu */
    /** File / New Layer **/
    public final NewAction newAction = new NewAction();
    /** File / Open... **/
    public final OpenFileAction openFile = new OpenFileAction();
    /** File / Open Recent &gt; **/
    public final RecentlyOpenedFilesMenu recentlyOpened = new RecentlyOpenedFilesMenu();
    /** File / Open Location... **/
    public final OpenLocationAction openLocation = new OpenLocationAction();
    /** File / Delete Layer **/
    public final DeleteLayerAction deleteLayerAction = new DeleteLayerAction();
    /** File / Save **/
    public final SaveAction save = SaveAction.getInstance();
    /** File / Save As... **/
    public final SaveAsAction saveAs = SaveAsAction.getInstance();
    /** File / Session &gt; Save Session As... **/
    public SessionSaveAsAction sessionSaveAs;
    /** File / Export to GPX... **/
    public final GpxExportAction gpxExport = new GpxExportAction();
    /** File / Download from OSM... **/
    public final DownloadAction download = new DownloadAction();
    /** File / Download in current view **/
    public final DownloadOsmInViewAction downloadInView = new DownloadOsmInViewAction();
    /** File / Download object... **/
    public final DownloadPrimitiveAction downloadPrimitive = new DownloadPrimitiveAction();
    /** File / Download notes in current view **/
    public final DownloadNotesInViewAction downloadNotesInView = DownloadNotesInViewAction.newActionWithNoteIcon();
    /** File / Search Notes... **/
    public final SearchNotesDownloadAction searchNotes = new SearchNotesDownloadAction();
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
    /** Edit / Paste at source */
    private final PasteAtSourcePositionAction pasteAtSource = new PasteAtSourcePositionAction();
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
    /** View / Hatch area outside download */
    public final DrawBoundariesOfDownloadedDataAction drawBoundariesOfDownloadedDataAction = new DrawBoundariesOfDownloadedDataAction();
    /** View / Advanced info */
    public final InfoAction info = new InfoAction();
    /** View / Advanced info (web) */
    public final InfoWebAction infoweb = new InfoWebAction();
    /** View / History */
    public final HistoryInfoAction historyinfo = new HistoryInfoAction();
    /** View / History (web) */
    public final HistoryInfoWebAction historyinfoweb = new HistoryInfoWebAction();
    /** View / "Zoom to"... actions */
    public final Map<String, AutoScaleAction> autoScaleActions = new HashMap<>();
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
    public final JoinNodeWayAction joinNodeWay = JoinNodeWayAction.createJoinNodeToWayAction();
    /** Tools / Join Way to Node */
    public final JoinNodeWayAction moveNodeOntoWay = JoinNodeWayAction.createMoveNodeOntoWayAction();
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
    /** Tools / Download along way */
    public final DownloadAlongWayAction downloadAlongWay = new DownloadAlongWayAction();

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
    /** Help / Report bug */
    public final ReportBugAction reportbug = new ReportBugAction();

    /**
     * fileMenu contains I/O actions
     */
    public final JMenu fileMenu = addMenu("File", /* I18N: mnemonic: F */ trc("menu", "File"), KeyEvent.VK_F, 0, ht("/Menu/File"));
    /**
     * editMenu contains editing actions
     */
    public final JMenu editMenu = addMenu("Edit", /* I18N: mnemonic: E */ trc("menu", "Edit"), KeyEvent.VK_E, 1, ht("/Menu/Edit"));
    /**
     * viewMenu contains display actions (zoom, map styles, etc.)
     */
    public final JMenu viewMenu = addMenu("View", /* I18N: mnemonic: V */ trc("menu", "View"), KeyEvent.VK_V, 2, ht("/Menu/View"));
    /**
     * modeMenu contains map modes
     */
    public final JMenu modeMenu = addMenu(new ModeMenu(), /* I18N: mnemonic: M */ trc("menu", "Mode"), KeyEvent.VK_M, 3, ht("/Menu/Mode"));
    /**
     * toolsMenu contains different geometry manipulation actions from JOSM core (most used)
     * The plugins should use other menus
     */
    public final JMenu toolsMenu = addMenu("Tools", /* I18N: mnemonic: T */ trc("menu", "Tools"), KeyEvent.VK_T, 4, ht("/Menu/Tools"));
    /**
     * moreToolsMenu contains geometry-related actions from all the plugins
     * @since 6082 (moved from Utilsplugin2)
     */
    // CHECKSTYLE.OFF: LineLength
    public final JMenu moreToolsMenu = addMenu("More tools", /* I18N: mnemonic: O */ trc("menu", "More tools"), KeyEvent.VK_O, 5, ht("/Menu/MoreTools"));
    /**
     * dataMenu contains plugin actions that are related to certain tagging schemes (addressing opening hours),
     * importing external data and using external web APIs
     * @since 6082
     */
    public final JMenu dataMenu = addMenu("Data", /* I18N: mnemonic: D */ trc("menu", "Data"), KeyEvent.VK_D, 6, ht("/Menu/Data"));
    /**
     * selectionMenu contains all actions related to selecting different objects
     * @since 6082 (moved from Utilsplugin2)
     */
    public final JMenu selectionMenu = addMenu("Selection", /* I18N: mnemonic: N */ trc("menu", "Selection"), KeyEvent.VK_N, 7, ht("/Menu/Selection"));
    /**
     * presetsMenu contains presets actions (search, presets tree)
     */
    public final JMenu presetsMenu = addMenu("Presets", /* I18N: mnemonic: P */ trc("menu", "Presets"), KeyEvent.VK_P, 8, ht("/Menu/Presets"));
    /**
     * submenu in Imagery menu that contains plugin-managed additional imagery layers
     * @since 6097
     */
    public final JMenu imagerySubMenu = new JMenu(tr("More..."));
    /**
     * imageryMenu contains all imagery-related actions
     */
    public final ImageryMenu imageryMenu = addMenu(new ImageryMenu(imagerySubMenu), /* untranslated name */ "Imagery", KeyEvent.VK_I, 9, ht("/Menu/Imagery"));
    /**
     * gpsMenu contains all plugin actions that are related
     * to using GPS data, including opening, uploading and real-time tracking
     * @since 6082
     */
    public final JMenu gpsMenu = addMenu("GPS", /* I18N: mnemonic: G */ trc("menu", "GPS"), KeyEvent.VK_G, 10, ht("/Menu/GPS"));
    /** the window menu is split into several groups. The first is for windows that can be opened from
     * this menu any time, e.g. the changeset editor. The second group is for toggle dialogs and the third
     * group is for currently open windows that cannot be toggled, e.g. relation editors. It's recommended
     * to use WINDOW_MENU_GROUP to determine the group integer.
     */
    public final WindowMenu windowMenu = addMenu(new WindowMenu(), /* untranslated name */ "Windows", KeyEvent.VK_W, 11, ht("/Help/Menu/Windows"));
    // CHECKSTYLE.ON: LineLength

    /**
     * audioMenu contains all audio-related actions. Be careful, this menu is not guaranteed to be displayed at all
     */
    public JMenu audioMenu;
    /**
     * helpMenu contains JOSM general actions (Help, About, etc.)
     */
    public final JMenu helpMenu = addMenu("Help", /* I18N: mnemonic: H */ trc("menu", "Help"), KeyEvent.VK_H, 12, ht("/Menu/Help"));

    private static final int defaultMenuPos = 12;

    /** Move the selection up */
    public final JosmAction moveUpAction = new MoveAction(MoveAction.Direction.UP);
    /** Move the selection down */
    public final JosmAction moveDownAction = new MoveAction(MoveAction.Direction.DOWN);
    /** Move the selection left */
    public final JosmAction moveLeftAction = new MoveAction(MoveAction.Direction.LEFT);
    /** Move the selection right */
    public final JosmAction moveRightAction = new MoveAction(MoveAction.Direction.RIGHT);

    /** Reorder imagery layers */
    public final ReorderImageryLayersAction reorderImageryLayersAction = new ReorderImageryLayersAction();

    /** Search tagging presets */
    public final TaggingPresetSearchAction presetSearchAction = new TaggingPresetSearchAction();
    /** Search objects by their tagging preset */
    public final TaggingPresetSearchPrimitiveDialog.Action presetSearchPrimitiveAction = new TaggingPresetSearchPrimitiveDialog.Action();
    /** Toggle visibility of dialogs panel */
    public final DialogsToggleAction dialogsToggleAction = new DialogsToggleAction();
    /** Toggle the full-screen mode */
    public FullscreenToggleAction fullscreenToggleAction;

    /** this menu listener hides unnecessary JSeparators in a menu list but does not remove them.
     * If at a later time the separators are required, they will be made visible again. Intended
     * usage is make menus not look broken if separators are used to group the menu and some of
     * these groups are empty.
     */
    public static final MenuListener menuSeparatorHandler = new MenuListener() {
        @Override
        public void menuCanceled(MenuEvent e) {
            // Do nothing
        }

        @Override
        public void menuDeselected(MenuEvent e) {
            // Do nothing
        }

        @Override
        public void menuSelected(MenuEvent a) {
            if (!(a.getSource() instanceof JMenu))
                return;
            final JPopupMenu m = ((JMenu) a.getSource()).getPopupMenu();
            for (int i = 0; i < m.getComponentCount()-1; i++) {
                // hide separator if the next menu item is one as well
                if (m.getComponent(i) instanceof JSeparator && m.getComponent(i+1) instanceof JSeparator) {
                    ((JSeparator) m.getComponent(i)).setVisible(false);
                }
            }
            // hide separator at the end of the menu
            if (m.getComponent(m.getComponentCount()-1) instanceof JSeparator) {
                ((JSeparator) m.getComponent(m.getComponentCount()-1)).setVisible(false);
            }
        }
    };

    /**
     * Returns the default position of new top-level menus.
     * @return the default position of new top-level menus
     * @since 6088
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
        if (action.getShortcut().isAutomatic())
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
     * @param existingMenuEntryAction an action already added to the menu {@code menu},
     * the action {@code actionToBeInserted} is added directly below
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
     * @param <E> group item enum type
     * @param menu to add the action to
     * @param action the action that should get a menu item
     * @param group the item should be added to. Groups are split by a separator. null will add the item to the end.
     * @return The created menu item
     */
    public static <E extends Enum<E>> JMenuItem add(JMenu menu, JosmAction action, Enum<E> group) {
        if (action.getShortcut().isAutomatic())
            return null;
        int i = group != null ? getInsertionIndexForGroup(menu, group.ordinal(), false) : -1;
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
     * @param <E> group enum item type
     * @param menu to add the action to
     * @param action the action that should get a menu item
     * @param group the item should be added to. Groups are split by a separator. Use
     *        one of the enums that are defined for some of the menus to tell in which
     *        group the item should go.
     * @return The created menu item
     */
    public static <E extends Enum<E>> JCheckBoxMenuItem addWithCheckbox(JMenu menu, JosmAction action, Enum<E> group) {
        return addWithCheckbox(menu, action, group, false, false);
    }

    /**
     * Add a JosmAction to a menu and automatically prints accelerator if available.
     * Also adds a checkbox that may be toggled.
     * @param <E> group enum item type
     * @param menu to add the action to
     * @param action the action that should get a menu item
     * @param group the item should be added to. Groups are split by a separator. Use
     *        one of the enums that are defined for some of the menus to tell in which
     *        group the item should go. null will add the item to the end.
     * @param isEntryExpert whether the entry should only be visible if the expert mode is activated
     * @param isGroupSeparatorExpert whether the group separator should only be visible if the expert mode is activated
     * @return The created menu item
     * @since 15633
     */
    public static <E extends Enum<E>> JCheckBoxMenuItem addWithCheckbox(JMenu menu, JosmAction action, Enum<E> group,
            boolean isEntryExpert, boolean isGroupSeparatorExpert) {
        int i = group != null ? getInsertionIndexForGroup(menu, group.ordinal(), isGroupSeparatorExpert) : -1;
        return addWithCheckbox(menu, action, i, isEntryExpert);
    }

    /**
     * Add a JosmAction to a menu and automatically prints accelerator if available.
     * Also adds a checkbox that may be toggled.
     * @param <E> group enum item type
     * @param menu to add the action to
     * @param action the action that should get a menu item
     * @param i the item position in the menu. -1 will add the item to the end.
     * @param isEntryExpert whether the entry should only be visible if the expert mode is activated
     * @return The created menu item
     * @since 15655
     */
    public static <E extends Enum<E>> JCheckBoxMenuItem addWithCheckbox(JMenu menu, JosmAction action, int i, boolean isEntryExpert) {
        final JCheckBoxMenuItem mi = new JCheckBoxMenuItem(action);
        final KeyStroke ks = action.getShortcut().getKeyStroke();
        if (ks != null) {
            mi.setAccelerator(ks);
        }
        if (isEntryExpert) {
            ExpertToggleAction.addVisibilitySwitcher(mi);
        }
        return (JCheckBoxMenuItem) menu.add(mi, i);
    }

    /**
     * Finds the correct insertion index for a given group and adds separators if necessary
     * @param menu menu
     * @param group group number
     * @param isGroupSeparatorExpert whether the added separators should only be visible if the expert mode is activated
     * @return correct insertion index
     */
    private static int getInsertionIndexForGroup(JMenu menu, int group, boolean isGroupSeparatorExpert) {
        if (group < 0)
            return -1;
        // look for separator that *ends* the group (or stop at end of menu)
        int i;
        for (i = 0; i < menu.getItemCount() && group >= 0; i++) {
            if (menu.getItem(i) == null) {
                group--;
            }
        }
        // insert before separator that ends the group
        if (group < 0) {
            i--;
        }
        // not enough separators have been found, add them
        while (group > 0) {
            menu.addSeparator();
            if (isGroupSeparatorExpert) {
                ExpertToggleAction.addVisibilitySwitcher(menu.getMenuComponent(menu.getMenuComponentCount() - 1));
            }
            group--;
            i++;
        }
        return i;
    }

    /**
     * Creates a menu and adds it on the given position to the main menu.
     *
     * @param name              the untranslated name (used as identifier for shortcut registration)
     * @param translatedName    the translated menu name (use {@code I18n.trc("menu", name)} to allow better internationalization
     * @param mnemonicKey       the mnemonic key to register
     * @param position          the position in the main menu
     * @param relativeHelpTopic the relative help topic
     * @return the newly created menu
     */
    public JMenu addMenu(String name, String translatedName, int mnemonicKey, int position, String relativeHelpTopic) {
        final JMenu menu = new JMenu(translatedName);
        if (!GraphicsEnvironment.isHeadless()) {
            MenuScroller.setScrollerFor(menu);
        }
        return addMenu(menu, name, mnemonicKey, position, relativeHelpTopic);
    }

    /**
     * Adds the given menu on the given position to the main menu.
     * @param <T> menu type
     *
     * @param menu              the menu to add
     * @param name              the untranslated name (used as identifier for shortcut registration)
     * @param mnemonicKey       the mnemonic key to register
     * @param position          the position in the main menu
     * @param relativeHelpTopic the relative help topic
     * @return the given {@code }menu}
     */
    public <T extends JMenu> T addMenu(T menu, String name, int mnemonicKey, int position, String relativeHelpTopic) {
        Shortcut.registerShortcut("menu:" + name, tr("Menu: {0}", name), mnemonicKey,
                Shortcut.MNEMONIC).setMnemonic(menu);
        add(menu, position);
        menu.putClientProperty("help", relativeHelpTopic);
        return menu;
    }

    /**
     * Initialize the main menu.
     * @since 10340
     */
    // CHECKSTYLE.OFF: ExecutableStatementCountCheck
    public void initialize() {
        moreToolsMenu.setVisible(false);
        dataMenu.setVisible(false);
        gpsMenu.setVisible(false);

        add(fileMenu, newAction);
        add(fileMenu, openFile);
        fileMenu.add(recentlyOpened);
        add(fileMenu, openLocation);
        add(fileMenu, deleteLayerAction);
        fileMenu.addSeparator();
        add(fileMenu, save);
        add(fileMenu, saveAs);
        sessionSaveAs = new SessionSaveAsAction();
        ExpertToggleAction.addVisibilitySwitcher(fileMenu.add(sessionSaveAs));
        add(fileMenu, gpxExport, true);
        fileMenu.addSeparator();
        add(fileMenu, download);
        add(fileMenu, downloadInView, true);
        add(fileMenu, downloadAlongWay);
        add(fileMenu, downloadPrimitive);
        add(fileMenu, searchNotes);
        add(fileMenu, downloadNotesInView);
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
        UndoRedoHandler.getInstance().addCommandQueueListener(undo);
        add(editMenu, redo);
        UndoRedoHandler.getInstance().addCommandQueueListener(redo);
        editMenu.addSeparator();
        add(editMenu, copy);
        add(editMenu, copyCoordinates, true);
        add(editMenu, paste);
        add(editMenu, pasteAtSource, true);
        add(editMenu, pasteTags);
        add(editMenu, duplicate);
        add(editMenu, delete);
        add(editMenu, purge, true);
        editMenu.addSeparator();
        add(editMenu, merge);
        add(editMenu, mergeSelected);
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
        final JCheckBoxMenuItem hatchAreaOutsideDownloadMenuItem = drawBoundariesOfDownloadedDataAction.getCheckbox();
        viewMenu.add(hatchAreaOutsideDownloadMenuItem);
        ExpertToggleAction.addVisibilitySwitcher(hatchAreaOutsideDownloadMenuItem);

        viewMenu.add(new MapPaintMenu());
        viewMenu.addSeparator();
        add(viewMenu, new ZoomInAction());
        add(viewMenu, new ZoomOutAction());
        viewMenu.addSeparator();
        for (AutoScaleMode mode : AutoScaleMode.values()) {
            AutoScaleAction autoScaleAction = new AutoScaleAction(mode);
            autoScaleActions.put(mode.getEnglishLabel(), autoScaleAction);
            add(viewMenu, autoScaleAction);
        }

        // -- viewport follow toggle action
        ViewportFollowToggleAction viewportFollowToggleAction = new ViewportFollowToggleAction();
        final JCheckBoxMenuItem vft = new JCheckBoxMenuItem(viewportFollowToggleAction);
        ExpertToggleAction.addVisibilitySwitcher(vft);
        viewMenu.add(vft);
        vft.setAccelerator(viewportFollowToggleAction.getShortcut().getKeyStroke());
        viewportFollowToggleAction.addButtonModel(vft.getModel());

        if (PlatformManager.getPlatform().canFullscreen()) {
            // -- fullscreen toggle action
            fullscreenToggleAction = new FullscreenToggleAction();
            final JCheckBoxMenuItem fullscreen = new JCheckBoxMenuItem(fullscreenToggleAction);
            viewMenu.addSeparator();
            viewMenu.add(fullscreen);
            fullscreen.setAccelerator(fullscreenToggleAction.getShortcut().getKeyStroke());
            fullscreenToggleAction.addButtonModel(fullscreen.getModel());
        }

        add(viewMenu, jumpToAct, true);
        viewMenu.addSeparator();
        add(viewMenu, info);
        add(viewMenu, infoweb);
        add(viewMenu, historyinfo);
        add(viewMenu, historyinfoweb);
        viewMenu.addSeparator();
        viewMenu.add(new PreferenceToggleAction(tr("Main toolbar"),
                tr("Toggles the visibility of the main toolbar (i.e., the horizontal toolbar)"),
                MapFrame.TOOLBAR_VISIBLE).getCheckbox());
        viewMenu.add(new PreferenceToggleAction(tr("Edit toolbar"),
                tr("Toggles the visibility of the edit toolbar (i.e., the vertical tool)"),
                MapFrame.SIDE_TOOLBAR_VISIBLE).getCheckbox());
        // -- dialogs panel toggle action
        final JCheckBoxMenuItem dialogsToggle = new JCheckBoxMenuItem(dialogsToggleAction);
        dialogsToggle.setAccelerator(dialogsToggleAction.getShortcut().getKeyStroke());
        dialogsToggleAction.addButtonModel(dialogsToggle.getModel());
        viewMenu.add(dialogsToggle);
        viewMenu.addSeparator();
        // -- expert mode toggle action
        final JCheckBoxMenuItem expertItem = new JCheckBoxMenuItem(ExpertToggleAction.getInstance());
        viewMenu.add(expertItem);
        ExpertToggleAction.getInstance().addButtonModel(expertItem.getModel());

        add(imageryMenu, reorderImageryLayersAction);
        add(imageryMenu, PreferencesAction.forPreferenceTab(tr("Imagery preferences..."),
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
        add(toolsMenu, moveNodeOntoWay);
        add(toolsMenu, unJoinNodeWay);
        add(toolsMenu, unglueNodes);
        toolsMenu.addSeparator();
        add(toolsMenu, joinAreas);
        add(toolsMenu, createMultipolygon);
        add(toolsMenu, updateMultipolygon);

        // -- changeset manager toggle action
        final JCheckBoxMenuItem mi = MainMenu.addWithCheckbox(windowMenu, changesetManager,
                WINDOW_MENU_GROUP.ALWAYS, true, false);
        changesetManager.addButtonModel(mi.getModel());

        if (!Config.getPref().getBoolean("audio.menuinvisible", false)) {
            showAudioMenu(true);
        }

        Config.getPref().addPreferenceChangeListener(e -> {
            if ("audio.menuinvisible".equals(e.getKey())) {
                showAudioMenu(!Boolean.parseBoolean(e.getNewValue().toString()));
            }
        });

        add(helpMenu, new MenuItemSearchDialog.Action());
        helpMenu.addSeparator();
        add(helpMenu, statusreport);
        add(helpMenu, reportbug);
        helpMenu.addSeparator();

        add(helpMenu, help);
        add(helpMenu, about);

        windowMenu.addMenuListener(menuSeparatorHandler);

        new EditLayerMenuEnabler(Arrays.asList(modeMenu, toolsMenu, moreToolsMenu, selectionMenu));
    }
    // CHECKSTYLE.ON: ExecutableStatementCountCheck

    /**
     * Search main menu for items with {@code textToFind} in title.
     * @param textToFind The text to find
     * @param skipPresets whether to skip the {@link #presetsMenu} in the search
     * @return not null list of found menu items.
     */
    public List<JMenuItem> findMenuItems(String textToFind, boolean skipPresets) {
        // Explicitly use default locale in this case, because we're looking for translated strings
        textToFind = textToFind.toLowerCase(Locale.getDefault());
        List<JMenuItem> result = new ArrayList<>();
        for (int i = 0; i < getMenuCount(); i++) {
            if (getMenu(i) != null && (!skipPresets || presetsMenu != getMenu(i))) {
                findMenuItems(getMenu(i), textToFind, result);
            }
        }
        return result;
    }

    /**
     * Returns the {@link JCheckBoxMenuItem} for the given {@link MapMode}.
     * @param mode map mode
     * @return the {@code JCheckBoxMenuItem} for the given {@code MapMode}
     * @since 15438
     */
    public Optional<JCheckBoxMenuItem> findMapModeMenuItem(MapMode mode) {
        return Arrays.stream(modeMenu.getMenuComponents())
                .filter(m -> m instanceof JCheckBoxMenuItem)
                .map(m -> (JCheckBoxMenuItem) m)
                .filter(m -> Objects.equals(mode, m.getAction()))
                .findFirst();
    }

    /**
     * Recursive walker for menu items. Only menu items with action are selected. If menu item
     * contains {@code textToFind} it's appended to result.
     * @param menu menu in which search will be performed
     * @param textToFind The text to find
     * @param result resulting list of menu items
     */
    private static void findMenuItems(final JMenu menu, final String textToFind, final List<JMenuItem> result) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem menuItem = menu.getItem(i);
            if (menuItem == null) continue;

            // Explicitly use default locale in this case, because we're looking for translated strings
            if (menuItem.getAction() != null && menuItem.getText().toLowerCase(Locale.getDefault()).contains(textToFind)) {
                result.add(menuItem);
            }

            // Go recursive if needed
            if (menuItem instanceof JMenu) {
                findMenuItems((JMenu) menuItem, textToFind, result);
            }
        }
    }

    protected void showAudioMenu(boolean showMenu) {
        if (showMenu && audioMenu == null) {
            audioMenu = addMenu("Audio", /* I18N: mnemonic: U */ trc("menu", "Audio"), KeyEvent.VK_U, defaultMenuPos, ht("/Menu/Audio"));
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

    static class EditLayerMenuEnabler implements ActiveLayerChangeListener {
        private final Collection<JMenu> menus;

        EditLayerMenuEnabler(Collection<JMenu> menus) {
            this.menus = Objects.requireNonNull(menus);
            MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(this);
        }

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            menus.forEach(m -> m.setEnabled(e.getSource().getEditLayer() != null));
        }
    }
}
