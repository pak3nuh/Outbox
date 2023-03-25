
create table if not exists stored_entries(
    id int primary key auto_increment,
    created timestamp not null,
    `key` blob not null,
    `value` blob not null,
    user_id varchar(1000) not null,
    metadata text not null,
    submitted timestamp,
    error text
);

create table if not exists application_locks(
    lock_id varchar(256) not null primary key,
    locked_at timestamp
);

alter table stored_entries add index id_stored_entry_created (created asc);
