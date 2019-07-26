package com.jethro.controller;

import com.jethro.service.VoiceRecognitionService;
import com.jethro.utils.response.ResponseVO;
import com.jethro.utils.response.SnowflakeIdWorker;
import com.jethro.utils.response.VoiceRecognitionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Author: 沈佳俊
 * @CreateTime: 2019-07-26
 * @Description: 科大讯飞语音识别
 */
@RestController
@RequestMapping("/api/voice")
@Api(value = "VoiceRecognitionController", description = "人脸识别接口")
public class VoiceRecognitionController {

    @Autowired
    private VoiceRecognitionUtil voiceRecognitionUtil;


    @ApiOperation(value = "上传语音进行识别")
    @PostMapping("/transform")
    @CrossOrigin
    public ResponseVO registerPic(@RequestParam("file") MultipartFile file) throws IOException {


        Long fileName = SnowflakeIdWorker.generateId();

        String originalFilename = file.getOriginalFilename();

        String filePath = "C:/a/" + fileName + originalFilename;


        //读取语音
        byte[] bytes = file.getBytes();
        getFile(bytes, "C:/a/", fileName + originalFilename);

        String targetpath = voiceRecognitionUtil.conversionFormat(filePath);

        String content = voiceRecognitionUtil.voiceToText(targetpath);
        System.out.println(content);
        return ResponseVO.build().setCode(200).setMessage("转换成功!").setData(content);

    }


    /**
     * 根据byte数组，生成文件
     * filePath  文件路径
     * fileName  文件名称（需要带后缀，如*.jpg、*.java、*.xml）
     */
    public static void getFile(byte[] bfile, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {//判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


}
