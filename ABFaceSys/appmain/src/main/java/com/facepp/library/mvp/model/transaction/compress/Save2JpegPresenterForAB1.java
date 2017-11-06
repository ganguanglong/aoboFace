package com.facepp.library.mvp.model.transaction.compress;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;

import com.facepp.library.util.YuvUtil;
import com.facepp.library.mvp.presenter.BasePresenter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.facepp.library.util.StaticClass.mPicUrl;

/**
 * Created by Administrator on 2017/10/25 0025.
 */

public class Save2JpegPresenterForAB1 extends BasePresenter<Video2JpegContract.View> implements Video2JpegContract.Presenter {
    String dir = mPicUrl;   //文件夹名
    @Override
    public String save2Jpeg(byte[] imgData, String fileName, int width, int height) {
        File sdRoot = Environment.getExternalStorageDirectory();    //系统路径
        File mkDir = new File(sdRoot, dir);
        if (!mkDir.exists()) {
            mkDir.mkdirs();   //目录不存在，则创建
        }
        imgData = YuvUtil.rotateYUV420spRotate180(imgData, width, height);
        File pictureFile = new File(sdRoot, dir + fileName);
        try {
            if (pictureFile.exists()) {
                pictureFile.delete();
                pictureFile.createNewFile();
            }
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pictureFile));
            //如果旋转了270或90度，宽高要互换
            YuvImage image = new YuvImage(imgData, ImageFormat.NV21, width, height, null);   //将NV21 data保存成YuvImage
            //图像压缩
            image.compressToJpeg(
                    new Rect(0, 0, image.getWidth(), image.getHeight()),
                    70, bos);   // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mkDir + "/" + fileName;
    }
}
