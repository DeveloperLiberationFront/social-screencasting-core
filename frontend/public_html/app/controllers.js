define(['angular', 'angular-bootstrap'], function (angular) {
  'use strict';
  
  /* Controllers */
  
  var socasterControllers = angular.module('socasterControllers', ['ui.bootstrap']);
  
  socasterControllers.controller('NavCtrl', ['$scope',
    function($scope) {
      $scope.userName = 'Test User';
      $scope.applications = ['Eclipse', 'Excel', 'Gmail'];
      $scope.application = $scope.applications[0];
    }]);

  socasterControllers.controller('DropdownCtrl', ['$scope', 
    function($scope) {
      $scope.status = {
          isopen: false
      };
      $scope.setApp = function(app) {
        $scope.application = app;
        $scope.status.isopen = false;
      }
    }]);

  return socasterControllers;
});
