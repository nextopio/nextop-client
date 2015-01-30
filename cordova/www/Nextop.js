


var argscheck = require('cordova/argscheck'),
    exec = require('cordova/exec');

// implements the spec at
// https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest




// METHODS
/*
addEventListener
removeEventListener
*/
/*
XMLHttpRequest(JSObject objParameters);
mozAnon
Boolean: Setting this flag to true will cause the browser not to expose the origin and user credentials when fetching resources. Most important, this means that cookies will not be sent unless explicitly added using setRequestHeader.
    mozSystem
Boolean: Setting this flag to true allows making cross-site connections without requiring the server to opt-in using CORS. Requires setting mozAnon: true, i.e. this can't be combined with sending cookies or other user credentials. This only works in privileged (reviewed) apps; it does not work on arbitrary webpages loaded in Firefox.
  */
/*
void abort()
*/
/*
DOMString getAllResponseHeaders()
 Returns all the response headers as a string, or null if no response has been received. Note: For multipart requests, this returns the headers from the current part of the request, not from the original channel.
 FIXME multipart stuff won't be supported in 0.1.1
 */
/*
 DOMString? getResponseHeader(DOMString header);
 */
/*
 void open(DOMString method, DOMString url, optional boolean async, optional DOMString? user, optional DOMString? password);

 method
 The HTTP method to use, such as "GET", "POST", "PUT", "DELETE", etc. Ignored for non-HTTP(S) URLs.
 url
 The URL to send the request to.
 async
 An optional boolean parameter, defaulting to true, indicating whether or not to perform the operation asynchronously. If this value is false, the send()method does not return until the response is received. If true, notification of a completed transaction is provided using event listeners. This must be true if the multipart attribute is true, or an exception will be thrown.
 Note: Starting with Gecko 30.0 (Firefox 30.0 / Thunderbird 30.0 / SeaMonkey 2.27), synchronous requests on the main thread have been deprecated due to the negative effects to the user experience.
 user
 The optional user name to use for authentication purposes; by default, this is an empty string.
 password
 The optional password to use for authentication purposes; by default, this is an empty string.
 */
/*
void overrideMimeType(DOMString mime);
 */
/*
 void send();
 void send(ArrayBuffer data);
 void send(ArrayBufferView data);
 void send(Blob data);
 void send(Document data);
 void send(DOMString? data);
 void send(FormData data);
 */
/*
 void setRequestHeader(
 DOMString header,
 DOMString value
 );
 */
// PROPERTIES
/*
 onreadystatechange Function?
 A JavaScript function object that is called whenever the readyState attribute changes. The callback is called from the user interface thread.
 */
/*
 readyState unsigned short
 The state of the request:
 */
/*
 response varies
 The response entity body according to responseType, as an ArrayBuffer, Blob, Document, JavaScript object (for "json"), or string. This is null if the request is not complete or was not successful.
 */
/*
 responseText DOMString
 The response to the request as text, or null if the request was unsuccessful or has not yet been sent.
 */
/*
 responseType XMLHttpRequestResponseType
 */
/*
 responseXML Document?

 */
/*
 status unsigned short
 */
/*
 statusText DOMString
 */
/*
 timeout unsigned long
 */
/*
 ontimeout Function
 A JavaScript function object that is called whenever the request times out.
 */
/*
 upload XMLHttpRequestUpload
 The upload process can be tracked by adding an event listener to upload.
 */
/*
 withCredentials boolean


 */
//
//FileTransfer.prototype.upload = function(filePath, server, successCallback, errorCallback, options, trustAllHosts) {
//    argscheck.checkArgs('ssFFO*', 'FileTransfer.upload', arguments);
//    // check for options
//    var fileKey = null;
//    var fileName = null;
//    var mimeType = null;
//    var params = null;
//    var chunkedMode = true;
//    var headers = null;
//    var httpMethod = null;
//    var basicAuthHeader = getBasicAuthHeader(server);
//    if (basicAuthHeader) {
//        server = server.replace(getUrlCredentials(server) + '@', '');
//
//        options = options || {};
//        options.headers = options.headers || {};
//        options.headers[basicAuthHeader.name] = basicAuthHeader.value;
//    }
//
//    if (options) {
//        fileKey = options.fileKey;
//        fileName = options.fileName;
//        mimeType = options.mimeType;
//        headers = options.headers;
//        httpMethod = options.httpMethod || "POST";
//        if (httpMethod.toUpperCase() == "PUT"){
//            httpMethod = "PUT";
//        } else {
//            httpMethod = "POST";
//        }
//        if (options.chunkedMode !== null || typeof options.chunkedMode != "undefined") {
//            chunkedMode = options.chunkedMode;
//        }
//        if (options.params) {
//            params = options.params;
//        }
//        else {
//            params = {};
//        }
//    }
//
//    var fail = errorCallback && function(e) {
//            var error = new FileTransferError(e.code, e.source, e.target, e.http_status, e.body, e.exception);
//            errorCallback(error);
//        };
//
//    var self = this;
//    var win = function(result) {
//        if (typeof result.lengthComputable != "undefined") {
//            if (self.onprogress) {
//                self.onprogress(newProgressEvent(result));
//            }
//        } else {
//            successCallback && successCallback(result);
//        }
//    };
//    exec(win, fail, 'FileTransfer', 'upload', [filePath, server, fileKey, fileName, mimeType, params, trustAllHosts, chunkedMode, headers, this._id, httpMethod]);
//};
//
///**
// * Downloads a file form a given URL and saves it to the specified directory.
// * @param source {String}          URL of the server to receive the file
// * @param target {String}         Full path of the file on the device
// * @param successCallback (Function}  Callback to be invoked when upload has completed
// * @param errorCallback {Function}    Callback to be invoked upon error
// * @param trustAllHosts {Boolean} Optional trust all hosts (e.g. for self-signed certs), defaults to false
// * @param options {FileDownloadOptions} Optional parameters such as headers
// */
//FileTransfer.prototype.download = function(source, target, successCallback, errorCallback, trustAllHosts, options) {
//    argscheck.checkArgs('ssFF*', 'FileTransfer.download', arguments);
//    var self = this;
//
//    var basicAuthHeader = getBasicAuthHeader(source);
//    if (basicAuthHeader) {
//        source = source.replace(getUrlCredentials(source) + '@', '');
//
//        options = options || {};
//        options.headers = options.headers || {};
//        options.headers[basicAuthHeader.name] = basicAuthHeader.value;
//    }
//
//    var headers = null;
//    if (options) {
//        headers = options.headers || null;
//    }
//
//    var win = function(result) {
//        if (typeof result.lengthComputable != "undefined") {
//            if (self.onprogress) {
//                return self.onprogress(newProgressEvent(result));
//            }
//        } else if (successCallback) {
//            var entry = null;
//            if (result.isDirectory) {
//                entry = new (require('org.apache.cordova.file.DirectoryEntry'))();
//            }
//            else if (result.isFile) {
//                entry = new (require('org.apache.cordova.file.FileEntry'))();
//            }
//            entry.isDirectory = result.isDirectory;
//            entry.isFile = result.isFile;
//            entry.name = result.name;
//            entry.fullPath = result.fullPath;
//            entry.filesystem = new FileSystem(result.filesystemName || (result.filesystem == window.PERSISTENT ? 'persistent' : 'temporary'));
//            entry.nativeURL = result.nativeURL;
//            successCallback(entry);
//        }
//    };
//
//    var fail = errorCallback && function(e) {
//            var error = new FileTransferError(e.code, e.source, e.target, e.http_status, e.body, e.exception);
//            errorCallback(error);
//        };
//
//    exec(win, fail, 'FileTransfer', 'download', [source, target, trustAllHosts, this._id, headers]);
//};
//
///**
// * Aborts the ongoing file transfer on this object. The original error
// * callback for the file transfer will be called if necessary.
// */
//FileTransfer.prototype.abort = function() {
//    exec(null, null, 'FileTransfer', 'abort', [this._id]);
//};
//
//module.exports = FileTransfer;
