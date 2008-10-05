// License: GPL. Copyright 2007 by Immanuel Scholz and others
//Licence: GPL

// Note: The name of the main class will be the name of the application menu on OS X.
//       so instead of exposing "org.openstreetmap.josm.gui.MainApplication" to the
//       user, we subclass it with a nicer name "JOSM".
//       An alternative would be to set the name in the plist file---but JOSM usually
//       is not delivered as an OS X Application Bundle, so we have no plist file.

import org.openstreetmap.josm.gui.MainApplication;

public class JOSM extends MainApplication {}
