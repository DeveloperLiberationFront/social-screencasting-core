/*global define */

var recoWeights = [];   //global because otherwise OrderingCtrl can't pass 
var trustWeights = {};  // them to weightRecommendation

var log_mean = 3;       //logistic trust constants
var log_scale = 1.365359;   //3/ln(9)

define(['angular',
        'jquery',
        'lodash',
        'bluebird',
        'ng-bootstrap',
        'ng-grid',
        'ng-ui-utils',
        'restangular',
        'LocalStorageModule',
        'services',
        'player'], function (ng, $, _, Promise) {
  /* Controllers */

  _.templateSettings.interpolate = /{{([\s\S]+?)}}/g;


  function trustForUserEmail(email) {
    if (trustWeights[email] === undefined) {
      return 0.1;
    }
    return 1/(Math.pow(Math.E, (log_mean - trustWeights[email])/log_scale) + 1);
  }

  function weightRecommendation(tool) {
    //TODO memomize this?
    var baseWeight = _.reduce(tool.recommendations, function(sum, reco) {
        var weight = _.find(recoWeights, {id: reco.algorithm_type});
        if (weight) {
          return sum + weight.value/reco.rank;    //Weights the ranks.
        }
        return sum;
    }, 0);


    var weight = _.find(recoWeights, {id: "TRUSTED_USERS_FIRST"}).value;
    if (weight > 0) {
      if (baseWeight === 0) {   //this should be blended with other weights
        baseWeight = 100;     //but this is a fallback for if it'ts not
      }
       var sum = _.reduce(tool.users, function(sum, user) {
          return sum + trustForUserEmail(user) * trustForUserEmail(user);   //sum of squared trusts
       }, 0 );

       baseWeight = baseWeight * weight * sum;
    }

    return baseWeight; 
  }

  function wait(condition, callback) {
    if (condition()) {
      setTimeout(function() {
        wait(condition, callback);
      }, 1000);
    } else {
      callback();
    }
    
  }


function updateTrustWithLikes(likeMap, Yammer, localStorageService) {
    var idsToEmailMap = localStorageService.get("idsToEmailMap");
    var emailToLikeMap = {};

    if (idsToEmailMap === null) {
      idsToEmailMap = {};
    }

    var yammerRequestCounts = 0;
    var waitingOnYammer = [];
    _.each(likeMap, function(likeCount, user_id) {

        if (idsToEmailMap[user_id] !== undefined) {
          _.each(idsToEmailMap[user_id], function(email) {
            emailToLikeMap[email] = likeCount;
          });
        } else {
          waitingOnYammer[yammerRequestCounts] = true;
          _.delay(function(index) {
            Yammer.platform.request({
              url: "users/"+user_id+".json",   
              method: "GET",
              success: function (user) { 
                var emailArray = _.pluck(user.contact.email_addresses, "address");
                idsToEmailMap[user_id] = emailArray;
                _.each(emailArray, function(email) {
                  emailToLikeMap[email] = likeCount;
                });
                waitingOnYammer[index] = false;
              },
              error: function (error) {
                console.error(error);
                waitingOnYammer[index] = false;
              }
            });
          },
        //this is to avoid having more than 10 requests in 10 seconds
         yammerRequestCounts * 1000, yammerRequestCounts);  

          yammerRequestCounts++;
        }
      }); 

    wait(function() {
      return _.some(waitingOnYammer);   //wait until all flags are cleared.
    }, function() {
      localStorageService.set("idsToEmailMap", idsToEmailMap);


      //could add in other interaction metrics here.
      localStorageService.set("emailToInteractions", emailToLikeMap);
      console.log("Done");
      console.log(emailToLikeMap);
    });

}


  function calculateTrust(Yammer, userInfo, localStorageService) {
    var myUserId = userInfo.access_token.user_id;
    // Check to see if we have pulled the like info recently

    // Pull the like info, if ours is stale.  Put into a table of id: likes

    var likeMap = {};

    Yammer.platform.request({
        url: "messages/liked_by/current.json",   
        method: "GET",
        success: function (likes) { 
          console.dir(likes);
          _.forEach(likes.messages, function(message) {
            var authorID = message.sender_id;
            if (authorID !== undefined) {
              // this will be an array
              var containsUser = _.where(message.liked_by.names, {"user_id": myUserId});
              
              if (containsUser.length > 0) {
                if (likeMap[authorID] === undefined) {
                  likeMap[authorID] = 1;
                } else {
                  likeMap[authorID] = likeMap[authorID] + 1;
                }
              }
              
            } 
          });

          updateTrustWithLikes(likeMap, Yammer, localStorageService);
          console.log(likeMap);
          
        },
        error: function (error) {
          console.error(error);
        }
      });


    // Check to see if id is in our stored table of ids : emails, if not, look it up and add to the list

    // Update email:trust table with like information

    // TODO repeat with messages?


    //can access likes with https://www.yammer.com/api/v1/messages/liked_by/current.json
  }

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
                               //'ui.tooltip',
                               'ngGrid',
                               'restangular',
                               'socasterServices',
                               'player'
                              ]);
  Controllers
  .controller('RootCtrl', function($scope, $filter, Hub, User, $window) {
      $scope.user = User;
      $scope.applications = Hub.all('applications').getList();
      $scope.user_list = Hub.all('users').getList();
      $window.Hub = Hub;

      //report interface usage to Local Hub
      $scope.queuedToolUsages = [];

      $scope.numberUnreadNotifications = 0;

      Hub.all("notifications").withHttpConfig({cache:false}).getList({
        'embedded': {'recipient': 1, 'sender': 1, 'tool': 1} //pull in recipient details instead of just the sender
      }).then(function(notifications) {
       // $scope.notifications = notifications;
        $scope.numberUnreadNotifications = _.where(notifications,{notification_status:"new", recipient: {email: $scope.user.email}}).length;
      });

      $scope.focus = function(bool) {
        console.log("focus "+bool);
      };

      $scope.$on('instrumented', function(e, event, info) {   //can be invoked by $scope.$emit('instrumented', [event name], [info]);
        info = info ? info : undefined;
        $scope.queuedToolUsages.push(new ToolUsage(event, "[GUI]", info)); 

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
    })

  .controller('MainCtrl', function($scope, Hub, localStorageService) {
    $scope.$emit('instrumented', "Loaded Interface", {"algorithmWeights": $scope.algorithms});

     var defaultRecoAlgorithms = [
     {
      id: "MOST_WIDELY_USED",
      name:"Most Widely Used",
      description:'Tools are ranked by how many users use them.',
      value:100
      },
      {
      id: "MOST_FREQUENTLY_USED",
      name:"Most Frequenly Used",
      description:'Tools are ranked by how many times they are invoked.',
      value:0
      },
      {
      id: "ITEM_BASED_CF",
      name:"Item-based CF",
      description:'Uses Collaborative Filtering to rank similarity between tools. ',
      value:0
      },
      {
      id: "USER_BASED_CF",
      name:"User-based CF",
      description:'Uses Collaborative Filtering to find similar users and recommend tools from them. ',
      value:0
      },
      {
      id:"LATENT_MODEL_BASED_CF",
      name:"Latent Matrix Factorization",
      description:'Use Apache Mahout\'s <a href="https://mahout.apache.org/users/basics/svd---singular-value-decomposition.html">Singular Value Decomposition algorithm</a>',
      value:0
      },
      {
      id:"MOST_POPULAR_LEARNING_RULE",
      name:"Most Popular Learning Rule",
      description:'This algorithm recommends the most commonly "learned" or "discovered" tools that a user is not using.',
      value:0
      },
      {
      id:"LEARNING_RULE",
      name:"Advanced Learning Rule",
      description:"Recommends the most popular discoveries that a user has the prerequisites for. For example, if we have A->C, C->D, A->C, A->B as our discovery pattern and Person1 only used A, then we would recommend C first and then B but we won't recommend D because the user does not know C yet.",
      value:0
      },
      {
      id:"MOST_PREREQ_LEARNING_RULE",
      name:"Most Prerequisite Learning Rule",
      description:"Sorts tools based on the number of prerequisite commands a user needs for learning a unknown tool.",
      value:0
      },
      {
      id:"TRUSTED_USERS_FIRST",
      name:"Trusted Users First",
      description:"Sorts tools based on their interactions on Yammer, which is a proxy for trust.  If you see a yammer login button below, click it to activate.",
      value:0
      }
    ];

    //global vars
    recoWeights = localStorageService.get("reco_algorithm_settings");
    trustWeights = localStorageService.get("emailToInteractions");

    if (recoWeights === null) {
        recoWeights = defaultRecoAlgorithms;
        localStorageService.set("reco_algorithm_settings",defaultRecoAlgorithms);
    }

    if (trustWeights === null) {
      trustWeights = {};
    }

      $scope.filters = {};
      $scope.ordering = {
        field: weightRecommendation, //weightRecommendation is a function defined at the top of this file
        reverse: true
      };
      $scope.tools = Hub.all('user_tools').getList();
      $scope.rerandomize = function() {
        var numTools = $scope.tools.$object.length;
        _.each($scope.tools.$object, function(tool) {
          tool.random = _.random(0,numTools);   //there will be some repeats, but the random spread should still be good enough
        });
      };
    })

  .controller('ToolListCtrl', ['$scope','Hub','Local',
    function($scope, Hub, Local) {

    $scope.enabled = function() {
      var enableDate = new Date(2014,10,10);      // the month has an OBO error.  WHY?
      return true;
    }

    $scope.getText = function() {
      return "We haven't collected enough data to make recommendations yet.  Check in after 11/10/14 or, if you just joined, in 24 hours.";
    }

    $scope.user.usages = Hub.all('usages').getList({
        'where': {'user': $scope.user.email},
      });

      Promise.all([$scope.user.usages, $scope.tools]).spread(function(usages, tools) {
        _.each(tools, function(tool) {
          tool.unused = !_.find(usages, {tool: tool.tool_id});
        });
      });

      Promise.all([$scope.tools, $scope.user_list]).spread(function(tools, users) {
        _.each(tools, function(tool) {
          if (tool.name === undefined || tool.application === undefined) {
            return;
          }

          tool.video = false;
          tool.userObjects = _.where(users, function(u) {
            //overwrite user list with more detailed user objects
            return _.contains(tool.users, u.email);
          });

          tool.usages = Hub.all('usages').getList({
            where: {tool: tool.tool_id}
          });
          tool.usages.then(function(usages) {
            tool.total_uses = _.reduce(usages, function(acc,usage) {
              return acc + usage.keyboard + usage.mouse;
            }, 0);

            tool.total_keyboard = _.reduce(usages, function(acc,usage) {
              return acc + usage.keyboard ;
            }, 0);

            tool.total_mouse = _.reduce(usages, function(acc,usage) {
              return acc + usage.mouse ;
            }, 0);
          });

          var hub_clips = Hub.all('clips').getList({
            where: {tool: tool.tool_id}
          });

          var local_clips = Local.all('clips').getList({
            app: tool.application,
            tool: tool.name
          });

          Promise.all([hub_clips, local_clips]).spread(function(hub_clips, local_clips) {
            tool.clips = _.union(hub_clips, local_clips);
            tool.video = hub_clips.length > 0;
          });
        });
      });
      
      $scope.limit = 10;
      $scope.scroll = function() {
        $scope.limit += 1;
      };
    }])

  .controller('FilterCtrl', ['$scope', '$filter',
    function($scope, $filter) {
      $scope.filterSet = { filters: [] }; //list of (tool) -> bool

      $scope.filters.toolFilter = function(tool) {
        if (tool.application === undefined || tool.name === undefined ||
           tool.application.indexOf("[") == 0 
          ) {
          return;     
        }
        return _.reduce($scope.filterSet.filters, function(accum, fn) {
          return accum && fn(tool);
        }, true);
      };
    }])

  .controller('OrderCtrl', function($scope, localStorageService) {

      $scope.weights = [];

      localStorageService.bind($scope, "weights", undefined, "reco_algorithm_settings");

     $scope.orderingFunc=function(field, weights){

      recoWeights = weights;

      $scope.$emit('instrumented', "Ordering Tool List", field);
      if (field ==="random") {
        $scope.rerandomize();
      }
       $scope.ordering.field=field;
     };

     $scope.sortOrder= function(value){
      if (value===undefined){
        return $scope.ordering.reverse;
      }else {
        $scope.$emit('instrumented', "Toggling Sort Order", !$scope.ordering.reverse);
        $scope.ordering.reverse = !$scope.ordering.reverse;
      }
     };
      $scope.getName=function(field){
        return _.find($scope.ordering.options,{field:field}).name;
      };



     $scope.ordering.options = [
     {name: "Name", field:"name"},
     {name: "Users", field:"usages.$object"},
     {name: "Recommended", field:weightRecommendation},   //weightRecommendation is a function defined at the top of this file
     {name: "Video", field: "video"},
     {name: "Random", field: "random"}
     ];
   })

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
          //ui-router turns &tool=foo&tool=bar into [foo,bar]
          var users = $stateParams.user_filter.push ? $stateParams.user_filter : [$stateParams.user_filter];
          _.each(users, function(email) {
            if (email) {
              var name = _.find(user_list, {email: email}).name;
              $scope.filter.filters.push({name: name, email: email});
            }
          });
        }
      });

      $scope.filterSet.filters.push(function(tool) {
        return $scope.filter.filters.length === 0 //all tools if no users selected
              || tool.users && _.every($scope.filter.filters, function(user){ //or only tools with all selected users
                return _.find(tool.users, function(email) {
                  return user.email === email;
                });
              });
      });

      $scope.removeFilter = function(filter) {
        $scope.$emit('instrumented', "Removed User Filter",filter);
        var users = $state.params.user_filter.push ? $state.params.user_filter : [$state.params.user_filter];
        $state.go('main', {
          user_filter: _.without(users, filter.email)
        });
      };

      $scope.addFilter = function(input){
        if (!input) return;
        $scope.$emit('instrumented', "Added User Filter",input);
        var users = $stateParams.user_filter ? $stateParams.user_filter : [];
        users = users.push ? users : [users];
        var user_filter = _.union(users, [input.email]);
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
        //ui-router turns &tool=foo&tool=bar into [foo,bar]
        var tools = $stateParams.tool_filter.push ? $stateParams.tool_filter: [$stateParams.tool_filter];
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
        $scope.$emit('instrumented', "Removed Tool Filter",filter);
        var tool_filters = $state.params.tool_filter.push ? $state.params.tool_filter : [$state.params.tool_filter];
        $state.go('main', {
          tool_filter: _.without(tool_filters, filter.name)
        });
      };

      $scope.addFilter = function(input){
        if (!input) return;
        $scope.$emit('instrumented', "Added Tool Filter",input);
        var tool_filter = $stateParams.tool_filter ? $stateParams.tool_filter : [];
        tool_filter = tool_filter.push ? tool_filter : [tool_filter];
        tool_filter = _.union(tool_filter, [input.name]);
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
        templateUrl: '',
        all: true
      };

      $scope.applications.then(function(apps) {
        apps = _.pluck(apps,'name');
        //hide hidden applications from the user
        _.remove(apps, function(name){
            return name.indexOf("[") === 0;
        });
        $scope.filter.source = apps;
        if($scope.filter.all)
          $scope.filter.apps = _.zipObject($scope.filter.source, _.times($scope.filter.source.length, function(){return true;}));
      });

      if ($state.params.app_filter) {
        var apps = $state.params.app_filter.push ? $state.params.app_filter : [$state.params.app_filter];
        //creates an object like {active_app_1:true, active_app_2:true ...}
        $scope.filter.apps = _.zipObject(apps, _.times(apps.length, function(){return true;}));
        $scope.filter.all = false;
      }

      $scope.filterSet.filters.push(function(tool) {
        return $scope.filter.all //unchecking all apps shows all tools
          || $scope.filter.apps[tool.application];
      });

      $scope.updateFilters = function() {
        $scope.$emit('instrumented', "Application Filter changed",$scope.filter.apps);
        $state.go('main', {
          app_filter: _.keys(_.pick($scope.filter.apps, function(v){return v;}))
        });
      };
      
      $scope.allFilters = function() {
        if($scope.filter.all === false)
          $scope.filter.apps = {};
        else
          $scope.filter.apps = _.zipObject($scope.filter.source, _.times($scope.filter.source.length, function(){return true;}));
        $state.go('main', {
          app_filter: []
        });
        //$scope.updateFilters();
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
        { text: "Have a screencast",
          id:"yes_video"
        },{
          text: "Use keyboard",
          id: "has_keyboard"
        },{
          text: "Use mouse",
          id: "has_mouse"
        }
        ],
        active_filters: {},
        templateUrl: ''
      };

      if ($state.params.misc_filter) {
        var misc_filters = $state.params.misc_filter.push ? $state.params.misc_filter : [$state.params.misc_filter];
        //creates an object like {active_app_1:true, active_app_2:true ...}
        $scope.filter.active_filters = _.zipObject(misc_filters, _.times(misc_filters.length, function(){return true;}));
      }

      $scope.filterSet.filters.push(function(tool) {
        return !_.any($scope.filter.active_filters) || //no filters shows all apps
          //or
          ($scope.filter.active_filters.not_used && !_.contains(tool.users, $scope.user.email)) ||
          //or
          ($scope.filter.active_filters.yes_video && tool.video) ||
          // or 
          ($scope.filter.active_filters.has_keyboard && tool.total_keyboard>0) ||
          // or 
          ($scope.filter.active_filters.has_mouse && tool.total_mouse>0);

      });

      $scope.updateFilters = function() {
        $scope.$emit('instrumented', "Misc Filter changed",$scope.filter.apps);
        $state.go('main', {
          misc_filter: _.keys(_.pick($scope.filter.active_filters, function(v){return v;}))
        });
      };

    }])

  .controller('ToolBlockCtrl', ['$scope', '$state', 'Local', 'Base64Img', 'localStorageService',
    function($scope, $state, Local, Base64Img, localStorageService) {
      $scope.hasVideo = function(user) {
        return _.find($scope.tool.clips, {user: user.email})
          || user.email == $scope.user.email;
      };

      $scope.stacked = function(user) {
        var clips = _.where(_.where($scope.tool.clips, {user: user.email}), 'thumbnail');
        return clips.length > 1;
      };

      $scope.thumbnail = function(user) {
        var clips = _.where(_.where($scope.tool.clips, {user: user.email}), 'thumbnail');
        if (clips.length > 0) {
          var c = clips[0]; //should be something like _.max(clips, 'rating');
          return Base64Img(_.isObject(c.thumbnail) ? c.thumbnail.data : c.thumbnail);
        } 
      };
      
      $scope.watched = function(user) {
        var clips = _.where(_.where($scope.tool.clips, {user: user.email}), 'name');
        if (clips.length > 0) {
          var clip_name = clips[0].name;
          var watched_clips = localStorageService.get("watched_clips");
          watched_clips = watched_clips || {};
          if(watched_clips[clip_name])
            return true;
          else
            return false;
        }
      }
      
      $scope.keyboard = function(user) {       
        var usg  = _.find($scope.tool.usages.$object, {user : user.email});
        if (usg){
          return usg.keyboard;
        }
      };
      
      $scope.mouse = function(user) {       
        var usg  = _.find($scope.tool.usages.$object, {user : user.email});
        if (usg){
          return usg.mouse;
        }
      };

      $scope.icon =  function(application) {
        var app = _.find($scope.applications.$object, {name:application});
        if (app)
          return Base64Img(app.icon);        
      };

      $scope.getVideo = function(user) {
        $scope.$emit('instrumented', "Clicked to View/request screencast", {"user":user,application: $scope.tool.application,
            tool_name: $scope.tool.name});
        var self = (user.email == $scope.user.email);
        var origin = (self ? 'local' : 'external');
        if ($scope.hasVideo(user) || self) {
          $state.go('main.video', {
            location: origin,
            owner: user.email,
            application: $scope.tool.application,
            tool_name: $scope.tool.name,
            tool_id: $scope.tool.tool_id,
          });
          var watched_clips = localStorageService.get("watched_clips");
          watched_clips = watched_clips || {};
          var clip = _.where($scope.tool.clips, {"user": user.email})[0];
          watched_clips[clip.name] = watched_clips[clip.name] || {};
          watched_clips[clip.name]["watched_ts"] = (new Date()).valueOf();
          localStorageService.set("watched_clips", watched_clips);
        } else {
          $state.go('main.request', {
            location: origin,
            owner: user._id,
            tool: $scope.tool.tool_id,
            application: $scope.tool.application
          });
          
        }
      };
    }]);

  function prepareMessagesForSentRequests(sent) {
    _.each(sent, function(sentItem) {
      if (sentItem.notification_type == "request_fulfilled") {
        var json = JSON.parse(sentItem.notification_status);
        console.log(json);
        sentItem.shared_videos = _.map(json.video_id, function(id){
          return _.template("#/video?location=external&owner={{recipient.email}}&application={{application}}&tool_name={{tool.name}}&tool_id={{tool._id}}",
            sentItem);
        });
        sentItem.message = _.template(
          "{{recipient.name}} granted access to {{application}}/{{tool.name}}",
          sentItem);
      }
      else {
        sentItem.message = _.template(
          "Requested access to {{recipient.name}}'s usage of {{application}}/{{tool.name}}",
          sentItem);
      }
    });
  }

  function prepareMessagesForReceivedRequests(request){
    _.each(request, function(requestItem){
      requestItem.message = _.template("{{sender.name}} has requested access to {{application}}/{{tool.name}}",requestItem);
    }); 
  }

  Controllers
  .controller('StatusCtrl', ['$scope', 'Hub', '$interpolate',
    function($scope, Hub, $interpolate) {
      $scope.$emit('instrumented', "View Status");
      
      $scope.receivedNotifications = [{}];
      $scope.sentNotifications = [{}];
      
      Hub.all("notifications").withHttpConfig({cache:false}).getList({
        'embedded': {'recipient': 1, 'sender': 1, 'tool': 1} //pull in recipient details instead of j
      }).then(function(notifications) {
       // $scope.notifications = notifications;

        $scope.sentNotifications = _.where(notifications, {sender: {email: $scope.user.email}});
       console.log($scope.sentNotifications);
        $scope.receivedNotifications = _(notifications) //wraps for lodash chaining
          .where({recipient: {email: $scope.user.email}})
          .reject(function(item) { //user doesn't need to see requests they have responded to
            return item.notification_type == "request_fulfilled";
          })
          .value();
        
        prepareMessagesForSentRequests($scope.sentNotifications, $scope.user_list.$object);

        prepareMessagesForReceivedRequests($scope.receivedNotifications);
        _.each($scope.receivedNotifications, function(item) {
          if (item.notification_status == "new") {
            item.notification_status = "seen";
            // since we asked for the expanded versions, we have to remove the
            // fields or else eve gets confused
            var tempTool = item.tool;
            item.tool = undefined;
            var tempRecipient = item.recipient;
            item.recipient = undefined;
            var tempSender = item.sender;
            item.sender = undefined;
            item.patch($scope.auth);

            //pop those items off so we get valid messages
            item.tool = tempTool;
            item.recipient = tempRecipient;
            item.sender = tempSender;
          }
        });
      });
      
      $scope.deleteRequest = function(request) {
        $scope.$emit('instrumented', "Deleted request", request);
        $scope.sentNotifications = _.reject($scope.sentNotifications, function(item) {
          return item._id == request._id;
        });
        request.remove();
      };
      
      $scope.getBadgeText = function(request) {
        if (_.contains(["new", "seen"], request.notification_status)) {
          return request.notification_status;
        }
        return undefined;
      };
        
      $scope.getShareLink = function(request) {
        if ("share_request" == request.notification_type) {
          return $interpolate(
            "#/share/{{application}}/{{tool.name}}"
              + "?share_with_name={{sender.name}}"
              + "&share_with_email={{sender.email}}"
              + "&request_id={{_id}}"
          )(request);
        }
        return "#";
      };
  }])
  
  .controller('SettingsCtrl', function($scope, localStorageService) {
    $scope.$emit('instrumented', "Opened Settings");
    $scope.algorithms = [];

    localStorageService.bind($scope, 'algorithms', undefined, "reco_algorithm_settings", true);
    
    
  })


 // To handle requests made by status page's recording button
  .controller('RecordingCtrl', ['$scope','Local',
    function($scope, Local){     
      Local.one('status','recording').get().then( function (st){ //st is restangular object        
        $scope.recordingStatus = function(value){          
          if (value===undefined){
            return st.status;            
          }
          else
          {            
            st.status=value;            
            st.put();
          }
        };            
      }); 
    }])

.controller('RequestCtrl', ['$scope', '$modalInstance', '$stateParams', 'Hub',
  function($scope, $modalInstance, $stateParams, Hub) {
    $scope.$emit('instrumented', "Opened Request Clip Dialog", {application: $stateParams.application, 
        tool: $stateParams.tool, recipient: $stateParams.owner});
    $scope.request = function() {
      $scope.$emit('instrumented', "Requesting Clip", {application: $stateParams.application, 
        tool: $stateParams.tool, recipient: $stateParams.owner});
      Hub.all('notifications').post({
        application: $stateParams.application,
        tool: $stateParams.tool,
        recipient: $stateParams.owner,
        notification_type: "share_request",
        message: "message",
        notification_status: "new"
      });
      $modalInstance.close();
    };

    $scope.cancel = function() {
    $scope.$emit('instrumented', "Closed Request Clip Dialog");
      $modalInstance.close();
    };
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

        $scope.$emit('instrumented', "No Share Reason Dropdown toggled", $scope.dropDownStatus.isopen);
      };
    }])

  .controller('TrustCtrl', function($rootScope, Yammer, localStorageService) {
    Yammer.getLoginStatus(
      function(response) {
        if (response.authResponse) {
          console.log("Logged into Yammer");
          calculateTrust(Yammer, response, localStorageService);
        }
        else {
          Yammer.connect.loginButton('#yammer-login', function (resp) {
           if (resp.authResponse) {
            calculateTrust(Yammer, response, localStorageService);
            document.getElementById('yammer-login').innerHTML = 'Logged in!'; 
         } 
       });

          // Yammer.platform.login(function (response) { //prompt user to login and authorize your app, as necessary
          //   if (response.authResponse) {
          //     console.log("Yammer login");
          //     calculateTrust(Yammer, response, localStorageService);
          //   } else {
          //     console.log("Could not log into Yammer");
          //   }
          // });
        }
      }
  );

  });

    return Controllers;
 });
