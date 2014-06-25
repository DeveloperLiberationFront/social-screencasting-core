define(['angular',
        'jquery',
        'underscore',
        'ng-bootstrap',
        'ng-grid',
        'ng-ui-utils',
        'services'],
       function (ng, $, _) {
  /* Controllers */

  return ng.module('socasterControllers',
                   ['ui.bootstrap',
                    'ui.format',
                    'ngGrid',
                    'ngResource',
                    'socasterServices'])
  
  .controller('NavCtrl', ['$scope', '$filter', 'User', 'Application',
    function($scope, $filter, User, Application) {
        $scope.user = User.get(function(user){
            $scope.auth = '?'+$.param(_.pick(user, 'name', 'email', 'token'));
        });
        $scope.applications = Application.query(function() {
            $scope.$broadcast('appSelected', $scope.applications[0]);
        });
        $scope.$on('appSelected', function(event, app) {
            app.$promise = app.$get();
            $scope.application = app;
        });
    }])

  .controller('MainCtrl', ['$scope',
    function($scope) {
    }])

  .controller('UserListCtrl', ['$scope',
    function($scope) {
        $scope.$on('appSelected', function(event, app) {
            app.$promise.then(function(){
                $scope.users = app.users
            });
        });
        $scope.gridOptions = { 
            data: 'users',
            columnDefs: [{field:'name', displayName:'Name'},
                         {field:'email', displayName:'Email'}]
        }
    }])

  .controller('LocalToolsCtrl', ['$scope', 'User',
    function($scope, User) {
        $scope.selection = [];
        $scope.$on('appSelected', function(event, app) {
            $scope.data = User.get(function(user) {
                $scope.tools = user.applications[app.name]
            })
        });
        $scope.gridOptions = { 
            data: 'tools',
            selectedItems: $scope.selection,
            multiSelect: false,
            columnDefs: [{field:'name', displayName:'Name'},
                         {field:'keyboard', displayName:'Keyboard'},
                         {field:'gui', displayName:'GUI'}]
        };
    }])

  .controller('ApplicationToolsCtrl', ['$scope', 'Application',
    function($scope, Application) {
        $scope.selection = [];
        $scope.$on('appSelected', function(event, app) {
            app.$promise.then(function(){
                $scope.tools = app.tools;
            });
        });
        $scope.gridOptions = { 
            data: 'tools',
            selectedItems: $scope.selection,
            multiSelect: false,
            columnDefs: [{field:'name', displayName:'Name'}]
        };
    }])

  .controller('DropdownCtrl', ['$scope', '$rootScope', 
    function($scope, $rootScope) {
      $scope.status = {
          isopen: false
      };
      $scope.setApp = function(app) {
        $scope.application = app;
        $scope.status.isopen = false;
        $rootScope.$broadcast('appSelected', app);
      }
    }])

  .controller('RatingCtrl', ['$scope',
    function($scope) {
    }])

  .controller('FullscreenCtrl', ['$scope',
    function($scope) {
        $scope.isFullscreen = false;
        $scope.toggle = function() {
            $scope.isFullscreen = !$scope.isFullscreen;
        }
    }])

  .controller('PlayerCtrl', ['$scope', '$interval', 'Clip', '$filter',
    function($scope, $interval, Clip, $filter) {
        $scope.player = {
            pos: 0,
            playing: false,
            editMode: true,
            start: 0,
            end: 1,
        };
        $scope.isFullscreen = false;
        $scope.showRating = false;
        $scope.playBtnImages = {
            true: 'images/playback/pause.svg',
            false:'images/playback/play.svg'
        }
        $scope.kbdOverlay = {
            mode: 0,
            status: 'inactive',
            images: {
                'active': [ 'image_text.png', 'image.png', 'text.png', 'none.png' ],
                'inactive': [ 'image_text_un.png', 'image_un.png', 'text_un.png', 'none.png']
            }
        }

        $scope.user.$promise.then(function() {
            $scope.clip = Clip.get(_.extend({
                creator:"kjlubick@ncsu.edu",
                app:"Eclipse",
                tool:"Extract Local Variable",
                clip:"Eclipse8d6980ed-a27a-30bd-97c5-3174bf71017cK",
            }, _.pick($scope.user, 'token', 'email', 'name')), function(clip) {
                $scope.imgDir = $filter('format')(
                    'http://screencaster-hub.appspot.com/api/:creator/:app/:tool/:name/',
                    clip
                )
                $scope.player.end = clip.frames.length-1;
                clip.loaded = clip.frames.map(function(frame){
                    return new Image().src = $scope.imgDir + frame + $scope.auth;
                })
                clip.keyboardEventFrame = 25;
            })
        });

        $scope.$watch(function(){ 
            return $scope.player.pos > $scope.clip.keyboardEventFrame; 
        }, function(newVal, oldVal) {
            $scope.kbdOverlay.status = (newVal ? 'active' : 'inactive')
        });

        $scope.timer = $interval(function() {
            if ($scope.player.playing) { //playing
                $scope.player.pos = Math.max($scope.player.start, 
                                             (+$scope.player.pos + 1) % ($scope.player.end+1));
            }
        }, 200);
    }])

  .controller('PlaybackSliderCtrl', ['$scope',
    function($scope) {
    }])

  .controller('EditSliderCtrl', ['$scope',
    function($scope) {
    }]);
});
