package org.openstreetmap.josm.data.osm;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.QuadTiling;

/**
 * Note: bbox of primitives added to QuadBuckets has to stay the same. In case of coordinate change, primitive must
 * be removed and readded.
 *
 */
public class QuadBuckets<T extends OsmPrimitive> implements Collection<T>
{
    private static boolean debug = false;
    private static final boolean consistency_testing = false;
    /*
     * Functions prefixed with __ need locking before
     * being called.
     */
    private final Object split_lock = new Object();

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

    public static int MAX_OBJECTS_PER_LEVEL = 16;
    class QBLevel
    {
        final int level;
        private final BBox bbox;
        final long quad;
        final QBLevel parent;

        public List<T> content;
        public QBLevel children[];
        @Override
        public String toString()
        {
            return super.toString()+ "["+level+"]: " + bbox();
        }
        /**
         * Constructor for root node
         */
        public QBLevel() {
            level = 0;
            quad = 0;
            parent = null;
            bbox = new BBox(-180, 90, 180, -90);
        }

        public QBLevel(QBLevel parent, int parent_index) {
            this.parent = parent;
            this.level = parent.level + 1;
            int shift = (QuadTiling.NR_LEVELS - level) * 2;
            long mult = 1;
            // Java blows the big one.  It seems to wrap when
            // you shift by > 31
            if (shift >= 30) {
                shift -= 30;
                mult = 1<<30;
            }
            long this_quadpart = mult * (parent_index << shift);
            this.quad = parent.quad | this_quadpart;
            this.bbox = calculateBBox(); // calculateBBox reference quad
            if (debug) {
                out("new level["+this.level+"] bbox["+parent_index+"]: " + this.bbox()
                        + " coor: " + this.coor()
                        + " quadpart: " + Long.toHexString(this_quadpart)
                        + " quad: " + Long.toHexString(this.quad));
            }
        }

        private BBox calculateBBox() {
            LatLon bottom_left = this.coor();
            double lat = bottom_left.lat() + parent.height() / 2;
            double lon = bottom_left.lon() + parent.width() / 2;
            LatLon top_right = new LatLon(lat, lon);
            return new BBox(bottom_left, top_right);
        }

        QBLevel findBucket(BBox bbox) {
            if (isLeaf())
                return this;
            else {
                int index = get_index(bbox, level);
                if (index == -1)
                    return this;
                if (children[index] == null) {
                    children[index] = new QBLevel(this, index);
                }
                return children[index].findBucket(bbox);
            }
        }

        boolean remove_content(T o)
        {
            synchronized (split_lock) {
                // If two threads try to remove item at the same time from different buckets of this QBLevel,
                // it might happen that one thread removes bucket but don't remove parent because it still sees
                // another bucket set. Second thread do the same. Due to thread memory caching, it's possible that
                // changes made by threads will show up in children array too late, leading to QBLevel with all children
                // set to null
                if (content == null)
                    return false;
                boolean ret = this.content.remove(o);
                if (this.content.size() == 0) {
                    this.content = null;
                }
                if (this.canRemove()) {
                    this.remove_from_parent();
                }
                return ret;
            }
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
        int get_index(BBox bbox, int level) {
            int index = -1;
            for (LatLon c : bbox.points()) {
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
                if (another_index != index)
                    return -1;
            }
            return index;
        }
        /*
         * There is a race between this and qb.nextContentNode().
         * If nextContentNode() runs into this bucket, it may
         * attempt to null out 'children' because it thinks this
         * is a dead end.
         */
        void __split() {
            if (debug) {
                out("splitting "+this.bbox()+" level "+level+" with "
                        + content.size() + " entries (my dimensions: "
                        + this.bbox().width()+", "+this.bbox().height()+")");
            }
            // deferring allocation of children until use
            // seems a bit faster
            //for (int i = 0; i < TILES_PER_LEVEL; i++)
            //    children[i] = new QBLevel(this, i);
            List<T> tmpcontent = content;
            content = null;
            children = newChildren();
            for (T o: tmpcontent) {
                add(o);
            }
            if (!hasChildren()) {
                // All items stay at this level (get_index == -1). Create at least first child to keep the contract
                // that at least one child always exists
                children[0] = new QBLevel(this, 0);
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
        boolean matches(T o, BBox search_bbox)
        {
            // This can be optimized when o is a node
            //return search_bbox.bounds(coor));
            return o.getBBox().intersects(search_bbox);
        }
        private void search_contents(BBox search_bbox, List<T> result)
        {
            if (debug) {
                out("searching contents (size: " + content == null?"<null>":content.size() + ") for " + search_bbox);
            }
            /*
             * It is possible that this was created in a split
             * but never got any content populated.
             */
            if (content == null)
                return;

            for (T o : content) {
                if (matches(o, search_bbox)) {
                    result.add(o);
                }
            }
            if (debug) {
                out("done searching quad " + Long.toHexString(this.quad));
            }
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
            if (parent.children == null)
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
            return content != null;
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
                if (child == null) {
                    continue;
                }
                ret = child;
                break;
            }
            return ret;
        }
        QBLevel nextNode()
        {
            if (this.isLeaf())
                return this.nextSibling();
            return this.firstChild();
        }
        QBLevel nextContentNode()
        {
            QBLevel next = this.nextNode();
            if (next == null)
                return next;
            if (next.hasContent())
                return next;
            return next.nextContentNode();
        }

        void doAdd(T o) {
            if (consistency_testing) {
                if (!matches(o, this.bbox())) {
                    out("-----------------------------");
                    debug = true;
                    get_index(o.getBBox(), level);
                    get_index(o.getBBox(), level-1);
                    int nr = 0;
                    for (QBLevel sibling : parent.children) {
                        out("sibling["+ (nr++) +"]: " + sibling.bbox() + " this: " + (this==sibling));
                    }
                    abort("\nobject " + o + " does not belong in node at level: " + level + " bbox: " + this.bbox());
                }
            }
            synchronized (split_lock) {
                __add_content(o);
                if (isLeaf() && content.size() > MAX_OBJECTS_PER_LEVEL && level < QuadTiling.NR_LEVELS) {
                    __split();
                }
            }
        }

        void add(T o) {
            synchronized (split_lock) {
                findBucket(o.getBBox()).doAdd(o);
            }
        }

        private void search(BBox search_bbox, List<T> result)
        {
            if (debug) {
                System.out.print("[" + level + "] qb bbox: " + this.bbox() + " ");
            }
            if (!this.bbox().intersects(search_bbox)) {
                if (debug) {
                    out("miss " + Long.toHexString(this.quad));
                    //QuadTiling.tile2xy(this.quad);
                }
                return;
            }
            if (this.hasContent()) {
                search_contents(search_bbox, result);
            }
            if (this.isLeaf())
                return;

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
                q.search(search_bbox, result);
                if (q.bbox().bounds(search_bbox)) {
                    search_cache = q;
                    // optimization: do not continue searching
                    // other tiles if this one wholly contained
                    // what we were looking for.
                    if (debug) {
                        out("break early");
                    }
                    break;
                }
            }
        }
        public String quads()
        {
            return Long.toHexString(quad);
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
        double width() {
            return bbox.width();
        }

        double height() {
            return bbox.height();
        }

        public BBox bbox() {
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
                    abort("attempt to remove non-empty child: " + this.content + " " + Arrays.toString(this.children));
                }
                parent.children[i] = null;
                nr_siblings--;
            }
            if (nr_siblings == 0) {
                parent.children = null;
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
    private int size;

    public QuadBuckets()
    {
        clear();
    }
    public void clear()  {
        synchronized (split_lock) {
            root = new QBLevel();
            search_cache = null;
            size = 0;
            if (debug) {
                out("QuadBuckets() cleared: " + this);
                out("root: " + root + " level: " + root.level + " bbox: " + root.bbox());
            }
        }
    }
    public boolean add(T n) {
        synchronized (split_lock) {
            root.add(n);
            size++;
            return true;
        }
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
        for (T o : objects) {
            changed = changed | this.add(o);
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
    public boolean remove(Object o) {
        @SuppressWarnings("unchecked") T t = (T) o;
        synchronized (split_lock) {
            search_cache = null; // Search cache might point to one of removed buckets
            QBLevel bucket = root.findBucket(t.getBBox());
            if (bucket.remove_content(t)) {
                size--;
                return true;
            } else
                return false;
        }
    }
    public boolean contains(Object o) {
        @SuppressWarnings("unchecked") T t = (T) o;
        QBLevel bucket = root.findBucket(t.getBBox());
        return bucket != null && bucket.content != null && bucket.content.contains(t);
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
            T object = peek();
            current_node.remove_content(object);
        }
    }
    public Iterator<T> iterator()
    {
        return new QuadBucketIterator(this);
    }
    public int size() {
        synchronized (split_lock) {
            return size;
        }
    }

    public boolean isEmpty()
    {
        if (this.size() == 0)
            return true;
        return false;
    }
    public List<T> search(BBox search_bbox) {
        if (debug) {
            out("qb root search at " + search_bbox);
            out("root bbox: " + root.bbox());
        }
        List<T> ret = new ArrayList<T>();
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

        // Save parent because search_cache might change during search call
        QBLevel tmp = search_cache.parent;

        search_cache.search(search_bbox, ret);

        // A way that spans this bucket may be stored in one
        // of the nodes which is a parent of the search cache
        while (tmp != null) {
            tmp.search_contents(search_bbox, ret);
            tmp = tmp.parent;
        }
        if (debug) {
            out("search of QuadBuckets for " + search_bbox + " ret len: " + ret.size());
        }
        return ret;
    }

    public void printTree() {
        printTreeRecursive(root, 0);
    }

    private void printTreeRecursive(QBLevel level, int indent) {
        if (level == null) {
            printIndented(indent, "<empty child>");
            return;
        }
        printIndented(indent, level);
        if (level.hasContent()) {
            for (T o:level.content) {
                printIndented(indent, o);
            }
        }
        if (level.children != null) {
            for (QBLevel child:level.children) {
                printTreeRecursive(child, indent + 2);
            }
        }
    }

    private void printIndented(int indent, Object msg) {
        for (int i=0; i<indent; i++) {
            System.out.print(' ');
        }
        System.out.println(msg);
    }
}
