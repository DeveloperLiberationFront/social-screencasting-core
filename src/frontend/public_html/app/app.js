/*global define, _*/
define(['angular',
        'bluebird',
        'services',
        'ng-route',
        'controllers',
        'share',
        'ng-bootstrap',
        'ng-fullscreen',
        'ng-ui-utils',
        'ng-ui-router',
        'ng-slider',
        'player',
        'directives',
        'lib/breadcrumb',
       ], function (ng, Promise) {
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
                url: "/?user_filter&tool_filter?app_filter?misc_filter", //can't call it tool because tool already used in video player
                views: {
                  'left-sidebar': {
                    templateUrl: 'partials/filter.html',
                    controller: 'FilterCtrl'
                  },
                  'center': {
                    templateUrl: 'partials/tool-list.html',
                    controller: 'ToolListCtrl'
                  }
                },
                breadcrumb: { title: 'Home' }
            })

            .state('main.video', {
              url: '/video?location&owner&tool_name&tool_id&clip_id&application&user_name',
              onEnter: function($state, $stateParams, $modal, $rootScope, Hub, Local) {
                var clips;
                if ($stateParams.location == "external") {
                  //remote clips; fetch from hub
                  clips = Hub.all('clips').getList({
                    where: {
                      tool: $stateParams.tool_id, 
                      user: $stateParams.owner //restrict to clips by owner, if specified
                    },
                    embedded: {user: 1}
                  });
                } else {
                  clips = Local.all('clips').getList({
                    app: $stateParams.application,
                    tool: $stateParams.tool_name
                  });
                  clips.then(function(clips) {
                    _.each(clips, function(clip) {
                      clip.id = clip.name; //add id so restangular can find images
                    })
                  })
                }

                clips.then(function(clips) {
                  _.each(clips, function(clip) {
                    clip.origin = $stateParams.location;
                  });

                  $modal.open({
                    templateUrl: 'partials/modal-player.html',
                    controller: 'ModalPlayer',
                    scope: $rootScope,
                    resolve: {
                      clip_name: function(){
                        return typeof clip_id == 'string' ? clip_id : false;
                      },
                      clips: function() { return clips; },
                      tool: function() { return $stateParams.tool_name;}
                    },
                    windowClass: (clips.length > 1 ? 'modal-multiclip-player'
                                  : 'modal-player'),
                    size: 'lg'
                  }).result.finally(function(result) {
                    return $state.transitionTo("main");
                  });
                });
              }
            })

            .state('main.request', {
                url: '/request?owner&application&tool',
                onEnter: function($stateParams, $state, $rootScope, $modal) {
                  $modal.open({
                    templateUrl: 'partials/request-share.html',
                    controller: 'RequestCtrl',
                    scope: $rootScope,
                  }).result.finally(function(result) {
                    return $state.transitionTo("main");
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
                views: {
                  'center': {
                    templateUrl: 'partials/share.html',
                    controller: 'ShareCtrl',
                  },
                },
                breadcrumb: { title: 'Share' }
            });
    }])

  .config(['RestangularProvider', 'User',
    function(RestangularProvider, User) {
      RestangularProvider.setDefaultHttpFields({cache: true});
      RestangularProvider.addRequestInterceptor(function(elem, operation) {
        if (operation === "remove") {
          return undefined;
        }
        return elem;
      });
      RestangularProvider.setDefaultHeaders({
        'Authorization': 'Basic ' + btoa(User.email + '|'
                                         + User.name + ':'
                                         + User.token)});
  }])
});
