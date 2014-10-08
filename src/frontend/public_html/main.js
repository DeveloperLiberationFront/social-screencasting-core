//Not really sure the right way to do this, unfortunately...

requirejs.config({
    baseUrl: 'app',
    paths: {
        lib: '../lib',
        jquery: '../lib/bower/jquery/dist/jquery.min',
        bootstrap: '../lib/bower/bootstrap/dist/js/bootstrap',
        'jquery-ui': '../lib/bower/jquery-ui/ui/minified/jquery-ui.min',
        text: '../lib/bower/requirejs-text/text',
        'angular': '../lib/bower/angular/angular',
        'ng-route': '../lib/bower/angular-route/angular-route.min',
        'ng-bootstrap': '../lib/bower/angular-bootstrap/ui-bootstrap-tpls.min',
        'ng-resource': '../lib/bower/angular-resource/angular-resource',
        'ng-grid': '../lib/bower/ng-grid/ng-grid-2.0.11.debug',
        'ng-fullscreen': '../lib/bower/ng-fullscreen/ng-fullscreen',
        'ng-slider': '../lib/bower/venturocket-angular-slider/build/angular-slider',
        'ng-touch': '../lib/bower/angular-touch/angular-touch',
        'ng-ui-utils': '../lib/bower/angular-ui-utils/ui-utils',
        'ng-ui-router': '../lib/bower/angular-ui-router/release/angular-ui-router',
        'restangular': '../lib/bower/restangular/dist/restangular',
        'lodash': '../lib/bower/lodash/dist/lodash',
        'deferredBootstrapper': '../lib/bower/angular-deferred-bootstrap/angular-deferred-bootstrap',
        'bluebird': '../lib/bower/bluebird/js/browser/bluebird',
        'LocalStorageModule' : '../lib/bower/angular-local-storage/dist/angular-local-storage',
        'tablesorter': '../lib/bower/jquery.tablesorter/js',
        'jquery.metadata': '../lib/bower/jquery.metadata/jquery.metadata',
        'jquery.cropper': '../lib/cropper/cropper'
    },
    map: {
        '*': {
            'underscore': 'lodash'
        }
    },
    shim: {
	    'angular' : {'exports' : 'angular'},
        'ng-route': ['angular'],
        'ng-bootstrap': ['angular'],
        'ng-resource': ['angular'],
        'ng-fullscreen': ['angular'],
        'ng-grid': ['angular'],
        'ng-touch': ['angular'],
        'ng-slider': ['angular', 'ng-touch'],
        'ng-ui-utils': ['angular'],
        'ng-ui-router': ['angular'],
        'deferredBootstrapper': {'exports': 'deferredBootstrapper', deps: ['angular']},
        'restangular': ['angular'],
        'bootstrap': ['jquery'],
        'jquery-ui': ['jquery'],
        'LocalStorageModule': ['angular'],
        'jquery.cropper':['jquery'],
        'tablesorter': ['jquery'],
        'jquery.metadata': ['jquery'],
        'lib/jquery.fullscreen': ['jquery']
    },
	priority: [
        "angular", "jquery"
    ]
});

requirejs([
    'angular',
    'app',
    'deferredBootstrapper',
    'jquery',
    'ng-route',
    'ng-bootstrap',
    'ng-grid',
    'bootstrap',
    'controllers',
    'services',
    'LocalStorageModule'
], function(angular, app, boot) {
  boot.bootstrap({
    element: document,
    module: app['name'],
    injectorModules: 'socasterServices',
    resolve: {
      User: function(Local, Hub) {
        var user = Local.one('user').get();
        return user;
      }
    }
  });
});
