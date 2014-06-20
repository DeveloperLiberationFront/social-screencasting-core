requirejs.config({
    baseUrl: 'lib',
    paths: {
        app: '../app',
        jquery: 'bower/jquery/dist/jquery.min',
        bootstrap: 'bower/bootstrap/dist/js/bootstrap.min.js',
        'jquery-ui': 'bower/jquery-ui/ui/minified/jquery-ui.min',
        text: 'bower/requirejs-text/text',
        angular: 'bower/angular/angular.min',
        'angular-route': 'bower/angular-route/angular-route.min',
        'angular-bootstrap': 'bower/angular-bootstrap/ui-bootstrap.min',

        'tablesorter': 'bower/jquery.tablesorter/js/jquery.tablesorter.min',
        'jquery.metadata': 'bower/jquery.metadata/jquery.metadata'
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
        "angular"
    ]
});

requirejs([
    'angular',
    'app/app',

    'jquery',
    'angular-route',

    //app
    'app/controllers',

    //legacy
    'jquery-ui',
    'jquery.metadata',
    'app/comparison',
    'app/instrumentation',
    'app/playback',
    'app/setUpPlayback',
    'jquery.fullscreen',
    'jquery.form',
    'jquery.rating.pack',
], function(angular, app) {
  'use strict';
  var $html = angular.element(document.getElementsByTagName('html')[0]);
  
  angular.element().ready(function() {
    angular.bootstrap(document, [app['name']]);
  });
});
