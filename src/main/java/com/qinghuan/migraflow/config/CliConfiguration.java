package com.qinghuan.migraflow.config;

import com.qinghuan.migraflow.application.MigrationService;
import com.qinghuan.migraflow.cli.MigrationCli;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Console;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
public class CliConfiguration {

    @Bean
    public MigrationCli migrationCli(MigrationService migrationService) {
        Console console = System.console();
        Reader input = console == null
                ? new InputStreamReader(System.in, StandardCharsets.UTF_8)
                : console.reader();
        PrintWriter output = console == null
                ? new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true)
                : console.writer();
        return new MigrationCli(input, output, migrationService::prepareWorkspace);
    }
}
