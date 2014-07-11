define(["angular", "lodash", "lib/utils"],
       function(angular, _, utils) {

  return angular.module('breadcrumb', ['ui.router'])

  .factory("Breadcrumbs", ['$state', '$stateParams', '$interpolate',
    function($state, $stateParams, $interpolate) {
      var list = [], title;

      function addBreadcrumb(title, state) {
          list.push({
              title: title,
              state: state
          });
      }

      function generateBreadcrumbs(state, params) {
          if(angular.isDefined(state.parent)) {
              generateBreadcrumbs(state.parent, params);
          }

          if(angular.isDefined(state.breadcrumb)) {
              if(angular.isDefined(state.breadcrumb.title)) {

                  var title = state.breadcrumb.title;
                  // Loop through ownParams and replace any expressions with the matching value
                  _.each(_.keys(params), function(param){
                      title = title.replace('{:' + param + '}',
                                                        params[param]);
                  });

                  addBreadcrumb($interpolate(title)(state.locals.globals),
                                state.name);
              }
          }
      }

      return {
          generate: function() {
              list = [];

              generateBreadcrumbs($state.$current, $stateParams);
              title = utils.intersperse(list, ' > ').join('');
          },

          title: function() {
              return title;
          },

          list: function() {
              return list;
          }
      };
  }])

  .directive("breadcrumbs", function(Breadcrumbs) {
      return {
          restrict: 'E',
          replace: true,
          priority: 100,
          template:
                '  <ul class="breadcrumb">'
              + '    <li>'
              + '      <ng-switch on="breadcrumbs.length === 0">'
              + '      <span ng-switch-when="true">Home</span>'
              + '      <span ng-switch-default><a href="/">Home</a></span>'
              + '      </ng-switch>'
              + '    </li>'
              + '    <li ng-repeat="breadcrumb in Breadcrumbs.list()">'
              + '      <a ng-if="breadcrumb.state && !$last" ui-sref="{{breadcrumb.state}}">'
              + '        {{breadcrumb.title}}'
              + '      </a>'
              + '      <span class="active" ng-show="$last">{{breadcrumb.title}}</span>'
              + '      <span ng-hide="$last" class="divider"></span>'
              + '    </li>'
              + '  </ul>',
          link: function($scope) {
              $scope.Breadcrumbs = Breadcrumbs;
              $scope.$on('$stateChangeSuccess', function(){
                  // Add the current state and params to the scope
                  // $scope.current = $state.$current;
                  // $scope.params = $stateParams;
                  Breadcrumbs.generate();
              });
          }
      }; 
 });
});
