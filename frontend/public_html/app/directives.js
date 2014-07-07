define(['angular',
       ], function (ng) {
  return ng.module('directives', [])

  .directive('clearable', ['$compile', function($compile) {
    return {
      restrict: 'A',
      require: '^ngModel',
      link: function(scope, element, attrs, controller) {
        container = ng.element('<div class="composite-input"></div>');
        element.wrap(container);
        element.addClass('clearable');
        element.after(
          $compile(
            ng.element('<button class="clear close" type="button"\
                        ng-show="'+attrs.ngModel+'.length > 0"\
                        ng-click="'+attrs.ngModel+'=null">&times;</button>'))
          (scope));
      }
    }
  }]);
});
