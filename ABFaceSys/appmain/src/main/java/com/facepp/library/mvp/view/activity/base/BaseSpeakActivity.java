package com.facepp.library.mvp.view.activity.base;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

/**
 * 项目名：   ABFaceSys
 * 包名：     com.facepp.library.mvp.view.activity.base
 * 创建者：   Arsenalong
 * 创建时间： 2017/11/03    14:49
 * 描述： 将来如果不用讯飞，或者需要多个朗读器进行替换，就要考虑使用Presenter，或者此部分代码提取到Presenter。
 */

public abstract class BaseSpeakActivity extends BaseActivity{

    protected SpeechSynthesizer mTts;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*初始化语音*/
        registerVoice(this);
    }

    private void registerVoice(Context context) {
        mTts = SpeechSynthesizer.createSynthesizer(this, null);
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mTts.setParameter(SpeechConstant.SPEED, "55");
        mTts.setParameter(SpeechConstant.VOLUME, "80");
        mTts.setParameter(SpeechConstant.ENGINE_MODE, SpeechConstant.MODE_AUTO);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
    }

    /**
     * 对朗读器进行配置，例如语速，音量等
     * @param key 想要配置的项
     * @param value 配置项的参数
     */
    public void setParameter(String key, String value) {
    }

    public void speak(String content){
        mTts.startSpeaking(content, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                BaseSpeakActivity.this.onSpeakBegin();
            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {
                BaseSpeakActivity.this.onBufferProgress(i, i1, i2, s);
            }

            @Override
            public void onSpeakPaused() {
                BaseSpeakActivity.this.onSpeakPaused();
            }

            @Override
            public void onSpeakResumed() {
                BaseSpeakActivity.this.onSpeakResumed();
            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {
                BaseSpeakActivity.this.onSpeakProgress(i, i1, i2);
            }

            @Override
            public void onCompleted(SpeechError speechError) {
                BaseSpeakActivity.this.onCompleted(speechError);
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {
                BaseSpeakActivity.this.onEvent(i, i1, i2, bundle);
            }
        });
    }

    public void onSpeakBegin() {

    }

    public void onBufferProgress(int i, int i1, int i2, String s) {

    }

    public void onSpeakPaused() {

    }

    public void onSpeakResumed() {

    }

    public void onSpeakProgress(int i, int i1, int i2) {

    }

    public void onCompleted(SpeechError speechError) {

    }

    public void onEvent(int i, int i1, int i2, Bundle bundle) {

    }
}
