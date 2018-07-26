// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import org.openstreetmap.josm.gui.NavigatableComponent;

import mockit.Mock;
import mockit.MockUp;

/**
 * MockUp for allowing a {@link NavigatableComponent} to be used in either headless or windowless
 * tests.
 */
public class WindowlessNavigatableComponentMocker extends MockUp<NavigatableComponent> {
    @Mock
    private boolean isVisibleOnScreen() {
        return true;
    }
}
