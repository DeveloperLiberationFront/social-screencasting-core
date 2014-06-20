define(['angular', 'angular-bootstrap'], function (angular) {
  /* Controllers */
  
  return angular.module('socasterControllers', ['ui.bootstrap'])
  
  .controller('NavCtrl', ['$scope',
    function($scope) {
      $scope.userName = 'Test User';
      $scope.applications = ['Eclipse', 'Excel', 'Gmail'];
      $scope.application = $scope.applications[0];
    }])

  .controller('DropdownCtrl', ['$scope', 
    function($scope) {
      $scope.status = {
          isopen: false
      };
      $scope.setApp = function(app) {
        $scope.application = app;
        $scope.status.isopen = false;
      }
    }]);
});
