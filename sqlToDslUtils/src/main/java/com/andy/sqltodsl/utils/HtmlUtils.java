package com.andy.sqltodsl.utils;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andy
 * @date 2022-12-09
 */
public class HtmlUtils {

    private static final List<String> TAG_LIST= Arrays.asList("input", "textarea", "select");

    public static void main(String[] args) throws IOException {
        parseHtml();

    }

    public static void parseHtml() throws IOException {
        String filePath = "C:\\Users\\Tokim\\Desktop\\Andy_files\\爬虫\\t3.html";
        File input = new File(filePath);
        Document doc = Jsoup.parse(input, "UTF-8", "http://192.168.0.125");
        //解析table
        Elements tableEleList = doc.getElementsByClass("tablebg_Common");
        List<String> nameList = new ArrayList<>();
        List<String> idList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        for (Element ele : tableEleList) {
            String id = ele.attr("id");
            if (StringUtils.isEmpty(id)){
                //id为空表格栏信息
                System.out.println(">>表格<<");

                if (ele.childrenSize() > 0) {
                    for (Element child : ele.children()) {
                        for (Element sonChildren : child.children()) {
                            for (Element lastChild : sonChildren.children()) {
                                String className = lastChild.attr("class");
                                if (Objects.equals(className, "leftTd_Common")) {
                                    //文本
                                    String text = lastChild.text();
                                    System.out.println("文本:" + text );
                                    nameList.add(text.trim().replace("：", ""));
                                }else{
                                    //value
                                    StringBuilder sb = new StringBuilder();
                                    Set<String> tagSet = lastChild.children().stream().map(andy -> andy.tag().toString()).collect(Collectors.toSet());
                                    for (Element chd : lastChild.children()) {
                                        String tag = chd.tag().toString();
                                        if (tagSet.contains("input")){
                                            if (Objects.equals(tag.toString(), "input")){
                                                System.out.println("id" + chd.id());
                                                if (sb.length() > 0){
                                                    sb.append(",");
                                                }
                                                sb.append(chd.id());
                                            }
                                        }else {
                                            if (TAG_LIST.contains(tag.toString())) {
                                                if (!StringUtils.isEmpty(chd.id())) {
                                                    System.out.println("id" + chd.id());
                                                    if (sb.length() > 0){
                                                        sb.append(",");
                                                    }
                                                    sb.append(chd.id());
                                                }
                                            }
                                        }
                                    }
                                    idList.add(sb.toString());
                                }
                            }

                        }
                    }
                }

            }else{
                //id不为空，附件列表信息
                System.out.println(">>附件列表<<");
                Node node = ele.childNode(1);
                for (Node parentNode : node.childNodes()) {
                    if (Objects.equals(parentNode.attr("align"), "top")){
                        for (int size = 0; size < parentNode.childNodeSize(); size++) {
                            Node sonNode = parentNode.childNode(size);
                            if (Objects.equals(sonNode.attr("class"), "rightTd_Common")) {
                                for (int i = 0; i < sonNode.childNodeSize(); i++) {
                                    Node n = sonNode.childNode(i);
                                    String fileEleId = n.attr("id");
                                    if (!StringUtils.isEmpty(fileEleId)){
                                        jsonObject.put("附件id", fileEleId);
                                        jsonObject.put("fileUrl", n.attr("src"));
                                    }
                                }
                            }
                        }
                    }
                }

            }

        }
        for (int size = 0; size < nameList.size(); size++) {
            if (size < idList.size()-1){
                jsonObject.put(nameList.get(size), idList.get(size));
            }
        }
        System.out.println(JSONObject.toJSONString(jsonObject));
    }
}
