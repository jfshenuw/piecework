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
package piecework.ui;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import piecework.Constants;
import piecework.enumeration.FieldTag;
import piecework.model.*;
import piecework.util.ManyMap;

import java.io.IOException;
import java.util.*;

/**
 * Decorates HTML based on
 *
 * @author James Renfro
 */
public class DecoratingVisitor implements TagNodeVisitor {

    private static final Logger LOG = Logger.getLogger(DecoratingVisitor.class);
    private static final String NEWLINE = System.getProperty("line.separator");

    private final Form form;
    private final ManyMap<String, TagDecorator> decoratorMap;

    public DecoratingVisitor(Form form) {
        this.form = form;
        this.decoratorMap = new ManyMap<String, TagDecorator>();
        initialize();
    }

    public void initialize() {
        decoratorMap.putOne("form", new FormDecorator(form));
        decoratorMap.putOne("body", new BodyDecorator(form));
        decoratorMap.putOne("div", new AttachmentsDecorator(form));

        VariableDecorator variableDecorator = new VariableDecorator(form);
        decoratorMap.putOne("span", variableDecorator);
        decoratorMap.putOne("input", variableDecorator);

        Screen screen = form.getScreen();
        Map<String, List<Value>> data = variableDecorator.getData();
        Map<String, List<Message>> results = variableDecorator.getResults();
        if (screen != null) {
            boolean readonly = screen.isReadonly();
            if (readonly)
                decoratorMap.putOne("button", new ButtonDecorator(readonly));
            List<Section> sections = screen.getSections();
            if (sections != null && !sections.isEmpty()) {
                for (Section section : sections) {
                    if (section.getTagId() != null)
                        decoratorMap.putOne("div", new SectionDecorator(section));

                    List<Field> fields = section.getFields();
                    if (fields == null || fields.isEmpty())
                        continue;

                    for (Field field : fields) {
                        List<Value> values = data.get(field.getName());
                        List<Message> messages = results.get(field.getName());
                        FieldDecorator fieldDecorator = new FieldDecorator(field, values, messages, readonly);
                        FieldTag fieldTag = fieldDecorator.getFieldTag();
                        decoratorMap.putOne(fieldTag.getTagName(), fieldDecorator);

                        switch (fieldTag) {
                            case FILE:
                                decoratorMap.putOne("form", new FileFieldFormDecorator(field, readonly));

                                if (StringUtils.isNotEmpty(field.getAccept()) && field.getAccept().contains("image/"))
                                    decoratorMap.putOne("img", new ImageFieldDecorator(field, values));

                                break;
                        }
                    }
                }
            }
        }
    }

    public boolean visit(TagNode tagNode, HtmlNode htmlNode) {
        if (htmlNode instanceof TagNode) {
            TagNode tag = (TagNode) htmlNode;
            String tagName = tag.getName();
            List<TagDecorator> decorators = decoratorMap.get(tagName);

            if (decorators == null || decorators.isEmpty())
                return true;

            String id = tag.getAttributeByName("id");
            String cls = tag.getAttributeByName("class");
            String name = tag.getAttributeByName("name");
            String variable = tag.getAttributeByName("data-process-variable");

            for (TagDecorator decorator : decorators) {
                if (decorator != null && decorator.canDecorate(tag, id, cls, name, variable)) {
                    decorator.decorate(tag, id , cls, name, variable);
                }
            }
        }
        return true;
    }

    class BodyDecorator implements TagDecorator {

        private final Form form;

        public BodyDecorator(Form form) {
            this.form = form;
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {
            Screen screen = form.getScreen();
            String screenType = screen.getType();

            if (screenType != null) {
                if (screenType.equals(Constants.ScreenTypes.WIZARD) || screenType.equals(Constants.ScreenTypes.WIZARD_TEMPLATE) || screenType.equals(Constants.ScreenTypes.STAGED)) {
                    TagNode contextScriptTag = new TagNode("script");

                    ObjectMapper mapper = new ObjectMapper();

                    try {
                        StringBuilder content = new StringBuilder(NEWLINE);
                        content.append("\t\tpiecework = {};").append(NEWLINE)
                               .append("\t\tpiecework.context = {};").append(NEWLINE)
                               .append("\t\tpiecework.context.resource = ").append(mapper.writer().writeValueAsString(form))
                               .append(";").append(NEWLINE);

                        contextScriptTag.addAttribute("type", "text/javascript");
                        contextScriptTag.addChild(new ContentNode(content.toString()));
                        tag.addChild(contextScriptTag);
                    } catch (JsonMappingException e) {
                        LOG.error("Unable to add script tag with form resource", e);
                    } catch (JsonGenerationException e) {
                        LOG.error("Unable to add script tag with form resource", e);
                    } catch (IOException e) {
                        LOG.error("Unable to add script tag with form resource", e);
                    }

                    TagNode requirejsScriptTag = new TagNode("script");
                    requirejsScriptTag.addAttribute("type", "text/javascript");
                    requirejsScriptTag.addAttribute("data-main", "../static/js/form.js");
                    requirejsScriptTag.addAttribute("src", "../static/js/vendor/require.js");
                }
            }
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            Screen screen = form.getScreen();
            String screenType = screen.getType();

            return screenType != null && screenType.equals(Constants.ScreenTypes.WIZARD) || screenType.equals(Constants.ScreenTypes.WIZARD_TEMPLATE);
        }

        public boolean isReusable() {
            return false;
        }
    }


    class AttachmentsDecorator implements TagDecorator {

        private final Form form;

        public AttachmentsDecorator(Form form) {
            this.form = form;
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {
            String contentType = tag.getAttributeByName("data-process-content-type");
            String[] contentTypes = contentType != null ? contentType.split("\\s*,\\s*") : new String[0];
            Set<String> contentTypeSet = Sets.newHashSet(contentTypes);

            if (form != null && form.getAttachments() != null && !form.getAttachments().isEmpty()) {
                tag.removeAllChildren();

                TagNode ulNode = new TagNode("ul");
                ulNode.addAttribute("class", "attachments");

                for (Attachment attachment : form.getAttachments()) {
                    if (contentTypeSet.isEmpty() || (attachment.getContentType() != null && contentTypeSet.contains(attachment.getContentType()))) {
                        TagNode liNode = new TagNode("li");
                        TagNode anchorNode = new TagNode("a");
                        anchorNode.addAttribute("href", attachment.getLink());
                        anchorNode.addChild(new ContentNode(attachment.getName()));
                        ulNode.addChild(liNode);
                    }
                }
            }
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            String container = tag.getAttributeByName("data-process-container");
            return container != null && container.equalsIgnoreCase("attachments");
        }

        public boolean isReusable() {
            return false;
        }
    }

    class ButtonDecorator implements TagDecorator {

        private final boolean readonly;

        public ButtonDecorator(boolean readonly) {
            this.readonly = readonly;
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {
            Map<String, String> attributes = new HashMap<String, String>();
            if (readonly)
                attributes.put("disabled", "disabled");
            attributes.putAll(tag.getAttributes());
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            return name != null && name.equalsIgnoreCase("button");
        }

        public boolean isReusable() {
            return false;
        }
    }

    class FormDecorator implements TagDecorator {

        private final Form form;
        private final Map<String, List<Value>> data;

        public FormDecorator(Form form) {
            this.form = form;
            this.data = form.getData();
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {
            String type = tag.getAttributeByName("data-process-form");
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.putAll(tag.getAttributes());

            if (type == null || type.equals("main")) {
                String formUri = form.getAction() != null ? form.getAction() : "";
                attributes.put("action", formUri);
                attributes.put("method", "POST");
                attributes.put("enctype", "multipart/form-data");
            } else if (type.equals("attachments")) {
                String attachmentUri = form.getAttachment() != null ? form.getAttachment() : "";
                attributes.put("action", attachmentUri);
                attributes.put("method", "POST");
            } else if (type.equals("cancellation")) {
                String attachmentUri = form.getCancellation() != null ? form.getCancellation() : "";
                attributes.put("action", attachmentUri);
                attributes.put("method", "POST");
            }
            tag.setAttributes(attributes);
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            String exclude = tag.getAttributeByName("data-process-exclude");
            return exclude == null || !exclude.equals("true");
        }

        public boolean isReusable() {
            return false;
        }
    }

    class SectionDecorator implements TagDecorator {

        private final Section section;

        public SectionDecorator(Section section) {
            this.section = section;
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {

        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            return id == null || section.getTagId() != null && section.getTagId().equals(id);
        }

        public boolean isReusable() {
            return true;
        }

    }

    class VariableDecorator implements TagDecorator {

        private final Map<String, List<Value>> data;
        private final Map<String, List<Message>> results;

        public VariableDecorator(Form form) {
            this.data = form.getData();
            this.results = form.getValidation();
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {

            String fieldName = variable;
            String attributeName = null;

            int indexOf = variable.indexOf('.');
            if (indexOf != -1) {
                fieldName = variable.substring(0, indexOf);

                if (variable.length() > indexOf)
                    attributeName = variable.substring(indexOf + 1);
            }


            List<Value> values = data.get(fieldName);

            String tagName = tag.getName() != null ? tag.getName() : "";

            if (tagName.equalsIgnoreCase("input")) {
                // Only modify the value if the input tag is disabled and it's a text input
                String type = tag.getAttributeByName("type");
                String disabled = tag.getAttributeByName("disabled");

                if (type != null && disabled != null && type.equals("text")) {
                    if (values != null) {
                        for (Value value : values) {
                            tag.addAttribute("value", value.getValue());
                            break;
                        }
                    }
                }
            } else {
                tag.removeAllChildren();
                if (values != null) {
                    for (Value value : values) {
                        if (value instanceof User) {
                            User user = User.class.cast(value);

                            if (attributeName == null || attributeName.equals("displayName"))
                                tag.addChild(new ContentNode(user.getDisplayName()));
                            else if (attributeName.equals("visibleId"))
                                tag.addChild(new ContentNode(user.getVisibleId()));

                        } else {
                            tag.addChild(new ContentNode(value.getValue()));
                        }
                    }
                }
            }
        }

        Map<String, List<Value>> getData() {
            return data;
        }

        Map<String, List<Message>> getResults() {
            return results;
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            if (StringUtils.isNotEmpty(variable))
                return true;

            return false;
        }

        public boolean isReusable() {
            return true;
        }
    }

    class FieldDecorator implements TagDecorator {

        private final Field field;
        private final FieldTag fieldTag;
        private final List<Value> values;
        private final List<Message> messages;
        private final boolean readonly;
        private int index;

        public FieldDecorator(Field field, List<Value> values, List<Message> messages, boolean readonly) {
            this.field = field;
            this.fieldTag = FieldTag.getInstance(field.getType());
            this.messages = messages;
            this.values = values;
            this.readonly = readonly;
            this.index = 0;
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.putAll(tag.getAttributes());
            attributes.putAll(fieldTag.getAttributes());

            if (StringUtils.isNotEmpty(field.getAccept()))
                attributes.put("accept", field.getAccept());

            tag.setAttributes(attributes);

            Value value = null;

            if (values != null) {
                if (values.size() > index) {
                    value = values.get(index);
                }

                if (messages != null && !messages.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    Iterator<Message> iterator = messages.iterator();
                    while (iterator.hasNext()) {
                        Message message = iterator.next();
                        builder.append(message.getText());
                        if (iterator.hasNext())
                            builder.append(",");
                    }
                    attributes.put("data-process-messages", builder.toString());
                }
            }

            if (!field.isVisible())
                attributes.put("class", cls + " hide");

            switch (fieldTag) {
                case EMAIL:
                case NUMBER:
                case TEXT:
                    if (field.getMaxValueLength() > 0)
                        attributes.put("maxlength", "" + field.getMaxValueLength());
                    if (field.getDisplayValueLength() > 0)
                        attributes.put("size", "" + field.getDisplayValueLength());
                case CHECKBOX:
                case RADIO:
                    if (value != null) {
                        if (value instanceof User) {
                            User user = User.class.cast(value);
                            attributes.put("value", user.getDisplayName());
                        } else {
                            attributes.put("value", value.getValue());
                        }
                    }
                    break;
                case TEXTAREA:
                    tag.removeAllChildren();
                    if (value != null)
                        tag.addChild(new ContentNode(value.getValue()));

                    break;
                case SELECT_MULTIPLE:
                case SELECT_ONE:
                    List<Option> options = field.getOptions();
                    if (options != null && !options.isEmpty()) {
                        tag.removeAllChildren();
                        for (Option option : options) {
                            TagNode optionNode = new TagNode("option");
                            Map<String, String> optionAttributes = new HashMap<String, String>();
                            optionAttributes.put("value", option.getValue());
                            if (option.getValue() != null && value != null && option.getValue().equals(value.getValue()))
                                optionAttributes.put("selected", "selected");
                            optionNode.setAttributes(optionAttributes);
                            optionNode.addChild(new ContentNode(option.getLabel()));
                            tag.addChild(optionNode);
                        }
                    }
                    break;
            }

            if (readonly)
                attributes.put("disabled", "disabled");

            this.index++;
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {

            if (id != null && id.equals(field.getFieldId()))
                return true;

            if (name != null && name.equals(field.getName()))
                return true;

            return false;
        }

        public FieldTag getFieldTag() {
            return fieldTag;
        }

        public boolean isReusable() {
            return true;
        }

    }

    class ImageFieldDecorator implements TagDecorator {

        private final Field field;
        private final List<Value> values;

        public ImageFieldDecorator(Field field, List<Value> values) {
            this.field = field;
            this.values = values;
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {
            if (values != null) {
                for (Value value : values) {
                    if (value instanceof File) {
                        File file = File.class.cast(value);
                        Map<String, String> attributes = new HashMap<String, String>();
                        attributes.putAll(tag.getAttributes());
                        attributes.put("src", file.getLink());
                        tag.setAttributes(attributes);
                        break;
                    }
                }
            }
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            return variable != null && field.getName() != null && variable.equals(field.getName());
        }

        public boolean isReusable() {
            return false;
        }
    }

    class FileFieldFormDecorator implements TagDecorator {

        private final Field field;
        private final boolean readonly;

        public FileFieldFormDecorator(Field field, boolean readonly) {
            this.field = field;
            this.readonly = readonly;
        }

        @Override
        public void decorate(TagNode tag, String id, String cls, String name, String variable) {
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.putAll(tag.getAttributes());
            if (!readonly) {
                attributes.put("action", field.getLink());
                attributes.put("method", "POST");
                attributes.put("enctype", "multipart/form-data");
            }
            tag.setAttributes(attributes);
        }

        @Override
        public boolean canDecorate(TagNode tag, String id, String cls, String name, String variable) {
            String exclude = tag.getAttributeByName("data-process-exclude");
            if (exclude == null || !exclude.equals("true")) {
                String type = tag.getAttributeByName("data-process-form");
                return type != null && type.equals(field.getName());
            }
            return false;
        }

        public boolean isReusable() {
            return false;
        }

    }

}
