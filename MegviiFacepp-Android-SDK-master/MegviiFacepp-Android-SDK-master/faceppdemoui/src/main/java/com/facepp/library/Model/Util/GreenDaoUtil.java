package com.facepp.library.Model.Util;

import android.content.Context;

import com.facepp.library.Model.Entity.DaoMaster;
import com.facepp.library.Model.Entity.DaoSession;

import org.greenrobot.greendao.database.Database;

import static com.facepp.library.Model.Util.Util.mDataBase;


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
