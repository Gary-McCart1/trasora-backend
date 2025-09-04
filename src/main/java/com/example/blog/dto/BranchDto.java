package com.example.blog.dto;

import lombok.Data;

@Data
public class BranchDto {
    private Long id;
    private String title;
    private String artist;
    private String albumArtUrl;
    private String trackId;
    private Integer position;
    private String addedByUsername;
}
