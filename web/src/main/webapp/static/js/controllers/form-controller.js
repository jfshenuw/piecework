define([
  'controllers/base/controller',
  'models/base/model',
  'models/base/collection',
  'models/runtime/form',
  'models/runtime/page',
  'views/form/button-view',
  'views/form/button-link-view',
  'views/form/fields-view',
  'views/form/form-view',
  'views/form/grouping-view',
  'views/runtime/head-view',
  'views/form/section-view',
  'views/form/sections-view',
  'views/base/view',
  'text!templates/form/button.hbs',
  'text!templates/form/button-link.hbs'
], function(Controller, Model, Collection, Form, Page, ButtonView, ButtonLinkView, FieldsView, FormView,
            GroupingView, HeadView, SectionView, SectionsView, View) {
  'use strict';

  var FormController = Controller.extend({
    index: function(params) {
        this.compose('formModel', Form, window.piecework.context.resource);
        this.compose('pageModel', Page, window.piecework.context);

        var formModel = this.compose('formModel');
        var pageModel = this.compose('pageModel');
        //        this.compose('headView', HeadView, {model: pageModel});

        var screenModel = formModel.get("screen");
        var screenType = screenModel.type;
        var sections = screenModel.sections;

        var groupingIndex = 0;
        var currentScreen = params.ordinal;
        if (currentScreen != undefined)
            groupingIndex = parseInt(currentScreen, 10) - 1;
        var groupings = screenModel.groupings;
        var grouping = groupings != undefined && groupings.length > groupingIndex ? groupings[groupingIndex] : { sectionIds : []};
        var includeAll = grouping.length < 1;

        var sectionVisibleMap = {};
        var lastVisibleSection;
        for (var i=0;i<grouping.sectionIds.length;i++) {
            sectionVisibleMap[grouping.sectionIds[i]] = true;
            lastVisibleSection = grouping.sectionIds[i];
        }

        this.compose('formView', {
            compose: function(options) {
                this.model = formModel;
                this.view = FormView;
                var autoRender, disabledAutoRender;
                this.item = new FormView({model: this.model});
                autoRender = this.item.autoRender;
                disabledAutoRender = autoRender === void 0 || !autoRender;
                if (disabledAutoRender && typeof this.item.render === "function") {
                    return this.item.render();
                }
            },
            check: function(options) {
                return true;
            },
        });

        $('.section').removeClass('selected');
        $('.section').addClass('hide');

        if (grouping != null) {
            var groupingId = grouping.groupingId;

            this.compose(groupingId, GroupingView, {model: new Model(grouping)});
        }

        var formDataMap = {};
        var formData = formModel.get("formData");

        if (formData != undefined) {
            for (var i=0;i<formData.length;i++) {
                var formValue = formData[i];
                formDataMap[formValue.name] = formValue;
            }
        }

        if (sections != null) {
            for (var i=0;i<sections.length;i++) {
                var section = sections[i];

                if (section == null)
                    continue;

                var sectionId = section.sectionId;
                var sectionType = section.type;
                var tagId = section.tagId;
                var fields = section.fields;
                var sectionViewId = tagId != null ? tagId : sectionId;
                var contentSelector = '#' + sectionViewId + " > .section-content";
                var buttonSelector = '#' + sectionViewId + " > .section-footer > .section-buttons";
                var showButtons = (i+1) == sections.length;
                var className;

                var doShow = sectionVisibleMap[sectionId] != undefined;

                if (doShow)
                    className = 'section selected';
                else
                    className = 'section hide';

                if (fields != undefined) {
                    for (var j=0;j<fields.length;j++) {
                        var field = fields[j];
                        var formValue = formDataMap[field.name];

                        if (formValue != undefined) {
//                            var values = formValue.values;
//                            for (var j=0;j<values.length;j++) {
//                                var value = values[j];
//                                field.value = value;
//                                break;
//                            }
                            field.messages = formValue.messages;
                        }
                    }
                }

                this.compose(sectionViewId, {
                    compose: function(options) {
                        this.model = new Model(section);
                        this.view = SectionView;
                        var autoRender, disabledAutoRender;
                        this.item = new SectionView({id: sectionViewId, className: className, container: 'ul.sections', model: this.model});
                        autoRender = this.item.autoRender;
                        disabledAutoRender = autoRender === void 0 || !autoRender;
                        if (disabledAutoRender && typeof this.item.render === "function") {
                            return this.item.render();
                        }
                    },
                    check: function(options) {
                        if (options.show) {
                            this.item.$el.addClass('selected');
                            this.item.$el.removeClass('hide');
                        } else {
                            this.item.$el.removeClass('selected');
                            this.item.$el.addClass('hide');
                        }
                        return true;
                    },
                    options: {
                        show: doShow
                    }
                });

                this.compose('fieldsView_' + sectionId, {
//                    options: { collection: new Collection(fields), container: contentSelector },
                    compose: function(options) {
                       this.collection = new Collection(fields);
                       var autoRender, disabledAutoRender;
                       this.item = new FieldsView({collection: this.collection, container: contentSelector });
                       autoRender = this.item.autoRender;
                       disabledAutoRender = autoRender === void 0 || !autoRender;
                       if (disabledAutoRender && typeof this.item.render === "function") {
                           return this.item.render();
                       }
                    },
                    check: function(options) {
                        return true;
                    }
                });

                if (sectionId == lastVisibleSection) {
                    if (section.buttons != undefined && section.buttons.length > 0) {
                        var buttons = section.buttons;
                        for (var b=0;b<buttons.length;b++) {
                            var button = buttons[b];
                            var buttonId = button.buttonId;

                            if (button.value != undefined) {
                                if (button.value == 'next') {
                                    button.value = 'step/' + (section.ordinal + 1);
                                    button.link = '#' + button.value;
                                } else if (buttons[b].value == 'prev') {
                                    button.value = 'step/' + (section.ordinal - 1);
                                    button.link = '#' + button.value;
                                }
                            }

                            if (button.type == 'button-link')
                                this.compose('buttons_' + buttonId, ButtonLinkView, {model: new Model(button), container: buttonSelector});
                            else
                                this.compose('buttons_' + buttonId, ButtonView, {model: new Model(button), container: buttonSelector});
                        }
                    } else {
                        this.compose('buttons_submitButton', ButtonView, {model: new Model({label: 'Submit'}), container: buttonSelector});
                    }
                }
            }

            var $requiredFields = $('.section.selected').find(':input.required');
            var $hiddenFields = $('.section').find(':input').not(':visible');
            $requiredFields.attr('required', true);
            $requiredFields.attr('aria-required', true);

            // Hidden fields are not required
            $hiddenFields.removeAttr('required');
            $hiddenFields.removeAttr('aria-required');
        }
    },
  });

  return FormController;
});