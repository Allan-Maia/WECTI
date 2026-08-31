package com.wecti.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Hash da senha (BCrypt). A geracao/validacao do hash entra junto com
     * a implementacao do /auth/login na Fase 1 - aqui e so a coluna.
     */
    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    /**
     * Registro Geral do Aluno - 8 digitos, obrigatorio e unico quando
     * perfil = ALUNO. Validar essa obrigatoriedade condicional no
     * service; o banco so garante unicidade e formato (ver constraint
     * chk_rgm_formato na migration V1__init.sql).
     */
    @Column(length = 8, unique = true)
    private String rgm;

    /**
     * CPF do admin - 11 digitos, obrigatorio e unico quando
     * perfil = ADMIN (mesma logica do RGM do aluno; e o identificador
     * usado no "esqueci minha senha"), validado no service. Nao se
     * aplica a aluno.
     */
    @Column(length = 11, unique = true)
    private String cpf;

    /**
     * Curso do aluno (ex.: "Ciencia da Computacao") - confirmado com o
     * professor que e so informativo, pra saber quais cursos estao
     * participando das palestras. Nao afeta pontuacao, elegibilidade nem
     * nenhuma outra regra de negocio - por isso texto livre, opcional,
     * sem formato/unicidade exigidos.
     */
    @Column(length = 100)
    private String curso;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoPersistir() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
