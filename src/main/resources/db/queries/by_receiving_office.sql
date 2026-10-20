-- Data quality per receiving office: useful to see where corrections come from.
SELECT COALESCE(d.receiving_office, '--')                   AS receiving_office,
       COUNT(*)                                            AS documents,
       SUM(CASE WHEN d.status = 'INVALID' THEN 1 ELSE 0 END) AS invalid,
       SUM(CASE WHEN d.status = 'WARNING' THEN 1 ELSE 0 END) AS with_warnings,
       ROUND(100.0 * SUM(CASE WHEN d.status = 'VALID' THEN 1 ELSE 0 END) / COUNT(*), 1) AS valid_pct
FROM checked_document d
WHERE d.run_id = ?
GROUP BY COALESCE(d.receiving_office, '--')
ORDER BY documents DESC, receiving_office
