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

  var fieldDefs = {
      'unused': {
          width: '50px',
          field:'unused', displayName:'Unused',
          cellTemplate: "<div class='ngCellText'><span class='glyphicon glyphicon-ok' ng-show='row.getProperty(col.field)'></span></div>"
      },
      'video': {
          width: '50px',
          field:'video', displayName:'Video',
          cellTemplate:  "<div class='ngCellText'><img src='images/video_icon_tiny.png' ng-show='row.getProperty(col.field)'/></div>",
          sortFn: function(x,y){return (x === y)? 0 : x? 1 : -1;}
      }
  };

  function ToolUsage(toolName, keypress, otherInfo) {
    //all the this.names are the same as on the server ToolStream.java
    this.Tool_Name = toolName;
    this.Tool_Key_Presses = keypress;
    this.Tool_Class = otherInfo;
    this.Tool_Timestamp = new Date().getTime();
    this.Tool_Duration = 1000;    //duration doesn't matter
  }

  function onApp($scope, callback) {
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

  return ng.module('socasterControllers',
                   ['ui.bootstrap',
                    'ui.format',
                    'ngGrid',
                    'restangular',
                    'socasterServices',
                    'player',
                   ])

  .controller('NavCtrl', ['$scope', '$filter', '$q', 'Local', 'Hub', "$rootScope",
    function($scope, $filter, $q, Local, Hub, $rootScope) {
      //$rootScope.auth = 
      var deferredAuth = $q.defer();
      $rootScope.preAuth = deferredAuth.promise;

        Local.one('user').get().then(function(user){     
            $rootScope.user = user;
            $rootScope.auth = user.plain();
            //allows us to use auth in routes, e.g. modal player
            deferredAuth.resolve(user.plain());
        });
        Hub.one('details').getList().then(function(apps){
            $scope.applications = apps;
            $scope.$broadcast('appSelected', apps[0]);
        });
        $scope.$on('appSelected', function(event, app) {
            var userTools = Local.one('user').one(app.name).get().get('tools');
            $scope.application = Hub.one('details').one(app.name).get($scope.auth);
            $scope.application.then(function(app) {
              _.each(app.users, function(user) {
                Hub.one(user.email).one(app.name).get($scope.auth).then(function(data) {
                  _.extend(user, data);
                });
              });
            });
            $q.all({userTools: userTools, app: $scope.application})
                .then(function(results){
                    _.each(results.app.tools, function(tool) {
                        tool.unused = _.findWhere(results.userTools, {name: tool.name}) === undefined;
                    });
                });
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

  .controller('UserListCtrl', ['$scope',
    function($scope) {
        $scope.gridOptions = {
            data: 'application.$object.users',
            multiSelect: false,
            columnDefs: [{field:'name', displayName:'Name'},
                         {field:'email', displayName:'Email'}]
        };
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
      onApp($scope, function(app) {
          $scope.filter.source = ng.copy(app.users);
        });

      console.log($stateParams);
      
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
          $scope.filter.filters.push({email: users[i]});
        }
      }

      $scope.filterSet.filters.push(function(tool) {
        return tool.users.length > 0 //tools must have at least one user
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
          appendQueryParam("user_filter",input.email);
        }
        $scope.filter.input = null;
      };
    }])

  .controller('ToolFilterCtrl', ['$scope','$stateParams',
    function($scope, $stateParams) {
      onApp($scope, function(app) {
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

  .controller('ToolListCtrl', ['$scope',
    function($scope) {
      onApp($scope, function(app) {
          $scope.tools = app.tools;
        });

      $scope.limit = 10;
      $scope.scroll = function() {
        $scope.limit += 1;
      };
    }])

  .controller('ToolBlockCtrl', ['$scope', '$state',
    function($scope, $state) {
      onApp($scope, function(app) {
        var unused = $scope.tool.unused;
        app.one($scope.tool.name).get($scope.auth).then(function(tool) {
          $scope.tool = _.extend(tool, {unused: unused});
        });
      });

      $scope.details = function(user) {
        return _.find(user.tools, {name: $scope.tool.name});
      };

      $scope.userVideo = function(user) {
        var self = (user.email == $scope.user.email);
        var origin = (self ? 'local' : 'external');
        if (user.video || self) {
          $state.go('main.video', {
            location: origin,
            owner: user.email,
            application: $scope.application.$object.name,
            tool: $scope.tool.name
          });
        } else {
          $state.go('main.request', {
            owner: user.email,
            application: $scope.application.$object.name,
            tool: $scope.tool.name
          });
        }
      };
    }])

  .controller('DropdownCtrl', ['$scope', '$rootScope',
    function($scope, $rootScope) {
      $scope.status = {
          isopen: false
      };
      $scope.setApp = function(app) {
        $scope.status.isopen = false;
        $rootScope.$broadcast('appSelected', app);
        $scope.$emit('instrumented', 'Changed Plugin', app);
      };
    }])

  .controller('StatusCtrl', ['$scope', 'Hub', '$http', function($scope, Hub) {
    
    $scope.$emit('instrumented', "View Status");

    $scope.received = [{}];
    $scope.sent = [{}];

    Hub.one("notifications").withHttpConfig({cache:false}).get($scope.auth).then(function(data){
      console.log(data.plain());
      $scope.sent = data.sent;
      $scope.received = _.filter(data.received, function(item) { 
          return item.type != "request_fulfilled";

      });

      for (var i = $scope.sent.length - 1; i >= 0; i--) {
        var sentItem = $scope.sent[i];
        if (sentItem.type == "request_fulfilled") {
          var json = JSON.parse(sentItem.status);
          sentItem.shared_videos = _.map(json.video_id, function(id){
            console.log(id);
            return "#/video/external/"+sentItem.recipient.email+"/"+sentItem.plugin+"/"+sentItem.tool+"/"+id;
          });
          console.log(sentItem.shared_videos);
          sentItem.message = sentItem.recipient.name+ " granted access to "+ sentItem.plugin +"/" + sentItem.tool;
        }
        else {
          sentItem.message =  "Requested access to "+sentItem.recipient.name+ "'s usage of "+ sentItem.plugin +"/" + sentItem.tool;
        }
      }

      for (i = $scope.received.length - 1; i >= 0; i--) {
        var item = $scope.received[i], put;
        if (item.status == "new") {
          //item.status = "seen";
          put = Hub.one("notifications").one(""+item.id);
          put.notification = {status:"seen"};
          put.put($scope.auth);
        } 
      }
    });

    $scope.deleteRequest = function(request) {
      console.log("deleting "+request);
        $scope.sent = _.reject($scope.sent, function(item) {
          return item.id == request.id;
        });

        Hub.one("notifications").one(""+request.id).remove($scope.auth);
    };


    $scope.getBadgeText = function(request) {
      if (request.status == "new") {
        return "new";
      } else if (request.status == "seen") {
        return "seen";
      } 

      else {
        return undefined;
      }
    };

  $scope.getRequestLink = function(request) {
    if ("request_fulfilled" == request.type) {
      var json = JSON.parse(request.status);
      if (json.video_id) {
        return "#/video/external/"+request.recipient.email+"/"+request.plugin+"/"+request.tool+"/"+json.video_id;
      }
      return undefined;
    }
    else {
      return undefined;
    }
  };

  $scope.getShareLink = function(request) {
    if ("share_request" == request.type) {
      return "#/share/"+request.plugin+"/"+request.tool+"?share_with_name="+
      request.sender.name+"&share_with_email="+request.sender.email+"&request_id="+request.id;
    }
    else {
      return "/#";
    }
  };

}])

.controller('ToolCtrl', ['$scope', '$stateParams',
  function($scope, $stateParams) {
    onApp($scope, function(app) {
      $scope.tool = app.one($stateParams.name).get($scope.auth);
    });
  }])

.controller('RequestCtrl', ['$scope', '$modalInstance', '$stateParams', 'Hub',
  function($scope, $modalInstance, $stateParams, Hub) {
    $scope.request = function(user) {
      _.extend(Hub.one('request-share'), {
        plugin: $stateParams.application,
        tool: $stateParams.tool,
        creator: $stateParams.owner
      }).put($scope.auth);
      $modalInstance.close();
    };

    $scope.cancel = $modalInstance.close;
  }])

  .controller('ShareDropDownCtrl', ['$scope',
    function($scope) {

      $('.btn-group').on('click', '.dropdown-menu',function(event){
        console.log("click");
       event.stopPropagation();
       event.preventDefault();
     });

      $scope.toggleDropdown = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.dropDownStatus.isopen = !$scope.dropDownStatus.isopen;
      };
    }]) 

  .controller('ShareCtrl', ['$scope', '$stateParams', 'Local', 'Hub',
    function($scope, $routeParams, Local, Hub) {
      console.log($routeParams);
      $scope.applicationName = $routeParams.application ? $routeParams.application : "nothing";
      $scope.toolName = $routeParams.tool ? $routeParams.tool : "nothing";
      $scope.shareWithName = $routeParams.share_with_name;
      $scope.shareWithEmail = $routeParams.share_with_email;
      $scope.respondingToNotification = $routeParams.request_id;
      $scope.editMode = true;
      $scope.cropData = {cropData:{}};    //make cropData updateable by child scope (player)

      $scope.selection = [];
      $scope.ready = false;

      $scope.dropDownStatus = {
          isopen:false
      };
    
      var toolEnd = Local.one($scope.user.email).one($scope.applicationName)
      .one($scope.toolName);

      //Go fetch all the clips
      toolEnd.get().then(function(tool) {
          for (var i in tool.keyclips) {
              $scope.clips.push({clipId: tool.keyclips[i], toDisplay: "Example "+(+i+1)+" using Keyboard" });
          }
          for (i in tool.guiclips) {
              $scope.clips.push({clipId: tool.guiclips[i], toDisplay: "Example "+(+i+1)+" using GUI"});
          }
        });

      $scope.clips = [];
      $scope.isFirst = true;
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
            console.log("Share clip "+shareWithAll);


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
