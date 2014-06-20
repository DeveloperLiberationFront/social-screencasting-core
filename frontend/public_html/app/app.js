define(['angular', 'app/controllers'], function (angular) {
  'use strict';

  /* App Module */
  
  var socasterApp = angular.module('socasterApp', [
    'ngRoute',
  
    'socasterControllers',
  //  'socasterServices'
  ]);
  
  // socasterApp.config(['$routeProvider',
  //   function($routeProvider) {
  //     $routeProvider.
  //       when('/', {
  //         templateUrl: 'partials/phone-list.html',
  //         controller: 'PhoneListCtrl'
  //       })
  //   }
  // ]);
  return socasterApp;
});
