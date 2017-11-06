package com.facepp.library.mvp.model.transaction.request;

/**
 * 项目名：   ABFaceSys
 * 包名：     com.facepp.library.mvp.model.transaction.request
 * 创建者：   Arsenalong
 * 创建时间： 2017/10/27    14:28
 * 描述：
 */

public interface CompareRequestContract {
    interface View{
        Boolean getSpeaking();

    }

    interface Presenter{
        void sendCompareRequest(String faceUrl1,String faceUrl2,float confidence,CompareRequestContract.CallBack callBack);
    }

    interface CallBack{
        void onSamePerson();
        void onCompareFailure();
        void onDiffPerson();
    }
}
