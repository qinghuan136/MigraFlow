package com.qinghuan.migraflow.cli;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {

    private final MigrationCli cli;

    @Override
    public void run(String... args) throws IOException {
        cli.run();
    }
}
