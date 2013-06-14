define([
        'chaplin',
        'models/design/field',
        'views/base/collection-view',
        'views/form/field-checkbox-view',
        'views/form/field-listbox-view',
        'views/form/field-html-view',
        'views/form/field-radio-view',
        'views/form/field-textarea-view',
        'views/form/field-textbox-view'
        ],
    function(Chaplin, Field, CollectionView, CheckboxView, ListboxView, HtmlView, RadioView, TextareaView,
				TextboxView) {
	'use strict';

	var FieldsView = CollectionView.extend({
		autoRender: false,
		className: "fields",
		container: ".section-content",
		tagName: 'div',
		initItemView: function(field) {
			var fieldId = field.cid;
			var type = field.get("type");
			if (type == 'checkbox')
				return new CheckboxView({id: fieldId, model: field});
	    	else if (type == 'select-one' || type == 'select-multiple')
	    		return new ListboxView({id: fieldId, model: field});
	    	else if (type == 'radio')
	    		return new RadioView({id: fieldId, model: field});
	    	else if (type == 'textarea')
	    		return new TextareaView({id: fieldId, model: field});
	        else if (type == 'html')
	            return new HtmlView({id: fieldId, model: field});
	    	else
	    		return new TextboxView({id: fieldId, model: field});
	    },
	});

	return FieldsView;
});