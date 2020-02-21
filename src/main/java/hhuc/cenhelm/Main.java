package hhuc.cenhelm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import hhuc.cenhelm.tools.TransApi;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class Main {

    /**
     * 百度翻译的id和密钥
     */
    private static final String APP_ID = "20200220000386507";
    private static final String SECURITY_KEY = "sMI8VsQ6CKEaSuVVokkr";

    /**
     * 读取txt文件
     *
     * @param pathName txt路径
     * @return 文件内容字符串
     */
    public static String read(String pathName) throws IOException {
        StringBuilder content = new StringBuilder();
        File fileName = new File(pathName);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(fileName));
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        content.append(line);
        while (line != null) {
            line = br.readLine();
            content.append(line);
        }
        return content.toString();
    }

    /**
     * 将英文文献中的单词分离保存到键值对
     *
     * @param rule    分离规则
     * @param content 文献内容
     * @return 单词和次数的键值对
     */
    public static Map<String, Integer> splitOut(String content, String rule) {
        StringTokenizer st = new StringTokenizer(content, rule);
        Map<String, Integer> wordMap = new HashMap<String, Integer>();
        while (st.hasMoreElements()) {
            String word = st.nextElement().toString().toLowerCase();
//            System.out.println(word);
            if (word.length() > 3) {
                if (wordMap.get(word) == null) {
                    wordMap.put(word, 1);
                } else {
                    int frequency = wordMap.get(word);
                    wordMap.remove(word);
                    wordMap.put(word, frequency + 1);
                }
            }

        }
        return wordMap;
    }

    /**
     * 调用百度翻译接口
     *
     * @param transApi 百度翻译接口
     * @param word     要翻译的内容
     * @return 翻译结果
     */
    public static String translate(TransApi transApi, String word) {
        //百度翻译接口返回的是json字符串
        String jsonResult = transApi.getTransResult(word, "auto", "zh");
        StringTokenizer tokenizer = new StringTokenizer(jsonResult, "\"}]");
        String result = "";
        //找到最后一个
        while (tokenizer.hasMoreTokens()) {
            result = tokenizer.nextToken();
        }
        //将json字符串简化一下
        String json = "{\"result\":\"" + result + "\"}";
        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.get("result").toString();
    }

    public static void main(String[] args) {
        // write your code here
        String content = null;
        try {
            content = read("src/main/resources/20200219.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(content);
        String rule = ", !./';:?\"()“”‘’-—$%#!&*";
        Map<String, Integer> wordMap = splitOut(content, rule);

        TransApi transApi = new TransApi(APP_ID, SECURITY_KEY);

        Iterator<String> iterator = wordMap.keySet().iterator();
        while (iterator.hasNext()) {
            String word = iterator.next();
            System.out.println(word + " " + translate(transApi, word) + " " + wordMap.get(word));
        }


    }
}
