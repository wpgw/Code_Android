package com.example.webview;

public class EqualsDemo {
    public static void main(String[] args) {
        class mThread extends Thread{
            volatile boolean flag=true;
            @Override
            public void run(){
                int num=0;
                while (!Thread.currentThread().isInterrupted()&&flag && num <= Integer.MAX_VALUE / 10) {
                    if(num%1000==0) {
                        System.out.println("1000的倍数" + num);
                    }
                    num++;
                }
            }
        }

        mThread thread = new mThread();
        //Thread thread = new Thread(mthread);
        thread.start();
        try
            {
                Thread.sleep(10);
            } catch(
            InterruptedException e)

            {
                e.printStackTrace();
            }
        //thread.interrupt();
        thread.flag=false;
        }
    }

