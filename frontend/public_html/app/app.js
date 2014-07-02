define(['angular',
        'ng-route',
        'controllers',
        'services',
        'ng-bootstrap',
        'ng-fullscreen',
        'ng-ui-utils',
        'ng-ui-router',
        'ng-slider',
        'player',
       ], function (ng) {
  'use strict';

  /* App Module */
  
  return ng.module('socasterApp', [
    'ui.router',
    'ui.bootstrap',
    'socasterControllers',
    'socasterServices',
    'player',
    'ngFullscreen',
    'vr.directives.slider',
    'restangular',
  ])
  
  .config(['$stateProvider','$urlRouterProvider',
    function($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise('/');
        $stateProvider
            .state('main', {
                url: "/",
                templateUrl: 'partials/main.html',
                controller: 'MainCtrl',
            })
            .state('tools', {
                url: '/tools/:name',
                templateUrl: 'partials/tool-details.html',
                controller: 'ToolCtrl'
            })
            .state('player', {
                url: '/player',
                templateUrl: 'partials/player.html',
                controller: 'PlayerCtrl'
            })
            .state('status', {
                url: '/status',
                templateUrl: 'partials/status.html',
                controller: 'StatusCtrl'
            })
            .state('share', {
                url: '/share/:application/:tool?shareWithName&shareWithEmail',
                templateUrl: 'partials/share.html',
                controller: 'ShareCtrl'
            });
    }])

  .config(['RestangularProvider',
    function(RestangularProvider) {
      RestangularProvider.setDefaultHttpFields({cache: true});
    }]);
});
