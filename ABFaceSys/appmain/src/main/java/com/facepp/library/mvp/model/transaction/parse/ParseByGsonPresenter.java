package com.facepp.library.mvp.model.transaction.parse;

import com.facepp.library.mvp.presenter.BasePresenter;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by Administrator on 2017/10/25 0025.
 */

public class ParseByGsonPresenter extends BasePresenter<ParseResponseContract.View> implements ParseResponseContract.Presenter {

    Gson mGson = new Gson();

    @Override
    public <T> T ParseResponse(Response response, Class<T> clazz) throws IOException {
        T t = null;
        if (mGson != null) {
            t = mGson.fromJson(response.body().string(), clazz);
        }
        return t;
    }
}
