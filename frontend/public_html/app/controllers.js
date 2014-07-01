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

  return ng.module('socasterControllers',
                   ['ui.bootstrap',
                    'ui.format',
                    'ngGrid',
                    'restangular',
                    'socasterServices',
                    'player',
                   ])

  .controller('NavCtrl', ['$scope', '$filter', 'Local', 'Hub',
    function($scope, $filter, Local, Hub) {
        Local.one('user').get().then(function(user){
            $scope.user = user;
            $scope.auth = user.plain();
        });
        Hub.one('details').getList().then(function(apps){
            $scope.applications = apps;
            $scope.$broadcast('appSelected', apps[0]);
        });
        $scope.$on('appSelected', function(event, app) {
            $scope.application = Hub.one('details').one(app.name).get($scope.auth);
        });
    }])

  .controller('MainCtrl', ['$scope',
    function($scope) {
    }])

  .controller('UserListCtrl', ['$scope',
    function($scope) {
        $scope.$on('appSelected', function() {
            $scope.application.then(function(app){
                $scope.users = app.users;
            });
        });
        $scope.gridOptions = {
            data: 'users',
            multiSelect: false,
            columnDefs: [{field:'name', displayName:'Name'},
                         {field:'email', displayName:'Email'}]
        };
    }])

  .controller('ApplicationToolsCtrl', ['$scope', '$modal', 'Local', 'Hub',
    function($scope, $modal, Tool) {
        $scope.selection = [];
        $scope.gridOptions = {
            data: 'application.$object.tools',
            selectedItems: $scope.selection,
            multiSelect: false,
            columnDefs: [
                { field:'name', displayName:'Name' },
                {
                    width: '50px',
                    field:'new', displayName:'New',
                    cellTemplate: "<div class='ngCellText'><span class='glyphicon glyphicon-ok' ng-show='row.getProperty(col.field)'></span></div>"
                },
                {
                    width: '50px',
                    field:'video', displayName:'Video',
                    cellTemplate:  "<div class='ngCellText'><img src='images/video_icon_tiny.png' ng-show='row.getProperty(col.field)'/></div>",
                    sortFn: function(x,y){return (x === y)? 0 : x? 1 : -1;}
                }
            ],
            sortInfo: {
                fields: ['new', 'video'],
                directions: ['desc', 'desc']
            },
            afterSelectionChange: function() {
                s = $scope.selection;

                if (s.length > 0 && s[0].video) {
                    tool = s[0];
                    $modal.open({
                        templateUrl: 'partials/modal-player.html',
                        controller: 'ModalPlayer',
                        scope: $scope,
                        resolve: { tool: function(){
                            return $scope.application.$object.one(tool.name).get($scope.auth);
                        }},
                        windowClass: 'modal-player',
                        size: 'lg'
                    });
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

  .controller('ShareCtrl', ['$scope', '$routeParams',
    function($scope, $routeParams) {
      console.log($routeParams);
      console.log($scope);
      $scope.applicationName = $routeParams.application ? $routeParams.application : "nothing";
      $scope.toolName = $routeParams.tool ? $routeParams.tool : "nothing";
      $scope.clipId = $routeParams.clip ? $routeParams.clip : "nothing";
      $scope.shareWithName = $routeParams.shareWithName;
      $scope.shareWithEmail = $routeParams.shareWithEmail;
  
      // $scope.clip = new Clip({
      //     name: $scope.clipId,
      //     tool: $scope.toolName,
      //     app: $scope.applicationName
      // });
    // $scope.clip.$get($scope.auth).then(function() {
    //   console.log("clip fetched");
    //   $scope.status = 'ready';
    //   $scope.$broadcast('refreshSlider');
    // });
    }]);
});
