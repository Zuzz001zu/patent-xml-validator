-- The last runs, to follow data quality over time.
SELECT r.id,
       r.started_at,
       COUNT(d.id)                                           AS documents,
       SUM(CASE WHEN d.status = 'INVALID' THEN 1 ELSE 0 END) AS invalid,
       (SELECT COUNT(*)
        FROM finding f
        JOIN checked_document fd ON fd.id = f.document_id
        WHERE fd.run_id = r.id AND f.severity = 'ERROR')    AS errors
FROM validation_run r
LEFT JOIN checked_document d ON d.run_id = r.id
GROUP BY r.id, r.started_at
ORDER BY r.id DESC
FETCH FIRST 10 ROWS ONLY
