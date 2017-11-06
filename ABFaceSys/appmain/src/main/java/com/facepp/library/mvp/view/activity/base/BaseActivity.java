package com.facepp.library.mvp.view.activity.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.facepp.library.util.StaticClass;
import com.facepp.library.mvp.presenter.BasePresenter;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Administrator on 2017/10/24 0024.
 */

public abstract class BaseActivity extends Activity {

    private List<BasePresenter> mPresenterList;
    protected ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    //初始化Presenter
    protected abstract void initPresenter();

    //注册Presenter到Presenter列表
    protected void registerPresenter(List<BasePresenter> presenterList, Context context) {
        mPresenterList=presenterList;
        for (BasePresenter mPresenter:mPresenterList) {
            mPresenter.bindActivity(context);
        }

    }

   //提示加载
    public void showProgress(String title, String message) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog.show(this,
                    title, message, true, true);
        } else if (progressDialog.isShowing()) {
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(true);
        }
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onCancleProgressDialog();
            }
        });
        progressDialog.show();
    }

    protected void onCancleProgressDialog() {
    }

    //隐藏提示加载
    public void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    //吐司
    public void toast(String message,int duration){
        Toast.makeText(this, message, duration).show();
    }

    //检查权限
    protected boolean haveAllPermission(Context context){
        if (EasyPermissions.hasPermissions(context, StaticClass.perms)){
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenterList != null){ //如果presenter不为空，
            for (BasePresenter mPresenter:mPresenterList) {
                mPresenter.destory(); //遍历presenter，presenter.destroy
            }
        }
    }
}
