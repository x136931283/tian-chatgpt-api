package com.tianblogs.controller;


import com.alibaba.fastjson.JSONObject;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.tianblogs.dto.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@CrossOrigin
@RestController
public class Controller {


    @PostMapping("/mySend")
    public ResponseModel mySend(HttpServletRequest request, @RequestBody JSONObject json) {
        String requestId = UUID.randomUUID().toString();
        try {
            String userMessage = json.getString("userMessage");
            String systemMessage = json.getString("systemMessage");
            String key = json.getString("key");
            String model = json.getString("model");
            if (StringUtils.hasText(model)){
                model = ChatCompletion.Model.GPT_3_5_TURBO.getName();
            }
            if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(key)) {
                return ResponseModel.fail("message can not be blank");
            }
            log.info("requestId {}, ip {}, send a message : {} ",
                    requestId, request.getRemoteHost(), userMessage);

            //国内需要代理 国外不需要

            ChatGPT chatGPT = ChatGPT.builder()
                    .apiKey(key)
                    .timeout(900)
                    .apiHost("https://api.openai.com/") //反向代理地址
                    .build()
                    .init();

            List<Message> listMessage = new ArrayList<>();
            if (StringUtils.hasText(systemMessage)){
                Message system = Message.ofSystem(systemMessage);
                listMessage.add(system);
                log.info("管理消息设置成功：{}",systemMessage);
            }

            Message message = Message.of(userMessage);
            listMessage.add(message);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model(model)
                    .messages(listMessage)
                    .maxTokens(3000)
                    .temperature(0.9)
                    .build();
            ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);
            Message res = response.getChoices().get(0).getMessage();
            log.info("requestId {}, ip {}, get a reply : {}", requestId, request.getRemoteHost(),res.getContent());
            return ResponseModel.success(res.getContent());
        } catch (Exception e) {
            log.error("requestId {}, ip {}, error", requestId, request.getRemoteHost(),e);
            return new ResponseModel(500, "error", e.getMessage());
        }
    }


}
