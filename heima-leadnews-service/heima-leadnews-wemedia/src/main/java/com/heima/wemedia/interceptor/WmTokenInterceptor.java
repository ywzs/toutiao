package com.heima.wemedia.interceptor;

import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.threadLocal.WmThreadLocalUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WmTokenInterceptor implements HandlerInterceptor {
    /**
     * 获取线程中的用户id 存入threadLocal中
     * @param request 请求（经过网关转发）
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("userId");
        if (StringUtils.isNotBlank(userId)){
            WmUser user = new WmUser();
            user.setApUserId(Integer.valueOf(userId));
            WmThreadLocalUtil.setUser(user);
        }
        return true;
    }

    /**
     * 清理threadLocal中的Id防止内存泄露
     * @param request
     * @param response
     * @param handler
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        WmThreadLocalUtil.removeUser();
    }
}
