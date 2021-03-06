/*
 * Copyright 2013 University of Washington
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package piecework.validation;

import piecework.model.Button;
import piecework.model.Field;
import piecework.util.ManyMap;

import java.util.*;

/**
 * @author James Renfro
 */
public class SubmissionTemplate {

    private final Set<String> acceptable;
    private final Set<String> buttonNames;
    private final Map<String, Button> buttonValueMap;
    private final Set<String> restricted;
    private final Set<String> userFields;
    private final Map<Field, List<ValidationRule>> fieldRuleMap;
    private final Map<String, Field> fieldMap;
    private final boolean isAttachmentAllowed;
    private final long maxAttachmentSize;

    private SubmissionTemplate() {
        this(new Builder());
    }

    private SubmissionTemplate(Builder builder) {
        this.acceptable = Collections.unmodifiableSet(builder.acceptable);
        this.buttonNames = Collections.unmodifiableSet(builder.buttonNames);
        this.buttonValueMap = Collections.unmodifiableMap(builder.buttonValueMap);
        this.fieldMap = Collections.unmodifiableMap(builder.fieldMap);
        this.restricted = Collections.unmodifiableSet(builder.restricted);
        this.userFields = Collections.unmodifiableSet(builder.userFields);
        this.isAttachmentAllowed = builder.isAttachmentAllowed;
        this.fieldRuleMap = Collections.unmodifiableMap(builder.fieldRuleMap);
        this.maxAttachmentSize = builder.maxAttachmentSize;
    }

    public Set<String> getAcceptable() {
        return acceptable;
    }

    public Button getButton(String value) {
        return buttonValueMap.get(value);
    }

    public Field getField(String name) {
        return fieldMap != null && name != null ? fieldMap.get(name) : null;
    }

    public Map<String, Field> getFieldMap() {
        return fieldMap;
    }

    public Set<String> getRestricted() {
        return restricted;
    }

    public Set<String> getUserFields() {
        return userFields;
    }

    public Set<String> getButtonNames() {
        return buttonNames;
    }

    public Map<String, Button> getButtonValueMap() {
        return buttonValueMap;
    }

    public Map<Field, List<ValidationRule>> getFieldRuleMap() {
        return fieldRuleMap;
    }

    public boolean isAcceptable(String name) {
        return acceptable.contains(name);
    }

    public boolean isDescription(String descriptionName) {
        int indexOf = descriptionName.indexOf("!description");
        if (indexOf != -1 && indexOf < descriptionName.length()) {
            String fieldName = descriptionName.substring(0, indexOf);
            return ((!acceptable.contains(descriptionName) && acceptable.contains(fieldName)) ||
                    (!restricted.contains(descriptionName) && restricted.contains(fieldName)));
        }
        return false;
    }

    public boolean isButton(String name) {
        return name != null && buttonNames.contains(name);
    }

    public boolean isAttachmentAllowed() {
        return isAttachmentAllowed;
    }

    public long getMaxAttachmentSize() {
        return maxAttachmentSize;
    }

    public boolean isRestricted(String name) {
        return restricted.contains(name);
    }

    public boolean isUserField(String name) {
        return userFields.contains(name);
    }

    public final static class Builder {
        private Set<String> acceptable;
        private Set<String> restricted;
        private Set<String> buttonNames;
        private Set<String> userFields;
        private Map<String, Button> buttonValueMap;
        private ManyMap<Field, ValidationRule> fieldRuleMap;
        private Map<String, Field> fieldMap;
        private boolean isAttachmentAllowed;
        private long maxAttachmentSize;

        public Builder() {
            this.acceptable = new HashSet<String>();
            this.buttonNames = new HashSet<String>();
            this.buttonValueMap = new HashMap<String, Button>();
            this.restricted = new HashSet<String>();
            this.userFields = new HashSet<String>();
            this.fieldRuleMap = new ManyMap<Field, ValidationRule>();
            this.fieldMap = new HashMap<String, Field>();
            this.maxAttachmentSize = 10485760l;
        }

        public SubmissionTemplate build() {
            return new SubmissionTemplate(this);
        }

        public Builder acceptable(String key) {
            this.acceptable.add(key);
            return this;
        }

        public Builder button(Button button) {
            this.buttonNames.add(button.getName());
            this.buttonValueMap.put(button.getValue(), button);
            return this;
        }

        public Builder field(Field field) {
            this.fieldMap.put(field.getName(), field);
            return this;
        }

        public Builder restricted(String key) {
            this.restricted.add(key);
            return this;
        }

        public Builder rule(Field field, ValidationRule rule) {
            if (rule != null)
                this.fieldRuleMap.putOne(field, rule);
            return this;
        }

        public Builder rules(Field field, List<ValidationRule> rules) {
            if (rules != null && !rules.isEmpty())
                this.fieldRuleMap.put(field, rules);
            return this;
        }

        public Builder userField(String name) {
            this.userFields.add(name);
            return this;
        }

        public Builder allowAttachments() {
            this.isAttachmentAllowed = true;
            return this;
        }

        public Builder maxAttachmentSize(long maxAttachmentSize) {
            this.maxAttachmentSize = maxAttachmentSize;
            return this;
        }
    }

}
