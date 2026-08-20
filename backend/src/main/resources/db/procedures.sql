-- Postgres stored function equivalent of a SQL Server stored procedure,
-- returning a settlement summary for a merchant (or all merchants when
-- p_merchant_id is null). Demonstrates writing/calling a stored routine
-- directly, as an alternative to the JPQL/native-query approach used
-- elsewhere in this codebase (see SettlementRepository.findSummary).
--
-- CREATE OR REPLACE makes this safe to re-run on every app startup.
--
-- Statements in this file are split on "//" (spring.sql.init.separator),
-- not ";" — the function body below contains its own internal ";", which
-- Spring's default ";"-based script splitter would otherwise cut through,
-- truncating the closing $$ and producing "Unterminated dollar quote".
create or replace function settlement_summary(p_merchant_id bigint default null)
returns table (
    merchant_id bigint,
    merchant_name varchar,
    period varchar,
    total_amount numeric(19,4),
    status varchar
)
language sql
as $$
    select m.id, m.name, s.period, s.total_amount, s.status
    from settlements s
    join merchants m on m.id = s.merchant_id
    where p_merchant_id is null or m.id = p_merchant_id
    order by s.total_amount desc;
$$
//
