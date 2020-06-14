/*
 * $Id: MultiSplitLayout.java,v 1.15 2005/10/26 14:29:54 hansmuller Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.UIManager;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * The MultiSplitLayout layout manager recursively arranges its
 * components in row and column groups called "Splits".  Elements of
 * the layout are separated by gaps called "Dividers".  The overall
 * layout is defined with a simple tree model whose nodes are
 * instances of MultiSplitLayout.Split, MultiSplitLayout.Divider,
 * and MultiSplitLayout.Leaf. Named Leaf nodes represent the space
 * allocated to a component that was added with a constraint that
 * matches the Leaf's name.  Extra space is distributed
 * among row/column siblings according to their 0.0 to 1.0 weight.
 * If no weights are specified then the last sibling always gets
 * all of the extra space, or space reduction.
 *
 * <p>
 * Although MultiSplitLayout can be used with any Container, it's
 * the default layout manager for MultiSplitPane.  MultiSplitPane
 * supports interactively dragging the Dividers, accessibility,
 * and other features associated with split panes.
 *
 * <p>
 * All properties in this class are bound: when a properties value
 * is changed, all PropertyChangeListeners are fired.
 *
 * @author Hans Muller - SwingX
 * @see MultiSplitPane
 */
public class MultiSplitLayout implements LayoutManager {
    private final Map<String, Component> childMap = new HashMap<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private Node model;
    private int dividerSize;
    private boolean floatingDividers = true;

    /**
     * Create a MultiSplitLayout with a default model with a single
     * Leaf node named "default".
     *
     * #see setModel
     */
    public MultiSplitLayout() {
        this(new Leaf("default"));
    }

    /**
     * Create a MultiSplitLayout with the specified model.
     *
     * #see setModel
     * @param model model
     */
    public MultiSplitLayout(Node model) {
        this.model = model;
        this.dividerSize = UIManager.getInt("SplitPane.dividerSize");
        if (this.dividerSize == 0) {
            this.dividerSize = 7;
        }
    }

    /**
     * Add property change listener.
     * @param listener listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            pcs.addPropertyChangeListener(listener);
        }
    }

    /**
     * Remove property change listener.
     * @param listener listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            pcs.removePropertyChangeListener(listener);
        }
    }

    /**
     * Replies list of property change listeners.
     * @return list of property change listeners
     */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    private void firePCS(String propertyName, Object oldValue, Object newValue) {
        if (!(oldValue != null && newValue != null && oldValue.equals(newValue))) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Return the root of the tree of Split, Leaf, and Divider nodes
     * that define this layout.
     *
     * @return the value of the model property
     * @see #setModel
     */
    public Node getModel() {
        return model;
    }

    /**
     * Set the root of the tree of Split, Leaf, and Divider nodes
     * that define this layout.  The model can be a Split node
     * (the typical case) or a Leaf.  The default value of this
     * property is a Leaf named "default".
     *
     * @param model the root of the tree of Split, Leaf, and Divider node
     * @throws IllegalArgumentException if model is a Divider or null
     * @see #getModel
     */
    public void setModel(Node model) {
        if ((model == null) || (model instanceof Divider))
            throw new IllegalArgumentException("invalid model");
        Node oldModel = model;
        this.model = model;
        firePCS("model", oldModel, model);
    }

    /**
     * Returns the width of Dividers in Split rows, and the height of
     * Dividers in Split columns.
     *
     * @return the value of the dividerSize property
     * @see #setDividerSize
     */
    public int getDividerSize() {
        return dividerSize;
    }

    /**
     * Sets the width of Dividers in Split rows, and the height of
     * Dividers in Split columns.  The default value of this property
     * is the same as for JSplitPane Dividers.
     *
     * @param dividerSize the size of dividers (pixels)
     * @throws IllegalArgumentException if dividerSize &lt; 0
     * @see #getDividerSize
     */
    public void setDividerSize(int dividerSize) {
        if (dividerSize < 0)
            throw new IllegalArgumentException("invalid dividerSize");
        int oldDividerSize = this.dividerSize;
        this.dividerSize = dividerSize;
        firePCS("dividerSize", oldDividerSize, dividerSize);
    }

    /**
     * Returns the value of the floatingDividers property.
     * @return the value of the floatingDividers property
     * @see #setFloatingDividers
     */
    public boolean getFloatingDividers() {
        return floatingDividers;
    }

    /**
     * If true, Leaf node bounds match the corresponding component's
     * preferred size and Splits/Dividers are resized accordingly.
     * If false then the Dividers define the bounds of the adjacent
     * Split and Leaf nodes.  Typically this property is set to false
     * after the (MultiSplitPane) user has dragged a Divider.
     * @param floatingDividers boolean value
     *
     * @see #getFloatingDividers
     */
    public void setFloatingDividers(boolean floatingDividers) {
        boolean oldFloatingDividers = this.floatingDividers;
        this.floatingDividers = floatingDividers;
        firePCS("floatingDividers", oldFloatingDividers, floatingDividers);
    }

    /**
     * Add a component to this MultiSplitLayout.  The
     * <code>name</code> should match the name property of the Leaf
     * node that represents the bounds of <code>child</code>.  After
     * layoutContainer() recomputes the bounds of all of the nodes in
     * the model, it will set this child's bounds to the bounds of the
     * Leaf node with <code>name</code>.  Note: if a component was already
     * added with the same name, this method does not remove it from
     * its parent.
     *
     * @param name identifies the Leaf node that defines the child's bounds
     * @param child the component to be added
     * @see #removeLayoutComponent
     */
    @Override
    public void addLayoutComponent(String name, Component child) {
        if (name == null)
            throw new IllegalArgumentException("name not specified");
        childMap.put(name, child);
    }

    /**
     * Removes the specified component from the layout.
     *
     * @param child the component to be removed
     * @see #addLayoutComponent
     */
    @Override
    public void removeLayoutComponent(Component child) {
        String name = child.getName();
        if (name != null) {
            childMap.remove(name);
        } else {
            childMap.values().removeIf(child::equals);
        }
    }

    private Component childForNode(Node node) {
        if (node instanceof Leaf) {
            Leaf leaf = (Leaf) node;
            String name = leaf.getName();
            return (name != null) ? childMap.get(name) : null;
        }
        return null;
    }

    private Dimension preferredComponentSize(Node node) {
        Component child = childForNode(node);
        return (child != null) ? child.getPreferredSize() : new Dimension(0, 0);

    }

    private Dimension preferredNodeSize(Node root) {
        if (root instanceof Leaf)
            return preferredComponentSize(root);
        else if (root instanceof Divider) {
            int dividerSize = getDividerSize();
            return new Dimension(dividerSize, dividerSize);
        } else {
            Split split = (Split) root;
            List<Node> splitChildren = split.getChildren();
            int width = 0;
            int height = 0;
            if (split.isRowLayout()) {
                for (Node splitChild : splitChildren) {
                    Dimension size = preferredNodeSize(splitChild);
                    width += size.width;
                    height = Math.max(height, size.height);
                }
            } else {
                for (Node splitChild : splitChildren) {
                    Dimension size = preferredNodeSize(splitChild);
                    width = Math.max(width, size.width);
                    height += size.height;
                }
            }
            return new Dimension(width, height);
        }
    }

    private Dimension minimumNodeSize(Node root) {
        if (root instanceof Leaf) {
            Component child = childForNode(root);
            return (child != null) ? child.getMinimumSize() : new Dimension(0, 0);
        } else if (root instanceof Divider) {
            int dividerSize = getDividerSize();
            return new Dimension(dividerSize, dividerSize);
        } else {
            Split split = (Split) root;
            List<Node> splitChildren = split.getChildren();
            int width = 0;
            int height = 0;
            if (split.isRowLayout()) {
                for (Node splitChild : splitChildren) {
                    Dimension size = minimumNodeSize(splitChild);
                    width += size.width;
                    height = Math.max(height, size.height);
                }
            } else {
                for (Node splitChild : splitChildren) {
                    Dimension size = minimumNodeSize(splitChild);
                    width = Math.max(width, size.width);
                    height += size.height;
                }
            }
            return new Dimension(width, height);
        }
    }

    private static Dimension sizeWithInsets(Container parent, Dimension size) {
        Insets insets = parent.getInsets();
        int width = size.width + insets.left + insets.right;
        int height = size.height + insets.top + insets.bottom;
        return new Dimension(width, height);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Dimension size = preferredNodeSize(getModel());
        return sizeWithInsets(parent, size);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Dimension size = minimumNodeSize(getModel());
        return sizeWithInsets(parent, size);
    }

    private static Rectangle boundsWithYandHeight(Rectangle bounds, double y, double height) {
        Rectangle r = new Rectangle();
        r.setBounds((int) (bounds.getX()), (int) y, (int) (bounds.getWidth()), (int) height);
        return r;
    }

    private static Rectangle boundsWithXandWidth(Rectangle bounds, double x, double width) {
        Rectangle r = new Rectangle();
        r.setBounds((int) x, (int) (bounds.getY()), (int) width, (int) (bounds.getHeight()));
        return r;
    }

    private static void minimizeSplitBounds(Split split, Rectangle bounds) {
        Rectangle splitBounds = new Rectangle(bounds.x, bounds.y, 0, 0);
        List<Node> splitChildren = split.getChildren();
        Node lastChild = splitChildren.get(splitChildren.size() - 1);
        Rectangle lastChildBounds = lastChild.getBounds();
        if (split.isRowLayout()) {
            int lastChildMaxX = lastChildBounds.x + lastChildBounds.width;
            splitBounds.add(lastChildMaxX, bounds.y + bounds.height);
        } else {
            int lastChildMaxY = lastChildBounds.y + lastChildBounds.height;
            splitBounds.add(bounds.x + bounds.width, lastChildMaxY);
        }
        split.setBounds(splitBounds);
    }

    private void layoutShrink(Split split, Rectangle bounds) {
        Rectangle splitBounds = split.getBounds();
        ListIterator<Node> splitChildren = split.getChildren().listIterator();

        if (split.isRowLayout()) {
            int totalWidth = 0;          // sum of the children's widths
            int minWeightedWidth = 0;    // sum of the weighted childrens' min widths
            int totalWeightedWidth = 0;  // sum of the weighted childrens' widths
            for (Node splitChild : split.getChildren()) {
                int nodeWidth = splitChild.getBounds().width;
                int nodeMinWidth = Math.min(nodeWidth, minimumNodeSize(splitChild).width);
                totalWidth += nodeWidth;
                if (splitChild.getWeight() > 0.0) {
                    minWeightedWidth += nodeMinWidth;
                    totalWeightedWidth += nodeWidth;
                }
            }

            double x = bounds.getX();
            double extraWidth = splitBounds.getWidth() - bounds.getWidth();
            double availableWidth = extraWidth;
            boolean onlyShrinkWeightedComponents =
                (totalWeightedWidth - minWeightedWidth) > extraWidth;

            while (splitChildren.hasNext()) {
                Node splitChild = splitChildren.next();
                Rectangle splitChildBounds = splitChild.getBounds();
                double minSplitChildWidth = minimumNodeSize(splitChild).getWidth();
                double splitChildWeight = onlyShrinkWeightedComponents
                ? splitChild.getWeight()
                        : (splitChildBounds.getWidth() / totalWidth);

                if (!splitChildren.hasNext()) {
                    double newWidth = Math.max(minSplitChildWidth, bounds.getMaxX() - x);
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                    layout2(splitChild, newSplitChildBounds);
                } else if ((availableWidth > 0.0) && (splitChildWeight > 0.0)) {
                    double allocatedWidth = Math.rint(splitChildWeight * extraWidth);
                    double oldWidth = splitChildBounds.getWidth();
                    double newWidth = Math.max(minSplitChildWidth, oldWidth - allocatedWidth);
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                    layout2(splitChild, newSplitChildBounds);
                    availableWidth -= (oldWidth - splitChild.getBounds().getWidth());
                } else {
                    double existingWidth = splitChildBounds.getWidth();
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, existingWidth);
                    layout2(splitChild, newSplitChildBounds);
                }
                x = splitChild.getBounds().getMaxX();
            }
        } else {
            int totalHeight = 0;          // sum of the children's heights
            int minWeightedHeight = 0;    // sum of the weighted childrens' min heights
            int totalWeightedHeight = 0;  // sum of the weighted childrens' heights
            for (Node splitChild : split.getChildren()) {
                int nodeHeight = splitChild.getBounds().height;
                int nodeMinHeight = Math.min(nodeHeight, minimumNodeSize(splitChild).height);
                totalHeight += nodeHeight;
                if (splitChild.getWeight() > 0.0) {
                    minWeightedHeight += nodeMinHeight;
                    totalWeightedHeight += nodeHeight;
                }
            }

            double y = bounds.getY();
            double extraHeight = splitBounds.getHeight() - bounds.getHeight();
            double availableHeight = extraHeight;
            boolean onlyShrinkWeightedComponents =
                (totalWeightedHeight - minWeightedHeight) > extraHeight;

            while (splitChildren.hasNext()) {
                Node splitChild = splitChildren.next();
                Rectangle splitChildBounds = splitChild.getBounds();
                double minSplitChildHeight = minimumNodeSize(splitChild).getHeight();
                double splitChildWeight = onlyShrinkWeightedComponents
                ? splitChild.getWeight()
                        : (splitChildBounds.getHeight() / totalHeight);

                if (!splitChildren.hasNext()) {
                    double oldHeight = splitChildBounds.getHeight();
                    double newHeight = Math.max(minSplitChildHeight, bounds.getMaxY() - y);
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                    layout2(splitChild, newSplitChildBounds);
                    availableHeight -= (oldHeight - splitChild.getBounds().getHeight());
                } else if ((availableHeight > 0.0) && (splitChildWeight > 0.0)) {
                    double allocatedHeight = Math.rint(splitChildWeight * extraHeight);
                    double oldHeight = splitChildBounds.getHeight();
                    double newHeight = Math.max(minSplitChildHeight, oldHeight - allocatedHeight);
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                    layout2(splitChild, newSplitChildBounds);
                    availableHeight -= (oldHeight - splitChild.getBounds().getHeight());
                } else {
                    double existingHeight = splitChildBounds.getHeight();
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, existingHeight);
                    layout2(splitChild, newSplitChildBounds);
                }
                y = splitChild.getBounds().getMaxY();
            }
        }

        /* The bounds of the Split node root are set to be
         * big enough to contain all of its children. Since
         * Leaf children can't be reduced below their
         * (corresponding java.awt.Component) minimum sizes,
         * the size of the Split's bounds maybe be larger than
         * the bounds we were asked to fit within.
         */
        minimizeSplitBounds(split, bounds);
    }

    private void layoutGrow(Split split, Rectangle bounds) {
        Rectangle splitBounds = split.getBounds();
        ListIterator<Node> splitChildren = split.getChildren().listIterator();
        Node lastWeightedChild = split.lastWeightedChild();

        if (split.isRowLayout()) {
            /* Layout the Split's child Nodes' along the X axis.  The bounds
             * of each child will have the same y coordinate and height as the
             * layoutGrow() bounds argument.  Extra width is allocated to the
             * to each child with a non-zero weight:
             *     newWidth = currentWidth + (extraWidth * splitChild.getWeight())
             * Any extraWidth "left over" (that's availableWidth in the loop
             * below) is given to the last child.  Note that Dividers always
             * have a weight of zero, and they're never the last child.
             */
            double x = bounds.getX();
            double extraWidth = bounds.getWidth() - splitBounds.getWidth();
            double availableWidth = extraWidth;

            while (splitChildren.hasNext()) {
                Node splitChild = splitChildren.next();
                Rectangle splitChildBounds = splitChild.getBounds();
                double splitChildWeight = splitChild.getWeight();

                if (!splitChildren.hasNext()) {
                    double newWidth = bounds.getMaxX() - x;
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                    layout2(splitChild, newSplitChildBounds);
                } else if ((availableWidth > 0.0) && (splitChildWeight > 0.0)) {
                    double allocatedWidth = splitChild.equals(lastWeightedChild)
                    ? availableWidth
                            : Math.rint(splitChildWeight * extraWidth);
                    double newWidth = splitChildBounds.getWidth() + allocatedWidth;
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                    layout2(splitChild, newSplitChildBounds);
                    availableWidth -= allocatedWidth;
                } else {
                    double existingWidth = splitChildBounds.getWidth();
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, existingWidth);
                    layout2(splitChild, newSplitChildBounds);
                }
                x = splitChild.getBounds().getMaxX();
            }
        } else {
            /* Layout the Split's child Nodes' along the Y axis.  The bounds
             * of each child will have the same x coordinate and width as the
             * layoutGrow() bounds argument.  Extra height is allocated to the
             * to each child with a non-zero weight:
             *     newHeight = currentHeight + (extraHeight * splitChild.getWeight())
             * Any extraHeight "left over" (that's availableHeight in the loop
             * below) is given to the last child.  Note that Dividers always
             * have a weight of zero, and they're never the last child.
             */
            double y = bounds.getY();
            double extraHeight = bounds.getMaxY() - splitBounds.getHeight();
            double availableHeight = extraHeight;

            while (splitChildren.hasNext()) {
                Node splitChild = splitChildren.next();
                Rectangle splitChildBounds = splitChild.getBounds();
                double splitChildWeight = splitChild.getWeight();

                if (!splitChildren.hasNext()) {
                    double newHeight = bounds.getMaxY() - y;
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                    layout2(splitChild, newSplitChildBounds);
                } else if ((availableHeight > 0.0) && (splitChildWeight > 0.0)) {
                    double allocatedHeight = splitChild.equals(lastWeightedChild)
                    ? availableHeight
                            : Math.rint(splitChildWeight * extraHeight);
                    double newHeight = splitChildBounds.getHeight() + allocatedHeight;
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                    layout2(splitChild, newSplitChildBounds);
                    availableHeight -= allocatedHeight;
                } else {
                    double existingHeight = splitChildBounds.getHeight();
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, existingHeight);
                    layout2(splitChild, newSplitChildBounds);
                }
                y = splitChild.getBounds().getMaxY();
            }
        }
    }

    /* Second pass of the layout algorithm: branch to layoutGrow/Shrink
     * as needed.
     */
    private void layout2(Node root, Rectangle bounds) {
        if (root instanceof Leaf) {
            Component child = childForNode(root);
            if (child != null) {
                child.setBounds(bounds);
            }
            root.setBounds(bounds);
        } else if (root instanceof Divider) {
            root.setBounds(bounds);
        } else if (root instanceof Split) {
            Split split = (Split) root;
            boolean grow = split.isRowLayout()
            ? split.getBounds().width <= bounds.width
                    : (split.getBounds().height <= bounds.height);
            if (grow) {
                layoutGrow(split, bounds);
                root.setBounds(bounds);
            } else {
                layoutShrink(split, bounds);
                // split.setBounds() called in layoutShrink()
            }
        }
    }

    /* First pass of the layout algorithm.
     *
     * If the Dividers are "floating" then set the bounds of each
     * node to accommodate the preferred size of all of the
     * Leaf's java.awt.Components.  Otherwise, just set the bounds
     * of each Leaf/Split node so that it's to the left of (for
     * Split.isRowLayout() Split children) or directly above
     * the Divider that follows.
     *
     * This pass sets the bounds of each Node in the layout model.  It
     * does not resize any of the parent Container's
     * (java.awt.Component) children.  That's done in the second pass,
     * see layoutGrow() and layoutShrink().
     */
    private void layout1(Node root, Rectangle bounds) {
        if (root instanceof Leaf) {
            root.setBounds(bounds);
        } else if (root instanceof Split) {
            Split split = (Split) root;
            Iterator<Node> splitChildren = split.getChildren().iterator();
            Rectangle childBounds;
            int dividerSize = getDividerSize();

            /* Layout the Split's child Nodes' along the X axis.  The bounds
             * of each child will have the same y coordinate and height as the
             * layout1() bounds argument.
             *
             * Note: the column layout code - that's the "else" clause below
             * this if, is identical to the X axis (rowLayout) code below.
             */
            if (split.isRowLayout()) {
                double x = bounds.getX();
                while (splitChildren.hasNext()) {
                    Node splitChild = splitChildren.next();
                    Divider dividerChild = null;
                    if (splitChildren.hasNext()) {
                        Node next = splitChildren.next();
                        if (next instanceof Divider) {
                            dividerChild = (Divider) next;
                        }
                    }

                    double childWidth;
                    if (getFloatingDividers()) {
                        childWidth = preferredNodeSize(splitChild).getWidth();
                    } else {
                        if (dividerChild != null) {
                            childWidth = dividerChild.getBounds().getX() - x;
                        } else {
                            childWidth = split.getBounds().getMaxX() - x;
                        }
                    }
                    childBounds = boundsWithXandWidth(bounds, x, childWidth);
                    layout1(splitChild, childBounds);

                    if (getFloatingDividers() && (dividerChild != null)) {
                        double dividerX = childBounds.getMaxX();
                        Rectangle dividerBounds = boundsWithXandWidth(bounds, dividerX, dividerSize);
                        dividerChild.setBounds(dividerBounds);
                    }
                    if (dividerChild != null) {
                        x = dividerChild.getBounds().getMaxX();
                    }
                }
            } else {
                /* Layout the Split's child Nodes' along the Y axis.  The bounds
                 * of each child will have the same x coordinate and width as the
                 * layout1() bounds argument.  The algorithm is identical to what's
                 * explained above, for the X axis case.
                 */
                double y = bounds.getY();
                while (splitChildren.hasNext()) {
                    Node splitChild = splitChildren.next();
                    Node nodeChild = splitChildren.hasNext() ? splitChildren.next() : null;
                    Divider dividerChild = nodeChild instanceof Divider ? (Divider) nodeChild : null;
                    double childHeight;
                    if (getFloatingDividers()) {
                        childHeight = preferredNodeSize(splitChild).getHeight();
                    } else {
                        if (dividerChild != null) {
                            childHeight = dividerChild.getBounds().getY() - y;
                        } else {
                            childHeight = split.getBounds().getMaxY() - y;
                        }
                    }
                    childBounds = boundsWithYandHeight(bounds, y, childHeight);
                    layout1(splitChild, childBounds);

                    if (getFloatingDividers() && (dividerChild != null)) {
                        double dividerY = childBounds.getMaxY();
                        Rectangle dividerBounds = boundsWithYandHeight(bounds, dividerY, dividerSize);
                        dividerChild.setBounds(dividerBounds);
                    }
                    if (dividerChild != null) {
                        y = dividerChild.getBounds().getMaxY();
                    }
                }
            }
            /* The bounds of the Split node root are set to be just
             * big enough to contain all of its children, but only
             * along the axis it's allocating space on.  That's
             * X for rows, Y for columns.  The second pass of the
             * layout algorithm - see layoutShrink()/layoutGrow()
             * allocates extra space.
             */
            minimizeSplitBounds(split, bounds);
        }
    }

    /**
     * The specified Node is either the wrong type or was configured incorrectly.
     */
    public static class InvalidLayoutException extends RuntimeException {
        private final transient Node node;

        /**
         * Constructs a new {@code InvalidLayoutException}.
         * @param msg the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         * @param node node
         */
        public InvalidLayoutException(String msg, Node node) {
            super(msg);
            this.node = node;
        }

        /**
         * Returns the invalid Node.
         * @return the invalid Node.
         */
        public Node getNode() {
            return node;
        }
    }

    private static void throwInvalidLayout(String msg, Node node) {
        throw new InvalidLayoutException(msg, node);
    }

    private static void checkLayout(Node root) {
        if (root instanceof Split) {
            Split split = (Split) root;
            if (split.getChildren().size() <= 2) {
                throwInvalidLayout("Split must have > 2 children", root);
            }
            Iterator<Node> splitChildren = split.getChildren().iterator();
            double weight = 0.0;
            while (splitChildren.hasNext()) {
                Node splitChild = splitChildren.next();
                if (splitChild instanceof Divider) {
                    throwInvalidLayout("expected a Split or Leaf Node", splitChild);
                }
                if (splitChildren.hasNext()) {
                    Node dividerChild = splitChildren.next();
                    if (!(dividerChild instanceof Divider)) {
                        throwInvalidLayout("expected a Divider Node", dividerChild);
                    }
                }
                weight += splitChild.getWeight();
                checkLayout(splitChild);
            }
            if (weight > 1.0 + 0.000000001) { /* add some epsilon to a double check */
                throwInvalidLayout("Split children's total weight > 1.0", root);
            }
        }
    }

    /**
     * Compute the bounds of all of the Split/Divider/Leaf Nodes in
     * the layout model, and then set the bounds of each child component
     * with a matching Leaf Node.
     */
    @Override
    public void layoutContainer(Container parent) {
        checkLayout(getModel());
        Insets insets = parent.getInsets();
        Dimension size = parent.getSize();
        int width = size.width - (insets.left + insets.right);
        int height = size.height - (insets.top + insets.bottom);
        Rectangle bounds = new Rectangle(insets.left, insets.top, width, height);
        layout1(getModel(), bounds);
        layout2(getModel(), bounds);
    }

    private static Divider dividerAt(Node root, int x, int y) {
        if (root instanceof Divider) {
            Divider divider = (Divider) root;
            return divider.getBounds().contains(x, y) ? divider : null;
        } else if (root instanceof Split) {
            Split split = (Split) root;
            return split.getChildren().stream()
                    .filter(child -> child.getBounds().contains(x, y))
                    .findFirst()
                    .map(child -> dividerAt(child, x, y))
                    .orElse(null);
        }
        return null;
    }

    /**
     * Return the Divider whose bounds contain the specified
     * point, or null if there isn't one.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return the Divider at x,y
     */
    public Divider dividerAt(int x, int y) {
        return dividerAt(getModel(), x, y);
    }

    private static boolean nodeOverlapsRectangle(Node node, Rectangle r2) {
        Rectangle r1 = node.getBounds();
        return
        (r1.x <= (r2.x + r2.width)) && ((r1.x + r1.width) >= r2.x) &&
        (r1.y <= (r2.y + r2.height)) && ((r1.y + r1.height) >= r2.y);
    }

    private static List<Divider> dividersThatOverlap(Node root, Rectangle r) {
        if (nodeOverlapsRectangle(root, r) && (root instanceof Split)) {
            List<Divider> dividers = new ArrayList<>();
            for (Node child : ((Split) root).getChildren()) {
                if (child instanceof Divider) {
                    if (nodeOverlapsRectangle(child, r)) {
                        dividers.add((Divider) child);
                    }
                } else if (child instanceof Split) {
                    dividers.addAll(dividersThatOverlap(child, r));
                }
            }
            return Collections.unmodifiableList(dividers);
        } else
            return Collections.emptyList();
    }

    /**
     * Return the Dividers whose bounds overlap the specified
     * Rectangle.
     *
     * @param r target Rectangle
     * @return the Dividers that overlap r
     * @throws IllegalArgumentException if the Rectangle is null
     */
    public List<Divider> dividersThatOverlap(Rectangle r) {
        CheckParameterUtil.ensureParameterNotNull(r, "r");
        return dividersThatOverlap(getModel(), r);
    }

    /**
     * Base class for the nodes that model a MultiSplitLayout.
     */
    public static class Node {
        private Split parent;
        private Rectangle bounds = new Rectangle();
        private double weight;

        /**
         * Constructs a new {@code Node}.
         */
        protected Node() {
            // Default constructor for subclasses only
        }

        /**
         * Returns the Split parent of this Node, or null.
         *
         * This method isn't called getParent(), in order to avoid problems
         * with recursive object creation when using XmlDecoder.
         *
         * @return the value of the parent property.
         * @see #setParent
         */
        public Split getParent() {
            return parent;
        }

        /**
         * Set the value of this Node's parent property.  The default
         * value of this property is null.
         *
         * This method isn't called setParent(), in order to avoid problems
         * with recursive object creation when using XmlEncoder.
         *
         * @param parent a Split or null
         * @see #getParent
         */
        public void setParent(Split parent) {
            this.parent = parent;
        }

        /**
         * Returns the bounding Rectangle for this Node.
         *
         * @return the value of the bounds property.
         * @see #setBounds
         */
        public Rectangle getBounds() {
            return new Rectangle(this.bounds);
        }

        /**
         * Set the bounding Rectangle for this node.  The value of
         * bounds may not be null.  The default value of bounds
         * is equal to <code>new Rectangle(0,0,0,0)</code>.
         *
         * @param bounds the new value of the bounds property
         * @throws IllegalArgumentException if bounds is null
         * @see #getBounds
         */
        public void setBounds(Rectangle bounds) {
            CheckParameterUtil.ensureParameterNotNull(bounds, "bounds");
            this.bounds = new Rectangle(bounds);
        }

        /**
         * Value between 0.0 and 1.0 used to compute how much space
         * to add to this sibling when the layout grows or how
         * much to reduce when the layout shrinks.
         *
         * @return the value of the weight property
         * @see #setWeight
         */
        public double getWeight() {
            return weight;
        }

        /**
         * The weight property is a between 0.0 and 1.0 used to
         * compute how much space to add to this sibling when the
         * layout grows or how much to reduce when the layout shrinks.
         * If rowLayout is true then this node's width grows
         * or shrinks by (extraSpace * weight).  If rowLayout is false,
         * then the node's height is changed.  The default value
         * of weight is 0.0.
         *
         * @param weight a double between 0.0 and 1.0
         * @throws IllegalArgumentException if weight is not between 0.0 and 1.0
         * @see #getWeight
         * @see MultiSplitLayout#layoutContainer
         */
        public void setWeight(double weight) {
            if ((weight < 0.0) || (weight > 1.0))
                throw new IllegalArgumentException("invalid weight");
            this.weight = weight;
        }

        private Node siblingAtOffset(int offset) {
            Split parent = getParent();
            if (parent == null)
                return null;
            List<Node> siblings = parent.getChildren();
            int index = siblings.indexOf(this);
            if (index == -1)
                return null;
            index += offset;
            return ((index > -1) && (index < siblings.size())) ? siblings.get(index) : null;
        }

        /**
         * Return the Node that comes after this one in the parent's
         * list of children, or null.  If this node's parent is null,
         * or if it's the last child, then return null.
         *
         * @return the Node that comes after this one in the parent's list of children.
         * @see #previousSibling
         * @see #getParent
         */
        public Node nextSibling() {
            return siblingAtOffset(+1);
        }

        /**
         * Return the Node that comes before this one in the parent's
         * list of children, or null.  If this node's parent is null,
         * or if it's the last child, then return null.
         *
         * @return the Node that comes before this one in the parent's list of children.
         * @see #nextSibling
         * @see #getParent
         */
        public Node previousSibling() {
            return siblingAtOffset(-1);
        }
    }

    /**
     * Defines a vertical or horizontal subdivision into two or more
     * tiles.
     */
    public static class Split extends Node {
        private List<Node> children = Collections.emptyList();
        private boolean rowLayout = true;

        /**
         * Returns true if the this Split's children are to be
         * laid out in a row: all the same height, left edge
         * equal to the previous Node's right edge.  If false,
         * children are laid on in a column.
         *
         * @return the value of the rowLayout property.
         * @see #setRowLayout
         */
        public boolean isRowLayout() {
            return rowLayout;
        }

        /**
         * Set the rowLayout property.  If true, all of this Split's
         * children are to be laid out in a row: all the same height,
         * each node's left edge equal to the previous Node's right
         * edge. If false, children are laid on in a column. Default value is true.
         *
         * @param rowLayout true for horizontal row layout, false for column
         * @see #isRowLayout
         */
        public void setRowLayout(boolean rowLayout) {
            this.rowLayout = rowLayout;
        }

        /**
         * Returns this Split node's children.  The returned value
         * is not a reference to the Split's internal list of children
         *
         * @return the value of the children property.
         * @see #setChildren
         */
        public List<Node> getChildren() {
            return new ArrayList<>(children);
        }

        /**
         * Set's the children property of this Split node.  The parent
         * of each new child is set to this Split node, and the parent
         * of each old child (if any) is set to null.  This method
         * defensively copies the incoming List. Default value is an empty List.
         *
         * @param children List of children
         * @throws IllegalArgumentException if children is null
         * @see #getChildren
         */
        public void setChildren(List<Node> children) {
            if (children == null)
                throw new IllegalArgumentException("children must be a non-null List");
            for (Node child : this.children) {
                child.setParent(null);
            }
            this.children = new ArrayList<>(children);
            for (Node child : this.children) {
                child.setParent(this);
            }
        }

        /**
         * Convenience method that returns the last child whose weight
         * is &gt; 0.0.
         *
         * @return the last child whose weight is &gt; 0.0.
         * @see #getChildren
         * @see Node#getWeight
         */
        public final Node lastWeightedChild() {
            List<Node> children = getChildren();
            Node weightedChild = null;
            for (Node child : children) {
                if (child.getWeight() > 0.0) {
                    weightedChild = child;
                }
            }
            return weightedChild;
        }

        @Override
        public String toString() {
            int nChildren = getChildren().size();
            StringBuilder sb = new StringBuilder("MultiSplitLayout.Split");
            sb.append(isRowLayout() ? " ROW [" : " COLUMN [")
              .append(nChildren + ((nChildren == 1) ? " child" : " children"))
              .append("] ")
              .append(getBounds());
            return sb.toString();
        }
    }

    /**
     * Models a java.awt Component child.
     */
    public static class Leaf extends Node {
        private String name = "";

        /**
         * Create a Leaf node. The default value of name is "".
         */
        public Leaf() {
            // Name can be set later with setName()
        }

        /**
         * Create a Leaf node with the specified name. Name can not be null.
         *
         * @param name value of the Leaf's name property
         * @throws IllegalArgumentException if name is null
         */
        public Leaf(String name) {
            CheckParameterUtil.ensureParameterNotNull(name, "name");
            this.name = name;
        }

        /**
         * Return the Leaf's name.
         *
         * @return the value of the name property.
         * @see #setName
         */
        public String getName() {
            return name;
        }

        /**
         * Set the value of the name property.  Name may not be null.
         *
         * @param name value of the name property
         * @throws IllegalArgumentException if name is null
         */
        public void setName(String name) {
            CheckParameterUtil.ensureParameterNotNull(name, "name");
            this.name = name;
        }

        @Override
        public String toString() {
            return new StringBuilder("MultiSplitLayout.Leaf \"")
              .append(getName())
              .append("\" weight=")
              .append(getWeight())
              .append(' ')
              .append(getBounds())
              .toString();
        }
    }

    /**
     * Models a single vertical/horiztonal divider.
     */
    public static class Divider extends Node {
        /**
         * Convenience method, returns true if the Divider's parent
         * is a Split row (a Split with isRowLayout() true), false
         * otherwise. In other words if this Divider's major axis
         * is vertical, return true.
         *
         * @return true if this Divider is part of a Split row.
         */
        public final boolean isVertical() {
            Split parent = getParent();
            return parent != null && parent.isRowLayout();
        }

        /**
         * Dividers can't have a weight, they don't grow or shrink.
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setWeight(double weight) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "MultiSplitLayout.Divider " + getBounds();
        }
    }
}
