-- "Curso" (ex.: Engenharia de Software, ADS) informado pelo aluno no
-- cadastro - confirmado com o professor que e so pra saber quais cursos
-- estao participando das palestras, sem nenhuma regra de negocio (nao
-- afeta pontuacao, elegibilidade pra inscricao, nem nada) - por isso
-- texto livre e opcional, sem unicidade e sem obrigatoriedade.
ALTER TABLE usuarios ADD COLUMN curso VARCHAR(100);
