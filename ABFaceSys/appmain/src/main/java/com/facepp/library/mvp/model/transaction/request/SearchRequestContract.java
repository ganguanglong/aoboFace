package com.facepp.library.mvp.model.transaction.request;

/**
 * Created by Administrator on 2017/10/25 0025.
 */

public interface SearchRequestContract {

    interface View{
        void onFaceSearchFailure();
        void onLowConfidence();
        void onFaceSearchFound(String faceToken);
        void onFaceSetEmpty();
        Boolean getSpeaking();
    }

    interface Presenter{
        void SendSearchRequest(String url,int confidence);
        void SendSearchRequest(String url,int confidence,SearchRequestContract.Callback callback);
    }

    interface Callback{
        void onFaceSearchFailure();
        void onLowConfidence();
        void onFaceSearchFound(String faceToken);
        void onFaceSetEmpty();
    }
}
