// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.QuadTiling;

/**
 * Note: bbox of primitives added to QuadBuckets has to stay the same. In case of coordinate change, primitive must
 * be removed and readded.
 *
 * This class is (no longer) thread safe.
 *
 */
public class QuadBuckets<T extends OsmPrimitive> implements Collection<T>
{
    private static final boolean consistency_testing = false;
    private static final int NW_INDEX = 1;
    private static final int NE_INDEX = 3;
    private static final int SE_INDEX = 2;
    private static final int SW_INDEX = 0;

    static void abort(String s) {
        throw new AssertionError(s);
    }

    public static final int MAX_OBJECTS_PER_LEVEL = 16;
    
    class QBLevel
    {
        final int level;
        private final BBox bbox;
        final long quad;
        final QBLevel parent;
        private boolean isLeaf = true;

        public List<T> content;
        public QBLevel nw, ne, sw, se;

        private QBLevel getChild(int index) {
            switch (index) {
            case NE_INDEX:
                if (ne == null) {
                    ne = new QBLevel(this, index);
                }
                return ne;
            case NW_INDEX:
                if (nw == null) {
                    nw = new QBLevel(this, index);
                }
                return nw;
            case SE_INDEX:
                if (se == null) {
                    se = new QBLevel(this, index);
                }
                return se;
            case SW_INDEX:
                if (sw == null) {
                    sw = new QBLevel(this, index);
                }
                return sw;
            default:
                return null;
            }
        }

        private QBLevel[] getChildren() {
            // This is ugly and hackish.  But, it seems to work,
            // and using an ArrayList here seems to cost us
            // a significant performance penalty -- 50% in my
            // testing.  Child access is one of the single
            // hottest code paths in this entire class.
            @SuppressWarnings("unchecked")
            QBLevel[] result = (QBLevel[]) Array.newInstance(this.getClass(), QuadTiling.TILES_PER_LEVEL);
            result[NW_INDEX] = nw;
            result[NE_INDEX] = ne;
            result[SW_INDEX] = sw;
            result[SE_INDEX] = se;
            return result;
        }

        @Override
        public String toString()  {
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
            // Java blows the big one. It seems to wrap when you shift by > 31
            if (shift >= 30) {
                shift -= 30;
                mult = 1<<30;
            }
            long this_quadpart = mult * (parent_index << shift);
            this.quad = parent.quad | this_quadpart;
            this.bbox = calculateBBox(); // calculateBBox reference quad
        }

        private BBox calculateBBox() {
            LatLon bottom_left = this.coor();
            double lat = bottom_left.lat() + parent.height() / 2;
            double lon = bottom_left.lon() + parent.width() / 2;
            LatLon top_right = new LatLon(lat, lon);
            return new BBox(bottom_left, top_right);
        }

        QBLevel findBucket(BBox bbox) {
            if (!hasChildren())
                return this;
            else {
                int index = bbox.getIndex(level);
                if (index == -1)
                    return this;
                return getChild(index).findBucket(bbox);
            }
        }

        boolean remove_content(T o) {
            // If two threads try to remove item at the same time from different buckets of this QBLevel,
            // it might happen that one thread removes bucket but don't remove parent because it still sees
            // another bucket set. Second thread do the same. Due to thread memory caching, it's possible that
            // changes made by threads will show up in children array too late, leading to QBLevel with all children
            // set to null
            if (content == null)
                return false;
            boolean ret = this.content.remove(o);
            if (this.content.isEmpty()) {
                this.content = null;
            }
            if (this.canRemove()) {
                this.remove_from_parent();
            }
            return ret;
        }

        /*
         * There is a race between this and qb.nextContentNode().
         * If nextContentNode() runs into this bucket, it may
         * attempt to null out 'children' because it thinks this
         * is a dead end.
         */
        void __split() {
            List<T> tmpcontent = content;
            content = null;

            for (T o: tmpcontent) {
                int index = o.getBBox().getIndex(level);
                if (index == -1) {
                    __add_content(o);
                } else {
                    getChild(index).doAdd(o);
                }
            }
            isLeaf = false; // It's not enough to check children because all items could end up in this level (index == -1)
        }

        boolean __add_content(T o) {
            boolean ret = false;
            // The split_lock will keep two concurrent calls from overwriting content
            if (content == null) {
                content = new ArrayList<T>();
            }
            ret = content.add(o);
            return ret;
        }
        
        boolean matches(T o, BBox search_bbox) {
            // This can be optimized when o is a node
            return o.getBBox().intersects(search_bbox);
        }
        
        private void search_contents(BBox search_bbox, List<T> result) {
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
        }
        
        /*
         * This is stupid. I tried to have a QBLeaf and QBBranch
         * class decending from a QBLevel. It's more than twice
         * as slow. So, this throws OO out the window, but it
         * is fast. Runtime type determination must be slow.
         */
        boolean isLeaf() {
            return isLeaf;
        }
        
        boolean hasChildren() {
            return nw != null || ne != null || sw != null || se != null;
        }

        QBLevel next_sibling() {
            boolean found_me = false;
            if (parent == null)
                return null;
            for (QBLevel sibling : parent.getChildren()) {
                if (sibling == null) {
                    continue;
                }
                // We're looking for the *next* child after us.
                if (sibling == this) {
                    found_me = true;
                    continue;
                }
                if (found_me)
                    return sibling;
            }
            return null;
        }
        
        boolean hasContent() {
            return content != null;
        }
        
        QBLevel nextSibling() {
            QBLevel next = this;
            QBLevel sibling = next.next_sibling();
            // Walk back up the tree to find the
            // next sibling node.  It may be either
            // a leaf or branch.
            while (sibling == null) {
                next = next.parent;
                if (next == null) {
                    break;
                }
                sibling = next.next_sibling();
            }
            next = sibling;
            return next;
        }
        
        QBLevel firstChild() {
            QBLevel ret = null;
            for (QBLevel child : getChildren()) {
                if (child == null) {
                    continue;
                }
                ret = child;
                break;
            }
            return ret;
        }
        
        QBLevel nextNode() {
            if (!this.hasChildren())
                return this.nextSibling();
            return this.firstChild();
        }
        
        QBLevel nextContentNode() {
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
                    o.getBBox().getIndex(level);
                    o.getBBox().getIndex(level-1);
                    int nr = 0;
                    abort("\nobject " + o + " does not belong in node at level: " + level + " bbox: " + this.bbox());
                }
            }
            __add_content(o);
            if (isLeaf() && content.size() > MAX_OBJECTS_PER_LEVEL && level < QuadTiling.NR_LEVELS) {
                __split();
            }
        }

        void add(T o) {
            findBucket(o.getBBox()).doAdd(o);
        }

        private void search(BBox search_bbox, List<T> result) {
            if (!this.bbox().intersects(search_bbox))
                return;
            else if (bbox().bounds(search_bbox)) {
                search_cache = this;
            }

            if (this.hasContent()) {
                search_contents(search_bbox, result);
            }

            //TODO Coincidence vector should be calculated here and only buckets that match search_bbox should be checked

            if (nw != null) {
                nw.search(search_bbox, result);
            }
            if (ne != null) {
                ne.search(search_bbox, result);
            }
            if (se != null) {
                se.search(search_bbox, result);
            }
            if (sw != null) {
                sw.search(search_bbox, result);
            }
        }
        
        public String quads() {
            return Long.toHexString(quad);
        }
        
        int index_of(QBLevel find_this) {
            QBLevel[] children = getChildren();
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
        LatLon coor() {
            return QuadTiling.tile2LatLon(this.quad);
        }
        
        void remove_from_parent() {
            if (parent == null)
                return;

            if (!canRemove()) {
                abort("attempt to remove non-empty child: " + this.content + " " + Arrays.toString(this.getChildren()));
            }

            if (parent.nw == this) {
                parent.nw = null;
            } else if (parent.ne == this) {
                parent.ne = null;
            } else if (parent.sw == this) {
                parent.sw = null;
            } else if (parent.se == this) {
                parent.se = null;
            }

            if (parent.canRemove()) {
                parent.remove_from_parent();
            }
        }
        
        boolean canRemove() {
            if (content != null && !content.isEmpty())
                return false;
            if (this.hasChildren())
                return false;
            return true;
        }
    }

    private QBLevel root;
    private QBLevel search_cache;
    private int size;

    /**
     * Constructs a new {@code QuadBuckets}.
     */
    public QuadBuckets() {
        clear();
    }
    
    @Override
    public void clear()  {
        root = new QBLevel();
        search_cache = null;
        size = 0;
    }
    
    @Override
    public boolean add(T n) {
        root.add(n);
        size++;
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        for (T o : this) {
            if (objects.contains(o)) {
                continue;
            }
            if (!this.remove(o))
                return false;
        }
        return true;
    }
    
    @Override
    public boolean removeAll(Collection<?> objects) {
        boolean changed = false;
        for (Object o : objects) {
            changed = changed | remove(o);
        }
        return changed;
    }
    
    @Override
    public boolean addAll(Collection<? extends T> objects) {
        boolean changed = false;
        for (T o : objects) {
            changed = changed | this.add(o);
        }
        return changed;
    }
    
    @Override
    public boolean containsAll(Collection<?> objects) {
        for (Object o : objects) {
            if (!this.contains(o))
                return false;
        }
        return true;
    }
    
    @Override
    public boolean remove(Object o) {
        @SuppressWarnings("unchecked") T t = (T) o;
        search_cache = null; // Search cache might point to one of removed buckets
        QBLevel bucket = root.findBucket(t.getBBox());
        if (bucket.remove_content(t)) {
            size--;
            return true;
        } else
            return false;
    }
    
    @Override
    public boolean contains(Object o) {
        @SuppressWarnings("unchecked") T t = (T) o;
        QBLevel bucket = root.findBucket(t.getBBox());
        return bucket != null && bucket.content != null && bucket.content.contains(t);
    }

    public ArrayList<T> toArrayList() {
        ArrayList<T> a = new ArrayList<T>();
        for (T n : this) {
            a.add(n);
        }
        return a;
    }
    
    @Override
    public Object[] toArray() {
        return this.toArrayList().toArray();
    }
    
    @Override
    public <A> A[] toArray(A[] template) {
        return this.toArrayList().toArray(template);
    }
    
    class QuadBucketIterator implements Iterator<T>
    {
        QBLevel current_node;
        int content_index;
        int iterated_over;
        QBLevel next_content_node(QBLevel q) {
            if (q == null)
                return null;
            QBLevel orig = q;
            QBLevel next;
            next = q.nextContentNode();
            //if (consistency_testing && (orig == next))
            if (orig == next) {
                abort("got same leaf back leaf: " + q.isLeaf());
            }
            return next;
        }
        
        public QuadBucketIterator(QuadBuckets<T> qb) {
            if (!qb.root.hasChildren() || qb.root.hasContent()) {
                current_node = qb.root;
            } else {
                current_node = next_content_node(qb.root);
            }
            iterated_over = 0;
        }
        
        @Override
        public boolean hasNext() {
            if (this.peek() == null)
                return false;
            return true;
        }
        
        T peek() {
            if (current_node == null)
                return null;
            while((current_node.content == null) ||
                    (content_index >= current_node.content.size())) {
                content_index = 0;
                current_node = next_content_node(current_node);
                if (current_node == null) {
                    break;
                }
            }
            if (current_node == null || current_node.content == null)
                return null;
            return current_node.content.get(content_index);
        }
        
        @Override
        public T next() {
            T ret = peek();
            content_index++;
            iterated_over++;
            return ret;
        }
        
        @Override
        public void remove() {
            // two uses
            // 1. Back up to the thing we just returned
            // 2. move the index back since we removed
            //    an element
            content_index--;
            T object = peek();
            current_node.remove_content(object);
        }
    }
    
    @Override
    public Iterator<T> iterator() {
        return new QuadBucketIterator(this);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        if (this.size() == 0)
            return true;
        return false;
    }
    
    public List<T> search(BBox search_bbox) {
        List<T> ret = new ArrayList<T>();
        // Doing this cuts down search cost on a real-life data set by about 25%
        boolean cache_searches = true;
        if (cache_searches) {
            if (search_cache == null) {
                search_cache = root;
            }
            // Walk back up the tree when the last search spot can not cover the current search
            while (search_cache != null && !search_cache.bbox().bounds(search_bbox)) {
                search_cache = search_cache.parent;
            }

            if (search_cache == null) {
                search_cache = root;
                Main.info("bbox: " + search_bbox + " is out of the world");
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
        for (QBLevel child:level.getChildren()) {
            printTreeRecursive(child, indent + 2);
        }
    }

    private void printIndented(int indent, Object msg) {
        for (int i=0; i<indent; i++) {
            System.out.print(' ');
        }
        System.out.println(msg);
    }
}
