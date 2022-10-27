package com.awsdev.awsdemo.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.UnsubscribeRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.awsdev.awsdemo.controller.ImagesController;
import com.awsdev.awsdemo.models.AwsMetaData;
import com.awsdev.awsdemo.models.ImageMetaData;
import com.awsdev.awsdemo.repository.ImagesRepository;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * AppService implementation class.
 */
@Service
@Transactional
public class AppServiceImpl implements AppService {

    @Autowired
    private AmazonS3Client awsS3Client;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private AmazonSNSClient amazonSNSClient;

    @Autowired
    private AmazonSQSClient amazonSQSClient;

    @Autowired
    private AWSLambda awsLambda;


    @Autowired
    Environment environment;

    @Value("${bucketName}")
    private String bucketName;

    @Value("${ec2MetaDataUrl}")
    private String ec2MetaDataUrl;

    @Value("${snsTopicArn}")
    private String snsTopicArn;

    @Value("${sqsQueueUrl}")
    private String sqsQueueUrl;

    @Value("${lambdaFunctionName}")
    private String lambdaFunctionName;

    Logger log = LoggerFactory.getLogger((this.getClass()));

    @Override
    public AwsMetaData ec2Info() {
        String az, region;
        log.info("Received request to obtain ec2 metadata info");
        try {
            region = Regions.getCurrentRegion().toString();
            log.info("Requesting ec2 instance current region");
        } catch (Exception e) {
            log.error("Error trying to obtain ec2 instance current region", e);
            region = "unavailable";
        }
        try {
            log.info("Requesting ec2 instance current az");
            az = restTemplate.getForObject(ec2MetaDataUrl, String.class);
        } catch (Exception e) {
            log.error("Error trying to obtain ec2 instance current az", e);
            az = "unavailable";
        }
        return new AwsMetaData(region, az);
    }

    @Override
    public String uploadImage(MultipartFile image) {


        String fileName = StringUtils.getFilename(
          image.getOriginalFilename());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(image.getSize());
        metadata.setContentType(image.getContentType());

        ImageMetaData newImageMetaData = new ImageMetaData(FilenameUtils.removeExtension(fileName),
                "" + metadata.getContentLength(),
                StringUtils.getFilenameExtension(image.getOriginalFilename()),
                getCurrentTime());

        log.info("Received request to upload image to S3 bucket");
        try {
            awsS3Client.putObject(bucketName,
                    StringUtils.getFilename(image.getOriginalFilename()),
                    image.getInputStream(),
                    metadata);
        } catch (IOException e) {
            log.error("Error uploading image to S3 bucket", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading file");
        }
        awsS3Client.setObjectAcl(bucketName,
                fileName,
                CannedAccessControlList.PublicReadWrite);


        List<ImageMetaData> imagesMetaData =
                imagesRepository.findByName(FilenameUtils.removeExtension(fileName));
        log.info("Uploading metadata info to database");


        if (imagesMetaData.isEmpty()) {
            log.info("Image is new... creating entity");
            imagesRepository.save(newImageMetaData);
        } else {
            log.info("Image already exists... updating entity");
            imagesMetaData.stream().forEach(imageMetaData ->
                            imagesRepository.updateDate(imageMetaData.getName(), getCurrentTime())
            );
        }
        log.info("Completed request to upload image to S3 bucket");
        log.info("Sending notification to SQS queue");
        try {
            sendMessageToSqSQueue(formatMessage(newImageMetaData));
        } catch (Exception e) {

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error sending notification");
        }
        return awsS3Client.getResourceUrl(bucketName, fileName);
    }

    @Override
    public String deleteImage(String image) {
        log.info("Received request to delete image from S3 bucket");
        awsS3Client.deleteObject(
                bucketName,
                image + ".png"
        );
        List<ImageMetaData> imagesMetaData = imagesRepository.findByName(image);
        imagesMetaData.stream().forEach((imageMetaData -> imagesRepository.deleteByName(imageMetaData.getName())));
        log.info("Completed request to delete image from S3 bucket");
        return "Delete operation completed.";
    }

    @Override
    public byte[] downloadImage(String image) {
        log.info("Received request to download image from S3 bucket");
        S3ObjectInputStream s3ObjectInputStream =
                awsS3Client.getObject(
                        bucketName,
                        image + ".png"
                ).getObjectContent();
        try {
            byte[] imageContent = IOUtils.toByteArray(s3ObjectInputStream);
            return imageContent;
        } catch (IOException e) {
            log.error("Error downloading image from S3 bucket", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file");
        }
    }

    @Override
    public List<ImageMetaData> getAllImagesMetaData() {
        log.info("Received request to get all images metaData");
        return imagesRepository.findAll();
    }

    @Override
    public List<ImageMetaData> getRandomImageMetaData() {
        log.info("Received request to get a random image metaData");
        return imagesRepository.randomImage();
    }

    @Override
    public String addSubscriptionToTopic(String email) {
        try {
            log.info("Received request to subscribe to SNS topic");
            SubscribeRequest subscribeRequest = new SubscribeRequest()
                    .withTopicArn(snsTopicArn)
                    .withProtocol("email")
                    .withEndpoint(email);
            amazonSNSClient.subscribe(subscribeRequest);
        } catch (AmazonSNSException e) {
            log.error("Error when trying to subscribe to SNS topic", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during subscribing process");
        }
        log.info("Successfully subscribed to SNS topic");
        return "Successfully subscribed";
    }

    @Override
    public String deleteSubscriptionFromTopic(String email) {
        log.info("Received request to unsubscribe from SNS topic");
            var currentSubscribers = amazonSNSClient
                    .listSubscriptionsByTopic(snsTopicArn);
               currentSubscribers.getSubscriptions()
                    .stream()
                    .filter(subscription -> subscription.getEndpoint().equals(email))
                    .forEach(subscription -> unsubscribeFromTopic(subscription.getSubscriptionArn()));
            log.info("Successfully unsubscribed from SNS topic");
            return "Successfully unsubscribed";
    }

    private void unsubscribeFromTopic(String subscriptionArn) {
        try {
            UnsubscribeRequest unsubscribeRequest =
                    new UnsubscribeRequest(subscriptionArn);
            amazonSNSClient.unsubscribe(unsubscribeRequest);
        } catch (AmazonSNSException e) {
            log.error("Error when trying to unsubscribe from SNS topic", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during unsubscribing process");
        }
    }

    private String getCurrentTime() {
        DateTimeFormatter formatter
                = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm:ss a");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }

    private void sendMessageToSqSQueue(String message) {
        SendMessageRequest sendRequest = new SendMessageRequest()
                .withQueueUrl(sqsQueueUrl)
                .withMessageBody(message)
                .withDelaySeconds(5);
        amazonSQSClient.sendMessage(sendRequest);
    }
    @Override
    public List<Message> receiveMessagesFromSqsQueueWithDelete() {
        ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                .withQueueUrl(sqsQueueUrl);
        List<Message> messages =
                amazonSQSClient.receiveMessage(receiveRequest).getMessages();

        messages.stream()
                .map(Message::getReceiptHandle)
                .forEach(receiptHandle -> {
            DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest()
                    .withQueueUrl(sqsQueueUrl)
                    .withReceiptHandle(receiptHandle);
            amazonSQSClient.deleteMessage(deleteMessageRequest);
        });
        return messages;
    }
    @Override
    public void sendMessagesToSNSTopic(String message) {
        PublishRequest publishRequest = new PublishRequest(snsTopicArn, message, "Image uploaded");
        amazonSNSClient.publish(publishRequest);
    }

    @Override
    public String triggerLambdaFunction() {
        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(lambdaFunctionName);
        try {
            InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
            return new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);
        } catch (ServiceException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error during lambda function execution");
        }
    }

    private  String formatMessage(ImageMetaData imageMetaData) {
        String downloadImageUrl = linkTo(methodOn(ImagesController.class)
                .downloadImage(imageMetaData.getName()))
                .toString();
        return "New image uploaded:" + "\n"
        + "- Image Name: " + imageMetaData.getName() + "\n"
        + "- Image Size: " + imageMetaData.getImageSize() + " bytes\n"
        + "- Image extension: " + imageMetaData.getFileExtension() + "\n"
        + "- Download image: " + downloadImageUrl;
    }
}
