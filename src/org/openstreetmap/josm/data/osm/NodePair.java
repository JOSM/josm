package org.openstreetmap.josm.data.osm;
import java.util.ArrayList;

/**
 * A pair of twe nodes.
 */
public final class NodePair {
	public Node a, b;

	public NodePair(Node a, Node b) {
		this.a = a;
		this.b = b;
	}

	@Override public int hashCode() {
		return a.hashCode() ^ b.hashCode();
	}

	@Override public boolean equals(Object o) {
		if (o == null || !(o instanceof NodePair)) {
			return false;
		}
		return a == ((NodePair) o).a && b == ((NodePair) o).b;
	}

	public ArrayList<Node> toArrayList() {
		ArrayList<Node> l = new ArrayList<Node>(2);
		l.add(a);
		l.add(b);
		return l;
	}

	public NodePair sort() {
		if (b.hashCode() < a.hashCode()) {
			Node tmp = a;
			a = b;
			b = tmp;
		}
		return this;
	}
}
