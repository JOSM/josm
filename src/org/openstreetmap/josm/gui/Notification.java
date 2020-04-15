// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Component;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * A Notification Message similar to a popup window, but without disrupting the
 * user's workflow.
 *
 * Non-modal info panel that vanishes after a certain time.
 *
 * This class only holds the data for a notification, {@link NotificationManager}
 * is responsible for building the message panel and displaying it on screen.
 *
 * example:
 * <pre>
 *      Notification note = new Notification("Hi there!");
 *      note.setIcon(JOptionPane.INFORMATION_MESSAGE); // optional
 *      note.setDuration(Notification.TIME_SHORT); // optional
 *      note.show();
 * </pre>
 */
public class Notification {

    /**
     * Default width of a notification
     */
    public static final int DEFAULT_CONTENT_WIDTH = 350;

    // some standard duration values (in milliseconds)

    /**
     * Very short and very easy to grasp message (3 s).
     * E.g. "Please select at least one node".
     */
    public static final int TIME_SHORT = Config.getPref().getInt("notification-time-short-ms", 3000);

    /**
     * Short message of one or two lines (5 s).
     */
    public static final int TIME_DEFAULT = Config.getPref().getInt("notification-time-default-ms", 5000);

    /**
     * Somewhat longer message (10 s).
     */
    public static final int TIME_LONG = Config.getPref().getInt("notification-time-long-ms", 10_000);

    /**
     * Long text.
     * (Make sure is still sensible to show as a notification)
     */
    public static final int TIME_VERY_LONG = Config.getPref().getInt("notification-time-very_long-ms", 20_000);

    private Component content;
    private int duration = Notification.TIME_DEFAULT;
    private Icon icon;
    private String helpTopic;

    /**
     * Constructs a new {@code Notification} without content.
     */
    public Notification() {
        // nothing to do.
    }

    /**
     * Constructs a new {@code Notification} with the given textual content.
     * @param msg The text to display
     */
    public Notification(String msg) {
        this();
        setContent(msg);
    }

    /**
     * Set the content of the message.
     *
     * @param content any Component to be shown
     *
     * @return the current Object, for convenience
     * @see #setContent(java.lang.String)
     */
    public Notification setContent(Component content) {
        this.content = content;
        return this;
    }

    /**
     * Set the notification text. (Convenience method)
     *
     * @param msg the message String. Will be wrapped in &lt;html&gt;, so
     * you can use &lt;br&gt; and other markup directly.
     *
     * @return the current Object, for convenience
     * @see #Notification(java.lang.String)
     */
    public Notification setContent(String msg) {
        JMultilineLabel lbl = new JMultilineLabel(msg);
        lbl.setMaxWidth(DEFAULT_CONTENT_WIDTH);
        lbl.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        lbl.setForeground(Color.BLACK);
        content = lbl;
        return this;
    }

    /**
     * Set the time after which the message is hidden.
     *
     * @param duration the time (in milliseconds)
     * Preset values {@link #TIME_SHORT}, {@link #TIME_DEFAULT}, {@link #TIME_LONG}
     * and {@link #TIME_VERY_LONG} can be used.
     * @return the current Object, for convenience
     */
    public Notification setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    /**
     * Set an icon to display on the left part of the message window.
     *
     * @param icon the icon (null means no icon is displayed)
     * @return the current Object, for convenience
     */
    public Notification setIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    /**
     * Set an icon to display on the left part of the message window by
     * choosing from the default JOptionPane icons.
     *
     * @param messageType one of the following: JOptionPane.ERROR_MESSAGE,
     * JOptionPane.INFORMATION_MESSAGE, JOptionPane.WARNING_MESSAGE,
     * JOptionPane.QUESTION_MESSAGE, JOptionPane.PLAIN_MESSAGE
     * @return the current Object, for convenience
     */
    public Notification setIcon(int messageType) {
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.errorIcon"));
            case JOptionPane.INFORMATION_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.informationIcon"));
            case JOptionPane.WARNING_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.warningIcon"));
            case JOptionPane.QUESTION_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.questionIcon"));
            case JOptionPane.PLAIN_MESSAGE:
                return setIcon(null);
            default:
                throw new IllegalArgumentException("Unknown message type!");
        }
    }

    /**
     * Display a help button at the bottom of the notification window.
     * @param helpTopic the help topic
     * @return the current Object, for convenience
     */
    public Notification setHelpTopic(String helpTopic) {
        this.helpTopic = helpTopic;
        return this;
    }

    /**
     * Gets the content component to use.
     * @return The content
     */
    public Component getContent() {
        return content;
    }

    /**
     * Gets the time the notification should be displayed
     * @return The time to display the notification
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets the icon that should be displayed next to the notification
     * @return The icon to display
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Gets the help topic for this notification
     * @return The help topic
     */
    public String getHelpTopic() {
        return helpTopic;
    }

    /**
     * Display the notification.
     */
    public void show() {
        NotificationManager.getInstance().showNotification(this);
    }

    private Object getContentTextOrComponent() {
        return content instanceof JTextComponent ? ((JTextComponent) content).getText() : content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return duration == that.duration
                && Objects.equals(getContentTextOrComponent(), that.getContentTextOrComponent())
                && Objects.equals(helpTopic, that.helpTopic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getContentTextOrComponent(), duration, helpTopic);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "content=" + getContentTextOrComponent() +
                ", duration=" + duration +
                ", helpTopic='" + helpTopic + '\'' +
                '}';
    }
}
