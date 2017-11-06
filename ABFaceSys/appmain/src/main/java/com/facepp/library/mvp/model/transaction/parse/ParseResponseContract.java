package com.facepp.library.mvp.model.transaction.parse;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by Administrator on 2017/10/25 0025.
 */

public interface ParseResponseContract  {
    interface View{}

    interface Presenter{
        <T> T ParseResponse(Response response, Class<T> clazz) throws IOException;
    }
}
