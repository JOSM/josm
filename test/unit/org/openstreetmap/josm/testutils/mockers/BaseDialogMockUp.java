// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.MockUp;

/**
 * Abstract class implementing the few common features of the dialog-mockers which are readily factorable.
 * @param <T> type
 */
abstract class BaseDialogMockUp<T> extends MockUp<T> {
    private final List<Object[]> invocationLog;

    /**
     * @return an unmodifiable view of the internal invocation log. Each entry is an array of Objects to
     *     allow for more advanced implementations to be able to express their invocations in their own
     *     ways. Typically the invocation's "result value" is used as the first element of the array.
     */
    public List<Object[]> getInvocationLog() {
        return this.invocationLog;
    }

    private final List<Object[]> invocationLogInternal = new ArrayList<>(4);

    /**
     * @return the actual (writable) invocation log
     */
    protected List<Object[]> getInvocationLogInternal() {
        return this.invocationLogInternal;
    }

    private final Map<String, Object> mockResultMap;

    /**
     * @return mapping to {@link Object}s so response button can be specified by String (label) or Integer
     *     - sorry, no type safety as java doesn't support union types
     */
    public Map<String, Object> getMockResultMap() {
        return this.mockResultMap;
    }

    BaseDialogMockUp(final Map<String, Object> mockResultMap) {
        this.mockResultMap = mockResultMap != null ? mockResultMap : new HashMap<>(4);
        this.invocationLog = Collections.unmodifiableList(this.invocationLogInternal);
    }
}
