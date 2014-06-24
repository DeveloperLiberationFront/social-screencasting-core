define(['angular',
        'jquery',
        'underscore',
        'ng-bootstrap',
        'ng-grid',
        'services'],
       function (ng, $, _) {
  /* Controllers */

  function hashToList(hash, names) {
      names = _.defaults(names, {key: 'name', val: 'value'})
      result = []
      $.each(hash, function(key, val) {
          if (!_.isObject(val)) {
              val = _.object([[names.val, val]]);
          }
          result.push(ng.extend(_.object([[names.key, key]]), val))
      });
      return result;
  }
  
  return ng.module('socasterControllers',
                   ['ui.bootstrap',
                    'ngGrid',
                    'ngResource',
                    'socasterServices'])
  
  .controller('NavCtrl', ['$scope', 'User', 'Application',
    function($scope, User, Application) {
        $scope.user = User.query();
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

  .controller('PlayerCtrl', ['$scope', '$interval',
    function($scope, $interval) {
        $scope.player = {
            frames: ["frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg","frame000.jpg"],
            pos: 0,
            frameCount: 100,
            playing: false
        };
        $scope.imgDir = 'images/';
        $scope.toolName = 'test';
        $scope.showRating = false;
        $scope.playBtnImages = {
            true: 'images/playback/pause.svg',
            false:'images/playback/play.svg'
        }

        $scope.timer = $interval(function() {
            if ($scope.player.playing) { //playing
                console.log($scope.player.pos);
                $scope.player.pos = (+$scope.player.pos + 1) % $scope.player.frameCount;
            }
        }, 200);
    }])

  .controller('PlaybackSliderCtrl', ['$scope',
    function($scope) {
    }]);
});
