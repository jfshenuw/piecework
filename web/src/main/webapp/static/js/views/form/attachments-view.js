define([
        'chaplin',
        'models/notification',
        'views/form/attachment-view',
        'views/base/collection-view',
        'views/form/notification-view',
        ],
    function(Chaplin, Notification, AttachmentView, CollectionView, NotificationView) {
	'use strict';

	var AttachmentsView = CollectionView.extend({
		autoRender: true,
		className: 'attachments col-lg-3 col-sm-3',
        container: '.main-content',
        fallbackSelector: '.attachment-fallback',
		tagName: 'ul',
        itemView: AttachmentView,
        listen: {
            'refreshAttachments mediator': '_onRefreshAttachments',
            'showAttachments mediator': '_onShowAttachments',
        },
        showAttachments: function() {
//            this._onShowAttachments();
        },
        _onRefreshAttachments: function() {
//            this.collection.fetch();
        },
        _onShowAttachments: function() {

              this.$el.toggle();
        }
	});

	return AttachmentsView;
});