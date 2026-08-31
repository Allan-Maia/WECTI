-- CPF do professor (mesma logica do RGM do aluno: obrigatorio e unico
-- quando perfil = PROFESSOR, validado na camada de servico).
ALTER TABLE usuarios ADD COLUMN cpf VARCHAR(11) UNIQUE;

-- O check-in deixou de ser por QR code individual do aluno (gerado na
-- inscricao, escaneado por um operador) - agora o QR e gerado pelo
-- admin/professor por EVENTO (um pra entrada, outro pra saida) e o
-- proprio aluno escaneia com o celular. O token individual da inscricao
-- nao serve mais pra nada.
ALTER TABLE inscricoes DROP COLUMN qrcode_token;

CREATE TABLE sessoes_checkin (
    id VARCHAR(36) PRIMARY KEY,
    evento_id VARCHAR(36) NOT NULL,
    -- tipo: ENTRADA ou SAIDA (validado no service)
    tipo VARCHAR(20) NOT NULL,
    criada_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em DATETIME NOT NULL,
    CONSTRAINT fk_sessoes_checkin_evento FOREIGN KEY (evento_id) REFERENCES eventos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sessoes_checkin_evento ON sessoes_checkin(evento_id);
