define(['angular',
        'jquery',
        'underscore',
        'jquery.cropper',
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
            $scope.auth = _.pick(user, 'name', 'email', 'token');
        });
        $scope.applications = Application.query(function() {
            $scope.$broadcast('appSelected', $scope.applications[0]);
        });
        $scope.$on('appSelected', function(event, app) {
            app.$promise = app.$get(_.pick($scope.user, 'name', 'email', 'token'));
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

  .controller('LocalToolsCtrl', ['$scope', 'User',
    function($scope, User) {
        $scope.selection = [];
        $scope.$on('appSelected', function(event, app) {
            $scope.data = User.get(function(user) {
                $scope.tools = user.applications[app.name];
            });
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

  .controller('ApplicationToolsCtrl', ['$scope', 'Application', '$modal', 'Tool',
    function($scope, Application, $modal, Tool) {
        $scope.selection = [];
        $scope.$on('appSelected', function(event, app) {
            app.$promise.then(function(){
                $scope.tools = app.tools.map(function(o){
                    return new Tool(_.extend(o, {app:$scope.application.name}));
                });
            });
        });
        $scope.gridOptions = {
            data: 'tools',
            selectedItems: $scope.selection,
            multiSelect: false,
            columnDefs: [
                {field:'name', displayName:'Name'},
                {
                    width: '50px',
                    field:'video', displayName:'Video',
                    cellTemplate:  "<div class='ngCellText'><img src='images/video_icon_tiny.png' ng-show='row.getProperty(col.field)'/></div>",
                    sortFn: function(x,y){return (x === y)? 0 : x? 1 : -1;},
                },
            ],
            sortInfo: {
                fields: ['video'],
                directions: ['desc'],
            },
            afterSelectionChange: function() {
                s = $scope.selection;
                if (s.length > 0 && s[0].video) {
                    $scope.tool = s[0];
                    $scope.tool.$promise = $scope.tool.$get($scope.auth);
                    $modal.open({
                        templateUrl: 'partials/modal-player.html',
                        controller: 'ModalPlayer',
                        scope: $scope,
                        windowClass: 'modal-player',
                        size: 'lg',
                    });
                }
            }, 
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
            editMode: true,
            isCropping: false
        };
        $scope.isFullscreen = false;
        $scope.toggleFullscreen = function() {
            if (!$scope.player.isCropping) {
                $scope.isFullscreen = !$scope.isFullscreen
            }
        }
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
            tooltip: ["Image and text", "Image only", "Text only", "No overlay"],
        };

        $scope.imgDir = $filter('format')(
            'http://screencaster-hub.appspot.com/api/:creator/:app/:tool/:name/',
            $scope.clip
        )

        $scope.imagePath = function(image) {
            return $scope.imgDir + image +'?'+ $.param($scope.auth);
        }

        _.extend($scope.clip, {
            loaded: $scope.clip.frames.map(function(frame){
                img = new Image();
                img.src = $scope.imagePath(frame);
                return img;
            }),
            keyboardEventFrame: 25,
            start: 0,
            end: $scope.clip.frames.length-1
        })

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
        }
    }])

  .controller('ModalPlayer', ['$scope', '$modalInstance', 'Clip',
    function($scope, $modalInstance, Clip) {
        $scope.status = 'loading';
        $scope.close = function () {
            $modalInstance.close();
        };

        $scope.tool.$promise.then(function() {
            if ($scope.tool.clips.length > 0) {
                $scope.clip = new Clip(_.extend($scope.tool.clips[0], {
                    tool: $scope.tool.name,
                    app: $scope.application,
                }));
                $scope.clip.$get($scope.auth).then(function() {
                    $scope.status = 'ready';
                    $scope.$broadcast('refreshSlider');
                })
            }
        });
    }])

  .controller('PlaybackSliderCtrl', ['$scope',
    function($scope) {
    }])

  .controller('EditSliderCtrl', ['$scope',
    function($scope) {
    }]);
});
