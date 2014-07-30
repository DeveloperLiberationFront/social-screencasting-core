/*global define */

define(['angular',
        'jquery',
        'lodash',
        'ng-bootstrap',
        'ng-grid',
        'ng-ui-utils',
        'restangular',
        'services',
        'player'],
       function (ng, $, _) {
  /* Controllers */

  function ToolUsage(toolName, keypress, otherInfo) {
    //all the this.names are the same as on the server ToolStream.java
    this.Tool_Name = toolName;
    this.Tool_Key_Presses = keypress;
    this.Tool_Class = otherInfo;
    this.Tool_Timestamp = new Date().getTime();
    this.Tool_Duration = 1000;    //duration doesn't matter
  }

  function onAppLoaded($scope, callback) {
    var setHandler = function() {
      $scope.application.then(callback);
    };
    if ($scope.application) {
      setHandler();
    }
    $scope.$on('appSelected', setHandler);
  }

  function appendQueryParam(key, param) {
    var qMark = (window.location.href.indexOf("?") == -1 ? "?" : "&");

    window.location.href = window.location.href + qMark + encodeURIComponent(key)+ 
      "="+encodeURIComponent(param);
  }

  function removeQueryParam(key, param) {
    var toRemove = encodeURIComponent(key)+"="+encodeURIComponent(param);
    //removes one instance of toRemove
    window.location.href = window.location.href.replace(toRemove, "").replace(/&+/g, "&").replace("?&","?");

    //remove question mark if it's the last thing
    if (window.location.href.indexOf("?") == window.location.href.length - 1) {
      window.location.href = window.location.href.substring(0, window.location.href.length - 1);
    }
  }

  var Controllers = ng.module('socasterControllers',
                              ['ui.bootstrap',
                               'ui.format',
                               'ngGrid',
                               'restangular',
                               'socasterServices',
                               'player',
                              ]);
  Controllers
  .controller('RootCtrl', ['$scope', '$filter', '$q', 'Local', 'Hub', "$rootScope",
    function($scope, $filter, $q, Local, Hub, $rootScope) {
      var user = Local.one('user').get();
      user.then(function(user){     
        Hub.setDefaultHeaders({'Authorization': 'Basic ' + btoa(user.email + '|' + user.name + ':' + user.token)});
        $rootScope.user = user;

        return Hub.all('applications').getList();
      }).then(function(apps){
        $scope.applications = apps;
      });

      //report interface usage to Local Hub
      $scope.queuedToolUsages = [];

      $scope.$on('instrumented', function(e, event, info) {
        console.log(e);
        console.log(event);
        console.log(info);

        info = info ? info : undefined;
        $scope.queuedToolUsages.push(new ToolUsage(event, "[GUI]")); 

        var toReport = JSON.stringify($scope.queuedToolUsages);
        var oldUsages = [];
        oldUsages.concat($scope.queuedToolUsages); //copy old ones in case error contacting server
        $scope.queuedToolUsages = [];

        $.ajax("http://localhost:4443/reportTool", {
          type: "POST",
          data: { pluginName: "[ScreencastingHub]",
                  toolUsages: toReport
                },
          error: function () {
            console.error("There was a problem sending things to the server.  Most likely, the server was not up");
            //stick the previously queued elements onto the end of the current
            $scope.queuedToolUsages.concat(oldUsages);
          }
        });
      });
    }])

  .controller('MainCtrl', ['$scope',
    function($scope) {
      $scope.filters = {};
      $scope.ordering = {
        field: "video",
        reverse: true
      };
    }])

  .controller('FilterCtrl', ['$scope', '$filter',
    function($scope, $filter) {
      $scope.filterSet = { filters: [] }; //list of (tool) -> bool

      $scope.filters.toolFilter = function(tool) {
        return _.reduce($scope.filterSet.filters, function(accum, fn) {
          return accum && fn(tool);
        }, true);
      };
    }])

  .controller('OrderCtrl', ['$scope',
    function($scope) {
      $scope.ordering.options = [
        {name: "Name", field:"name"},
        {name: "Usages", field:"usages"},
        {name: "Unused", field:"unused"}, 
        {name: "Recommended", field:""}, 
        {name: "Video", field:"video"},
      ];
    }])

  .controller('UserFilterCtrl', ['$scope','$stateParams',
    function($scope, $stateParams) {
      onAppLoaded($scope, function(app) {       //previously known as onApp()
        $scope.filter.source = ng.copy(app.users);
        //update any placeholders
        console.log(app.users);
        for(var i in $scope.filter.filters) {
          var oldUser = $scope.filter.filters[i];
          if (oldUser.needsRefresh) {
            var fullUsers = _.where(app.users, {email:oldUser.email});
            if (fullUsers.length !== 0) {
              $scope.filter.filters[i] = fullUsers[0];
            }
          }
        }
      });

      $scope.filter = {
        name: 'User',
        input: null,
        source: [],
        filters: [],
        templateUrl: 'partials/user-list-item.html'
      };

      if ($stateParams.user_filter) {
        //ui-router does not turn &tool=foo&tool=bar into [foo,bar] as you might expect, but
        //treats it as if the query params were &tool=foo,bar
        var users = $stateParams.user_filter.split(",");
        for (var i in users) {
          $scope.filter.filters.push({name: users[i], email: users[i], needsRefresh:true});
        }
      }

      $scope.filterSet.filters.push(function(tool) {
        return tool.users && tool.users.length > 0 //tools must have at least one user
          && ($scope.filter.filters.length === 0 //all tools if no users selected
              || _.every($scope.filter.filters, function(user){ //or only tools with all selected users
                return _.contains(tool.users, user.email); 
              }));
      });

      $scope.removeFilter = function(filter) {
        _.pull($scope.filter.filters, filter);
        console.log(filter);
        removeQueryParam("user_filter",filter.email);
      };

      $scope.addFilter = function(input){
        if (input && !_.contains($scope.filter.filters, input)) {
          $scope.filter.filters.push(input);
          console.log(input);
          appendQueryParam("user_filter",input.email);
        }
        $scope.filter.input = null;
      };
    }])

  .controller('ToolFilterCtrl', ['$scope','$stateParams',
    function($scope, $stateParams) {
      onAppLoaded($scope, function(app) {
        $scope.filter.source = _.filter(ng.copy(app.tools), function(tool) {
          return tool.users.length > 0;
        });
      });

      $scope.filter = {
        name: 'Tool',
        input: null,
        source: [],
        filters: [],
        templateUrl: ''
      };
      
      if ($stateParams.tool_filter) {
        //ui-router does not turn &tool=foo&tool=bar into [foo,bar] as you might expect, but
        //treats it as if the query params were &tool=foo,bar
        var tools = $stateParams.tool_filter.split(",");
        for (var i in tools) {
          $scope.filter.filters.push({name: tools[i]});
        }
      }

      $scope.filterSet.filters.push(function(tool) {
        return $scope.filter.filters.length === 0 ||
          _.any($scope.filter.filters, {name: tool.name});
      });

      $scope.removeFilter = function(filter) {
        _.pull($scope.filter.filters, filter);
        removeQueryParam("tool_filter",filter.name);
      };

      $scope.addFilter = function(input){
        if (input && !_.contains($scope.filter.filters, input)) {
          $scope.filter.filters.push(input);
          appendQueryParam("tool_filter",input.name);
        }
        $scope.filter.input = null;
      };
    }])

  .controller('ToolListCtrl', ['$scope','Hub',
    function($scope, Hub) {
      $scope.user.usages = Hub.all('usages').getList({
        'where': {'user': $scope.user.email},
      });
      $scope.tools = Hub.all('tools').getList();
      
      $scope.limit = 10;
      $scope.scroll = function() {
        $scope.limit += 1;
      };
    }])

  .controller('ToolBlockCtrl', ['$scope', '$state',
    function($scope, $state) {
      $scope.userVideo = function(user) {
        var self = (user.email == $scope.user.email);
        var origin = (self ? 'local' : 'external');
        if (user.video || self) {
          $state.go('main.video', {
            location: origin,
            owner: user.email,
            tool: $scope.tool._id
          });
        } else {
          $state.go('main.request', {
            owner: user.email,
            tool: $scope.tool._id
          });
        }
      };
    }]);

  function prepareMessagesForSentRequests(sent) {
    $interpolate = angular.injector(['ng']).get('$interpolate');
    _.each(sent, function(sentItem) {
      if (sentItem.type == "request_fulfilled") {
        var json = JSON.parse(sentItem.status);
        sentItem.shared_videos = _.map(json.video_id, function(id){
          return $interpolate(
            "#/video/external/{{recipient.email}}/"
              + "{{application}}/{{tool}}/" + id
          )(sentItem);
        });
        sentItem.message = $interpolate(
          "{{recipient.name}} granted access to {{application}}/{{tool}}"
        )(sentItem);
      }
      else {
        sentItem.message = $interpolate(
          "Requested access to {{recipient.name}}'s usage of {{application}}/{{tool}}"
        )(sentItem);
      }
    });
  }

  Controllers
  .controller('StatusCtrl', ['$scope', 'Hub', '$interpolate',
    function($scope, Hub, $interpolate) {
      $scope.$emit('instrumented', "View Status");
      
      $scope.received = [{}];
      $scope.sent = [{}];
      
      Hub.all("notifications").withHttpConfig({cache:false}).getList({
        'embedded': {'recipient': 1} //pull in recipient details instead of just id
      }).then(function(notifications) {
        $scope.sent = _.where(notifications, {sender: $scope.user.email});
        $scope.received = _(notifications)
          .where({recipient: $scope.user.email})
          .reject(function(item) { //user doesn't need to see requests they have responded to
            return item.type == "request_fulfilled";
          });
        
        prepareMessagesForSentRequests($scope.sent);
        
        _.each($scope.received, function(item) {
          if (item.status == "new") {
            item.status = "seen";
            item.put($scope.auth);
            console.log(item);
          }
        });
      });
      
      $scope.deleteRequest = function(request) {
        $scope.sent = _.reject($scope.sent, function(item) {
          return item._id == request._id;
        });
        request.remove();
      };
      
      $scope.getBadgeText = function(request) {
        if (_.contains(["new", "seen"], request.status)) {
          return request.status;
        }
        return undefined;
      };
        
      $scope.getShareLink = function(request) {
        if ("share_request" == request.type) {
          return $interpolate(
            "#/share/{{application}}/{{tool}}"
              + "?share_with_name={{sender.name}}"
              + "&share_with_email={{sender.email}}"
              + "&request_id={{_id}}"
          )(request);
        }
        return "#";
      };
  }])

.controller('RequestCtrl', ['$scope', '$modalInstance', '$stateParams', 'Hub',
  function($scope, $modalInstance, $stateParams, Hub) {
    $scope.request = function(user) {
      Hub.all('notifications').post({
        application: $stateParams.application,
        tool: $stateParams.tool._id,
        recipient: $stateParams.owner,
        type: "share_request"
      });
      $modalInstance.close();
    };

    $scope.cancel = $modalInstance.close;
  }])

  .controller('ShareDropDownCtrl', ['$scope',
    function($scope) {
      $('.btn-group').on('click', '.dropdown-menu', function(event){
        //stop propagation to prevent clicking in the text area from closing the dropdown
        event.stopPropagation();
        event.preventDefault();
      });

      $scope.toggleDropdown = function($event) {
        //todo: perhaps don't need to stop propagation
        $event.preventDefault();
        $event.stopPropagation();
        $scope.dropDownStatus.isopen = !$scope.dropDownStatus.isopen;
      };
    }]) 

  .controller('ShareCtrl', ['$scope', '$stateParams', 'Local', 'Hub',
    function($scope, $routeParams, Local, Hub) {
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
      
      var toolEnd = Local.one($scope.user.email)
        .one($routeParams.application)
        .one($routeParams.tool);

      //Go fetch all the clips
      toolEnd.get().then(function(tool) {
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
            toolEnd.one(clipId).get().then(function(clip){
              $scope.clip = clip;
              $scope.ready = true;
              $scope.$broadcast('refreshSlider');
            });
          }
        }
      };

      $scope.shareClip = function(shareWithAll) {
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

        var put = Hub.one("notifications").one(""+$scope.respondingToNotification);

        put.get($scope.auth).then(function(notification){
          if (notification.type == "request_fulfilled") {
            //add this shared video to the list
            var json = JSON.parse(notification.status);
            json.video_id.push($scope.selection[0].clipId);
            put.notification = {status: JSON.stringify(json)};
            console.log("putting to array");
            put.put($scope.auth);
          }
          else {
            //mark this notification as responded, and make the status a hash with an array of clip ids
            put.notification = {status:JSON.stringify({video_id:[$scope.selection[0].clipId]}), type:"request_fulfilled"};
            console.log("putting first video id");
            put.put($scope.auth);
          }
        });

      };

      $scope.toggled = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        console.log("toggling ");
        console.log($event);
        return false;
      };

      $scope.cancelSharing = function() {
        console.log("cancelSharing");

        var reasonText = $("#no-share-reason").val();
        var put = Hub.one("notifications").one(""+$scope.respondingToNotification);
        put.notification = {status:"responded" , type:"request_denied", message:"Your request to " + $scope.shareWithName +" for " +
                            $scope.applicationName+"/"+$scope.toolName +" was not fulfilled. " + reasonText};
        put.put($scope.auth);

        console.log(reasonText);
        $scope.dropDownStatus.isopen = false;

        $scope.cancelled = true;
        $("h2").text("Decided not to share " + $scope.applicationName+"/"+$scope.toolName +" with " + $scope.shareWithName);
      };
    }]);
  });
