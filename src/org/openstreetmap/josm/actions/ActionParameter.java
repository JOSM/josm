// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;

public abstract class ActionParameter<T> {

    private final String name;

    public ActionParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Class<T> getType();

    public abstract String writeToString(T value);

    public abstract T readFromString(String s);

    public static class StringActionParameter extends ActionParameter<String> {

        public StringActionParameter(String name) {
            super(name);
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public String readFromString(String s) {
            return s;
        }

        @Override
        public String writeToString(String value) {
            return value;
        }
    }

    public static class SearchSettingsActionParameter extends ActionParameter<SearchSetting> {

        public SearchSettingsActionParameter(String name) {
            super(name);
        }

        @Override
        public Class<SearchSetting> getType() {
            return SearchSetting.class;
        }

        @Override
        public SearchSetting readFromString(String s) {
            return SearchSetting.readFromString(s);
        }

        @Override
        public String writeToString(SearchSetting value) {
            if (value == null)
                return "";
            return value.writeToString();
        }
    }
}
