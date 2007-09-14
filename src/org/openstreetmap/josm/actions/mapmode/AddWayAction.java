// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ReorderAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Add a new way. The action is split into the first phase, where a new way get
 * created or selected and the second, where this way is modified.
 *
 * Way creation mode:
 * If there is a selection when the mode is entered, all segments in this
 * selection form a new way. All non-segment objects are deselected. If there
 * were ways selected, the user is asked whether to select all segments of these
 * ways or not, except there is exactly one way selected, which enter the
 * edit ways mode for this way immediatly.
 * 
 * If there is no selection on entering, and the user clicks on an segment, 
 * the way editing starts the with a new way and this segment. If the user click
 * on a way (not holding Alt down), then this way is edited in the way edit mode.
 *
 * Way editing mode:
 * The user can click on subsequent segments. If the segment belonged to the way
 * it get removed from the way. Elsewhere it get added to the way. JOSM try to add
 * the segment in the correct position. This is done by searching for connections
 * to the segment at its 'to' node which are also in the way. The segemnt is 
 * inserted in the way as predecessor of the found segment (or at last segment, if
 * nothing found). 
 *
 * @author imi
 */
public class AddWayAction extends MapMode implements SelectionChangedListener {
	private Way way;

	/**
	 * Create a new AddWayAction.
	 * @param mapFrame The MapFrame this action belongs to.
	 * @param followMode The mode to go into when finished creating a way.
	 */
	public AddWayAction(MapFrame mapFrame) {
		super(tr("Add Way"), "addway", tr("Add a new way to the data."), KeyEvent.VK_W, mapFrame, ImageProvider.getCursor("normal", "way"));
		DataSet.listeners.add(this);
	}

	@Override public void enterMode() {
		super.enterMode();
		way = makeWay();
		Main.ds.setSelected(way);
		Main.map.mapView.addMouseListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		way = null;
		Main.map.mapView.removeMouseListener(this);
	}

	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		Segment s = Main.map.mapView.getNearestSegment(e.getPoint());
		if (s == null)
			return;

		// special case for initial selecting one way
		if (way == null && (e.getModifiers() & MouseEvent.ALT_DOWN_MASK) == 0) {
			Way w = Main.map.mapView.getNearestWay(e.getPoint());
			if (w != null) {
				way = w;
				Main.ds.setSelected(way);
				for (Segment seg : way.segments) {
					if (seg.incomplete) {
						JOptionPane.showMessageDialog(Main.parent,tr("Warning: This way is incomplete. Try to download it before adding segments."));
						return;
					}
				}
				return;
			}
		}

		if (way != null && way.segments.contains(s)) {
			Way copy = new Way(way);

			copy.segments.remove(s);
			if (copy.segments.isEmpty()) {
				Main.main.undoRedo.add(new DeleteCommand(Arrays.asList(new OsmPrimitive[]{way})));
				way = null;
			} else
				Main.main.undoRedo.add(new ChangeCommand(way, copy));
		} else {
			if (way == null) {
				way = new Way();
				way.segments.add(s);
				Main.main.undoRedo.add(new AddCommand(way));
			} else {
				Way copy = new Way(way);
				int i;
				for (i = 0; i < way.segments.size(); ++i)
					if (way.segments.get(i).from == s.to)
						break;
				copy.segments.add(i, s);
				Main.main.undoRedo.add(new ChangeCommand(way, copy));
			}
		}
		Main.ds.setSelected(way);
	}

	/**
	 * Form a way, either out of the (one) selected way or by creating a way over the selected
	 * line segments.
	 */
	private Way makeWay() {
		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		if (selection.isEmpty())
			return null;

		if (selection.size() == 1 && selection.iterator().next() instanceof Way) {
			Way way = (Way)selection.iterator().next();
			for (Segment seg : way.segments) {
				if (seg.incomplete) {
					JOptionPane.showMessageDialog(Main.parent, tr("Warning: This way is incomplete. Try to download it before adding segments."));
					break;
				}
			}
			return way;
		}

		HashSet<Segment> segmentSet = new HashSet<Segment>();
		int numberOfSelectedWays = 0;
		for (OsmPrimitive osm : selection) {
			if (osm instanceof Way)
				numberOfSelectedWays++;
			else if (osm instanceof Segment)
				segmentSet.add((Segment)osm);
		}

		Way wayToAdd = null;
		boolean reordered = false;
		if (numberOfSelectedWays > 0) {
			int answer = JOptionPane.showConfirmDialog(Main.parent,trn("{0} way has been selected.\nDo you wish to select all segments belonging to the way instead?","{0} ways have been selected.\nDo you wish to select all segments belonging to the ways instead?",numberOfSelectedWays,numberOfSelectedWays),tr("Add segments from ways"), JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				for (OsmPrimitive osm : selection)
					if (osm instanceof Way)
						segmentSet.addAll(((Way)osm).segments);
			} else if (numberOfSelectedWays == 1) {
				answer = JOptionPane.showConfirmDialog(Main.parent,tr("Do you want to add all other selected segments to the one selected way?"),tr("Add segments to way?"), JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					for (OsmPrimitive osm : selection) {
						if (osm instanceof Way) {
							wayToAdd = (Way)osm;
							answer = JOptionPane.showConfirmDialog(Main.parent,tr("Reorder all line segments?"), tr("Reorder?"), JOptionPane.YES_NO_CANCEL_OPTION);
							if (answer == JOptionPane.CANCEL_OPTION)
								return wayToAdd;
							if (answer == JOptionPane.YES_OPTION) {
								segmentSet.addAll(wayToAdd.segments);
								reordered = true;
							} else
								segmentSet.removeAll(wayToAdd.segments);
							break;
						}
					}
				}
			}
		}

		if (segmentSet.isEmpty())
			return null;

		LinkedList<Segment> rawSegments = new LinkedList<Segment>(segmentSet);
		LinkedList<Segment> sortedSegments = ReorderAction.sortSegments(rawSegments, true);

		if (wayToAdd != null) {
			Way w = new Way(wayToAdd);
			if (reordered)
				w.segments.clear();
			w.segments.addAll(sortedSegments);
			Main.main.undoRedo.add(new ChangeCommand(wayToAdd, w));
			return wayToAdd;
		}

		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(Main.parent,trn("Create a new way out of {0} segment?","Create a new way out of {0} segments?",sortedSegments.size(),sortedSegments.size()), tr("Create new way"), JOptionPane.YES_NO_OPTION))
			return null;

		Way w = new Way();
		w.segments.addAll(sortedSegments);
		Main.main.undoRedo.add(new AddCommand(w));
		return w;
	}

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		if (newSelection.size() == 1) {
			OsmPrimitive osm = newSelection.iterator().next();
			way = osm instanceof Way ? (Way)osm : null;
		} else
			way = null;
    }
}
