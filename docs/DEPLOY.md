# Deploy do WECTI

Guia para colocar o sistema no ar. Escrito para ser seguido de cima para
baixo — a ordem importa.

O sistema tem três peças: o **frontend** (arquivos estáticos), a **API**
(um `.jar` Java) e o **banco MySQL**.

---

## 1. Antes de tudo: HTTPS

**Isto não é configuração da aplicação — é do painel da hospedagem.**

Sem HTTPS, senha e token de sessão trafegam legíveis pela rede. Em Wi-Fi
de campus isso é interceptável por qualquer pessoa conectada.

### Passo a passo no cPanel

**Emitir o certificado**

1. Entrar no cPanel e procurar a seção **Security** (Segurança).
2. Abrir **SSL/TLS Status** — é a tela que lista os domínios e mostra se
   cada um já tem certificado.
   - Se aparecer o botão **Run AutoSSL**, clicar. O cPanel emite sozinho
     (Let's Encrypt) e leva de segundos a alguns minutos.
   - Se não houver AutoSSL, procurar **Let's Encrypt™ SSL** na seção
     Security, selecionar o domínio e clicar em *Issue*.
   - Se nenhuma das duas existir, o recurso pode estar desativado no
     plano — nesse caso é preciso abrir chamado com a Integrator Host
     pedindo AutoSSL/Let's Encrypt para o domínio.
3. Voltar em **SSL/TLS Status** e confirmar que o domínio aparece com o
   cadeado verde / *Certificate is valid*.

**Forçar HTTPS**

4. Ir em **Domains** (Domínios) no cPanel. Na linha do domínio há uma
   chave **Force HTTPS Redirect** — ligar.
   - Se essa opção não existir na sua versão do painel, dá para fazer
     pelo `.htaccess` (ver abaixo).
5. Conferir: abrir `http://jadir9152.c44.integrator.host` no navegador.
   Deve virar `https://` sozinho, com o cadeado fechado.

> **Ordem importa:** só ligue o Force HTTPS **depois** que o certificado
> estiver emitido e válido. Ligar antes deixa o site inacessível, porque
> ele passa a redirecionar para um HTTPS que ainda não funciona.

**Alternativa pelo `.htaccess`** (só se o painel não tiver a opção)

O projeto já publica um `.htaccess` na raiz do site. Se precisar forçar
HTTPS por ali, acrescente estas linhas **no topo** do arquivo:

```apache
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteCond %{HTTPS} off
  RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
</IfModule>
```

Do lado da aplicação **já está tudo pronto**: a API está configurada com
`server.forward-headers-strategy=framework`, que faz ela entender que a
requisição original veio por HTTPS mesmo recebendo HTTP do servidor web.
Isso é necessário para duas coisas funcionarem corretamente atrás do
proxy: o IP real do visitante (usado no limite de tentativas de login) e
qualquer URL que a aplicação gere.

### E se o certificado ainda não saiu?

Dá para adiantar o deploy em HTTP e migrar depois — mas os endereços
precisam ser **coerentes entre si** nas duas fases. O navegador compara a
origem exata: `http://dominio` e `https://dominio` são origens
**diferentes**, e misturar as duas faz o CORS bloquear todas as chamadas
do site (o sintoma é o login falhar com "Não foi possível entrar" mesmo
com a senha certa).

**Fase 1 — enquanto está só em HTTP**

| Onde | Valor |
|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://jadir9152.c44.integrator.host` |
| `FRONTEND_URL` | `http://jadir9152.c44.integrator.host` |
| `frontend/.env.production` | `VITE_API_URL=http://jadir9152.c44.integrator.host` |

E **não ligue** o *Integrator Force HTTPS* ainda.

**Fase 2 — quando o certificado estiver válido**

1. Trocar os três valores acima para `https://`.
2. Refazer o build do frontend (`npm run build`) e subir de novo — o
   endereço da API fica **gravado dentro** dos arquivos no build, então
   trocar só a variável não adianta.
3. Reiniciar a API para as variáveis novas valerem.
4. **Agora sim** ligar o *Integrator Force HTTPS*.

Se preferir evitar esse retrabalho, espere o certificado e faça tudo
direto em `https://`.

---

## 2. Banco de dados no cPanel

### Se o banco JÁ existe (versão anterior do sistema)

O Flyway aplica sozinho as migrations que faltam — mas **faça backup
antes**, porque uma delas apaga uma coluna.

1. **Backup primeiro.** cPanel → **Backup** → *Download a MySQL Database
   Backup* → escolher o banco. Guarde o arquivo.
2. cPanel → **phpMyAdmin** → selecionar o banco → aba **SQL** → colar o
   conteúdo de `docs/verificar-banco.sql` e executar. Ele mostra em que
   versão o schema está e se alguma migration falhou.
3. Comparar com o que o sistema espera hoje: **versão 4**.

| Última versão no banco | O que o Flyway vai aplicar sozinho |
|---|---|
| 1 | V2, V3 e V4 |
| 2 | V3 e V4 |
| 3 | Só a V4 |
| 4 | Nada — já está atualizado |

O que cada uma faz:

- **V2** — adiciona `cpf` em usuários, cria a tabela `sessoes_checkin` e
  **apaga a coluna `inscricoes.qrcode_token`**. Essa é a parte destrutiva:
  a coluna era do modelo antigo de QR por aluno, que não existe mais. Não
  é usada por nada hoje, mas é o motivo do backup.
- **V3** — adiciona `curso` em usuários.
- **V4** — adiciona `codigo` em certificados e preenche os já existentes.

> Se a consulta 2 do arquivo retornar alguma linha (`success = 0`), **não
> suba a aplicação**: há uma migration que falhou no meio e o banco está
> inconsistente. Restaure o backup ou me chame antes de continuar.

Se o banco tiver apenas dados de teste, vale limpar antes de abrir para os
alunos — assim ninguém começa com inscrição ou pontuação de mentira.

### Se o banco ainda NÃO existe

1. cPanel → **MySQL® Databases**.
2. Em *Create New Database*, criar o banco. O cPanel prefixa o nome com a
   conta — anote o nome **completo** que aparecer depois de criado
   (algo como `jadir9152_wecti`).
3. Em *MySQL Users → Add New User*, criar o usuário e uma senha forte.
   Anote também o nome completo do usuário (também vem prefixado).
4. Em *Add User To Database*, associar o usuário ao banco e marcar
   **ALL PRIVILEGES**.

Guarde os três valores — são o `DB_USER`, o `DB_PASSWORD` e o `DB_NAME`.
As tabelas **não** precisam ser criadas à mão: o Flyway cria tudo no
primeiro start da API.

> O padrão de `DB_NAME` é `jadir9152_wecti`. Se o seu banco tiver outro
> nome, defina a variável `DB_NAME` — não é preciso recompilar nada.

---

## 3. Variáveis de ambiente da API

Configurar no painel da hospedagem (ou no `application.yml` externo, em
`appservers/standalone`, se o painel não tiver campo para variáveis).

> **Atenção — isto já causou problema neste projeto.** A Integrator Host
> injeta automaticamente variáveis chamadas `SPRING_DATASOURCE_USERNAME` e
> `SPRING_DATASOURCE_PASSWORD` no processo, e variável de ambiente sempre
> vence o `application.yml`. Por isso a aplicação usa um namespace próprio
> (`app.datasource.*`, lido por `DataSourceConfig`) e **ignora**
> `spring.datasource.*`. Não renomeie essas propriedades achando que está
> "padronizando" — a conexão volta a quebrar.

### Obrigatórias — a aplicação **não sobe** sem elas

| Variável | O que é |
|---|---|
| `DB_USER` | Usuário do MySQL |
| `DB_PASSWORD` | Senha do MySQL |
| `JWT_SECRET` | Chave que assina os tokens de login |

O `JWT_SECRET` precisa ser longo e aleatório, **exclusivo de produção** —
nunca o mesmo usado em desenvolvimento. Quem tiver essa chave consegue
forjar um token de admin. Para gerar:

```bash
openssl rand -base64 48
```

### Obrigatórias para o site funcionar corretamente

| Variável | Valor | Por quê |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `https://jadir9152.c44.integrator.host` | Sem isso o navegador bloqueia todas as chamadas do site para a API |
| `FRONTEND_URL` | `https://jadir9152.c44.integrator.host` | Endereço de reserva usado ao montar os links dos QR codes |

O padrão de `CORS_ALLOWED_ORIGINS` libera `localhost` e faixas de rede
local — correto para desenvolvimento, **errado para produção**. Definir a
variável substitui o padrão inteiro.

### Recomendadas

| Variável | Valor sugerido | Por quê |
|---|---|---|
| `SPRINGDOC_ENABLED` | `false` | Desliga a documentação interativa da API (`/docs` e `/api-docs`). Não há motivo para deixar o mapa dos endpoints aberto ao público |
| `RATE_LIMIT_MAX_FALHAS` | `10` (padrão) | Tentativas de login com senha errada antes de bloquear o IP |
| `RATE_LIMIT_JANELA_MINUTOS` | `15` (padrão) | Duração do bloqueio |

Sobre o limite de tentativas: ele conta **apenas tentativas que falham**.
Numa palestra, a turma toda acessa pelo mesmo Wi-Fi e sai com o mesmo IP
público — se o contador somasse acessos bem-sucedidos, os alunos
bloqueariam uns aos outros. Quem digita a senha certa nunca entra na
conta. O bloqueio vale para as três rotas de autenticação em conjunto,
então não dá para contorná-lo trocando de rota.

---

## 4. Frontend

Antes de gerar o build, apontar `frontend/.env.production` para o
endereço **HTTPS** da API:

```
VITE_API_URL=https://jadir9152.c44.integrator.host
```

Depois:

```bash
cd frontend
npm ci
npm run build
```

### Publicando no cPanel, passo a passo

**No seu computador:**

1. Confirme o conteúdo de `frontend/.env.production` (o endereço da API).
2. Gere o build:
   ```bash
   cd frontend
   npm ci
   npm run build
   ```
3. Confira que o `.htaccess` foi junto:
   ```bash
   ls -a dist/
   ```
   Tem que aparecer `.htaccess`, `index.html` e `assets/`.
4. Compacte o **conteúdo** de `dist/`, não a pasta. Ao abrir o zip você
   deve ver `index.html` na raiz — se vir uma pasta `dist` dentro, o site
   fica em `/dist/` e não funciona.
   - No Windows: entre em `dist`, `Ctrl+A`, botão direito → *Enviar para →
     Pasta compactada*.
   - O Explorer do Windows **não** inclui arquivos que começam com ponto
     por padrão. Se o `.htaccess` não entrar no zip, envie ele à parte
     (passo 9).

**No cPanel:**

5. **Gerenciador de arquivos** → entrar em `public_html`.
6. Clicar em **Settings** (canto superior direito) → marcar
   **Show Hidden Files (dotfiles)** → *Save*. Faça isso **agora**, antes
   de subir: sem essa opção você não enxerga o `.htaccess` e não tem como
   conferir nada.
7. Se já houver site antigo: selecionar tudo e **Compress** para
   `backup-site-antigo.zip` (fica guardado), depois apagar os originais.
8. **Upload** do zip → voltar para `public_html` → botão direito no zip →
   **Extract** → apagar o zip depois.
9. Conferir que `public_html` tem: `index.html`, `assets/` e **`.htaccess`**.
   Se o `.htaccess` não estiver lá:
   - **+ File** → nome `.htaccess` → **Create New File**
   - botão direito nele → **Edit** → colar o conteúdo de
     `frontend/public/.htaccess` → *Save Changes*

### Conferindo

Abra o site e teste **as duas coisas separadamente**:

1. `http://jadir9152.c44.integrator.host` — deve carregar a tela de login.
   Se aparecer página em branco, abra o console do navegador (F12): erro
   404 em arquivo `.js` normalmente significa que a pasta `dist` foi
   junto no zip.
2. `http://jadir9152.c44.integrator.host/validar` — **este é o teste do
   `.htaccess`**. Se carregar a tela de validação, está certo. Se der
   **404 do servidor**, o `.htaccess` não está funcionando, e todos os
   links de QR code vão falhar.

O segundo teste é o que costuma ser esquecido, porque a home funciona sem
o `.htaccess` — o problema só aparece quando alguém escaneia um QR.

O `.htaccess` necessário **já está no projeto** (`frontend/public/.htaccess`)
e é copiado para `dist/` automaticamente no build. Ele existe porque as
rotas do site (`/eventos`, `/validar/...`, `/checkin/confirmar/...`) só
existem no navegador, não como arquivos no servidor — sem ele, abrir um
link de QR code direto devolveria 404. Só confirme que o arquivo subiu:
alguns clientes de FTP escondem arquivos que começam com ponto.

### Se a API ficar em um endereço diferente do site

O `.env.production` está apontando para o mesmo endereço do site
(`https://jadir9152.c44.integrator.host`). Se o professor decidir separar
— por exemplo, a API em um subdomínio ou sob um caminho `/api` — basta
ajustar duas coisas, e nada mais no código:

| Cenário | `VITE_API_URL` (frontend) | `CORS_ALLOWED_ORIGINS` (API) |
|---|---|---|
| Mesmo endereço (atual) | `https://jadir9152.c44.integrator.host` | `https://jadir9152.c44.integrator.host` |
| API sob `/api` | `https://jadir9152.c44.integrator.host/api` | `https://jadir9152.c44.integrator.host` |
| API em subdomínio | `https://api.SEUDOMINIO` | `https://SEUDOMINIO` (a origem do **site**, não da API) |

A regra do `CORS_ALLOWED_ORIGINS` é sempre a mesma: ele lista de onde o
**navegador** está chamando, ou seja, o endereço do site — nunca o da API.

---

## 5. Backend

```bash
cd backend
mvn clean package
```

O arquivo gerado é `backend/target/wecti-api-0.1.0-SNAPSHOT.jar` (cerca de
74 MB — contém tudo, inclusive as fontes do certificado).

Repare que **não** usamos `-DskipTests`: os testes automatizados rodam
como parte do build e não precisam de banco (usam H2 em memória). Se algum
falhar, o `.jar` não é gerado — que é justamente o objetivo, para não
publicar uma versão com regra de negócio quebrada.

### Publicando pelo Integrator Spring Boot

O plugin fica em cPanel → seção **Avançado** → **Integrator Spring Boot**.

1. **Se já houver uma aplicação rodando ali, pare ela primeiro.** Duas
   instâncias tentando a mesma porta fazem a nova falhar com *Port already
   in use*.
2. **Upload do `.jar`** (`wecti-api-0.1.0-SNAPSHOT.jar`, ~74 MB). Se o
   upload cair no meio pelo navegador, envie por FTP e depois aponte o
   caminho no plugin.
3. **Anote a porta** que o plugin atribuir. Ela é interna — o visitante
   nunca a digita; o servidor web repassa. Se houver campo de porta,
   ela corresponde à variável `SERVER_PORT`.
4. **Configurar as variáveis de ambiente** (seção 3). Duas formas:

   **a) O plugin tem campo de variáveis** — preencha ali, uma por linha,
   no formato `NOME=valor`.

   **b) O plugin não tem campo** — crie um `application.yml` externo em
   `appservers/standalone` (pelo Gerenciador de arquivos). O conteúdo é
   YAML, não `NOME=valor`:

   ```yaml
   app:
     datasource:
       username: jadir9152_seuusuario
       password: SUA_SENHA_DO_BANCO
     cors:
       allowed-origins: https://jadir9152.c44.integrator.host
     frontend-url: https://jadir9152.c44.integrator.host
   jwt:
     secret: COLE_AQUI_O_VALOR_GERADO
   springdoc:
     api-docs:
       enabled: false
     swagger-ui:
       enabled: false
   ```

   > Repare que aqui vão os **nomes das propriedades** (`app.datasource.
   > username`), não os nomes das variáveis (`DB_USER`). Os dois caminhos
   > levam ao mesmo lugar — variável de ambiente **ou** arquivo, não
   > precisa dos dois.

5. **Iniciar** a aplicação.
6. **Abrir o log** — este passo não é opcional. Uma aplicação que "iniciou"
   no painel pode ter morrido no boot; só o log conta a verdade.

### Lendo o log

Start bem-sucedido, na ordem:

```
Successfully validated N migrations       (ou "Migrating schema ... to version 4")
Tomcat started on port XXXX
Started WectiApiApplication in X.X seconds
```

Erros que já apareceram neste projeto:

| No log | Causa | O que fazer |
|---|---|---|
| `PlaceholderResolutionException: ... app.datasource.username` | Falta `DB_USER`/`DB_PASSWORD` (ou o `application.yml` externo sumiu) | Conferir a seção 3. Se estiver tudo certo, é `.jar` antigo — refaça o `mvn clean package` |
| `Access denied for user 'X'@'%'` (sem citar banco) | Senha errada, ou o usuário não existe | Conferir `DB_USER` / `DB_PASSWORD` |
| `Access denied for user 'X'@'%' to database 'Y'` | O usuário existe, mas **não está associado** ao banco `Y` — ou `DB_NAME` está errado | Conferir se `DB_NAME` é o nome completo (com prefixo da conta) e refazer o *Add User To Database* com ALL PRIVILEGES |
| `Unknown database` | O banco não existe com esse nome | Criar em MySQL® Databases, ou corrigir `DB_NAME` |
| `Port already in use` | Instância anterior ainda rodando | Parar a antiga no plugin |
| `Communications link failure` | Host/porta do MySQL diferentes | Conferir `DB_HOST` / `DB_PORT` |

### Conferindo que a API respondeu

Com a aplicação no ar, abra no navegador:

```
http://jadir9152.c44.integrator.host/health
```

Deve responder algo simples de status. Se der 404, o servidor web não está
repassando as chamadas para a aplicação — nesse caso é configuração de
proxy do plugin, e vale abrir chamado com a Integrator.

No log de um start bem-sucedido você deve ver, em ordem:

```
Successfully validated N migrations   (ou "Migrating schema ... to version 4")
Tomcat started on port XXXX
Started WectiApiApplication in X.X seconds
```

Se aparecer `PlaceholderResolutionException` citando `app.datasource.url`,
`DB_USER` ou `JWT_SECRET`, é variável de ambiente faltando — volte à
seção 3. (Esse erro já apareceu neste projeto por causa de um `.jar`
antigo, gerado antes das variáveis existirem: se persistir, refaça o
`mvn clean package` e suba o arquivo novo.)

As migrations do banco (Flyway) rodam sozinhas no primeiro start, na
ordem correta. O schema **nunca** é alterado por geração automática.

---

## 6. Primeiro acesso

O sistema não cria um usuário administrador sozinho. Para o primeiro:

1. cPanel → **phpMyAdmin** → selecionar o banco → aba **SQL** → colar,
   trocando nome, e-mail e CPF pelos reais:

   ```sql
   INSERT INTO usuarios (id, nome, email, senha, perfil, cpf, criado_em)
   VALUES (UUID(), 'Nome do Administrador', 'admin@unicid.edu.br',
           'definir-pela-tela', 'ADMIN', '12345678901', NOW());
   ```

   O campo `senha` recebe esse texto de propósito: ele **não** é uma senha
   válida (não é um hash), então ninguém consegue entrar com ele. É
   substituído no passo seguinte.

2. Abrir `https://jadir9152.c44.integrator.host/recuperar-senha` e informar
   o e-mail e o CPF cadastrados. Isso define a senha real e já devolve o
   acesso.

O CPF é obrigatório para Admin justamente por isso: é o identificador que
permite recuperar o acesso.

Depois, ainda antes de cadastrar o primeiro evento, é preciso **criar o
período (semestre) vigente** — o cadastro de evento é recusado se a data
não cair dentro de um período existente. Não há tela para isso; hoje é
via API:

```bash
curl -X POST https://jadir9152.c44.integrator.host/periodos \
  -H "Authorization: Bearer SEU_TOKEN_DE_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"2026.2","data_inicio":"2026-08-01","data_fim":"2026-12-20"}'
```

---

## 7. Backup

**Antes de liberar para os alunos.** Perder o banco significa perder
inscrições, presenças e certificados emitidos — dados que não podem ser
reconstruídos.

1. Agendar um dump diário do MySQL no painel (procure por *Backup* ou
   *Cron Jobs*):
   ```bash
   mysqldump -u USUARIO -p'SENHA' NOME_DO_BANCO > backup-$(date +\%F).sql
   ```
2. Definir por quantos dias guardar.
3. **Testar uma restauração** em um banco vazio. Backup que nunca foi
   restaurado não conta como backup.

---

## 8. Conferir depois de subir

- [ ] `https://jadir9152.c44.integrator.host` abre com cadeado fechado
- [ ] `http://jadir9152.c44.integrator.host` redireciona sozinho para HTTPS
- [ ] Login funciona pelo site (se falhar com "verifique seus dados" mas
      funcionar via `curl`, o problema é `CORS_ALLOWED_ORIGINS`)
- [ ] Um aluno consegue se cadastrar e se inscrever
- [ ] O QR de check-in gerado aponta para `https://jadir9152.c44.integrator.host/...`
- [ ] Um celular consegue ler o QR e confirmar presença
- [ ] O certificado sai em PDF e o código valida em `/validar`
- [ ] `/docs` **não** abre (se `SPRINGDOC_ENABLED=false`)
- [ ] 11 tentativas de login com senha errada devolvem `429`

---

## Fazer um piloto primeiro

Antes de liberar para todos os alunos, rodar **um evento real com turma
reduzida**, do cadastro até a emissão do certificado. É a forma mais
barata de descobrir um problema de configuração — com 20 pessoas em vez
de 300.
