package com.andy.sqltodsl.bean.enums;

import java.util.Arrays;

/**
 * @author Andy
 * @date 2023-02-02
 */
public enum QueryType {

    /**
     * 等于整型
     */
    EQ_INTEGER,

    /**
     * 等于字符串
     */
    EQ_STR,

    /**
     * 已存在的range查询
     */
    EXIST_RANGE,

    /**
     * 不存在的range查询
     */
    NOT_EXIST_RANGE,

    /**
     * 已存在的nested查询
     */
    EXIST_NESTED,

    /**
     * 不存在的nested查询
     */
    NOT_EXIST_NESTED;

    public static boolean isExist(QueryType queryType){
        return Arrays.asList(EXIST_NESTED, EXIST_RANGE).contains(queryType);
    }
}
