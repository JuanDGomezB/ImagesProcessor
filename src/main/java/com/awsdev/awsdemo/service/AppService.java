package com.awsdev.awsdemo.service;

import com.amazonaws.services.sqs.model.Message;
import com.awsdev.awsdemo.models.AwsMetaData;
import com.awsdev.awsdemo.models.ImageMetaData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * App Service interface.
 */
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
