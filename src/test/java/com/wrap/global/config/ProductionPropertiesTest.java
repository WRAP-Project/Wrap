package com.wrap.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

class ProductionPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues(
                    "spring.profiles.active=prod",
                    "PORT=10000",
                    "DB_URL=jdbc:mysql://mysql.example.com:3306/wrap",
                    "DB_USERNAME=wrap_user",
                    "DB_PASSWORD=wrap_password",
                    "FRONTEND_ORIGIN=https://wrap.example.com"
            );

    @Test
    void prodProfileMapsDeploymentEnvironmentVariables() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("server.address")).isEqualTo("0.0.0.0");
            assertThat(environment.getProperty("server.port")).isEqualTo("10000");
            assertThat(environment.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:mysql://mysql.example.com:3306/wrap");
            assertThat(environment.getProperty("spring.datasource.username"))
                    .isEqualTo("wrap_user");
            assertThat(environment.getProperty("spring.datasource.password"))
                    .isEqualTo("wrap_password");
            assertThat(environment.getProperty("app.cors.allowed-origin"))
                    .isEqualTo("https://wrap.example.com");
        });
    }

    @Test
    void prodProfileUsesMySqlAndDisablesDevelopmentOptions() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                    .isEqualTo("com.mysql.cj.jdbc.Driver");
            assertThat(environment.getProperty("spring.jpa.database-platform"))
                    .isEqualTo("org.hibernate.dialect.MySQLDialect");
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("update");
            assertThat(environment.getProperty("spring.h2.console.enabled"))
                    .isEqualTo("false");
            assertThat(environment.getProperty("spring.jpa.show-sql"))
                    .isEqualTo("false");
            assertThat(environment.getProperty("spring.jpa.properties.hibernate.format_sql"))
                    .isEqualTo("false");
        });
    }

    @Test
    void prodProfileUsesCrossSiteSecureSessionCookie() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("server.servlet.session.cookie.secure"))
                    .isEqualTo("true");
            assertThat(environment.getProperty("server.servlet.session.cookie.http-only"))
                    .isEqualTo("true");
            assertThat(environment.getProperty("server.servlet.session.cookie.same-site"))
                    .isEqualTo("none");
        });
    }

    @Test
    void prodProfileOnlyExposesHealthEndpointWithoutDetails() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                    .isEqualTo("health");
            assertThat(environment.getProperty("management.endpoint.health.show-details"))
                    .isEqualTo("never");
        });
    }
}
