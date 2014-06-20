//Not really sure the right way to do this, unfortunately...

requirejs.config({
    baseUrl: 'app',
    paths: {
        lib: '../lib',
        jquery: '../lib/bower/jquery/dist/jquery.min',
        bootstrap: '../lib/bower/bootstrap/dist/js',
        'jquery-ui': '../lib/bower/jquery-ui/ui/minified/jquery-ui.min',
        text: '../lib/bower/requirejs-text/text',
        'angular': '../lib/bower/angular/angular.min',
        'angular-route': '../lib/bower/angular-route/angular-route.min',
        'angular-bootstrap': '../lib/bower/angular-bootstrap/ui-bootstrap.min',

        'tablesorter': '../lib/bower/jquery.tablesorter/js',
        'jquery.metadata': '../lib/bower/jquery.metadata/jquery.metadata'
    },
    shim: {
	    'angular' : {'exports' : 'angular'},
        'angular-route': ['angular'],
        'angular-bootstrap': ['angular'],
        'bootstrap': ['jquery'],
        'jquery-ui': ['jquery'],
        'tablesorter': ['jquery']
    },
	priority: [
        "angular", "jquery"
    ]
});

requirejs([
    'angular',
    'app',
    'controllers',

    'jquery',
    'angular-route',

    //legacy
    'jquery-ui',
    //'jquery.metadata',
    // 'comparison',
    // 'instrumentation',
    // 'playback',
    // 'setUpPlayback',
    'lib/jquery.fullscreen',
    'lib/jquery.form',
    'lib/jquery.rating.pack',
], function(angular, app) {
  'use strict';
  angular.bootstrap(document, [app['name']]);
});
