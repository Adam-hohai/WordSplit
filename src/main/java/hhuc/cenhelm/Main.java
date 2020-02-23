package hhuc.cenhelm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import hhuc.cenhelm.tools.TransApi;

import java.io.*;
import java.sql.*;
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

    /**
     * 连接数据库
     *
     * @return 返回Connection对象
     * @throws Exception 可能抛出异常
     */
    public static Connection getConnection() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = "jdbc:sqlserver://127.0.0.1:1433;DatabaseName=EnNewspaperHKW";
        String user = "sa";
        String password = "990919";
        Connection conn = DriverManager.getConnection(url, user, password);
        System.out.println("数据库连接成功");
        return conn;
    }

    public static void main(String[] args) throws Exception {
        // write your code here

        String content = null;
        try {
            content = read("src/main/resources/20200223.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(content);
        String rule = ", !./';:?\"()“”‘’-—$%#!&*——_1234567890|`~·";
        Map<String, Integer> wordMap = splitOut(content, rule);
        //调用百度翻译的接口
        TransApi transApi = new TransApi(APP_ID, SECURITY_KEY);
        //遍历键值对,操作数据库
        Connection connection = getConnection();
        PreparedStatement psSel = null, psIns = null, psUpd = null;
        ResultSet rsSel = null;
        String sqlSel, sqlIns, sqlUpd = "";
        Iterator<String> iterator = wordMap.keySet().iterator();
        while (iterator.hasNext()) {
            String word = iterator.next();
            String translation = translate(transApi, word);
            int frequency = (int)wordMap.get(word);
            System.out.println(word + " " + translation + " " + frequency);

            //写入txt
            File fileName = new File("src/main/resources/20200223output.txt");
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName,true));//文件追加
            out.write(word + " " + translation + " " + frequency + "\r\n");
            out.flush();
            out.close();

            //操作数据库
            sqlSel = "select * from hkwDailyData where word=?";
            psSel = connection.prepareStatement(sqlSel);
            psSel.setString(1, word);
            rsSel = psSel.executeQuery();
            if (rsSel.next()) {
                sqlUpd = "update hkwDailyData set frequency = frequency +? where word =?";
                psUpd = connection.prepareStatement(sqlUpd);
                psUpd.setInt(1,frequency);
                psUpd.setString(2,word);
                psUpd.executeUpdate();
                System.out.println("更新成功");

            } else {
                sqlIns = "insert into hkwDailyData(word,translation,frequency) values (?,?,?)";
                psIns = connection.prepareStatement(sqlIns);
                psIns.setString(1, word);
                psIns.setString(2, translation);
                psIns.setInt(3, frequency);
                psIns.executeUpdate();
                System.out.println("插入成功");
            }

        }
        psUpd.close();
        psIns.close();
        psSel.close();
        rsSel.close();
        connection.close();
        System.out.println("提取结束");
    }
}
