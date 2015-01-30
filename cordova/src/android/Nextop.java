package io.nextop.cordova;

// FIXME set e X-Requested-With header to XMLHttpRequest
// see http://stackoverflow.com/questions/15945118/detecting-ajax-requests-on-nodejs-with-express
public class Nextop extends CordovaPlugin {

    public Nextop() {
    }


    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
//        if (action.equals("upload") || action.equals("download")) {
//            String source = args.getString(0);
//            String target = args.getString(1);
//
//            if (action.equals("upload")) {
//                upload(source, target, args, callbackContext);
//            } else {
//                download(source, target, args, callbackContext);
//            }
//            return true;
//        } else if (action.equals("abort")) {
//            String objectId = args.getString(0);
//            abort(objectId);
//            callbackContext.success();
//            return true;
//        }
//        return false;
        return true;
    }
}
