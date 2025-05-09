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
    public static final long HKEY_CURRENT_USER = 0x80000001L;

    /**
     * Registry entries subordinate to this key define the physical state of the computer, including data about the bus type,
     * system memory, and installed hardware and software. It contains subkeys that hold current configuration data,
     * including Plug and Play information (the Enum branch, which includes a complete list of all hardware that has ever been
     * on the system), network logon preferences, network security information, software-related information (such as server
     * names and the location of the server), and other system information.
     * See <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/ms724836(v=vs.85).aspx">Predefined Keys</a>
     */
    public static final long HKEY_LOCAL_MACHINE = 0x80000002L;

    private static final long REG_SUCCESS = 0L;

    private static final int KEY_READ = 0x20019;
    private static final Preferences USER_ROOT = Preferences.userRoot();
    private static final Preferences SYSTEM_ROOT = Preferences.systemRoot();
    private static final Class<? extends Preferences> USER_CLASS = USER_ROOT.getClass();
    private static final String HKEY_EQ = "hkey=";

    /**
     * Wrapper for Windows registry API RegOpenKey(long, byte[], int)
     * Returns {@code long[]}
     */
    private static final Method REG_OPEN_KEY;
    /**
     * Wrapper for Windows registry API RegCloseKey(long)
     * Returns {@code int}
     */
    private static final Method REG_CLOSE_KEY;
    /**
     * Wrapper for Windows registry API RegQueryValueEx(long, byte[])
     * Returns {@code byte[]}
     */
    private static final Method REG_QUERY_VALUE_EX;
    /**
     * Wrapper for Windows registry API RegEnumValue(long, int, int)
     * Returns {@code byte[]}
     */
    private static final Method REG_ENUM_VALUE;
    /**
     * Wrapper for RegQueryInfoKey(long)
     * Returns {@code long[]}
     */
    private static final Method REG_QUERY_INFO_KEY;
    /**
     * Wrapper for RegEnumKeyEx(long, int, int)
     * Returns {@code byte[]}
     */
    private static final Method REG_ENUM_KEY_EX;

    static {
        REG_OPEN_KEY = getDeclaredMethod("WindowsRegOpenKey", long.class, byte[].class, int.class);
        REG_CLOSE_KEY = getDeclaredMethod("WindowsRegCloseKey", long.class);
        REG_QUERY_VALUE_EX = getDeclaredMethod("WindowsRegQueryValueEx", long.class, byte[].class);
        REG_ENUM_VALUE = getDeclaredMethod("WindowsRegEnumValue", long.class, int.class, int.class);
        REG_QUERY_INFO_KEY = getDeclaredMethod("WindowsRegQueryInfoKey1", long.class);
        REG_ENUM_KEY_EX = getDeclaredMethod("WindowsRegEnumKeyEx", long.class, int.class, int.class);
        ReflectionUtils.setObjectsAccessible(REG_OPEN_KEY, REG_CLOSE_KEY, REG_QUERY_VALUE_EX, REG_ENUM_VALUE,
                REG_QUERY_INFO_KEY, REG_ENUM_KEY_EX);
    }

    private static Method getDeclaredMethod(String name, Class<?>... parameterTypes) {
        try {
            return USER_CLASS.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to find WindowsReg method", e);
            return null;
        } catch (RuntimeException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get WindowsReg method", e);
            return null;
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
     * @since 19100 (method definition)
     */
    public static String readString(long hkey, String key, String valueName)
            throws IllegalAccessException, InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readString(SYSTEM_ROOT, hkey, key, valueName);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readString(USER_ROOT, hkey, key, valueName);
        } else {
            throw new IllegalArgumentException(HKEY_EQ + hkey);
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
    public static Map<String, String> readStringValues(long hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringValues(SYSTEM_ROOT, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringValues(USER_ROOT, hkey, key);
        } else {
            throw new IllegalArgumentException(HKEY_EQ + hkey);
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
    public static List<String> readStringSubKeys(long hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringSubKeys(SYSTEM_ROOT, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringSubKeys(USER_ROOT, hkey, key);
        } else {
            throw new IllegalArgumentException(HKEY_EQ + hkey);
        }
    }

    // =====================

    private static long getNumber(Object array, int index) {
        if (array instanceof long[]) {
            return ((long[]) array)[index];
        }
        throw new IllegalArgumentException();
    }

    private static String readString(Preferences root, long hkey, String key, String value)
            throws IllegalAccessException, InvocationTargetException {
        if (REG_OPEN_KEY == null || REG_QUERY_VALUE_EX == null || REG_CLOSE_KEY == null) {
            return null;
        }
        Object handles = REG_OPEN_KEY.invoke(root, hkey, toCstr(key), KEY_READ);
        if (getNumber(handles, 1) != REG_SUCCESS) {
            return null;
        }
        byte[] valb = (byte[]) REG_QUERY_VALUE_EX.invoke(root, getNumber(handles, 0), toCstr(value));
        REG_CLOSE_KEY.invoke(root, getNumber(handles, 0));
        return (valb != null ? new String(valb, StandardCharsets.UTF_8).trim() : null);
    }

    private static Map<String, String> readStringValues(Preferences root, long hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        if (REG_OPEN_KEY == null || REG_QUERY_INFO_KEY == null || REG_ENUM_VALUE == null || REG_CLOSE_KEY == null) {
            return Collections.emptyMap();
        }
        HashMap<String, String> results = new HashMap<>();
        Object handles = REG_OPEN_KEY.invoke(root, hkey, toCstr(key), KEY_READ);
        if (getNumber(handles, 1) != REG_SUCCESS) {
            return Collections.emptyMap();
        }
        Object info = REG_QUERY_INFO_KEY.invoke(root, getNumber(handles, 0));

        int count = Math.toIntExact(getNumber(info, 0));
        int maxlen = Math.toIntExact(getNumber(info, 3));
        for (int index = 0; index < count; index++) {
            byte[] name = (byte[]) REG_ENUM_VALUE.invoke(root, getNumber(handles, 0), index, maxlen + 1);
            String value = readString(hkey, key, new String(name, StandardCharsets.UTF_8));
            results.put(new String(name, StandardCharsets.UTF_8).trim(), value);
        }
        REG_CLOSE_KEY.invoke(root, getNumber(handles, 0));
        return Collections.unmodifiableMap(results);
    }

    private static List<String> readStringSubKeys(Preferences root, long hkey, String key)
            throws IllegalAccessException, InvocationTargetException {
        if (REG_OPEN_KEY == null || REG_QUERY_INFO_KEY == null || REG_ENUM_KEY_EX == null || REG_CLOSE_KEY == null) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>();
        Object handles = REG_OPEN_KEY.invoke(root, hkey, toCstr(key), KEY_READ);
        if (getNumber(handles, 1) != REG_SUCCESS) {
            return Collections.emptyList();
        }
        Object info = REG_QUERY_INFO_KEY.invoke(root, getNumber(handles, 0));

        int count = Math.toIntExact(getNumber(info, 0));
        int maxlen = Math.toIntExact(getNumber(info, 3));
        for (int index = 0; index < count; index++) {
            byte[] name = (byte[]) REG_ENUM_KEY_EX.invoke(root, getNumber(handles, 0), index, maxlen + 1);
            results.add(new String(name, StandardCharsets.UTF_8).trim());
        }
        REG_CLOSE_KEY.invoke(root, getNumber(handles, 0));
        return Collections.unmodifiableList(results);
    }

    // utility
    private static byte[] toCstr(String str) {
        byte[] array = str.getBytes(StandardCharsets.UTF_8);
        byte[] biggerCopy = Arrays.copyOf(array, array.length + 1);
        biggerCopy[array.length] = 0;
        return biggerCopy;
    }
}
