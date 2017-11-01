package com.facepp.library.view.activity.permission;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.facepp.library.R;
import com.facepp.library.model.util.Util;
import com.facepp.library.view.activity.BaseActivity;
import com.facepp.library.view.activity.detect.DetectActivity;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 项目名：   ABFaceSys
 * 包名：     com.facepp.library.view.activity.permission
 * 创建者：   Arsenalong
 * 创建时间： 2017/10/31    17:14
 * 描述：
 */

public class PermissionActivity  extends BaseActivity implements EasyPermissions.PermissionCallbacks{
    private Button btn_rq;
    // 权限回调的标示
    private static final int RC = 0x0100;
    @Override
    protected void initPresenter() {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(haveAllPermission(this)){
            startActivity(new Intent(this,DetectActivity.class));
        }
        setContentView(R.layout.layout_permission);
        btn_rq= (Button) findViewById(R.id.btn_rq);
        btn_rq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPerm();
            }
        });
    }

    /**
     * 申请权限的方法
     */
    @AfterPermissionGranted(RC)
    private void requestPerm() {
        if (EasyPermissions.hasPermissions(this, Util.perms)) {
            startActivity(new Intent(this,DetectActivity.class));
            finish();
        } else {
            EasyPermissions.requestPermissions(this, "授予所必须的权限",
                    RC, Util.perms);
        }
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        // 如果权限有没有申请成功的权限存在，则弹出弹出框，用户点击后去到设置界面自己打开权限
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog
                    .Builder(this)
                    .build()
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 传递对应的参数，并且告知接收权限的处理者是我自己
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
