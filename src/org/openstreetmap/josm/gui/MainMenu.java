// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DuplicateAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.actions.NewAction;
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PasteAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UnselectAllAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.DataSetChecker;

/**
 * This is the JOSM main menu bar. It is overwritten to initialize itself and provide
 * all menu entries as member variables (sort of collect them).
 *
 * It also provides possibilities to attach new menu entries (used by plugins).
 *
 * @author Immanuel.Scholz
 */
public class MainMenu extends JMenuBar {

	public final UndoAction undo = new UndoAction();
	public final RedoAction redo = new RedoAction();
	public final Action copy = new CopyAction();
	public final Action paste = new PasteAction();
	public final Action duplicate = new DuplicateAction(); 
	public final Action selectAll = new SelectAllAction();
	public final Action unselectAll = new UnselectAllAction();
	public final Action search = new SearchAction();
	public final NewAction newAction = new NewAction();
	public final OpenAction open = new OpenAction();
	public final DownloadAction download = new DownloadAction();
	public final Action reverseWay = new ReverseWayAction();
	public final Action splitWay = new SplitWayAction();
	public final Action combineWay = new CombineWayAction();
	public final Action alignInCircle = new AlignInCircleAction();
	public final Action alignInLine = new AlignInLineAction();
	public final Action upload = new UploadAction();
	public final Action save = new SaveAction(null);
	public final Action saveAs = new SaveAsAction(null);
	public final Action gpxExport = new GpxExportAction(null);
	public final Action exit = new ExitAction();
	public final Action preferences = new PreferencesAction();
	public final HelpAction help = new HelpAction();
	public final Action about = new AboutAction();
	
	public final JMenu layerMenu = new JMenu(tr("Layer"));
	public final JMenu editMenu = new JMenu(tr("Edit"));
	public final JMenu viewMenu = new JMenu(tr("View"));
	public final JMenu helpMenu = new JMenu(tr("Help"));
	public final JMenu fileMenu = new JMenu(tr("Files"));
	public final JMenu connectionMenu = new JMenu(tr("Connection"));
	public final JMenu toolsMenu = new JMenu(tr("Tools"));
	public final JMenu presetsMenu = new JMenu(tr("Presets"));

	public final JMenu zoomToMenu = new JMenu(tr("Zoom To"));


	public MainMenu() {
		fileMenu.setMnemonic('F');
		fileMenu.add(newAction);
		fileMenu.add(open);
		fileMenu.add(save);
		fileMenu.add(saveAs);
		fileMenu.add(gpxExport);
		fileMenu.addSeparator();
		fileMenu.add(exit);
		add(fileMenu);

		editMenu.setMnemonic('E');
		editMenu.add(undo);
		editMenu.add(redo);
		editMenu.addSeparator();
		editMenu.add(copy);
		editMenu.add(paste);
		editMenu.add(duplicate);
		
		editMenu.addSeparator();
		editMenu.add(selectAll);
		editMenu.add(unselectAll);
		editMenu.addSeparator();
		editMenu.add(search);
		editMenu.addSeparator();
		editMenu.add(preferences);
		add(editMenu);
		
		viewMenu.setMnemonic('V');
		viewMenu.setVisible(false);
		viewMenu.add(zoomToMenu);
		for (String mode : AutoScaleAction.modes)
			zoomToMenu.add(new AutoScaleAction(mode));
		add(viewMenu);

		toolsMenu.setMnemonic('T');
		toolsMenu.add(alignInCircle);
		toolsMenu.add(alignInLine);
		toolsMenu.addSeparator();
		toolsMenu.add(reverseWay);
		toolsMenu.addSeparator();
		toolsMenu.add(splitWay);
		toolsMenu.add(combineWay);
		add(toolsMenu);

		connectionMenu.setMnemonic('C');
		connectionMenu.add(download);
		connectionMenu.add(upload);
		add(connectionMenu);

		layerMenu.setMnemonic('L');
		add(layerMenu);
		layerMenu.setVisible(false);

		add(presetsMenu);
		presetsMenu.setMnemonic('P');
		
		JMenuItem check = new JMenuItem("DEBUG: Check Dataset");
		check.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				DataSetChecker.check();
            }
		});
		helpMenu.add(check);

		helpMenu.setMnemonic('H');
		helpMenu.add(help);
		helpMenu.add(about);
		add(helpMenu);
		
    }
}
