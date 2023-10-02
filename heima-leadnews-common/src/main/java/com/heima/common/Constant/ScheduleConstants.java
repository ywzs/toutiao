package com.heima.common.Constant;

public class ScheduleConstants {

    //task状态
    public static final int SCHEDULED=0;     //初始化状态

    public static final int EXECUTED=1;        //已执行状态

    public static final int CANCELLED=2;      //已取消状态

    public static final int ENTER_CACHE_TIME = 5;  //提前加载5分钟的任务

    public static final int TASK_HANDLE_TIME = 3000;  //任务消费的时间间隔
}