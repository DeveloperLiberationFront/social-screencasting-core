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

  .controller('PlayerCtrl', ['$scope', '$interval', '$q',
    function($scope, $interval, $q) {

        $scope.player = {
            pos: 0,
            playing: false,
            editMode: $scope.editMode ? $scope.editMode : false,
            isCropping: false,
            isFullscreen: false,
            status: 'loading'
        };

        $scope.toggleFullscreen = function() {
          var player = $scope.player;
          if (!player.isCropping) {
            player.isFullscreen = !player.isFullscreen;
          }
        };
        $scope.playBtnImages = {
            true: 'images/playback/pause.svg',
            false:'images/playback/play.svg'
        };
        $scope.kbdOverlay = {
            mode: 0,
            enabled: $scope.clip.name.substr(-1) == 'K', //only clips ending in 'k' have keyboard info
            status: 'inactive',
            images: {
                'active': [ 'image_text.png', 'image.png', 'text.png', 'none.png' ],
                'inactive': [ 'image_text_un.png', 'image_un.png', 'text_un.png', 'none.png']
            },
            tooltip: ["Image and text", "Image only", "Text only", "No overlay"]
        };

        $scope.imagePath = function(image) {
            return $scope.clip.getRestangularUrl() + '/' + image + '?'+ $.param($scope.auth);
        };

        //load all images, and then set the status to ready
        function loadClip(clip) {
          $scope.player.status = 'loading';
          _.extend(clip, {
            start: 0,
            end: clip.frames.length-1,
          });
          $q.all(
            clip.frames.map(function(frame){
              var deferred = $q.defer()
              var img = new Image();
              $(img)
                .load(function(){ deferred.resolve(img); })
                .prop('src', $scope.imagePath(frame));
              return deferred.promise;
            })).then(function() {
              $scope.player.status = 'ready';
              $scope.$broadcast('refreshSlider');
            })
        }
        loadClip($scope.clip);

        $scope.$watch('clip', function(newValue) {
            $scope.player.pos = 0;
            loadClip($scope.clip);
            $scope.$broadcast('refreshSlider');

            if ($scope.player.isCropping) {
                $scope.player.isCropping = false;
                $("#frameLoc").cropper("disable");
                $scope.cropData.cropData = {};
            }
        });

      $scope.posChange = function() {
        if ($scope.player.isCropping) {
          //the cropper spawns two <img> tags that don't get updated automatically.
          //I tried doing the preferred $("#frameLoc").cropper("setImgSrc", $("#frameLoc").attr("src"));
          //but that was buggy and jittery
          $(".img-container")
            .find("img")
            .attr("src", $("#frameLoc").attr("ng-src"));
        }

        var active = _.any($scope.clip.event_frames, function(frame) {
          //show overlay after event, for 7 or 8 frames
          diff = $scope.pos - frame;
          return 0 < diff && diff < _.sample([7,8]); //Randomly choose between 7 or 8 frames. -Kevin
        });
        $scope.kbdOverlay.status = (active ? 'active' : 'inactive');
      };

        $scope.timer = $interval(function() {
            if ($scope.player.playing) { //playing
                $scope.player.pos = Math.max($scope.clip.start,
                   (+$scope.player.pos + 1) % ($scope.clip.end+1));   
              $scope.posChange(); //check on keyboard overlay
            }
        }, 200);

        $scope.crop = function() {
            if (!$scope.player.isCropping) {
                $scope.player.isCropping = true;
                $("#frameLoc").cropper({
                    aspectRatio: "auto",
                    done: function(data) {
                        $scope.cropData.cropData = data;
                    }
                }).cropper('enable');
            } else {
                $scope.player.isCropping = false;
                $("#frameLoc").cropper("disable");
                $scope.cropData.cropData = {};
            }
        };
    }])

  .controller('ModalPlayer', ['$scope', '$modalInstance', 'clips', 'clip_id',
    function($scope, $modalInstance, clips, clip_id) {
      //tool is a restangular object
      $scope.close = function () {
        $modalInstance.close(true);
      };

      $scope.clips = clips;
      $scope.clip = (clip_id ? _.find(clips, {name: clip_id}) : clips[0]);
      
      _.each(clips, function(clip) {
        clip.event_frames = [25]; //temporary
        _.extend(clip, {
          frame: function(name){
            return clip.getRestangularUrl() + '/' + name + '?'+ $.param($scope.auth);
          },
          thumbnail: clip.frames[Math.min(clip.event_frames[0],
                                          clip.frames.length-1)]
        });
      });

      $scope.loadClip = function(clip) {
        $scope.clip = clip;
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

  .controller('ClipThumbnailCtrl', ['$scope',
    function($scope) {
    }])

  .controller('EditSliderCtrl', ['$scope',
    function($scope) {
    }]);
});
