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


var Nextop = function(objParameters) {

    // MDN

    this.onreadystatechange = null;
    this.readyState = RS_UNSET;
    this.response = '';
    this.responseText = '';
    this.responseType = RT_UNSET;

    this.responseXML = null;
    this.status = 0;
    this.statusText = null;
    this.timeout = 0;
    this.ontimeout = null;
    /** FIXME see
     * oReq.upload.addEventListener("progress", updateProgress, false);
     * oReq.upload.addEventListener("load", transferComplete, false);
     * oReq.upload.addEventListener("error", transferFailed, false);
     * oReq.upload.addEventListener("abort", transferCanceled, false);
     */
    this.upload = null;

    this.withCredentials = false;


    // NEXTOP EXTENSIONS

    // FIXME behaves like upload, but for the download side
    //this.download = null;

    // TODO pull this from the build
    this.nextopVersion = '0.1.3';


    // internal

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
        // TODO argscheck.checkArgs('ssb*', 'XMLHttpRequest.open', arguments);
        if (opened) {
            throw 'Already opened';
        }
        opened = true;

        sendMethod = method;
        sendUrl = url;
        sendUser = user;
        sendPassword = password;
    };
    this.send = function(data) {
        if (!opened) {
            throw 'Not opened';
        }
        if (sent) {
            throw 'Already sent'
        }
        sent = true;

        if (async) {
            // use Nextop

            var successCallback = function (responseObject) {
                self.status = responseObject.status;
                self.statusText = responseObject.statusText;
                responseHeaders = responseObject.responseHeaders;

                self.responseType = /* FIXME parse the responseType/response properly */ RT_TEXT;
                self.response = '';
                self.responseText = responseObject.responseText;

                setReadyState(RS_DONE);
            }
            var errorCallback = function (error) {
                // FIXME handle network error different than other errors
                // fall back to the legacy
                legacySend(data);
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
            legacySend(data);
        }

    };
    function legacySend(data) {
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



// DEEP INTEGRATION
// <img src="">
// <script src="">

/* use the MutationObserver API to intercept 'src' attributes for <img> and <script>
 * and channel them through the Nextop XHR.
  * @see https://hacks.mozilla.org/2012/05/dom-mutationobserver-reacting-to-dom-changes-without-killing-browser-performance */

var rootObserver = new MutationObserver(function(mutations) {
    mutations.forEach(function(mutation) {
        if ('childList' == mutation.type) {
            mutation.addedNodes.forEach(function(node) {
                if ('IMG' == node.tagName) {
                    attachNextopImageLoader(node);
                } else if ('SCRIPT' == node.tagName) {
                    attachNextopLoader(node, 'src');
                }
            });
            mutation.removedNodes.forEach(function(node) {
                if ('IMG' == node.tagName) {
                    detachNextopLoader(node);
                } else if ('SCRIPT' == node.tagName) {
                    detachNextopLoader(node);
                }
            });
        }
    });
});

rootObserver.observe(document.body, {
    subtree: true,
    childList: true,
    attributes: false,
    characterData: false
});

function attachNextopImageLoader(img) {
    _attachNextopLoader(node, 'src', loadImage);
}

function attachNextopLoader(node, attrName) {
    _attachNextopLoader(node, attrName, load);
}

function _attachNextopLoader(node, attrName, loader) {
    var state = attach(node);
    if (node.getAttribute(attrName)) {
        var uri = node.getAttribute(attrName);
        if (uri && !uri.startsWith('data:')) {
            // clear the network uri so the webview does not load it
            node.setAttribute(attrName, '');
            state.cancelAllInFlight();
            loader(node, attrName, uri, state);
        }
    }
    state.observer = new MutationObserver(function(mutation) {
        if ('attribute' == mutation.type) {
            if (attrName == mutation.attributeName) {
                var uri = node.getAttribute(attrName);
                if (uri && !uri.startsWith('data:')) {
                    // clear the network uri so the webview does not load it
                    node.setAttribute(attrName, '');
                    state.cancelAllInFlight();
                    loader(node, attrName, uri, state);
                }
            }
        }
    });
    state.observer.observe(document.body, {
        subtree: false,
        childList: false,
        attributes: true,
        characterData: false
    });
}

function detachNextopLoader(node) {
    var state = detach(node);
    if (state) {
        // cancel all when the node is removed from the screen
        state.cancelAllInFlight();
        if (state.observer) {
            state.observer.disconnect();
        }
    }
}


/////// LOADERS ///////

function loadImage(target, targetAttrName, uri, state) {
    // TODO load in layers
    load(target, targetAttrName, uri, state);
}

function load(target, targetAttrName, uri, state) {
    var xhr = new XMLHttpRequest();
    state.addInFlight(xhr);

    xhr.onreadystatechange = function() {
        if (4 == xhr.readyState && 200 == xhr.status) {
            // Nextop translates binary response into base64 automatically
            target.setAttribute(targetAttrName, xhr.responseText);
            state.removeInFlight(xhr);
        }
    };

    xhr.open('GET', uri, true);
    xhr.send();
}


/////// STATE ///////

var attachStates = [];

function attach(node) {
    var i = attachStates_indexOf(node);
    if (0 <= i) {
        return attachStates[i];
    }
    var attachState = new AttachState(node);
    attachStates.push(attachState);
    return attachState;
}

function detatch(node) {
    var i = attachStates_indexOf(node);
    if (0 <= i) {
        var attachState = attachStates[i];
        attachStates = attachStates.splice(i);
        return attachState;
    }
    return null;
}

function attachStates_indexOf(node) {
    for (var i = 0, n = attachStates.length; i < n; ++i) {
        var attachState = attachStates[i];
        if (node === atachState.node) {
            return i;
        }
    }
    return -1;
}


var AttachState = function(node) {
    this.node = node;
    this.observer = null;

    /** XHR objects */
    var inFlight = [];

    this.addInFlight = function (xhr) {
        inFlight.push(xhr);
    }

    this.removeInFlight = function (xhr) {
        var i = inFlight.indexOf(xhr);
        if (0 <= i) {
            inFlight = inFlight.splice(i);
        }
    }

    this.cancelAllInFlight = function() {
        for (var xhr = inFlight.pop(); xhr; xhr = inFlight.pop()) {
            xhr.abort();
        }
    }
};



// EXPORT

module.exports = Nextop;

