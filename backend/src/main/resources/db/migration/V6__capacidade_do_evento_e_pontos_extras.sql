-- 1) CAPACIDADE DO EVENTO
--
-- Ate agora nada impedia 300 inscricoes num auditorio de 80 lugares. O
-- limite fica por evento (o auditorio muda), e nao numa constante no
-- codigo.
--
-- NULL de proposito = "sem limite". Eventos que ja existem nao tinham
-- limite nenhum, entao NULL e o unico valor que descreve a verdade sobre
-- eles - inventar um numero aqui poderia trancar inscricoes que ja
-- estavam abertas.
ALTER TABLE eventos ADD COLUMN capacidade INT NULL;


-- 2) PONTOS EXTRAS (GINCANAS)
--
-- Algumas palestras tem gincana, e os vencedores ganham pontos que se
-- somam aos das palestras assistidas. Esses pontos nao saem de nenhuma
-- regra automatica: sao lancados a mao pelo admin.
--
-- Ficam numa tabela separada, e nao numa coluna de "saldo" no usuario,
-- por tres motivos:
--   - a pontuacao do sistema inteiro e calculada a cada consulta, nunca
--     guardada como saldo (ver PontuacaoService) - um saldo aqui seria a
--     unica excecao e sairia de sincronia;
--   - pontos extras pertencem a um PERIODO, e a pontuacao reinicia a
--     cada semestre;
--   - cada lancamento guarda quem lancou e por que, o que permite
--     conferir e desfazer um erro sem apagar o resto.
CREATE TABLE pontuacoes_extras (
    id VARCHAR(36) PRIMARY KEY,
    aluno_id VARCHAR(36) NOT NULL,
    periodo_id VARCHAR(36) NOT NULL,
    -- Pode ser negativo: e assim que o admin corrige um lancamento a
    -- maior sem precisar apagar o historico.
    pontos INT NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    -- Quem lancou. Sem isso nao ha como auditar um ranking contestado.
    criado_por_id VARCHAR(36) NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pontos_extras_aluno FOREIGN KEY (aluno_id) REFERENCES usuarios(id),
    CONSTRAINT fk_pontos_extras_periodo FOREIGN KEY (periodo_id) REFERENCES periodos(id),
    CONSTRAINT fk_pontos_extras_criado_por FOREIGN KEY (criado_por_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- O ranking soma os extras de todos os alunos de um periodo de uma vez;
-- a tela do aluno le so os dele. Este indice cobre os dois casos.
CREATE INDEX idx_pontos_extras_periodo_aluno ON pontuacoes_extras(periodo_id, aluno_id);
