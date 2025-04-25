package com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.service;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.dto.TaskDataDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ChallengeStartupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ChallengeStartupRunner.class);
    private final ChallengeService challengeService;

    @Value("${user.regNo}")
    private String registrationNumber;

    // Inject ObjectMapper along with the service
    public ChallengeStartupRunner(ChallengeService challengeService, ObjectMapper objectMapper) {
        this.challengeService = challengeService;
    }

    @Override
    public void run(String... args) throws Exception {

        challengeService.initiateChallenge()
            .flatMap(webhookResponse -> {
                // --- START: Added Logging ---
                try {
                    
                    log.info("receeived w", webhookResponse.getWebhook());
                    log.info("received at", webhookResponse.getAccessToken());
                    log.info("parsed data",webhookResponse.getData());
                    
                } catch (Exception e) {
                    log.warn("error in data", e.getMessage());
                    
                }
                if (webhookResponse == null || webhookResponse.getWebhook() == null
                        || webhookResponse.getAccessToken() == null || webhookResponse.getData() == null
                        || webhookResponse.getData().getUserData() == null) {
                    return Mono.error(new RuntimeException("invalid response"));
                }

                TaskDataDto taskData = webhookResponse.getData();
                Object outcome;

                try {
                    String lastTwoDigitsStr = registrationNumber.substring(registrationNumber.length() - 2);
                    int lastTwoDigits = Integer.parseInt(lastTwoDigitsStr);

                    if (lastTwoDigits % 2 != 0) {
                        outcome = challengeService.findMutualFollowers(taskData);
                    } else {
                        outcome = challengeService.findNthLevelFollowers(taskData);
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    return Mono.error(new RuntimeException("Cannot get reg no", e));
                }

               

                log.info(" submitting");
                return challengeService.submitSolution(
                        webhookResponse.getWebhook(),
                        webhookResponse.getAccessToken(),
                        outcome
                );
            })
            .subscribe(
                null,
                error -> log.error("failed", error.getMessage()),
                () -> log.info("finished.")
            );
    }
}
