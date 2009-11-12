package org.openstreetmap.josm.data.osm;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.QuadTiling;


public class QuadBuckets<T extends OsmPrimitive> implements Collection<T>
{
    public static boolean debug = false;
    static boolean consistency_testing = false;
    /*
     * Functions prefixed with __ need locking before
     * being called.
     */
    private Object split_lock = new Object();

    static void abort(String s)
    {
        throw new AssertionError(s);
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
    // Maybe this should just be a Rectangle??
    public static class BBox
    {
        private double xmin = Double.POSITIVE_INFINITY;
        private double xmax = Double.NEGATIVE_INFINITY;
        private double ymin = Double.POSITIVE_INFINITY;
        private double ymax = Double.NEGATIVE_INFINITY;
        void sanity()
        {
            if (xmin < -180.0) {
                xmin = -180.0;
            }
            if (xmax >  180.0) {
                xmax =  180.0;
            }
            if (ymin <  -90.0) {
                ymin =  -90.0;
            }
            if (ymax >   90.0) {
                ymax =   90.0;
            }
            if ((xmin < -180.0) ||
                    (xmax >  180.0) ||
                    (ymin <  -90.0) ||
                    (ymax >   90.0))
                throw new IllegalArgumentException("bad BBox: " + this);
        }
        @Override
        public String toString()
        {
            return "[ x: " + xmin + " -> " + xmax +
            ", y: " + ymin + " -> " + ymax + " ]";
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
        private void add(LatLon c)
        {
            xmin = min(xmin, c.lon());
            xmax = max(xmax, c.lon());
            ymin = min(ymin, c.lat());
            ymax = max(ymax, c.lat());
        }
        public BBox(LatLon a, LatLon b)
        {
            add(a);
            add(b);
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
        public BBox(Way w)
        {
            for (Node n : w.getNodes()) {
                LatLon coor = n.getCoor();
                if (coor == null) {
                    continue;
                }
                add(coor);
            }
            this.sanity();
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
        List<LatLon> points()
        {
            LatLon p1 = new LatLon(ymin, xmin);
            LatLon p2 = new LatLon(ymin, xmax);
            LatLon p3 = new LatLon(ymax, xmin);
            LatLon p4 = new LatLon(ymax, xmax);
            List<LatLon> ret = new ArrayList<LatLon>();
            ret.add(p1);
            ret.add(p2);
            ret.add(p3);
            ret.add(p4);
            return ret;
        }
    }
    class QBLevel
    {
        int level;
        long quad;
        QBLevel parent;

        public List<T> content;
        public QBLevel children[];
        @Override
        public String toString()
        {
            return super.toString()+ "["+level+"]: " + bbox;
        }
        public QBLevel(QBLevel parent)
        {
            init(parent);
        }
        synchronized boolean remove_content(T o)
        {
            boolean ret = this.content.remove(o);
            if (this.content.size() == 0) {
                this.content = null;
            }
            if (this.canRemove()) {
                this.remove_from_parent();
            }
            return ret;
        }
        QBLevel[] newChildren()
        {
            // This is ugly and hackish.  But, it seems to work,
            // and using an ArrayList here seems to cost us
            // a significant performance penalty -- 50% in my
            // testing.  Child access is one of the single
            // hottest code paths in this entire class.
            return (QBLevel[])Array.newInstance(this.getClass(), QuadTiling.TILES_PER_LEVEL);
        }
        // Get the correct index for the given primitive
        // at the given level.  If the primitive can not
        // fit into a single quad at this level, return -1
        int get_index(T o, int level)
        {
            if (debug) {
                out("getting index for " + o + " at level: " + level);
            }
            int index = -1;
            for (LatLon c : o.getBBox().points()) {
                if (debug) {
                    out("getting index for point: " + c);
                }
                if (index == -1) {
                    index = QuadTiling.index(c, level);
                    if (debug) {
                        out("set initial index to: " + index);
                    }
                    continue;
                }
                int another_index = QuadTiling.index(c, level);
                if (debug) {
                    out("other point index: " + another_index);
                }
                if (another_index != index) {
                    // This happens at level 0 sometimes when a way has no
                    // nodes present.
                    if (debug) {
                        out("primitive ("+o.getId()+") would have gone across two quads "
                                + another_index + "/" + index + " at level: " + level + "    ");
                    }
                    return -1;
                }
            }
            return index;
        }
        /*
         * There is a race between this and qb.nextContentNode().
         * If nextContentNode() runs into this bucket, it may
         * attempt to null out 'children' because it thinks this
         * is a dead end.
         */
        void __split()
        {
            if (debug) {
                out("splitting "+this.bbox()+" level "+level+" with "
                        + content.size() + " entries (my dimensions: "
                        + this.bbox.width()+", "+this.bbox.height()+")");
            }
            if (children != null) {
                abort("overwrote children");
            }
            children = newChildren();
            // deferring allocation of children until use
            // seems a bit faster
            //for (int i = 0; i < TILES_PER_LEVEL; i++)
            //    children[i] = new QBLevel(this, i);
            List<T> tmpcontent = content;
            content = null;
            for (T o : tmpcontent) {
                int new_index = get_index(o, level);
                if (new_index == -1) {
                    this.__add_content(o);
                    //out("adding content to branch: " + this);
                    continue;
                }
                if (children[new_index] == null) {
                    children[new_index] = new QBLevel(this, new_index);
                }
                QBLevel child = children[new_index];
                if (debug) {
                    out("putting "+o+"(q:"+Long.toHexString(QuadTiling.quadTile(o.getBBox().points().get(0)))+") into ["+new_index+"] " + child.bbox());
                }
                child.add(o);
            }
        }
        void split() {
            synchronized(split_lock) {
                __split();
            }
        }
        boolean __add_content(T o)
        {
            boolean ret = false;
            // The split_lock will keep two concurrent
            // calls from overwriting content
            if (content == null) {
                content = new ArrayList<T>();
            }
            ret = content.add(o);
            if (debug && !this.isLeaf()) {
                pout("added "+o.getClass().getName()+" to non-leaf with size: " + content.size());
            }
            return ret;
        }
        void __add_to_leaf(T o)
        {
            __add_content(o);
            if (content.size() > MAX_OBJECTS_PER_LEVEL) {
                if (debug) {
                    out("["+level+"] deciding to split");
                }
                if (level >= NR_LEVELS-1) {
                    if (debug) {
                        out("can not split, but too deep: " + level + " size: " + content.size());
                    }
                    return;
                }
                int before_size = -1;
                if (consistency_testing) {
                    before_size = this.size();
                }
                this.split();
                if (consistency_testing) {
                    int after_size = this.size();
                    if (before_size != after_size) {
                        abort("["+level+"] split done before: " + before_size + " after: " + after_size);
                    }
                }
                return;
            }
        }

        boolean matches(T o, BBox search_bbox)
        {
            return o.getBBox().intersects(search_bbox);
        }
        private List<T> search_contents(BBox search_bbox)
        {
            String size = "null";
            if (content != null) {
                size = ""+content.size();
            }
            if (debug) {
                out("searching contents (size: "+size+") for " + search_bbox);
            }
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
            List<T> ret = new LinkedList<T>();
            for (T o : content) {
                if (matches(o, search_bbox)) {
                    ret.add(o);
                }
            }
            if (debug) {
                out("done searching quad " + Long.toHexString(this.quad));
            }
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
                    if (debug) {
                        out("[" + this.level + "] null child nr: " + nr);
                    }
                    continue;
                }
                // We're looking for the *next* child
                // after us.
                if (sibling == this) {
                    if (debug) {
                        out("[" + this.level + "] I was child nr: " + nr);
                    }
                    found_me = true;
                    continue;
                }
                if (found_me) {
                    if (debug) {
                        out("[" + this.level + "] next sibling was child nr: " + nr);
                    }
                    return sibling;
                }
                if (debug) {
                    out("[" + this.level + "] nr: " + nr + " is before me, ignoring...");
                }
            }
            return null;
        }
        boolean hasContent()
        {
            if (content == null)
                return false;
            return true;
        }
        QBLevel nextSibling()
        {
            QBLevel next = this;
            QBLevel sibling = next.next_sibling();
            // Walk back up the tree to find the
            // next sibling node.  It may be either
            // a leaf or branch.
            while (sibling == null) {
                if (debug) {
                    out("no siblings at level["+next.level+"], moving to parent");
                }
                next = next.parent;
                if (next == null) {
                    break;
                }
                sibling = next.next_sibling();
            }
            next = sibling;
            return next;
        }
        QBLevel firstChild()
        {
            QBLevel ret = null;
            for (QBLevel child : this.children) {
                if (child == null)
                    continue;
                ret = child;
                break;
            }
            return ret;
        }
        QBLevel nextContentNode()
        {
            QBLevel next = this;
            if (this.isLeaf()) {
                next = this.nextSibling();
            }
            if (next == null)
                return null;
            // Walk back down the tree and find the first leaf
            while (!next.isLeaf()) {
                QBLevel child;
                if (next.hasContent() && next != this) {
                    break;
                }
                child = next.firstChild();
                if (debug) {
                    out("["+next.level+"] next node ("+next+") is a branch (content: "+next.hasContent()+"), moving down...");
                }
                if (child == null)
                    abort("branch node had no children");
                next = child;
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
                if (debug) {
                    out("["+level+"] leaf size: null content, children? " + Arrays.toString(children));
                }
                return 0;
            }
            if (debug) {
                out("["+level+"] leaf size: " + content.size());
            }
            return content.size();
        }
        private int size_branch()
        {
            int ret = 0;
            for (QBLevel l : children) {
                if (l == null) {
                    continue;
                }
                ret += l.size();
            }
            if (content != null) {
                ret += content.size();
            }
            if (debug) {
                out("["+level+"] branch size: " + ret);
            }
            return ret;
        }
        boolean contains(T n)
        {
            QBLevel res = find_exact(n);
            if (res == null)
                return false;
            return true;
        }
        QBLevel find_exact(T n)
        {
            if (content != null && content.contains(n))
                return this;
            return find_exact_child(n);
        }
        private QBLevel find_exact_child(T n)
        {
            QBLevel ret = null;
            if (children == null)
                return ret;
            for (QBLevel l : children) {
                if (l == null) {
                    continue;
                }
                ret = l.find_exact(n);
                if (ret != null) {
                    break;
                }
            }
            return ret;
        }
        boolean add(T n)
        {
            if (consistency_testing) {
                if (!matches(n, this.bbox())) {
                    out("-----------------------------");
                    debug = true;
                    get_index(n, level);
                    abort("object " + n + " does not belong in node at level: " + level + " bbox: " + this.bbox());
                }
            }
            synchronized (split_lock) {
                if (isLeaf()) {
                    __add_to_leaf(n);
                } else {
                    __add_to_branch(n);
                }
            }
            return true;
        }
        QBLevel __add_to_branch(T n)
        {
            int index = get_index(n, level);
            if (index == -1) {
                if (debug) {
                    out("unable to get index for " + n + "at level: " + level + ", adding content to branch: " + this);
                }
                this.__add_content(n);
                return this;
            }
            if (debug) {
                out("[" + level + "]: " + n +
                        " index " + index + " levelquad:" + this.quads() + " level bbox:" + this.bbox());
                out("   put in child["+index+"]");
            }
            if (children[index] == null) {
                children[index] = new QBLevel(this, index);
            }
            children[index].add(n);
            return this;
        }
        private List<T> search(BBox search_bbox)
        {
            List<T> ret = null;
            if (debug) {
                System.out.print("[" + level + "] qb bbox: " + this.bbox() + " ");
            }
            if (!this.bbox().intersects(search_bbox)) {
                if (debug) {
                    out("miss " + Long.toHexString(this.quad));
                    //QuadTiling.tile2xy(this.quad);
                }
                return ret;
            }
            if (this.hasContent()) {
                ret = this.search_contents(search_bbox);
            }
            if (this.isLeaf())
                return ret;
            //if (this.hasContent())
            //    abort("branch had stuff");
            if (debug) {
                out("hit " + this.quads());
            }

            if (debug) {
                out("[" + level + "] not leaf, moving down");
            }
            for (int i = 0; i < children.length; i++) {
                QBLevel q = children[i];
                if (debug) {
                    out("child["+i+"]: " + q);
                }
                if (q == null) {
                    continue;
                }
                if (debug) {
                    System.out.print(i+": ");
                }
                List<T> coors = q.search(search_bbox);
                if (coors == null) {
                    continue;
                }
                if (ret == null) {
                    ret = coors;
                } else {
                    ret.addAll(coors);
                }
                if (q.bbox().bounds(search_bbox)) {
                    search_cache = q;
                    // optimization: do not continue searching
                    // other tiles if this one wholly contained
                    // what we were looking for.
                    if (coors.size() > 0 ) {
                        if (debug) {
                            out("break early");
                        }
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
            if (parent == null) {
                this.level = 0;
            } else {
                this.level = parent.level + 1;
            }
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
            if (debug) {
                out("new level["+this.level+"] bbox["+parent_index+"]: " + this.bbox()
                        + " coor: " + this.coor()
                        + " quadpart: " + Long.toHexString(this_quadpart)
                        + " quad: " + Long.toHexString(this.quad));
            }
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
        boolean hasChildren()
        {
            if (children == null)
                return false;

            for (QBLevel child : children) {
                if (child != null)
                    return true;
            }
            return false;
        }
        void remove_from_parent()
        {
            if (parent == null)
                return;

            int nr_siblings = 0;
            for (int i = 0; i < parent.children.length; i++) {
                QBLevel sibling = parent.children[i];
                if (sibling != null) {
                    nr_siblings++;
                }
                if (sibling != this) {
                    continue;
                }
                if (!canRemove()) {
                    abort("attempt to remove non-empty child: " + this.content + " " + this.children);
                }
                parent.children[i] = null;
                nr_siblings--;
            }
            if (parent.canRemove()) {
                parent.remove_from_parent();
            }
        }
        boolean canRemove()
        {
            if (content != null && content.size() > 0)
                return false;
            if (this.hasChildren())
                return false;
            return true;
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
        if (debug) {
            out("QuadBuckets() cleared: " + this);
            out("root: " + root + " level: " + root.level + " bbox: " + root.bbox());
        }
    }
    public boolean add(T n)
    {
        if (debug) {
            out("QuadBuckets() add: " + n + " size now: " + this.size());
        }
        int before_size = -1;
        if (consistency_testing) {
            before_size = root.size();
        }
        boolean ret;
        // A way with no nodes will have an infinitely large
        // bounding box.  Just stash it in the root node.
        if (!n.isUsable()) {
            synchronized (split_lock) {
                ret = root.__add_content(n);
            }
        } else {
            ret = root.add(n);
        }
        if (consistency_testing) {
            int end_size = root.size();
            if (ret) {
                end_size--;
            }
            if (before_size != end_size) {
                abort("size inconsistency before add: " + before_size + " after: " + end_size);
            }
        }
        if (debug) {
            out("QuadBuckets() add: " + n + " size after: " + this.size());
        }
        return ret;
    }
    public void reindex()
    {
        QBLevel newroot = new QBLevel(null);
        Iterator<T> i = this.iterator();
        while (i.hasNext()) {
            T o = i.next();
            newroot.add(o);
        }
        root = newroot;
    }
    public void unsupported()
    {
        out("unsupported operation");
        throw new UnsupportedOperationException();
    }
    public boolean retainAll(Collection<?> objects)
    {
        for (T o : this) {
            if (objects.contains(o)) {
                continue;
            }
            if (!this.remove(o))
                return false;
        }
        return true;
    }
    public boolean removeAll(Collection<?> objects)
    {
        boolean changed = false;
        for (Object o : objects) {
            changed = changed | remove(o);
        }
        return changed;
    }
    public boolean addAll(Collection<? extends T> objects)
    {
        boolean changed = false;
        for (Object o : objects) {
            changed = changed | this.add(convert(o));
        }
        return changed;
    }
    public boolean containsAll(Collection<?> objects)
    {
        for (Object o : objects) {
            if (!this.contains(o))
                return false;
        }
        return true;
    }
    // If anyone has suggestions for how to fix
    // this properly, I'm listening :)
    @SuppressWarnings("unchecked")
    private T convert(Object raw)
    {
        return (T)raw;
    }
    public boolean remove(Object o)
    {
        return this.remove(convert(o));
    }
    public boolean remove_slow(T removeme)
    {
        boolean ret = false;
        Iterator<T> i = this.iterator();
        while (i.hasNext()) {
            T o = i.next();
            if (o != removeme) {
                continue;
            }
            i.remove();
            ret = true;
            break;
        }
        if (debug) {
            out("qb slow remove result: " + ret);
        }
        return ret;
    }
    public boolean remove(T o)
    {
        /*
         * We first try a locational search
         */
        QBLevel bucket = root.find_exact(o);
        /*
         * That may fail because the object was
         * moved or changed in some way, so we
         * resort to an iterative search:
         */
        if (bucket == null)
            return remove_slow(o);
        boolean ret = bucket.remove_content(o);
        if (debug) {
            out("qb remove result: " + ret);
        }
        return ret;
    }
    public boolean contains(Object o)
    {
        QBLevel bucket = root.find_exact(convert(o));
        if (bucket == null)
            return false;
        return true;
    }
    public ArrayList<T> toArrayList()
    {
        ArrayList<T> a = new ArrayList<T>();
        for (T n : this) {
            a.add(n);
        }
        if (debug) {
            out("returning array list with size: " + a.size());
        }
        return a;
    }
    public Object[] toArray()
    {
        return this.toArrayList().toArray();
    }
    public <A> A[] toArray(A[] template)
    {
        return this.toArrayList().toArray(template);
    }
    class QuadBucketIterator implements Iterator<T>
    {
        QBLevel current_node;
        int content_index;
        int iterated_over;
        QBLevel next_content_node(QBLevel q)
        {
            if (q == null)
                return null;
            QBLevel orig = q;
            QBLevel next;
            synchronized (split_lock) {
                next = q.nextContentNode();
            }
            //if (consistency_testing && (orig == next))
            if (orig == next) {
                abort("got same leaf back leaf: " + q.isLeaf());
            }
            return next;
        }
        public QuadBucketIterator(QuadBuckets<T> qb)
        {
            if (debug) {
                out(this + " is a new iterator qb: " + qb + " size: " + qb.size());
            }
            if (qb.root.isLeaf() || qb.root.hasContent()) {
                current_node = qb.root;
            } else {
                current_node = next_content_node(qb.root);
            }
            if (debug) {
                out("\titerator first leaf: " + current_node);
            }
            iterated_over = 0;
        }
        public boolean hasNext()
        {
            if (this.peek() == null) {
                if (debug) {
                    out(this + " no hasNext(), but iterated over so far: " + iterated_over);
                }
                return false;
            }
            return true;
        }
        T peek()
        {
            if (current_node == null) {
                if (debug) {
                    out("null current leaf, nowhere to go");
                }
                return null;
            }
            while((current_node.content == null) ||
                    (content_index >= current_node.content.size())) {
                if (debug) {
                    out("moving to next leaf");
                }
                content_index = 0;
                current_node = next_content_node(current_node);
                if (current_node == null) {
                    break;
                }
            }
            if (current_node == null || current_node.content == null) {
                if (debug) {
                    out("late nowhere to go " + current_node);
                }
                return null;
            }
            return current_node.content.get(content_index);
        }
        public T next()
        {
            T ret = peek();
            content_index++;
            if (debug) {
                out("iteration["+iterated_over+"] " + content_index + " leaf: " + current_node);
            }
            iterated_over++;
            if (ret == null) {
                if (debug) {
                    out(this + " no next node, but iterated over so far: " + iterated_over);
                }
            }
            return ret;
        }
        public void remove()
        {
            // two uses
            // 1. Back up to the thing we just returned
            // 2. move the index back since we removed
            //    an element
            content_index--;
            T object = peek(); //TODO Is the call to peek() necessary?
            current_node.remove_content(object);
        }
    }
    public Iterator<T> iterator()
    {
        return new QuadBucketIterator(this);
    }
    public int size()
    {
        // This can certainly by optimized
        int ret = root.size();
        if (debug) {
            out(this + " QuadBuckets size: " + ret);
        }
        return ret;
    }
    public int size_iter()
    {
        int count = 0;
        Iterator<T> i = this.iterator();
        while (i.hasNext()) {
            i.next();
            count++;
        }
        return count;
    }
    public boolean isEmpty()
    {
        if (this.size() == 0)
            return true;
        return false;
    }
    public static BBox search_to_bbox(LatLon point, double radius)
    {
        BBox bbox = new BBox(point.lon() - radius, point.lat() - radius,
                             point.lon() + radius, point.lat() + radius);
        if (debug) {
            out("search bbox before sanity: " +  bbox);
        }
        bbox.sanity();
        if (debug) {
            out("search bbox after sanity: " +  bbox);
        }
        return bbox;
    }
    List<T> search(Way w)
    {
        BBox way_bbox = new BBox(w);
        return this.search(way_bbox);
    }
    public List<T> search(Node n, double radius)
    {
        return this.search(n.getCoor(), radius);
    }
    public List<T> search(LatLon point, double radius)
    {
        if (point == null)
            return Collections.emptyList();
        return this.search(search_to_bbox(point, radius));
    }
    public List<T> search(LatLon b1, LatLon b2)
    {
        BBox bbox = new BBox(b1.lon(), b1.lat(), b2.lon(), b2.lat());
        bbox.sanity();
        return this.search(bbox);
    }
    List<T> search(BBox search_bbox)
    {
        if (debug) {
            out("qb root search at " + search_bbox);
            out("root bbox: " + root.bbox());
        }
        List<T> ret;
        // Doing this cuts down search cost on a real-life data
        // set by about 25%
        boolean cache_searches = true;
        if (cache_searches) {
            if (search_cache == null) {
                search_cache = root;
            }
            // Walk back up the tree when the last
            // search spot can not cover the current
            // search
            while (!search_cache.bbox().bounds(search_bbox)) {
                if (debug) {
                    out("bbox: " + search_bbox);
                }
                if (debug) {
                    out("search_cache: " + search_cache + " level: " + search_cache.level);
                    out("search_cache.bbox(): " + search_cache.bbox());
                }
                search_cache = search_cache.parent;
                if (debug) {
                    out("new search_cache: " + search_cache);
                }
            }
        } else {
            search_cache = root;
        }
        ret = search_cache.search(search_bbox);
        if (ret == null) {
            ret = new ArrayList<T>();
        }
        // A way that spans this bucket may be stored in one
        // of the nodes which is a parent of the search cache
        QBLevel tmp = search_cache.parent;
        while (tmp != null) {
            List<T> content_result = tmp.search_contents(search_bbox);
            if (content_result != null) {
                ret.addAll(content_result);
            }
            tmp = tmp.parent;
        }
        if (debug) {
            out("search of QuadBuckets for " + search_bbox + " ret len: " + ret.size());
        }
        return ret;
    }
}
