-- Schema inicial do WECTI, espelhando as entidades JPA e o contrato de
-- docs/openapi.yaml. UUIDs sao gerados pela aplicacao (Hibernate) e
-- armazenados como VARCHAR(36) - MySQL nao tem um tipo UUID nativo.
--
-- Colunas de data/hora usam DATETIME (nao TIMESTAMP): as entidades usam
-- java.time.LocalDateTime, que nao carrega timezone. O tipo TIMESTAMP do
-- MySQL converte para/de UTC com base no timezone da sessao, o que
-- deslocaria os horarios armazenados; DATETIME grava o valor literal,
-- igual ao TIMESTAMP (sem fuso) do Postgres usado antes.
--
-- Constraints CHECK de enum (perfil, status) e de formato (rgm) foram
-- removidas - ficam documentadas em comentario e validadas na camada de
-- servico (UsuarioService, etc.), evitando problemas de compatibilidade
-- do Flyway com CHECK no MySQL.

CREATE TABLE periodos (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(20) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usuarios (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    -- perfil: um de ADMIN, PROFESSOR, ALUNO (validado no service)
    perfil VARCHAR(20) NOT NULL,
    -- rgm: 8 digitos, obrigatorio e unico quando perfil = ALUNO
    -- (formato e obrigatoriedade condicional validados no service)
    rgm VARCHAR(8) UNIQUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE palestrantes (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    bio LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE eventos (
    id VARCHAR(36) PRIMARY KEY,
    periodo_id VARCHAR(36) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descricao LONGTEXT,
    local VARCHAR(200),
    data_hora_inicio DATETIME NOT NULL,
    data_hora_fim DATETIME NOT NULL,
    -- pontos: validado como >= 0 na camada de servico/DTO
    pontos INT NOT NULL,
    CONSTRAINT fk_eventos_periodo FOREIGN KEY (periodo_id) REFERENCES periodos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evento_palestrante (
    evento_id VARCHAR(36) NOT NULL,
    palestrante_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (evento_id, palestrante_id),
    CONSTRAINT fk_evento_palestrante_evento FOREIGN KEY (evento_id) REFERENCES eventos(id) ON DELETE CASCADE,
    CONSTRAINT fk_evento_palestrante_palestrante FOREIGN KEY (palestrante_id) REFERENCES palestrantes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inscricoes (
    id VARCHAR(36) PRIMARY KEY,
    aluno_id VARCHAR(36) NOT NULL,
    evento_id VARCHAR(36) NOT NULL,
    -- status: um de ATIVA, CANCELADA (validado no service)
    status VARCHAR(20) NOT NULL,
    criada_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelada_em DATETIME,
    qrcode_token VARCHAR(64) NOT NULL UNIQUE,
    CONSTRAINT fk_inscricoes_aluno FOREIGN KEY (aluno_id) REFERENCES usuarios(id),
    CONSTRAINT fk_inscricoes_evento FOREIGN KEY (evento_id) REFERENCES eventos(id),
    CONSTRAINT uq_inscricoes_aluno_evento UNIQUE (aluno_id, evento_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE checkins (
    id VARCHAR(36) PRIMARY KEY,
    inscricao_id VARCHAR(36) NOT NULL UNIQUE,
    entrada DATETIME NOT NULL,
    saida DATETIME,
    CONSTRAINT fk_checkins_inscricao FOREIGN KEY (inscricao_id) REFERENCES inscricoes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE certificados (
    id VARCHAR(36) PRIMARY KEY,
    inscricao_id VARCHAR(36) NOT NULL UNIQUE,
    emitido_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    url_pdf VARCHAR(500),
    CONSTRAINT fk_certificados_inscricao FOREIGN KEY (inscricao_id) REFERENCES inscricoes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_inscricoes_aluno ON inscricoes(aluno_id);
CREATE INDEX idx_inscricoes_evento ON inscricoes(evento_id);
CREATE INDEX idx_eventos_periodo ON eventos(periodo_id);
