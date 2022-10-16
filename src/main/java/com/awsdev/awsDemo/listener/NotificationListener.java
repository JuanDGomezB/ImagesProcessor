package com.awsdev.awsDemo.listener;

import com.awsdev.awsDemo.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationListener {
    private final AppService appService;

    public void readMessagesAndSendToSNSTopic () {
        var messages = appService.receiveMessagesFromSqsQueueWithDelete();
        messages.stream()
                .forEach(message -> appService.sendMessagesToSNSTopic(message.getBody()));
    }
}
