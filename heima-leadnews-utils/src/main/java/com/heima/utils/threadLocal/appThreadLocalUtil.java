package com.heima.utils.threadLocal;


import com.heima.model.user.pojos.ApUser;

public class appThreadLocalUtil {
    private static final ThreadLocal<ApUser> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(ApUser user){
        WM_USER_THREAD_LOCAL.set(user);
    }

    public static ApUser getUser(){
        return WM_USER_THREAD_LOCAL.get();
    }
    public static void removeUser(){
        WM_USER_THREAD_LOCAL.remove();
    }
}
