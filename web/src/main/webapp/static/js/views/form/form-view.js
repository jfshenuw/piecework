define([ 'chaplin',
        'models/attachments', 'models/buttons', 'models/base/collection', 'models/base/model', 'models/notification', 'models/design/sections',
        'views/form/attachments-view', 'views/form/buttons-view', 'views/base/collection-view', 'views/form/fields-view', 'views/form/form-toolbar-view',
        'views/form/grouping-view', 'views/form/notification-view', 'views/form/section-view', 'views/form/sections-view',
        'views/base/view', 'text!templates/form/form.hbs' ],
		function(Chaplin, Attachments, Buttons, Collection, Model, Notification, Sections, AttachmentsView, ButtonsView, CollectionView, FieldsView,
		         FormToolbarView, GroupingView, NotificationView, SectionView, SectionsView, View, template) {
	'use strict';

	var FormView = View.extend({
		autoRender : false,
		container: '.main-content',
		id: 'main-form',
		tagName: 'form',
	    template: template,
	    events: {
	        'submit': '_onFormSubmit',
	        'load': '_onLoaded',
	    },
	    listen: {
             'addedToDOM': '_onAddedToDOM',
             'groupingIndex:change mediator': '_onGroupingIndexChange',
             'showAttachments mediator': '_onShowAttachments',
	    },
	    initialize: function(model, options) {
	        View.__super__.initialize.apply(this, options);
            this.params = options.params;
            this.subview('formToolbarView', new FormToolbarView({model: this.model}));

            var screen = this.model.get("screen");
            if (screen != undefined) {
                var link = this.model.get("link");
                var formInstanceId = this.model.get("formInstanceId");
                var re = new RegExp(formInstanceId + "$");

                if (! re.test(link))
                    link += '/' + formInstanceId;

                var groupings = screen.groupings;

                for (var i=0;i<groupings.length;i++) {
                    groupings[i].breadcrumbLink = link + '/step/' + groupings[i].ordinal;
                }

            }
	        return this;
	    },
        render: function() {
            View.__super__.render.apply(this);

            var action = this.model.get("action");

            if (action != undefined) {
                this.$el.attr('action', action + '.html');
                this.$el.attr('method', 'POST');
                this.$el.attr('enctype', 'multipart/form-data');
            }

            this.$el.attr('novalidate', 'novalidate');

            var screen = this.model.get("screen");

            if (screen == undefined)
                return this;

            var pageLink = this.model.get("link");
            var groupings = screen.groupings;
            var groupingIndex = this.model.get("groupingIndex");
            if (groupingIndex == undefined)
                groupingIndex = 0;
            var grouping = groupings != undefined && groupings.length > groupingIndex ? groupings[groupingIndex] : { sectionIds : []};

            var sectionList = screen.sections;
            if (sectionList != undefined && sectionList.length > 0) {
                var sectionMap = {};
                if (grouping != undefined) {
                    for (var i=0;i<grouping.sectionIds.length;i++) {
                        var sectionId = grouping.sectionIds[i];
                        if (sectionId == undefined)
                            continue;
                        sectionMap[sectionId] = true;
                    }
                }
                for (var i=0;i<sectionList.length;i++) {
                    var section = sectionList[i];
                    var sectionId = section.sectionId;
                    var isSelected = sectionMap[sectionId];
                    section.selected = isSelected != undefined ? isSelected : false;
                }
            }

            var sectionsView = this.subview('sectionsView');
            if (!sectionsView) {
                var sections = new Sections(sectionList);
                sectionsView = new SectionsView({autoRender: false, collection: sections});
                this.subview('sectionsView', sectionsView);
            }

            var groupingView = this.subview('groupingView');
            if (!groupingView && grouping != null) {
                var groupingId = grouping.groupingId;
                groupingView = new GroupingView({autoRender: false, model: new Model(grouping)});
                this.subview('groupingView', groupingView);
            }

            sectionsView.render();
            var $sectionsViewContainer = this.$el.find(sectionsView.container);
            $sectionsViewContainer.addClass(sectionsView.className);
            $sectionsViewContainer.append(sectionsView.$el);

            return this;
        },
	    _doValidate: function($button) {
	        var data = new FormData();

            $('.generated').remove();
            $('.control-group').removeClass('error');
            $('.control-group').removeClass('warning');

            $('.section:visible').find(':input').each(function(index, element) {
                var name = element.name;
                if (name == undefined || name == null || name == '')
                    return;

                if (element.files !== undefined && element.files != null) {
                    $.each(element.files, function(fileIndex, file) {
                        if (file != null)
                            data.append(name, file);
                    });
                } else {
                    var $element = $(element);
                    var value = $(element).val();

                    if (($element.is(':radio') || $element.is(':checkbox'))) {
                        if ($element.is(":checked")) {
                            if (value != undefined)
                                data.append(name, value);
                        }
                    } else {
                        data.append(name, value);
                    }
                }
            });

            var screen = this.model.get("screen");
            var groupings = screen.groupings;
            var groupingIndex = this.model.get("groupingIndex");
            var grouping = groupings[groupingIndex];
            var groupingId = grouping.groupingId;
            var url = this.model.get("action") + '/' + groupingId + '.json';

            $.ajax({
                url : url,
                data : data,
                processData : false,
                contentType : false,
                type : 'POST',
                success: function() {
                    $button.attr('data-validated', true);
                },
                statusCode : {
                    204 : this._onFormValid,
                    400 : this._onFormInvalid,
                    'default' : this._onFailure,
                }
            });
	    },
	    _onAddedToDOM: function(event) {
            var formValues = this.model.get('formData');
            if (formValues != undefined && formValues.length > 0) {
                for (var i=0;i<formValues.length;i++) {
                    var formValue = formValues[i];
                    var name = formValue.name;
                    if (name != undefined) {
                        var selector = ':input[name="' + name + '"]';
                        var $element = $(selector);
                        var values = formValue.values;
                        if (values != null && values.length > 0) {
                            if (values.length > 1) {
                                var $controlGroup = $element.closest('.control-group');
                                var $input = $controlGroup.find(':input[type="text"]:last');
                                var $clone = $input.clone();
                                $clone.val();
                                $controlGroup.append("<br/>");
                                $controlGroup.append($clone);
                            }

                            if ($element.attr('type') != 'file') {
                                $element.val(values);
                            } else {
                                var re = RegExp("image/");
                                if (formValue.accept != null && re.test(formValue.accept)) {
                                    $element.before('<image src="' + formValue.link + '" alt="' + values[0] + '"/>');
                                } else {
                                    $element.before('<div class="file"><a href="' + formValue.link + '">' + values[0] + "</a></div>");
                                }
                            }
                        }
                    }
                }
            }
	    },
	    _onLoaded: function(event) {
            Chaplin.mediator.publish('formAddedToDOM');
	    },
	    _onFormSubmit: function(event) {
	        var screen = this.model.get("screen");
            var type = screen.type;

            var $button = $(event.target);
            var validated = $button.attr('data-validated');
            if (validated)
                return true;

//            var validated = $('#main-form').prop("validated");
//            if (type != 'wizard' || (validated != undefined && validated))
//                return true;

            this._doValidate($button);

            return false;

	    },
	    _onFormValid: function(data, textStatus, jqXHR) {

            var next = $(':button[type="submit"]:visible').val();

            if (next == 'submit' || next == 'reject' || next == 'approve') {
                $('#main-form').prop("validated", true);
                $('#main-form').submit();
            } else {
                var breadcrumbSelector = 'a[href="' + next + '"]';
                var $li = $('ul.breadcrumb').find(breadcrumbSelector).closest('li'); //.prev('li');
                $li.find('span.inactive-text').remove();
                $li.find('a').removeClass('hide');

                Chaplin.mediator.publish('!router:route', next);
            }
	    },
	    _onFormInvalid: function(jqXHR, textStatus, errorThrown) {
            var errors = $.parseJSON(jqXHR.responseText);

            if (errors.items != null) {
                for (var i=0;i<errors.items.length;i++) {
                    var item = errors.items[i];
                    var selector = ':input[name="' + item.propertyName + '"]';
                    var $input = $(selector);
                    var $element = $input;

                    $input.closest('.control-group').addClass(item.type);

                    if ($input.is(':checkbox') || $input.is(':radio')) {
                        $element = $input.closest('.control-group').find('label');
                    }
                    $element.after('<span class="help-inline generated">' + item.message + '</span>')
                }
            }
	    },
	    _onGroupingIndexChange: function(groupingIndex) {
	        this.model.set("groupingIndex", groupingIndex);
	        var screen = this.model.get("screen");

            if (screen == undefined)
                return this;

            var task = this.model.get("task");
            var pageLink = this.model.get("link");
            var groupings = screen.groupings;
            var grouping = groupings != undefined && groupings.length > groupingIndex ? groupings[groupingIndex] : { sectionIds : []};
            if (grouping !== undefined) {
                $(".section").addClass('hide');
                for (var i=0;i<grouping.sectionIds.length;i++) {
                    var selector = '.section#' + grouping.sectionIds[i];
                    this.$(selector).removeClass('hide');
                }
            }

            this.removeSubview('buttonsView');

            if (task !== undefined && task != null) {
                if (!task.active) {
                    var inputs = this.$el.find(':input');
                    inputs.prop('disabled', true);

                    $('#comment-button').prop('disabled', true);
                    $('#suspend-button').addClass('btn-success');
                    $('#suspend-button').attr('title', 'Reactivate')

                    var notification = new Notification({title: 'Process suspended', message: 'This process has been suspended and no other actions can be taken on it until it has been reactivated. Use the green button at the top-left of this window to reactivate.'})
                    this.subview('notification', new NotificationView({container: '.notifications', model: notification}));

                    return;
                }
            }

            var buttonsView;
            if (grouping.buttons != undefined && grouping.buttons.length > 0) {
              var buttonList = grouping.buttons;
              for (var b=0;b<buttonList.length;b++) {
                  var button = buttonList[b];
                  var buttonId = button.buttonId;

                  if (button.value != undefined) {
                      if (button.value == 'next') {
                          button.value = groupings.length > grouping.ordinal ? groupings[groupingIndex + 1].breadcrumbLink : '';
                          button.link = button.value;
                          button.alt = "Next";
                      } else if (buttonList[b].value == 'prev') {
                          if (groupingIndex > 0)
                            button.value = groupings[groupingIndex - 1].breadcrumbLink;
                          else {
                            var rootLink = pageLink;
                            var indexOfLastSlash = rootLink.lastIndexOf('/');
                            if (indexOfLastSlash != -1 && indexOfLastSlash < rootLink.length)
                                rootLink = rootLink.substring(0, indexOfLastSlash);
                            button.value = rootLink;
                          }
                          button.link = button.value;
                          button.alt = "Previous";
                      }
                  }

                  var buttons = new Buttons(buttonList);
                  buttonsView = new ButtonsView({autoRender: false, collection: buttons});
                  this.subview('buttonsView', buttonsView);
              }
            }
            if (buttonsView !== undefined) {
                buttonsView.render();
                var $buttonsViewContainer = this.$el.find(buttonsView.container);
                $buttonsViewContainer.addClass(buttonsView.className);
                $buttonsViewContainer.append(buttonsView.$el);
            }
	    },
	    _onFailure: function(jqXHR, textStatus, errorThrown) {
            alert('Failure!');
	    },
	    _onShowAttachments: function() {
	        var attachmentsView = this.subview('attachmentsView');

	        if (attachmentsView == undefined) {
	            var urlRoot = this.model.get("attachment");
	            var attachments = new Attachments({}, {url: urlRoot});
	            this.listenTo(attachments, 'sync', this._onSyncAttachments);
                this.subview('attachmentsView', new AttachmentsView({collection: attachments}));
                attachments.fetch();
            } else {
                attachmentsView.collection.fetch();
                attachmentsView.$el.toggle(0);
            }
	    },
	    _onSyncAttachments: function(attachments) {
            Chaplin.mediator.publish('attachmentCountChanged', attachments.length);
            this.$el.toggleClass('span9');
	    }
	});

	return FormView;
});