package com.facepp.library;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.facepp.library.entity.DetectFace;
import com.facepp.library.util.Util;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.facepp.library.util.Util.API_KEY;
import static com.facepp.library.util.Util.API_SECRET;
import static com.facepp.library.util.Util.MEDIA_TYPE_JPEG;
import static com.facepp.library.util.Util.TAG;
import static com.facepp.library.util.Util.mApiKey;
import static com.facepp.library.util.Util.mApiSecret;
import static com.facepp.library.util.Util.mAttributes;
import static com.facepp.library.util.Util.url_base;
import static com.facepp.library.util.Util.url_detect;
import static com.facepp.library.util.Util.mImageFile;
import static com.facepp.library.util.Util.mImageUrl;

/**
 * Created by Administrator on 2017/9/20 0020.
 */


public class DetectActivity extends Activity {

    private ImageView mImageView;
    private String infoString;
    private Button btn_detect;
    private final OkHttpClient client = new OkHttpClient();
    private File mFile;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        /*获得传过来的FilePath*/
        getInfo();
        /*更新相册*/
        updateGallery(Util.FILE_PATH);
        /*初始化相册*/
        initial();
        mFile = new File(Util.FILE_PATH + "abjs.jpg");


    }

    private void initial() {
        btn_detect = (Button) findViewById(R.id.btn_detect);
        btn_detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                sentPostUrl();
                sentPostJpeg();
            }
        });

        mImageView = (ImageView) findViewById(R.id.iv);
//        Bitmap bitmap = BitmapFactory.decodeFile(infoString);
        Bitmap bitmap = BitmapFactory.decodeFile(Util.FILE_PATH + "abjs.jpg");
        mImageView.setImageBitmap(bitmap);
    }

    private void sentPostJpeg() {
        /*实例化复合请求体*/
        MultipartBody mMultipartBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(mApiKey, API_KEY)
                            .addFormDataPart(mApiSecret, API_SECRET)
                            .addFormDataPart(mAttributes, "gender,age")
                            .addFormDataPart(mImageFile, mFile.getName(), RequestBody.create(MEDIA_TYPE_JPEG, mFile))
                            .build();
        //构建请求体
        Request request = new Request.Builder()
                .url(url_base + url_detect)
                .post(mMultipartBody)
                .build();
        executeRequest(request);
    }

    private void sentPostUrl() {
        /*构造builder*/
        Request.Builder builder = new Request.Builder();
//        final Request request = new Request.Builder()
//                .url("https://api-cn.faceplusplus.com/facepp/v3/detect")
//                .build();
        /*构造FormBody*/
        FormBody mFormBody = new FormBody.Builder()
                .add(mApiKey, API_KEY)
                .add(mApiSecret, API_SECRET)
                .add(mImageUrl, "http://p1.pstatp.com/origin/3a21000db6ecc87e4c4a")
                .add(mAttributes, "gender,age")
                .build();
        /*构造Url,以及传入构造FormBody*/
        Request request = builder.url(url_base + url_detect).post(mFormBody).build();
        executeRequest(request);

    }

    private void executeRequest(Request request) {
        //new call
        Call call = client.newCall(request);
        //请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                DetectFace detectFace = new DetectFace();
                detectFace =parseWithGson(response);
                Log.i(TAG, "Gender:"+ detectFace.getFaces().get(0).getAttributes().getGender().getValue()+";" +
                        "age:"+ detectFace.getFaces().get(0).getAttributes().getAge().getValue()+";"+
                        "face_token:"+ detectFace.getFaces().get(0).getFace_token());

            }
        });
    }
    //"face_token": "5b532d9645de1adc30ffd716d6f20ada"
    //"faceset_token": "a10aa8ee161464c8b4a0b582b35a4ff5"
    private DetectFace parseWithGson(Response response) throws IOException {
        Gson mGson = new Gson();
        DetectFace detectFace = mGson.fromJson(response.body().string(),DetectFace.class);
        return detectFace;
    }

    private void updateGallery(String infoString) {
        MediaScannerConnection.scanFile(this, new String[]{infoString}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

    private void getInfo() {
        // 获取传递过来的信息。
        infoString = getIntent().getStringExtra(Util.RETURN_INFO);
    }
}
