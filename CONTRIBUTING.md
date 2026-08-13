# Como contribuir - WECTI

## Fluxo de Git

1. `main` é protegida - nunca commitar direto nela.
2. Toda tarefa nasce como Issue, ligada a uma branch:
   `feature/nome-curto`, `fix/nome-curto`, `chore/nome-curto`.
3. Commits no padrão Conventional Commits: `feat:`, `fix:`, `refactor:`,
   `test:`, `chore:`, `docs:`.
4. Pull Request pequeno (idealmente menos de 400 linhas), com pelo menos
   1 revisor de outro módulo aprovando antes do merge.
5. O CI (`.github/workflows/ci.yml`) precisa passar antes de mergear.

## Configurando a proteção da branch `main`

Isso é feito uma vez, por quem tem acesso de admin no repositório - não é
algo que dá pra automatizar via código, é uma configuração do GitHub.

No GitHub: Settings > Branches > Add branch protection rule

1. Branch name pattern: `main`
2. Marcar "Require a pull request before merging"
   - "Require approvals": 1
3. Marcar "Require status checks to pass before merging"
   - selecionar o job `build` do workflow de CI (aparece na lista depois
     que o workflow rodar pelo menos uma vez)
4. Marcar "Do not allow bypassing the above settings" (inclui admins)
5. Desmarcar "Allow force pushes" e "Allow deletions"

## Ambiente local

Ver `backend/README.md` para detalhes. Resumo rápido:

```bash
cd backend
docker compose up -d db
mvn spring-boot:run
```

API sobe em `http://localhost:8080`, Swagger UI em
`http://localhost:8080/docs`.

## Contrato de API

Antes de implementar ou mudar um endpoint, atualize `docs/openapi.yaml`.
Esse arquivo é a referência combinada com o time - evita que cada um
implemente uma versão diferente do mesmo endpoint.
