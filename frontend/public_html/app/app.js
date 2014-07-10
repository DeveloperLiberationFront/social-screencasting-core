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
                    template: "<div class='gridStyle' ng-grid='gridOptions'></div>",
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
            .state('video', {
              url: '/video/:location/:owner/:application/:tool/:clip_id',
              onEnter: function($stateParams, $state, $modal, $rootScope, Hub, Local) {
                $modal.open({
                  templateUrl: 'partials/modal-player.html',
                  controller: 'ModalPlayer',
                  scope: $rootScope,
                  resolve: {
                    clips: ['$q', function($q){
                      //promise chain - return a promise based on preAuth.  When preAuth is resolved, a new set of promises is set off
                      return $rootScope.preAuth.then(function(auth){
                        $rootScope.auth = auth;
                        if ($stateParams.location == "external") {
                          //clips is expected to be an array, so we use $q.all which will return an array of length 1, this clip
                          return $q.all([Hub.one($stateParams.owner)
                            .one($stateParams.application)
                            .one($stateParams.tool)
                            .one($stateParams.clip_id).get(auth)]);
                        } else {
                          //clips is expected to be an array, so we use $q.all which will return an array of length 1, this clip
                          return $q.all([Local.one($stateParams.owner)
                            .one($stateParams.application)
                            .one($stateParams.tool)
                            .one($stateParams.clip_id).get(auth)]);
                        }
                      });
                      
                      
                    }]},
                    windowClass: 'modal-player',
                    size: 'lg'
                  }).result.then(function(result) {
                    console.log("In the then of the modal");
                    if (result) {
                      return $state.transitionTo("items");
                    }
                  });
                }
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
      RestangularProvider.addRequestInterceptor(function(elem, operation) {
          if (operation === "remove") {
           return undefined;
       } 
       return elem;
   });
  }]);
});
