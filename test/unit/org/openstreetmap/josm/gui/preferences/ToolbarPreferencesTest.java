// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.actions.ActionParameter;
import org.openstreetmap.josm.actions.ActionParameter.StringActionParameter;
import org.openstreetmap.josm.actions.ParameterizedAction;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences.ActionDefinition;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences.ActionParser;

public class ToolbarPreferencesTest {

    private static class TestAction extends AbstractAction implements ParameterizedAction {

        public TestAction() {
            putValue("toolbar", "action");
        }

        public void actionPerformed(ActionEvent e, Map<String, Object> parameters) {
        }

        public List<ActionParameter<?>> getActionParameters() {
            List<ActionParameter<?>> result = new ArrayList<>();
            result.add(new StringActionParameter("param1"));
            result.add(new StringActionParameter("param2"));
            return result;
        }

        public void actionPerformed(ActionEvent e) {

        }

    }

    private void checkAction(ActionDefinition a, Object... params) {
        Map<String, Object> expected = new HashMap<>();
        for (int i=0; i<params.length; i+=2) {
            expected.put((String)params[i], params[i+1]);
        }
        Assert.assertEquals(expected, a.getParameters());
    }

    @Test
    public void test1() {
        Map<String, Action> actions = new HashMap<>();
        actions.put("action", new TestAction());
        ActionParser parser = new ActionParser(actions);
        checkAction(parser.loadAction("action(param1=value1)"), "param1", "value1");
        checkAction(parser.loadAction("action(param1=value1,param2=2)"), "param1", "value1", "param2", "2");
        checkAction(parser.loadAction("action(param1=value1,param2=2\\(\\=\\,\\\\)"), "param1", "value1", "param2", "2(=,\\");
        checkAction(parser.loadAction("action(param1=value1,"), "param1", "value1");
        checkAction(parser.loadAction("action(param1=value1"), "param1", "value1");
        checkAction(parser.loadAction("action(param1="));
        checkAction(parser.loadAction("action(param1"));
        checkAction(parser.loadAction("action("));
        checkAction(parser.loadAction("action"));
        checkAction(parser.loadAction("action(uknownParam=aa)"));

        Assert.assertEquals("action(param1=value1,param2=value2)", parser.saveAction(parser.loadAction("action(param1=value1,param2=value2)")));
        Assert.assertEquals("action(param1=value1,param2=)", parser.saveAction(parser.loadAction("action(param1=value1)")));
        Assert.assertEquals("action(param1=value1,param2=2\\(\\=\\,\\\\)", parser.saveAction(parser.loadAction("action(param1=value1,param2=2\\(\\=\\,\\\\)")));
    }

}
