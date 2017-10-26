package com.facepp.library.zuseless;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facepp.library.R;
import com.facepp.library.model.util.CameraMatrix;
import com.facepp.library.model.util.ConUtil;
import com.facepp.library.model.util.DialogUtil;
import com.facepp.library.model.util.ICamera;
import com.facepp.library.model.util.MediaRecorderUtil;
import com.facepp.library.model.util.OpenGLDrawRect;
import com.facepp.library.model.util.OpenGLUtil;
import com.facepp.library.model.util.PointsMatrix;
import com.facepp.library.model.util.Screen;
import com.facepp.library.model.util.SensorEventUtil;
import com.facepp.library.model.util.YuvUtil;
import com.megvii.facepp.sdk.Facepp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static com.facepp.library.model.util.Util.TAG;

public class OldOpenglActivity extends Activity
        implements
        PreviewCallback,/*返回摄像头的预览数据*/
        Renderer,
        SurfaceTexture.OnFrameAvailableListener /*SurfaceTexture.OnFrameAvailableListener用于让SurfaceTexture的使用者知道有新数据到来。*/ {
    private int printTime = 31; /*刷新页面的时间时间，31毫秒挺流畅的*/

    Handler timeHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                /*主动刷新，依赖printTime的数值，越小越流畅*/
                case 0:
//                    Log.i(TAG,"timeHandle case 0");
                    mGlSurfaceView.requestRender();// 发送去绘制照相机不断去回调
                    Log.i(TAG,"timeHandle:sendEmptyMessageDelayed");
                    timeHandle.sendEmptyMessageDelayed(0, printTime);
                    break;
                /*不主动刷新，但是效果挺好，不影响流畅度*/
                case 1:
                    Log.i(TAG, "timeHandle case 1");
                    mGlSurfaceView.requestRender();// 发送去绘制照相机不断去回调
                    break;
            }
        }
    };

    String AttriButeStr = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        Screen.initialize(this);                                                                                /*1.1屏幕初始化*/
        setContentView(R.layout.activity_opengl);                                                               /*1.2设置布局*/
        init();                                                                                                 /*1.3view初始化*/
                                                                                                                /*到此为止完成了一半的初始化，接下来走的是onResume()*/
        new Handler().postDelayed(new Runnable() {                                                              /*1.4新开一个线程每2秒执行startRecorder()*/
            @Override
            public void run() {
                startRecorder();                                                                                /*1.5开始录制*/
            }
        }, 2000);
    }

    private Facepp facepp; /*实例化一个Facepp*/
    private boolean isStartRecorder,/*是不是录像*/
            is3DPose,/*是不是3d模型*/
            isDebug,/*是不是调试信息*/
            isROIDetect,/*是不是在区域里检测*/
            is106Points,/*是不是106个关键点，不是的话就是81个关键点*/
            isBackCamera,/*是不是后置摄像头*/
            isFaceProperty,/*是否显示人脸属性*/
            isOneFaceTrackig;/*是否单脸跟踪*/
    private int min_face_size = 200;
    private int detection_interval = 25;/*检测间隔*/
    private HashMap<String, Integer> resolutionMap; /*貌似跟录像有关*/
    private SensorEventUtil sensorUtil;/*实例化一个传感器工具类，应该和3d建模有关*/
    /*在子线程实例化一个命名为(facepp)的线程*/
    private HandlerThread mHandlerThread = new HandlerThread("facepp");
    private Handler mHandler; /*实例化一个mHandler*/
    private GLSurfaceView mGlSurfaceView; /*GLSurfaceView*/
    private ICamera mICamera; /*实例化一个手机类*/
    private DialogUtil mDialogUtil; /*实例化一个对话框类*/
    private void init() {
        Log.i(TAG, "init");

        /*1.3.1检查手机版本信息是否等于PLK-AL10，如果等于的话，刷新时间设定为50*/
        if (android.os.Build.MODEL.equals("PLK-AL10"))
            printTime = 50;
        /*1.3.2获取一些默认设置，包括是否录制，3d模型，调试模式，区域选择等*/
        isStartRecorder = getIntent().getBooleanExtra("isStartRecorder", false);
        is3DPose = getIntent().getBooleanExtra("is3DPose", false);
        isDebug = getIntent().getBooleanExtra("isdebug", false);
        isROIDetect = getIntent().getBooleanExtra("ROIDetect", false);
        is106Points = getIntent().getBooleanExtra("is106Points", false);
        isBackCamera = getIntent().getBooleanExtra("isBackCamera", false);
        isFaceProperty = getIntent().getBooleanExtra("isFaceProperty", false);
        isOneFaceTrackig = getIntent().getBooleanExtra("isOneFaceTrackig", false);
        trackModel = getIntent().getStringExtra("trackModel");
        min_face_size = getIntent().getIntExtra("faceSize", min_face_size);
        detection_interval = getIntent().getIntExtra("interval", detection_interval);
        resolutionMap = (HashMap<String, Integer>) getIntent().getSerializableExtra("resolution");

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
        mGlSurfaceView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                autoFocus();/*1.3.5.6自动对焦*/
            }
        });

        mICamera = new ICamera();                                                                                      /*1.3.6*/

        mDialogUtil = new DialogUtil(this);                                                                             /*1.3.7*/

        debugInfoText = (TextView) findViewById(R.id.opengl_layout_debugInfotext);                                      /*1.3.8*/
        AttriButetext = (TextView) findViewById(R.id.opengl_layout_AttriButetext);                                      /*1.3.9*/

        debugPrinttext = (TextView) findViewById(R.id.opengl_layout_debugPrinttext);                                   /*1.3.10设置调试信息是否显示*/
        if (isDebug)
            debugInfoText.setVisibility(View.VISIBLE);
        else
            debugInfoText.setVisibility(View.INVISIBLE);
    }

    /**
     * 内容：
     * 1.保持屏幕常量
     * 2.打开摄像头，并获得摄像头的参数。
     * 3.把摄像头宽高设置给mGlSurfaceView。
     * 4.facepp的初始化和初始设置。
     */
    /*开始时间*/
    long startTime;
    private float roi_ratio = 0.8f;/*会影响举行框的大小比例*/
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
        /*记录开始时间*/
        startTime = System.currentTimeMillis();
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
            if (isROIDetect) {
                float line = height * roi_ratio;
                left = (int) ((width - line) / 2.0f);
                top = (int) ((height - line) / 2.0f);
                right = width - left;
                bottom = height - top;
            }

            /*错误代码，通过facepp.init初始化*/
            String errorCode = facepp.init(this, ConUtil.getFileContent(this, R.raw.megviifacepp_0_4_7_model));
            /*获取facepp的设置类*/
            Facepp.FaceppConfig faceppConfig = facepp.getFaceppConfig();
            faceppConfig.interval = detection_interval;
            faceppConfig.minFaceSize = min_face_size;
            faceppConfig.roi_left = left;
            faceppConfig.roi_top = top;
            faceppConfig.roi_right = right;
            faceppConfig.roi_bottom = bottom;
            /*如果是单脸跟踪*/
            if (isOneFaceTrackig)
                faceppConfig.one_face_tracking = 1;
            else
                faceppConfig.one_face_tracking = 0;
            /*字符串数组array拿到跟踪模式是：Normal/Robust/Fast，并设置跟踪模式*/
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
     * @param gl
     * @param config
     */
    private int mTextureID = -1;
    private SurfaceTexture mSurface;
    private CameraMatrix mCameraMatrix;
    private PointsMatrix mPointsMatrix;
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
        mPointsMatrix = new PointsMatrix();

        /*
        * 开始预览，这一条必须开启，不开启的话，都没有预览画面了
        * */
        mICamera.startPreview(mSurface);// 设置预览容器
        /*开始检测人脸,其实并没有开始，只是配置了camera的setPreviewCallback，然后在里面检测人脸*/
        mICamera.actionDetect(this);
        if (isTiming) {
            /*触发第一次刷新，触发后会自动不断按照间隔值(printTime=31)刷新*/
            //会引起onDrawFrame被不断触发
            Log.i(TAG, "onSurfaceCreated:sendEmptyMessageDelayed," + isTiming);
            timeHandle.sendEmptyMessageDelayed(0, printTime);
        }
        if (isROIDetect)
            /*如果是区域选择模式，会根据人脸位置画矩形*/
            drawShowRect();
    }

    /**
     * 走完onSurfaceCreated之后，会走这个方法，第一次拿到Surface的属性
     *
     * 内容：
     * 1.设置Gl的投影宽高，否则影像会超出或少于GLSurfaceView的范围
     * 2.设置投影转换矩阵
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
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        // Matrix.perspectiveM(mProjMatrix, 0, 0.382f, ratio, 3, 700);
    }

    /**
     * 这个方法很明确是被GLSurfaceView.requestRender()触发的。
     * 这个方法会绘制人脸关键点，以及整个画面，然后再更新画面
     *
     * 内容：
     * 1.绘制摄像头预览画面和人脸关键点
     * 2.更新画面
     * @param gl
     */
    private TextView debugInfoText, debugPrinttext, AttriButetext;
    private final float[] mMVPMatrix = new float[16]; /*这个不知道是什么矩阵*/
    private final float[] mVMatrix = new float[16];  /*这个也不知道是什么矩阵*/
    private final float[] mRotationMatrix = new float[16];  /*这个也不知道是什么矩阵*/
    @Override
    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "onDrawFrame----");
        final long actionTime = System.currentTimeMillis();
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
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1f, 0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
        /*跟人脸点有关的绘制，注释掉就看不到人脸点了*/
        mPointsMatrix.draw(mMVPMatrix);
        /*输出一个printTime到UI里*/
        if (isDebug) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final long endTime = System.currentTimeMillis() - actionTime;
                    debugPrinttext.setText("printTime: " + endTime);
                }
            });
        }
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
     * 在这里我们没必要对传入对serfaceTexture做什么处理了
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // TODO Auto-generated method stub

    }

    /**
     * 这个方法是在mCamera.setPreviewCallback(mActivity);之后触发的。
     * 这个方法主要是把预览画面发出去，并处理接收回来的数据。
     * 如果把mCamera.setPreviewCallback(mActivity)注释掉，也就是不走这个方法的话
     *
     * 这个方法的意义完全只在于它所传回来的参数imgData，如果单纯是预览画面，是不需要走
     * 这个方法的，但是为了后续绘制摄像头内容和绘制人脸关键点，需要把imgData发送出去。
     * 并且拿到返回数据进行下一步处理。
     *
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
    long time_AgeGender_end = 0;
    boolean handleFace=true;
    String fileName;
//    int number=0;
    @Override
    public void onPreviewFrame(final byte[] imgData, final Camera camera) {
        Log.i(TAG, "onPreviewFrame----");

        /*如果isSuccess为True，我也不知道代表这什么，反正若为True就让程序返回*/
        if (isSuccess)
            return;
        /*设定isSuccess为true*/
        isSuccess = true;
//        if(!handleFace){
//            Intent startIntent = new Intent(OldOpenglActivity.this,
//                    DetectActivity.class);
//            startIntent.putExtra(Util.RETURN_INFO,fileName);
//            startActivity(startIntent);
//            return;
//        }

        /*开启一个线程*/
        mHandler.post(new Runnable() {

            @Override
            public void run() {

                /*获得摄像头的宽高*/
                int width = mICamera.cameraWidth;
                int height = mICamera.cameraHeight;
                /*获得当前系统时间，作为faceDetectTime_action*/
                long faceDetectTime_action = System.currentTimeMillis();
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
                /*algorithm的意思是运算时间，应该是计算一下从线程开始获得人脸数据的耗时*/
                final long algorithmTime = System.currentTimeMillis() - faceDetectTime_action;
                /*判断faces返回的数据是否为空*/
                if (faces != null) {
//                    number++;
                    /*actionMaticsTime设置为当前时间*/
                    long actionMaticsTime = System.currentTimeMillis();
                    /*新建一个动态数组 和人脸关键点有关的*/
                    ArrayList<ArrayList> pointsOpengl = new ArrayList<ArrayList>();
                    confidence = 0.0f;
                    /*faces的长度如果大于等于0*/

                    if (faces.length >= 0) {
                        /*遍历faces*/
                        for (int c = 0; c < faces.length; c++) {
                            Log.i(TAG,"face[c]");
                            /*打印face的信息*/
//                                printInfo(faces[c]);
                            /*保存为JPEG照片*/
                            fileName=saveToJPEGandOutput(imgData,width,height);
                            /*只保存第一次找到人脸，把这个值设为false，后面的都不保存*/
                            handleFace=false;
                            /*如果是106个点*/
                            if (is106Points)
                                /*这里还需要通过facepp.getLandmark来解析*/
                                facepp.getLandmark(faces[c], Facepp.FPP_GET_LANDMARK106);
                            /*否则是认为是81个点*/
                            else
                                facepp.getLandmark(faces[c], Facepp.FPP_GET_LANDMARK81);
                            /*是否3d建模*/
                            if (is3DPose) {
                                facepp.get3DPose(faces[c]);
                            }
                            /*拿到这个face*/
                            Facepp.Face face = faces[c];
                            /*如果要求显示人脸属性*/
                            if (isFaceProperty) {
                                /*获取当前时间*/
                                long time_AgeGender_action = System.currentTimeMillis();
                                /*获取一些人脸的信息（年龄，性别）*/
                                facepp.getAgeGender(faces[c]);
                                /*计算获取人脸信息的耗时*/
                                time_AgeGender_end = System.currentTimeMillis() - time_AgeGender_action;
                                String gender = "man";
                                if (face.female > face.male)
                                    gender = "woman";
                                AttriButeStr = "\nage: " + (int) Math.max(face.age, 1) + "\ngender: " + gender;
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
                            /*人脸关键点的描绘*/
                            ArrayList<FloatBuffer> triangleVBList = new ArrayList<FloatBuffer>();
                            for (int i = 0; i < faces[c].points.length; i++) {
                                float x = (faces[c].points[i].x / height) * 2 - 1;
                                if (isBackCamera)
                                    x = -x;
                                float y = 1 - (faces[c].points[i].y / width) * 2;
                                float[] pointf = new float[]{x, y, 0.0f};
                                if (orientation == 1)
                                    pointf = new float[]{-y, x, 0.0f};
                                if (orientation == 2)
                                    pointf = new float[]{y, -x, 0.0f};
                                if (orientation == 3)
                                    pointf = new float[]{-x, -y, 0.0f};

                                FloatBuffer fb = mCameraMatrix.floatBufferUtil(pointf);
                                triangleVBList.add(fb);
                            }
                            pointsOpengl.add(triangleVBList);
                        }
                    /*faces的长度如果小于0*/
                    } else {
                        pitch = 0.0f;
                        yaw = 0.0f;
                        roll = 0.0f;
                    }
                    /*faces的长度如果小于怎么又来一遍，好像对效果来讲影响不大*/
                    if (faces.length > 0 && is3DPose)
                        mPointsMatrix.bottomVertexBuffer = OpenGLDrawRect.drawBottomShowRect(0.15f, 0, -0.7f, pitch,
                                -yaw, roll, rotation);
                    else
                        mPointsMatrix.bottomVertexBuffer = null;
                    synchronized (mPointsMatrix) {
                        /*把搜集到的人脸关键点交给mPointsMatrix，它在onDrawFrame里面绘制*/
                        mPointsMatrix.points = pointsOpengl;
                    }
                    /*估计是算一下建模绘图的时间吧*/
                    final long matrixTime = System.currentTimeMillis() - actionMaticsTime;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String logStr = "\ncameraWidth: " + mICamera.cameraWidth + "\ncameraHeight: "
                                    + mICamera.cameraHeight + "\nalgorithmTime: " + algorithmTime + "ms"
                                    + "\nmatrixTime: " + matrixTime + "\nconfidence:" + confidence;
                            debugInfoText.setText(logStr);
                            if (faces.length > 0 && isFaceProperty && AttriButeStr != null && AttriButeStr.length() > 0)
                                AttriButetext.setText(AttriButeStr + "\nAgeGenderTime:" + time_AgeGender_end);
                            else
                                AttriButetext.setText("");
                        }
                    });
                }
                isSuccess = false;
                if (!isTiming) {
                    timeHandle.sendEmptyMessage(1);
                }
            }
        });
    }

    private String saveToJPEGandOutput(byte[] imgData, int width, int height) {
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";  //jpeg文件名定义
        File sdRoot = Environment.getExternalStorageDirectory();    //系统路径
        String dir = "/TAGjpeg/";   //文件夹名
        File mkDir = new File(sdRoot, dir);
        Log.i(TAG,width+"和"+height);
        if (!mkDir.exists())
        {
            mkDir.mkdirs();   //目录不存在，则创建
        }
        /*旋转图像的方向*/
        byte[] Data = YuvUtil.rotateYUV420Degree270(imgData,width,height);

        File pictureFile = new File(sdRoot, dir + fileName);
        if (!pictureFile.exists()) {
            try {
                pictureFile.createNewFile();

                FileOutputStream filecon = new FileOutputStream(pictureFile);
                //由于旋转了270度，宽高要互换，同理，90度也是
                YuvImage image = new YuvImage(Data, ImageFormat.NV21,height ,width , null);   //将NV21 data保存成YuvImage
                //图像压缩
                image.compressToJpeg(
                        new Rect(0, 0, image.getWidth(), image.getHeight()),
                        70, filecon);   // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流

            }catch (IOException e)
            {
                e.printStackTrace();
            }

        }
        Log.i(TAG,"filePath："+mkDir+"/"+fileName);
        return mkDir+"/"+fileName;
    }



    private void printInfo(Facepp.Face face) {
        Log.i(TAG, "points:"+String.valueOf(face.points));
        Log.i(TAG, "blurness:"+(face.blurness));
        Log.i(TAG, "confidence:"+face.confidence);
        Log.i(TAG, "feature:"+face.feature);
        Log.i(TAG, "female:"+face.female);
        Log.i(TAG, "leftEyestatus:"+face.leftEyestatus);
        Log.i(TAG, "male:"+face.male);
        Log.i(TAG, "moutstatus:"+face.moutstatus);
        Log.i(TAG, "rect:"+face.rect);
        Log.i(TAG, "pitch:"+face.pitch);
        Log.i(TAG, "rightEyestatus:"+face.rightEyestatus);
    }

    /**
     * 此方法被onPreviewFrame触发
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
    /**
     * 开始录制（如果不是录制模式，就没有这一步了）
     */
    private MediaRecorderUtil mediaRecorderUtil;/*实例化一个摄像头录像工具类*/
    private void startRecorder() {
        Log.i(TAG, "startRecorder");
        /*是否录像模式*/
        if (isStartRecorder) {
            /*计算角度*/
            int Angle = 360 - mICamera.Angle;
            /*是否后置摄像头*/
            if (isBackCamera)
                /*角度等于摄像头的角度*/
                Angle = mICamera.Angle;
            /*录像工具类实例化*/
            //这个构造方法除了设置数值，还干了一个我不懂的方法---MediaRecorder()
            mediaRecorderUtil = new MediaRecorderUtil(this, mCamera, mICamera.cameraWidth, mICamera.cameraHeight);
            /*录像类的初始化*/
            isStartRecorder = mediaRecorderUtil.prepareVideoRecorder(Angle);
            /*是否完成了初始化*/
            if (isStartRecorder) {
                /*开始录像*/
                boolean isRecordSucess = mediaRecorderUtil.start();
                /*是否成功开始录制*/
                if (isRecordSucess)
                    /*开始检测人脸*/
                    mICamera.actionDetect(this);
                else
                    /*无法成功开始录制，弹出提示框*/
                    mDialogUtil.showDialog(getResources().getString(R.string.no_record));
            }
        }
    }

    /**
     * 自动对焦，在点击的时候才会触发
     */
    private void autoFocus() {
        Log.i(TAG, "autoFocus");
        if (mCamera != null && isBackCamera) {
            mCamera.cancelAutoFocus();/*停止对焦*/
            Parameters parameters = mCamera.getParameters();/*获取相机属性*/
            parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);/*设定相机属性为自动对焦*/
            mCamera.setParameters(parameters);/*把设置好的属性添加进去*/
            mCamera.autoFocus(null);/*对焦后是否需要回调*/
        }
    }


    /**
     * 画绿色框
     */
    private void drawShowRect() {
        Log.i(TAG, "drawShowRect");
        mPointsMatrix.vertexBuffers = OpenGLDrawRect.drawCenterShowRect(isBackCamera, mICamera.cameraWidth,
                mICamera.cameraHeight, roi_ratio);
    }



    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        ConUtil.releaseWakeLock();
        if (mediaRecorderUtil != null) {
            mediaRecorderUtil.releaseMediaRecorder();
        }
        mICamera.closeCamera();
        mCamera = null;

        timeHandle.removeMessages(0);

        finish();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        facepp.release();
    }



}
