define(['angular',
        'jquery',
        'angular-bootstrap',
        'ng-grid',
        'services'],
       function (ng, $) {
  /* Controllers */
  
  return ng.module('socasterControllers',
                        ['ui.bootstrap',
                         'ngGrid',
                         'ngResource',
                         'socasterServices'])
  
  .controller('NavCtrl', ['$scope', 'User',
    function($scope, User) {
        User.get(function(user) {
            $scope.userName = user.user.name;
            $scope.applications = Object.keys(user.applications);
            app = $scope.applications[0];
            $scope.application = app;
            $scope.$broadcast('appSelected', app);
        });
        $scope.$on('appSelected', function(event, app) {
            $scope.application = app;
        });
    }])

  .controller('ToolTableCtrl', ['$scope', 'User',
    function($scope, User) {
        $scope.selection = [];
        $scope.$on('appSelected', function(event, app) {
            $scope.data = User.get(function(user) {
                tools = user.applications[app]
                $scope.tools = []
                $.each(tools, function(key, val) {
                    $scope.tools.push(ng.extend({name: key}, val))
                });
            })
        });
        $scope.gridOptions = { 
            data: 'tools',
            selectedItems: $scope.selection,
            multiSelect: false,
        };
    }])

  .controller('DropdownCtrl', ['$scope', '$rootScope', 
    function($scope, $rootScope) {
      $scope.status = {
          isopen: false
      };
      $scope.setApp = function(app) {
        $scope.application.name = app;
        $scope.status.isopen = false;
        $rootScope.$broadcast('appSelected', app);
      }
    }]);
});
