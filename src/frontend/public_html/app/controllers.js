/*global define */

define(['angular',
        'jquery',
        'lodash',
        'ng-bootstrap',
        'ng-grid',
        'ng-ui-utils',
        'restangular',
        'services',
        'player'], function (ng, $, _) {
  /* Controllers */

  function ToolUsage(toolName, keypress, otherInfo) {
    //all the this.names are the same as on the server ToolStream.java
    this.Tool_Name = toolName;
    this.Tool_Key_Presses = keypress;
    this.Tool_Class = otherInfo;
    this.Tool_Timestamp = new Date().getTime();
    this.Tool_Duration = 1000;    //duration doesn't matter
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
  .controller('RootCtrl', ['$scope', '$filter', 'Hub', 'User',
    function($scope, $filter, Hub, User) {
      $scope.user = User;
      Hub.all('applications').getList().then(function(apps){
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

  .controller('MainCtrl', ['$scope', 'Hub',
    function($scope, Hub) {
      $scope.filters = {};
      $scope.ordering = {
        field: "video",
        reverse: true
      };
      $scope.user_list = Hub.all('users').getList();
    }])

  .controller('ToolListCtrl', ['$scope','Hub',
    function($scope, Hub) {
      $scope.user.usages = Hub.all('usages').getList({
        'where': {'user': $scope.user.email},
      });
      Hub.all('tools').getList().then(function(tools) {
        $scope.tools = tools;
        _.each(tools, function(tool) {
          Hub.all('usages').getList({
            where: {user: $scope.user.email, tool: tool._id}
          }).then(function(usages) {
            tool.usages = usages;
            tool.users = _.pluck(usages, 'user');
          });
        });
      });
      
      $scope.scroll = function() {
        //todo: get more tools...
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

  .controller('UserFilterCtrl', ['$scope','$state', '$stateParams',
    function($scope, $state, $stateParams) {
      $scope.filter = {
        name: 'User',
        input: null,
        source: [],
        filters: [],
        templateUrl: 'partials/user-list-item.html'
      };

      $scope.user_list.then(function(user_list) {
        $scope.filter.source = ng.copy(user_list);

        if ($stateParams.user_filter) {
          //ui-router does not turn &tool=foo&tool=bar into [foo,bar] as you might expect, but
          //treats it as if the query params were &tool=foo,bar
          var users = $stateParams.user_filter.split(",");
          _.each(users, function(email) {
            name = _.find(user_list, {email: email}).name;
            $scope.filter.filters.push({name: name, email: email});
          });
        }
      });

      $scope.filterSet.filters.push(function(tool) {
        return tool.users && tool.users.length > 0 //tools must have at least one user
          && ($scope.filter.filters.length === 0 //all tools if no users selected
              || _.every($scope.filter.filters, function(user){ //or only tools with all selected users
                return _.contains(tool.users, user.email); 
              }));
      });

      $scope.removeFilter = function(filter) {
        $state.go('main', {
          user_filter: _.without($state.params.user_filter.split(','), filter.email)
        })
      };

      $scope.addFilter = function(input){
        $state.go('main', {
          user_filter: _.union($stateParams.user_filter.split(','), [input.email])
        });
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
    }]);

    return Controllers;
 });
