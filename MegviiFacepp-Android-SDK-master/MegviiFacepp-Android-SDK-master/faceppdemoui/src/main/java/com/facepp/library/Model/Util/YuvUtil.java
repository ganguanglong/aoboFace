package com.facepp.library.Model.Util;

/**
 * Created by Administrator on 2017/9/20 0020.
 */

public class YuvUtil {
    public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth, int imageHeight) {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        int n = 0;
        int uvHeight = imageHeight >> 1;
        int wh = imageWidth * imageHeight;
        //copy y
        for (int j = imageWidth - 1; j >= 0; j--) {
            for (int i = 0; i < imageHeight; i++) {
                yuv[n++] = data[imageWidth * i + j];
            }
        }

        for (int j = imageWidth - 1; j > 0; j -= 2) {
            for (int i = 0; i < uvHeight; i++) {
                yuv[n++] = data[wh + imageWidth * i + j - 1];
                yuv[n++] = data[wh + imageWidth * i + j];
            }
        }

        return yuv;
    }
    //旋转180度（顺时逆时结果是一样的）
    public static byte[] rotateYUV420spRotate180(byte[] data, int imageWidth, int imageHeight) {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        int n = 0;
        int uh = imageHeight >> 1;
        int wh = imageWidth * imageHeight;
        //copy y
        for (int j = imageHeight - 1; j >= 0; j--) {
            for (int i = imageWidth - 1; i >= 0; i--) {
                yuv[n++] = data[imageWidth * j + i];
            }
        }


        for (int j = uh - 1; j >= 0; j--) {
            for (int i = imageWidth - 1; i > 0; i -= 2) {
                yuv[n] = data[wh + imageWidth * j + i - 1];
                yuv[n + 1] = data[wh + imageWidth * j + i];
                n += 2;
            }
        }

        return yuv;
    }

    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }



}
