package com.wecti.api.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * DataSource montado manualmente, lendo de app.datasource.* (namespace
 * proprio, fora da convencao spring.datasource.*) em vez de deixar o
 * Spring Boot auto-configurar a partir de spring.datasource.username/
 * password - a hospedagem injeta SPRING_DATASOURCE_USERNAME/PASSWORD
 * como variavel de ambiente do processo, e variavel de ambiente sempre
 * ganha de qualquer application.yml. Como app.datasource.* nao e uma
 * propriedade padrao do Spring, a hospedagem nao tem como injetar valor
 * nela por engano, entao o valor que vem do application.yml (ou de
 * DB_USER/DB_PASSWORD, se essas estiverem configuradas la) e respeitado.
 *
 * Ver WectiApiApplication (DataSourceAutoConfiguration excluida) - sem
 * isso, o Spring Boot ainda tentaria criar o DataSource dele mesmo com
 * as credenciais erradas do ambiente, e o boot quebraria antes mesmo
 * deste bean entrar em uso.
 *
 * Nao tem @Profile aqui de proposito: tanto producao (application.yml)
 * quanto local (application-dev.yml) definem app.datasource.* - se essa
 * classe so valesse para "!dev", o perfil dev ficaria sem nenhum
 * DataSource (a auto-configuracao esta excluida incondicionalmente).
 */
@Configuration
public class DataSourceConfig {

    @Value("${app.datasource.url}")
    private String url;

    @Value("${app.datasource.username}")
    private String username;

    @Value("${app.datasource.password}")
    private String password;

    @Value("${app.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }
}
