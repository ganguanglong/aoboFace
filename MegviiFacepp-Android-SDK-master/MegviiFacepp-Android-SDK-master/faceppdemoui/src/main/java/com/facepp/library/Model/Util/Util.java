package com.facepp.library.Model.Util;

import okhttp3.MediaType;

public class Util {

    //在这边填写 API_KEY 和 API_SECRET
    public static  String API_KEY = "nwZBPpSFMAet_ThLvG6MLlUSZGKcQhTo";
    public static  String API_SECRET = "vzhw1aUaZv03N_Dp97kGTY3CxZfwDOmI";
    //这是第二个账号（13828460119），因为【以为】澳博平板用不了上面的账号，多申请了一个账户
//    public static  String API_KEY = "xa3gwCvjp060-nZwxXJyeJ5y8np1FEQB";
//    public static  String API_SECRET = "W6LbpUt9u8vSXytCGd-opLeqODl8WJx_";
    //outerid值
//    public static final String OUTER_ID ="aoboTest";  //这个暂时不用了
//    public static final String OUTER_ID ="aoboTest2"; //这个暂时不用了
    public static final String OUTER_ID ="aoboTest3";
    //facepp key
    public static final String mApiKey ="api_key";
    //facepp 密钥
    public static final String mApiSecret ="api_secret";

    public static final String RETURN_INFO ="JepgPath";
    public static final String FILE_PATH ="/storage/emulated/0/ggljpeg/";
    public static final String TAG = "ggl";
    //face Url
    public static final String url_base ="https://api-cn.faceplusplus.com/facepp/v3/";
    //检测 api
    public static final String url_detect ="detect";
    //搜索 api
    public static final String url_search ="search";
    //addFace api
    public static final String url_add_face ="faceset/addface";
    //图像地址
    public static final String mImageUrl ="image_url";
    //图像文件
    public static final String mImageFile ="image_file";
    //返回人脸属性
    public static final String mAttributes ="return_attributes";

    //faceSet outer_id
    //abtest

    public static final String mOuterId ="outer_id";
    public static final String mFactTokens ="face_tokens";

    public static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    //拍照
    public static final int REQUEST_IMAGE_CAPTURE = 1;

    //拍照储存地址
    public static final String mPicUrl = "/ggljpeg/";

    //本地数据库
    public static final String mDataBase = "face_user-db";


}
