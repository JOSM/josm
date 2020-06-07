// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;

/**
 * This is an abstract base class for dialogs used for entering generic upload options.
 * @since 7358
 */
public abstract class AbstractUploadDialog extends JDialog implements IUploadDialog {

    private boolean canceled;

    /**
     * Creates a dialog with an empty title and the specified modality and
     * {@code Window} as its owner.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param modalityType specifies whether dialog blocks input to other
     *     windows when shown. {@code null} value and unsupported modality
     *     types are equivalent to {@code MODELESS}
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException
     *     if the calling thread does not have permission to create modal dialogs
     *     with the given {@code modalityType}
     *
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    protected AbstractUploadDialog(Window owner, ModalityType modalityType) {
        super(owner, modalityType);
    }

    /**
     * Creates a dialog with the specified title, owner {@code Window},
     * modality and {@code GraphicsConfiguration}.
     * <p>
     * NOTE: Any popup components ({@code JComboBox},
     * {@code JPopupMenu}, {@code JMenuBar})
     * created within a modal dialog will be forced to be lightweight.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param title the {@code String} to display in the dialog's
     *     title bar or {@code null} if the dialog has no title
     * @param modalityType specifies whether dialog blocks input to other
     *     windows when shown. {@code null} value and unsupported modality
     *     types are equivalent to {@code MODELESS}
     * @param gc the {@code GraphicsConfiguration} of the target screen device;
     *     if {@code null}, the default system {@code GraphicsConfiguration}
     *     is assumed
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException
     *     if the calling thread does not have permission to create modal dialogs
     *     with the given {@code modalityType}
     *
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    protected AbstractUploadDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
    }

    /**
     * Creates a dialog with the specified title, owner {@code Window} and
     * modality.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param title the {@code String} to display in the dialog's
     *     title bar or {@code null} if the dialog has no title
     * @param modalityType specifies whether dialog blocks input to other
     *     windows when shown. {@code null} value and unsupported modality
     *     types are equivalent to {@code MODELESS}
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     * @throws SecurityException
     *     if the calling thread does not have permission to create modal dialogs
     *     with the given {@code modalityType}
     *
     * @see java.awt.Dialog.ModalityType
     * @see java.awt.Dialog#setModal
     * @see java.awt.Dialog#setModalityType
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    protected AbstractUploadDialog(Window owner, String title, ModalityType modalityType) {
        super(owner, title, modalityType);
    }

    /**
     * Creates a modeless dialog with the specified title and owner
     * {@code Window}.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     * @param title the {@code String} to display in the dialog's
     *     title bar or {@code null} if the dialog has no title
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    protected AbstractUploadDialog(Window owner, String title) {
        super(owner, title);
    }

    /**
     * Creates a modeless dialog with the specified {@code Window}
     * as its owner and an empty title.
     * <p>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     *
     * @param owner the {@code Window} from which the dialog is displayed or
     *     {@code null} if this dialog has no owner
     *
     * @throws IllegalArgumentException
     *     if the {@code owner} is not an instance of {@link java.awt.Dialog Dialog}
     *     or {@link java.awt.Frame Frame}
     * @throws IllegalArgumentException
     *     if the {@code owner}'s {@code GraphicsConfiguration} is not from a screen device
     * @throws HeadlessException
     *     when {@code GraphicsEnvironment.isHeadless()} returns {@code true}
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see JComponent#getDefaultLocale
     */
    protected AbstractUploadDialog(Window owner) {
        super(owner);
    }

    @Override
    public final boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets whether the dialog was canceled
     *
     * @param canceled true if the dialog is canceled
     */
    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void rememberUserInput() {
        // Override if needed
    }
}
