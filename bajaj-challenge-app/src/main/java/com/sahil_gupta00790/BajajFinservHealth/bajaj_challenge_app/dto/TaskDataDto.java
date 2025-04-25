package com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // Important for mapping
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskDataDto {

    @JsonProperty("users") 
    private UserDataWrapper userData;
 } 