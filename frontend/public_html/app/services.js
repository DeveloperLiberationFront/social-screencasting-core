define(['angular', 'angular-resource'], function (ng) {
    return ng.module('socasterServices', ['ngResource'])

    .factory('User', ['$resource', 
      function($resource) {
          return $resource('api/user', {}, {
              query: {method:'GET', isArray: false}
          });
      }])
});
