package com.andy.sqltodsl.utils;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.parser.EOFParserException;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.JdbcConstants;
import com.andy.sqltodsl.bean.models.OrderColumnModel;
import com.andy.sqltodsl.bean.models.TreeNode;
import com.google.inject.internal.util.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SqlUtils {


    public static void main(String[] args) {
        String sql = "SELECT state, left(name, 20) as name FROM T1 WHERE STATE = 0";
//        List<String> selectList = getSelectList(sql);
//        System.out.println(selectList);
        MySqlSelectQueryBlock queryBlock = getQueryBlock(sql);
        List<SQLSelectItem> selectList = queryBlock.getSelectList();
        System.out.println("d");
    }







    /**
    *获取limit参数
    *@author Andy
    *@param sql sql语句
    *@date 2022/11/30
    */
    public static Map<String, Object> getLimitArgMap(String sql){
        MySqlSelectQueryBlock queryBlock = getQueryBlock(sql);
        SQLLimit limit = queryBlock.getLimit();
        Map<String, Object> returnMap = new HashMap<>();
        if(!Objects.isNull(limit)){
            returnMap.put("from", Objects.isNull(limit.getOffset()) ? 0 : limit.getOffset().toString());
            returnMap.put("size", limit.getRowCount().toString());
        }
        return returnMap;
    }

    /**
    *获取group by 参数
    *@author Andy
    *@param sql sql语句
    *@date 2022/11/30
    */
    public static List<String> getGroupByFieldList(String sql){
        List<String> resultList = new ArrayList<>();
        MySqlSelectQueryBlock queryBlock = getQueryBlock(sql);
        if (!Objects.isNull(queryBlock.getGroupBy())) {
            for (SQLExpr item : queryBlock.getGroupBy().getItems()) {
                resultList.add(item.toString());
            }
        }
        return resultList;
    }

    /**
    *获取查询的字段
    *@author Andy
    *@date 2023/2/2
    */
    public static List<String> getSelectList(String sql){
        MySqlSelectQueryBlock queryBlock = getQueryBlock(sql);
        List<SQLSelectItem> selectList = queryBlock.getSelectList();
        if (CollectionUtils.isNotEmpty(selectList)){
            //如果含有*,且只能只含有* 这里的filter:当查询字段为空后存在子查询, queryBlock会将整条sql语句作为查询字段, 去除这种情况
            List<String> fieldList = selectList.stream()
                                               .filter(ele -> (ele.getExpr() instanceof SQLQueryExpr))
                                               .map(ele -> ele.getExpr().toString())
                                               .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(fieldList)) {
                if (fieldList.contains("*")) {
                    if (fieldList.size() == 1) {
                        return Collections.emptyList();
                    } else {
                        throw new IllegalArgumentException("查询字段有误");
                    }
                }
                return fieldList;
            }else{
                throw new IllegalArgumentException("查询字段为空");
            }
        }else{
            throw new IllegalArgumentException("查询字段为空");
        }

    }

    public static MySqlSelectQueryBlock getQueryBlock(String sql){
        List<SQLStatement> sqlStatementList = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        return ((MySqlSelectQueryBlock) (((SQLSelectStatement) sqlStatementList.get(0)).getSelect()).getQuery());
    }


    public static MySqlSchemaStatVisitor getVisitor(String sql){
        //获取visitor
        MySqlStatementParser parser = new MySqlStatementParser(sql);
        SQLStatement sqlStatement = parser.parseStatement();
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        sqlStatement.accept(visitor);
        return visitor;
    }



    /**
    *获取排序字段
    *@author Andy
    *@param sql 语句
    *@return 排序字段
    *@date 2022/11/25
    */
    public static List<OrderColumnModel> getOrderColumnList(String sql){
        List<OrderColumnModel> modelList = new ArrayList<>();
        OrderColumnModel model;
        MySqlSchemaStatVisitor visitor = getVisitor(sql);
        for (TableStat.Column column : visitor.getOrderByColumns()) {
            model = new OrderColumnModel();
            //默认名字
            model.setName(column.getName());
            //默认增
            model.setOrderType(MapUtils.getString(column.getAttributes(), "orderBy.type", "ASC"));
            modelList.add(model);
        }
        return modelList;
    }


    /**
     * 将sql的查询语句去除空格存入set
     * @author Andy
     * @date 2022/11/14 22:31
     * @param sql 待解析sql
     * @return conditions set
     **/
    public static TreeNode parseQueryCondition(String sql){
        MySqlSelectQueryBlock queryBlock = getQueryBlock(sql);
        SQLBinaryOpExpr where = (SQLBinaryOpExpr) queryBlock.getWhere();
        if (!Objects.isNull(where)) {
            return TreeUtils.getExprTreeByExpr(where);
        }
        return null;
    }







    /**
    *获取where完整语句, 只考虑单表单where
    *@author Andy
    *@param sql sql语句
    *@return 完整的where语句
    *@date 2022/11/15
    */
    public static String getWhereStatement(String sql){
        List<SQLStatement> sqlStatementList;
        try {
            sqlStatementList = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        }catch (EOFParserException e){
            throw new RuntimeException("sql语句有误");
        }
        Preconditions.checkArgument(Objects.equals(sqlStatementList.size(), 1), "只支持单WHERE条件查询");
        MySqlSelectQueryBlock mysqlSelectQueryBlock = ((MySqlSelectQueryBlock) (((SQLSelectStatement) sqlStatementList.get(0)).getSelect()).getQuery());
        SQLExpr sqlExpr = mysqlSelectQueryBlock.getWhere();
        String stm = "";
        if (!Objects.isNull(sqlExpr)) {
            stm = sqlExpr.toString();
            //去除多余的符号
            stm = stm.replace("\n", "");
            stm = stm.replace("\t", "");
        }
        return stm;
    }




    /**
    *获取Where条件中的where查询val
    *@author Andy
    *@date 2023/2/22
    */
    public static List<String> getCondByWhere(SQLExpr sqlExpr){
        List<String> resultList = new ArrayList<>();
        if (!Objects.isNull(sqlExpr)){
            if (sqlExpr instanceof SQLBinaryOpExpr){
                SQLBinaryOpExpr opExpr = (SQLBinaryOpExpr) sqlExpr;
                SQLExpr left = opExpr.getLeft();
                SQLExpr right = opExpr.getRight();
                boolean logical = opExpr.getOperator().isLogical();
                if (logical){
                    //逻辑符号型
                    resultList.addAll(getCondByWhere(left));
                    resultList.addAll(getCondByWhere(right));
                }else{
                    resultList.add(opExpr.getRight().toString());
                }
            }else if (sqlExpr instanceof SQLInSubQueryExpr){
                MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) ((SQLInSubQueryExpr) sqlExpr).getSubQuery().getQuery();
                resultList.addAll(getWhereValue(query));
            }
        }
        return resultList;
    }


    /**
    *获取From语句中的where查询val
    *@author Andy
    *@date 2023/2/22
    */
    public static List<String> getCondByFrom(SQLTableSource tbSrc) {
        List<String> resultList = new ArrayList<>();
        if (!Objects.isNull(tbSrc)){
            if (tbSrc instanceof SQLSubqueryTableSource) {
                MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) ((SQLSubqueryTableSource) tbSrc).getSelect().getQuery();
                resultList.addAll(getWhereValue(query));
            }
        }
        return resultList;
    }

    /**
    *获取Select语句中where查询val
    *@author Andy
    *@date 2023/2/22
    */
    public static List<String> getCondBySelect(List<SQLSelectItem> selectList) {
        List<String> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(selectList)){
            for (SQLSelectItem sqlSelectItem : selectList) {
                SQLExpr expr = sqlSelectItem.getExpr();
                if (expr instanceof SQLQueryExpr){
                    MySqlSelectQueryBlock subQuery = (MySqlSelectQueryBlock) ((SQLQueryExpr) expr).subQuery.getQueryBlock();
                    resultList.addAll(getWhereValue(subQuery));
                }
            }
        }
        return resultList;
    }


    public static List<String> getWhereValue(MySqlSelectQueryBlock queryBlock){
        List<String> resultList = new ArrayList<>();
        //遍历where
        resultList.addAll(getCondByWhere(queryBlock.getWhere()));
        //遍历from
        resultList.addAll(getCondByFrom(queryBlock.getFrom()));
        //遍历select
        resultList.addAll(getCondBySelect(queryBlock.getSelectList()));
        return resultList;
    }
}
