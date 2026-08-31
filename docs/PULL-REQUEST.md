# Levar a `feature/backend` para a `main`

Repositório: <https://github.com/Allan-Maia/WECTI>

> **Atenção ao nome da branch principal.** Ela se chama **`main`**, não
> `master`. Não existe branch `master` neste repositório.

---

## O problema que precisa ser resolvido antes do PR

As duas branches foram criadas de forma independente e **não têm nenhum
commit em comum**:

| Branch | Raiz do histórico | Commits | Conteúdo |
|---|---|---|---|
| `main` | `647eaef` | 2 | Só a landing page e as fotos |
| `feature/backend` | `7d155f6` | 9 | O sistema inteiro (API, site, testes) |

`git merge-base main feature/backend` não devolve nada — são duas árvores
separadas que por acaso vivem no mesmo repositório.

**Consequência prática:** abrir o PR direto e clicar em *Merge* no GitHub
**não funciona**. O git recusa:

```
fatal: refusing to merge unrelated histories
```

A correção é unir os históricos **antes** de abrir o PR, do lado de quem
conhece as mudanças. Depois disso o PR vira uma revisão normal, e quem
faz o merge não precisa resolver nada.

---

# Parte 1 — Samuel (antes de avisar o professor)

Rodar na pasta do projeto, na branch `feature/backend`.

### 1. Confirmar onde você está

```bash
git branch --show-current
```

Tem que responder `feature/backend`. Se não, `git checkout feature/backend`.

### 2. Commitar seu trabalho

```bash
git add -A
git status
```

Confira a lista antes de commitar. **Não deve aparecer** nada dentro de
`fotos/` (são 228 MB de originais, já versionados na `main` — o
`.gitignore` cuida disso). Deve aparecer `frontend/public/fotos/`, que é
a versão otimizada de 16 MB que o site publica.

```bash
git commit -m "feat: landing como pagina inicial, ranking e controle de vagas"
```

### 3. Trazer a `main` para dentro da sua branch

É este passo que resolve o problema dos históricos separados.

```bash
git fetch origin
git merge origin/main --allow-unrelated-histories
```

Testei este merge: ele passa **sem nenhum conflito**. O que ele traz da
`main` é `favicon.svg` e os 127 arquivos de `fotos/`.

### 4. Remover as cópias duplicadas da landing

O merge ressuscita na raiz o `index.html`, o `style.css` e o
`favicon.svg` da `main`. Essas são as versões **antigas**: as que valem
agora moraram para `frontend/public/`, que é de onde o `npm run build`
monta o site. Deixar as duas versões no repositório é pedir para alguém
editar a errada daqui a um mês.

```bash
git rm index.html style.css favicon.svg
git commit -m "chore: remove copias antigas da landing na raiz (agora em frontend/public)"
```

> Se algum desses arquivos não existir, o `git rm` reclama. Tudo bem —
> rode sem o que faltar.

### 5. Conferir que o site continua fechando

```bash
cd frontend && npm run build && cd ..
```

Tem que terminar sem erro, e `frontend/dist/` precisa conter
`home.html`, `index.html`, `assets/`, `palestrantes/`, `fotos/` e
`.htaccess`.

### 6. Enviar

```bash
git push -u origin feature/backend
```

O `-u` existe porque a branch local ainda não tinha upstream configurado.

### 7. Avisar o professor

Mandar para ele a **Parte 2** deste arquivo, com este link já pronto:

<https://github.com/Allan-Maia/WECTI/compare/main...feature/backend>

---

# Parte 2 — Dono do repositório (revisão e merge)

## Abrir o Pull Request

1. Acessar:
   <https://github.com/Allan-Maia/WECTI/compare/main...feature/backend>

   A tela já vem com `base: main` ← `compare: feature/backend`. Confira
   que está nessa ordem — invertido, o PR levaria a `main` para dentro da
   branch de trabalho, que é o contrário do que se quer.

2. Clicar em **Create pull request**.

3. Sugestão de título:

   > Sistema WECTI completo: API, área do aluno e do admin, e landing como página inicial

4. Se aparecer **"Can't automatically merge"** ou *"entirely different
   commit histories"*: a Parte 1 não foi concluída. Peça ao Samuel para
   rodar os passos 3 e 4 e dar push de novo — não tente resolver pelo
   GitHub.

## O que está entrando

| Área | O que é |
|---|---|
| `backend/` | API REST em Spring Boot 4.1 + MySQL, com 76 testes automatizados |
| `frontend/src/` | Site em React (aluno e admin): inscrições, check-in por QR, pontuação, ranking, certificados |
| `frontend/public/` | Landing pública do evento, agora a página inicial do site |
| `docs/` | Contrato da API (`openapi.yaml`) e guia de deploy no cPanel (`DEPLOY.md`) |

## Merge

Usar **"Create a merge commit"** (opção padrão). Ela preserva os 9
commits da branch, que contam a evolução do projeto.

> Evite *Squash and merge* aqui: os commits descrevem decisões de
> arquitetura que valem como registro do trabalho.

## Depois do merge

O merge **não publica nada** — só atualiza o código no GitHub. Colocar no
ar é um processo à parte, descrito passo a passo em
[`docs/DEPLOY.md`](DEPLOY.md).

Dois pontos desse guia que valem destaque:

- **O banco vai da versão 4 para a 6.** As migrations rodam sozinhas no
  primeiro start da API, mas **faça backup antes** — a V2 apaga uma coluna
  do modelo antigo. Seção 2 do guia.
- **O certificado HTTPS ainda não saiu.** Nove domínios da conta estão
  numa lista de exclusão do AutoSSL, e só o suporte da Integrator Host
  pode retirá-los. O guia tem um roteiro de duas fases para subir em HTTP
  e migrar depois sem retrabalho. Seção 1.
