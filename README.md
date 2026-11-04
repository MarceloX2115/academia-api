# 🏋️ API de Gerenciamento de Academia

API REST em **Spring Boot** para gerenciar **alunos**, **planos** e **matrículas** de uma academia, com controle de vencimento das matrículas e regras de negócio aplicadas na camada de Service.

## Descrição e objetivo

O sistema permite cadastrar alunos e planos (mensal, trimestral, anual...) e matricular alunos nos planos. A cada matrícula, a API calcula automaticamente a **data de vencimento** (início + duração do plano) e o **valor total** (valor mensal × duração), e controla a situação da matrícula (`ATIVA`, `VENCIDA`, `CANCELADA`).

## Tecnologias

- Java 17 + Spring Boot 3.4
- Spring Web, Spring Data JPA (Hibernate), Bean Validation
- H2 (padrão, em arquivo) e PostgreSQL (perfil `postgres`)
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + MockMvc (testes)
- Docker / Docker Compose
- Frontend simples em HTML/JS puro (ponto extra)

## Arquitetura (camadas)

```
controller  -> recebe HTTP, valida entrada, devolve ResponseEntity com o status correto
service     -> regras de negócio e transações
repository  -> acesso a dados (Spring Data JPA, Specifications para filtros)
model       -> entidades JPA (Aluno, Plano, Matricula) e enum StatusMatricula
dto         -> objetos de entrada/saída (a entidade nunca é exposta pela API)
mapper      -> conversão manual entidade <-> DTO
exception   -> exceções de negócio + @ControllerAdvice (tratamento centralizado)
config      -> OpenAPI e carga de dados de exemplo
```

### Modelo de dados

```
Aluno 1 ────── N Matricula N ────── 1 Plano
```

- `Aluno`: nome, e-mail (único), CPF (único), telefone, data de nascimento
- `Plano`: nome (único), descrição, valor mensal, duração em meses, ativo
- `Matricula`: aluno, plano, data de início, data de vencimento, valor total, status

Relacionamentos: `@OneToMany` em `Aluno` e `Plano`, `@ManyToOne` em `Matricula`.

## Regras de negócio

| # | Regra | Resultado |
|---|-------|-----------|
| 1 | Impedir matrícula duplicada (mesmo aluno + mesmo plano com matrícula ativa) | 409 |
| 2 | Impedir matrícula em plano inativo | 409 |
| 3 | Controlar vencimento: vencimento = início + duração do plano; matrículas passadas do prazo viram `VENCIDA` automaticamente (rotina diária às 00:05 e a cada consulta); renovação e consulta de "vencendo em N dias" | — |
| 4 | Valor total calculado automaticamente (valor mensal × meses) | — |
| 5 | Data de início não pode estar no passado nem passar de 30 dias à frente | 409 |
| 6 | Não excluir aluno ou plano que possuam matrículas | 409 |
| 7 | Matrícula ativa não pode ser excluída (cancelar antes); cancelada não pode ser alterada/renovada | 409 |
| 8 | E-mail/CPF de aluno e nome de plano não podem se repetir | 409 |

## Como executar

### Pré-requisitos
- JDK 17+ e Maven 3.9+ (ou Docker)

### Opção 1 — Local (H2, zero configuração)

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Na primeira execução são carregados dados de exemplo (4 planos, 3 alunos, 1 matrícula). Para desativar: `app.seed=false`.

### Opção 2 — Docker Compose (PostgreSQL)

```bash
docker compose up --build
```

### Testes

```bash
mvn test
```

## Configuração do banco de dados

- **Padrão (H2 em arquivo)**: configurado em `src/main/resources/application.properties` (`jdbc:h2:file:./data/academia`). Console em `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/academia`, usuário `sa`, senha vazia).
- **PostgreSQL**: `application-postgres.properties`. Ative com `--spring.profiles.active=postgres` e informe, se necessário, as variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. As tabelas são criadas automaticamente (`ddl-auto=update`).

## Documentação Swagger

- Swagger UI: **http://localhost:8080/swagger-ui.html**
- OpenAPI (JSON): http://localhost:8080/v3/api-docs

## Frontend (ponto extra)

Painel simples em **http://localhost:8080/** que consome a API: cadastro, edição, exclusão e filtros de alunos, planos e matrículas, além de renovar/cancelar matrículas e listar as que vencem em 7 dias. Mensagens de erro da API (400/404/409) são exibidas na tela.

## Endpoints

### Alunos — `/api/alunos`
| Método | Rota | Descrição | Status |
|--------|------|-----------|--------|
| POST | `/api/alunos` | Cadastrar | 201 / 400 / 409 |
| GET | `/api/alunos/{id}` | Buscar por id | 200 / 404 |
| GET | `/api/alunos?nome=ana` | Listar (filtro por nome, paginado) | 200 |
| PUT | `/api/alunos/{id}` | Atualizar | 200 / 400 / 404 / 409 |
| DELETE | `/api/alunos/{id}` | Excluir | 204 / 404 / 409 |

### Planos — `/api/planos`
| Método | Rota | Descrição | Status |
|--------|------|-----------|--------|
| POST | `/api/planos` | Cadastrar | 201 / 400 / 409 |
| GET | `/api/planos/{id}` | Buscar por id | 200 / 404 |
| GET | `/api/planos?ativo=true` | Listar (filtro por situação, paginado) | 200 |
| PUT | `/api/planos/{id}` | Atualizar (inclusive ativar/inativar) | 200 / 400 / 404 / 409 |
| DELETE | `/api/planos/{id}` | Excluir | 204 / 404 / 409 |

### Matrículas — `/api/matriculas`
| Método | Rota | Descrição | Status |
|--------|------|-----------|--------|
| POST | `/api/matriculas` | Matricular aluno em plano | 201 / 400 / 404 / 409 |
| GET | `/api/matriculas/{id}` | Buscar por id | 200 / 404 |
| GET | `/api/matriculas?nomeAluno=ana&status=ATIVA` | Listar (filtros nome do aluno e status, paginado) | 200 / 400 |
| GET | `/api/matriculas/vencendo?dias=7` | Ativas que vencem nos próximos N dias | 200 / 400 |
| PUT | `/api/matriculas/{id}` | Trocar plano / data de início | 200 / 400 / 404 / 409 |
| PATCH | `/api/matriculas/{id}/renovar` | Renovar | 200 / 404 / 409 |
| PATCH | `/api/matriculas/{id}/cancelar` | Cancelar | 200 / 404 / 409 |
| DELETE | `/api/matriculas/{id}` | Excluir (somente não ativas) | 204 / 404 / 409 |

As listagens aceitam paginação e ordenação: `?page=0&size=10&sort=nome,asc`.

## Exemplos de uso (curl)

**Criar plano**
```bash
curl -X POST http://localhost:8080/api/planos -H "Content-Type: application/json" \
  -d '{"nome":"Semestral","descricao":"6 meses","valorMensal":74.90,"duracaoMeses":6,"ativo":true}'
```

**Criar aluno**
```bash
curl -X POST http://localhost:8080/api/alunos -H "Content-Type: application/json" \
  -d '{"nome":"Maria da Silva","email":"maria@email.com","cpf":"12345678901","telefone":"(86) 99999-0000","dataNascimento":"1998-05-20"}'
```

**Matricular** (retorna 201 com vencimento e valor total calculados)
```bash
curl -X POST http://localhost:8080/api/matriculas -H "Content-Type: application/json" \
  -d '{"alunoId":1,"planoId":2}'
```

**Resposta**
```json
{
  "id": 2, "alunoId": 1, "alunoNome": "Ana Souza", "planoId": 2, "planoNome": "Trimestral",
  "dataInicio": "2026-11-03", "dataVencimento": "2027-02-03",
  "valorTotal": 239.70, "status": "ATIVA", "diasParaVencimento": 92
}
```

**Erro de regra de negócio (matrícula duplicada) — 409**
```json
{
  "timestamp": "2026-11-03T16:51:00",
  "status": 409,
  "error": "Conflict",
  "message": "O aluno 'Ana Souza' já possui uma matrícula ativa no plano 'Mensal'.",
  "path": "/api/matriculas",
  "details": []
}
```

**Erro de validação — 400**
```json
{
  "status": 400,
  "message": "Dados inválidos.",
  "details": ["nome: O nome é obrigatório", "cpf: O CPF deve conter exatamente 11 dígitos numéricos"]
}
```

**Filtros**
```bash
curl "http://localhost:8080/api/alunos?nome=ana"
curl "http://localhost:8080/api/matriculas?nomeAluno=ana&status=ATIVA"
curl "http://localhost:8080/api/matriculas/vencendo?dias=15"
```

**Renovar / cancelar / excluir**
```bash
curl -X PATCH  http://localhost:8080/api/matriculas/1/renovar
curl -X PATCH  http://localhost:8080/api/matriculas/1/cancelar
curl -X DELETE http://localhost:8080/api/matriculas/1
```

## Diferenciais implementados

Paginação e ordenação, testes automatizados (unitários e de integração), logs (SLF4J), Docker/Docker Compose e frontend integrado.
