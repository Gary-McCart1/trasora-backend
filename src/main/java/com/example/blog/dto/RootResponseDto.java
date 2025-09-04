package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RootResponseDto {
    private Long id;
    private String trackTitle;
    private String artistName;
    private String albumArtUrl;
    private String trackId;
    private int position;
    private String username;
}
