
create table if not exists stored_entries(
    id int generated always as identity,
    created timestamp not null,
    "key" bytea not null,
    "value" bytea not null ,
    user_id varchar(1000) not null,
    metadata varchar(8000) not null,
    submitted timestamp,
    error varchar(8000)
);

create table if not exists application_locks(
    lock_id varchar(1000) not null primary key,
    locked_at timestamp
);

create index if not exists id_stored_entry_created ON stored_entries(created asc);
