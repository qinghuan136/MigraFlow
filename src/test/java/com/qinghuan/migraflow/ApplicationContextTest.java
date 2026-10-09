package com.qinghuan.migraflow;

import com.qinghuan.migraflow.application.MigrationService;
import com.qinghuan.migraflow.cli.CliRunner;
import com.qinghuan.migraflow.cli.MigrationCli;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationContextTest {

    @Test
    void wiresCliAndEnablesAopValidationAndJsonWithoutRunningInputLoop() {
        new ApplicationContextRunner()
                .withUserConfiguration(Main.class, AspectConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(MigrationCli.class)
                            .hasSingleBean(CliRunner.class)
                            .hasSingleBean(Validator.class)
                            .hasSingleBean(JsonMapper.class);

                    MigrationService service = context.getBean(MigrationService.class);
                    assertThat(AopUtils.isAopProxy(service)).isTrue();
                    service.prepareWorkspace(Path.of("."));
                    assertThat(context.getBean(WorkspaceInvocationAspect.class).invocations).isEqualTo(1);

                    Validator validator = context.getBean(Validator.class);
                    assertThat(validator.validate(new InputSample(" "))).hasSize(1);
                    JsonMapper mapper = context.getBean(JsonMapper.class);
                    assertThat(mapper.readTree("{\"project\":\"local\"}").get("project").asText())
                            .isEqualTo("local");
                });
    }

    record InputSample(@NotBlank String project) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AspectConfiguration {

        @Bean
        WorkspaceInvocationAspect workspaceInvocationAspect() {
            return new WorkspaceInvocationAspect();
        }
    }

    @Aspect
    static class WorkspaceInvocationAspect {

        private int invocations;

        @Around("execution(* com.qinghuan.migraflow.application.MigrationService.prepareWorkspace(..))")
        public Object recordInvocation(ProceedingJoinPoint invocation) throws Throwable {
            invocations++;
            return invocation.proceed();
        }
    }
}
