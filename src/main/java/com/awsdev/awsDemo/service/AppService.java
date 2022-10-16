package com.awsdev.awsDemo.service;

import com.amazonaws.services.sqs.model.Message;
import com.awsdev.awsDemo.models.AwsMetaData;
import com.awsdev.awsDemo.models.ImageMetaData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AppService {
    AwsMetaData ec2Info();
    String uploadImage(MultipartFile image);
    String deleteImage(String image);
    byte[] downloadImage(String image);
    List<ImageMetaData> getAllImagesMetaData();
    List<ImageMetaData> getRandomImageMetaData();
    String addSubscriptionToTopic(String email);
    String deleteSubscriptionFromTopic(String email);
    List<Message> receiveMessagesFromSqsQueueWithDelete();
    void sendMessagesToSNSTopic(String message);
    String triggerLambdaFunction();
}
