-- Включение расширений для трейсинга и статистики
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Представление для просмотра статистики запросов (трейсинг)
CREATE OR REPLACE VIEW query_stats AS
SELECT 
    queryid,
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    min_exec_time,
    max_exec_time,
    rows,
    shared_blks_hit,
    shared_blks_read
FROM pg_stat_statements
ORDER BY total_exec_time DESC;
