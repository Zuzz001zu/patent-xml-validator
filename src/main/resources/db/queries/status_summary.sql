SELECT status, COUNT(*) AS documents
FROM checked_document
WHERE run_id = ?
GROUP BY status
ORDER BY CASE status WHEN 'INVALID' THEN 1 WHEN 'WARNING' THEN 2 ELSE 3 END
