package com.facepp.library.mvp.presenter.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.library.R;
import com.facepp.library.mvp.model.entity.AddFace;
import com.facepp.library.mvp.model.entity.DaoSession;
import com.facepp.library.mvp.model.entity.DetectFace;
import com.facepp.library.mvp.model.entity.FaceUser;
import com.facepp.library.mvp.model.entity.FaceUserDao;
import com.facepp.library.mvp.model.entity.SearchFace;
import com.facepp.library.mvp.presenter.BasePresenter;
import com.facepp.library.mvp.presenter.function.parse.ParseByGsonPresenter;
import com.facepp.library.mvp.presenter.function.parse.ParseResponseContract;
import com.facepp.library.mvp.presenter.function.speak.SpeakByiFlyPresenter;
import com.facepp.library.mvp.presenter.function.speak.SpeakContract;
import com.facepp.library.mvp.view.activity.register.RegisterActivity;
import com.facepp.library.util.GreenDaoUtil;
import com.facepp.library.util.StaticClass;

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

import static com.facepp.library.R.id.et_first_name;
import static com.facepp.library.util.ConUtil.updateGallery;
import static com.facepp.library.util.StaticClass.API_KEY;
import static com.facepp.library.util.StaticClass.API_SECRET;
import static com.facepp.library.util.StaticClass.MEDIA_TYPE_JPEG;
import static com.facepp.library.util.StaticClass.OUTER_ID;
import static com.facepp.library.util.StaticClass.REQUEST_IMAGE_CAPTURE;
import static com.facepp.library.util.StaticClass.mApiKey;
import static com.facepp.library.util.StaticClass.mApiSecret;
import static com.facepp.library.util.StaticClass.mAttributes;
import static com.facepp.library.util.StaticClass.mFactTokens;
import static com.facepp.library.util.StaticClass.mImageFile;
import static com.facepp.library.util.StaticClass.mOuterId;
import static com.facepp.library.util.StaticClass.mPicUrl;
import static com.facepp.library.util.StaticClass.url_add_face;
import static com.facepp.library.util.StaticClass.url_base;
import static com.facepp.library.util.StaticClass.url_detect;
import static com.facepp.library.util.StaticClass.url_search;

/**
 * 项目名：   ABFaceSys
 * 包名：     com.facepp.library.mvp.presenter.activity
 * 创建者：   Arsenalong
 * 创建时间： 2017/11/03    11:39
 * 描述：
 */

public class RegiterActivityPresenter extends BasePresenter<RegisterActivityContract.View>
        implements RegisterActivityContract.Presenter {
    private String face_token,  // 人脸token
            gender = "",        // 性别
            name;               // 姓名
    private int age;
    private final OkHttpClient client = new OkHttpClient();
    private DaoSession daoSession;
    private FaceUserDao mFaceUserDao;
    private ParseResponseContract.Presenter mParseResponsePresenter = new ParseByGsonPresenter();
    private File pictureFile = new File(Environment.getExternalStorageDirectory(), mPicUrl + "Camera.jpg");

    @Override
    public void init() {
        initGreenDao(); //初始化GreenDao
    }

        //作用：拿到操作数据库所需要的DAO
        private void initGreenDao() {
            daoSession = GreenDaoUtil.getDaoSession(mView.getContext());
            mFaceUserDao = daoSession.getFaceUserDao();
        }

    /*
     * 拍照
     * 1.开启拍照
     */
    @Override
    public void takePhoto() {
        /*设置文件路径*/
        File mkDir = new File(Environment.getExternalStorageDirectory(), mPicUrl);
        if (!mkDir.exists()) {
            mkDir.mkdirs();   //目录不存在，则创建
        }

        Uri uri = Uri.fromFile(pictureFile);

        Intent intent = new Intent();
        // 指定开启系统相机的Action
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra("android.intent.extra.quickCapture", true);
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        mView.takePhotoForResult(intent, REQUEST_IMAGE_CAPTURE);
    }
    /*
     * 拍照
     * 2.处理拍照返回结果
     */
    @Override
    public Bitmap handleResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = null;
        Log.i(StaticClass.TAG, "handleResult: REQUEST_IMAGE_CAPTURE == requestCode");
        if (REQUEST_IMAGE_CAPTURE == requestCode) {
            if (-1 == resultCode) {
                Log.i(StaticClass.TAG, "handleResult: updateGallery");
                updateGallery(mView.getContext(), mPicUrl);
                Log.i(StaticClass.TAG, "handleResult: bitmap");
                bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
                return bitmap;
            } else {
                Log.i(StaticClass.TAG, "handleResult: else");
                bitmap = BitmapFactory.decodeResource(mView.getContext().getResources(), R.drawable.avatar);
                return bitmap;
            }
        }
        Log.i(StaticClass.TAG, "handleResult: return");
        return bitmap;

    }

    MultipartBody mMultipartBody;
    Request request;
    Call call;
    String error;
    // 注册
    @Override
    public void doRegister(String name) {
        this.name = name;
        doVerify(new File(StaticClass.FILE_PATH + "Camera" + ".jpg"));
    }
        /*
         * 注册
         * 1.验证人脸：验证是否一张人脸，并获取face_token
         *
         */
        private void doVerify(final File mFile) {
            //实例化复合请求体
            mMultipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(mApiKey, API_KEY)
                    .addFormDataPart(mApiSecret, API_SECRET)
                    .addFormDataPart(mAttributes, "gender,age")
                    .addFormDataPart(mImageFile, mFile.getName(), RequestBody.create(MEDIA_TYPE_JPEG, mFile))
                    .build();
            //构建请求体
            request = new Request.Builder()
                    .url(url_base + url_detect)
                    .post(mMultipartBody)
                    .build();
            //new call
            call = client.newCall(request);
            //请求加入调度
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {
                    if (e.toString().contains("closed") || e.toString().contains("Canceled")) {
                        mView.onRegisterFailure("请求被取消,原因是：" + e);
                    } else {
                        mView.onRegisterFailure("验证失败,原因是：" + e);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 200) { //如果返回正确的响应值
                        //转化返回结果
                        DetectFace detectFace = mParseResponsePresenter.ParseResponse(response, DetectFace.class);
                        if (detectFace.getFaces().size() == 1) { //判断这张照片是否一个人
                            face_token = detectFace.getFaces().get(0).getFace_token();
                            age = detectFace.getFaces().get(0).getAttributes().getAge().getValue();
                            doSearch(mFile);
                        } else { //如果不止一个人
                            mView.onRegisterFailure("照片中出现的人数超过1人，请重新拍摄");
                        }
                        /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                    } else if (response.code() == 403) {
                        doVerify(mFile);
                    } else {
                        mView.onRegisterFailure("网络出现问题，请重试！");
                    }
                }
            });
        }

            /*
             * 注册
             * 2.搜索人脸：看看是否找到一样的人脸，以判断是否已经注册过
             * @param file 照片所在路径文件
             */
            private void doSearch(final File mFile) {
                //实例化复合请求体
                mMultipartBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(mApiKey, API_KEY)
                        .addFormDataPart(mApiSecret, API_SECRET)
                        .addFormDataPart(mOuterId, OUTER_ID)
                        .addFormDataPart(mImageFile, mFile.getName(), RequestBody.create(MEDIA_TYPE_JPEG, mFile))
                        .build();
                //构建请求体
                request = new Request.Builder()
                        .url(url_base + url_search)
                        .post(mMultipartBody)
                        .build();
                //new call
                call = client.newCall(request);
                //请求加入调度
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (e.toString().contains("closed") || e.toString().contains("Canceled")) {
                            mView.onRegisterFailure("请求被取消,原因是：" + e);
                        } else {
                            mView.onRegisterFailure("验证失败,原因是：" + e);
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.code() == 200) { // 如果返回正确的响应值
                            SearchFace searchFace = mParseResponsePresenter.ParseResponse(response, SearchFace.class);
                            if (searchFace.getResults().get(0).getConfidence() < 85) { // 判断这张这个人是否已注册
                                doRealRegister();
                            } else {
                                mView.onRegisterFailure("你已经注册过");
                            }
                        } else if (response.body().string().contains("EMPTY_FACESET")) {// 如果返回一个空集合，说明这个集合里面没有添加过人脸，可以注册*/
                            doRealRegister();
                        } else if (response.code() == 403) { // 如果返回了错误的响应值，例如并发问题，或者超时问题，重复这一步
                            doSearch(mFile);
                        } else {
                            error = response.body().string();
                            mView.onRegisterFailure("网络出现问题，请重试！/n" + error);
                        }
                    }
                });
            }

                /*
                 * 注册
                 * 3.注册人脸到人脸库
                 */
                private void doRealRegister() {
                    FormBody mFormBody = new FormBody.Builder() //构造FormBody
                            .add(mApiKey, API_KEY)
                            .add(mApiSecret, API_SECRET)
                            .add(mOuterId, OUTER_ID)
                            .add(mFactTokens, face_token)
                            .build();

                    request = new Request.Builder().url(url_base + url_add_face).post(mFormBody).build(); //构造Url,以及传入构造FormBody
                    call = client.newCall(request); //new call
                    call.enqueue(new Callback() { //请求加入调度
                        @Override
                        public void onFailure(Call call, IOException e) {
                            if (e.toString().contains("closed") || e.toString().contains("Canceled")) {
                                mView.onRegisterFailure("请求被取消,原因是：" + e);
                            } else {
                                mView.onRegisterFailure("验证失败,原因是：" + e);
                            }
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.code() == 200) { // 如果返回正确的响应值
                                AddFace addFace = mParseResponsePresenter.ParseResponse(response, AddFace.class);
                                if (addFace.getFace_added() == 1) { //判断这张照片是否已经成功添加到人脸库
                                    addFaceToDB();
                                    mView.onRegisterSuccess("恭喜，注册成功！");
                                } else { //如果人脸添加到人脸库失败
                                    mView.onRegisterFailure("添加人脸失败，请重试");
                                }
                            } else if (response.code() == 403) { //如果返回了错误的响应值，例如并发问题，或者超时问题，重复这一步骤:注册
                                doRealRegister();
                            } else {
                                mView.onRegisterFailure("网络出现问题，请重试！");
                            }
                        }
                    });
                }

                    /*
                     * 注册
                     * 4.添加到数据库：把face_token（同时也是照片的地址），性别，年龄，称呼添加到数据库
                     */
                    private void addFaceToDB() {
                        /*增加*/
                        FaceUser user = new FaceUser(null, face_token, gender, age, name);
                        mFaceUserDao.insert(user);
                    }
}
