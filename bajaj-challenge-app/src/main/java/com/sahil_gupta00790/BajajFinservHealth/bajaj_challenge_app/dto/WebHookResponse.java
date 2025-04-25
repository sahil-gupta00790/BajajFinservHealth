package com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WebHookResponse {
    private String webhook;
    private String accessToken;
    private TaskDataDto data;
    
}