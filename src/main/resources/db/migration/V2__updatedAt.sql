alter table offer_search
  alter column last_updated type timestamp using last_updated::timestamp;

alter table offer_search
  rename column last_updated to updated_at;

alter table offer add column created_at timestamp default now();

alter table offer add column updated_at timestamp default now();

alter table search_request add column created_at timestamp default now();

alter table search_request add column updated_at timestamp default now();
