

exports.defineAutoTests = function () {
    describe('tests.nextop.io', function () {

    });
};

exports.defineManualTests = function(contentEl, createActionButton) {

    /** Runs all tests at http://w3c-test.org/XMLHttpRequest/,
     * using the value of window.XMLHttpRequest set by Cordova.
     * This pulls the latest tests from the web.
     * @see https://github.com/w3c/web-platform-tests/tree/master/XMLHttpRequest */
    createActionButton('Run all W3C XMLHttpRequest tests', function() {

        function appendW3cTest(file) {
            var base = 'http://w3c-test.org/XMLHttpRequest/';
            var top = '<base href="' + base + '" />' +
                    '<script type="text/javascript">if (parent) {window.XMLHttpRequest = parent.window.XMLHttpRequest;}</script>';

            var xhr = new XMLHttpRequest();
            xhr.open('GET', base + file, false);
            xhr.send();

            // insert top
            // - immediately after <head ...> if present
            // - else immediately after <!doctype ...> if present
            // - else at front

            var doc = xhr.responseText;

            var testDoc;
            var head = new RegExp('<head[^>]*>', 'gim').exec(doc);
            if (head) {
                var spliti = head.index + head[0].length;
                testDoc = doc.substring(0, spliti) + top + doc.substring(spliti, doc.length);
            } else {
                var doctype = new RegExp('<!doctype[^>]*>').exec(doc);
                if (doctype) {
                    var spliti = doctype.index + doctype[0].length;
                    testDoc = doc.substring(0, spliti) + top + doc.substring(spliti, doc.length);
                } else {
                    testDoc = top + doc;
                }
            }

            // append the testDoc with chrome

            var s = document.createElement('div');

            var h = document.createElement('h3');
            h.appendChild(document.createTextNode(file));
            s.appendChild(h);

            var f = document.createElement('iframe');
            f.width = '100%';
            f.height = '180';
            f.srcdoc = testDoc;
            s.appendChild(f);

            contentEl.appendChild(s);

            // start an interval that replaces the iframe with the #results element, when available
            replaceW3cTestResults(s, f);
        }
        function replaceW3cTestResults(s, f) {
            var results = f.contentWindow.document.getElementById('results');
            if (results) {
                s.appendChild(results);
                s.removeChild(f);
            } else {
                // keep polling
                setTimeout(function() {
                    replaceW3cTestResults(s, f);
                }, 2000);
            }
        }

        // append the w3c stylesheet so results look correct when inlined
        // <link rel="stylesheet" href="http://w3c-test.org/resources/testharness.css">
        var w3cLink = document.createElement('link');
        w3cLink.rel = 'stylesheet';
        w3cLink.type = 'text/css';
        w3cLink.href = 'http://w3c-test.org/resources/testharness.css';
        contentEl.appendChild(w3cLink);

        // read the index and parse out test files
        // for each test file, call appendW3cTest
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'http://w3c-test.org/XMLHttpRequest/', false);
        xhr.send();

        // parse out test urls
        var re = new RegExp('<li[^>]*><a[^>]*>([^<]+\\.(?:htm|html))</a>', 'gim');
        for (var m = re.exec(xhr.responseText); m; m = re.exec(xhr.responseText)) {
            var file = m[1];
            appendW3cTest(file);
        }
    });

};
