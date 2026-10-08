-- 007: papel USER/ADMIN + tokens de reposicao OTP
alter table if exists users add column if not exists role varchar(20) not null default 'USER';

create table if not exists passwordresettoken(
    id serial primary key,
    email varchar(100) not null,
    otp_hash varchar(255) not null,
    expires_at timestamp not null,
    attempts int not null default 0,
    used boolean not null default false,
    reset_token_hash varchar(255),
    reset_token_expires_at timestamp
);
