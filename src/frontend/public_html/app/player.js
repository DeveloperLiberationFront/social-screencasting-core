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

  .controller('PlayerCtrl', ['$scope', '$interval', '$q', 'Base64Img',
    function($scope, $interval, $q, Base64Img) {

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
                'active': [ 'image_text', 'image', 'text', 'none' ],
                'inactive': [ 'image_text_un', 'image_un', 'text_un', 'none']
            },
            tooltip: ["Image and text", "Image only", "Text only", "No overlay"]
        };

        //load all images, and then set the status to ready
      function loadClip(clip) {
        images = clip.all('images').getList()
        
        $scope.player.status = 'loading';
        
        images.then(function(images) {
          clip.images = images;
          _.extend(clip, {
            start: 0,
            end: clip.frames.length-1,
          });
          $scope.player.status = 'ready';
          $scope.$broadcast('refreshSlider');
        });
      }
        loadClip($scope.clip);

        $scope.getImage = function(name, type) {
          image = _.find($scope.clip.images, {name: name})
          if (image)
            return Base64Img(image.data,type);
        };

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

  .controller('ModalPlayer', ['$scope', '$modalInstance', 'Base64Img', 'clips', 'clip_name', 'tool',
    function($scope, $modalInstance, Base64Img, clips, clip_id, tool) {
      $scope.title = tool;
      $scope.clips = clips;
      $scope.$emit('instrumented', "Loaded ModalPlayer", $scope.clip);

      $scope.close = function () {
        $modalInstance.close(true);
        $scope.$emit('instrumented', "Closed ModalPlayer", $scope.clip);
      };

      $scope.thumbnail = function(clip) {
        return Base64Img(_.isObject(clip.thumbnail) ? clip.thumbnail.data : clip.thumbnail);
      };

      $scope.loadClip = function(clip) {
        $scope.selectedClip = $scope.clip = clip;
        $scope.selectedClip = clip;
        var user = _.find($scope.user_list, {email: clip.user});
        $scope.thumbTitle = (user.name ? user.name : $scope.title);
        $scope.$emit('instrumented', "Selected Clip", clip);
      };
      //select first clip
      $scope.loadClip(clip_id ? _.find(clips, {name: clip_name}) : clips[0]);

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
