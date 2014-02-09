/* code from: http://iharder.sourceforge.net/current/java/filedrop/
  (public domain) with only very small additions */
package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.OpenFileAction;

/**
 * This class makes it easy to drag and drop files from the operating
 * system to a Java program. Any {@link java.awt.Component} can be
 * dropped onto, but only {@link javax.swing.JComponent}s will indicate
 * the drop event with a changed border.
 * <p>
 * To use this class, construct a new <tt>FileDrop</tt> by passing
 * it the target component and a <tt>Listener</tt> to receive notification
 * when file(s) have been dropped. Here is an example:
 * <p>
 * <code>
 *      JPanel myPanel = new JPanel();
 *      new FileDrop( myPanel, new FileDrop.Listener()
 *      {   public void filesDropped( java.io.File[] files )
 *          {
 *              // handle file drop
 *              ...
 *          }   // end filesDropped
 *      }); // end FileDrop.Listener
 * </code>
 * <p>
 * You can specify the border that will appear when files are being dragged by
 * calling the constructor with a {@link javax.swing.border.Border}. Only
 * <tt>JComponent</tt>s will show any indication with a border.
 * <p>
 * You can turn on some debugging features by passing a <tt>PrintStream</tt>
 * object (such as <tt>System.out</tt>) into the full constructor. A <tt>null</tt>
 * value will result in no extra debugging information being output.
 * <p>
 *
 * <p>I'm releasing this code into the Public Domain. Enjoy.
 * </p>
 * <p><em>Original author: Robert Harder, rharder@usa.net</em></p>
 * <p>2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.</p>
 *
 * @author  Robert Harder
 * @author  rharder@users.sf.net
 * @version 1.0.1
 * @since 1231
 */
public class FileDrop
{
    private transient Border normalBorder;
    private transient DropTargetListener dropListener;

    /** Discover if the running JVM is modern enough to have drag and drop. */
    private static Boolean supportsDnD;

    // Default border color
    private static Color defaultBorderColor = new Color( 0f, 0f, 1f, 0.25f );

    /**
     * Constructor for JOSM file drop
     * @param c The drop target
     */
    public FileDrop(final Component c){
        this(
                c,     // Drop target
                BorderFactory.createMatteBorder( 2, 2, 2, 2, defaultBorderColor ), // Drag border
                true, // Recursive
                new FileDrop.Listener(){
                    @Override
                    public void filesDropped( File[] files ){
                        // start asynchronous loading of files
                        OpenFileAction.OpenFileTask task = new OpenFileAction.OpenFileTask(Arrays.asList(files), null);
                        task.setRecordHistory(true);
                        Main.worker.submit(task);
                    }
                }
        );
    }

    /**
     * Full constructor with a specified border and debugging optionally turned on.
     * With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for
     * the parameter <tt>out</tt> will result in no debugging output.
     *
     * @param c Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param recursive Recursively set children as drop targets.
     * @param listener Listens for <tt>filesDropped</tt>.
     */
    public FileDrop(
            final Component c,
            final Border dragBorder,
            final boolean recursive,
            final Listener listener)
    {

        if( supportsDnD() )
        {   // Make a drop listener
            dropListener = new DropTargetListener()
            {   @Override
                public void dragEnter( DropTargetDragEvent evt )
            {       Main.trace("FileDrop: dragEnter event." );

            // Is this an acceptable drag event?
            if( isDragOk( evt ) )
            {
                // If it's a Swing component, set its border
                if( c instanceof JComponent )
                {   JComponent jc = (JComponent) c;
                normalBorder = jc.getBorder();
                Main.trace("FileDrop: normal border saved." );
                jc.setBorder( dragBorder );
                Main.trace("FileDrop: drag border set." );
                }   // end if: JComponent

                // Acknowledge that it's okay to enter
                evt.acceptDrag( DnDConstants.ACTION_COPY );
                Main.trace("FileDrop: event accepted." );
            }   // end if: drag ok
            else
            {   // Reject the drag event
                evt.rejectDrag();
                Main.trace("FileDrop: event rejected." );
            }   // end else: drag not ok
            }   // end dragEnter

            @Override
            public void dragOver( DropTargetDragEvent evt )
            {   // This is called continually as long as the mouse is
                // over the drag target.
            }   // end dragOver

            @Override
            public void drop( DropTargetDropEvent evt )
            {   Main.trace("FileDrop: drop event." );
            try
            {   // Get whatever was dropped
                Transferable tr = evt.getTransferable();

                // Is it a file list?
                if (tr.isDataFlavorSupported (DataFlavor.javaFileListFlavor))
                {
                    // Say we'll take it.
                    evt.acceptDrop ( DnDConstants.ACTION_COPY );
                    Main.trace("FileDrop: file list accepted." );

                    // Get a useful list
                    List<?> fileList = (List<?>)tr.getTransferData(DataFlavor.javaFileListFlavor);

                    // Convert list to array
                    final File[] files = fileList.toArray(new File[fileList.size()]);

                    // Alert listener to drop.
                    if( listener != null ) {
                        listener.filesDropped( files );
                    }

                    // Mark that drop is completed.
                    evt.getDropTargetContext().dropComplete(true);
                    Main.trace("FileDrop: drop complete." );
                }   // end if: file list
                else // this section will check for a reader flavor.
                {
                    // Thanks, Nathan!
                    // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
                    DataFlavor[] flavors = tr.getTransferDataFlavors();
                    boolean handled = false;
                    for (DataFlavor flavor : flavors) {
                        if (flavor.isRepresentationClassReader()) {
                            // Say we'll take it.
                            evt.acceptDrop(DnDConstants.ACTION_COPY);
                            Main.trace("FileDrop: reader accepted.");

                            Reader reader = flavor.getReaderForText(tr);

                            BufferedReader br = new BufferedReader(reader);

                            if (listener != null) {
                                listener.filesDropped(createFileArray(br));
                            }

                            // Mark that drop is completed.
                            evt.getDropTargetContext().dropComplete(true);
                            Main.trace("FileDrop: drop complete.");
                            handled = true;
                            break;
                        }
                    }
                    if(!handled){
                        Main.trace("FileDrop: not a file list or reader - abort." );
                        evt.rejectDrop();
                    }
                    // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
                }   // end else: not a file list
            }   // end try
            catch ( IOException io)
            {   Main.warn("FileDrop: IOException - abort:" );
            Main.error(io);
            evt.rejectDrop();
            }   // end catch IOException
            catch (UnsupportedFlavorException ufe)
            {   Main.warn("FileDrop: UnsupportedFlavorException - abort:" );
            Main.error(ufe);
            evt.rejectDrop();
            }   // end catch: UnsupportedFlavorException
            finally
            {
                // If it's a Swing component, reset its border
                if( c instanceof JComponent )
                {   JComponent jc = (JComponent) c;
                jc.setBorder( normalBorder );
                Main.debug("FileDrop: normal border restored." );
                }   // end if: JComponent
            }   // end finally
            }   // end drop

            @Override
            public void dragExit( DropTargetEvent evt )
            {   Main.debug("FileDrop: dragExit event." );
            // If it's a Swing component, reset its border
            if( c instanceof JComponent )
            {   JComponent jc = (JComponent) c;
            jc.setBorder( normalBorder );
            Main.debug("FileDrop: normal border restored." );
            }   // end if: JComponent
            }   // end dragExit

            @Override
            public void dropActionChanged( DropTargetDragEvent evt )
            {   Main.debug("FileDrop: dropActionChanged event." );
            // Is this an acceptable drag event?
            if( isDragOk( evt ) )
            {
                evt.acceptDrag( DnDConstants.ACTION_COPY );
                Main.debug("FileDrop: event accepted." );
            }   // end if: drag ok
            else
            {   evt.rejectDrag();
            Main.debug("FileDrop: event rejected." );
            }   // end else: drag not ok
            }   // end dropActionChanged
            }; // end DropTargetListener

            // Make the component (and possibly children) drop targets
            makeDropTarget( c, recursive );
        }   // end if: supports dnd
        else
        {   Main.info("FileDrop: Drag and drop is not supported with this JVM" );
        }   // end else: does not support DnD
    }   // end constructor

    private static boolean supportsDnD()
    {   // Static Boolean
        if( supportsDnD == null )
        {
            boolean support = false;
            try {
                Class.forName( "java.awt.dnd.DnDConstants" );
                support = true;
            } catch( Exception e ) {
                support = false;
            }
            supportsDnD = support;
        }   // end if: first time through
        return supportsDnD.booleanValue();
    }   // end supportsDnD

    // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
    private static final String ZERO_CHAR_STRING = "" + (char)0;
    private static File[] createFileArray(BufferedReader bReader)
    {
        try {
            List<File> list = new ArrayList<File>();
            String line = null;
            while ((line = bReader.readLine()) != null) {
                try {
                    // kde seems to append a 0 char to the end of the reader
                    if (ZERO_CHAR_STRING.equals(line)) {
                        continue;
                    }

                    File file = new File(new URI(line));
                    list.add(file);
                } catch (Exception ex) {
                    Main.warn("Error with " + line + ": " + ex.getMessage());
                }
            }

            return list.toArray(new File[list.size()]);
        } catch (IOException ex) {
            Main.warn("FileDrop: IOException");
        }
        return new File[0];
    }
    // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.

    private void makeDropTarget( final Component c, boolean recursive )
    {
        // Make drop target
        final DropTarget dt = new DropTarget();
        try
        {   dt.addDropTargetListener( dropListener );
        }   // end try
        catch( TooManyListenersException e )
        {   Main.error(e);
        Main.warn("FileDrop: Drop will not work due to previous error. Do you have another listener attached?" );
        }   // end catch

        // Listen for hierarchy changes and remove the drop target when the parent gets cleared out.
        c.addHierarchyListener( new HierarchyListener()
        {   @Override
            public void hierarchyChanged( HierarchyEvent evt )
        {   Main.trace("FileDrop: Hierarchy changed." );
        Component parent = c.getParent();
        if( parent == null )
        {   c.setDropTarget( null );
        Main.trace("FileDrop: Drop target cleared from component." );
        }   // end if: null parent
        else
        {   new DropTarget(c, dropListener);
        Main.trace("FileDrop: Drop target added to component." );
        }   // end else: parent not null
        }   // end hierarchyChanged
        }); // end hierarchy listener
        if( c.getParent() != null ) {
            new DropTarget(c, dropListener);
        }

        if( recursive && (c instanceof Container ) )
        {
            // Get the container
            Container cont = (Container) c;

            // Get it's components
            Component[] comps = cont.getComponents();

            // Set it's components as listeners also
            for (Component comp : comps) {
                makeDropTarget( comp, recursive);
            }
        }   // end if: recursively set components as listener
    }   // end dropListener

    /** Determine if the dragged data is a file list. */
    private boolean isDragOk( final DropTargetDragEvent evt )
    {   boolean ok = false;

    // Get data flavors being dragged
    DataFlavor[] flavors = evt.getCurrentDataFlavors();

    // See if any of the flavors are a file list
    int i = 0;
    while( !ok && i < flavors.length )
    {
        // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
        // Is the flavor a file list?
        final DataFlavor curFlavor = flavors[i];
        if( curFlavor.equals( DataFlavor.javaFileListFlavor ) ||
                curFlavor.isRepresentationClassReader()){
            ok = true;
        }
        // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
        i++;
    }   // end while: through flavors

    // show data flavors
    if( flavors.length == 0 ) {
        Main.trace("FileDrop: no data flavors." );
    }
    for( i = 0; i < flavors.length; i++ ) {
        Main.trace(flavors[i].toString() );
    }

    return ok;
    }   // end isDragOk

    /**
     * Removes the drag-and-drop hooks from the component and optionally
     * from the all children. You should call this if you add and remove
     * components after you've set up the drag-and-drop.
     * This will recursively unregister all components contained within
     * <var>c</var> if <var>c</var> is a {@link java.awt.Container}.
     *
     * @param c The component to unregister as a drop target
     * @return {@code true} if at least one item has been removed, {@code false} otherwise
     */
    public static boolean remove( Component c)
    {   return remove( c, true );
    }   // end remove

    /**
     * Removes the drag-and-drop hooks from the component and optionally
     * from the all children. You should call this if you add and remove
     * components after you've set up the drag-and-drop.
     *
     * @param c The component to unregister
     * @param recursive Recursively unregister components within a container
     * @return {@code true} if at least one item has been removed, {@code false} otherwise
     */
    public static boolean remove( Component c, boolean recursive )
    {   // Make sure we support dnd.
        if (supportsDnD()) {
            Main.trace("FileDrop: Removing drag-and-drop hooks.");
            c.setDropTarget(null);
            if (recursive && (c instanceof Container)) {
                for (Component comp : ((Container) c).getComponents()) {
                    remove(comp, recursive);
                }
                return true;
            }   // end if: recursive
            else return false;
        }   // end if: supports DnD
        else return false;
    }   // end remove

    /* ********  I N N E R   I N T E R F A C E   L I S T E N E R  ******** */

    /**
     * Implement this inner interface to listen for when files are dropped. For example
     * your class declaration may begin like this:
     * <code>
     *      public class MyClass implements FileDrop.Listener
     *      ...
     *      public void filesDropped( java.io.File[] files )
     *      {
     *          ...
     *      }   // end filesDropped
     *      ...
     * </code>
     */
    public static interface Listener {

        /**
         * This method is called when files have been successfully dropped.
         *
         * @param files An array of <tt>File</tt>s that were dropped.
         */
        public abstract void filesDropped( File[] files );

    }   // end inner-interface Listener

    /* ********  I N N E R   C L A S S  ******** */

    /**
     * At last an easy way to encapsulate your custom objects for dragging and dropping
     * in your Java programs!
     * When you need to create a {@link java.awt.datatransfer.Transferable} object,
     * use this class to wrap your object.
     * For example:
     * <pre><code>
     *      ...
     *      MyCoolClass myObj = new MyCoolClass();
     *      Transferable xfer = new TransferableObject( myObj );
     *      ...
     * </code></pre>
     * Or if you need to know when the data was actually dropped, like when you're
     * moving data out of a list, say, you can use the {@link TransferableObject.Fetcher}
     * inner class to return your object Just in Time.
     * For example:
     * <pre><code>
     *      ...
     *      final MyCoolClass myObj = new MyCoolClass();
     *
     *      TransferableObject.Fetcher fetcher = new TransferableObject.Fetcher()
     *      {   public Object getObject(){ return myObj; }
     *      }; // end fetcher
     *
     *      Transferable xfer = new TransferableObject( fetcher );
     *      ...
     * </code></pre>
     *
     * The {@link java.awt.datatransfer.DataFlavor} associated with
     * {@link TransferableObject} has the representation class
     * <tt>net.iharder.dnd.TransferableObject.class</tt> and MIME type
     * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
     * This data flavor is accessible via the static
     * {@link #DATA_FLAVOR} property.
     *
     *
     * <p>I'm releasing this code into the Public Domain. Enjoy.</p>
     *
     * @author  Robert Harder
     * @author  rob@iharder.net
     * @version 1.2
     */
    public static class TransferableObject implements Transferable
    {
        /**
         * The MIME type for {@link #DATA_FLAVOR} is
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         */
        public final static String MIME_TYPE = "application/x-net.iharder.dnd.TransferableObject";

        /**
         * The default {@link java.awt.datatransfer.DataFlavor} for
         * {@link TransferableObject} has the representation class
         * <tt>net.iharder.dnd.TransferableObject.class</tt>
         * and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         */
        public final static DataFlavor DATA_FLAVOR =
            new DataFlavor( FileDrop.TransferableObject.class, MIME_TYPE );

        private Fetcher fetcher;
        private Object data;

        private DataFlavor customFlavor;

        /**
         * Creates a new {@link TransferableObject} that wraps <var>data</var>.
         * Along with the {@link #DATA_FLAVOR} associated with this class,
         * this creates a custom data flavor with a representation class
         * determined from <code>data.getClass()</code> and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @param data The data to transfer
         */
        public TransferableObject( Object data )
        {   this.data = data;
            this.customFlavor = new DataFlavor( data.getClass(), MIME_TYPE );
        }   // end constructor

        /**
         * Creates a new {@link TransferableObject} that will return the
         * object that is returned by <var>fetcher</var>.
         * No custom data flavor is set other than the default
         * {@link #DATA_FLAVOR}.
         *
         * @see Fetcher
         * @param fetcher The {@link Fetcher} that will return the data object
         */
        public TransferableObject( Fetcher fetcher )
        {   this.fetcher = fetcher;
        }   // end constructor

        /**
         * Creates a new {@link TransferableObject} that will return the
         * object that is returned by <var>fetcher</var>.
         * Along with the {@link #DATA_FLAVOR} associated with this class,
         * this creates a custom data flavor with a representation class <var>dataClass</var>
         * and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @see Fetcher
         * @param dataClass The {@link java.lang.Class} to use in the custom data flavor
         * @param fetcher The {@link Fetcher} that will return the data object
         */
        public TransferableObject(Class<?> dataClass, Fetcher fetcher )
        {   this.fetcher = fetcher;
        this.customFlavor = new DataFlavor( dataClass, MIME_TYPE );
        }   // end constructor

        /**
         * Returns the custom {@link java.awt.datatransfer.DataFlavor} associated
         * with the encapsulated object or <tt>null</tt> if the {@link Fetcher}
         * constructor was used without passing a {@link java.lang.Class}.
         *
         * @return The custom data flavor for the encapsulated object
         */
        public DataFlavor getCustomDataFlavor()
        {   return customFlavor;
        }   // end getCustomDataFlavor

        /* ********  T R A N S F E R A B L E   M E T H O D S  ******** */

        /**
         * Returns a two- or three-element array containing first
         * the custom data flavor, if one was created in the constructors,
         * second the default {@link #DATA_FLAVOR} associated with
         * {@link TransferableObject}, and third the
         * {@link java.awt.datatransfer.DataFlavor#stringFlavor}.
         *
         * @return An array of supported data flavors
         */
        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            if( customFlavor != null )
                return new DataFlavor[]
                                                            {   customFlavor,
                    DATA_FLAVOR,
                    DataFlavor.stringFlavor
                                                            };  // end flavors array
            else
                return new DataFlavor[]
                                                            {   DATA_FLAVOR,
                    DataFlavor.stringFlavor
                                                            };  // end flavors array
        }   // end getTransferDataFlavors

        /**
         * Returns the data encapsulated in this {@link TransferableObject}.
         * If the {@link Fetcher} constructor was used, then this is when
         * the {@link Fetcher#getObject getObject()} method will be called.
         * If the requested data flavor is not supported, then the
         * {@link Fetcher#getObject getObject()} method will not be called.
         *
         * @param flavor The data flavor for the data to return
         * @return The dropped data
         */
        @Override
        public Object getTransferData( DataFlavor flavor )
        throws UnsupportedFlavorException, IOException
        {
            // Native object
            if( flavor.equals( DATA_FLAVOR ) )
                return fetcher == null ? data : fetcher.getObject();

            // String
            if( flavor.equals( DataFlavor.stringFlavor ) )
                return fetcher == null ? data.toString() : fetcher.getObject().toString();

                // We can't do anything else
                throw new UnsupportedFlavorException(flavor);
        }   // end getTransferData

        /**
         * Returns <tt>true</tt> if <var>flavor</var> is one of the supported
         * flavors. Flavors are supported using the <code>equals(...)</code> method.
         *
         * @param flavor The data flavor to check
         * @return Whether or not the flavor is supported
         */
        @Override
        public boolean isDataFlavorSupported( DataFlavor flavor )
        {
            // Native object
            if( flavor.equals( DATA_FLAVOR ) )
                return true;

            // String
            if( flavor.equals( DataFlavor.stringFlavor ) )
                return true;

            // We can't do anything else
            return false;
        }   // end isDataFlavorSupported

        /* ********  I N N E R   I N T E R F A C E   F E T C H E R  ******** */

        /**
         * Instead of passing your data directly to the {@link TransferableObject}
         * constructor, you may want to know exactly when your data was received
         * in case you need to remove it from its source (or do anyting else to it).
         * When the {@link #getTransferData getTransferData(...)} method is called
         * on the {@link TransferableObject}, the {@link Fetcher}'s
         * {@link #getObject getObject()} method will be called.
         *
         * @author Robert Harder
         */
        public static interface Fetcher
        {
            /**
             * Return the object being encapsulated in the
             * {@link TransferableObject}.
             *
             * @return The dropped object
             */
            public abstract Object getObject();
        }   // end inner interface Fetcher

    }   // end class TransferableObject

}   // end class FileDrop
