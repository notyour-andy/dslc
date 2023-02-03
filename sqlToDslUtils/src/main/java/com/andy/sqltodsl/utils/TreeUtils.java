package com.andy.sqltodsl.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Andy
 * @date 2023-02-02
 */
public class TreeUtils {

    /**
     *构建指定pid下的节点树
     *@author Andy
     *@param pid  父节点id
     *@param array 全部实体数据，
     *@date 2022/9/28
     */
    public static JSONArray getChildrenList(String pid, JSONArray array, String pidName, String idName, String childName) {
        List<JSONObject> childrenList = array.stream().filter(m -> Objects.equals(((JSONObject)m).getString(pidName), pid)).map(
                (m) -> {
                    ((JSONObject) m).put(childName, getChildrenList(((JSONObject) m).getString(idName), array, pidName, idName, childName));
                    return (JSONObject) m;
                }
        ).collect(Collectors.toList());
        JSONArray jsonArray = new JSONArray();
        if (CollectionUtils.isNotEmpty(childrenList)) {
            jsonArray.addAll(childrenList);
        }
        return jsonArray;
    }
}
