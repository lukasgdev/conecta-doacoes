# ConectaDoações

Projeto de Programação Orientada a Objetos do Bacharelado em Sistemas de Informação do IFAL. O ConectaDoações aproxima pessoas que precisam de materiais escolares de quem pode doá-los.

## Integrantes

- Lukas Gomes da Silva
- Allysson Matheus Paulo Ferreira
- Abel Lucas Cavalcante de Melo

## Como executar no Windows

Instale o JDK 17 ou superior. Deixe o executável [sqlite3](https://www.sqlite.org/download.html) disponível no PATH. O aplicativo compila com o JDK, sem dependências Java externas.

Na raiz do projeto, execute:

```powershell
.\iniciar.cmd
```

Abra [localhost:8080/entrar](http://localhost:8080/entrar), crie uma conta de doador e entre com e-mail e senha. A [demonstração de solicitação de materiais](http://localhost:8080/) continua disponível na página inicial. Use **Ctrl+C** para encerrar o servidor.

Na **Área do doador**, escolha um material, informe a quantidade e clique em **Adicionar ao estoque**. Ao adicionar o mesmo material de novo, a quantidade é somada ao saldo do seu estoque.

## Banco local

O arquivo inicial `banco/banco-inicial.sqlite` contém as tabelas de doadores e de estoque vazias. Na primeira execução, o servidor o copia para `dados-locais/conecta-doacoes.sqlite`. As contas e os estoques ficam nessa cópia, ignorada pelo Git, e permanecem após reiniciar o servidor. Ao abrir uma cópia criada antes do estoque, o servidor adiciona a tabela nova e preserva as contas. O esquema está em `banco/esquema.sql`.

## Camadas e padrões

- **View.** Apresenta os formulários em HTML, recebe os dados e controla a sessão local.
- **VO.** Transporta os campos de cadastro, acesso e estoque.
- **BO.** Valida os dados, protege a senha, confere as credenciais e valida os materiais e as quantidades. A classe abstrata `Usuario` contém os dados comuns, e `Doador` acrescenta endereço e hash da senha.
- **DAO.** Insere e consulta doadores e grava os saldos de materiais de cada conta. A restrição de unicidade do SQLite impede e-mails duplicados.

**Strategy.** O BO usa a interface `ProtecaoSenha`. A implementação usa PBKDF2 com HMAC-SHA256 e um sal aleatório por conta, com as APIs do JDK.

**Adapter.** `AdaptadorSqlite` executa as instruções por meio do programa `sqlite3`. O DAO mapeia os dados e delega ao adaptador a comunicação com o processo.

## Testes

Execute `.iniciar.cmd testar`. Os testes verificam o BO com um DAO em memória, o DAO real com uma cópia temporária do SQLite, as regras de solicitação e o estoque individual.
