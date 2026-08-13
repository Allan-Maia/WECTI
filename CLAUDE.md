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
  cancelamento, QR code). Consome a API, não acessa o banco.
- **App Android** - usado só por professor e admin, para escanear o QR
  code na entrada/saída do evento e, eventualmente, cadastrar aluno ou
  convidado na hora. Não existe perfil "operador" separado.

O contrato de API (fonte de verdade dos endpoints) está em
`docs/openapi.yaml`. Qualquer endpoint novo ou alterado deve ser refletido
lá primeiro, antes de implementar.

## Perfis de usuário

`admin`, `professor`, `aluno` (enum `Perfil`). Não existe perfil "operador".

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
- **Período**: a pontuação é acumulada dentro do período (semestre) e
  reinicia a cada novo período cadastrado (`Periodo`).
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
