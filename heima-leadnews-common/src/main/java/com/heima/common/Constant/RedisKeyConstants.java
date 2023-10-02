package com.heima.common.Constant;

public class RedisKeyConstants {
    public static final String  TASK_FUTURE_PREV = "task:future:"; //未来数据key前缀
    public static final String  TASK_NOW_PREV = "task:now:";  //当前数据key前缀
    public static final String  TASK_SYNC_KEYS = "task:sync:keys";  //同步锁前缀
    public static final String  TASK_SYNC_DB = "task:sync:db";  //同步锁前缀

    public static final int  TASK_SYNC_TIME_MS = 3*1000;  //同步锁前缀
}
