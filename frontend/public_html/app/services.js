define(['angular', 'ng-resource'], function (ng) {
    return ng.module('socasterServices', ['ngResource'])

    .factory('User', ['$resource', 
      function($resource) {
          return $resource('api/user', {}, {
              query: {method:'GET', isArray: false}
          });
      }])

    .factory('Application', ['$resource', 
      function($resource) {
          return $resource('http://screencaster-hub.appspot.com/api/details/:app', {
              app: '@name'
          }, {
              query: {method:'GET', isArray: true}
          });
      }])

    .factory('Tool', ['$resource', 
      function($resource) {
          return $resource('http://screencaster-hub.appspot.com/api/details/:app/:tool', {
              app: '@app', tool: '@name',
          }, {
              query: {method:'GET', isArray: false}
          });
      }])

    .factory('Peer', ['$resource', 
      function($resource) {
          return $resource('http://screencaster-hub.appspot.com/api/:email/:app', {
              email:'@email', app:'',
          }, {
              query: {method:'GET', isArray: false}
          });
      }])

    .factory('Clip', ['$resource', 
      function($resource) {
          return $resource('http://screencaster-hub.appspot.com/api/:creator/:app/:tool/:clip', {
              creator: '@creator', app:'@app', tool:'@tool', clip: '@name',
          }, {
              query: {method:'GET', isArray: false},
          });
      }])
});
