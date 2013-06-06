define([ 'chaplin', 'models/base/collection', 'models/design/process'], function(Chaplin, Collection, Process) {
	'use strict';

	var Processes = Collection.extend({
		model: Process,
		comparator: 'ordinal',
		url: 'process',
		parse: function(response, options) {
			return response.list;
		},
	});
	return Processes;
});