package com.sahil_gupta00790.BajajFinservHealth.bajaj_challenge_app.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class UserDto {
    private int id;
    private String name;
    private List<Integer> follows;
}
