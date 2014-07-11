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
        $urlRouterProvider.otherwise('/');
        $stateProvider
            .state('main', {
                url: "/",
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
                  'right-sidebar': {}
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
            .state('video', {
              url: '/video/:location/:owner/:application/:tool?clip_id',
              onEnter: function($stateParams, $state, $modal, $rootScope, Hub, Local) {
                var origin = ($stateParams.location == "external" ? Hub : Local);
                var tool = origin.one($stateParams.owner)
                  .one($stateParams.application)
                  .one($stateParams.tool);
                
                $rootScope.preAuth.then(function(auth){
                  $rootScope.auth = auth;
                  return tool.get(auth);
                }).then(function(tool) {
                  return _.union(tool.keyclips, tool.guiclips);
                }).then(function(clips) {
                  $modal.open({
                    templateUrl: 'partials/modal-player.html',
                    controller: 'ModalPlayer',
                    scope: $rootScope,
                    resolve: {
                      clip_id: function(){return typeof clip_id == 'string' ? clip_id : false; },
                      clips: ['$q', function($q){
                        //fetch all of the users clips for use in the player
                        return $q.all(clips.map(function(clip) {
                          return tool.one(clip).get($rootScope.auth);
                        }));
                      }]
                    },
                    windowClass: (clips.length > 1 ? 'modal-multiclip-player'
                                  : 'modal-player'),
                    size: 'lg'
                  }).result.finally(function(result) {
                    return $state.transitionTo("main");
                  });
                });
              }})
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
      RestangularProvider.addRequestInterceptor(function(elem, operation) {
          if (operation === "remove") {
           return undefined;
       } 
       return elem;
   });
  }]);
});
