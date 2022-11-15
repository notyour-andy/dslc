package com.andy.sqltodsl;

import com.andy.sqltodsl.utils.ElasticSearchUtils;

/**
 * 测试类
 *
 * @author Andy
 * @date 2022-11-15
 */
public class MainTest {

    public static void main(String[] args) {
        String sql = "select * from z_test where a = '1' and b.dd = '2' or (c = '3' or (d = '4' and e = '5'))";
        String dsl = ElasticSearchUtils.parseSqlToDsl(sql);
        System.out.println(dsl);
    }
}
