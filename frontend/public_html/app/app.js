define(['angular',
        'ng-route',
        'controllers',
        'services',
        'ng-bootstrap',
        'ng-fullscreen',
        'ng-ui-utils',
        'ng-slider',
       ], function (ng) {
  'use strict';

  /* App Module */
  
  return ng.module('socasterApp', [
    'ngRoute',
    'ui.bootstrap',
    'ui.jq',
    'socasterControllers',
    'socasterServices',
    'ngFullscreen',
    'vr.directives.slider',
  ])
  
  .config(['$routeProvider',
    function($routeProvider) {
      $routeProvider.
        when('/', {
          templateUrl: 'partials/player.html',
          controller: 'PlayerCtrl'
        })
        .otherwise({
            redirectTo: '/'
        });
    }
  ])

  .value('uiJqConfig', {
      // The Tooltip namespace
      tooltip: {
          // Tooltip options. This object will be used as the defaults
          placement: 'right'
      }
  });
});
