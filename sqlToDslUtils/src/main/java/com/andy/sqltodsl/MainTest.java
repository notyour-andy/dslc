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
        String sql = "select * from tableA where  (  ( P_getBaseInfo_year >= 1996 and P_getBaseInfo_year < 1998 )  )  and  ( status = 'Activity_1ozegsj' or status = 'Activity_1v76epi' or status = 'Activity_0m3dyek' or status = 'Activity_0wzecj7' or status = 'Activity_0sggd7x' or status = 'Activity_1omysls' or status = 'Activity_1gtlsn5' or status = 'Activity_11yh3ig' or status = 'Activity_1a4z8nx' or status = 'Activity_01gt2y7' or status = 'Activity_015z54l' or status = 'Activity_1pusyom' or status = 'Activity_09mmasg' or status = 'Activity_145hkas' )";
        String dsl = ElasticSearchUtils.convertSqlToDsl(sql);
        System.out.println(dsl);
    }
}
