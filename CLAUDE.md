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

- **Cancelamento**: o aluno pode cancelar a inscrição até 1 dia antes do
  início do evento. Depois disso, conta como no-show se não houver
  check-in.
- **No-show**: penaliza apenas quem NÃO cancelou E NÃO fez check-in.
- **Certificado**: exige check-in + check-out + permanência >= 75% da
  duração do evento (`data_hora_fim - data_hora_inicio`). Ver o método
  `Checkin.isPresencaQualificada(...)`.
- **Pontuação**: cada evento tem um valor fixo de pontos
  (`Evento.pontos`), definido no cadastro do evento. Só conta para o
  aluno quando ele cumpre o mesmo critério do certificado - não basta o
  check-in.
- **Penalidade**: o no-show desconta exatamente os pontos que aquele
  evento valeria.
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
