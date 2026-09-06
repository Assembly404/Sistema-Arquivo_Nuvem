create table if not exists perfil(
    id int primary key,
    nome varchar(100) not null,
    permissao varchar(100) not null,
    descricao varchar(100) not null
);