package com.andy.sqltodsl.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.andy.sqltodsl.bean.enums.Operator;
import com.andy.sqltodsl.bean.enums.QueryType;
import com.andy.sqltodsl.bean.factory.QueryBuilderFactory;
import com.andy.sqltodsl.bean.models.OrderColumnModel;
import com.andy.sqltodsl.bean.models.TreeNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andy
 * @date 2022-11-15
 */
public class ElasticSearchUtils {

    public static void main(String[] args) {
        String sql = "select * from text where firstrecepttimeTimestrap > 1654012800000 and firstrecepttimeTimestrap < 1656518400000 group by reasoncontrolfind";
        String dsl = convertSqlToDsl(sql);
        System.out.println(dsl);
    }


    /**
    *sql转化dsl
    *@author Andy
    *@date 2023/2/3
    */
    public static String convertSqlToDsl(String sql){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //解析where语句
        parseWhere(sql, searchSourceBuilder);
        //order
        parseOrderBy(sql, searchSourceBuilder);
        //limit
        parseLimit(sql, searchSourceBuilder);
        //group by
        parseGroupBy(sql, searchSourceBuilder);
        //select list
        parseSelectList(sql, searchSourceBuilder);
        return searchSourceBuilder.toString();
    }

    private static void parseSelectList(String sql, SearchSourceBuilder searchSourceBuilder) {
        List<String> selectList = SqlUtils.getSelectList(sql);
        if (CollectionUtils.isNotEmpty(selectList)){
            searchSourceBuilder.fetchSource(selectList.toArray(new String[0]), null);
        }
    }

    /**
     * 解析group by语句
     * @author Andy
     * @date 2023/1/19 22:03
     * @param sql sql语句
     * @param searchSourceBuilder dsl语句
     **/
    private static void parseGroupBy(String sql, SearchSourceBuilder searchSourceBuilder) {
        //获取group的字段
        List<String> fieldList = SqlUtils.getGroupByFieldList(sql);
        if (CollectionUtils.isNotEmpty(fieldList)){
            TermsAggregationBuilder last = null;
            TermsAggregationBuilder first = null;
            for (String field : fieldList) {
                TermsAggregationBuilder aggBuilder = AggregationBuilders.terms("groupBy" + field.substring(0,1).toUpperCase(Locale.ROOT) + field.substring(1)).field(field);
                if (!Objects.isNull(last)){
                    //如果last非空, 则说明是子聚合
                    last.subAggregation(aggBuilder);
                }
                if (fieldList.indexOf(field) == 0){
                    //如果是列表第一个字段, 则是最顶层聚合
                    first = aggBuilder;
                }
                last = aggBuilder;
            }
            searchSourceBuilder.aggregation(first);
        }
    }

    /**
     * 解析limit语句
     * @author Andy
     * @date 2023/1/19 21:45
     * @param sql 语句
     * @param searchSourceBuilder dsl语句
     **/
    private static void parseLimit(String sql, SearchSourceBuilder searchSourceBuilder) {
        Map<String, Object> argMap = SqlUtils.getLimitArgMap(sql);
        if (MapUtils.isNotEmpty(argMap)){
            searchSourceBuilder.from(MapUtils.getInteger(argMap, "from"));
            searchSourceBuilder.size(MapUtils.getInteger(argMap, "size"));
        }
    }


    /**
     * 解析order by语句
     * @author Andy
     * @date 2023/1/19 21:32
     * @param sql sql语句
     * @param searchSourceBuilder 查询语句builder
     **/
    private static void parseOrderBy(String sql, SearchSourceBuilder searchSourceBuilder) {
        List<OrderColumnModel> orderColumnList = SqlUtils.getOrderColumnList(sql);
        if (CollectionUtils.isNotEmpty(orderColumnList)){
            for (OrderColumnModel model : orderColumnList) {
                searchSourceBuilder.sort(model.getName(), SortOrder.fromString(model.getOrderType()));
            }
        }
    }


    /**
     * 解析where语句
     * @author Andy
     * @date 2023/1/19 22:06
     * @param sql sql语句
     * @param searchSourceBuilder dsl语句
     **/
    public static void parseWhere(String sql, SearchSourceBuilder searchSourceBuilder){
        searchSourceBuilder.query(parseQuerySqlToDsl(sql));
    }

    /**
     * 将where语句转换成dsl语句
     * @author Andy
     * @date 2023/1/19 22:15
     * @param sql sql语句
     **/
    public static BoolQueryBuilder parseQuerySqlToDsl(String sql){
        String expr = SqlUtils.getWhereStatement(sql).replace(" ", "");
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotBlank(expr)) {
            //where条件为空
            TreeNode tree = SqlUtils.parseQueryCondition(sql);
            if (!Objects.isNull(tree)) {
                return transTreeToDsl(tree, queryBuilder, tree.getValue(), new ArrayList<>(), new ArrayList<>());
            }
        }else{
            queryBuilder.must().add(QueryBuilders.matchAllQuery());
        }
        return queryBuilder;
    }


    /**
     * 解析where语句
     * @author Andy
     * @date 2023/1/19 22:16
     * @param treeNode 树节点
     * @param boolQueryBuilder bool语句
     * @param operator 操作符
     * @param rangeQueryList 本域内所有的rangeBuilder
     **/
    private static BoolQueryBuilder transTreeToDsl(TreeNode treeNode, BoolQueryBuilder boolQueryBuilder, String operator, List<RangeQueryBuilder> rangeQueryList, List<TreeNode> nestedNodeList){
        if (treeNode != null){
            if (Objects.equals(treeNode.type, 1)){
                //逻辑符号
                boolean isNew = isNew(operator, treeNode.value);
                if(!isNew) {
                    //未发生改变
                    transTreeToDsl(treeNode.left, boolQueryBuilder, treeNode.value, rangeQueryList, nestedNodeList);
                    transTreeToDsl(treeNode.right, boolQueryBuilder, treeNode.value, rangeQueryList, nestedNodeList);
                    if (CollectionUtils.isNotEmpty(nestedNodeList)){
                        boolQueryBuilder = constructNestQuery(nestedNodeList, boolQueryBuilder, operator);
                        //避免重复
                        nestedNodeList.clear();
                    }
                }else {
                    //处理该域内所有的nested查询
                    if (CollectionUtils.isNotEmpty(nestedNodeList)){
                        boolQueryBuilder = constructNestQuery(nestedNodeList, boolQueryBuilder, operator);
                        nestedNodeList.clear();
                    }
                    //发生改变，则使用新的bool
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    List<RangeQueryBuilder> rangeList = new ArrayList<>();
                    List<TreeNode> sonPathSet = new ArrayList<>();
                    transTreeToDsl(treeNode.left, boolQuery, treeNode.value, rangeList, sonPathSet);
                    transTreeToDsl(treeNode.right, boolQuery, treeNode.value, rangeList, sonPathSet);
                    if (CollectionUtils.isNotEmpty(sonPathSet)){
                        boolQuery = constructNestQuery(sonPathSet, boolQuery, treeNode.value);
                        sonPathSet.clear();
                    }
                    List<QueryBuilder> builderList = Objects.equals(operator, Operator.OR.getMark()) ? boolQueryBuilder.should() : boolQueryBuilder.must();
                    builderList.add(boolQuery);
                }
            }else{
                //表达式节点
                if (treeNode.getField().contains(".")){
                    //nested查询, 先加入列表, 出域后统一处理
                    nestedNodeList.add(treeNode);
                }else {
                    //判断与,或
                    List<QueryBuilder> builderList = Objects.equals(operator, Operator.OR.getMark()) ? boolQueryBuilder.should() : boolQueryBuilder.must();
                    //确定type
                    QueryType queryType = parseVal(treeNode, rangeQueryList);
                    //如果为或
                    QueryBuilder queryBuilder = QueryBuilderFactory.generateQueryBuilder(treeNode, queryType, rangeQueryList, operator);
                    if (!QueryType.isExist(queryType) || (QueryType.isExist(queryType) && Objects.equals(operator, Operator.OR.getMark()))) {
                        builderList.add(queryBuilder);
                    }
                }
            }
        }
        return boolQueryBuilder;
    }

    /**
     *构造BoolQueryBuilder
     *@author Andy
     *@date 2022/11/3
     */
    private static BoolQueryBuilder constructNestQuery(List<TreeNode> nestedNodeList,  BoolQueryBuilder boolQueryBuilder, String operator) {
        Set<String> pathSet = nestedNodeList.stream().map(TreeNode::getField).collect(Collectors.toSet());
        //初始化参数树
        JSONArray jsonArray = initFieldTree(pathSet);
        //为参数树赋值
        Map<String,Object> conditionMap = new HashMap<>();
        nestedNodeList.forEach(ele -> {
            Object val = Objects.equals(ele.getValType(), 0) ? Long.parseLong(ele.getValue()) : ele.getValue();
            conditionMap.put(ele.getField(), val);
        });
        //赋值
        applyValue(jsonArray, conditionMap);
        return constructNestQuery(jsonArray, boolQueryBuilder, operator, nestedNodeList);
    }

    /**
     *构造BoolQueryBuilder
     *@author Andy
     *@date 2022/11/3
     */
    public static BoolQueryBuilder constructNestQuery(JSONArray jsonArray, BoolQueryBuilder boolQuery, String operator, List<TreeNode> nestedNodeList) {
        List<RangeQueryBuilder> rangeList = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String field = jsonObject.getString("field");
            String value = jsonObject.getString("value");
            //判断与,或
            List<QueryBuilder> builderList = Objects.equals(operator, Operator.OR.getMark()) ? boolQuery.should() : boolQuery.must();
            //不考虑current, size
            if (StringUtils.isNotEmpty(value)){
                //如果值不为空, 则说明属于该层的query
                Optional<TreeNode> optionalNode = nestedNodeList.stream().filter(ele -> Objects.equals(field, ele.getField())).findAny();
                if (optionalNode.isPresent()){
                    TreeNode treeNode = optionalNode.get();
                    //确定type
                    QueryType queryType = parseVal(treeNode, rangeList);
                    //如果为或
                    QueryBuilder queryBuilder = QueryBuilderFactory.generateQueryBuilder(treeNode, queryType, rangeList, operator);
                    if (!QueryType.isExist(queryType) || (QueryType.isExist(queryType) && Objects.equals(operator, Operator.OR.getMark()))) {
                        builderList.add(queryBuilder);
                    }
                }else{
                    throw new RuntimeException("程序设计有误");
                }
            }else{
                //如果没有值，则说明属于nest字段
                builderList.add(QueryBuilders.nestedQuery(field, constructNestQuery(jsonObject.getJSONArray("children"), QueryBuilders.boolQuery(), operator, nestedNodeList), ScoreMode.Avg));
            }
        }
        return boolQuery;
    }

    /**
     *初始化Nested参数树
     *@author Andy
     *@date 2022/11/3
     */
    public static JSONArray initFieldTree(Set<String> keySet){
        JSONArray jsonArray = new JSONArray();
        JSONObject json;
        Set<String> existedSet = new HashSet<>();
        for (String key : keySet) {
            String lastName = "";
            String[] keyArray = key.split("\\.");
            if (keyArray.length < 2){
                throw new RuntimeException("nested查询字段有误");
            }
            for (String ele : keyArray) {
                String field = lastName.concat(StringUtils.isNotEmpty(lastName) ? "." : "").concat(ele);
                if (!existedSet.contains(field)) {
                    json = new JSONObject();
                    json.put("path", lastName);
                    json.put("field", field);
                    existedSet.add(field);
                    jsonArray.add(json);
                }
                lastName = field;
            }
        }
        return TreeUtils.getChildrenList("", jsonArray, "path", "field", "children");
    }

    /**
     *为参数树赋值
     *@author Andy
     *@date 2022/11/3
     */
    public static void applyValue(JSONArray jsonArray, Map<String, Object> conditionMap){
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            String field = json.getString("field");
            JSONArray children = json.getJSONArray("children");
            if (conditionMap.containsKey(field)){
                //存在值，则赋值
                json.put("value", conditionMap.get(field));
            }
            //存在子节点，则为子节点赋值
            if (CollectionUtils.isNotEmpty(children)){
                applyValue(children, conditionMap);
            }
        }
    }

    /**
    *设置rangeQueryBuilder参数
    *@author Andy
    *@date 2022/11/29
    */
    public static void setRangeQueryBuilder(RangeQueryBuilder builder, String operator, Object value){
        //如果存在多个范围查询，要考虑求交集的情况
        // >= > 求交集
        // <= < 求交集
        Long minVal = CommonUtils.getLongVal(builder.from());
        Long maxVal = CommonUtils.getLongVal(builder.to());
        Long realVal = CommonUtils.getLongVal(value);
        if (!Objects.isNull(realVal)) {
            if (Objects.equals(operator, ">=")) {
                //>= minVal
                if (!Objects.isNull(minVal)) {
                    if (realVal > minVal){
                        //实际值 > 最小值
                        builder.gte(realVal);
                    }
                }else{
                    builder.gte(realVal);
                }
            } else if (Objects.equals(operator, ">")) {
                if (!Objects.isNull(minVal)){
                    if (realVal > minVal){
                        //实际值 > 最小值
                        builder.gt(realVal);
                    }else if (Objects.equals(realVal, minVal)){
                        //如果相等，如果原本是闭区间，则去除端点值
                        if (builder.includeLower()){
                            builder.gt(realVal);
                        }
                    }
                }else{
                    builder.gt(realVal);
                }
            } else if (Objects.equals(operator, "<=")) {
                if (!Objects.isNull(maxVal)){
                    if (realVal < maxVal){
                        //实际值 < 最大值
                        builder.lte(realVal);
                    }
                }else{
                    builder.lte(realVal);
                }
            } else if (Objects.equals(operator, "<")){
                if (!Objects.isNull(maxVal)){
                    if (realVal < maxVal){
                        //实际值 < 最大值
                        builder.lt(realVal);
                    }else if (Objects.equals(realVal, maxVal)){
                        if (builder.includeUpper()){
                            builder.lt(realVal);
                        }
                    }
                }else {
                    builder.lt(value);
                }
            }
        }
    }



    /**
     *校验参数类型，0: = 整型 1: = 字符串 2:不存在的range查询 3:存在的range查询 4:nested查询
     *@author Andy
     *@param treeNode 树节点
     *@date 2022/11/29
     */
    private static QueryType parseVal(TreeNode treeNode, List<RangeQueryBuilder> rangeQueryList){
        if (Objects.equals(treeNode.getOperator(), "=")) {
            return Objects.equals(treeNode.getValType(), 0) ? QueryType.EQ_INTEGER : QueryType.EQ_STR;
        } else {
            //范围查询
            Optional<RangeQueryBuilder> optional = rangeQueryList.stream().filter(ele -> Objects.equals(ele.fieldName(), treeNode.getField())).findAny();
            return optional.isPresent() ? QueryType.EXIST_RANGE : QueryType.NOT_EXIST_RANGE;
        }

    }
    
    /**
     * 判断是否是新的操作符
     * @author Andy
     * @date `2023/1/19 21:18
     * @param initOp 初始操作符
     * @param currentOp 当前操作符
     **/
    private static boolean isNew(String initOp, String currentOp){
        //如果操作符部位null, 并且前后操作符不等，则使用新的bool查询
        return !Objects.isNull(initOp) && !Objects.equals(initOp, currentOp);
    }
}
