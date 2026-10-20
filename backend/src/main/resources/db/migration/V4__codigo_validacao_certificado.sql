-- Codigo publico de validacao do certificado (ex.: WCT-2026-A7F3K2) -
-- impresso no PDF junto do QR code, e resolvido pela pagina publica
-- GET /validar/{codigo}. Substitui a assinatura fisica de coordenador
-- como prova de autenticidade (mesmo modelo de Coursera/Alura/FIAP).
--
-- Coluna NULLABLE de proposito, apesar de o app sempre gravar um valor:
-- linhas antigas (certificados emitidos antes desta versao) nao tem
-- codigo, e o backfill abaixo cobre as que existem hoje. Deixar
-- NOT NULL faria a migration quebrar se aparecesse alguma linha nova
-- sem codigo entre o deploy e o backfill; o CertificadoService gera o
-- codigo sob demanda pra qualquer linha que ainda esteja sem.
--
-- UNIQUE e a rede de seguranca contra codigo repetido: o app ja sorteia
-- de novo em caso de colisao, mas se essa checagem falhar (duas emissoes
-- simultaneas sorteando o mesmo codigo, por exemplo), o banco recusa em
-- vez de gravar dois certificados com o mesmo codigo publico.
ALTER TABLE certificados ADD COLUMN codigo VARCHAR(20) NULL UNIQUE;

-- Backfill das linhas ja existentes. Usa UUID() (aleatorio) em vez do id
-- da propria linha pra nao deixar o codigo publico derivavel do id
-- interno. O alfabeto aqui e hexadecimal (limitacao do UUID do MySQL),
-- diferente do alfabeto sem ambiguidade que o app usa daqui pra frente -
-- aceitavel porque essas linhas antigas correspondem a PDFs que foram
-- emitidos sem codigo nenhum impresso, entao esse valor nunca vai ser
-- digitado por ninguem; serve so pra nao deixar a coluna vazia.
UPDATE certificados
SET codigo = CONCAT('WCT-', YEAR(emitido_em), '-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 6)))
WHERE codigo IS NULL;
