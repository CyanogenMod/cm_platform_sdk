/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;

/**
 * A simple concept of a "setting" within the provider
 */
public class Setting implements Serializable, Comparable<Setting> {
    private static final long serialVersionUID = 0;

    private String key;
    private String value;
    private String keyType;
    private String valueType;

    public Setting() {
        this.keyType = SettingType.TYPE_NULL;
        this.valueType = SettingType.TYPE_NULL;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String type) {
        this.keyType = type;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    /** s - string, i - integer, f - float */
    public static class SettingType {
        private static final String TYPE_NULL = "NULL";
        private static final String TYPE_STRING = "s";
        private static final String TYPE_INTEGER = "i";
        private static final String TYPE_FLOAT = "f";
        private static final String TYPE_BLOB = "d";

        //THIS IS FROM CURSOR.JAVA, DO NOT MODIFY
        /** Value returned by {@link #getType(int)} if the specified column is null */
        static final int FIELD_TYPE_NULL = 0;
        /** Value returned by {@link #getType(int)} if the specified  column type is integer */
        static final int FIELD_TYPE_INTEGER = 1;
        /** Value returned by {@link #getType(int)} if the specified column type is float */
        static final int FIELD_TYPE_FLOAT = 2;
        /** Value returned by {@link #getType(int)} if the specified column type is string */
        static final int FIELD_TYPE_STRING = 3;
        /** Value returned by {@link #getType(int)} if the specified column type is blob */
        static final int FIELD_TYPE_BLOB = 4;


        public static String mapNumericToType(int numeric) {
            switch (numeric) {
                case FIELD_TYPE_NULL:
                    return TYPE_NULL;
                case FIELD_TYPE_STRING:
                    return TYPE_STRING;
                case FIELD_TYPE_INTEGER:
                    return TYPE_INTEGER;
                case FIELD_TYPE_FLOAT:
                    return TYPE_FLOAT;
                case FIELD_TYPE_BLOB:
                    return TYPE_BLOB;
                default:
                    return TYPE_NULL;
            }
        }
    }

    @Override
    public int compareTo(Setting o) {
        return this.key.compareTo(o.getKey());
    }
}
