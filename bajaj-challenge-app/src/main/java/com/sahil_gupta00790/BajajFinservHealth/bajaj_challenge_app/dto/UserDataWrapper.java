package com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // Important for mapping
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class UserDataWrapper {


    @JsonProperty("users") 
    private List<UserDto> userList;

    private Integer findId;

    private Integer n;
}
