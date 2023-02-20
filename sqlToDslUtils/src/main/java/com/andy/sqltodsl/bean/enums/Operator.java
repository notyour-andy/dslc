package com.andy.sqltodsl.bean.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Getter
@AllArgsConstructor
public enum Operator {

    LEFT("(", 3, '('),

    RIGHT(")", 0, ')'),

    OR("OR", 2, '*'),

    AND("AND", 1, '+');


    /**
    *获取所有字符
    *@author Andy
    *@return 字符
    *@date 2022/11/15
    */
    public static Set<Character> getAllChr(){
        return Arrays.stream(Operator.values())
                     .map(Operator::getChr)
                     .collect(Collectors.toSet());
    }


    /**
    *通过chr寻找操作符
    *@author Andy
    *@return 对应操作符
    *@date 2022/11/15
    */
    public static Operator getByChr(Character chr){
        return Arrays.stream(Operator.values())
                     .filter(ele -> Objects.equals(ele.getChr(), chr))
                     .findAny()
                     .orElse(null);
    }


    private final String mark;

    private final Integer priority;

    private final Character chr;

}
