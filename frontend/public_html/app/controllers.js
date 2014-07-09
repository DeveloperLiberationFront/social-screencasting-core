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
      'new': {
          width: '50px',
          field:'new', displayName:'New',
          cellTemplate: "<div class='ngCellText'><span class='glyphicon glyphicon-ok' ng-show='row.getProperty(col.field)'></span></div>"
      },
      'video': {
          width: '50px',
          field:'video', displayName:'Video',
          cellTemplate:  "<div class='ngCellText'><img src='images/video_icon_tiny.png' ng-show='row.getProperty(col.field)'/></div>",
          sortFn: function(x,y){return (x === y)? 0 : x? 1 : -1;}
      }
  };

  function onApp($scope, callback) {
      var setHandler = function() {
        $scope.application.then(callback);
      };
      if ($scope.application) {
        setHandler();
      }
      $scope.$on('appSelected', setHandler);
  }

  return ng.module('socasterControllers',
                   ['ui.bootstrap',
                    'ui.format',
                    'ngGrid',
                    'restangular',
                    'socasterServices',
                    'player',
                   ])

  .controller('NavCtrl', ['$scope', '$filter', '$q', 'Local', 'Hub',
    function($scope, $filter, $q, Local, Hub) {
        Local.one('user').get().then(function(user){
            $scope.user = user;
            $scope.auth = user.plain();
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
                })
              })
            })
            $q.all({userTools: userTools, app: $scope.application})
                .then(function(results){
                    _.each(results.app.tools, function(tool) {
                        tool.new = _.findWhere(results.userTools, {name: tool.name}) === undefined;
                    });
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

  .controller('UserFilterCtrl', ['$scope',
    function($scope) {
      onApp($scope, function(app) {
          $scope.filter.source = ng.copy(app.users);
        });
      
      $scope.filter = {
        name: 'User',
        input: null,
        source: [],
        filters: [],
        templateUrl: 'partials/user-list-item.html'
      };

      $scope.filterSet.filters.push(function(tool) {
        return $scope.filter.filters.length === 0 //all tools if no users selected
          && tool.users.length > 0 //if it has at least one user
          || _.every($scope.filter.filters, function(user){ //
            return _.find(tool.users, {email: user.email}) != null; //
          });
      });

      $scope.removeFilter = function(filter) {
        _.pull($scope.filter.filters, filter);
      };

      $scope.addFilter = function(input){
        if (input && !_.contains($scope.filter.filters, input)) {
          $scope.filter.filters.push(input);
        }
        $scope.filter.input = null;
      };
    }])

  .controller('ToolFilterCtrl', ['$scope',
    function($scope) {
      onApp($scope, function(app) {
          $scope.filter.source = ng.copy(app.tools);
        });

      $scope.filter = {
        name: 'Tool',
        input: null,
        source: [],
        filters: [],
        templateUrl: ''
      };

      $scope.filterSet.filters.push(function(tool) {
        return $scope.filter.filters.length === 0 ||
          _.any($scope.filter.filters, {name: tool.name});
      });

      $scope.removeFilter = function(filter) {
        _.pull($scope.filter.filters, filter);
      };

      $scope.addFilter = function(input){
        if (input && !_.contains($scope.filter.filters, input)) {
          $scope.filter.filters.push(input);
        }
        $scope.filter.input = null;
      };
    }])

  .controller('ToolListCtrl', ['$scope',
    function($scope) {
      onApp($scope, function(app) {
          $scope.tools = app.tools;
        });
    }])

  .controller('ToolBlockCtrl', ['$scope',
    function($scope) {
      onApp($scope, function(app) {
        $scope.tool.users = _.map($scope.tool.users, function(user) {
          return _.find($scope.application.$object.users, {email: user});
        });
      });

      $scope.details = function(user) {
        return _.find(user.tools, {name: $scope.tool.name});
      };
    }])

  .controller('ToolUsersCtrl', ['$scope', '$modal',
    function($scope, $modal) {
        //$scope.selection
        $scope.gridOptions = {
            data: 'tool.$object.users',
            multiSelect: false,
            columnDefs: [{field:'name', displayName:'Name'},
                         {field:'email', displayName:'Email'},
                         {field:'usages', displayName:'Usages'},
                         fieldDefs.video],
            afterSelectionChange: function(row) {
                if (row && row.entity && row.selected) {
                    var user = row.entity;
                    var userClips = _.where($scope.tool.$object.clips, {creator: user.email});
                    if (userClips.length > 0) {
                        $modal.open({
                            templateUrl: 'partials/modal-player.html',
                            controller: 'ModalPlayer',
                            scope: $scope,
                            resolve: {
                              clips: ['$q', function($q){
                                return $q.all(
                                  userClips.map(function(clip) {
                                    return $scope.tool.$object.one(clip.name).get($scope.auth);
                                  })
                                );
                            }]},
                            windowClass: (userClips.length > 1 ? 'modal-multiclip-player'
                                          : 'modal-player'),
                            size: 'lg'
                        });
                    }
                }
            }
        };
    }])

  .controller('ApplicationToolsCtrl', ['$scope', '$modal', '$state', 'Local', 'Hub',
    function($scope, $modal, $state, Tool) {
        $scope.selection = [];
        $scope.gridOptions = {
            data: 'application.$object.tools',
            selectedItems: $scope.selection,
            multiSelect: false,
            columnDefs: [
                { field:'name', displayName:'Name' },
                fieldDefs['new'],
                fieldDefs.video
            ],
            sortInfo: {
                fields: ['new', 'video'],
                directions: ['desc', 'desc']
            },
            afterSelectionChange: function() {
                var s = $scope.selection;

                if (s.length > 0) {
                    $state.go('tools', {name: s[0].name});
                }
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
      };
    }])

  .controller('StatusCtrl', ['$scope', 'Hub', '$http', function($scope, Hub, $http) {
    $scope.received = [{}];
    $scope.sent = [{}];

    Hub.one("notifications").get($scope.auth).then(function(data){
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
            return "#/video/"+sentItem.plugin+"/"+sentItem.tool+"/"+id;
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

        //$http.delete("http://screencaster-hub.appspot.com/api/notifications/"+request.id+"?email="+
        //  encodeURIComponent($scope.auth.email)+"&name="+encodeURIComponent($scope.auth.name)+"&token="+encodeURIComponent($scope.auth.token));
        // $.ajax({
        //   type:"DELETE",
        //   url:"http://screencaster-hub.appspot.com/api/notifications/"+request.id,
        //   data: $scope.auth,
        //   dataType: "html"
        // });
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
        return "#/video/"+request.plugin+"/"+request.tool+"/"+json.video_id;
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
          //console.log(tool.plain());
          for (var i in tool.keyclips) {
              $scope.clips.push({clipId: tool.keyclips[i], toDisplay: "Example "+(+i+1)+" using Keyboard" });
          }
          for (i in tool.guiclips) {
              $scope.clips.push({clipId: tool.guiclips[i], toDisplay: "Example "+(+i+1)+" using GUI"});
          }
          //console.log($scope.clips);

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
          // $scope.fakeAuth = {email:"test@mailinator.com",
          //       name:"Test User",
          //       token:"123"};

          // console.log("testing updating notification");
          // var put = Hub.one("notifications").one("5715999101812736");
          // put.get($scope.fakeAuth).then(function(notification){
          //   if (notification.type == "request_fulfilled") {
          //     //add this shared video to the list
          //     var json = JSON.parse(notification.status);
          //     json.video_id.push("Eclipse793bcb61-b7c9-31d2-b20e-af103c38d83bG");
          //     put.notification = {status: JSON.stringify(json)};
          //     console.log("putting to array");
          //     put.put($scope.fakeAuth);
          //   }
          //   else {
          //     //mark this notification as responded, and make the status a hash with an array of clip ids
          //     put.notification = {status:JSON.stringify({video_id:["Eclipse793bcb61-b7c9-31d2-b20e-af103c38d83bG"]}), type:"request_fulfilled"};
          //     console.log("putting first video id");
          //     put.put($scope.fakeAuth);
          //   }
          // });


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
