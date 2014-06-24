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
            frames: ["frame0000.jpg","frame0001.jpg","frame0002.jpg","frame0003.jpg","frame0004.jpg","frame0005.jpg","frame0006.jpg","frame0007.jpg","frame0008.jpg","frame0009.jpg","frame0010.jpg","frame0011.jpg","frame0012.jpg","frame0013.jpg","frame0014.jpg","frame0015.jpg","frame0016.jpg","frame0017.jpg","frame0018.jpg","frame0019.jpg","frame0020.jpg","frame0021.jpg","frame0022.jpg","frame0023.jpg","frame0024.jpg","frame0025.jpg","frame0026.jpg","frame0027.jpg","frame0028.jpg","frame0029.jpg","frame0030.jpg","frame0031.jpg","frame0032.jpg","frame0033.jpg","frame0034.jpg","frame0035.jpg","frame0036.jpg","frame0037.jpg","frame0038.jpg","frame0039.jpg","frame0040.jpg","frame0041.jpg","frame0042.jpg","frame0043.jpg","frame0044.jpg"],
            pos: 0,
            frameCount: 45,
            playing: false
            
        };
        $scope.isFullscreen = false;
        $scope.imgDir = 'images/Eclipse0f989895-65a6-3af3-bf02-497cd0f4f35bK/';
        $scope.toolName = 'test';
        $scope.showRating = false;
        $scope.playBtnImages = {
            true: 'images/playback/pause.svg',
            false:'images/playback/play.svg'
        }
        $scope.keyboardMode = 0;
        $scope.keyboardOverlay = 'inactive';
        $scope.keyboardImages = {
            'active': [ 'image_text.png', 'image.png', 'text.png' ],
            'inactive': [ 'image_text_un.png', 'image_un.png', 'text_un.png']
        }
        $scope.clipDetails = {
            keyboardEventFrame: 25,
        }

        $scope.$watch(function(){ 
            return $scope.player.pos > $scope.clipDetails.keyboardEventFrame; 
        }, function(newVal, oldVal) {
            $scope.keyboardOverlay = (newVal ? 'active' : 'inactive')
        });

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
