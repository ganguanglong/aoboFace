package com.facepp.library.presenter;

/**
 * Created by Administrator on 2017/10/24 0024.
 */

public abstract class BasePresenter<T> {

    protected T mView;


    public void bindActivity(T view){
        mView = view;
    }


    public void destory() {
        if(mView != null) {
            mView = null;
        }
    }


}
