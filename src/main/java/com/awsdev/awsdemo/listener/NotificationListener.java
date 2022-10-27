package com.awsdev.awsdemo.listener;

import com.awsdev.awsdemo.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * NotificationListener Class.
 */
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
