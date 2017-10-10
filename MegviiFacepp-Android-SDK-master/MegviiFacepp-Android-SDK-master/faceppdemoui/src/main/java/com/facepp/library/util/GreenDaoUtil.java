package com.facepp.library.util;

import android.app.Activity;
import android.content.Context;

import com.facepp.library.entity.DaoMaster;
import com.facepp.library.entity.DaoSession;

import org.greenrobot.greendao.database.Database;

import static com.facepp.library.util.Util.mDataBase;


/**
 * Created by Administrator on 2017/9/29 0029.
 */

public class GreenDaoUtil {
    private static DaoSession daoSession;

    public static DaoSession getDaoSession(Context context) {
        if(daoSession==null) {
        /*GreenDao*/
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, mDataBase);
            Database db = helper.getWritableDb();
            daoSession = new DaoMaster(db).newSession();
        }
        return daoSession;
    }

}
