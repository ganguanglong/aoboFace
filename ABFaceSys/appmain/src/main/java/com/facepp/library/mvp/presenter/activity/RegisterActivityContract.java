package com.facepp.library.mvp.presenter.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;

/**
 * 项目名：   ABFaceSys
 * 包名：     com.facepp.library.mvp.presenter.activity
 * 创建者：   Arsenalong
 * 创建时间： 2017/11/03    11:39
 * 描述：
 */

public interface RegisterActivityContract {

    interface View {
        void onRegisterSuccess(String info);

        void onRegisterFailure(String error);

        Context getContext();

        void takePhotoForResult(Intent intent, int requestCode);
    }

    interface Presenter {
        void init();

        void doRegister(String name);

        void takePhoto();

        Bitmap handleResult(int requestCode, int resultCode, Intent data);
    }


}
