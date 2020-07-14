package com.example.webview;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class myDBHelper extends SQLiteOpenHelper {
    private static final String DB_Name="plex.db";
    private static final int DB_Version=1;
    private static myDBHelper mHelper=null;
    private SQLiteDatabase mDB=null;
    private buildMasterActivity.ScanData1 scanData;
    public static String Table_Name="scan";

    private myDBHelper(Context context){
        super(context,DB_Name,null,DB_Version);
    }

    private myDBHelper(Context context,int version){
        super(context,DB_Name,null,version);
    }

    public static myDBHelper getInstance(Context context,int version){
        //只有 mHelper为空时，才运行
        if(version>0&&mHelper==null){
            mHelper=new myDBHelper(context,version);
        }else if(mHelper==null){
            mHelper=new myDBHelper(context);
        }
        return mHelper;
    }

    //打开读连接
    public SQLiteDatabase openReadLink(){
        if(mDB==null||!mDB.isOpen()){
            mDB=mHelper.getReadableDatabase();
        }
        return mDB;
    }
    //打开写连接
    public SQLiteDatabase openWriteLink(){
        if(mDB==null||!mDB.isOpen()){
            mDB=mHelper.getWritableDatabase();
        }
        return mDB;
    }

    //关闭数据库连接
    public void closeLink(){
        if(mDB!=null||mDB.isOpen()){
            mDB.close();
            mDB=null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String drop_sql="DROP TABLE IF EXISTS "+Table_Name+";";
        db.execSQL(drop_sql);
        String create_sql="CREATE TABLE IF NOT EXISTS "+Table_Name
                +" (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                +"serial VARCHAR NOT NULL, master VARCHAR NOT NULL,"
                +"date DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, count INTEGER DEFAULT 0 NOT NULL)";
        db.execSQL(create_sql);

//        String myDate =cursor.getString(cursor.getColumnIndex("datetime(timestamp,'localtime')"));
//        SimpleDateFormat format = newSimpleDateFormat("yyyy-MM-dd HH:mm");
//        Date date = format.parse(myDate)
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int delete(String condition){
        return mDB.delete(Table_Name,condition,null);
    }

    public long insert(buildMasterActivity.ScanData1 scanData){
        ContentValues cv=new ContentValues();
        cv.put("serial",scanData.serial);
        cv.put("master",scanData.master);
        cv.put("count",scanData.count);
        //成功返回行号，失败返回 -1
        return mDB.insert(Table_Name,"",cv);
    }


}
