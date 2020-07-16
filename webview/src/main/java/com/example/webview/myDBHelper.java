package com.example.webview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class myDBHelper extends SQLiteOpenHelper {
    private static final String DB_Name="plex1.db";
    private static final int DB_Version=1;
    private static myDBHelper mHelper=null;
    private SQLiteDatabase mDB=null;
    private buildMasterActivity.ScanData1 scanData;
    public static String Table_Name="scandata";

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

    public int delete(String condition,String[] args){
        return mDB.delete(Table_Name,condition,args);
    }

    public long insert(buildMasterActivity.ScanData1 scanData){
        ContentValues cv=new ContentValues();
        cv.put("serial",scanData.serial);
        cv.put("master",scanData.master);
        cv.put("count",scanData.count);
        //日期数据库有 默认值
        if (scanData.date!=null){
            //把本地时间显示转为 GMT 时间
            String strdate;
            {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Long timestamp=scanData.date.getTime();
                int offset= TimeZone.getDefault().getRawOffset();  //获取和 格林威治标准时区 的偏移值
                System.out.println("时区 的偏移值"+offset);
                timestamp-=offset;

                Date GMT_date=new Date(timestamp);
                strdate=format.format(GMT_date);
            }
            cv.put("date",strdate);   /////////////这里 无法还原时间
        }
        //成功返回行号，失败返回 -1
        return mDB.insert(Table_Name,"",cv);
    }

    public ArrayList<buildMasterActivity.ScanData1> query(){
        System.out.println("------------开始查询：");
        ArrayList<buildMasterActivity.ScanData1> list=new ArrayList<buildMasterActivity.ScanData1>();
        //时间已含 时区转换
        Cursor cursor=mDB.rawQuery("select _id,serial,master,datetime(date,'localtime') as date,count from "+Table_Name,null);

        while(cursor.moveToNext()){
            Integer id=cursor.getInt(cursor.getColumnIndex("_id"));
            String serial=cursor.getString(cursor.getColumnIndex("serial"));
            String master=cursor.getString(cursor.getColumnIndex("master"));
            String strdate=cursor.getString(cursor.getColumnIndex("date"));
            Integer count=cursor.getInt(cursor.getColumnIndex("count"));

            //字符串String 转 Date
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date= null;
            try {
                date = format.parse(strdate);
            } catch (ParseException e) {
                System.out.println("日期转出错！");
                e.printStackTrace();
            }
            System.out.printf("%s %s %s %s %s %n",id.toString(),serial,master,strdate,count.toString());    //////////////////////
            buildMasterActivity.ScanData1 scandata=new buildMasterActivity.ScanData1(serial,master,date,count);   ////////////////
            list.add(scandata);
        }
        return list;
    }

    public boolean contains(String serial){
        Cursor cursor=mDB.rawQuery("select serial from "+Table_Name +" where serial=?",new String[]{serial});
        if (cursor.getCount()>0){
            return true;
        }else{
            return false;
        }
    }
}
