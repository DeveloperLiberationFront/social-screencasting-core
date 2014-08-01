/*global define, angular */

define(['angular',
        'jquery',
        'lodash',
        'bluebird',
        'ng-bootstrap',
        'ng-grid',
        'ng-ui-utils',
        'restangular',
        'services',
        'player'], function (ng, $, _, Promise) {
  /* Controllers */

  function ToolUsage(toolName, keypress, otherInfo) {
    //all the this.names are the same as on the server ToolStream.java
    this.Tool_Name = toolName;
    this.Tool_Key_Presses = keypress;
    this.Tool_Class = otherInfo;
    this.Tool_Timestamp = new Date().getTime();
    this.Tool_Duration = 1000;    //duration doesn't matter
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
      $scope.applications = Hub.all('applications').getList();

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
      $scope.tools = Hub.all('tools').getList();
    }])

  .controller('ToolListCtrl', ['$scope','Hub',
    function($scope, Hub) {
      $scope.user.usages = Hub.all('usages').getList({
        'where': {'user': $scope.user.email},
      });

      Promise.all([$scope.tools, $scope.user_list]).spread(function(tools, users) {
        _.each(tools, function(tool) {
          Hub.all('usages').getList({
            where: {tool: tool._id}
          }).then(function(usages) {
            tool.usages = usages;
            tool.users = _.where(users, function(u) {
              return _.contains(_.pluck(usages, 'user'), u.email);
            });
          });
          tool.clips = Hub.all('clips').getList({
            where: {tool: tool._id}
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
            if (email) {
              var name = _.find(user_list, {email: email}).name;
              $scope.filter.filters.push({name: name, email: email});
            }
          });
        }
      });

      $scope.filterSet.filters.push(function(tool) {
        return tool.users && ($scope.filter.filters.length === 0 //all tools if no users selected
              || _.every($scope.filter.filters, function(user){ //or only tools with all selected users
                return _.find(tool.users, {email: user.email}); 
              }));
      });

      $scope.removeFilter = function(filter) {
        $state.go('main', {
          user_filter: _.without($state.params.user_filter.split(','), filter.email)
        });
      };

      $scope.addFilter = function(input){
        var user_filter = ($stateParams.user_filter ?
                           _.union($stateParams.user_filter.split(','), [input.email])
                           : [input.email]);
        $state.go('main', {
          user_filter: user_filter
        });
        $scope.filter.input = null;
      };
    }])

  .controller('ToolFilterCtrl', ['$scope','$state','$stateParams',
    function($scope, $state, $stateParams) {
      $scope.tools.then(function(tools) {
        $scope.filter.source = ng.copy(tools);
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
        _.each(tools, function(tool) {
          if (tool) {
            $scope.filter.filters.push({name: tool});
          }
        });
      }

      $scope.filterSet.filters.push(function(tool) {
        return $scope.filter.filters.length === 0 ||
          _.any($scope.filter.filters, {name: tool.name});
      });

      $scope.removeFilter = function(filter) {
        $state.go('main', {
          tool_filter: _.without($state.params.tool_filter.split(','), filter.name, "")
        });
      };

      $scope.addFilter = function(input){
        var tool_filter = ($stateParams.tool_filter ?
                           _.union($stateParams.tool_filter.split(','), [input.name])
                           : [input.name]);
        $state.go('main', {
          tool_filter: tool_filter
        });
        $scope.filter.input = null;
      };
    }])

  .controller('ApplicationFilterCtrl', ['$scope', '$state',
    function($scope, $state) {
      $scope.filter = {
        name: 'Application',
        input: null,
        source: [],
        apps: {},
        templateUrl: ''
      };

      $scope.applications.then(function(apps) {
        $scope.filter.source = _.pluck(apps, 'name');
      });

      if ($state.params.app_filter) {
        var apps = $state.params.app_filter.split(',');
        //creates an object like {active_app_1:true, active_app_2:true ...}
        $scope.filter.apps = _.zipObject(apps, _.times(apps.length, function(){return true;}));
      }

      $scope.filterSet.filters.push(function(tool) {
        return !_.any($scope.filter.apps) //unchecking all apps shows all tools
          || $scope.filter.apps[tool.application];
      });

      $scope.updateFilters = function() {
        $state.go('main', {
          app_filter: _.keys(_.pick($scope.filter.apps, function(v){return v;}))
        });
      };
    }])

  .controller('MiscFilterCtrl', ['$scope', '$state',
    function($scope, $state) {

      $scope.filter = {
        name: 'Misc Filters',
        input: null,
        source: [{
          text: "I haven't used yet",
          id:"not_used"
        },
        { text: "have a screencast",
          id:"yes_video"
        },
        ],
        active_filters: {},
        templateUrl: ''
      };

      if ($state.params.misc_filter) {
        var misc_filters = $state.params.misc_filter.split(',');
        //creates an object like {active_app_1:true, active_app_2:true ...}
        $scope.filter.active_filters = _.zipObject(misc_filters, _.times(misc_filters.length, function(){return true;}));
      }

      $scope.filterSet.filters.push(function(tool) {
        return !_.any($scope.filter.active_filters) || //no filters shows all apps
          //or
          ($scope.filter.active_filters.not_used && !_.find(tool.users, {email : $scope.user.email})) ||
          //or
          ($scope.filter.active_filters.yes_video && tool.clips.$object.length > 0);

      });

      $scope.updateFilters = function() {
        $state.go('main', {
          misc_filter: _.keys(_.pick($scope.filter.active_filters, function(v){return v;}))
        });
      };

    }])

  .controller('ToolBlockCtrl', ['$scope', '$state',
    function($scope, $state) {
      $scope.hasVideo = function(user) {
        return _.find($scope.tool.clips.$object, {user: user.email});
      };

      $scope.getVideo = function(user) {
        var self = (user == $scope.user.email);
        var origin = (self ? 'local' : 'external');
        if (user.video || self) {
          $state.go('main.video', {
            location: origin,
            owner: user,
            tool: $scope.tool._id
          });
        } else {
          $state.go('main.request', {
            owner: user,
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
