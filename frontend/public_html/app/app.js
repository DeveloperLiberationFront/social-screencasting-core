define(['angular',
        'ng-route',
        'controllers',
        'services',
        'ng-bootstrap',
        'ng-fullscreen',
        'ng-slider',
       ], function (ng) {
  'use strict';

  /* App Module */
  
  return ng.module('socasterApp', [
    'ngRoute',
    'ui.bootstrap',
    'socasterControllers',
    'socasterServices',
    'ngFullscreen',
    'uiSlider',
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
  ]);
});
