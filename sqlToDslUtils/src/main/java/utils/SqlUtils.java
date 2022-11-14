package utils;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;

import java.util.*;

public class SqlUtils {


    private static final String FIELD = "field";

    private static final String VALUE = "value";

    private static final String OPERATOR = "operator";

    public static void main(String[] args) {
        String sql = "select * from z_test where a = '1' and b = '2' or (c = '3' or ( d = '4' and e = '5'))";
        List<String> dataSet = parseQueryConditions(sql);
        System.out.println("dd");
    }



    /**
     * 将sql的查询语句去除空格存入set
     * @author Andy
     * @date 2022/11/14 22:31
     * @param sql 待解析sql
     * @return conditions set
     **/
    public static List<String> parseQueryConditions(String sql){
        List<String> resultSet = new ArrayList<>();
        Map<String ,Object> dataMap = null;
        MySqlStatementParser parser = new MySqlStatementParser(sql);
        SQLStatement sqlStatement = parser.parseStatement();
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        sqlStatement.accept(visitor);
        for (TableStat.Condition con : visitor.getConditions()) {
            Class<?> aClass = con.getValues().get(0).getClass();
            if (Objects.equals(aClass.getName(), "java.lang.String")) {
                resultSet.add(con.getColumn().getName() + con.getOperator() +  "'" + con.getValues().get(0) + "'");
            }else{
                resultSet.add(con.getColumn().getName() + con.getOperator() + con.getValues().get(0));
            }
        }
        return resultSet;
    }
}
