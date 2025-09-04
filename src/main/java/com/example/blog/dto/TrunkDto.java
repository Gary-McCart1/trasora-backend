package com.example.blog.dto;

import lombok.Data;
import java.util.List;

@Data
public class TrunkDto {
    private Long id; // for responses
    private String name;
    private String description;
    private String username; // owner of the trunk
    private List<BranchDto> branches; // nested branches if needed
    private boolean publicFlag;
}
