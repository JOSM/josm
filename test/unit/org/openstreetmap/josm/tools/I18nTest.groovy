// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools

class I18nTest extends GroovyTestCase {
    void testEscape() {
        def foobar = "{foo'bar}"
        assert I18n.escape(foobar) == "'{'foo''bar'}'"
        assert I18n.tr(I18n.escape(foobar)) == foobar
    }
}
