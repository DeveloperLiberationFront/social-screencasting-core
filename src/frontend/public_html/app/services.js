define(['angular', 'lodash', 'ng-resource', 'restangular'], function (ng, _) {
    return ng.module('socasterServices', ['ngResource', 'restangular'])

    .factory('Local', ['Restangular', function(Restangular) {
      return Restangular.withConfig(function(RestangularConfigurer) {
        RestangularConfigurer.setBaseUrl('/api'); //relative to top level (which is already localhost:4443)
      });
    }])
    
    .config(['$httpProvider', function ($httpProvider) {
      if(window.location.origin.indexOf("http://localhost") !== -1) {
          $httpProvider.interceptors.push(function () {
            return {
                "request": function (config) {
                  if(config.url.indexOf("/api/v2/clips") !== -1) {
                    config.url = config.url.replace("http://recommender.oscar.ncsu.edu", "");
                    config.url = config.url.replace("/api/v2/clips", "/mock/clips");
                  }
                  return config;
                }
            };
        });
      }
    }])
    
    .factory('Hub', ['Restangular', function(Restangular) {
      return Restangular.withConfig(function(RestangularConfigurer) {
          var rc = RestangularConfigurer;
          rc.setBaseUrl('http://recommender.oscar.ncsu.edu/api/v2');
          rc.setRestangularFields({ id: "_id", etag: '_etag' });
          rc.addResponseInterceptor(function(data, operation, what, url, response, deferred) {
              var extractedData;
              // .. to look for getList operations
              if (operation === "getList") {
                  // .. and handle the data and meta data
                  extractedData = data._items;
                  extractedData._meta = data._meta;
                  extractedData._links = data._links;
              } else {
                  extractedData = data;
              }
              return extractedData;
          });
      });
    }])

    .factory('Yammer', function() {
      return window.yam;
    })

    .factory('Base64Img', function() {
      return function(data, type) {
        type = _.isUndefined(type) ? 'jpg' : type; 
        return 'data:image/' + type + ';base64,' + data;
      };
    });
});
