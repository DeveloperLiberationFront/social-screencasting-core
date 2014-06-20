define(['angular', 'controllers'], function (ng) {
  'use strict';

  /* App Module */
  
  return ng.module('socasterApp', [
    'ngRoute',
  
    'socasterControllers',
  //  'socasterServices'
  ])
  
  // .config(['$routeProvider',
  //   function($routeProvider) {
  //     $routeProvider.
  //       when('/', {
  //         templateUrl: 'partials/phone-list.html',
  //         controller: 'PhoneListCtrl'
  //       })
  //   }
  // ]);
});
