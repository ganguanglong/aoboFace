package com.facepp.library.view.activity.registeractivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.library.model.entity.AddFace;
import com.facepp.library.model.entity.DaoSession;
import com.facepp.library.model.entity.DetectFace;
import com.facepp.library.model.entity.FaceUser;
import com.facepp.library.model.entity.FaceUserDao;
import com.facepp.library.model.entity.SearchFace;
import com.facepp.library.model.util.GreenDaoUtil;
import com.facepp.library.model.util.Util;
import com.facepp.library.presenter.BasePresenter;
import com.facepp.library.presenter.parse.ParseByGsonPresenter;
import com.facepp.library.presenter.parse.ParseResponseContract;
import com.facepp.library.R;
import com.facepp.library.view.activity.BaseActivity;
import com.facepp.library.view.activity.detect.DetectActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.facepp.library.model.util.Util.API_KEY;
import static com.facepp.library.model.util.Util.API_SECRET;
import static com.facepp.library.model.util.Util.MEDIA_TYPE_JPEG;
import static com.facepp.library.model.util.Util.OUTER_ID;
import static com.facepp.library.model.util.Util.REQUEST_IMAGE_CAPTURE;
import static com.facepp.library.model.util.Util.TAG;
import static com.facepp.library.model.util.Util.mPicUrl;
import static com.facepp.library.model.util.Util.url_add_face;
import static com.facepp.library.model.util.Util.mApiKey;
import static com.facepp.library.model.util.Util.mApiSecret;
import static com.facepp.library.model.util.Util.mAttributes;
import static com.facepp.library.model.util.Util.url_base;
import static com.facepp.library.model.util.Util.url_detect;
import static com.facepp.library.model.util.Util.mFactTokens;
import static com.facepp.library.model.util.Util.mImageFile;
import static com.facepp.library.model.util.Util.mOuterId;
import static com.facepp.library.model.util.Util.url_search;

/**
 * Created by Administrator on 2017/9/21 0021.
 */

public class RegisterActivity extends BaseActivity {

    private File mFile, pictureFile;
    private final OkHttpClient client = new OkHttpClient();
    private EditText et_first_name;
    private TextView tv_speech_tips;
    private ImageView iv_face;
    private Switch s_gender;
    private Button btn_camera, btn_reg;

    private String face_token, gender;
    private int age;
    private DaoSession daoSession;
    private FaceUserDao mFaceUserDao;

    private ParseResponseContract.Presenter mParseResponsePresenter = new ParseByGsonPresenter();
    private List<BasePresenter> mPresenterList = new ArrayList<>();
    @Override
    protected void initPresenter() {
        mPresenterList.add((BasePresenter) mParseResponsePresenter);
        initPresenterList(mPresenterList,this);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        /*初始化控件*/
        initView();
        /*GreenDao初始化*/
        initGreenDao();
    }

    /**
     * 作用：拿到操作数据库所需要的DAO
     */
    private void initGreenDao() {
        daoSession = GreenDaoUtil.getDaoSession(this);
        mFaceUserDao = daoSession.getFaceUserDao();
    }

    private void initView() {
        et_first_name = (EditText) findViewById(R.id.et_first_name);
        tv_speech_tips = (TextView) findViewById(R.id.tv_speech_tips);
        iv_face = (ImageView) findViewById(R.id.iv_face);
        iv_face.setImageResource(R.drawable.avatar);
        s_gender = (Switch) findViewById(R.id.s_gender);
        btn_camera = (Button) findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        btn_reg = (Button) findViewById(R.id.btn_reg);
        btn_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerFace();
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
                }else{
                    tv_speech_tips.setText("将称呼您为：" + s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * 作用：拍照，拿到一张返回的照片，然后放到imageView里面
     * 流程：
     * 1.开启一个intent拍照,并拿到照片，传入到图像控件 openCameraAndShowPhoto();
     */
    private void takePicture() {
        openCameraAndShowPhoto();
    }

    /**
     * 作用：打开摄像头，并把图像传入到iv控件；
     * 流程：
     * 1.拍照
     * 2.拿到拍照结果进行处理 onActivityResult();
     */
    private void openCameraAndShowPhoto() {
        /*设置文件路径*/
        String fileName = "Camera.jpg";  //jpeg文件名定义
        File sdRoot = Environment.getExternalStorageDirectory();    //系统路径
        String dir = mPicUrl;   //文件夹名
        File mkDir = new File(sdRoot, dir);
        if (!mkDir.exists()) {
            mkDir.mkdirs();   //目录不存在，则创建
        }
        pictureFile = new File(sdRoot, dir + fileName);
        Uri uri = Uri.fromFile(pictureFile);

        Intent intent = new Intent();
        // 指定开启系统相机的Action
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra("android.intent.extra.quickCapture",true);
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    /**
     * 作用：拿到返回的照片
     * 流程：
     * 1.对图片进行压缩（由于时间关系，此步骤略去）
     * 2.根据地址刷新媒体库
     * 3.把图片地址传入到imageView  showPhoto();
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_IMAGE_CAPTURE == requestCode) {
            if (-1 == resultCode) {
                updateGallery(mPicUrl);
                showPhoto(pictureFile.getAbsolutePath());
            } else {
                iv_face.setImageResource(R.drawable.avatar);
            }
        }
    }

        /**
         * 刷新媒体库
         */

    private void updateGallery(String infoString) {
        MediaScannerConnection.scanFile(this, new String[]{infoString}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

    /**
     * 作用：处理照片数据
     * 流程：
     * 1.压缩成jpg格式的图片
     */
    private String handleCameraData() {
        return null;
    }


    /**
     * 作用：通过图片名把图片呈现到imageView
     */
    private void showPhoto(String photoName) {
        Bitmap mBitmap = BitmapFactory.decodeFile(photoName);
        iv_face.setImageBitmap(mBitmap);

    }


    /**
     * 作用：注册人脸
     * 流程：
     * 1.检查录入信息的合法性，如果不合法，返回并弹出提示，checkInfo()；
     * 2.判断是否只有一张人脸，以及这个人是否认识的，进而添加人脸。handleFace1()；
     */
    private void registerFace() {
        if (!checkInfo()) {
            return;
        }
        getGender();
        handleFace1();
    }

    /**
     * 作用：真正对人脸进行注册
     * 步骤：
     * 1.把face_token录入到faceSet,addFaceToFaceSet();
     * 2.把人脸添加到数据库（包括face_token，照片地址，性别，年龄，称呼）,addFaceToDB();
     */
    private void doRegister() {
         /*构造builder*/
        Request.Builder builder = new Request.Builder();

        /*构造FormBody*/
        FormBody mFormBody = new FormBody.Builder()
                .add(mApiKey, API_KEY)
                .add(mApiSecret, API_SECRET)
                .add(mOuterId, OUTER_ID)
                .add(mFactTokens, face_token)
                .build();
        /*构造Url,以及传入构造FormBody*/
        Request request = builder.url(url_base + url_add_face).post(mFormBody).build();
        //new call
        Call call = client.newCall(request);
        //请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (e.toString().contains("closed") || e.toString().contains("Canceled")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "请求被取消", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                /*如果返回正确的响应值*/
                if (response.code() == 200) {
                    hideProgress();
                    /*判断这张照片是否已经成功添加*/
                    AddFace addFace;
                    addFace = mParseResponsePresenter.ParseResponse(response, AddFace.class);
                    if (addFace.getFace_added() == 1) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                addFaceToDB();
                                et_first_name.setText("");
                                iv_face.setImageResource(R.drawable.avatar);
                                Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        hideProgress();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "照片中出现的人数超过1人，请重新拍摄", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                    /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                } else if (response.code() == 403) {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            doRegister();
                        }
                    });
                } else {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "网络出现问题，请重试！", Toast.LENGTH_SHORT).show();
                        }
                    });

                }


            }
//
        });
    }

    private String name;

    /**
     * 作用：判断输入框是否为空，如果不为空，去掉空格，赋值到name；
     *
     * @return
     */
    private boolean checkInfo() {
        if (TextUtils.isEmpty(et_first_name.getText())) {
            Toast.makeText(RegisterActivity.this, "姓氏不能为空", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            name = et_first_name.getText().toString().trim();
            Log.i(TAG, "checkInfo: " + name);
        }
        return true;
    }

    /**
     * 作用：判断这是否一个人脸，以及判断这个人是否认识的然后进行注册。
     * 流程：
     * 1.发送数据到faceDetect；
     * 2.如果只是一个人的人脸，获取face_token
     * 3.进行是否认识的人 的验证 handleFace2()；
     *
     * @return
     */
    private void handleFace1() {
        showProgress("验证中", "正在验证照片有效性..，请稍后");
        mFile = new File(Util.FILE_PATH + "Camera" + ".jpg");
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
        //new call
        Call call = client.newCall(request);
        //请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                if (e.toString().contains("closed") || e.toString().contains("Canceled")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "请求被取消", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "验证失败："+e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                /*如果返回正确的响应值*/
                if (response.code() == 200) {
                    hideProgress();
                    /*判断这张照片是否一个人*/
                    DetectFace detectFace;
                    detectFace = mParseResponsePresenter.ParseResponse(response, DetectFace.class);

                    if (detectFace.getFaces().size() == 1) {
                        /*清空上一次获取到的face_token*/
                        face_token = "";
                        face_token = detectFace.getFaces().get(0).getFace_token();
                        age = detectFace.getFaces().get(0).getAttributes().getAge().getValue();
                        Log.i(TAG, "detect success");
                        Log.i(TAG, "face_token: " + face_token);
                        Log.i(TAG, "time_used: " + detectFace.getTime_used());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleFace2();
                            }
                        });
                    } else {
                        hideProgress();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "照片中出现的人数超过1人，请重新拍摄", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                    /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                } else if (response.code() == 403) {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleFace1();
                        }
                    });
                } else {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "网络出现问题，请重试！", Toast.LENGTH_SHORT).show();
                        }
                    });

                }


            }
        });


    }

    /**
     * 作用：判断这个人是否认识的然后进行注册。
     * 流程：
     * 1.发送数据到faceSearch；
     * 2.判断是否已经注册，如果是则提示，如果否则进行注册 doRegister();
     */
    String error;

    private void handleFace2() {
        showProgress("验证中", "正在验证照片有效性..，请稍后");
        mFile = new File(Util.FILE_PATH + "Camera" + ".jpg");
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
        //请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "onFailure: " + e);
                if (e.toString().contains("closed") || e.toString().contains("Canceled")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "请求被取消", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                /*如果返回正确的响应值*/
                if (response.code() == 200) {
                    hideProgress();
                    /*判断这张这个人是否已注册*/
                    SearchFace searchFace;
                    searchFace = mParseResponsePresenter.ParseResponse(response, SearchFace.class);
                    if (searchFace.getResults().get(0).getConfidence() < 85) {
                        /*清空上一次获取到的face_token*/
                        Log.i(TAG, "onResponse: 这是一个不认识的人，可以注册");
                        doRegister();

                    } else {
                        hideProgress();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "你已经注册过", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                    /*如果返回一个空集合，说明这个集合里面没有添加过人脸，可以注册*/
                } else if (response.body().string().contains("EMPTY_FACESET")) {
                    doRegister();
                    /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                } else if (response.code() == 403) {
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleFace2();
                        }
                    });
                } else {
                    error = response.body().string();
                    hideProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "网络出现问题，请重试！/n" + error, Toast.LENGTH_SHORT).show();
                        }
                    });

                }


            }
        });


    }

    /**
     * 作用：把face_token（同时也是照片的地址），性别，年龄，称呼添加到数据库
     * 参数：face_token，性别，年龄，称呼
     * 流程：
     * 1.复制照片（新的名称）（时间关系，暂时不做）
     * 2.获取FaceUserDao
     * 2.把相关属性添加到FaceUser类
     * 3.把FaceUser类添加到数据库
     */

    private void addFaceToDB() {
        /*增加*/
        FaceUser user = new FaceUser(null, face_token, gender, age, name);
        mFaceUserDao.insert(user);
    }

    /**
     * 作用：人脸检测api，可以拿到返回的face_token，以及性别，年龄
     * 参数：人脸照片地址
     */


    private void executeRequest(final Request request) {
        //new call
        Call call = client.newCall(request);
        //请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                DetectFace detectFace;
//                detectFace = parseWithGson(response,DetectFace.class);
                Log.i(TAG, response.body().string());

            }
        });
    }





    public String getGender() {
        if (s_gender.isChecked()) {
            gender = "female";
        } else {
            gender = "male";
        }
        return gender;
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            if (keyCode == KeyEvent.KEYCODE_BACK) { //表示按返回键 时的操作
//                // 监听到返回按钮点击事件
//                Log.i(TAG, "onKeyDown: 监听到了点击返回键");
//                Intent intent = new Intent();
//                intent.setClass(this, DetectActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//If set, and the activity being launched is already running in the current task, then instead of launching a new instance of that activity,all of the other activities on top of it will be closed and this Intent will be delivered to the (now on top) old activity as a new Intent.
//                startActivity(intent);
//
//                finish();
//                  return false;    //已处理
//            }
//        }
//        return super.onKeyDown(keyCode, event);
//    }



}
