package com.awsdev.awsDemo.controller;

import com.awsdev.awsDemo.models.AwsMetaData;
import com.awsdev.awsDemo.models.ImageMetaData;
import com.awsdev.awsDemo.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(produces = "application/json")
public class Controller {

    @Autowired
    private AppService appService;

    @GetMapping("/ec2info")
    public ResponseEntity<AwsMetaData> ec2MetaDataInfo() {
        AwsMetaData awsMetaData = appService.ec2Info();
        return new ResponseEntity<>(awsMetaData, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("image") MultipartFile imageName
    ) {
        String imageUrl = appService.uploadImage(imageName);
        Map<String, String> response = new HashMap<>();
        response.put("Uploaded image URL", imageUrl);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteImage(
            @RequestParam("image") String imageName
    ) {
        Map<String, String> response = new HashMap();
        response.put("Operation Status", appService.deleteImage(imageName));
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @GetMapping("/download")
    public ResponseEntity<ByteArrayResource> downloadImage(
            @RequestParam("image") String imageName
    ) {
        byte[] imageObject = appService.downloadImage(imageName);
        return ResponseEntity
                .ok()
                .contentLength(imageObject.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + imageName + "\"")
                .body(new ByteArrayResource(imageObject));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<ImageMetaData>> getAllImages() {
        List<ImageMetaData> response = appService.getAllImagesMetaData();
        return new ResponseEntity<>(response, HttpStatus.OK);
    };

    @GetMapping("/random")
    public ResponseEntity<List<ImageMetaData>> randomImageMetaData() {
        List<ImageMetaData> response = appService.getRandomImageMetaData();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> addSubscriptionToSnsTopic(@RequestBody String email) {
        Map<String, String> response = new HashMap();
        response.put("email", email);
        response.put("Operation", appService.addSubscriptionToTopic(email));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> removeSubscriptionToSnsTopic(@RequestBody String email) {
        Map<String, String> response = new HashMap();
        response.put("email", email);
        response.put("Operation", appService.deleteSubscriptionFromTopic(email));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerLambdaFunction() {
        Map<String, String> response = new HashMap<>();
        response.put("Operation", appService.triggerLambdaFunction());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}


