package com.example.salonflow.dto.Upload;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UploadResponse {

    private Long id;

    private String url;

    private String fileName;

    private Long size;

    private String contentType;
}