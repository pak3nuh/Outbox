
create sequence stored_entries_id_seq start with 0;

create table stored_entries(
    id int NOT NULL primary key,
    created timestamp not null,
    key bytea not null,
    value bytea not null ,
    user_id varchar(1000),
    submitted timestamp,
    error varchar(max)
);

create index id_stored_entry_created ON stored_entries(created asc);
