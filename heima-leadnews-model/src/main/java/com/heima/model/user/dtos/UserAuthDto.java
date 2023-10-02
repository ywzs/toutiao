package com.heima.model.user.dtos;

import lombok.Data;

/**
 * {
 * 	"id": 0,
 * 	"msg": "",
 * 	"page": 0,
 * 	"size": 0,
 * 	"status": 0
 * }
 */
@Data
public class UserAuthDto {
    private Integer id;
    private String msg;
    private Integer page;
    private Integer size;
    private Integer status;

}
