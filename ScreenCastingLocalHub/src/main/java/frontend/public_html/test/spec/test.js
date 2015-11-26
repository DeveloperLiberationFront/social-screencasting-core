define(
  [
    'angular',
    'angular-mocks',
    'app'
  ],
  function( angular, mocks ){
    var http;
    
    beforeEach(mocks.module(function($provide) {
      $provide.constant('User' , {"token":"221ed3d8-6a09-4967-91b6-482783ec5313","email":"kjlubick+test@ncsu.edu","name":"Kevin Test"});
    }));

    describe('Controller', function() {
      var $controller;
      var localService;
      var scope;
      var http;

      beforeEach(mocks.inject(function(_$controller_, $httpBackend, $rootScope){
        $controller = _$controller_;
        scope = $rootScope.$new();
        http = $httpBackend;
        http.whenGET('http://recommender.oscar.ncsu.edu/api/v2/applications').respond({"_items": [{"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "Excel", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6c", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6c", "_etag": "2c9ad2c2422b63e2f1fadc5444e09b9de69006c6"}, {"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "Eclipse", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6d", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6d", "_etag": "4366baa39d445db4a8eb15b5824ed7e230a553e7"}, {"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "[ScreencastingHub]", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6e", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6e", "_etag": "e4e3edbf71d630dec55040ec4f5d740264377878"}, {"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "Gmail", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6f", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6f", "_etag": "46e7afc9fb6b51cf85d8fc104a5b0bf8d3b1e893"}], "_links": {"self": {"href": "/applications", "title": "applications"}, "parent": {"href": "", "title": "home"}}});
        http.whenGET('http://recommender.oscar.ncsu.edu/api/v2/users').respond({"_items":[{"_updated":"Tue, 13 Jan 2015 12:49:48 GMT","user_id":"test@ncsu.edu","name":"Test User","last_upload_date":"Tue, 12 Jan 2015 12:49:48 GMT","last_recommendation_date":"Sun, 12 Jan 2015 08:39:52 GMT","_etag":"f023645ea980c5b3396f28a9c50a3bd98c341d43","_links":{"self":{"href":"/users/5453d90207c00a6c986472c3","title":"User"}},"_created":"Fri, 31 Nov 2014 14:46:26 GMT","_id":"5453d90207c00a6c986472c3","email":"test@ncsu.edu","last_recommendation_algorithm":"LATENT_MODEL_BASED_CF"},{"_updated":"Sat, 06 Dec 2014 10:02:56 GMT","user_id":"test2a@ncsu.edu","name":"Test User 2","last_upload_date":"Tue, 09 Dec 2014 10:02:56 GMT","last_recommendation_date":"Tue, 16 Dec 2014 08:21:31 GMT","_etag":"868401434098733aee2a5a7d2a598eac49b68c5c","_links":{"self":{"href":"/users/545d006307c00a163fdb6dc7","title":"User"}},"_created":"Fri, 07 Nov 2014 12:24:51 GMT","_id":"545d006307c00a163fdb6dc7","email":"test2@ncsu.edu","last_recommendation_algorithm":"LATENT_MODEL_BASED_CF"},{"_updated":"Wed, 05 Nov 2014 14:27:01 GMT","user_id":"test3@ncsu.edu","name":"Test User 3","last_upload_date":"Fri, 13 Nov 2014 14:27:01 GMT","last_recommendation_date":"Fri, 20 Dec 2014 08:15:02 GMT","_etag":"33b39ddb8b29138dfc55aeb9a3c8e498c716f958","_links":{"self":{"href":"/users/5460e84807c00a163fdb74ee","title":"User"}},"_created":"Mon, 10 Nov 2014 11:31:04 GMT","_id":"5460e84807c00a163fdb74ee","email":"test3@ncsu.edu","last_recommendation_algorithm":"LATENT_MODEL_BASED_CF"},{"_updated":"Tue, 05 Jan 2015 21:00:50 GMT","name":"Test User 4","email":"test4@ncsu.edu","_links":{"self":{"href":"/users/54ac935207c00a163fdc3095","title":"User"}},"_created":"Wed, 01 Jan 2015 21:00:50 GMT","_id":"54ac935207c00a163fdc3095","_etag":"32627a6c5fcf46b83600234e9905513d34235ba7"}],"_links":{"self":{"href":"/users","title":"users"},"parent":{"href":"","title":"home"}}});
        http.whenGET(/http:\/\/recommender.oscar.ncsu.edu\/api\/v2\/notifications/).respond({"_items":[],"_links":{"self":{"href":"/users","title":"users"},"parent":{"href":"","title":"home"}}});
      }));
      
      afterEach(function() {
    	  http.flush();
          http.verifyNoOutstandingExpectation();
          http.verifyNoOutstandingRequest();
      });

      it('Check user injection', function() {
        var $scope = scope;
        var controller = $controller('RootCtrl', { $scope: $scope});
        expect($scope.user.email).toEqual('kjlubick+test@ncsu.edu');
      });
      
    });

    describe("Services", function() {
      var local, http, hub;
      beforeEach(mocks.module('socasterServices'));
      beforeEach(inject(function(_Local_, _Hub_, $httpBackend) {
        local = _Local_;
        hub = _Hub_;
        http = $httpBackend;
      }));

      afterEach(function() {
        http.verifyNoOutstandingExpectation();
        http.verifyNoOutstandingRequest();
      });

      it("should test getting a user", function() {
        http.whenGET('/api/user').respond({"user":{"token":"221ed3d8-6a09-4967-91b6-482783ec5313","email":"kjlubick+test@ncsu.edu","name":"Kevin Test"}});
        var user = local.one('user').get().then(function(data) {
          expect(data.user.email).toEqual('kjlubick+test@ncsu.edu');
        });
        console.log(user);

        http.flush();
      });
      
      it("should test getting applications", function() {
        http.whenGET('http://recommender.oscar.ncsu.edu/api/v2/applications').respond({"_items": [{"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "Excel", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6c", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6c", "_etag": "2c9ad2c2422b63e2f1fadc5444e09b9de69006c6"}, {"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "Eclipse", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6d", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6d", "_etag": "4366baa39d445db4a8eb15b5824ed7e230a553e7"}, {"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "[ScreencastingHub]", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6e", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6e", "_etag": "e4e3edbf71d630dec55040ec4f5d740264377878"}, {"_updated": "Thu, 01 Jan 1970 00:00:00 GMT", "name": "Gmail", "_links": {"self": {"href": "/applications/5453d90b0d1ff13d518c3b6f", "title": "Application"}}, "_created": "Thu, 01 Jan 1970 00:00:00 GMT", "_id": "5453d90b0d1ff13d518c3b6f", "_etag": "46e7afc9fb6b51cf85d8fc104a5b0bf8d3b1e893"}], "_links": {"self": {"href": "/applications", "title": "applications"}, "parent": {"href": "", "title": "home"}}});
        var user = hub.all('applications').getList().then(function(data) {
          expect(_.any(data, {name: 'Excel'})).toEqual(true);
        });

        http.flush();
      });
    });
  }
);