define(['angular', 'ng-resource', 'restangular'], function (ng) {
    return ng.module('socasterServices', ['ngResource', 'restangular'])

    .factory('Local', ['Restangular', function(Restangular) {
      return Restangular.withConfig(function(RestangularConfigurer) {
        RestangularConfigurer.setBaseUrl('/api'); //relative to top level (which is already localhost:4443)
      });
    }])

    .factory('Hub', ['Restangular', function(Restangular) {
      return Restangular.withConfig(function(RestangularConfigurer) {
        RestangularConfigurer.setBaseUrl('http://recommender.oscar.ncsu.edu/api/v1');
      });
    }])
});
