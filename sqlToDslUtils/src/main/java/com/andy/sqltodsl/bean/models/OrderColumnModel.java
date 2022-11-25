package com.andy.sqltodsl.bean.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排序字段model
 *
 * @author Andy
 * @date 2022-11-25
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderColumnModel {

    /**
     * 字段名称
     */
    private String name;

    /**
     * 排序顺序
     */
    private String orderType;
}
