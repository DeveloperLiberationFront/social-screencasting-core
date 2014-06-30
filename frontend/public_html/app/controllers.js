define(['angular',
        'jquery',
        'lodash',
        'jquery.cropper',
        'ng-bootstrap',
        'ng-grid',
        'ng-ui-utils',
        'restangular',
        'services'],
       function (ng, $, _) {
  /* Controllers */

  return ng.module('socasterControllers',
                   ['ui.bootstrap',
                    'ui.format',
                    'ngGrid',
                    'ngResource',
                    'socasterServices',
                    'restangular'])

  .controller('NavCtrl', ['$scope', '$filter', 'Local', 'Hub',
    function($scope, $filter, Local, Hub) {
        Local.one('user').get().then(function(user){
            $scope.user = user;
            $scope.auth = user.plain();
        });
        Hub.one('details').getList().then(function(apps){
            $scope.applications = apps;
            $scope.$broadcast('appSelected', apps[0]);
        });
        $scope.$on('appSelected', function(event, app) {
            $scope.application = Hub.one('details').one(app.name).get($scope.auth);
        });
    }])

  .controller('MainCtrl', ['$scope',
    function($scope) {
    }])

  .controller('UserListCtrl', ['$scope',
    function($scope) {
        $scope.$on('appSelected', function() {
            $scope.application.then(function(app){
                $scope.users = app.users;
            });
        });
        $scope.gridOptions = {
            data: 'users',
            multiSelect: false,
            columnDefs: [{field:'name', displayName:'Name'},
                         {field:'email', displayName:'Email'}]
        };
    }])

  .controller('ApplicationToolsCtrl', ['$scope', '$modal', 'Local', 'Hub',
    function($scope, $modal, Tool) {
        $scope.selection = [];
        $scope.gridOptions = {
            data: 'application.$object.tools',
            selectedItems: $scope.selection,
            multiSelect: false,
            columnDefs: [
                { field:'name', displayName:'Name' },
                {
                    width: '50px',
                    field:'new', displayName:'New',
                    cellTemplate: "<div class='ngCellText'><span class='glyphicon glyphicon-ok' ng-show='row.getProperty(col.field)'></span></div>"
                },
                {
                    width: '50px',
                    field:'video', displayName:'Video',
                    cellTemplate:  "<div class='ngCellText'><img src='images/video_icon_tiny.png' ng-show='row.getProperty(col.field)'/></div>",
                    sortFn: function(x,y){return (x === y)? 0 : x? 1 : -1;}
                }
            ],
            sortInfo: {
                fields: ['new', 'video'],
                directions: ['desc', 'desc']
            },
            afterSelectionChange: function() {
                s = $scope.selection;

                if (s.length > 0 && s[0].video) {
                    tool = s[0];
                    $modal.open({
                        templateUrl: 'partials/modal-player.html',
                        controller: 'ModalPlayer',
                        scope: $scope,
                        resolve: { tool: function(){
                            return $scope.application.$object.one(tool.name).get($scope.auth);
                        }},
                        windowClass: 'modal-player',
                        size: 'lg'
                    });
                }
            }
        };
    }])

  .controller('DropdownCtrl', ['$scope', '$rootScope',
    function($scope, $rootScope) {
      $scope.status = {
          isopen: false
      };
      $scope.setApp = function(app) {
        $scope.status.isopen = false;
        $rootScope.$broadcast('appSelected', app);
      };
    }])

  .controller('RatingCtrl', ['$scope',
    function($scope) {
    }])

  .controller('FullscreenCtrl', ['$scope',
    function($scope) {
        if (!$scope.isCropping) {
            $scope.isFullscreen = !$scope.isFullscreen;
        }
    }])

  .controller('PlayerCtrl', ['$scope', '$interval', 'Clip', '$filter',
    function($scope, $interval, Clip, $filter) {
        $scope.player = {
            pos: 0,
            playing: false,
            editMode: false,
            isCropping: false
        };
        $scope.isFullscreen = false;
        $scope.toggleFullscreen = function() {
          if (!$scope.player.isCropping) {
            $scope.isFullscreen = !$scope.isFullscreen;
          }
        };
        $scope.showRating = false;
        $scope.playBtnImages = {
            true: 'images/playback/pause.svg',
            false:'images/playback/play.svg'
        };
        $scope.kbdOverlay = {
            mode: 0,
            enabled: $scope.clip.name.substr(-1)=='K', //only clips ending in 'k' have keyboard info
            status: 'inactive',
            images: {
                'active': [ 'image_text.png', 'image.png', 'text.png', 'none.png' ],
                'inactive': [ 'image_text_un.png', 'image_un.png', 'text_un.png', 'none.png']
            },
            tooltip: ["Image and text", "Image only", "Text only", "No overlay"]
        };

        $scope.imgDir = $scope.clip.getRestangularUrl() + '/';

        $scope.imagePath = function(image) {
            return $scope.imgDir + image +'?'+ $.param($scope.auth);
        };

        _.extend($scope.clip, {
            loaded: $scope.clip.frames.map(function(frame){
                var img = new Image();
                img.src = $scope.imagePath(frame);
                return img;
            }),
            keyboardEventFrame: 25,
            start: 0,
            end: $scope.clip.frames.length-1
        });

        $scope.$watch(function(){
            return $scope.player.pos > $scope.clip.keyboardEventFrame;
        }, function(newVal, oldVal) {
            $scope.kbdOverlay.status = (newVal ? 'active' : 'inactive');
        });

        $scope.timer = $interval(function() {
            if ($scope.player.playing) { //playing
                $scope.player.pos = Math.max($scope.clip.start,
                                             (+$scope.player.pos + 1) % ($scope.clip.end+1));
            }
        }, 200);

        $scope.crop = function() {
            if (!$scope.player.isCropping) {
                $scope.player.isCropping = true;
                $(".img-container img").cropper({
                    aspectRatio: "auto",
                    done: function(data) {
                        console.log(data);
                    }
                }).cropper('enable');
            } else {
                $scope.player.isCropping = false;
                $(".img-container img").cropper("disable");
                //handle crop completion?
            }
        };
    }])

  .controller('ModalPlayer', ['$scope', '$modalInstance', 'tool',
    function($scope, $modalInstance, tool) {
        //tool is a restangular object
        $scope.status = 'loading';
        $scope.close = function () {
            $modalInstance.close();
        };

        if (tool.clips.length > 0) {
            tool.one(tool.clips[0].name).get($scope.auth).then(function(clip) {
                $scope.clip = clip; 
                $scope.status = 'ready';
                $scope.$broadcast('refreshSlider');
            });
        }
    }])

  .controller('PlaybackSliderCtrl', ['$scope',
    function($scope) {
    }])

  .controller('StatusCtrl', function() {

  })
  .controller('ShareCtrl', ['$scope', '$routeParams', 'Clip', function($scope, $routeParams, Clip) {
    console.log($routeParams);
    console.log($scope);
    $scope.applicationName = $routeParams.application ? $routeParams.application : "nothing";
    $scope.toolName = $routeParams.tool ? $routeParams.tool : "nothing";
    $scope.clipId = $routeParams.clip ? $routeParams.clip : "nothing";
    $scope.shareWithName = $routeParams.shareWithName;
    $scope.shareWithEmail = $routeParams.shareWithEmail;

    $scope.clip = new Clip({
        name: $scope.clipId,
        tool: $scope.toolName,
        app: $scope.applicationName
    });
  // $scope.clip.$get($scope.auth).then(function() {
  //   console.log("clip fetched");
  //   $scope.status = 'ready';
  //   $scope.$broadcast('refreshSlider');
  // });


}])

.controller('EditSliderCtrl', ['$scope',
    function($scope) {
    }]);
});

