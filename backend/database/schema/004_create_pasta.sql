create table if not exists pasta(
    id serial primary key,
    nome varchar(100) not null,
    dataCriacao date not null,
    dataCarregamento date,
    dateAcesso date not null,
    
    
    id_permissao int not null,
    id_usuario int not null,
    id_pasta_pai int 


    foreign key(id_usuario) references usuario(id),
    foreign key(id_permissao) references permissao(id)
    foreign key(id_pasta_pai) references pasta(id)
);