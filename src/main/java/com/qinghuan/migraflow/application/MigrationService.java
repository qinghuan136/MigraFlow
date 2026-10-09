package com.qinghuan.migraflow.application;

import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class MigrationService {

    public void prepareWorkspace(Path projectDirectory) {
        // TODO 根据目标项目目录构建 Workspace；当前仅预留入口。
    }
}
