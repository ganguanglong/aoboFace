package com.facepp.library.mvp.model.transaction.speak;

import android.content.Context;

/**
 * Created by Administrator on 2017/10/25 0025.
 */

public interface SpeakContract {
    interface View{

        void onSpeakBegin();

        void onSpeakPaused();

        void onSpeakResumed();

        void onCompleted();
    }

    interface Presenter{
        void registerVoice(Context context);

        void setParameter(String str,String str1);

        void speak(String str);
    }

    interface SpeakListener{
        void onSpeakBegin();

        void onSpeakPaused();

        void onSpeakResumed();

        void onCompleted();
    }
}
