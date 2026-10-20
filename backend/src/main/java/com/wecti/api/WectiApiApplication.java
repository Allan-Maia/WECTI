package com.wecti.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DataSourceAutoConfiguration e excluida de proposito: a hospedagem
 * (Integrator Host) injeta SPRING_DATASOURCE_USERNAME/PASSWORD como
 * variavel de ambiente do processo, e variavel de ambiente sempre tem
 * prioridade sobre qualquer application.yml (interno ou externo) no
 * Spring Boot - nao tem como sobrescrever isso via YAML. O DataSource
 * real e criado manualmente em config/DataSourceConfig.java, lendo de
 * propriedades com nome proprio (app.datasource.*) que a hospedagem nao
 * injeta.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
public class WectiApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(WectiApiApplication.class, args);
    }
}