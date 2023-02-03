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
        String sql = "select * from z_test where a = '1,2,3,4,5' and b.dd = '2' or (c = '3' or (d = '4' and e = '5'))";
        String dsl = ElasticSearchUtils.transSqlToDsl(sql);
        System.out.println(dsl);
    }
}
