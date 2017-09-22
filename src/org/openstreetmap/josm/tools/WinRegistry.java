// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Utility class to access Window registry (read access only).
 * As the implementation relies on internal JDK class {@code java.util.prefs.WindowsPreferences} and its native JNI
 * method {@code Java_java_util_prefs_WindowsPreferences_WindowsRegQueryValueEx}, only String values (REG_SZ)
 * are supported.
 * Adapted from <a href="http://stackoverflow.com/a/6163701/2257172">StackOverflow</a>.
 * @since 12217
 */
public final class WinRegistry {

    /**
     * Registry entries subordinate to this key define the preferences of the current user.
     * These preferences include the settings of environment variables, data about program groups,
     * colors, printers, network connections, and application preferences.
     * See <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/ms724836(v=vs.85).aspx">Predefined Keys</a>
     */
    public static final int HKEY_CURRENT_USER = 0x80000001;

    /**
     * Registry entries subordinate to this key define the physical state of the computer, including data about the bus type,
     * system memory, and installed hardware and software. It contains subkeys that hold current configuration data,
     * including Plug and Play information (the Enum branch, which includes a complete list of all hardware that has ever been
     * on the system), network logon preferences, network security information, software-related information (such as server
     * names and the location of the server), and other system information.
     * See <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/ms724836(v=vs.85).aspx">Predefined Keys</a>
     */
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;

    private static final int REG_SUCCESS = 0;

    private static final int KEY_READ = 0x20019;
    private static final Preferences userRoot = Preferences.userRoot();
    private static final Preferences systemRoot = Preferences.systemRoot();
    private static final Class<? extends Preferences> userClass = userRoot.getClass();
    private static final Method regOpenKey;
    private static final Method regCloseKey;
    private static final Method regQueryValueEx;
    private static final Method regEnumValue;
    private static final Method regQueryInfoKey;
    private static final Method regEnumKeyEx;

    static {
        try {
            regOpenKey = userClass.getDeclaredMethod("WindowsRegOpenKey", int.class, byte[].class, int.class);
            regCloseKey = userClass.getDeclaredMethod("WindowsRegCloseKey", int.class);
            regQueryValueEx = userClass.getDeclaredMethod("WindowsRegQueryValueEx", int.class, byte[].class);
            regEnumValue = userClass.getDeclaredMethod("WindowsRegEnumValue", int.class, int.class, int.class);
            regQueryInfoKey = userClass.getDeclaredMethod("WindowsRegQueryInfoKey1", int.class);
            regEnumKeyEx = userClass.getDeclaredMethod("WindowsRegEnumKeyEx", int.class, int.class, int.class);
            Utils.setObjectsAccessible(regOpenKey, regCloseKey, regQueryValueEx, regEnumValue, regQueryInfoKey, regEnumKeyEx);
        } catch (RuntimeException | ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private WinRegistry() {
        // Hide default constructor for utilities classes
    }

    /**
     * Read a value from key and value name
     * @param hkey  HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  key name
     * @param valueName  value name
     * @return the value
     * @throws IllegalArgumentException if hkey not HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @throws IllegalAccessException if Java language access control is enforced and the underlying method is inaccessible
     * @throws InvocationTargetException if the underlying method throws an exception
     */
    public static String readString(int hkey, String key, String valueName)
            throws IllegalAccessException, InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readString(systemRoot, hkey, key, valueName);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readString(userRoot, hkey, key, valueName);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Read value(s) and value name(s) form given key
     * @param hkey  HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  key name
     * @return the value name(s) plus the value(s)
     * @throws IllegalArgumentException if hkey not HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @throws IllegalAccessException if Java language access control is enforced and the underlying method is inaccessible
     * @throws InvocationTargetException if the underlying method throws an exception
     */
    public static Map<String, String> readStringValues(int hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringValues(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringValues(userRoot, hkey, key);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Read the value name(s) from a given key
     * @param hkey  HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  key name
     * @return the value name(s)
     * @throws IllegalArgumentException if hkey not HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @throws IllegalAccessException if Java language access control is enforced and the underlying method is inaccessible
     * @throws InvocationTargetException if the underlying method throws an exception
     */
    public static List<String> readStringSubKeys(int hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringSubKeys(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringSubKeys(userRoot, hkey, key);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    // =====================

    private static String readString(Preferences root, int hkey, String key, String value)
            throws IllegalAccessException, InvocationTargetException {
        int[] handles = (int[]) regOpenKey.invoke(root, Integer.valueOf(hkey), toCstr(key), Integer.valueOf(KEY_READ));
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        byte[] valb = (byte[]) regQueryValueEx.invoke(root, Integer.valueOf(handles[0]), toCstr(value));
        regCloseKey.invoke(root, Integer.valueOf(handles[0]));
        return (valb != null ? new String(valb, StandardCharsets.UTF_8).trim() : null);
    }

    private static Map<String, String> readStringValues(Preferences root, int hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        HashMap<String, String> results = new HashMap<>();
        int[] handles = (int[]) regOpenKey.invoke(root, Integer.valueOf(hkey), toCstr(key), Integer.valueOf(KEY_READ));
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        int[] info = (int[]) regQueryInfoKey.invoke(root, Integer.valueOf(handles[0]));

        int count = info[0]; // count
        int maxlen = info[3]; // value length max
        for (int index = 0; index < count; index++) {
            byte[] name = (byte[]) regEnumValue.invoke(root, Integer.valueOf(handles[0]), Integer.valueOf(index), Integer.valueOf(maxlen + 1));
            String value = readString(hkey, key, new String(name, StandardCharsets.UTF_8));
            results.put(new String(name, StandardCharsets.UTF_8).trim(), value);
        }
        regCloseKey.invoke(root, Integer.valueOf(handles[0]));
        return results;
    }

    private static List<String> readStringSubKeys(Preferences root, int hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        List<String> results = new ArrayList<>();
        int[] handles = (int[]) regOpenKey.invoke(root, Integer.valueOf(hkey), toCstr(key), Integer.valueOf(KEY_READ));
        if (handles[1] != REG_SUCCESS) {
            return Collections.emptyList();
        }
        int[] info = (int[]) regQueryInfoKey.invoke(root, Integer.valueOf(handles[0]));

        int count = info[0]; // Fix: info[2] was being used here with wrong results. Suggested by davenpcj, confirmed by Petrucio
        int maxlen = info[3]; // value length max
        for (int index = 0; index < count; index++) {
            byte[] name = (byte[]) regEnumKeyEx.invoke(root, Integer.valueOf(handles[0]), Integer.valueOf(index), Integer.valueOf(maxlen + 1));
            results.add(new String(name, StandardCharsets.UTF_8).trim());
        }
        regCloseKey.invoke(root, Integer.valueOf(handles[0]));
        return results;
    }

    // utility
    private static byte[] toCstr(String str) {
        byte[] array = str.getBytes(StandardCharsets.UTF_8);
        byte[] biggerCopy = Arrays.copyOf(array, array.length + 1);
        biggerCopy[array.length] = 0;
        return biggerCopy;
    }
}
