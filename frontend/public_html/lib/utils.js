define(['lodash', 'jquery'], function(_, $) {
    return {
        intersperse: function(array, value) {
            return _(array)
                .zip(_(array).initial().map(function(){return value}).value())
                .flatten()
                .value()
        }
    };
})
