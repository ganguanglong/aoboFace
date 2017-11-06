package com.facepp.library.mvp.model.transaction.speak;

import android.content.Context;
import android.os.Bundle;

import com.facepp.library.mvp.presenter.BasePresenter;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

/**
 * Created by Administrator on 2017/10/25 0025.
 */

public class SpeakByiFlyPresenter extends BasePresenter<SpeakContract.View> implements SpeakContract.Presenter{

    protected SpeechSynthesizer mTts;

    @Override
    public void registerVoice(Context context) {
        mTts = SpeechSynthesizer.createSynthesizer(context, null);
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mTts.setParameter(SpeechConstant.SPEED, "55");
        mTts.setParameter(SpeechConstant.VOLUME, "80");
        mTts.setParameter(SpeechConstant.ENGINE_MODE, SpeechConstant.MODE_AUTO);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
    }

    @Override
    public void setParameter(String str, String str1) {

    }

    @Override
    public void speak(String str) {
        mTts.startSpeaking(str, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                mView.onSpeakBegin();
            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {
            }

            @Override
            public void onSpeakPaused() {
                mView.onSpeakPaused();
            }

            @Override
            public void onSpeakResumed() {
                mView.onSpeakResumed();
            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {
                mView.onCompleted();
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
    }

}
