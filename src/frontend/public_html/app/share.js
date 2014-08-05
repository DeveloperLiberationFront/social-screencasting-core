define(['jquery', 'lodash', 'controllers'], function($, _, Controllers) {
  function shareClip($scope, Local, shareWithAll) {
    var post = Local.one("shareClip");
    post.data = {
      clip_id : $scope.selection[0].clipId,
      recipient : shareWithAll? "all" : $scope.shareWithEmail,
      start_frame: $scope.clip.start,
      end_frame: $scope.clip.end,
      crop_rect: JSON.stringify($scope.cropData.cropData)
    };
    post.post();

    $scope.hasShared = true;

    note = Hub.one("notifications",$scope.respondingToNotification).get();

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
  };

  function cancelSharing($scope, Hub) {
    var reasonText = $("#no-share-reason").val();
    note = Hub.one("notifications",$scope.respondingToNotification);
    _.assign(note, {
      status:"responded",
      type:"request_denied",
      message: $interpolate(
        "Your request to {{share_with_name}} for {{application}}/{{tool}} was not fulfilled. "
          + reasonText)($scope.displayInfo)
    });
    note.put();

    $scope.dropDownStatus.isopen = false;
    $scope.cancelled = true;
    $("h2").text($interpolate(
      "Decided not to share {{application}}/{{tool}} with {{share_with_name}}"
    )($scope.displayInfo));
  };

  Controllers.controller('ShareCtrl', [
    '$scope', '$stateParams', 'Local', 'Hub', '$interpolate',
    function($scope, $routeParams, Local, Hub, $interpolate) {
      $scope.displayInfo = $routeParams;
      $scope.respondingToNotification = $routeParams.request_id;
      $scope.editMode = true;
      $scope.cropData = {cropData:{}};    //make cropData updateable by child scope (player)
      $scope.clips = [];
      $scope.isFirst = true;
      $scope.selection = [];
      $scope.ready = false;

      $scope.dropDownStatus = {
        isopen:false
      };

      //Go fetch all the clips
      Local.getList("clips").then(function(tool) {
        _.each(tool.keyclips, function(clip, i) {
          $scope.clips.push({clipId: clip, toDisplay: "Example "+(+i+1)+" using Keyboard" });
        });
        _.each(tool.guiclips, function(clip, i) {
          $scope.clips.push({clipId: clip, toDisplay: "Example "+(+i+1)+" using GUI"});
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
            toolEnd.one(clipId).get().then(function(clip){      //TODO fix for updated local API
              $scope.clip = clip;
              $scope.ready = true;
              $scope.$broadcast('refreshSlider');
            });
          }
        }
      };

      $scope.shareClip = _.curry(shareClip)($scope, Local);
      $scope.cancelSharing = _.curry(cancelSharing)($scope, Hub);
    }]);//end ShareCtrl
});//end define()
