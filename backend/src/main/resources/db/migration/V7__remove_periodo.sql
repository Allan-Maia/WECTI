-- Remove o conceito de Periodo (semestre) do sistema.
--
-- POR QUE SAI
--
-- A tabela nasceu de uma suposicao do rascunho inicial do contrato de
-- API: "a pontuacao acumula dentro do semestre e reinicia a cada novo
-- periodo". Essa regra nunca foi validada com o professor. Quando ele
-- foi perguntado diretamente se o semestre do aluno impactaria a
-- pontuacao das palestras, respondeu: "Qualquer aluno pode se matricular
-- de qualquer palestra. Nao precisa relacionar com nada."
--
-- Os dados confirmam que o conceito nunca fez sentido para quem usa: os
-- dois periodos cadastrados em producao se chamavam "Matutino" e
-- "Noturno", com datas IDENTICAS (2026-08-01 a 2026-12-20). Ninguem
-- entendeu "periodo" como semestre - entenderam como turno. E, como o
-- periodo de um evento era escolhido pela data, os dez eventos caiam
-- todos no primeiro registro que o banco devolvesse, por acaso.
--
-- O QUE ISSO CONSERTA
--
-- Alem da confusao, havia uma falha com data marcada. A pontuacao e o
-- ranking, quando consultados sem informar um periodo (o caso de todas
-- as telas), buscavam "o periodo que contem hoje" e falhavam se nao
-- houvesse nenhum. Os dois periodos terminavam em 2026-12-20: a partir
-- de 21/12/2026 a aba Pontuacao de todo aluno e o Ranking parariam de
-- carregar, sem erro visivel - apenas um estado vazio, plausivel, com os
-- pontos ainda no banco.
--
-- Sem periodo, a pontuacao passa a ser simplesmente "os pontos do aluno
-- no WECTI", que e o que o professor descreveu.
--
-- SEGURANCA DOS DADOS
--
-- Nada de valor se perde: periodo_id apontava para um registro que
-- ninguem quis dizer. Inscricoes, check-ins, pontos e certificados nao
-- dependem de periodo e ficam intactos.

ALTER TABLE eventos DROP FOREIGN KEY fk_eventos_periodo;
DROP INDEX idx_eventos_periodo ON eventos;
ALTER TABLE eventos DROP COLUMN periodo_id;

ALTER TABLE pontuacoes_extras DROP FOREIGN KEY fk_pontos_extras_periodo;
DROP INDEX idx_pontos_extras_periodo_aluno ON pontuacoes_extras;
ALTER TABLE pontuacoes_extras DROP COLUMN periodo_id;

-- O ranking soma os lancamentos extras de todos os alunos, e a tela do
-- aluno le so os dele. Sem a coluna de periodo, o indice util e por
-- aluno.
CREATE INDEX idx_pontos_extras_aluno ON pontuacoes_extras(aluno_id);

DROP TABLE periodos;
