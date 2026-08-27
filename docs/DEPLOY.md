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

No painel da Integrator Host (cPanel):

1. Emitir o certificado TLS para o domínio (procure por *SSL/TLS Status*,
   *Let's Encrypt* ou *AutoSSL*). Costuma ser um clique e sai de graça.
2. Ligar o redirecionamento automático de HTTP para HTTPS (normalmente
   *Force HTTPS Redirect*).
3. Conferir: abrir `http://jadir9152.c44.integrator.host` deve virar `https://jadir9152.c44.integrator.host`
   sozinho, com o cadeado fechado.

Do lado da aplicação **já está tudo pronto**: a API está configurada com
`server.forward-headers-strategy=framework`, que faz ela entender que a
requisição original veio por HTTPS mesmo recebendo HTTP do servidor web.
Isso é necessário para duas coisas funcionarem corretamente atrás do
proxy: o IP real do visitante (usado no limite de tentativas de login) e
qualquer URL que a aplicação gere.

---

## 2. Variáveis de ambiente da API

Configurar no painel da hospedagem (ou no `application.yml` externo, em
`appservers/standalone`, se o painel não tiver campo para variáveis).

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

## 3. Frontend

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

Publicar o conteúdo de `frontend/dist/` na raiz do site.

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

## 4. Backend

```bash
cd backend
mvn clean package -DskipTests
```

O arquivo gerado é `backend/target/wecti-api-0.1.0-SNAPSHOT.jar`. Subir
esse `.jar` pelo painel da hospedagem.

As migrations do banco (Flyway) rodam sozinhas no primeiro start, na
ordem correta. O schema **nunca** é alterado por geração automática.

---

## 5. Primeiro acesso

O sistema não cria um usuário administrador sozinho. Para o primeiro:

1. Inserir a linha direto no MySQL, com `perfil = 'ADMIN'` e um **CPF**
   preenchido (11 dígitos). O campo `senha` pode receber qualquer texto —
   ele será substituído no passo seguinte.
2. Abrir `https://jadir9152.c44.integrator.host/recuperar-senha` e informar o e-mail e o CPF
   cadastrados. Isso define a senha real e já devolve o acesso.

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

## 6. Backup

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

## 7. Conferir depois de subir

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
