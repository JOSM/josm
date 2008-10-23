// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.AlignInRectangleAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.CreateCircleAction;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DuplicateAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.actions.HistoryInfoAction;
import org.openstreetmap.josm.actions.JoinNodeWayAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.actions.NewAction;
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PasteAction;
import org.openstreetmap.josm.actions.PasteTagsAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.UnGlueAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UnselectAllAction;
import org.openstreetmap.josm.actions.UploadAction;
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
import org.openstreetmap.josm.actions.ToggleGPXLinesAction;
import org.openstreetmap.josm.data.DataSetChecker;
import org.openstreetmap.josm.tools.ShortCut;

/**
 * This is the JOSM main menu bar. It is overwritten to initialize itself and provide
 * all menu entries as member variables (sort of collect them).
 *
 * It also provides possibilities to attach new menu entries (used by plugins).
 *
 * @author Immanuel.Scholz
 */
public class MainMenu extends JMenuBar {

	/* File menu */
	public final NewAction newAction = new NewAction();
	public final OpenAction open = new OpenAction();
	public final JosmAction save = new SaveAction(null);
	public final JosmAction saveAs = new SaveAsAction(null);
	public final JosmAction gpxExport = new GpxExportAction(null);
	public final DownloadAction download = new DownloadAction();
	public final JosmAction upload = new UploadAction();
	public final JosmAction exit = new ExitAction();

	/* Edit menu */
	public final UndoAction undo = new UndoAction();
	public final RedoAction redo = new RedoAction();
	public final JosmAction copy = new CopyAction();
	public final JosmAction paste = new PasteAction();
	public final JosmAction delete = new DeleteAction();
	public final JosmAction pasteTags = new PasteTagsAction(copy);
	public final JosmAction duplicate = new DuplicateAction();
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
	public final JosmAction alignInRect = new AlignInRectangleAction();
	public final JosmAction createCircle = new CreateCircleAction();
	public final JosmAction mergeNodes = new MergeNodesAction();
	public final JosmAction joinNodeWay = new JoinNodeWayAction();
	public final JosmAction unglueNodes = new UnGlueAction();

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
	public final HistoryInfoAction historyinfo = new HistoryInfoAction();

	public final JMenu fileMenu = new JMenu(tr("File"));
	public final JMenu editMenu = new JMenu(tr("Edit"));
	public final JMenu viewMenu = new JMenu(tr("View"));
	public final JMenu toolsMenu = new JMenu(tr("Tools"));
	public final JMenu audioMenu = new JMenu(tr("Audio"));
	public final JMenu presetsMenu = new JMenu(tr("Presets"));
	public final JMenu helpMenu = new JMenu(tr("Help"));

	/**
	 * Add a JosmAction to a menu.
	 *
	 * This method handles all the shortcut handling.
	 * It also makes sure that actions that are handled by the
	 * OS are not duplicated on the menu.
	 */
	public static void add(JMenu menu, JosmAction action) {
		if (!action.getShortCut().getAutomatic()) {
			JMenuItem menuitem = menu.add(action);
			KeyStroke ks = action.getShortCut().getKeyStroke();
			if (ks != null) {
				menuitem.setAccelerator(ks);
			}
		}
	}

	/**
	 * Add a menu to the main menu.
	 *
	 * This method handles all the shortcut handling.
	 */
	public void add(JMenu menu, int mnemonicKey, String shortName) {
		ShortCut.registerShortCut("menu:"+shortName, tr("Menu: {0}", menu.getText()), mnemonicKey, ShortCut.GROUP_MNEMONIC).setMnemonic(menu);
		add(menu);
	}

	public MainMenu() {
		JMenuItem current;

		add(fileMenu, newAction);
		add(fileMenu, open);
		fileMenu.addSeparator();
		add(fileMenu, save);
		add(fileMenu, saveAs);
		add(fileMenu, gpxExport);
		fileMenu.addSeparator();
		add(fileMenu, download);
		add(fileMenu, upload);
		add(fileMenu, exit);
		add(fileMenu, KeyEvent.VK_F, "file");

		add(editMenu, undo);
		add(editMenu, redo);
		editMenu.addSeparator();
		add(editMenu, copy);
		add(editMenu, delete);
		add(editMenu, paste);
		add(editMenu, pasteTags);
		add(editMenu, duplicate);
		editMenu.addSeparator();
		add(editMenu, selectAll);
		add(editMenu, unselectAll);
		editMenu.addSeparator();
		add(editMenu, search);
		editMenu.addSeparator();
		add(editMenu, preferences);
		add(editMenu, KeyEvent.VK_E, "edit");

		for (String mode : AutoScaleAction.modes) {
			JosmAction autoScaleAction = new AutoScaleAction(mode);
			add(viewMenu, autoScaleAction);
		}
		viewMenu.addSeparator();
		add(viewMenu, new ZoomOutAction());
		add(viewMenu, new ZoomInAction());
		viewMenu.addSeparator();
		// TODO move code to an "action" like the others?
		final JCheckBoxMenuItem wireframe = new JCheckBoxMenuItem(tr("Wireframe view"));
		wireframe.setSelected(Main.pref.getBoolean("draw.wireframe", false));
		wireframe.setAccelerator(ShortCut.registerShortCut("menu:view:wireframe", "Toggle Wireframe view", KeyEvent.VK_W, ShortCut.GROUP_MENU).getKeyStroke());
		wireframe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				Main.pref.put("draw.wireframe", wireframe.isSelected());
				if (Main.map != null) {
					Main.map.mapView.repaint();
				}
			}
		});
		viewMenu.add(wireframe);
		add(viewMenu, KeyEvent.VK_V, "view");

		add(toolsMenu, splitWay);
		add(toolsMenu, combineWay);
		toolsMenu.addSeparator();
		add(toolsMenu, reverseWay);
		toolsMenu.addSeparator();
		add(toolsMenu, alignInCircle);
		add(toolsMenu, alignInLine);
		add(toolsMenu, alignInRect);
		toolsMenu.addSeparator();
		add(toolsMenu, createCircle);
		toolsMenu.addSeparator();
		add(toolsMenu, mergeNodes);
		add(toolsMenu, joinNodeWay);
		add(toolsMenu, unglueNodes);
		add(toolsMenu, KeyEvent.VK_T, "tools");

		if (! Main.pref.getBoolean("audio.menuinvisible")) {
			add(audioMenu, audioPlayPause);
			add(audioMenu, audioNext);
			add(audioMenu, audioPrev);
			add(audioMenu, audioFwd);
			add(audioMenu, audioBack);
			add(audioMenu, audioSlower);
			add(audioMenu, audioFaster);
			add(audioMenu, KeyEvent.VK_A, "audio");
		}

		add(presetsMenu, KeyEvent.VK_P, "presets");

		JMenuItem check = new JMenuItem("DEBUG: Check Dataset");
		check.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				DataSetChecker.check();
			}
		});
		helpMenu.add(check);
		current = helpMenu.add(help); // why is help not a JosmAction?
		current.setAccelerator(ShortCut.registerShortCut("system:help", tr("Help"), KeyEvent.VK_F1, ShortCut.GROUP_DIRECT).getKeyStroke());
		add(helpMenu, about);
		add(helpMenu, historyinfo);
		add(helpMenu, KeyEvent.VK_H, "help");
    }
}
