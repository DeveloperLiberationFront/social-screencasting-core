//Not really sure the right way to do this, unfortunately...

requirejs.config({
    baseUrl: 'app',
    paths: {
        lib: '../lib',
        jquery: '../lib/bower/jquery/dist/jquery.min',
        bootstrap: '../lib/bower/bootstrap/dist/js',
//        'jquery-ui': '../lib/bower/jquery-ui/ui/minified/jquery-ui.min',
        text: '../lib/bower/requirejs-text/text',
        'angular': '../lib/bower/angular/angular',
        'ng-route': '../lib/bower/angular-route/angular-route.min',
        'ng-bootstrap': '../lib/bower/angular-bootstrap/ui-bootstrap-tpls.min',
        'ng-resource': '../lib/bower/angular-resource/angular-resource',
        'ng-grid': '../lib/bower/ng-grid/ng-grid-2.0.11.min',
        'ng-fullscreen': '../lib/bower/ng-fullscreen/ng-fullscreen',
        'ng-slider': '../lib/bower/angular-slider/angular-slider',
        'underscore': '../lib/bower/underscore/underscore',

        'tablesorter': '../lib/bower/jquery.tablesorter/js',
        'jquery.metadata': '../lib/bower/jquery.metadata/jquery.metadata'
    },
    shim: {
	    'angular' : {'exports' : 'angular'},
        'ng-route': ['angular'],
        'ng-bootstrap': ['angular'],
        'ng-resource': ['angular'],
        'ng-fullscreen': ['angular'],
        'ng-grid': ['angular'],
        'ng-slider': ['angular'],
        'bootstrap': ['jquery'],
        'jquery-ui': ['jquery'],
        'tablesorter': ['jquery'],
        'jquery.metadata': ['jquery'],
        'lib/jquery.fullscreen': ['jquery'],
    },
	priority: [
        "angular", "jquery"
    ]
});

requirejs([
    'angular',
    'app',
    'jquery',
    'ng-route',
    'ng-bootstrap',
    'ng-grid',

    'controllers',
//    'playback',
], function(angular, app) {
  'use strict';
  angular.bootstrap(document, [app['name']]);
});
