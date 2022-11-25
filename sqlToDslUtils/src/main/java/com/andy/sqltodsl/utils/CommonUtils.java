package com.andy.sqltodsl.utils;

import com.andy.sqltodsl.bean.enums.Operator;
import com.andy.sqltodsl.bean.models.TreeNode;
import com.google.inject.internal.util.Preconditions;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class CommonUtils {

    public static void main(String[] args) {
        String sql = "select * from z_test where a = '1' and b.dd = '2' or (c = '3' or (d = '4' and e = '5'))";
        String expr = SqlUtils.getWhereStatement(sql).replace(" ", "");
        List<String> conditionList = SqlUtils.parseQueryConditions(sql);
        List<Map<String, Object>> mapList = SqlUtils.parseQueryConditionsToMapList(sql);
        String patten = getPattens(expr, conditionList);
        String result = transInfixToSuffixExpr(patten);
        TreeNode tree = makeExprTree(result, mapList);
        System.out.println(patten);
        System.out.println(result);
    }




    /**
    *将表达式转换为指定模式，便于后续操作
    *@author Andy
    *@param expr 表达式
    *@param conditionList 查询条件列表
    *@date 2022/11/15
    */
    public static String getPattens(String expr, List<String> conditionList){
        for (String con : conditionList) {
            expr = expr.replace(con, String.valueOf(conditionList.indexOf(con)));
        }
        expr = expr.replace(Operator.OR.getMark(), Operator.OR.getChr().toString());
        expr = expr.replace(Operator.AND.getMark(), Operator.AND.getChr().toString());
        return expr;
    }

    /**
     * 将中缀表达式转换为后缀表达式
     * @author Andy
     * @date 2022/11/14 22:42
     * @param expr 表达式
     * @return 后缀表达式
     **/
    public static String transInfixToSuffixExpr(String expr){
        Stack<Character> stack = new Stack<>();
        StringBuilder sb = new StringBuilder();
        for (char chr : expr.toCharArray()){
            if (Operator.getAllChr().contains(chr)){
                //如果是字符
                boolean flag = true;
                while(flag){
                    if (!stack.isEmpty()){
                        Character peek = stack.peek();
                        Operator src = Preconditions.checkNotNull(Operator.getByChr(chr), String.format("未查询到%s对应的操作符", chr));
                        Operator desc = Preconditions.checkNotNull(Operator.getByChr(peek), String.format("未查询到%s对应的操作符", peek));
                        if (src.getPriority() < desc.getPriority()){
                            //如果小于
                            if (Objects.equals(peek, Operator.LEFT.getChr())){
                                if (Objects.equals(chr, Operator.RIGHT.getChr())){
                                    stack.pop();
                                }else{
                                    flag = false;
                                }
                            }else{
                                sb.append(stack.pop());
                            }
                        }else{
                            //如果优先级大于, 则停止遍历
                            flag = false;
                        }
                    }else{
                        //栈为空，结束遍历
                        flag = false;
                    }
                }
                if (!Objects.equals(chr, Operator.RIGHT.getChr())){
                    //是操作符不是右括号，则入栈
                    stack.push(chr);
                }
            }else{
                sb.append(chr);
            }
        }
        while(!stack.isEmpty()){
            sb.append(stack.pop());
        }
        return sb.toString();
    }

    /**
    *根据后缀表达式生成中缀表达式树
    *@author Andy
    *@param expr 后缀表达式
    *@return 中缀表达式树
    *@date 2022/11/15
    */
    public static TreeNode makeExprTree(String expr, List<Map<String,Object>> condtionMapList){
        Stack<TreeNode> stack = new Stack<>();
        for (char chr :expr.toCharArray()){
            TreeNode node = null;
            if (Operator.getAllChr().contains(chr)){
                //逻辑符号
                if (stack.size() < 2){
                    throw new RuntimeException("表达式有误");
                }
                TreeNode rihgt = stack.pop();
                TreeNode left = stack.pop();
                node = new TreeNode(1, null, null, Operator.getByChr(chr).getMark(), left, rihgt);
            }else{
                int index = (int) chr - '0';
                //表达式
                Preconditions.checkArgument(index < condtionMapList.size() , String.format("{%s}:index大小范围有误",chr));
                Map<String, Object> dataMap = condtionMapList.get(index);
                node = new TreeNode(0, MapUtils.getString(dataMap, "field"), MapUtils.getString(dataMap, "operator"), MapUtils.getString(dataMap, "value"), null, null);
            }
            stack.push(node);

        }
        return stack.pop();
    }





}
