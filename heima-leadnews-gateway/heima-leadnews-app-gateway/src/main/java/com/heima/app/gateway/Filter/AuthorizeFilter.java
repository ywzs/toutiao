package com.heima.app.gateway.Filter;

import com.heima.app.gateway.Utils.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizeFilter implements Ordered, GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //2.判断是否为登录请求
        if (request.getURI().getPath().contains("login")) {
            return chain.filter(exchange);
        }
        //3.获取token
        String token = request.getHeaders().getFirst("token");
        //4.检验token
        if (StringUtils.isBlank(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);   //设置401
            return response.setComplete();
        }
        Claims claims;
        try {   //有可能解析失败
            claims = AppJwtUtil.getClaimsBody(token);
            int code = AppJwtUtil.verifyToken(claims);
            if (code == 1 || code == 2) {
                //过期
                response.setStatusCode(HttpStatus.UNAUTHORIZED);   //设置401
                return response.setComplete();
            }
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);   //设置401
            return response.setComplete();
        }
        //获取用户id 并写入header 利于后面使用
        assert claims != null;
        Object id = claims.get("id");
        ServerHttpRequest httpRequest = request.mutate().headers(httpHeaders -> {
            httpHeaders.add("userId", id + "");
        }).build();
        //重置请求
        exchange.mutate().request(httpRequest);
        return chain.filter(exchange);
    }

    @Override  //优先级   值越小越先执行
    public int getOrder() {
        return 0;
    }
}
