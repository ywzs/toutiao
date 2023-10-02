package com.heima.model.wemedia.dtos;

import lombok.Data;

import java.util.Date;

/**
 * {
 * 	"createdTime": "",
 * 	"description": "",
 * 	"id": 0,
 * 	"isDefault": true,
 * 	"name": "",
 * 	"ord": 0,
 * 	"status": true
 * }
 */
@Data
public class WmChannelDto {
    private Integer id;
    private String name;
    private Integer ord;
    private Date createdTime;
    private String description;
    private boolean status;
    private boolean idDefault;
}
