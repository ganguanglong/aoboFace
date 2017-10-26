package com.facepp.library.view.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.facepp.library.presenter.BasePresenter;
import java.util.List;

/**
 * Created by Administrator on 2017/10/24 0024.
 */

public abstract class BaseActivity extends Activity {

    private List<BasePresenter> mPresenterList;
    private ProgressDialog progressDialog;
    protected void initPresenterList(List<BasePresenter> presenterList,Context context) {
        mPresenterList=presenterList;
        for (BasePresenter mPresenter:mPresenterList) {
            mPresenter.bindActivity(context);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected abstract void initPresenter();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenterList != null){
            for (BasePresenter mPresenter:mPresenterList) {
                mPresenter.destory();
            }
        }
    }

    /*
     * 提示加载
     */
    public void showProgress(String title, String message) {
        if (progressDialog == null) {

            progressDialog = ProgressDialog.show(this,
                    title, message, true, true);
        } else if (progressDialog.isShowing()) {
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(true);
        }
        progressDialog.show();
    }

    /*
     * 隐藏提示加载
     */
    public void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

    }

    /*
     * 吐司
     */
    public void toast(String message,int duration){
        Toast.makeText(this, message, duration).show();
    }

}
