create table if not exists usuario(
    id serial primary key,
    nome varchar(100) not null,
    apelido varchar(100) not null,
    email varchar(100) not null,
    password varchar(100) not null,
    dataRegistro date not null,
    ultimoAcesso timestamp not null,
    id_perfil int not null,
    foreign key(id_perfil) references perfil(id)
);