/*global define*/

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
            $scope.$emit('instrumented', "Toggling Fullscreen", !player.isFullscreen);
            player.isFullscreen = !player.isFullscreen;
          }
        };
        $scope.togglePlaying = function() {
          var player = $scope.player;
          player.playing = !player.playing;
          $scope.$emit('instrumented', player.playing ? "Playing Clip" : "Pausing Clip", $scope.clip);
          
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

        $scope.toggleKeyboardOverlay = function() {
          var kbdOverlay = $scope.kbdOverlay;

          kbdOverlay.mode = (kbdOverlay.mode + 1) % 4;
          $scope.$emit('instrumented', "Changing Keyboard Overlay", kbdOverlay.tooltip[kbdOverlay]);
        };

        

        //load all images, and then set the status to ready
      function loadClip(clip) {
        // if (clip.images !== undefined && clip.images.length !== undefined && clip.images.length > 0) {
        //   $scope.player.status = 'ready';
        //   $scope.$broadcast('refreshSlider');
        //   return;   //we have already loaded the images
        // }
        var images = clip.all('images').getList();
        
        $scope.player.status = 'loading';
        
        images.then(function(images) {
          clip.images = images; //these include frames and animation images.

          _.extend(clip, {
            start: 0,
            end: clip.frames.length-1,
          });
          $scope.player.status = 'ready';
          $scope.$broadcast('refreshSlider');
        }, //error handling
        function(stuff)  { console.log(stuff);}, function(stuff)  { console.log(stuff);});
      }
        loadClip($scope.clip);

        $scope.getImage = function(name, type) {
          var image = _.find($scope.clip.images, {name: name});
          if (image) {
            return Base64Img(image.data,type);
          } else {
            console.log("Couldn't find "+name);
          }
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
          var diff = $scope.player.pos - frame;
          return 0 < diff && diff < 8;
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
                $scope.$emit('instrumented', "Entering Crop Mode");
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
                $scope.$emit('instrumented', "Exiting Crop Mode");
            }
        };
    }])

  .controller('ModalPlayer', ['$scope', '$modalInstance', 'Base64Img', 'Hub',
                              'clips', 'clip_name', 'tool',
    function($scope, $modalInstance, Base64Img, Hub, clips, clip_id, tool) {
      $scope.clips = clips;
      $scope.$emit('instrumented', "Loaded ModalPlayer", $scope.clip);

      $scope.close = function () {
        $modalInstance.close(true);
        $scope.$emit('instrumented', "Closed ModalPlayer", $scope.clip);
      };

      $scope.thumbnail = function(clip) {
        return Base64Img(_.isObject(clip.thumbnail) ? clip.thumbnail.data : clip.thumbnail);
      };

      var users = Hub.all('users').getList();
      $scope.loadClip = function(clip) {
        $scope.selectedClip = $scope.clip = clip;
        $scope.$emit('instrumented', "Selected Clip", clip);
        users.then(function(users) {
          var user = _.find(users, {email: $scope.clip.user});
          $scope.title = tool + " - " + (user.name ? user.name : $scope.title);
        });
      };
      //select first clip
      $scope.loadClip(clip_id ? _.find(clips, {id: clip_id}) : clips[0]);

    }])

  .controller('RatingCtrl', function() {

    })

  .controller('ClipThumbnailCtrl', function() {

    })

  .controller('EditSliderCtrl', function() {

    });
});
