define(['controllers/form-controller'], function() {
  'use strict';

  return function(match) {
    match('', 'form#index');
    match('step/:ordinal', 'form#index');
  };
});