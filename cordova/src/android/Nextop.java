package io.nextop.cordova;

import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Route;
import io.nextop.WireValue;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;

import java.lang.Override;

import rx.Observer;

// FIXME set e X-Requested-With header to XMLHttpRequest
// see http://stackoverflow.com/questions/15945118/detecting-ajax-requests-on-nodejs-with-express
public class Nextop extends CordovaPlugin implements io.nextop.NextopContext {

    private io.nextop.Nextop nextop;

    /** JS id to Nextop Id forward-translation **/
    Map<Integer, Id> idTranslationMap = new HashMap<Integer, Id>(32);


    public Nextop() {
    }


    @Override
    public io.nextop.Nextop getNextop() {
        return nextop;
    }


    @Override
    protected void pluginInitialize() {
        String accessKey = preferences.getString("NextopAccessKey", null);
        // TODO
        String[] grantKeys = new String[0];

        nextop = io.nextop.Nextop.create(cordova.getActivity(), accessKey, grantKeys).start();
    }

    @Override
    public void onDestroy() {
        nextop = nextop.stop();
    }

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        // id is always [0] then action-specific args follow
        final int id = args.getInt(0);

        if ("send".equals(action)) {
            JSONObject requestArgs = args.getJSONObject(1);

            Message.Builder builder = Message.newBuilder()
                    .setRoute(Route.valueOf(requestArgs.getString("method") + " " + requestArgs.getString("url")));
            // headers
            String mimeType = requestArgs.optString("mimeType");
            if (null != mimeType) {
                builder.setHeader("Content-Type", mimeType);
                // can be overridden below
            }
            JSONObject requestHeaders = requestArgs.optJSONObject("requestHeaders");
            if (null != requestHeaders) {
                for (Iterator itr = requestHeaders.keys(); itr.hasNext(); ) {
                    String key = (String) itr.next();
                    String value = requestHeaders.getString(key);
                    builder.setHeader(key, value);
                }
            }

            Message message = builder.build();

            // track it
            idTranslationMap.put(id, message.id);

            // send it
            nextop.send(message).subscribe(new Observer<Message>() {
                Queue<Message> chunks = new LinkedList<Message>();

                @Override
                public void onNext(Message t) {
                    chunks.add(t);
                }

                @Override
                public void onCompleted() {
                    idTranslationMap.remove(id);

                    // FIXME merge the chunks; currently only do one
                    Message head = chunks.poll();
                    if (null != head) {
                        try {
                            JSONObject responseArgs = new JSONObject();
                            responseArgs.put("status", head.getCode());
                            responseArgs.put("statusText", head.getReason());

                            // headers
                            JSONObject responseHeaders = new JSONObject();
                            for (Map.Entry<WireValue, WireValue> e : head.headers.entrySet()) {
                                responseHeaders.put(e.getKey().toText(), e.getValue().toText());
                            }

                            // FIXME copy response type correctly
                            responseArgs.put("responseText", head.getContent().toText());


                            callbackContext.success(responseArgs);
                        } catch (JSONException e) {
                            // could not create json
                            callbackContext.error(/* FIXME */ 0);
                        }
                    } else {
                        callbackContext.error(/* FIXME */ 0);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    idTranslationMap.remove(id);
                    callbackContext.error(/* FIXME */ 0);
                }
            });


            return true;
        } else if ("abort".equals(action)) {
            Id messageId = idTranslationMap.get(id);
            if (null != messageId) {
                nextop.cancelSend(messageId);
            }
            return true;
        } else {
            return false;
        }
    }
}
