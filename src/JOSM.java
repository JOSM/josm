// License: GPL. For details, see LICENSE file.
import org.openstreetmap.josm.gui.MainApplication;

/**
 * JOSM main class (entry point of the application).<br/>
 *
 * The name of the main class will be the name of the application menu on OS X.
 * so instead of exposing "org.openstreetmap.josm.gui.MainApplication" to the
 * user, we subclass it with a nicer name "JOSM".
 * An alternative is to set the name in the plist file for the  OS X Application Bundle.
 *
 * @since 1023
 */
public class JOSM extends MainApplication {

}
