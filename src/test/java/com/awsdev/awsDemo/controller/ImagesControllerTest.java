package com.awsdev.awsDemo.controller;

import com.awsdev.awsDemo.models.AwsMetaData;
import com.awsdev.awsDemo.service.AppService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void getEC2InstanceMetadataGetRequest() throws  Exception {
        var region = "us-west-1";
        var availabilityZone = "1a";

        var awsMetaData = new AwsMetaData(region, availabilityZone);

        Mockito.when(appService.ec2Info())
                .thenReturn(awsMetaData);

        var requestBuilder = MockMvcRequestBuilders.get(
                "/ec2info");

        var requestResponse = mockMvc.perform(requestBuilder).andReturn();
        mockMvc.perform(requestBuilder.contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        var mockResponse = "{\"region\":\"us-west-1\",\"availabilityZone\":\"1a\"}";
        Assertions.assertEquals(mockResponse, requestResponse.getResponse().getContentAsString());
    }
}
