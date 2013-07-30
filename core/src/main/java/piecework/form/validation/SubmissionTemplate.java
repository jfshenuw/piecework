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
package piecework.form.validation;

import com.google.common.collect.Sets;
import piecework.form.handler.ValueHandler;
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
    private final Map<String, ValueHandler> handlerMap;
    private final Set<String> restricted;
    private final Map<Field, List<ValidationRule>> fieldRuleMap;
    private final boolean isAttachmentAllowed;

    private SubmissionTemplate() {
        this(new Builder());
    }

    private SubmissionTemplate(Builder builder) {
        this.acceptable = Collections.unmodifiableSet(builder.acceptable);
        this.buttonNames = Collections.unmodifiableSet(builder.buttonNames);
        this.buttonValueMap = Collections.unmodifiableMap(builder.buttonValueMap);
        this.handlerMap = Collections.unmodifiableMap(builder.handlerMap);
        this.restricted = Collections.unmodifiableSet(builder.restricted);
        this.isAttachmentAllowed = builder.isAttachmentAllowed;
        this.fieldRuleMap = Collections.unmodifiableMap(builder.fieldRuleMap);
    }

    public Set<String> getAcceptable() {
        return acceptable;
    }

    public Button getButton(String value) {
        return buttonValueMap.get(value);
    }

    public Map<String, ValueHandler> getHandlerMap() {
        return handlerMap;
    }

    public Set<String> getRestricted() {
        return restricted;
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

    public ValueHandler handler(String name) {
        return handlerMap.get(name);
    }

    public boolean isAcceptable(String name) {
        return acceptable.contains(name);
    }

    public boolean isButton(String name) {
        return buttonNames.contains(name);
    }

    public boolean isAttachmentAllowed() {
        return isAttachmentAllowed;
    }

    public boolean isRestricted(String name) {
        return restricted.contains(name);
    }

    public final static class Builder {
        private Set<String> acceptable;
        private Set<String> restricted;
        private Set<String> buttonNames;
        private Map<String, Button> buttonValueMap;
        private Map<String, ValueHandler> handlerMap;
        private ManyMap<Field, ValidationRule> fieldRuleMap;
        private boolean isAttachmentAllowed;

        public Builder() {
            this.acceptable = new HashSet<String>();
            this.buttonNames = new HashSet<String>();
            this.buttonValueMap = new HashMap<String, Button>();
            this.handlerMap = new HashMap<String, ValueHandler>();
            this.restricted = new HashSet<String>();
            this.fieldRuleMap = new ManyMap<Field, ValidationRule>();
        }

        public SubmissionTemplate build() {
            return new SubmissionTemplate(this);
        }

        public Builder acceptable(String key) {
            this.acceptable.add(key);
            return this;
        }

        public Builder acceptables(Collection<String> keys) {
            this.acceptable.addAll(keys);
            return this;
        }

        public Builder button(Button button) {
            this.buttonNames.add(button.getName());
            this.buttonValueMap.put(button.getValue(), button);
            return this;
        }

        public Builder handler(String key, ValueHandler handler) {
            this.handlerMap.put(key, handler);
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

        public Builder allowAttachments() {
            this.isAttachmentAllowed = true;
            return this;
        }
    }

}
