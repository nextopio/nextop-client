'use strict';


var argscheck = require('cordova/argscheck'),
    exec = require('cordova/exec');

/** implements the description at
 * https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest
 */
/* TESTING
 * - Run the W3C XHR tests in under the Manual Tests section
 * - Run the automated tests
 */


var XMLHttpRequest = window.XMLHttpRequest;

var sendCount = 0;

var RS_UNSET = 0;
var RS_OPENED = 1;
var RS_HEADERS_RECEIVED = 2;
var RS_LOADING = 3;
var RS_DONE = 4;

var RT_UNSET = '';
var RT_ARRAY_BUFFER = 'arraybuffer';
var RT_BLOB = 'blob';
var RT_DOCUMENT = 'document';
var RT_JSON = 'json';
var RT_TEXT = 'text';


// FIXME move properties and methods to prototype

var Nextop = function(objParameters) {

    var self = this;


    var opened = false;
    var sent = false;

    var aborted = false;
    var abortf = null;


    var mimeType = null;
    var requestHeaders = {};
    var responseHeaders = {};

    var sendMethod = null;
    var sendUrl = null;
    var sendUser = null;
    var sendPassword = null;



    this.getAllResponseHeaders = function() {
        return responseHeaders;
    };
    this.getResponseHeader = function(header) {
        return responseHeaders[header];
    };

    this.overrideMimeType = function(mime) {
        mimeType = mime;
    };

    this.setRequestHeader = function(header, value) {
        if (null == value) {
            delete requestHeaders[header];
        } else {
            requestHeaders[header] = value;
        }
    };

    this.open = function(method, url, async, user, password) {
        //argscheck.checkArgs('ssb*', 'XMLHttpRequest.open', arguments);
        if (opened) {
            // FIXME (alredy opened) what is the expected behavior here?
        }
        opened = true;

        sendMethod = method;
        sendUrl = url;
        sendUser = user;
        sendPassword = password;
    };
    this.send = function(data) {
        if (!opened) {
            // FIXME (not opened) what is the expected behavior here?
        }
        if (sent) {
            // FIXME (already sent) what is the expected behavior here?
        }
        sent = true;

        // FIXME open the flood gates!
        if (false && async) {
            // use Nextop

            var successCallback = function (responseObject) {
                // FIXME status, statusText, responseHeaders, HERE
            }
            var errorCallback = function (error) {
                // FIXME
            }

            var id = ++sendCount;
            var args = [id, {
                mimeType: mimeType,
                requestHeaders: requestHeaders,
                method: sendMethod,
                url: sendUrl,
                user: sendUser,
                password: sendPassword
            }];
            if (typeof data !== 'undefined') {
                args.push(data);
            }

            // FIXME timeout
            // FIXME progress, request status, etc

            abortf = function() {
                exec(successCallback, errorCallback, "Nextop", "abort", [id]);
            };
            exec(successCallback, errorCallback, "Nextop", "send", args);
        } else {
            // use legacy

            var legacy = new XMLHttpRequest(objParameters);


            legacy.open(sendMethod, sendUrl, false, sendUser, sendPassword);


            legacy.timeout = self.timeout;
            legacy.ontimeout = function() {
                if (self.ontimeout) {
                    self.ontimeout();
                }
            }

            legacy.onreadystatechange = function() {
                if (RS_HEADERS_RECEIVED == legacy.readyState) {
                    copyStatus(legacy);
                    copyResponseHeaders(legacy);
                } else if (RS_DONE == legacy.readyState) {
                    copyStatus(legacy);
                    copyResponseHeaders(legacy);
                    copyResponse(legacy);
                }
                setReadyState(legacy.readyState);
            };


            for (var header in requestHeaders) {
                legacy.setRequestHeader(header, requestHeaders[header]);
            }
            if (mimeType) {
                legacy.overrideMimeType(mimeType);
            }

            abortf = function() {
                legacy.abort();
            }
            legacy.send(data);
        }

    };
    this.abort = function() {
        if (abortf) {
            abortf();
        }
    };

    function copyStatus(from) {
        self.status = from.status;
        self.statusText = from.statusText;
    }
    function copyResponseHeaders(from) {
        responseHeaders = from.getAllResponseHeaders();
    }
    function copyResponse(from) {
        self.response = from.response;
        self.responseText = from.responseText;
        self.responseType = from.responseType;
        self.responseXML = from.responseXML;
    }


    function setReadyState(readyState) {
        self.readyState = readyState;
        if (self.onreadystatechange) {
            self.onreadystatechange();
        }
    }

};




/** legacy class */
Nextop.prototype.XMLHttpRequest = XMLHttpRequest;

Nextop.prototype.onreadystatechange = null;
Nextop.prototype.readyState = RS_UNSET;
Nextop.prototype.response = '';
Nextop.prototype.responseText = '';
Nextop.prototype.responseType = RT_UNSET;

Nextop.prototype.responseXML = null;
Nextop.prototype.status = 0;
Nextop.prototype.statusText = null;
Nextop.prototype.timeout = 0;
Nextop.prototype.ontimeout = null;
/** FIXME see
 * oReq.upload.addEventListener("progress", updateProgress, false);
 * oReq.upload.addEventListener("load", transferComplete, false);
 * oReq.upload.addEventListener("error", transferFailed, false);
 * oReq.upload.addEventListener("abort", transferCanceled, false);
 */
Nextop.prototype.upload = null;

Nextop.prototype.withCredentials = false;



// NEXTOP EXTENSIONS

// FIXME behaves like upload, but for the download side
Nextop.prototype.download = null;

// TODO pull this from the build
Nextop.prototype.nextopVersion = '0.1.3';







module.exports = Nextop;


