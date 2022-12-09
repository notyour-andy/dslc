package com.andy.sqltodsl.utils;

import com.andy.sqltodsl.bean.enums.Operator;
import com.andy.sqltodsl.bean.models.TreeNode;
import com.google.inject.internal.util.Preconditions;
import org.apache.commons.collections4.MapUtils;

import java.util.*;

public class CommonUtils {

    public static void main(String[] args) {
        test();
    }


    public static void test(){
        Map<String, String> map = new HashMap<>();
        map.put("业务档案", "Levent=1&GUID=98304a90-ac73-418c-a066-076aa6b80626&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_BUSINESS&IsSubitemtype=0");
        map.put("文书档案", "Levent=1&GUID=3fb9eac0-4ea8-4a35-9018-6b6d81b1a499&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_TRADITIONMETHOD&IsSubitemtype=1");
        map.put("传统方法整理", "Levent=2&GUID=2172a5f4-06a4-4810-8e7c-68a24f409fab&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_TRADITIONMETHOD&IsSubitemtype=0");
        map.put("简化方法整理", "Levent=2&GUID=cfd5985a-c8eb-494f-a2a1-9ced50c5a8a1&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_SIMPLIFYITEM&IsSubitemtype=0");
        map.put("标准档案", "Levent=1&GUID=5f39d523-0ac7-4bc0-83ef-1446bad4dcb5&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=1&TableName=T_DA_SIMPLIFYITEM&IsSubitemtype=0");
        map.put("会计档案", "Levent=1&GUID=0094bafa-ae7c-4cef-9c44-0af6e9c8ab31&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_ACCOUNTINGITEM&IsSubitemtype=0");
        map.put("特种载体", "Levent=1&GUID=27d8519d-3f6e-4b93-a866-1c30b8770a2d&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_PHOTOSFILE&IsSubitemtype=1");
        map.put("实物档案", "Levent=2&GUID=beb90342-a0f4-4233-b2e9-5386ad3b56e5&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_PHYSICALITEM&IsSubitemtype=0");
        map.put("照片档案", "Levent=2&GUID=aefd7f32-5354-4331-9347-08e43e081d73&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_PHOTOSFILE&IsSubitemtype=0");
        map.put("科技档案", "Levent=1&GUID=666fb6f7-f517-4445-8d40-08403e87f0e3&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_EQUIPMENT&IsSubitemtype=1");
        map.put("设备档案", "Levent=2&GUID=3acc7f62-2ce6-40aa-aa94-814246c8b553&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_EQUIPMENT&IsSubitemtype=0");
        map.put("设备档案子级", "Levent=2&GUID=3acc7f62-2ce6-40aa-aa94-814246c8b553&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_EQUIPMENT&IsSubitemtype=0&ProjectID=18e265d2-fb5e-457d-91af-e44afa8538cf");
        map.put("基建档案", "Levent=2&GUID=0fb126a9-d696-452e-b1ce-d5f161f7f44d&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_INFRASTRUCTURE&IsSubitemtype=0");
        map.put("基建档案子级", "Levent=2&GUID=0fb126a9-d696-452e-b1ce-d5f161f7f44d&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_INFRASTRUCTURE&IsSubitemtype=0&ProjectID=e8c57f74-9e1e-4491-ab72-c3d48830eb5a");
        map.put("环境科学研究", "Levent=1&GUID=b3dc65a9-9a1d-4d12-b391-5951883628c6&CompanyID=43f00833-586d-4f53-81c7-1e10f528ca9c&ISEdit=0&TableName=T_DA_SIMPLIFYITEM&IsSubitemtype=0");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry.getKey()+ "---------");
            for (String ele : entry.getValue().split("&")) {
                String[] split = ele.split("=");
                System.out.printf("%s: %s%n", split[0], split[1]);
            }
        }
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
                node = new TreeNode(1, null, null, Operator.getByChr(chr).getMark(), 1, left, rihgt);
            }else{
                int index = (int) chr - '0';
                //表达式
                Preconditions.checkArgument(index < condtionMapList.size() , String.format("{%s}:index大小范围有误",chr));
                Map<String, Object> dataMap = condtionMapList.get(index);
                String val = MapUtils.getString(dataMap, "value");
                //参数类型
                int valType = 0;
                if (val.startsWith("'")){
                    valType = 1;
                    val = val.substring(1, val.length()-1);
                }
                node = new TreeNode(0, MapUtils.getString(dataMap, "field"), MapUtils.getString(dataMap, "operator"), val, valType, null, null);
            }
            stack.push(node);
        }
        return stack.pop();
    }
}
