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
          return $resource('http://screencaster-hub.appspot.com/api/details/:app/:tool', {}, {
              query: {method:'GET', isArray: false}
          });
      }])

    .factory('Peer', ['$resource', 
      function($resource) {
          return $resource('http://screencaster-hub.appspot.com/api/:email/:app?:auth', {
              email:'@email', app:'', auth: '@auth'
          }, {
              query: {method:'GET', isArray: false}
          });
      }])

    .factory('Rating', ['$resource', 
      function($resource) {
          return $resource('http://screencaster-hub.appspot.com/api/:email/:app/:tool/:clip?:auth', {
              email: '@email', app:'@app', tool:'@tool', clip: '@clip',
          }, {
              query: {method:'GET', isArray: false}
          });
      }])
});
