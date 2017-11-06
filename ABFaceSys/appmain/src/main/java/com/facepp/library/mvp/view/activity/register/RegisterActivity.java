package com.facepp.library.mvp.view.activity.register;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.library.R;
import com.facepp.library.mvp.presenter.BasePresenter;
import com.facepp.library.mvp.presenter.activity.RegisterActivityContract;
import com.facepp.library.mvp.presenter.activity.RegiterActivityPresenter;
import com.facepp.library.mvp.view.activity.base.BaseSpeakActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.facepp.library.util.StaticClass.mPicUrl;

/**
 * Created by Administrator on 2017/9/21 0021.
 */

public class RegisterActivity extends BaseSpeakActivity implements RegisterActivityContract.View{

    private File
            //拍摄照片缓存文件
            pictureFile= new File(Environment.getExternalStorageDirectory(), mPicUrl + "Camera.jpg");
    private EditText et_first_name;
    private TextView tv_speech_tips;
    private ImageView iv_face;
    private Button btn_camera, btn_reg;

    private RegisterActivityContract.Presenter mPresenter = new RegiterActivityPresenter();
    private List<BasePresenter> mPresenterList = new ArrayList<>();
    @Override
    protected void initPresenter() {
        mPresenterList.add((BasePresenter) mPresenter);
        registerPresenter(mPresenterList,this);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initPresenter();//注册mPresenter
        initView();// 初始化控件
        mPresenter.init();// 初始化mPresenter
    }

    private void initView() {
        et_first_name = (EditText) findViewById(R.id.et_first_name);
        tv_speech_tips = (TextView) findViewById(R.id.tv_speech_tips);
        iv_face = (ImageView) findViewById(R.id.iv_face);
        iv_face.setImageResource(R.drawable.avatar);
        btn_camera = (Button) findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.takePhoto();
            }
        });
        btn_reg = (Button) findViewById(R.id.btn_reg);
        btn_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(et_first_name.getText())) {
                    Toast.makeText(RegisterActivity.this, "称呼不能为空", Toast.LENGTH_SHORT).show();
                    speak("称呼不能为空噢！");
                    return;
                }
                showProgress("验证中", "正在验证照片有效性..，请稍后");
                mPresenter.doRegister(et_first_name.getText().toString().trim());
            }
        });

        et_first_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().equals("")) {
                    tv_speech_tips.setText(" ");
                }else {
                    tv_speech_tips.setText("将称呼您为：" + s);
                    speak("我将称呼您为：" + s);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    //作用：拿到返回的照片
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        iv_face.setImageBitmap(mPresenter.handleResult(requestCode,resultCode,data));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pictureFile != null) {
            if (pictureFile.exists() && pictureFile.isFile()) {
                pictureFile.delete();
            }
        }
    }

    @Override
    public void onRegisterSuccess(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgress();
                et_first_name.setText("");
                iv_face.setImageResource(R.drawable.avatar);
                toast(info,Toast.LENGTH_SHORT);
                speak(info);
            }
        });

    }

    @Override
    public void onRegisterFailure(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgress();
                toast(error,Toast.LENGTH_SHORT);
                speak("注册失败");
            }
        });

    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void takePhotoForResult(Intent intent, int requestCode) {
        startActivityForResult(intent,requestCode);
    }

}
