package com.jethro.utils.response;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: 沈佳俊
 * @CreateTime: 2019-07-26
 * @Description: 科大讯飞语音工具类
 */
@Component
public class VoiceRecognitionUtil {


    @Value("${ai.webroot}")
    private String webroot;

    final String URL = "http://openapi.xfyun.cn/v2/aiui";
    private static final String APPID = "5c492a63";
    private static final String API_KEY = "e97c396d36e52fc8e202a01319008109";
    private static final String DATA_TYPE = "audio";
    private static final String SCENE = "main";
    private static final String SAMPLE_RATE = "16000";
    private static final String AUTH_ID = "4a3f4a9f802140ac35ae1043d54ac2c4";
    private static final String AUE = "raw";
    // 个性化参数，需转义
    private static final String PERS_PARAM = "{\\\"auth_id\\\":\\\"4a3f4a9f802140ac35ae1043d54ac2c4\\\"}";


    private static Map<String, String> buildHeader() throws UnsupportedEncodingException, ParseException {
        String curTime = System.currentTimeMillis() / 1000L + "";
        String param = "{\"aue\":\"" + AUE + "\",\"sample_rate\":\"" + SAMPLE_RATE + "\",\"auth_id\":\"" + AUTH_ID + "\",\"data_type\":\"" + DATA_TYPE + "\",\"scene\":\"" + SCENE + "\"}";
        //使用个性化参数时参数格式如下：
        //String param = "{\"aue\":\""+AUE+"\",\"sample_rate\":\""+SAMPLE_RATE+"\",\"auth_id\":\""+AUTH_ID+"\",\"data_type\":\""+DATA_TYPE+"\",\"scene\":\""+SCENE+"\",\"pers_param\":\""+PERS_PARAM+"\"}";
        String paramBase64 = new String(org.apache.commons.codec.binary.Base64.encodeBase64(param.getBytes("UTF-8")));
        String checkSum = DigestUtils.md5Hex(API_KEY + curTime + paramBase64);

        Map<String, String> header = new HashMap<String, String>();
        header.put("X-Param", paramBase64);
        header.put("X-CurTime", curTime);
        header.put("X-CheckSum", checkSum);
        header.put("X-Appid", APPID);
        return header;
    }


    /**
     * @Author: 沈佳俊
     * @CreateTime: 2019-1-15
     * @Description: 将语音格式进行转换(.m4a->wav)
     */
    public String conversionFormat(String sourcePath) {
        StringBuilder sb = new StringBuilder();
        if (isWindows()) {
            System.out.println("windows操作系统");
            sb.append(webroot).append("/ffmpeg ");// windows ffmpeg地址
        } else {
            System.out.println("linux操作系统");
            sb.append("ffmpeg ");// windows ffmpeg地址
        }
        String targetPath = sourcePath.replaceAll(".m4a", ".wav");
        sb.append("-y -i").append(" ").append(sourcePath).append(" ").append(" -acodec pcm_s16le -ac 2 -ar 8000 ");
        sb.append(targetPath);
        Runtime run = null;
        try {
            run = Runtime.getRuntime();
            long start = System.currentTimeMillis();
            Process proc = run.exec(sb.toString());
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            System.out.println("<ERROR>");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("</ERROR>");
            int exitVal = proc.waitFor();
            long end = System.currentTimeMillis();
            System.out.println("Process exitValue: " + exitVal);
            System.out.println(sourcePath + " convert success, costs:" + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            run.freeMemory();
        }
        return targetPath;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("win")) {
            return true;
        }
        return false;
    }


    /**
     * @Author: 沈佳俊
     * @CreateTime: 2019-1-15
     * @Description: 将语音转换成文字
     */
    public String voiceToText(String targetPath) {

        Map<String, String> header = null;
        try {
            header = buildHeader();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


        byte[] dataByteArray = new byte[0];
        try {
            dataByteArray = readFile(targetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String result = httpPost(URL, header, dataByteArray);

        //将结果进行解析合并
        StringBuilder text = new StringBuilder();
        if (StringUtils.isNotEmpty(result)) {
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONArray data = jsonObject.getJSONArray("data");
            for (Object datum : data) {
                JSONObject object = (JSONObject) datum;
                text.append(object.get("text").toString());
            }
        }

        return text.toString();

    }


    private static byte[] readFile(String filePath) throws IOException {
        InputStream in = new FileInputStream(filePath);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        byte[] data = out.toByteArray();
        in.close();
        return data;
    }

    private static String httpPost(String url, Map<String, String> header, byte[] body) {
        String result = "";
        BufferedReader in = null;
        OutputStream out = null;
        try {

            java.net.URL realUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            for (String key : header.keySet()) {
                connection.setRequestProperty(key, header.get(key));
            }
            connection.setDoOutput(true);
            connection.setDoInput(true);

            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            try {
                out = connection.getOutputStream();
                out.write(body);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


}
