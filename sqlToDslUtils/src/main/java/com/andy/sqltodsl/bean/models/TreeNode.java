package com.andy.sqltodsl.bean.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Andy
 * @date 2022-11-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode {


    /**
     * type = 0 表达式节点 1：逻辑符号节点
     */
    public Integer type;

    /**
     * 字段
     */
    public String field;

    /**
     * 运算符
     */
    public String operator;

    /**
     * type: 0 表达式的值，type:1 逻辑符号节点
     */
    public String value;

    /**
     * 左节点
     */
    public TreeNode left;

    /**
     * 右节点
     */
    public TreeNode right;
}
