define(['angular',
        'jquery',
       ], function (ng, $) {
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
  }])

  .directive('whenScrolled', function() {
    return {
      restrict: 'A',
      link: function(scope, elm, attrs) {
        var raw = elm[0];
        $(document).scroll(function() {
          if (raw.scrollTop + raw.offsetHeight >= raw.scrollHeight) {
            scope.$apply(attrs.whenScrolled);
          }
        });
      }
    };
  });

});
