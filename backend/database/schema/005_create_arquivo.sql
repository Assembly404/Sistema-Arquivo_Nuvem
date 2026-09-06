create table if not exists arquivo(
    id serial primary key,
    nome varchar(100) not null,
    tamanho int not null,
    extensao varchar(100) not null,
    dataCarregamento timestamp not null,
    dataTransferencia timestamp not null,

    id_permissao int not null
    id_usuario int not null,
    id_pasta int ,

    foreign key(id_usuario) references usuario(id),
    foreign key(id_permissao) references permissao(id),
    foreign key(id_pasta) references pasta(id)
);