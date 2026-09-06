create table if not exists armazenamento(
    id serial primary key,
    id_arquivo int not null,

    provedor varchar(100) not null,
    nome_bucket varchar(100) not null,

    chave_armazenamento varchar(255) not null,

    dataCriacao timestamp not null, 

    foreign key(id_arquivo) references arquivo(id)

);