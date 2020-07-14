package com.example.webview;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class myDBHelper extends SQLiteOpenHelper {
    private static final String DB_Name="plex.db";
    private static final int DB_Version=1;
    private static myDBHelper mHelper=null;
    private SQLiteDatabase mDB=null;
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
        }else if(Helper==null){
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


    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
