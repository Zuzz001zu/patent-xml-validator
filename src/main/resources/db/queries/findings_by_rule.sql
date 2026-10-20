-- Which rules fail most often in this run, and in how many documents.
SELECT f.rule_id,
       f.severity,
       COUNT(*)                      AS occurrences,
       COUNT(DISTINCT f.document_id) AS documents
FROM finding f
JOIN checked_document d ON d.id = f.document_id
WHERE d.run_id = ?
GROUP BY f.rule_id, f.severity
ORDER BY occurrences DESC, f.rule_id
