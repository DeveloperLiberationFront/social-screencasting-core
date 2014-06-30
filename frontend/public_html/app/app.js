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
        .when('/status', {
          templateUrl: 'partials/status.html',
          controller: 'StatusCtrl'
        })
        .when('/share/:application/:tool/:clip', {
          templateUrl: 'partials/share.html',
          controller: 'ShareCtrl'
        })

        .otherwise({
            redirectTo: '/'
        });
    }
  ]);
});
