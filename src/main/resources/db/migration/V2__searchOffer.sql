alter table offer_search
  alter column last_updated type timestamptz using last_updated::timestamptz;

alter table offer_search
  RENAME COLUMN last_updated to updated_at;
