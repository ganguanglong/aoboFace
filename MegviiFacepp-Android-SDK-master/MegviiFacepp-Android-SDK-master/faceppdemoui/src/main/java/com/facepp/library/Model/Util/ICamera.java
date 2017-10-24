package com.facepp.library.Model.Util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.Surface;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.facepp.library.Model.Util.Util.TAG;

/**
 * 照相机工具类
 */
public class ICamera {

    public Camera mCamera;
    public int cameraWidth;
    public int cameraHeight;
    public int cameraId = 0;// 前置摄像头
    public int Angle;

    public ICamera() {
    }

    /**
     * 打开相机
     */
    public Camera openCamera(boolean isBackCamera, Activity activity,
                             HashMap<String, Integer> resolutionMap) {
        try {
            /*这个是平常用的，但是在澳博的平板里好像不适用，所以注释掉*/
//            if (isBackCamera)
//                cameraId = 0;
//            else
//                cameraId = 1;
            /*这个是专门适配澳博平板用的*/
            if (isBackCamera)
                cameraId = 1;
            else
                cameraId = 0;

            int width = 640;
            int height = 480;


            if (resolutionMap != null) {
                width = resolutionMap.get("width");
                height = resolutionMap.get("height");
            }
            /*打开摄像头*/
            mCamera = Camera.open(cameraId);
            CameraInfo cameraInfo = new CameraInfo();
            /*通过摄像头id拿到摄像头信息*/
            Camera.getCameraInfo(cameraId, cameraInfo);
            /*获得摄像头的尺寸*/
            Camera.Parameters params = mCamera.getParameters();
            // Camera.Size bestPreviewSize = calBestPreviewSize(
            // mCamera.getParameters(), Screen.mWidth, Screen.mHeight);
            Camera.Size bestPreviewSize = calBestPreviewSize(
                    mCamera.getParameters(), width, height);
            cameraWidth = bestPreviewSize.width;
            cameraHeight = bestPreviewSize.height;
            /*defult*/
            params.setPreviewSize(cameraWidth,cameraHeight);

            /*获得摄像头的角度（0，90，180，270）*/
            Angle = getCameraAngle(activity);
            /*如果是6.0版本，需要angle不能-180，因为会变成负数，此条注释掉，用下一条*/
//            mCamera.setDisplayOrientation(Angle-180);
            mCamera.setDisplayOrientation(Angle);
            mCamera.setParameters(params);
            /*返回这个摄像头*/
            return mCamera;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 通过屏幕参数、相机预览尺寸计算布局参数
    public RelativeLayout.LayoutParams getLayoutParam() {
        float scale = cameraWidth * 1.0f / cameraHeight;
        int layout_width= Screen.mWidth;
        int layout_height = (int) (layout_width * scale);

        if (Screen.mWidth >= Screen.mHeight) {
            layout_height = Screen.mHeight;
            layout_width = (int) (layout_height / scale);
        }
        /*defult*/
//        RelativeLayout.LayoutParams layout_params = new RelativeLayout.LayoutParams(
//                layout_width,layout_height);
        /*custom*/
        RelativeLayout.LayoutParams layout_params = new RelativeLayout.LayoutParams(
                layout_height/9,layout_width/9);

//        layout_params.addRule(RelativeLayout.CENTER_HORIZONTAL);// 设置照相机水平居中
        layout_params.addRule(RelativeLayout.ALIGN_PARENT_START);// 设置照相机在左上角


        return layout_params;
    }

    /**
     * 开始检测脸
     */
    public void actionDetect(Camera.PreviewCallback mActivity) {
        if (mCamera != null) {
            Log.i("ggl", "setPreviewCallback");
            mCamera.setPreviewCallback(mActivity);
        }
    }

    public void startPreview(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public static ArrayList<HashMap<String, Integer>> getCameraPreviewSize(
            int cameraId) {
        ArrayList<HashMap<String, Integer>> size = new ArrayList<HashMap<String, Integer>>();
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
            if (camera == null)
                camera = Camera.open(0);

            List<Camera.Size> allSupportedSize = camera.getParameters()
                    .getSupportedPreviewSizes();
            for (Camera.Size tmpSize : allSupportedSize) {
                if (tmpSize.width > tmpSize.height) {
                    HashMap<String, Integer> map = new HashMap<String, Integer>();
                    map.put("width", tmpSize.width);
                    map.put("height", tmpSize.height);
                    size.add(map);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            }
        }

        return size;
    }

    /**
     * 通过传入的宽高算出最接近于宽高值的相机大小
     */
    private Camera.Size calBestPreviewSize(Camera.Parameters camPara,
                                           final int width, final int height) {
        List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
        ArrayList<Camera.Size> widthLargerSize = new ArrayList<Camera.Size>();
        for (Camera.Size tmpSize : allSupportedSize) {
            Log.w("ceshi", "tmpSize.width===" + tmpSize.width
                    + ", tmpSize.height===" + tmpSize.height);
            if (tmpSize.width > tmpSize.height) {
                widthLargerSize.add(tmpSize);
            }
        }

        Collections.sort(widthLargerSize, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                int off_one = Math.abs(lhs.width * lhs.height - width * height);
                int off_two = Math.abs(rhs.width * rhs.height - width * height);
                return off_one - off_two;
            }
        });

        return widthLargerSize.get(0);
    }

    /**
     * 打开前置或后置摄像头
     */
    public Camera getCameraSafely(int cameraId) {
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            camera = null;
        }
        return camera;
    }

    public Bitmap getBitMap(byte[] data, Camera camera, boolean mIsFrontalCamera) {
        int width = camera.getParameters().getPreviewSize().width;
        int height = camera.getParameters().getPreviewSize().height;
        YuvImage yuvImage = new YuvImage(data, camera.getParameters()
                .getPreviewFormat(), width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80,
                byteArrayOutputStream);
        byte[] jpegData = byteArrayOutputStream.toByteArray();
        // 获取照相后的bitmap
        Bitmap tmpBitmap = BitmapFactory.decodeByteArray(jpegData, 0,
                jpegData.length);
        Matrix matrix = new Matrix();
        matrix.reset();
        if (mIsFrontalCamera) {
            matrix.setRotate(-90);
        } else {
            matrix.setRotate(90);

        }
        tmpBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(),
                tmpBitmap.getHeight(), matrix, true);
        tmpBitmap = tmpBitmap.copy(Bitmap.Config.ARGB_8888, true);

        int hight = tmpBitmap.getHeight() > tmpBitmap.getWidth() ? tmpBitmap
                .getHeight() : tmpBitmap.getWidth();

        float scale = hight / 800.0f;

        if (scale > 1) {
            tmpBitmap = Bitmap.createScaledBitmap(tmpBitmap,
                    (int) (tmpBitmap.getWidth() / scale),
                    (int) (tmpBitmap.getHeight() / scale), false);
        }
        return tmpBitmap;
    }

    /**
     * 获取照相机旋转角度
     */
    public int getCameraAngle(Activity activity) {
        int rotateAngle = 90;
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        Log.i(TAG, "getCameraAngle: "+degrees);
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            rotateAngle = (info.orientation + degrees) % 360;
            rotateAngle = (360 - rotateAngle) % 360; // compensate the mirror
        } else { // back-facing
            rotateAngle = (info.orientation - degrees + 360) % 360;
        }
        Log.i(TAG, "getCameraAngle: "+rotateAngle);
        return rotateAngle;
    }
}