

exports.defineAutoTests = function () {
    describe('tests.nextop.io', function () {

        it("ONE", function () {
        });

        it("TWO", function (done) {
            done();
        });

        it("THREE", function () {

        });

    });

    // http://w3c-test.org/XMLHttpRequest


    describe('http://w3c-test.org/XMLHttpRequest', function () {

        // FIXME for each .htm test,
        // FIXME create an automated test function
        // FIXME

        it("ONE", function () {
        });

        it("TWO", function (done) {
            done();
        });

        it("THREE", function () {

        });

    });

};


function sandbox(html) {
    var el;

    beforeEach(function () {
        el = $(html);
        $(document.body).append(el);
    });


    afterEach(function () {
        el.remove();
        el = null;
    });
}
