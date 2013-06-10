define([ 'chaplin',
         'views/base/view',
         'text!templates/intro.hbs',
         'models/design/process',
         'views/design/process-list-view' ],
	function(Chaplin, View, template, Process, ProcessListView) {
	'use strict';

	var IntroView = View.extend({
		autoRender : true,
		container: '#main-screen',
//		region: 'main',
	    template: template,
	});

	return IntroView;
});