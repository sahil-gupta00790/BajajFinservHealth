package com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.service;


import com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
public class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);
    private final WebClient webClient;
    private final WebClient webhookSubmissionClient;
    private final int MAX_RETRY_ATTEMPTS = 4;

    @Value("${user.name}")
    private String name;
    @Value("${user.regNo}")
    private String RegNo;
    @Value("${user.email}")
    private String userEmail;

    public ChallengeService(WebClient.Builder webClientBuilder, @Value("${challenge.api.baseurl}") String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.webhookSubmissionClient = webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<WebHookResponse> initiateChallenge() {
        InitialRequest requestPayload = new InitialRequest(name, RegNo, userEmail);
        log.info("initial req", requestPayload);

        return this.webClient.post()
                .uri("/generateWebhook")
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(WebHookResponse.class)
                .doOnError(error -> log.error("error in data", error.getMessage()));
    }

    public List<List<Integer>> findMutualFollowers(TaskDataDto taskData) {
        if (taskData == null || taskData.getUserData() == null || taskData.getUserData().getUserList() == null) {
            log.warn("Null data");
            return Collections.emptyList();
        }
        List<UserDto> users = taskData.getUserData().getUserList();
        HashMap<Integer, Set<Integer>> followsMap = new HashMap<>();
        for (UserDto user : users) {
            followsMap.put(user.getId(), user.getFollows() != null ? new HashSet<>(user.getFollows()) : new HashSet<>());
        }

        Set<List<Integer>> mutualPairs = new HashSet<>();
        for (Map.Entry<Integer, Set<Integer>> entry : followsMap.entrySet()) {
            int userAId = entry.getKey();
            Set<Integer> userAFollows = entry.getValue();
            for (int userBId : userAFollows) {
                if (followsMap.containsKey(userBId) && followsMap.get(userBId).contains(userAId)) {
                    List<Integer> pair = Arrays.asList(Math.min(userAId, userBId), Math.max(userAId, userBId));
                    mutualPairs.add(pair);
                }
            }
        }
        return new ArrayList<>(mutualPairs);
    }
    //2nd question.Extra since I had time
    public List<Integer> findNthLevelFollowers(TaskDataDto taskData) {
        if (taskData == null || taskData.getUserData() == null || taskData.getUserData().getUserList() == null
                || taskData.getUserData().getFindId() == null || taskData.getUserData().getN() == null) {
            log.warn("invalid data");
            return Collections.emptyList();
        }

        List<UserDto> users = taskData.getUserData().getUserList();
        int startUserId = taskData.getUserData().getFindId();
        int targetLevel = taskData.getUserData().getN();


       
        if (targetLevel == 0) {
            
            boolean startUserExists = users.stream().anyMatch(u -> u.getId() == startUserId);
            return startUserExists ? Collections.singletonList(startUserId) : Collections.emptyList();
        }

        Map<Integer, List<Integer>> adj = new HashMap<>();
        Set<Integer> allUserIds = new HashSet<>();
        for (UserDto user : users) {
            adj.put(user.getId(), user.getFollows() != null ? new ArrayList<>(user.getFollows()) : new ArrayList<>());
            allUserIds.add(user.getId());
        }

        if (!allUserIds.contains(startUserId)) {
            log.warn("start not present", startUserId);
            return Collections.emptyList();
        }

        Queue<Integer> queue = new LinkedList<>();
        Queue<Integer> levelQueue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        List<Integer> nthLevelFollowers = new ArrayList<>();

        queue.offer(startUserId);
        levelQueue.offer(0);
        visited.add(startUserId);

        while (!queue.isEmpty()) {
            int currentUser = queue.poll();
            int currentLevel = levelQueue.poll();

            if (currentLevel >= targetLevel) {
                continue;
            }

            List<Integer> neighbors = adj.getOrDefault(currentUser, Collections.emptyList());
            for (int neighbor : neighbors) {
                 if (allUserIds.contains(neighbor) && !visited.contains(neighbor)) { 
                    visited.add(neighbor);
                    queue.offer(neighbor);
                    levelQueue.offer(currentLevel + 1);

                    if (currentLevel + 1 == targetLevel) {
                        nthLevelFollowers.add(neighbor);
                    }
                }
            }
        }


        Collections.sort(nthLevelFollowers);
        return nthLevelFollowers;
    }


    public Mono<Void> submitSolution(String webhookUrl, String accessToken, Object outcome) { // Changed List<List<Integer>> to Object
        if (webhookUrl == null || webhookUrl.isBlank()) {
           log.error("no url");
           return Mono.error(new IllegalArgumentException("URL cannot be empty"));
        }
       SubmissionPayload payload = new SubmissionPayload(RegNo, outcome); // Pass the generic outcome
       

       return this.webhookSubmissionClient.post()
               .uri(webhookUrl)
               .header(HttpHeaders.AUTHORIZATION, accessToken)
               .bodyValue(payload)
               .retrieve()
               .bodyToMono(Void.class)
               .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1))
                       .doBeforeRetry(retrySignal -> log.warn("failed,try number{}",
                               retrySignal.totalRetries() + 1))
               )
               .doOnSuccess(v -> log.info("submitted"))
               .doOnError(error -> log.error("failed after 4 retries: {}", MAX_RETRY_ATTEMPTS, error.getMessage()));
   }
}

