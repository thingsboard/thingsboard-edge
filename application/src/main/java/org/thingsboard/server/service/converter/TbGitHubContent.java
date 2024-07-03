package org.thingsboard.server.service.converter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbGitHubContent {
    private String name;
    private String path;
    private String sha;
    private String url;
    private String git_url;
    private String html_url;
    private String download_url;
    private String type;
    private String content;
    private String encoding;
}