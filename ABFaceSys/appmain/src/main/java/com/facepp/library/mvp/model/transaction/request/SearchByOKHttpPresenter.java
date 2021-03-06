package com.facepp.library.mvp.model.transaction.request;

import android.util.Log;

import com.facepp.library.mvp.model.entity.SearchFace;
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
import static com.facepp.library.util.StaticClass.OUTER_ID;
import static com.facepp.library.util.StaticClass.mApiKey;
import static com.facepp.library.util.StaticClass.mApiSecret;
import static com.facepp.library.util.StaticClass.mImageFile;
import static com.facepp.library.util.StaticClass.mOuterId;
import static com.facepp.library.util.StaticClass.url_base;
import static com.facepp.library.util.StaticClass.url_search;

/**
 * Created by Administrator on 2017/10/25 0025.
 */

public class SearchByOKHttpPresenter extends BasePresenter<SearchRequestContract.View> implements
        SearchRequestContract.Presenter {

    private final OkHttpClient client = new OkHttpClient();
    private File mFile;
    private String face_token_know = "";
    private SearchFace searchFace;
    private ParseResponseContract.Presenter mParseResponsePresenter = new ParseByGsonPresenter();

    @Override
    public void SendSearchRequest(final String url, final int confidence) {
        mFile = new File(url);
        /*实例化复合请求体*/
        MultipartBody mMultipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(mApiKey, API_KEY)
                .addFormDataPart(mApiSecret, API_SECRET)
                .addFormDataPart(mOuterId, OUTER_ID)
                .addFormDataPart(mImageFile, mFile.getName(), RequestBody.create(MEDIA_TYPE_JPEG, mFile))
                .build();
        //构建请求体
        Request request = new Request.Builder()
                .url(url_base + url_search)
                .post(mMultipartBody)
                .build();
        //new call
        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(StaticClass.TAG, "onResponse:onFailure: ");
                mView.onFaceSearchFailure();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                /*如果返回正确的响应值*/
                if (response.code() == 200) {
                    /*判断这个人是否已注册*/
                    searchFace = mParseResponsePresenter.ParseResponse(response, SearchFace.class);
                    if (searchFace == null || mView.getSpeaking() || searchFace.getFaces().size() == 0) {
                        Log.i(StaticClass.TAG, "onResponse: "+
                                "searchFace==null:"+(searchFace==null)+
                                "mView.getSpeaking():"+mView.getSpeaking()+
                                "searchFace.getFaces().size()==0:"+(searchFace.getFaces().size()==0));

//                        SendSearchRequest(url, confidence);

                        mView.onFaceSearchFailure();
                        return;
                    }
                    if (searchFace.getResults().get(0).getConfidence() < confidence) {
                        Log.i(StaticClass.TAG, "onResponse: onLowConfidence");
                        mView.onLowConfidence();
                    } else {
                        synchronized (this) {
                            Log.i(StaticClass.TAG, "onResponse: onFaceSearchFound");
                            face_token_know = searchFace.getResults().get(0).getFace_token();
                            mView.onFaceSearchFound(face_token_know);
                        }
                    }
                    /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                } else if (!mView.getSpeaking() && response.body().string().contains("EMPTY_FACESET")) {
                    Log.i(StaticClass.TAG, "onResponse: onFaceSetEmpty");
                    mView.onFaceSetEmpty();
                } else {
                    Log.i(StaticClass.TAG, "onResponse: else");
                    SendSearchRequest(url, confidence);
//                    mView.onFaceSearchFailure();
                }
            }
        });
    }

    @Override
    public void SendSearchRequest(String url, final int confidence, final SearchRequestContract.Callback callback) {
        mFile = new File(url);
        /*实例化复合请求体*/
        MultipartBody mMultipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(mApiKey, API_KEY)
                .addFormDataPart(mApiSecret, API_SECRET)
                .addFormDataPart(mOuterId, OUTER_ID)
                .addFormDataPart(mImageFile, mFile.getName(), RequestBody.create(MEDIA_TYPE_JPEG, mFile))
                .build();
        //构建请求体
        Request request = new Request.Builder()
                .url(url_base + url_search)
                .post(mMultipartBody)
                .build();
        //new call
        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFaceSearchFailure();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                /*如果返回正确的响应值*/
                if (response.code() == 200) {
                    /*判断这个人是否已注册*/
                    searchFace = mParseResponsePresenter.ParseResponse(response, SearchFace.class);
                    if (searchFace == null || mView.getSpeaking() || searchFace.getFaces().size() == 0) {

                        callback.onFaceSearchFailure();
                        return;
                    }
                    if (searchFace.getResults().get(0).getConfidence() < confidence) {
                        callback.onLowConfidence();
                    } else {
                        synchronized (this) {
                            face_token_know = searchFace.getResults().get(0).getFace_token();
                            callback.onFaceSearchFound(face_token_know);
                        }
                    }
                    /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                } else if (!mView.getSpeaking() && response.body().string().contains("EMPTY_FACESET")) {
                    callback.onFaceSetEmpty();
                } else {
                    callback.onFaceSearchFailure();
                }
            }
        });
    }
}
