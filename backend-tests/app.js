var fs = require('fs');
var express = require('express');
var bodyParser = require('body-parser');


var configObj;
try {
    configObj = JSON.parse(fs.readFileSync('./config.json'));
    console.dir(configObj);
}
catch (err) {
    console.log(err);
}


/////// TEST ENDPOINT ///////


/* tests.nextop.io is an endpoint for tests.
 * It takes in a spec (as a body JSON) on what (or not) to send back. */
function test(method, req, res) {
    var spec = req.body;

    console.log(spec);

    if (spec.drop) {
        // TODO this should drop the connection with no response
        res.set('Connection', 'close');
        res.end();
    }

    var responseCode = 200;
    var responseHeaders = {};
    headerAppend(responseHeaders, 'Content-Type', 'application/json', true);
    var responseBody = {};

    if ('response' in spec) {
        var responseSpec = spec.response;
        if ('code' in responseSpec) {
            responseCode = responseSpec.code;
        }
        if ('headers' in responseSpec) {
            var h = responseSpec.headers;
            for (key in h) {
                headerAppend(responseHeaders, key, h[key], true);
            }
        }
        if ('body' in responseSpec) {
            responseBody = responseSpec.body;
        }
    }

    if (0 <= responseHeaders['Content-Type'].indexOf('application/json') && spec.echoRequestHeaders) {
        responseBody.requestHeaders = req.headers;
    }

    res.status(responseCode);
    for (key in responseHeaders) {
        res.set(key, responseHeaders[key].join(','));
    }
    if ('application/json' == responseHeaders['Content-Type']) {
        res.json(responseBody);
    } else {
        // TODO might have to do conversion here
        res.send(responseBody);
    }
    res.end();
}

function headerAppend(headers, key, value, replaceOnAppendFail) {
    if (key in headers) {
        // TODO complete ...
        var appendable = !('Content-Type' == key || 'Content-Length' == key);
        if (appendable) {
            headers[key].push(value);
        } else if (replaceOnAppendFail) {
            headers[key] = [value];
        }
    } else {
        headers[key] = [value];
    }
}


/////// ROUTING + HTTP ///////

var logger = function(req, res, next) {
    console.log("" + req.url);
    next();
}


var app = express();
app.use(bodyParser.json());
app.use(logger);


/* @see http://expressjs.com/api.html
 */

app.get('/status', function(req, res) {
    res.status(200);
    res.end();
});

app.get('/', function(req, res) {
    test('get', req, res);
});

app.post('/', function(req, res) {
    test('post', req, res);
});

app.put('/', function(req, res) {
    test('put', req, res);
});

// TODO complete methods ...


var server = app.listen(process.env.PORT || configObj.http.port, function () {
    var host = server.address().address;
    var port = server.address().port;

    console.log('backend-tests listening at http://%s:%s', host, port)
});
