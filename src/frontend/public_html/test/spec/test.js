define(
  [
    'angular',
    'angular-mocks',
    'app'
  ],
  function( angular, mocks ){

    describe('Controller', function() {
      var $controller;
      var localService;
      var scope;

      beforeEach(mocks.module(function($provide) {
        $provide.constant('User' , {"token":"221ed3d8-6a09-4967-91b6-482783ec5313","email":"kjlubick+test@ncsu.edu","name":"Kevin Test"});
      }));
      beforeEach(mocks.module('socasterApp'));
      beforeEach(mocks.inject(function(_$controller_, $rootScope){
        $controller = _$controller_;
        scope = $rootScope.$new();
      }));

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
        $httpBackend.whenGET('/api/user').respond({"user":{"token":"221ed3d8-6a09-4967-91b6-482783ec5313","email":"kjlubick+test@ncsu.edu","name":"Kevin Test"}});
      }));

      afterEach(function() {
        http.verifyNoOutstandingExpectation();
        http.verifyNoOutstandingRequest();
      });

      it("should test getting a user", function() {
        var user = local.one('user').get().then(function(data) {
          expect(data.user.email).toEqual('kjlubick+test@ncsu.edu');
        });

        http.flush();
      });
    });
  }
);