/*global define*/

define(['jquery', 'lodash', 'controllers'], function($, _, Controllers) {
  _.templateSettings.interpolate = /{{([\s\S]+?)}}/g;

  function shareClip($scope, Local, Hub, shareWithAll) {

    var post = Local.one("shareClip");
    post.data = {
      clip_id : $scope.selection[0].clipId,
      recipient : shareWithAll? "all" : $scope.shareWithEmail,
      start_frame: $scope.clip.start,
      end_frame: $scope.clip.end,
      crop_rect: JSON.stringify($scope.cropData.cropData)
    };

    $scope.$emit('instrumented', "Sharing a Clip", post.data);
    post.post();

    $scope.hasShared = true;

    var note = Hub.one("notifications",$scope.respondingToNotification).get();

    note.then(function(notification){
      if (notification.type == "request_fulfilled") {
        //add this shared video to the list
        var json = JSON.parse(notification.status);
        json.video_id.push($scope.selection[0].clipId);
        notification.status = JSON.stringify(json);
        notification.put();
      }
      else {
        //mark this notification as responded, and make the status a hash with an array of clip ids
        _.assign(notification, {
          status: JSON.stringify({video_id:[$scope.selection[0].clipId]}),
          type: "request_fulfilled"
        });
        notification.put();
      }
    });
  }

  function cancelSharing($scope, Hub) {
    var reasonText = $("#no-share-reason").val();

    $scope.$emit('instrumented', "NOT Sharing a Clip", reasonText);

    Hub.one("notifications",$scope.respondingToNotification).get()
      .then(function(note){
        _.assign(note, {
          status:"responded",
          type:"request_denied",
          message: _.template(
            "Your request to {{share_with_name}} for {{application}}/{{tool}} was not fulfilled. " + reasonText,
            $scope.displayInfo)
        });
        note.put();
    });

    $scope.dropDownStatus.isopen = false;
    $scope.cancelled = true;
    $("h2").text(_.template(
      "Decided not to share {{application}}/{{tool}} with {{share_with_name}}",
      $scope.displayInfo));
  }

  Controllers.controller('ShareCtrl', [
    '$scope', '$stateParams', 'Local', 'Hub',
    function($scope, $stateParams, Local, Hub) {
      $scope.displayInfo = $stateParams;
      $scope.respondingToNotification = $stateParams.request_id;
      $scope.editMode = true;
      $scope.cropData = {cropData:{}};    //make cropData updateable by child scope (player)
      $scope.clips = [];
      $scope.isFirst = true;
      $scope.selection = [];
      $scope.ready = false;
      $scope.cancelled = false;
      $scope.hasShared = false;

      $scope.$emit('instrumented', "Share Clip UI Opened", {toolInfo:$scope.displayInfo, respondingToNotification: $stateParams.request_id});

      $scope.dropDownStatus = {
        isopen:false
      };
      
      $stateParams.app = $stateParams.application;

      //Go fetch all the clips
      Local.all("clips").getList($stateParams).then(function(clips) {
        _.each(clips, function(clip, i) {
          var type;
          if (clip.name.slice(-1) == 'K') {
            type = "Keyboard";
          } else {
            type = "GUI";
          }
          $scope.clips.push({
            clipId: clip,
            toDisplay: "Example "+(+i+1)+" using " + type
          });
        });
      });

      $scope.shareGridOptions = {       //set up the grid to display
        selectedItems: $scope.selection,
        multiSelect: false,
        data: "clips",   //this is a string of the name of the obj in the $scope that has data
        columnDefs: [{ field:'toDisplay'}],
        headerRowHeight:0,    //hide the header
        afterSelectionChange: function() {
          var c = $scope.selection;

          if (c.length > 0) {
            var clipId = c[0].clipId;
            Local.one(clipId).get().then(function(clip){      //TODO fix for updated local API
              $scope.clip = clip;
              $scope.ready = true;
              $scope.$broadcast('refreshSlider');
            });
          }
        }
      };

      $scope.shareClip = _.curry(shareClip)($scope, Local, Hub);
      $scope.cancelSharing = _.curry(cancelSharing)($scope, Hub);
    }]);//end ShareCtrl
});//end define()
