package org.openstreetmap.josm.data.osm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.tools.Pair;

import java.nio.MappedByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Map.Entry;
import java.awt.geom.Point2D;

//import java.util.List;
import java.util.*;
import java.util.Collection;

import org.openstreetmap.josm.data.coor.QuadTiling;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


public class QuadBuckets implements Collection<Node>
{
    public static boolean debug = false;
    static boolean consistency_testing = false;

    static void abort(String s)
    {
        out(s);
        Object o = null;
        o.hashCode();
    }
    static void out(String s)
    {
        System.out.println(s);
    }
    // periodic output
    long last_out = -1;
    void pout(String s)
    {
        long now = System.currentTimeMillis();
        if (now - last_out < 300)
            return;
        last_out = now;
        System.out.print(s + "\r");
    }
    void pout(String s, int i, int total)
    {
        long now = System.currentTimeMillis();
        if ((now - last_out < 300) &&
            ((i+1) < total))
            return;
        last_out = now;
        // cast to float to keep the output size down
        System.out.print(s + " " + (float)((i+1)*100.0/total) + "% done    \r");
    }
    // 24 levels of buckets gives us bottom-level
    // buckets that are about 2 meters on a side.
    // That should do. :)
    public static int NR_LEVELS = 24;
    public static double WORLD_PARTS = (1 << NR_LEVELS);

    public static int MAX_OBJECTS_PER_LEVEL = 16;
    // has to be a power of 2
    public static int TILES_PER_LEVEL_SHIFT = 2;
    public static int TILES_PER_LEVEL = 1<<TILES_PER_LEVEL_SHIFT;
    class BBox
    {
        private double xmin;
        private double xmax;
        private double ymin;
        private double ymax;
        void sanity()
        {
            if (xmin < -180.0)
                xmin = -180.0;
            if (xmax >  180.0)
                xmax =  180.0;
            if (ymin <  -90.0)
                ymin =  -90.0;
            if (ymax >   90.0)
                ymax =   90.0;
            if ((xmin < -180.0) ||
                (xmax >  180.0) ||
                (ymin <  -90.0) ||
                (ymax >   90.0)) {
                out("bad BBox: " + this);
                Object o = null;
                o.hashCode();
            }
        }
        public String toString()
        {
            return "[ " + xmin + " -> " + xmax + ", " +
                         ymin + " -> " + ymax + " ]";
        }
        double min(double a, double b)
        {
            if (a < b)
                return a;
            return b;
        }
        double max(double a, double b)
        {
            if (a > b)
                return a;
            return b;
        }
        public BBox(LatLon a, LatLon b)
        {
            xmin = min(a.lon(), b.lon());
            xmax = max(a.lon(), b.lon());
            ymin = min(a.lat(), b.lat());
            ymax = max(a.lat(), b.lat());
            sanity();
        }
        public BBox(double a_x, double a_y, double b_x, double b_y)
        {
            xmin = min(a_x, b_x);
            xmax = max(a_x, b_x);
            ymin = min(a_y, b_y);
            ymax = max(a_y, b_y);
            sanity();
        }
        public double height()
        {
            return ymax-ymin;
        }
        public double width()
        {
            return xmax-xmin;
        }
        boolean bounds(BBox b)
        {
            if (!(xmin <= b.xmin) ||
                !(xmax >= b.xmax) ||
                !(ymin <= b.ymin) ||
                !(ymax >= b.ymax))
                return false;
            return true;
        }
        boolean bounds(LatLon c)
        {
            if ((xmin <= c.lon()) &&
                (xmax >= c.lon()) &&
                (ymin <= c.lat()) &&
                (ymax >= c.lat()))
                return true;
            return false;
        }
        boolean inside(BBox b)
        {
            if (xmin >= b.xmax)
                return false;
            if (xmax <= b.xmin)
                return false;
            if (ymin >= b.ymax)
                return false;
            if (ymax <= b.ymin)
                return false;
            return true;
        }
        boolean intersects(BBox b)
        {
            return this.inside(b) || b.inside(this);
        }
    }
    class QBLevel
    {
        int level;
        long quad;
        QBLevel parent;

        public List<Node> content;
        public QBLevel children[];
        public QBLevel(QBLevel parent)
        {
            init(parent);
        }
        String quads(Node n)
        {
            return Long.toHexString(QuadTiling.quadTile(n.getCoor()));
        }
        void split()
        {
            if (debug)
                out("splitting "+this.bbox()+" level "+level+" with "
                        + content.size() + " entries (my dimensions: "
                        + this.bbox.width()+", "+this.bbox.height()+")");
            if (children != null) {
                abort("overwrote children");
            }
            children = new QBLevel[QuadTiling.TILES_PER_LEVEL];
            // deferring allocation of children until use
            // seems a bit faster
            //for (int i = 0; i < TILES_PER_LEVEL; i++)
            //    children[i] = new QBLevel(this, i);
            List<Node> tmpcontent = content;
            content = null;
            for (Node n : tmpcontent) {
                int new_index = QuadTiling.index(n, level);
                if (children[new_index] == null)
                    children[new_index] = new QBLevel(this, new_index);
                QBLevel child = children[new_index];
                if (debug)
                    out("putting "+n+"(q:"+quads(n)+") into ["+new_index+"] " + child.bbox());
                child.add(n);
            }
        }
        void add_leaf(Node n)
        {
            LatLon c = n.getCoor();
            QBLevel ret = this;
            if (content == null) {
                if (debug)
                    out("   new content array");
                // I chose a LinkedList because we do not do
                // any real random access to this.  We generally
                // just run through the whole thing all at once
                content = new LinkedList<Node>();
            }
            content.add(n);
            if (content.size() > MAX_OBJECTS_PER_LEVEL) {
                if (debug)
                    out("["+level+"] deciding to split");
                if (level >= NR_LEVELS-1) {
                    out("can not split, but too deep: " + level + " size: " + content.size());
                    return;
                }
                int before_size = -1;
                if (consistency_testing)
                    before_size = this.size();
                this.split();
                if (consistency_testing) {
                    int after_size = this.size();
                    if (before_size != after_size) {
                        abort("["+level+"] split done before: " + before_size + " after: " + after_size);
                    }
                }
                return;
            }
            if (debug) {
                out("   plain content put now have " + content.size());
                if (content.contains(c))
                    out("   and I already have that one");
            }
        }

        List<Node> search(BBox bbox, LatLon point, double radius)
        {
            if (isLeaf())
                return search_leaf(bbox, point, radius);
            return search_branch(bbox, point, radius);
        }
        private List<Node> search_leaf(BBox bbox, LatLon point, double radius)
        {
            if (debug)
                out("searching contents in tile " + this.bbox() + " for " + bbox);
            /*
             * It is possible that this was created in a split
             * but never got any content populated.
             */
            if (content == null)
                return null;
            // We could delay allocating this.  But, the way it is currently
            // used, we almost always have a node in the area that we're
            // searching since we're searching around other nodes.
            //
            // the iterator's calls to ArrayList.size() were showing up in
            // some profiles, so I'm trying a LinkedList instead
            List<Node> ret = new LinkedList<Node>();
            for (Node n : content) {
                // This supports two modes: one where the bbox is just
                // an outline of the point/radius combo, and one where
                // it is *just* the bbox.  If it is *just* the bbox,
                // don't consider the points below
                if (point == null) {
                    ret.add(n);
                    continue;
                }
                LatLon c = n.getCoor();
                // is greatCircleDistance really necessary?
                double d = c.greatCircleDistance(point);
                if (debug)
                    out("[" + level + "] Checking coor: " + c + " dist: " + d + " vs. " + radius + " " + quads(n));
                if (d > radius)
                    continue;
                if (debug)
                    out("hit in quad: "+Long.toHexString(this.quad)+"\n");
                //if (ret == null)
                //    ret = new LinkedList<Node>();
                ret.add(n);
            }
            if (debug)
                out("done searching quad " + Long.toHexString(this.quad));
            return ret;
        }
        /*
         * This is stupid.  I tried to have a QBLeaf and QBBranch
         * class decending from a QBLevel.  It's more than twice
         * as slow.  So, this throws OO out the window, but it
         * is fast.  Runtime type determination must be slow.
         */
        boolean isLeaf()
        {
            if (children == null)
                return true;
            return false;
        }
        QBLevel next_sibling()
        {
            boolean found_me = false;
            if (parent == null)
                return null;
            int __nr = 0;
            for (QBLevel sibling : parent.children) {
                __nr++;
                int nr = __nr-1;
                if (sibling == null) {
                    if (debug)
                        out("[" + this.level + "] null child nr: " + nr);
                    continue;
                }
                // We're looking for the *next* child
                // after us.
                if (sibling == this) {
                    if (debug)
                        out("[" + this.level + "] I was child nr: " + nr);
                    found_me = true;
                    continue;
                }
                if (found_me) {
                    if (debug)
                        out("[" + this.level + "] next sibling was child nr: " + nr);
                    return sibling;
                }
                if (debug)
                    out("[" + this.level + "] nr: " + nr + " is before me, ignoring...");
            }
            return null;
        }
        QBLevel nextLeaf()
        {
            QBLevel next = this;
            if (this.isLeaf()) {
                QBLevel sibling = next.next_sibling();
                // Walk back up the tree to find the
                // next sibling node.  It may be either
                // a leaf or branch.
                while (sibling == null) {
                    if (debug)
                        out("no siblings at level["+next.level+"], moving to parent");
                    next = next.parent;
                    if (next == null)
                        break;
                    sibling = next.next_sibling();
                }
                next = sibling;
            }
            if (next == null)
                return null;
            // all branches are guaranteed to have
            // at least one leaf.  It may not have
            // any contents, but there *will* be a
            // leaf.  So, walk back down the tree
            // and find the first leaf
            while (!next.isLeaf()) {
                if (debug)
                    out("["+next.level+"] next node is a branch, moving down...");
                for (QBLevel child : next.children) {
                    if (child == null)
                        continue;
                    next = child;
                    break;
                }
            }
            return next;
        }
        int size()
        {
            if (isLeaf())
                return size_leaf();
            return size_branch();
        }
        private int size_leaf()
        {
            if (content == null) {
                if (debug)
                    out("["+level+"] leaf size: null content, children? " + children);
                return 0;
            }
            if (debug)
                out("["+level+"] leaf size: " + content.size());
            return content.size();
        }
        private int size_branch()
        {
            int ret = 0;
            for (QBLevel l : children) {
                if (l == null)
                    continue;
                ret += l.size();
            }
            if (debug)
                out("["+level+"] branch size: " + ret);
            return ret;
        }
        boolean contains(Node n)
        {
            QBLevel res = find_exact(n);
            if (res == null)
                return false;
            return true;
        }
        QBLevel find_exact(Node n)
        {
            if (isLeaf())
                return find_exact_leaf(n);
            return find_exact_branch(n);
        }
        QBLevel find_exact_leaf(Node n)
        {
            QBLevel ret = null;
            if (content != null && content.contains(n))
                ret = this;
            return ret;
        }
        QBLevel find_exact_branch(Node n)
        {
            QBLevel ret = null;
            for (QBLevel l : children) {
                if (l == null)
                    continue;
                ret = l.find_exact(n);
                if (ret != null)
                    break;
            }
            return ret;
        }
        boolean add(Node n)
        {
            if (isLeaf())
                add_leaf(n);
            else
                add_branch(n);
            return true;
        }
        QBLevel add_branch(Node n)
        {
            LatLon c = n.getCoor();
            int index = QuadTiling.index(n, level);
            if (debug)
                out("[" + level + "]: " + n +
                    " index " + index + " levelquad:" + this.quads() + " level bbox:" + this.bbox());
            if (debug)
                out("   put in child["+index+"]");
            if (children[index] == null)
                children[index] = new QBLevel(this, index);
            children[index].add(n);
            /* this is broken at the moment because we need to handle the null n.getCoor()
            if (consistency_testing && !children[index].bbox().bounds(n.getCoor())) {
                out("target child["+index+"] does not bound " + children[index].bbox() + " " + c);
                for (int i = 0; i < children.length; i++) {
                    QBLevel ch = children[i];
                    if (ch == null)
                        continue;
                    out("child["+i+"] quad: " + ch.quads() + " bounds: " + ch.bbox.bounds(n.getCoor()));
                }
                out(" coor quad: " + quads(n));
                abort("");
            }*/

            if (consistency_testing) {
                for (int i = 0; i < children.length; i++) {
                    QBLevel ch = children[i];
                    if (ch == null)
                        continue;
                    if (ch.bbox().bounds(c) && i != index) {
                        out("multible bounding?? expected: " + index + " but saw at " + i + " " + ch.quads());
                        out("child["+i+"] bbox: " + ch.bbox());
                    }
                }
            }
            return this;
        }
        private List<Node> search_branch(BBox bbox, LatLon point, double radius)
        {
            List<Node> ret = null;
            if (debug)
                System.out.print("[" + level + "] qb bbox: " + this.bbox() + " ");
            if (!this.bbox().intersects(bbox)) {
                if (debug) {
                    out("miss " + Long.toHexString(this.quad));
                    //QuadTiling.tile2xy(this.quad);
                }
                return ret;
            }
            if (debug)
                out("hit " + this.quads());

            if (debug)
                out("[" + level + "] not leaf, moving down");
            int child_nr = 0;
            for (QBLevel q : children) {
                if (q == null)
                    continue;
                child_nr++;
                if (debug)
                    System.out.print(child_nr+": ");
                List<Node> coors = q.search(bbox, point, radius);
                if (coors == null)
                    continue;
                if (ret == null)
                    ret = coors;
                else
                    ret.addAll(coors);
                if (q.bbox().bounds(bbox)) {
                    search_cache = q;
                    // optimization: do not continue searching
                    // other tiles if this one wholly contained
                    // what we were looking for.
                    if (coors.size() > 0 ) {
                        if (debug)
                            out("break early");
                        break;
                    }
                }
            }
            return ret;
        }
        public String quads()
        {
            return Long.toHexString(quad);
        }
        public void init(QBLevel parent)
        {
                this.parent = parent;
                if (parent != null)
                    this.level = parent.level + 1;
                this.quad = 0;
        }
        int index_of(QBLevel find_this)
        {
            if (this.isLeaf())
                return -1;

            for (int i = 0; i < QuadTiling.TILES_PER_LEVEL; i++) {
                if (children[i] == find_this)
                    return i;
            }
            return -1;
        }
        public QBLevel(QBLevel parent, int parent_index)
        {
            this.init(parent);
            this.level = parent.level+1;
            this.parent = parent;
            int shift = (QuadBuckets.NR_LEVELS - level) * 2;
            long mult = 1;
            // Java blows the big one.  It seems to wrap when
            // you shift by > 31
            if (shift >= 30) {
                shift -= 30;
                mult = 1<<30;
            }
            long this_quadpart = mult * (parent_index << shift);
            this.quad = parent.quad | this_quadpart;
            if (debug)
                out("new level["+this.level+"] bbox["+parent_index+"]: " + this.bbox()
                        + " coor: " + this.coor()
                        + " quadpart: " + Long.toHexString(this_quadpart)
                        + " quad: " + Long.toHexString(this.quad));
        }
        /*
         * Surely we can calculate these efficiently instead of storing
         */
        double width = Double.NEGATIVE_INFINITY;
        double width()
        {
            if (width != Double.NEGATIVE_INFINITY)
                return this.width;
            if (level == 0) {
                width = this.bbox().width();
            } else {
                width = parent.width()/2;
            }
            return width;
        }
        double height()
        {
            return width()/2;
        }
        BBox bbox = null;
        public BBox bbox()
        {
            if (bbox != null) {
                bbox.sanity();
                return bbox;
            }
            if (level == 0) {
                bbox = new BBox(-180, 90, 180, -90);
            } else {
                LatLon bottom_left = this.coor();
                double lat = bottom_left.lat() + this.height();
                double lon = bottom_left.lon() + this.width();
                LatLon top_right = new LatLon(lat, lon);
                bbox = new BBox(bottom_left, top_right);
            }
            bbox.sanity();
            return bbox;
        }
        /*
         * This gives the coordinate of the bottom-left
         * corner of the box
         */
        LatLon coor()
        {
            return QuadTiling.tile2LatLon(this.quad);
        }
    }

    private QBLevel root;
    private QBLevel search_cache;
    public QuadBuckets()
    {
        clear();
    }
    public void clear()
    {
        root = new QBLevel(null);
        search_cache = null;
        if (debug)
            out("QuadBuckets() cleared: " + this);
    }
    public boolean add(Node n)
    {
        if (debug)
            out(this + " QuadBuckets() add: " + n + " size now: " + this.size());
        int before_size = -1;
        if (consistency_testing)
            before_size = root.size();
        boolean ret = root.add(n);
        if (consistency_testing) {
            int end_size = root.size();
            if (ret)
                end_size--;
            if (before_size != end_size)
                abort("size inconsistency before add: " + before_size + " after: " + end_size);
        }
        return ret;
    }
    public void unsupported()
    {
        out("unsupported operation");
        Object o = null;
        o.hashCode();
        throw new UnsupportedOperationException();
    }
    public boolean retainAll(Collection nodes)
    {
        unsupported();
        return false;
    }
    public boolean removeAll(Collection nodes)
    {
        unsupported();
        return false;
    }
    public boolean addAll(Collection nodes)
    {
        unsupported();
        return false;
    }
    public boolean containsAll(Collection nodes)
    {
        unsupported();
        return false;
    }
    private void check_type(Object o)
    {
        if (o instanceof Node)
            return;
        unsupported();
    }
    public boolean remove(Object o)
    {
        check_type(o);
        return this.remove((Node)o);
    }
    public boolean remove(Node n)
    {
        QBLevel bucket = root.find_exact(n);
        if (!bucket.isLeaf())
            abort("found branch where leaf expected");
        return bucket.content.remove(n);
    }
    public boolean contains(Object o)
    {
        check_type(o);
        QBLevel bucket = root.find_exact((Node)o);
        if (bucket == null)
            return false;
        return true;
    }
    public ArrayList<Node> toArrayList()
    {
        ArrayList<Node> a = new ArrayList<Node>();
        for (Node n : this)
            a.add(n);
        out("returning array list with size: " + a.size());
        return a;
    }
    public Object[] toArray()
    {
        return this.toArrayList().toArray();
    }
    public <T> T[] toArray(T[] template)
    {
        return this.toArrayList().toArray(template);
    }
    class QuadBucketIterator implements Iterator<Node>
    {
        QBLevel current_leaf;
        int index_in_leaf;
        int iterated_over;
        QBLevel next_leaf(QBLevel q)
        {
            if (q == null)
                return null;
            QBLevel orig = q;
            QBLevel next = q.nextLeaf();
            if (consistency_testing && (orig == next))
                abort("got same leaf back leaf: " + q.isLeaf());
            return next;
        }
        public QuadBucketIterator(QuadBuckets qb)
        {
            if (debug)
                out(this + " is a new iterator qb.root: " + qb.root + " size: " + qb.size());
            if (qb.root.isLeaf())
                current_leaf = qb.root;
            else
                current_leaf = next_leaf(qb.root);
            if (debug)
                out("\titerator first leaf: " + current_leaf);
            iterated_over = 0;
        }
        public boolean hasNext()
        {
            if (this.peek() == null) {
                if (debug)
                   out(this + " no hasNext(), but iterated over so far: " + iterated_over);
                return false;
            }
            return true;
        }
        Node peek()
        {
            if (current_leaf == null) {
                if (debug)
                    out("null current leaf, nowhere to go");
                return null;
            }
            while((current_leaf.content == null) ||
                  (index_in_leaf >= current_leaf.content.size())) {
                if (debug)
                    out("moving to next leaf");
                index_in_leaf = 0;
                current_leaf = next_leaf(current_leaf);
                if (current_leaf == null)
                    break;
            }
            if (current_leaf == null || current_leaf.content == null) {
                if (debug)
                    out("late nowhere to go " + current_leaf);
                return null;
            }
            return current_leaf.content.get(index_in_leaf);
        }
        public Node next()
        {
            Node ret = peek();
            index_in_leaf++;
            if (debug)
                out("iteration["+iterated_over+"] " + index_in_leaf + " leaf: " + current_leaf);
            iterated_over++;
            if (ret == null) {
                if (debug)
                    out(this + " no next node, but iterated over so far: " + iterated_over);
            }
            return ret;
        }
        public void remove()
        {
            Node object = peek();
            current_leaf.content.remove(object);
        }
    }
    public Iterator<Node> iterator()
    {
        return new QuadBucketIterator(this);
    }
    public int size()
    {
        // This can certainly by optimized
        int ret = root.size();
        if (debug)
            out(this + " QuadBuckets size: " + ret);
        return ret;
    }
    public boolean isEmpty()
    {
        if (this.size() == 0)
            return true;
        return false;
    }
    public BBox search_to_bbox(LatLon point, double radius)
    {
        BBox bbox = new BBox(point.lon() - radius, point.lat() - radius,
                             point.lon() + radius, point.lat() + radius);
        if (debug)
            out("search bbox before sanity: " +  bbox);
        bbox.sanity();
        if (debug)
            out("search bbox after sanity: " +  bbox);
        return bbox;
    }
    List<Node> search(LatLon point, double radius)
    {
        return this.search(search_to_bbox(point, radius), point, radius);
    }
    List<Node> search(BBox bbox)
    {
        return this.search(bbox, null, -1);
    }
    public List<Node> search(LatLon b1, LatLon b2)
    {
        BBox bbox = new BBox(b1.lon(), b1.lat(), b2.lon(), b2.lat());
        bbox.sanity();
        return this.search(bbox);
    }
    List<Node> search(BBox bbox, LatLon point, double radius)
    {
        if (debug) {
            out("qb root search at " + point + " around: " + radius);
            out("root bbox: " + root.bbox());
        }
        List<Node> ret = null;
        // Doing this cuts down search cost on a real-life data
        // set by about 25%
        boolean cache_searches = true;
        if (cache_searches) {
            if (search_cache == null)
                search_cache = root;
            // Walk back up the tree when the last
            // search spot can not cover the current
            // search
            while (!search_cache.bbox().bounds(bbox)) {
                search_cache = search_cache.parent;
            }
        } else {
            search_cache = root;
        }
        return search_cache.search(bbox, point, radius);
    }
}
