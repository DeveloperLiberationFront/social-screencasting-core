define(['angular',
        'jquery',
        'lodash',
        'jquery.cropper',
        'ng-bootstrap',
        'ng-grid',
        'ng-ui-utils',
        'restangular'],
        function (ng, $, _) {
  /* Video player */
  return ng.module('player',
                   ['ui.bootstrap',
                    'socasterServices',
                    'restangular'])

  .controller('PlayerCtrl', ['$scope', '$interval', '$filter',
    function($scope, $interval, $filter) {
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

  .controller('RatingCtrl', ['$scope',
    function($scope) {
    }])

  .controller('FullscreenCtrl', ['$scope',
    function($scope) {
        if (!$scope.isCropping) {
            $scope.isFullscreen = !$scope.isFullscreen;
        }
    }])

  .controller('EditSliderCtrl', ['$scope',
    function($scope) {
    }]);
});
