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
        'directives',
        'lib/breadcrumb',
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
    'breadcrumb',
    'directives'
  ])
  
  .config(['$stateProvider','$urlRouterProvider',
    function($stateProvider, $urlRouterProvider) {
//        $urlRouterProvider.otherwise('');
        $stateProvider
            .state('main', {
                url: "",
                templateUrl: 'partials/main.html',
                views: {
                  'left-sidebar': {
                    templateUrl: 'partials/filter.html',
                    controller: 'FilterCtrl'
                  },
                  'center': {
                    templateUrl: 'partials/tool-list.html',
                    controller: 'ToolListCtrl'
                  },
                  'right-sidebar': {
                    template: "<div class='gridStyle'\
                                    ng-grid='gridOptions'></div>",
                    controller: 'ApplicationToolsCtrl'
                  }
                },
                breadcrumb: { title: 'Home' }
            })
            .state('tools', {
                url: '/tools/:name',
                templateUrl: 'partials/tool-details.html',
                controller: 'ToolCtrl',
                breadcrumb: { title: 'Tool: {:name}' }
            })
            .state('player', {
                url: '/player',
                templateUrl: 'partials/player.html',
                controller: 'PlayerCtrl',
                breadcrumb: { title: '{clip.name}' }
            })
            .state('status', {
                url: '/status',
                templateUrl: 'partials/status.html',
                controller: 'StatusCtrl',
                breadcrumb: { title: 'Status' }
            })
            .state('share', {
                url: '/share/:application/:tool?share_with_name&share_with_email&request_id',
                templateUrl: 'partials/share.html',
                controller: 'ShareCtrl',
                breadcrumb: { title: 'Share' }
            });
    }])

  .config(['RestangularProvider',
    function(RestangularProvider) {
      RestangularProvider.setDefaultHttpFields({cache: true});
    }]);
});
