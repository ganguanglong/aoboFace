package com.facepp.library.mvp.view.activity.detect;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facepp.library.mvp.model.entity.DaoSession;
import com.facepp.library.mvp.model.entity.FaceUser;
import com.facepp.library.mvp.model.entity.FaceUserDao;
import com.facepp.library.mvp.view.activity.base.BaseSpeakActivity;
import com.facepp.library.util.CameraMatrix;
import com.facepp.library.util.ConUtil;
import com.facepp.library.util.StaticClass;
import com.facepp.library.util.DialogUtil;
import com.facepp.library.util.GreenDaoUtil;
import com.facepp.library.util.ICamera;
import com.facepp.library.util.OpenGLUtil;
import com.facepp.library.util.Screen;
import com.facepp.library.util.SensorEventUtil;
import com.facepp.library.mvp.presenter.BasePresenter;
import com.facepp.library.mvp.model.transaction.compress.Save2JpegPresenterForAB2;
import com.facepp.library.mvp.model.transaction.compress.Video2JpegContract;
import com.facepp.library.mvp.model.transaction.parse.ParseByGsonPresenter;
import com.facepp.library.mvp.model.transaction.parse.ParseResponseContract;
import com.facepp.library.mvp.model.transaction.request.CompareByOKhttpPresenter;
import com.facepp.library.mvp.model.transaction.request.CompareRequestContract;
import com.facepp.library.mvp.model.transaction.request.SearchByOKHttpPresenter;
import com.facepp.library.mvp.model.transaction.request.SearchRequestContract;
import com.facepp.library.mvp.model.transaction.speak.SpeakByiFlyPresenter;
import com.facepp.library.mvp.model.transaction.speak.SpeakContract;
import com.facepp.library.R;
import com.facepp.library.mvp.view.activity.base.BaseActivity;
import com.facepp.library.mvp.view.activity.permission.PermissionActivity;
import com.facepp.library.mvp.view.activity.register.RegisterActivity;
import com.iflytek.cloud.SpeechError;
import com.megvii.facepp.sdk.Facepp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static com.facepp.library.util.StaticClass.TAG;

public class DetectActivity extends BaseSpeakActivity
        implements
        PreviewCallback,/*返回摄像头的预览数据*/
        Renderer,
        SurfaceTexture.OnFrameAvailableListener,/*SurfaceTexture.OnFrameAvailableListener用于让SurfaceTexture的使用者知道有新数据到来。*/
        SearchRequestContract.View,
        CompareRequestContract.View {
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
    private Video2JpegContract.Presenter mSave2JpegPresenter = new Save2JpegPresenterForAB2();
    private ParseResponseContract.Presenter mParseResponsePresenter = new ParseByGsonPresenter();
    private SearchRequestContract.Presenter mSearchRequestPresenter = new SearchByOKHttpPresenter();
    private CompareRequestContract.Presenter mCompareRequestPresenter = new CompareByOKhttpPresenter();
    private List<BasePresenter> mPresenterList = new ArrayList<>();
    @Override
    protected void initPresenter() {
        mPresenterList.add((BasePresenter) mSave2JpegPresenter);
        mPresenterList.add((BasePresenter) mCompareRequestPresenter);
        mPresenterList.add((BasePresenter) mParseResponsePresenter);
        mPresenterList.add((BasePresenter) mSearchRequestPresenter);
        registerPresenter(mPresenterList, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!haveAllPermission(this)) { /*检查是否有权限*/
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
        }
        Screen.initialize(this); /*屏幕初始化*/
        setContentView(R.layout.activity_opengl); /*设置布局*/
        initCamera(); /*初始化摄像头*/
        init(); /*view初始化*/
        initPresenter(); /*presenter初始化*/
        initGreenDao(); /*get GreenDao*/
    }

    //查看摄像头个数
    private Camera.CameraInfo cameraInfo = null;
    private int camerAcount = 0;
    private boolean CameraIsBack =false;
    private void initCamera() {
        cameraInfo = new Camera.CameraInfo();
        camerAcount = Camera.getNumberOfCameras();
        Log.i(TAG, "initCamera: 有"+ camerAcount +"个摄像头");
        hascamera();
    }
        private void hascamera() {
            for (int cameranum = 0; cameranum < camerAcount; cameranum++) {
                Camera.getCameraInfo(cameranum, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Log.i(TAG, "hascamera: 有前置摄像头");
                    CameraIsBack = false;
                } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    Log.i(TAG, "hascamera: 有后置摄像头");
                    CameraIsBack = true;
                }
            }
        }

    private DaoSession daoSession;

    private void initGreenDao() {
        daoSession = GreenDaoUtil.getDaoSession(this);
        mFaceUserDao = daoSession.getFaceUserDao();
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

        isBackCamera = CameraIsBack?true:false;
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
    private boolean onResume = false;

    @Override
    protected void onResume() {
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
            //！！！！这个trackModel的设置是一个很重要的部分，如果删除后，
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
            if (!onResume) {
                onResume = true;
            } else {
                mICamera.startPreview(mSurface);// 设置预览容器
                mICamera.actionDetect(this);
            }
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
    private boolean isSuccess = false;/*判断是否正在进行检测，正在检测就不接受其他数据传入了*/
    private boolean startOnLineDetect = false;
    private String fileUrlLast, fileUrlCache;
    private long sendRequestTime;

    @Override
    public void onPreviewFrame(final byte[] imgData, final Camera camera) {
        if (isSuccess)
            return;
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
                final Facepp.Face[] faces = facepp.detect(imgData, width, height, Facepp.IMAGEMODE_NV21);
                /*判断faces返回的数据是否为空，并且人脸数为1，并且人脸置信度高于0.98，才去保存照片*/
                if (faces != null && faces.length == 1 && faces[0].confidence > 0.98) {
                    /*
                     * 比对且搜索方案：时间慢1秒左右，更省钱
                     * 只是搜索方法：时间快，花钱更多
                     * 二选一
                     */

//                    /*后两位：人脸比对信心值、人脸搜索信心值*/
//                    compareAndSearch(imgData,width,height,95,85);
                    /*后一位：人脸搜索信心值*/
                    searchFace(imgData, width, height, 85);

                }
                /*从方向计算宽高的代码*/
                if (orientation == 1 || orientation == 2) {
                    width = mICamera.cameraHeight;
                    height = mICamera.cameraWidth;
                }
                isSuccess = false;

                if (!isTiming) {
                    timeHandle.sendEmptyMessage(1);
                }
            }
        });
    }

    private void searchFace(byte[] imgData, int width, int height, int searchConfidence) {
        if (!startOnLineDetect) {
            startRequest();
            /*保存为JPEG照片*/
            fileUrlCache = mSave2JpegPresenter.save2Jpeg(imgData, StaticClass.detectFileCache, width, height);
            /*发送请求，看看是否认识的人*/
            mSearchRequestPresenter.SendSearchRequest(fileUrlCache, searchConfidence);
        }
    }

    private String userName;

    private String getUserName(String faceTk) {
        /*条件查询*/
        FaceUser user = mFaceUserDao.queryBuilder().where(FaceUserDao.Properties.FaceToken.eq(faceTk)).build().unique();
        userName = user.getName();
        return userName;
    }

    /**
     * 此方法被onPreviewFrame触发
     *
     * @param rotation
     */
    private void setConfig(int rotation) {
        Facepp.FaceppConfig faceppConfig = facepp.getFaceppConfig();
        if (faceppConfig.rotation != rotation) {
            faceppConfig.rotation = rotation;
            facepp.setFaceppConfig(faceppConfig);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ConUtil.releaseWakeLock();

        mICamera.closeCamera();
        mCamera = null;

        timeHandle.removeMessages(0);
    }

    private File f;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileUrlLast != null) {
            f = new File(fileUrlLast);
            if (f.exists() && f.isFile()) {
                f.delete();
            }
        }
        if (fileUrlCache != null) {
            f = new File(fileUrlCache);
            if (f.exists() && f.isFile()) {
                f.delete();
            }
        }
        facepp.release();
    }

    @Override
    public void onFaceSearchFailure() {
        stopRequest();

    }

    @Override
    public void onLowConfidence() {
        speak("你好新朋友，请注册");
    }

    @Override
    public void onFaceSearchFound(String faceToken) {
        userName = getUserName(faceToken);
        speak("您好" + userName + ",欢迎回来！");
    }

    @Override
    public void onFaceSetEmpty() {
        speak("注册为第一个客户吧~！");
    }

    public Boolean getSpeaking() {
        return isSpeaking;
    }


    /*语音回调*/
    /*为了不被打断，判断是否正在朗读，false代表没有正在朗读，可以进行下一段朗读，true则代表不接受新的朗读请求*/
    public Boolean isSpeaking = false;
    private long startSpeakTime;

    @Override
    public void onSpeakBegin() {
        startOnLineDetect = false;
        isSpeaking = true;
        hideProgress();
        startSpeakTime = System.currentTimeMillis();
        toast("检测耗时：" + (startSpeakTime - sendRequestTime), Toast.LENGTH_SHORT);
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
    public void onCompleted(SpeechError speechError) {
        isSpeaking = false;
    }

    public void stopRequest() {
        startOnLineDetect = false;
        hideProgress();
    }

    public void startRequest() {
        startOnLineDetect = true;
        sendRequestTime = System.currentTimeMillis();
        showProgress("检测中", "正在验证人脸");
    }

    public void compareAndSearch(final byte[] imgData, int width, int height, int compareConfidence, final int searchConfidence) {
        if (fileUrlLast == null) {
            fileUrlLast = mSave2JpegPresenter.save2Jpeg(imgData, StaticClass.detectFileNameLast, width, height);
            if (!startOnLineDetect) {
                startRequest();
                mSearchRequestPresenter.SendSearchRequest(fileUrlLast, searchConfidence);
            }
        } else {
            if (!startOnLineDetect) {
                startRequest();
                /*保存为DetectCache.jpg，地址为fileUrlCache*/
                fileUrlCache = mSave2JpegPresenter.save2Jpeg(imgData, StaticClass.detectFileCache, width, height);

                /*
                 *  startOnlineDetect 在线检测
                 *  1.在线比对人脸，判断是否跟之前是同一个人
                 *      a.如果是同一个人，关闭一些UI动态
                 *      b.如果不是同一个人（新的人脸），进行搜索，看看是否认识
                 */
                //进行比对
                final int finalWidth = width;
                final int finalHeight = height;
                mCompareRequestPresenter.sendCompareRequest(fileUrlLast, fileUrlCache, compareConfidence, new CompareRequestContract.CallBack() {
                    /*同一个人*/
                    @Override
                    public void onSamePerson() {
                        Log.i(TAG, "onSamePerson: 人脸比对：同一个人");
                        fileUrlLast = mSave2JpegPresenter.save2Jpeg(imgData, StaticClass.detectFileNameLast, finalWidth, finalHeight);
                        stopRequest();
                        Log.i(TAG, "onSamePerson: 同一个人");

                    }

                    /*比对失败*/
                    @Override
                    public void onCompareFailure() {
                        Log.i(TAG, "onCompareFailure: 人脸比对失败");
                        stopRequest();
                    }

                    /*不是同一个人*/
                    @Override
                    public void onDiffPerson() {
                        mSearchRequestPresenter.SendSearchRequest(fileUrlCache, searchConfidence, new SearchRequestContract.Callback() {
                            @Override
                            public void onFaceSearchFailure() {
                                stopRequest();
                            }

                            @Override
                            public void onLowConfidence() {
                                fileUrlLast = mSave2JpegPresenter.save2Jpeg(imgData, StaticClass.detectFileNameLast, finalWidth, finalHeight);
                                speak("你好新朋友，请注册");
                            }

                            @Override
                            public void onFaceSearchFound(String faceToken) {
                                fileUrlLast = mSave2JpegPresenter.save2Jpeg(imgData, StaticClass.detectFileNameLast, finalWidth, finalHeight);
                                userName = getUserName(faceToken);
                                speak("您好" + userName + ",欢迎回来！");
                            }

                            @Override
                            public void onFaceSetEmpty() {
                                speak("注册为第一个客户吧~！");
                            }
                        });
                    }
                });
            }
        }
    }

    @Override
    protected void onCancleProgressDialog() {
        stopRequest();
    }
}


