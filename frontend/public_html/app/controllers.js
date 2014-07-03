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
            $q.all({userTools: userTools, app: $scope.application})
                .then(function(results){
                    _.each(results.app.tools, function(tool) {
                        tool.new = _.findWhere(results.userTools, {name: tool.name}) === undefined;
                    });
                });
        });
    }])

  .controller('MainCtrl', ['$scope',
    function($scope) {
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

  .controller('ToolUsersCtrl', ['$scope', '$modal',
    function($scope, $modal) {
        $scope.selection
        $scope.gridOptions = {
            data: 'tool.$object.users',
            multiSelect: false,
            columnDefs: [{field:'name', displayName:'Name'},
                         {field:'email', displayName:'Email'},
                         {field:'usages', displayName:'Usages'},
                         fieldDefs['video']],
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
                            windowClass: 'modal-player',
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
                fieldDefs['video']
            ],
            sortInfo: {
                fields: ['new', 'video'],
                directions: ['desc', 'desc']
            },
            afterSelectionChange: function() {
                s = $scope.selection;

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

  .controller('StatusCtrl', function() {
  })

  .controller('ToolCtrl', ['$scope', '$stateParams',
    function($scope, $stateParams) {
      var setHandler = function() {
          $scope.application.then(function(app) {
              $scope.tool = app.one($stateParams.name).get($scope.auth);
          });
      };
      if ($scope.application) {
          setHandler();
      }
      $scope.$on('appSelected', setHandler);
    }])

  .controller('ShareCtrl', ['$scope', '$stateParams', 'Local',
    function($scope, $routeParams, Local) {
      console.log($routeParams);
      $scope.applicationName = $routeParams.application ? $routeParams.application : "nothing";
      $scope.toolName = $routeParams.tool ? $routeParams.tool : "nothing";
      $scope.shareWithName = $routeParams.shareWithName;
      $scope.shareWithEmail = $routeParams.shareWithEmail;
      $scope.editMode = true;
      $scope.cropData = {cropData:{}};    //make cropData updateable by child scope (player)

      $scope.selection = [];
      $scope.ready = false;
    
      var toolEnd = Local.one($scope.user.email).one($scope.applicationName)
      .one($scope.toolName);

      toolEnd.get().then(function(tool) {
          console.log(tool.plain());
          for (var i in tool.keyclips) {
              $scope.clips.push({clipId: tool.keyclips[i], toDisplay: "Example "+(+i+1)+" using Keyboard" });
          }
          for (i in tool.guiclips) {
              $scope.clips.push({clipId: tool.guiclips[i], toDisplay: "Example "+(+i+1)+" using GUI"});
          }
          console.log($scope.clips);

        });

      $scope.clips = [];
      $scope.isFirst = true;
      $scope.shareGridOptions = {
        selectedItems: $scope.selection,
        multiSelect: false,
            data: "clips",   //this is a string of the name of the obj in the $scope that has data
            columnDefs: [{ field:'toDisplay'}],
            headerRowHeight:0,
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

            $.ajax({
              url: "shareClip",
              type: "POST",
              data: {
                clip_id : $scope.selection[0].clipId,
                recipient : shareWithAll? "all" : $scope.shareWithEmail,
                start_frame: $scope.clip.start,
                end_frame: $scope.clip.end,
                crop_rect: JSON.stringify($scope.cropData.cropData)
              }
            });
          };
          $scope.cancelSharing = function() {
            console.log("cancelSharing");
          };
    }]);
});
