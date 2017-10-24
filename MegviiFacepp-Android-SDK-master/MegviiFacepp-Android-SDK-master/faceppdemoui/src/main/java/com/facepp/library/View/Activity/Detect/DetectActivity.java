package com.facepp.library.View.Activity.Detect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture; /*Surface相关*/
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView; /*Surface相关*/
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facepp.library.Model.Entity.DaoSession;
import com.facepp.library.Model.Entity.FaceUser;
import com.facepp.library.Model.Entity.FaceUserDao;
import com.facepp.library.Model.Entity.SearchFace;
import com.facepp.library.Model.Util.CameraMatrix;
import com.facepp.library.Model.Util.ConUtil;
import com.facepp.library.Model.Util.DialogUtil;
import com.facepp.library.Model.Util.GreenDaoUtil;
import com.facepp.library.Model.Util.ICamera;
import com.facepp.library.Model.Util.OpenGLUtil;
import com.facepp.library.Model.Util.Screen;
import com.facepp.library.Model.Util.SensorEventUtil;
import com.facepp.library.Model.Util.Util;
import com.facepp.library.Model.Util.YuvUtil;
import com.facepp.library.R;
import com.facepp.library.View.Activity.RegisterActivity.RegisterActivity;
import com.google.gson.Gson;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.megvii.facepp.sdk.Facepp;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static com.facepp.library.Model.Util.Util.API_KEY;
import static com.facepp.library.Model.Util.Util.API_SECRET;
import static com.facepp.library.Model.Util.Util.MEDIA_TYPE_JPEG;
import static com.facepp.library.Model.Util.Util.OUTER_ID;
import static com.facepp.library.Model.Util.Util.TAG;
import static com.facepp.library.Model.Util.Util.mApiKey;
import static com.facepp.library.Model.Util.Util.mApiSecret;
import static com.facepp.library.Model.Util.Util.mImageFile;
import static com.facepp.library.Model.Util.Util.mOuterId;
import static com.facepp.library.Model.Util.Util.mPicUrl;
import static com.facepp.library.Model.Util.Util.url_base;
import static com.facepp.library.Model.Util.Util.url_search;

public class DetectActivity extends Activity
        implements
        PreviewCallback,/*返回摄像头的预览数据*/
        Renderer,
        SurfaceTexture.OnFrameAvailableListener /*SurfaceTexture.OnFrameAvailableListener用于让SurfaceTexture的使用者知道有新数据到来。*/ {
    private int printTime = 31; /*刷新页面的时间时间，31毫秒挺流畅的*/

    Handler timeHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                /*自动刷新，依赖printTime的数值，越小越流畅*/
                case 0:
                    mGlSurfaceView.requestRender();// 发送去绘制照相机不断去回调
                    timeHandle.sendEmptyMessageDelayed(0, printTime);
                    break;
                /*除了自动刷新，每次onPreviewFrame处理完数据之后，都会手动刷新一次*/
                case 1:
                    mGlSurfaceView.requestRender();// 发送去绘制照相机不断去回调
                    break;
            }
        }
    };
    private FaceUserDao mFaceUserDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        /*1.1屏幕初始化*/
        Screen.initialize(this);
        /*1.2设置布局*/
        setContentView(R.layout.activity_opengl);
        /*1.3view初始化*/
        init();
        /*初始化语音*/
        initVoice();
        /*get GreenDao*/
        initGreenDao();
    }

    private DaoSession daoSession;

    private void initGreenDao() {
        daoSession = GreenDaoUtil.getDaoSession(this);
        mFaceUserDao = daoSession.getFaceUserDao();
    }

    private SpeechSynthesizer mTts;

    private void initVoice() {
        mTts = SpeechSynthesizer.createSynthesizer(this, null);
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mTts.setParameter(SpeechConstant.SPEED, "55");
        mTts.setParameter(SpeechConstant.VOLUME, "80");
        mTts.setParameter(SpeechConstant.ENGINE_MODE, SpeechConstant.MODE_AUTO);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
    }

    private Facepp facepp; /*实例化一个Facepp*/
    private boolean isBackCamera;/*是不是后置摄像头*/
    private int detection_interval = 25;/*检测间隔*/
    private HashMap<String, Integer> resolutionMap; /*貌似跟录像有关*/
    private SensorEventUtil sensorUtil;/*实例化一个传感器工具类，应该和3d建模有关*/
    /*在子线程实例化一个命名为(facepp)的线程*/
    private HandlerThread mHandlerThread = new HandlerThread("facepp");
    private Handler mHandler; /*实例化一个mHandler*/
    private GLSurfaceView mGlSurfaceView; /*GLSurfaceView*/
    private ICamera mICamera; /*实例化一个摄像头类*/
    private Button btn_register;
    private DialogUtil mDialogUtil; /*实例化一个对话框类*/

    private void init() {
        Log.i(TAG, "init");

        /*1.3.1检查手机版本信息是否等于PLK-AL10，如果等于的话，刷新时间设定为50*/
        if (android.os.Build.MODEL.equals("PLK-AL10"))
            printTime = 50;
        /*1.3.2获取一些默认设置，包括是否录制，3d模型，调试模式，区域选择等*/
//        isBackCamera = getIntent().getBooleanExtra("isBackCamera", false);
//        trackModel = getIntent().getStringExtra("trackModel");
//        detection_interval = getIntent().getIntExtra("interval", detection_interval);
//        resolutionMap = (HashMap<String, Integer>) getIntent().getSerializableExtra("resolution");
        /*手动设置，不获取FaceppActionActivity的数据*/
        isBackCamera = false;
        trackModel = "Normal";
        detection_interval = 100;
        resolutionMap = null;


        facepp = new Facepp();

        /*1.3.3*/
        sensorUtil = new SensorEventUtil(this);

        /*1.3.4 开启人脸检测的线程*/
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        /*1.3.5绑定控件及设置mGlSurfaceView的参数*/
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.opengl_layout_surfaceview);
        /*生命要使用的是openGLES的2.0版本*/
        mGlSurfaceView.setEGLContextClientVersion(2);// 创建一个OpenGL ES 2.0
        // context
        /*设置这个Activity为渲染器，则渲染器会回调这里的函数*/
        mGlSurfaceView.setRenderer(this);// 设置渲染器进入gl
        /* RENDERMODE_CONTINUOUSLY不停渲染 */
        /* RENDERMODE_WHEN_DIRTY懒惰渲染，需要手动调用 glSurfaceView.requestRender() 才会进行更新 */
        mGlSurfaceView.setRenderMode(mGlSurfaceView.RENDERMODE_WHEN_DIRTY);// 设置渲染器模式

        mICamera = new ICamera();                                                                                      /*1.3.6*/

        mDialogUtil = new DialogUtil(this);                                                                             /*1.3.7*/

        btn_register = (Button) findViewById(R.id.btn_register);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DetectActivity.this, RegisterActivity.class));
            }
        });


    }

    /**
     * 内容：
     * 1.保持屏幕常量
     * 2.打开摄像头，并获得摄像头的参数。
     * 3.把摄像头宽高设置给mGlSurfaceView。
     * 4.facepp的初始化和初始设置。
     */
    private int Angle;
    int rotation = Angle;
    private Camera mCamera;
    private String trackModel;  /*轨迹模式*/

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        /*这个工具类的方法是用于保持屏幕常量，可以是灰的*/

        ConUtil.acquireWakeLock(this);
        /*打开摄像头*/
        mCamera = mICamera.openCamera(isBackCamera, this, resolutionMap);
        if (mCamera != null) {
            /*返回摄像头的角度*/
            Angle = 360 - mICamera.Angle;
            if (isBackCamera)
                Angle = mICamera.Angle;
            /*通过摄像头的宽高，设置mGlSurfaceView的宽高*/
            RelativeLayout.LayoutParams layout_params = mICamera.getLayoutParam();
            mGlSurfaceView.setLayoutParams(layout_params);



            /*设置区域选择的属性*/
            /*获得摄像头的最佳宽高*/
            int width = mICamera.cameraWidth;
            int height = mICamera.cameraHeight;
            int left = 0;
            int top = 0;
            int right = width;
            int bottom = height;


            /*错误代码，通过facepp.init初始化*/
            String errorCode = facepp.init(this, ConUtil.getFileContent(this, R.raw.megviifacepp_0_4_7_model));
            /*获取facepp的设置类*/
            Facepp.FaceppConfig faceppConfig = facepp.getFaceppConfig();
            faceppConfig.interval = detection_interval;
            faceppConfig.roi_left = left;
            faceppConfig.roi_top = top;
            faceppConfig.roi_right = right;
            faceppConfig.roi_bottom = bottom;
            /*如果是单脸跟踪*/
            faceppConfig.one_face_tracking = 0;

            /*字符串数组array拿到跟踪模式是：Normal/Robust/Fast，并设置跟踪模式*/
            //！！！！这个trackModel得设置是一个很重要的部分，如果删除后，
            // 侦察人脸特别快，但不知道会不会影响获取人脸的质量，待一切运行顺畅后，可以尝试摘取此段代码，
            // 提高效率。
            String[] array = getResources().getStringArray(R.array.trackig_mode_array);
            if (trackModel.equals(array[0]))
                faceppConfig.detectionMode = Facepp.FaceppConfig.DETECTION_MODE_TRACKING;
            else if (trackModel.equals(array[1]))
                faceppConfig.detectionMode = Facepp.FaceppConfig.DETECTION_MODE_TRACKING_ROBUST;
            else if (trackModel.equals(array[2]))
                faceppConfig.detectionMode = Facepp.FaceppConfig.DETECTION_MODE_TRACKING_FAST;
            /*把跟踪模式设置到facepp*/
            facepp.setFaceppConfig(faceppConfig);
        } else {
            /*如果摄像头为空，显示错误dialog*/
            mDialogUtil.showDialog(getResources().getString(R.string.camera_error));
        }
    }

    /**
     * onResume以后走的是这个方法,GLSurface创建以后的回调方法
     * 内容：
     * 1.创造纹理
     * 2.把SurfaceTexture按照纹理创造出来，并添加接口，才能展示预览，并让相机开始预览
     * 3.创建绘制画面和绘制人脸关键点的矩阵类
     * 4.触发第一次画面刷新
     * 5.通过设置这个Activity作为相机的预览回调，让相机的预览帧在下面的回调方法里得到处理
     * 6.区域模式的话，绘制矩形
     *
     * @param gl
     * @param config
     */
    private int mTextureID = -1;
    private SurfaceTexture mSurface;
    private CameraMatrix mCameraMatrix;

    private boolean isTiming = false; /*是否是定时去刷新界面;*/

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        /*
        * 据说是起到清除缓冲区颜色的作用。
        * 必须强调 glClearColor只起到Set的作用，并不Clear任何！不要混淆~
        * 清除颜色缓冲区的作用是，防止缓冲区中原有的颜色信息影响本次绘图（注意！即使认为可以直接覆盖原值，也是有
        * 可能会影响的），当绘图区域为整个窗口时，就是通常看到的，颜色缓冲区的清除值就是窗口的背景颜色。所以，这两条
        * 清除指令并不是必须的：比如对于静态画面只需要设置一次，比如不需要背景色/背景色为白色。
        *
        * 现在明白原来是和后面onDrawFrame方法的
        * GLES20.glClear(GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT)配合着使用的。
        *
        * 但是把以上两条都注释掉，好像完全没有差别，估计影响不大
        */
        // 黑色背景
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        /*创造一个纹理*/
        mTextureID = OpenGLUtil.createTextureID();
        /*根据这个纹理创造一个SurfaceTexture*/
        mSurface = new SurfaceTexture(mTextureID);
        /*设定mSurface的Listener
        * 由于对于回调函数onFrameAvailable里面并没有干什么事，所以
        * 把这一条注释掉也没关系什么，只要记得更新画面（mSurface.updateTexImage();）就行了。
        */
        // 这个接口就干了这么一件事，当有数据上来后会进到onFrameAvailable方法
        mSurface.setOnFrameAvailableListener(this);// 设置照相机有数据时进入
        /*我也不知道干了些什么，可能是跟3d有关的*/
        mCameraMatrix = new CameraMatrix(mTextureID);
        /*
        * 开始预览，这一条必须开启，不开启的话，都没有预览画面了
        * */
        mICamera.startPreview(mSurface);// 设置预览容器
        /*开始检测人脸,其实并没有开始，只是配置了camera的setPreviewCallback，然后在里面检测人脸*/
        mICamera.actionDetect(this);
        if (isTiming) {
            /*触发第一次刷新，触发后会自动不断按照间隔值(printTime=31)刷新*/
            //会引起onDrawFrame被不断触发
            timeHandle.sendEmptyMessageDelayed(0, printTime);
        }
    }

    /**
     * 走完onSurfaceCreated之后，会走这个方法，第一次拿到Surface的属性
     * <p>
     * 内容：
     * 1.设置Gl的投影宽高，否则影像会超出或少于GLSurfaceView的范围
     * 2.设置投影转换矩阵
     *
     * @param gl
     * @param width
     * @param height
     */
    private final float[] mProjMatrix = new float[16];  /*这个也不知道是什么矩阵*/

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
        /*设置画面的大小
        * glViewport它负责把视景体截取的图像按照怎样的高和宽显示到屏幕上。
        */
        GLES20.glViewport(0, 0, width, height);

        /*这一条的计算公式是错的，下面又重新赋值了。*/
        //ratio是比例的意思
        float ratio = (float) width / height;
        /*这个值会影响人脸关键点的宽高比例*/
        ratio = 1; // 这样OpenGL就可以按照屏幕框来画了，不是一个正方形了

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        /*这是一个投影转换矩阵，其中的学问是关于gl和Matrix，比较深入*/
//        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);


        // Matrix.perspectiveM(mProjMatrix, 0, 0.382f, ratio, 3, 700);
    }

    /**
     * 这个方法很明确是被GLSurfaceView.requestRender()触发的。
     * 这个方法会绘制人脸关键点，以及整个画面，然后再更新画面
     * <p>
     * 内容：
     * 1.绘制摄像头预览画面和人脸关键点
     * 2.更新画面
     *
     * @param gl
     */
    private final float[] mMVPMatrix = new float[16]; /*这个不知道是什么矩阵*/
    private final float[] mVMatrix = new float[16];  /*这个也不知道是什么矩阵*/

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "onDrawFrame----");
        /*前面进行了GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);*/
        // Log.w("ceshi", "onDrawFrame===");
        GLES20.glClear(GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);// 清除屏幕和深度缓存

        float[] mtx = new float[16];

        /*
        当对纹理用amplerExternalOES采样器采样时，应该首先使用getTransformMatrix(float[])查询得到的矩阵
        来变换纹理坐标，每次调用updateTexImage()的时候，可能会导致变换矩阵发生变化，因此在纹理图像更新时需要
        重新查询，该矩阵将传统的2D OpenGL ES纹理坐标列向量(s,t,0,1)，其中s，t∈[0,1]，变换为纹理中对应的采
        样位置。该变换补偿了图像流中任何可能导致与传统OpenGL ES纹理有差异的属性。

        起到重新规整纹理的作用

        注释掉它能看出来画面是混乱的，但是人脸关键点是清晰的。
         */
        mSurface.getTransformMatrix(mtx);

        /*跟预览画面有关的绘制，注释掉的话就看不到预览画面了,只剩人脸关键点了*/
        mCameraMatrix.draw(mtx);


        // Set the camera position (View matrix)

//        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1f, 0f);
        // Calculate the projection and view transformation
//        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
        /*跟人脸点有关的绘制，注释掉就看不到人脸点了*/

        /*mSurface = new SurfaceTexture
        * 效果体现在预览画面的更新，
        * 注释掉它就没有预览画，只剩下人脸关键点
        */
        // 更新image，会调用onFrameAvailable方法，
        mSurface.updateTexImage();


    }

    /**
     * 这个方法很明确是被SurfaceTexture.updateTexImage触发的。
     * 此时画面已经被更新了，也就是画面已经处理完毕了，
     * 在这里我们没必要对传入对surfaceTexture做什么处理了
     *
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // TODO Auto-generated method stub

    }

    /**
     * 这个方法是在mCamera.setPreviewCallback(mActivity);之后触发的。
     * 这里拿到的预览画面，应该是onDrawFrame处理过的画面，至少，是onDrawFrame之后，
     * GLES20或者mtx被处理过后，才到这个方法。
     * 这个方法主要是把预览画面发出去，并处理接收回来的数据。
     * 如果把mCamera.setPreviewCallback(mActivity)注释掉，也就是不走这个方法的话
     * <p>
     * 这个方法的意义完全只在于它所传回来的参数imgData，如果单纯是预览画面，是不需要走
     * 这个方法的，但是为了后续绘制摄像头内容和绘制人脸关键点，需要把imgData发送出去。
     * 并且拿到返回数据进行下一步处理。
     * <p>
     * 内容：发送人脸数据，并对数据进行一部分的处理。
     *
     * @param imgData
     * @param camera
     */
    boolean isSuccess = false; /*是否成功*/
    float confidence;   /*信心系数*/
    /*
    * pitch; 一个弧度，表示物体顺时针饶x轴旋转的弧度。
    * yaw; 一个弧度，表示物体顺时针饶y轴旋转的弧度。
    * roll; 一个弧度，表示物体顺时针饶z轴旋转的弧度。
    * */
    float pitch, yaw, roll;
    boolean sendSearchRequest = false;
    String fileName;
    long sendRequestTime;

    @Override
    public void onPreviewFrame(final byte[] imgData, final Camera camera) {
        Log.i(TAG, "onPreviewFrame----");

        /*如果isSuccess为True，我也不知道代表这什么，反正若为True就让程序返回*/
        if (isSuccess)
            return;
        /*设定isSuccess为true*/
        isSuccess = true;

        /*开启一个线程*/
        mHandler.post(new Runnable() {

            @Override
            public void run() {

                /*获得摄像头的宽高*/
                int width = mICamera.cameraWidth;
                int height = mICamera.cameraHeight;
                /*通过传感器获得摄像头的角度*/
                int orientation = sensorUtil.orientation;
                /*计算角度*/
                if (orientation == 0)
                    rotation = Angle;
                else if (orientation == 1)
                    rotation = 0;
                else if (orientation == 2)
                    rotation = 180;
                else if (orientation == 3)
                    rotation = 360 - Angle;
                /*把计算结果设置到摄像头*/
                setConfig(rotation);
                /*人脸检测，返回一个faces，是一个face的数组*/
                //把onPreviewFrame获得的imgData传进去应该是最重要的
//                final Facepp.Face[] faces = facepp.detect(imgData,height ,width,Facepp.IMAGEMODE_NV21);
//                fileName = saveToJPEGandOutput(imgData, height,width );

                final Facepp.Face[] faces = facepp.detect(imgData, width, height, Facepp.IMAGEMODE_NV21);

                /*判断faces返回的数据是否为空*/
                if (faces != null) {
                    /*新建一个动态数组 和人脸关键点有关的*/
                    confidence = 0.0f;
                    /*faces的长度如果大于等于0*/
                    if (faces.length >= 0) {
                        /*遍历faces*/
                        for (int c = 0; c < faces.length; c++) {
                            if (!sendSearchRequest) {
                                sendSearchRequest = true;
                                sendRequestTime = System.currentTimeMillis();
                                Log.i(TAG, "run:: sendSearchRequest = true;");
                                Log.i(TAG, "run:: progressDialog正要开启");
                                showProgressDialog("检测中", "正在验证人脸");
                            /*保存为JPEG照片*/
                                fileName = saveToJPEGandOutput(imgData, width, height);
                                Log.i(TAG, "检测：保存了一张新照片");
                                Log.i(TAG, "检测：可以检测");
                                /*发送请求，看看是否认识的人*/
                                sendSearchRequest(fileName);
                            } else {
                                Log.i(TAG, "检测：正在发送请求，不用检测 ");
                            }
                            /*
                            * pitch; 一个弧度，表示物体顺时针饶x轴旋转的弧度。
                            * yaw; 一个弧度，表示物体顺时针饶y轴旋转的弧度。
                            * roll; 一个弧度，表示物体顺时针饶z轴旋转的弧度。
                            * */
                            pitch = faces[c].pitch;
                            yaw = faces[c].yaw;
                            roll = faces[c].roll;
                            confidence = faces[c].confidence;
                            /*从方向计算宽高的代码*/
                            if (orientation == 1 || orientation == 2) {
                                width = mICamera.cameraHeight;
                                height = mICamera.cameraWidth;
                            }

                        }
                    /*faces的长度如果小于0*/
                    } else {
                        pitch = 0.0f;
                        yaw = 0.0f;
                        roll = 0.0f;
                    }
                }
                isSuccess = false;
                if (!isTiming) {
                    timeHandle.sendEmptyMessage(1);
                }
            }
        });
    }

    /**
     * 作用：发送Search请求，看看是否认识的人，如果是，提示欢迎回来，如果不是，提示你好新朋友
     */
    private final OkHttpClient client = new OkHttpClient();
    private File mFile;
    private String face_token_new = "", face_token_know = "";
    long newTokenTime = System.currentTimeMillis();
    long knowTokenTime = System.currentTimeMillis();

    private void sendSearchRequest(String fileName) {
        mFile = new File(Util.FILE_PATH + "Detect" + ".jpg");
//        mFile = new File(fileName);
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
                sendSearchRequest = false;
                hideProgressDialog();
                Log.i(TAG, "onFailure: sendSearchRequest = false;");
                Log.i(TAG, "onFailure: progressDialog已经关闭");
                Log.i(TAG, "验证失败！");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                /*如果返回正确的响应值*/
                if (response.code() == 200) {
                    /*判断这个人是否已注册*/
                    SearchFace searchFace;
                    searchFace = parseWithGson(response, SearchFace.class);

                    if (searchFace == null) {
                        sendSearchRequest = false;
                        hideProgressDialog();
                        return;
                    }
                    Log.i(TAG, "onResponse: " + searchFace.getFaces().size());
                    if (searchFace.getFaces().size() == 0) {
                        sendSearchRequest = false;
                        hideProgressDialog();
                        return;
                    }
                    if (searchFace.getResults().get(0).getConfidence() < 85) {
                        synchronized (this) {
//                            /*如果是同一个face_token，则不用再判断了，浪费时间*/
//                            if (System.currentTimeMillis() - newTokenTime > 5000) {
////                                Log.i(TAG, "sendSearchRequest: 判断，我来清空了一下数值");
//                                newTokenTime = System.currentTimeMillis();
//                                face_token_new = "";
//                            }
////                            Log.i(TAG, "onResponse: 判断，此时getFace_token="+searchFace.getResults().get(0).getFace_token());
////                            Log.i(TAG, "onResponse: 判断，此时Face_token_new="+face_token_new);
//                            if (searchFace.getResults().get(0).getFace_token().equals(face_token_new)) {
//                                sendSearchRequest = false;
//                                hideProgressDialog();
////                                Log.i(TAG, "新朋友判断:做了一次判断，不可执行下去 ");
////                                Log.i(TAG, "新朋友判断:因为 "+searchFace.getResults().get(0).getFace_token()+"等于"+face_token_new);
//                                return;
//                            }
//                            Log.i(TAG, "新朋友判断:因为 "+searchFace.getResults().get(0).getFace_token()+"不等于"+face_token_new);
//                            Log.i(TAG, "新朋友判断:可执行下去，但下次不可执行了");
                            face_token_new = searchFace.getResults().get(0).getFace_token();
//                            Log.i(TAG, "新朋友判断:，现在 face_token_new="+face_token_new);
                            if (!isSpeaking) {
                                startSpeak("你好新朋友，请注册");

                            }else{
                                Log.i(TAG, "onResponse: 正在讲话");
                                sendSearchRequest = false;
                                hideProgressDialog();
                            }
                        }
                    } else {
                        synchronized (this) {
//                            /*如果是同一个face_token，则不用再判断了，浪费时间*/
//                            if (sameFaceIn5Seconds()){
//                                sendSearchRequest = false;
//                                hideProgressDialog();
//                            }
//                            if (System.currentTimeMillis() - knowTokenTime > 5000) {
////                                Log.i(TAG, "sendSearchRequest: 判断，我来清空了一下数值");
//                                knowTokenTime = System.currentTimeMillis();
//                                face_token_know = "";
//                            }
////                            Log.i(TAG, "onResponse: 判断，此时getFace_token="+searchFace.getResults().get(0).getFace_token());
////                            Log.i(TAG, "onResponse: 判断，此时Face_token_new="+face_token_know);
//                            if (searchFace.getResults().get(0).getFace_token().equals(face_token_know)) {
//                                sendSearchRequest = false;
//                                hideProgressDialog();
////                                Log.i(TAG, "老朋友判断:做了一次判断，不可执行下去 ");
////                                Log.i(TAG, "老朋友判断:因为"+searchFace.getResults().get(0).getFace_token()+"等于"+face_token_know);
//                                return;
//                            }
//                            Log.i(TAG, "老朋友判断:因为"+searchFace.getResults().get(0).getFace_token()+"不等于"+face_token_know);
//                            Log.i(TAG, "老朋友判断: 可执行下去，但下次不可执行了");
                            face_token_know = searchFace.getResults().get(0).getFace_token();
//                            Log.i(TAG, "老朋友判断:，现在 face_token_new="+face_token_know);

                            if (!isSpeaking) {
                                getUserName();
                                startSpeak("您好" + userName + ",欢迎回来！");
                            }else{
                                Log.i(TAG, "onResponse: 正在讲话");
                                sendSearchRequest = false;
                                hideProgressDialog();
                            }
                        }


                    }
                    /*如果返回了错误的响应值，例如并发问题，或者超时问题*/
                } else if (response.code() == 403) {
                    sendSearchRequest = false;
                    hideProgressDialog();
                } else {
                    sendSearchRequest = false;
                    hideProgressDialog();
                }
            }
        });

    }

    private String userName;

    private void getUserName() {
//         /*全部查询*/
//        List<FaceUser> mList = mFaceUserDao.queryBuilder().listLazy();
//        for (FaceUser u : mList) {
//            Log.i("ggl", "onCreate: " + u.getName() + "....." + u.getFaceToken() + "....." + u.getAge() + "..." + u.getGender());

        /*条件查询*/
        FaceUser user = mFaceUserDao.queryBuilder().where(FaceUserDao.Properties.FaceToken.eq(face_token_know)).build().unique();
        userName = user.getName();

    }

    /*为了不被打断，判断是否正在朗读，false代表没有正在朗读，可以进行下一段朗读，true则代表不接受新的朗读请求*/
    private Boolean isSpeaking = false;
    private long startSpeakTime;

    private void startSpeak(String string) {
        mTts.startSpeaking(string, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                sendSearchRequest = false;
                isSpeaking = true;
                hideProgressDialog();
                Log.i(TAG, "onSpeakBegin: sendSearchRequest = false;");
                Log.i(TAG, "onSpeakBegin: progressDialog已经关闭");
                startSpeakTime = System.currentTimeMillis();

                Toast.makeText(DetectActivity.this, "检测耗时：" + (startSpeakTime - sendRequestTime), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {
            }

            @Override
            public void onSpeakPaused() {
                isSpeaking = false;
            }

            @Override
            public void onSpeakResumed() {
                isSpeaking = true;
            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {
                isSpeaking = false;
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
    }

    private String saveToJPEGandOutput(byte[] imgData, int width, int height) {
//        String fileName = System.currentTimeMillis() + ".jpg";  //jpeg文件名定义
        String fileName = "Detect" + ".jpg";  //jpeg文件名定义
        File sdRoot = Environment.getExternalStorageDirectory();    //系统路径
        String dir = mPicUrl;   //文件夹名
        File mkDir = new File(sdRoot, dir);
        if (!mkDir.exists()) {
            mkDir.mkdirs();   //目录不存在，则创建
        }
        ///storage/emulated/0/ggljpeg/storage/emulated/0/ggljpeg/Detect.jpg:
        /*旋转图像的方向*/
//        imgData=rotate270(imgData,width,height);
       /*澳博的平板需要旋转180度，但是android6.0的不需要旋转180度*/
//        imgData = rotate180(imgData, width, height);

        File pictureFile = new File(sdRoot, dir + fileName);

        try {
            if (pictureFile.exists()) {
                pictureFile.delete();
                pictureFile.createNewFile();
            }
//                FileOutputStream filecon = new FileOutputStream(pictureFile);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pictureFile));
            //由于旋转了270度，宽高要互换，同理，90度也是
            YuvImage image = new YuvImage(imgData, ImageFormat.NV21, width, height, null);   //将NV21 data保存成YuvImage
            //图像压缩
            Log.i(TAG, "照片有了 ");
            image.compressToJpeg(
                    new Rect(0, 0, image.getWidth(), image.getHeight()),
                    70, bos);   // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "检测： " + mkDir + "/" + fileName);
        return mkDir + "/" + fileName;
    }

    private byte[] rotate180(byte[] imgData, int width, int height) {
        byte[] Data = YuvUtil.rotateYUV420spRotate180(imgData, width, height);
        return Data;
    }

    private byte[] rotate270(byte[] imgData, int width, int height) {
        byte[] Data = YuvUtil.rotateYUV420Degree270(imgData, width, height);
        return Data;
    }

    private <T> T parseWithGson(Response response, Class<T> clazz) throws IOException {
        Gson mGson = new Gson();
        T t = null;
        if (mGson != null) {
            t = mGson.fromJson(response.body().string(), clazz);

        }
        return t;
    }

    /**
     * 作用：对返回结果进行处理
     * 流程：
     * 1.判断是否正确的返回结果
     * 2.判断是否是认识的人
     * 2.1如果是：
     * 2.1.1：给出提示。(暂用Toast）
     * 2.1.2：返回这个face_token所关联的图片名。getFileNameFromDB();
     * 2.1.3：通过图片名把图片呈现出来 showPhoto();
     * 2.2如果不是，给出提示。（暂用Toast）
     */
    private void handelSearchResult() {
//        getPhotoFromDB("");
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
     * 作用：通过face_token,查找数据库，并返回关联的图片名
     */
    private String getPhotoFromDB(String face_token) {
        return null;
    }

    /**
     * 此方法被onPreviewFrame触发
     *
     * @param rotation
     */
    private void setConfig(int rotation) {
        Log.i(TAG, "setConfig");
        Facepp.FaceppConfig faceppConfig = facepp.getFaceppConfig();
        if (faceppConfig.rotation != rotation) {
            faceppConfig.rotation = rotation;
            facepp.setFaceppConfig(faceppConfig);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        ConUtil.releaseWakeLock();

        mICamera.closeCamera();
        mCamera = null;

        timeHandle.removeMessages(0);

//        finish();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        facepp.release();
    }

    private ProgressDialog progressDialog;

    /*
        * 提示加载
        */
    public void showProgressDialog(String title, String message) {
        if (progressDialog == null) {

            progressDialog = ProgressDialog.show(DetectActivity.this,
                    title, message, true, true);
        } else if (progressDialog.isShowing()) {
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(true);
        }

        progressDialog.show();
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                client.dispatcher().cancelAll();
            }
        });

    }

    /*
     * 隐藏提示加载
     */
    public void hideProgressDialog() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

    }

}


