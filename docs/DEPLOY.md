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

---

## 2. Banco de dados no cPanel

Antes de subir a API, o banco precisa existir.

1. cPanel → **MySQL® Databases**.
2. Em *Create New Database*, criar o banco. O cPanel prefixa o nome com a
   conta — anote o nome **completo** que aparecer depois de criado
   (algo como `jadir9152_wecti`).
3. Em *MySQL Users → Add New User*, criar o usuário e uma senha forte.
   Anote também o nome completo do usuário (também vem prefixado).
4. Em *Add User To Database*, associar o usuário ao banco e marcar
   **ALL PRIVILEGES**.

Guarde os três valores — são o `DB_USER`, o `DB_PASSWORD` e o nome do
banco. As tabelas **não** precisam ser criadas à mão: o Flyway cria tudo
no primeiro start da API.

> Se o nome do banco for diferente de `jadir9152_wecti`, a URL de conexão
> em `application.yml` precisa ser ajustada — ou sobrescrita junto das
> outras variáveis.

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

**Publicando no cPanel:**

1. Compacte o conteúdo de `dist/` em um `.zip` — o **conteúdo**, não a
   pasta: ao abrir o zip você deve ver `index.html` e `assets/` na raiz,
   não uma pasta `dist` dentro.
2. cPanel → **File Manager** → entrar em `public_html`.
3. Se já houver um site antigo ali, apagar antes (ou mover para uma pasta
   `backup-antigo/`).
4. **Upload** do zip → botão direito sobre ele → **Extract**.
5. Apagar o zip depois de extrair.
6. No File Manager, clicar em **Settings** (canto superior direito) e
   marcar **Show Hidden Files (dotfiles)** — é a única forma de ver se o
   `.htaccess` subiu. Sem ele, os links de QR code dão 404.

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
mvn clean package -DskipTests
```

O arquivo gerado é `backend/target/wecti-api-0.1.0-SNAPSHOT.jar` (cerca de
60 MB — contém tudo, inclusive as fontes do certificado).

**Publicando no cPanel:** a Integrator Host usa um plugin próprio, o
**Integrator Spring Boot**. Procure por ele no painel (costuma ficar em
*Software*). Nele você:

1. Faz o upload do `.jar`.
2. Define a porta (o plugin costuma atribuir uma; ela vira a
   `SERVER_PORT`).
3. Define as variáveis de ambiente da seção 3 — **se o plugin não tiver
   campo para isso**, use o arquivo `application.yml` externo, na pasta
   `appservers/standalone`, com o mesmo conteúdo em formato YAML.
4. Inicia a aplicação e **abre o log** para conferir se subiu.

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
