# CLAUDE.md - Convenções do projeto WECTI

Este arquivo dá contexto para qualquer sessão de Claude Code (ou para
qualquer pessoa do time) trabalhando neste repositório. Leia antes de
gerar código novo.

## O que é o WECTI

Sistema de controle de acesso a palestras e eventos acadêmicos. Tem três
clientes, mas só uma fonte de verdade:

- **Backend (API REST)** - pasta `backend/` deste repositório. É o único
  que fala com o banco de dados. Ninguém mais acessa o MySQL direto.
- **Web do aluno** - autoatendimento (inscrição, histórico, certificado,
  cancelamento, check-in/check-out via QR code lido pela câmera do
  celular). Consome a API, não acessa o banco.
- **Web do admin** - cadastra eventos e usuários, gera o QR code de
  check-in/check-out de cada evento (projetado na tela pro aluno
  escanear com a câmera do próprio celular - sem app dedicado).

O contrato de API (fonte de verdade dos endpoints) está em
`docs/openapi.yaml`. Qualquer endpoint novo ou alterado deve ser refletido
lá primeiro, antes de implementar.

## Perfis de usuário

`admin`, `aluno` (enum `Perfil`) são os únicos perfis ativos nesta
versão - confirmado com o professor (stakeholder do projeto): não existe
perfil "operador", e o perfil `professor` (que existia numa versão
anterior) não é mais oferecido no cadastro nem tem tela própria. Ele
continua existindo no enum só por compatibilidade com cadastros que já
existiam no banco antes dessa mudança - uma conta assim ainda consegue
logar, mas não tem acesso a nada além do que qualquer usuário autenticado
tem (não é redirecionada num loop - ver `homeDoPerfil` no frontend e o
comentário em `SecurityConfig.java`). Não inventar volta desse perfil sem
confirmar de novo.

- **Admin**: cadastra eventos e usuários (só Aluno ou Admin), gera os QR
  codes de check-in/check-out, acompanha a lista de presença.
- **Aluno**: se autocadastra (nome, RGM, email, senha - ver
  `CadastroAlunoRequest`), escolhe os eventos em que participa, confirma
  a própria presença escaneando o QR code, acompanha pontuação e emite
  certificado.

## Regras de negócio já fechadas (não inventar alternativa)

- **Inscrição e cancelamento fecham juntos, 15 minutos depois do início
  do evento.** Até lá o aluno pode entrar e sair livremente; a partir daí,
  nem uma coisa nem outra. Quem ficou inscrito e não apareceu conta como
  no-show.

  A folga de 15 minutos é para quem chega atrasado: sem ela, quem aparece
  5 minutos depois do começo não se inscreve, logo não faz check-in, logo
  não pontua — ficaria de fora de uma palestra em que está presente.
  Configurável em `app.inscricao.tolerancia-apos-inicio-minutos`. O
  limite útil é a própria janela de check-in, que fecha em
  `fim do evento + app.checkin.tolerancia-depois-minutos`: uma folga
  maior que isso deixaria o aluno se inscrever numa palestra em que já
  não consegue marcar presença — ele acha que vai pontuar e não pontua.

  > Antes o cancelamento era "até 1 dia antes" e a inscrição ia até o
  > **fim** do evento — dois prazos diferentes, e nenhum deles fazia
  > sentido. O de 1 dia tornava impossível desistir de uma palestra
  > marcada para o mesmo dia; o outro deixava alguém se inscrever no meio
  > da palestra (ou depois dela, levando no-show na hora). O professor
  > pediu a mudança ao testar o sistema no ar, em setembro de 2026, e
  > pediu a folga para retardatários logo em seguida.
  >
  > Fechando os dois no mesmo instante, o incentivo fica certo: quem não
  > vai mais, cancela e devolve a vaga; quem não cancelar, perde os
  > pontos. A regra vive em `PrazoInscricao` — um lugar só, usado pela
  > inscrição, pelo cancelamento e pelo que a tela mostra.
- **No-show**: marca quem NÃO cancelou E NÃO fez check-in. Hoje vale 0
  pontos (ver Penalidade abaixo) — continua registrado porque é o que
  mostra quem reservou vaga e não apareceu.
- **Certificado**: exige **check-in + check-out**, e nada mais. Ver
  `Checkin.isPresencaQualificada()`.

  > **Não há mais exigência de permanência mínima.** Até setembro de 2026
  > pedia 75% da duração do evento, contados pela interseção entre
  > `[entrada, saida]` e o horário da palestra. O professor tirou a
  > regra: a leitura dos dois QR codes passou a ser a prova de presença.
  > O percentual continua calculado e aparece na lista de presença do
  > admin (`percentual_presenca`), mas é só informativo — não decide
  > pontuação nem certificado.
- **Pontuação**: cada evento tem um valor fixo de pontos
  (`Evento.pontos`), definido no cadastro do evento. Só conta para o
  aluno quando ele cumpre o mesmo critério do certificado - não basta o
  check-in.
- **Penalidade de no-show: ZERO.** Faltar não tira ponto nenhum
  (`app.pontuacao.penalidade-no-show`, padrão **0**). O evento continua
  aparecendo como "não compareceu" na tela do aluno e na do admin — o
  registro serve para saber quem reservou vaga e não foi —, só que
  valendo 0.

  > Esta regra já mudou duas vezes. Era "perde os pontos daquela
  > palestra"; virou "perde 100 fixos" (para que faltar custasse o mesmo
  > em qualquer palestra); e o professor zerou em setembro de 2026. A
  > configuração continua existindo justamente por isso: se voltar a
  > valer, basta trocar o número, sem reescrever código.
- **Teto de pontos de evento**: o aluno ganha no máximo
  `app.pontuacao.limite-eventos` (padrão **2000**) em palestras.

  O teto apara **apenas os ganhos**; a penalidade de no-show é descontada
  **depois** dele. Com a penalidade em zero isso não muda nada hoje —
  `min(ganhos, 2000)` e pronto —, mas a ordem está assim de propósito: se
  a penalidade voltar a valer, quem já passou do teto continua sentindo a
  falta. Aplicada sobre o saldo, ela sumiria junto com o excedente
  aparado. Exemplo com penalidade 100: 2300 ganhos + 1 falta →
  `min(2300, 2000) - 100 = 1900`.

  **Gincana fica fora do teto** (é premiação lançada à mão pelo admin,
  não pontuação de palestra). A conta inteira vive em `RegraPontuacao`,
  usada pela tela individual e pelo ranking - um número diferente nos dois
  lugares derrubaria a confiança na competição.
- **Ranking: só admin.** O aluno vê a própria pontuação (`/me/pontuacao`),
  não a classificação da turma. `GET /ranking` exige ADMIN no
  `SecurityConfig` - a restrição é do backend, não só do menu.
- **Cada pessoa corrige o próprio cadastro** em `PUT /usuarios/me`, os
  dois perfis: **aluno** em nome, RGM e curso; **admin** em nome e CPF.
  O campo do outro perfil vem no corpo mas é ignorado, então aluno não
  ganha CPF nem admin ganha RGM.

  **Não** mexe em e-mail (é o login - errar ali tiraria o acesso da
  própria pessoa), perfil (seria escalada de privilégio) nem senha (essa
  vai por `/auth/redefinir-senha`). O id vem do token, nunca do corpo.
- **O admin edita qualquer cadastro** em `PUT /usuarios/{id}` (tela de
  Usuários), incluindo e-mail e perfil - campos que o dono não mexe
  sozinho. Duas travas impedem que ele se tranque fora do sistema:

  - **não pode rebaixar a si mesmo** para ALUNO;
  - **não pode excluir a própria conta**.

  Só admin cria admin, então um admin que se rebaixasse não teria quem o
  promovesse de volta - o sistema ficaria sem administrador e só um
  acesso direto ao banco resolveria. Rebaixar **outro** admin é
  permitido: isso sempre deixa pelo menos um de pé, que é ele mesmo.
- **Não existe recorte por semestre.** A pontuação é simplesmente a do
  aluno no WECTI, somando todas as palestras mais os pontos de gincana.
  Não há reinício por período.

  > Uma versão anterior deste arquivo afirmava o contrário — "a pontuação
  > é acumulada dentro do período (semestre) e reinicia a cada novo
  > período" — e listava isso como regra fechada. **Era errado.** A regra
  > nasceu do rascunho inicial do `openapi.yaml`, escrito antes de falar
  > com o professor, e nunca foi validada. Perguntado diretamente se o
  > semestre do aluno impactaria a pontuação das palestras, ele
  > respondeu: *"Qualquer aluno pode se matricular de qualquer palestra.
  > Não precisa relacionar com nada."*
  >
  > A entidade `Periodo` foi removida na migration V7. Os dados mostravam
  > que ninguém entendia o conceito: os dois registros em produção se
  > chamavam "Matutino" e "Noturno", com datas idênticas. Pior, a
  > pontuação e o ranking buscavam "o período que contém hoje" e falhavam
  > quando não havia nenhum — as telas parariam de carregar em
  > 21/12/2026, sem erro visível.
  >
  > Se um dia for preciso separar edições do WECTI, o recorte é por
  > **data do evento**, não por uma entidade nova.
- **RGM**: identificador acadêmico do aluno, 8 dígitos, único.
  Obrigatório quando `perfil = ALUNO`; essa obrigatoriedade é validada em
  código (service), não no schema do banco (a constraint do banco só
  garante o formato de 8 dígitos quando o valor não é nulo).

## Convenções de código (backend)

- Java 17+, Spring Boot 4.1, Maven.
- Pacote base `com.wecti.api`, organizado por camada: `domain`,
  `repository`, `service`, `controller`, `config`, `dto`.
- Entidades JPA usam Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`,
  `@AllArgsConstructor`) e UUID como chave primária
  (`@GeneratedValue(strategy = GenerationType.UUID)`).
- Migrações de banco via Flyway, em `backend/src/main/resources/db/migration`.
  Nunca usar `ddl-auto: update` ou `create` - o schema só muda via
  migration versionada (`ddl-auto` está fixo em `validate`).
- Nomes de coluna em `snake_case` no banco, `camelCase` em Java (mapeado
  via `@Column(name = "...")`).

## O que NÃO está implementado neste esqueleto

Autenticação JWT real (login, geração/validação de token, autorização por
perfil) é tarefa da Fase 1 - o `SecurityConfig` atual libera tudo
(`permitAll`) só para o esqueleto subir e o Swagger UI funcionar.
