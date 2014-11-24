package io.nextop.client.android.demo.collab;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import io.nextop.client.android.NxContext;

public class ClientFragment extends Fragment {
    public static ClientFragment newInstance(String documentId) {
        ClientFragment f = new ClientFragment();
        Bundle args = new Bundle();
        args.putString("documentId", documentId);
        f.setArguments(args);
        return f;
    }


    protected String getDocumentId() {
        return getArguments().getString("documentId");
    }


    


    @Override
    public void onResume() {
        super.onResume();

        // FIXME set up observers
    }

    @Override
    public void onPause() {
        // FIXME remove observers

        super.onPause();
    }
}
