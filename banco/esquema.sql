CREATE TABLE IF NOT EXISTS doadores (
    id INTEGER PRIMARY KEY,
    nome_completo TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    telefone TEXT NOT NULL,
    endereco TEXT NOT NULL,
    hash_senha TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS estoque_doador (
    doador_id INTEGER NOT NULL REFERENCES doadores(id) ON DELETE CASCADE,
    material TEXT NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade BETWEEN 1 AND 2147483647),
    PRIMARY KEY (doador_id, material)
);
