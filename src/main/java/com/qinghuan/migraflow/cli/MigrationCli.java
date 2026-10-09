package com.qinghuan.migraflow.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public final class MigrationCli {

    private final BufferedReader input;
    private final PrintWriter output;
    private final Consumer<Path> directoryHandler;

    public MigrationCli(Reader input, PrintWriter output, Consumer<Path> directoryHandler) {
        this.input = new BufferedReader(Objects.requireNonNull(input));
        this.output = Objects.requireNonNull(output);
        this.directoryHandler = Objects.requireNonNull(directoryHandler);
    }

    public void run() throws IOException {
        output.println("MigraFlow");
        output.println("请输入要迁移的本地目录路径（相对路径或绝对路径），输入 /exit 退出。");
        output.println("当前仅接收项目目录，Workspace 构建暂未实现。");

        while (true) {
            output.print("migraflow> ");
            output.flush();
            String line = input.readLine();
            if (line == null || line.strip().equals("/exit")) {
                output.println("已退出。");
                output.flush();
                return;
            }

            String directory = line.strip();
            if (directory.isEmpty()) {
                continue;
            }
            if (directory.length() >= 2 && directory.startsWith("\"") && directory.endsWith("\"")) {
                directory = directory.substring(1, directory.length() - 1);
            }
            if (directory.isBlank()) {
                output.println("请输入有效的本地目录路径。");
                continue;
            }

            Path projectDirectory;
            try {
                projectDirectory = Path.of(directory).toRealPath();
                if (!Files.isDirectory(projectDirectory)) {
                    output.println("该路径不是目录，请重新输入。");
                    continue;
                }
            } catch (InvalidPathException | IOException | SecurityException exception) {
                output.println("目录不存在、不可访问或路径格式无效，请重新输入。");
                continue;
            }

            try {
                directoryHandler.accept(projectDirectory);
                output.println("已接收本地目录：" + projectDirectory);
            } catch (RuntimeException exception) {
                output.println("处理目录失败：" + exception.getMessage());
            }
        }
    }
}
