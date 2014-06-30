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
    'socasterControllers',
    'socasterServices',
    'ngFullscreen',
    'vr.directives.slider',
    'restangular',
  ])
  
  .config(['$routeProvider',
    function($routeProvider) {
      $routeProvider
        .when('/', {
          templateUrl: 'partials/main.html',
          controller: 'MainCtrl',
        })
        .when('/player', {
          templateUrl: 'partials/player.html',
          controller: 'PlayerCtrl'
        })
        .otherwise({
            redirectTo: '/'
        });
    }
  ])
});
