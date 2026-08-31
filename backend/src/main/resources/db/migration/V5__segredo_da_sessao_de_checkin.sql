-- O QR de check-in era um link fixo (/checkin/confirmar/{sessaoId})
-- valido por 6 horas. Como e o MESMO link pra sala inteira, bastava um
-- aluno presente fotografar a tela e mandar no grupo pra quem nao veio
-- marcar presenca de casa.
--
-- Agora o link carrega um codigo rotativo derivado deste segredo
-- (HMAC-SHA256 do segredo com a janela de tempo atual - ver
-- CodigoRotativoCheckin). A tela do admin regenera o QR a cada janela, e
-- um print compartilhado fica invalido junto com a janela em que foi
-- tirado.
--
-- O segredo nunca sai da API: nao aparece em nenhum DTO, so no calculo
-- do codigo.
ALTER TABLE sessoes_checkin ADD COLUMN segredo VARCHAR(64) NULL;

-- Sessoes que ja existiam nao tem segredo. Deixar em branco faria o
-- codigo delas ser calculavel por qualquer um (HMAC com chave vazia e
-- deterministico), entao cada uma ganha um segredo proprio e aleatorio.
-- Elas ja estao expiradas na pratica, isso e so pra nao abrir brecha.
UPDATE sessoes_checkin
   SET segredo = SHA2(CONCAT(id, UUID(), RAND()), 256)
 WHERE segredo IS NULL;

ALTER TABLE sessoes_checkin MODIFY COLUMN segredo VARCHAR(64) NOT NULL;
