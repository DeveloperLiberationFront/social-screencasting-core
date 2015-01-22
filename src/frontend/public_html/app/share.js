/*global define*/

define(['jquery', 'lodash', 'controllers'], function($, _, Controllers) {
  _.templateSettings.interpolate = /{{([\s\S]+?)}}/g;

  function shareClip($scope, Local, Hub, shareWithAll) {

    var post = Local.one("shareClip");
    post.data = {
      clip_id : $scope.selection[0].id,
      recipient : shareWithAll? "all" : $scope.display.info.share_with_email,
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
        var json = JSON.parse(notification.notification_status);
        json.video_id.push($scope.selection[0].id);
        notification.notification_status = JSON.stringify(json);
        notification.put();
      }
      else {
        //mark this notification as responded, and make the status a hash with an array of clip ids
        _.assign(notification, {
          notification_status: JSON.stringify({video_id:[$scope.selection[0].id]}),
          type: "request_fulfilled"
        });
        notification.put().then(function(e) {
          console.log("Object saved OK");
          console.log(e);
        }, function(e,e1) {
          console.log("There was an error saving");
          console.log(e);
          console.log(e1);
        });
      }
    });
  }

  function cancelSharing($scope, Hub) { 
    console.log("Cancelling sharing");
    var reasonText = $("#no-share-reason").val();

    $scope.$emit('instrumented', "NOT Sharing a Clip", reasonText);

    Hub.one("notifications",$scope.respondingToNotification).get()
      .then(function(note){
        _.assign(note, {
          notification_status:"responded",
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
      $scope.display = {info: $stateParams};
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
          $scope.clips.push(_.assign(clip, {
            id : clip.name,
            toDisplay: "Example "+(+i+1)+" using " + type
          }
          ));
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
            $scope.clip = c[0];
            $scope.ready = true;
            $scope.$broadcast('refreshSlider');
          }
        }
      };

      $scope.shareClip = _.curry(shareClip)($scope, Local, Hub);
      $scope.cancelSharing = _.partial(cancelSharing, $scope, Hub);
    }]);//end ShareCtrl
});//end define()
