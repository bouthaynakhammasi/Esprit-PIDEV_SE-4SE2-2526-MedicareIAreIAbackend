package com.aziz.demosec.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WhatsAppAlertRequest {
    private Long postId;
    private String postTitle;
    private String postContent;
    private String authorName;
    private String authorRole;
    private List<String> targetRoles;
}
