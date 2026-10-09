package com.qinghuan.migraflow.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationCliTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsQuotedUnicodePathAndKeepsReadingUntilExit() throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("旧 项目"));
        List<Path> received = new ArrayList<>();

        String output = runCli("\"" + project + "\"\n" + project + "\n/exit\n", received::add);

        assertEquals(List.of(project.toRealPath(), project.toRealPath()), received);
        assertTrue(output.contains("已接收本地目录："));
        assertTrue(output.contains("已退出。"));
    }

    @Test
    void resolvesRelativeDirectoryFromWorkingDirectory() throws IOException {
        List<Path> received = new ArrayList<>();

        runCli(".\n/exit\n", received::add);

        assertEquals(List.of(Path.of(".").toRealPath()), received);
    }

    @Test
    void rejectsMissingPathsFilesAndInvalidPathsThenAcceptsDirectory() throws IOException {
        Path file = Files.writeString(temporaryDirectory.resolve("file.txt"), "baseline");
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        List<Path> received = new ArrayList<>();
        String input = temporaryDirectory.resolve("missing") + "\n" + file
                + "\ninvalid\u0000path\n" + project + "\n/exit\n";

        String output = runCli(input, received::add);

        assertEquals(List.of(project.toRealPath()), received);
        assertTrue(output.contains("目录不存在、不可访问或路径格式无效"));
        assertTrue(output.contains("该路径不是目录"));
        assertEquals("baseline", Files.readString(file));
    }

    @Test
    void handlesEmptyInputAndEndOfInputWithoutCallingWorkspaceEntry() throws IOException {
        List<Path> received = new ArrayList<>();

        String output = runCli("\n   \n\"\"\n", received::add);

        assertTrue(received.isEmpty());
        assertTrue(output.contains("请输入有效的本地目录路径"));
        assertTrue(output.contains("已退出。"));
    }

    @Test
    void exitDoesNotProcessRemainingInput() throws IOException {
        List<Path> received = new ArrayList<>();

        runCli("/exit\n" + temporaryDirectory + "\n", received::add);

        assertTrue(received.isEmpty());
    }

    @Test
    void handlerFailureDoesNotStopInteractionLoop() throws IOException {
        List<Path> received = new ArrayList<>();
        String input = temporaryDirectory + "\n" + temporaryDirectory + "\n/exit\n";

        String output = runCli(input, path -> {
            received.add(path);
            if (received.size() == 1) {
                throw new IllegalStateException("Workspace 暂不可用");
            }
        });

        assertEquals(2, received.size());
        assertTrue(output.contains("处理目录失败：Workspace 暂不可用"));
        assertTrue(output.contains("已接收本地目录："));
    }

    private String runCli(String input, Consumer<Path> handler) throws IOException {
        StringWriter output = new StringWriter();
        new MigrationCli(new StringReader(input), new PrintWriter(output), handler).run();
        return output.toString();
    }
}
