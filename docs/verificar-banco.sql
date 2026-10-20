-- =============================================================================
-- Diagnostico do banco antes de subir uma versao nova da API
-- =============================================================================
-- Rode no phpMyAdmin (aba SQL) com o banco do WECTI selecionado.
-- Nao altera nada - so consulta.
-- =============================================================================


-- 1. Em que versao o schema esta?
--    A ultima linha mostra ate onde as migrations ja foram aplicadas.
--    Hoje o projeto vai ate a versao 4.
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;


-- 2. Alguma migration falhou no meio?
--    Se retornar QUALQUER linha, PARE: o banco esta num estado
--    inconsistente e subir por cima piora. Nesse caso e preciso reparar o
--    Flyway antes (ou restaurar um backup).
SELECT version, description, installed_on
FROM flyway_schema_history
WHERE success = 0;


-- 3. Quais tabelas existem?
--    Depois da versao 4 devem ser 9 (+ flyway_schema_history):
--    periodos, usuarios, palestrantes, eventos, evento_palestrante,
--    inscricoes, checkins, certificados, sessoes_checkin
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;


-- 4. Tem dado de verdade la dentro?
--    Se forem so testes antigos, considere limpar antes de abrir pros
--    alunos - assim o sistema comeca do zero, sem inscricao ou presenca
--    de teste contando pontuacao.
SELECT 'usuarios' AS tabela, COUNT(*) AS registros FROM usuarios
UNION ALL SELECT 'eventos',      COUNT(*) FROM eventos
UNION ALL SELECT 'inscricoes',   COUNT(*) FROM inscricoes
UNION ALL SELECT 'checkins',     COUNT(*) FROM checkins
UNION ALL SELECT 'certificados', COUNT(*) FROM certificados
UNION ALL SELECT 'periodos',     COUNT(*) FROM periodos;
