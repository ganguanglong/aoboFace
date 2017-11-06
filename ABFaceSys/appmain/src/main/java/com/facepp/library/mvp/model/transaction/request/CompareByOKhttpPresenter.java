package com.facepp.library.mvp.model.transaction.request;

import android.util.Log;

import com.facepp.library.mvp.model.entity.CompareFace;
import com.facepp.library.util.StaticClass;
import com.facepp.library.mvp.presenter.BasePresenter;
import com.facepp.library.mvp.model.transaction.parse.ParseByGsonPresenter;
import com.facepp.library.mvp.model.transaction.parse.ParseResponseContract;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.facepp.library.util.StaticClass.API_KEY;
import static com.facepp.library.util.StaticClass.API_SECRET;
import static com.facepp.library.util.StaticClass.MEDIA_TYPE_JPEG;
import static com.facepp.library.util.StaticClass.mApiKey;
import static com.facepp.library.util.StaticClass.mApiSecret;
import static com.facepp.library.util.StaticClass.CompareFile1;
import static com.facepp.library.util.StaticClass.CompareFile2;
import static com.facepp.library.util.StaticClass.url_base;
import static com.facepp.library.util.StaticClass.url_compare;

/**
 * 项目名：   ABFaceSys
 * 包名：     com.facepp.library.mvp.model.transaction.request
 * 创建者：   Arsenalong
 * 创建时间： 2017/10/27    17:02
 * 描述：
 */

public class CompareByOKhttpPresenter extends BasePresenter<CompareRequestContract.View> implements
        CompareRequestContract.Presenter {

    private final OkHttpClient client = new OkHttpClient();
    private File mFileLast, mFileNow;
    private CompareFace mCompareFace;
    private ParseResponseContract.Presenter mParseResponsePresenter = new ParseByGsonPresenter();

    @Override
    public void sendCompareRequest(String faceUrl1, String faceUrl2, final float confidence, final CompareRequestContract.CallBack callBack) {
        Log.i(StaticClass.TAG, "sendCompareRequest: ");
        mFileLast = new File(faceUrl1);
        mFileNow = new File(faceUrl2);
        /*实例化复合请求体*/
        MultipartBody mMultipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(mApiKey, API_KEY)
                .addFormDataPart(mApiSecret, API_SECRET)
                .addFormDataPart(CompareFile1, mFileLast.getName(), RequestBody.create(MEDIA_TYPE_JPEG, mFileLast))
                .addFormDataPart(CompareFile2, mFileNow.getName(), RequestBody.create(MEDIA_TYPE_JPEG, mFileNow))
                .build();
        //构建请求体
        Request request = new Request.Builder()
                .url(url_base + url_compare)
                .post(mMultipartBody)
                .build();
        //new call
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(StaticClass.TAG, "onFailure: 发送失败："+e);
                callBack.onCompareFailure();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                /*如果返回正确的响应值*/
                if (response.code() == 200) {
                    /*判断获取的数据是否失败*/
                    mCompareFace = mParseResponsePresenter.ParseResponse(response, CompareFace.class);
                    if (mCompareFace == null || mView.getSpeaking()) {
                        callBack.onCompareFailure();
                    }
                    if (mCompareFace.getConfidence() < confidence) {
                        Log.i(StaticClass.TAG, "onResponse: confidence:"+mCompareFace.getConfidence());
                        callBack.onDiffPerson();
                    } else {
                        Log.i(StaticClass.TAG, "onResponse: confidence:"+mCompareFace.getConfidence());
                        /*检测到为同一个人*/
                        callBack.onSamePerson();
                    }
                    /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                } else {
                    callBack.onCompareFailure();
                }
            }
        });
    }
}
