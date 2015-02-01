

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

    /** Diffs the test W3C test results using window.XMLHttpRequest and window.Nextop.
     * Displays the different value for window.Nextop. */
    createActionButton('Diff W3C XMLHttpRequest tests', function() {

        function appendW3cTestDiff(file) {
            var base = 'http://w3c-test.org/XMLHttpRequest/';
            var top1 = '<base href="' + base + '" />' +
                '<script type="text/javascript">if (parent) {window.XMLHttpRequest = parent.window.XMLHttpRequest.XMLHttpRequest;}</script>';
            var top2 = '<base href="' + base + '" />' +
                '<script type="text/javascript">if (parent) {window.XMLHttpRequest = parent.window.XMLHttpRequest;}</script>';

            var xhr = new XMLHttpRequest();
            xhr.open('GET', base + file, false);
            xhr.send();

            // see strategy in 'Run all W3C XMLHttpRequest tests' above
            var doc = xhr.responseText;

            var testDoc1;
            var testDoc2;
            var head = new RegExp('<head[^>]*>', 'gim').exec(doc);
            if (head) {
                var spliti = head.index + head[0].length;
                testDoc1 = doc.substring(0, spliti) + top1 + doc.substring(spliti, doc.length);
                testDoc2 = doc.substring(0, spliti) + top2 + doc.substring(spliti, doc.length);
            } else {
                var doctype = new RegExp('<!doctype[^>]*>').exec(doc);
                if (doctype) {
                    var spliti = doctype.index + doctype[0].length;
                    testDoc1 = doc.substring(0, spliti) + top1 + doc.substring(spliti, doc.length);
                    testDoc2 = doc.substring(0, spliti) + top2 + doc.substring(spliti, doc.length);
                } else {
                    testDoc1 = top1 + doc;
                    testDoc2 = top2 + doc;
                }
            }

            // append the testDoc with chrome

            var s = document.createElement('div');

            var h = document.createElement('h3');
            h.appendChild(document.createTextNode(file));
            s.appendChild(h);

            var f1 = document.createElement('iframe');
            f1.width = '100%';
            f1.height = '180';
            f1.srcdoc = testDoc1;
            s.appendChild(f1);

            var f2 = document.createElement('iframe');
            f2.width = '100%';
            f2.height = '180';
            f2.srcdoc = testDoc2;
            s.appendChild(f2);

            contentEl.appendChild(s);

            // start an interval that replaces the iframe with the diff'd #results elements, when available
            replaceW3cTestDiffResults(s, f1, f2);
        }
        function replaceW3cTestDiffResults(s, f1, f2) {
            var results1 = f1.contentWindow.document.getElementById('results');
            var results2 = f2.contentWindow.document.getElementById('results');
            if (results1 && results2) {
                // diff rows by class
                var trs1 = results1.getElementsByTagName('tr');
                var trs2 = results2.getElementsByTagName('tr');
                if (trs1.length == trs2.length) {
                    var c = 0;
                    // be careful when iterating over a nodelist and removing from it
                    // (go in reverse so indexes remain fixed)
                    for (var n = trs1.length, i = n - 1; 0 <= i; --i) {
                        var tr2 = trs2[i];
                        if (trs1[i].className == tr2.className) {
                            tr2.parentNode.removeChild(tr2);
                        } else {
                            ++c;
                        }
                    }
                    if (0 < c) {
                        s.appendChild(results2);
                    } else {
                        s.parentNode.removeChild(s);
                    }
                } else {
                    // can't diff; show all results2
                    s.appendChild(results2);
                }
                s.removeChild(f1);
                s.removeChild(f2);
            } else {
                // keep polling
                setTimeout(function() {
                    replaceW3cTestDiffResults(s, f1, f2);
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
            appendW3cTestDiff(file);
        }
    });

};
