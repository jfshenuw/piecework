define([
        'chaplin',
        'views/form/attachment-view',
        'views/base/collection-view',
        ],
    function(Chaplin, AttachmentView, CollectionView) {
	'use strict';

	var AttachmentsView = CollectionView.extend({
		autoRender: true,
		className: 'attachments span3',
        container: '.main-content',
        fallbackSelector: '.attachment-fallback',
		tagName: 'ul',
        itemView: AttachmentView,
        render: function() {
            this.$el.append('<div class="attachment-fallback hide">No items</div>');
            CollectionView.__super__.render.apply(this);
            return this;
        }
	});

	return AttachmentsView;
});