package com.awsdev.awsdemo.controller;

import com.awsdev.awsdemo.models.AwsMetaData;
import com.awsdev.awsdemo.service.AppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ControllerTest Class.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(value = ImagesController.class)
public class ImagesControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @MockBean
    private AppService appService;

    @Test
    public void eC2InstanceMetadataGetRequest() throws Exception {
        var region = "us-west-1";
        var availabilityZone = "1a";

        var awsMetaData = new AwsMetaData(region, availabilityZone);

        Mockito.when(appService.ec2Info())
                .thenReturn(awsMetaData);

        var mockResponse = "{\"region\":\"us-west-1\",\"availabilityZone\":\"1a\"}";

        mockMvc.perform(MockMvcRequestBuilders.get("/ec2info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(content().string(mockResponse));
    }

    @Test
    public void uploadImagePostRequest() throws Exception {
        var imageFile = new MockMultipartFile(
                "mockImage",
                "2-AMI.png",
                MediaType.IMAGE_PNG_VALUE,
                "2-AMI.png".getBytes());

        Mockito.when(appService.uploadImage(any()))
                .thenReturn("2-AMI.png");

        var mockResponse = "{\"Uploaded image URL\":\"2-AMI.png\"}";
        mockMvc.perform(MockMvcRequestBuilders.multipart("/upload")
                        .file("image", imageFile.getBytes()))
                .andExpect(status().is(201))
                .andExpect(content().string(mockResponse));
    }
}
