-- Seed transactions for customer C101, October 2026
-- Run this against securestatement_db to populate the transaction table.
-- Works with both Docker Compose (docker exec) and a local PostgreSQL instance.

INSERT INTO transaction (transaction_id, customer_id, amount, currency, location, timestamp) VALUES
  ('TXN-C101-001', 'C101', 15000.00, 'ZAR', 'Cape Town Branch',      '2026-10-02 08:45:00'),
  ('TXN-C101-002', 'C101',  -850.50, 'ZAR', 'Sandton ATM',           '2026-10-05 13:10:00'),
  ('TXN-C101-003', 'C101',  3200.75, 'ZAR', 'Durban Online',         '2026-10-10 09:00:00'),
  ('TXN-C101-004', 'C101', -1250.00, 'ZAR', 'Rosebank Mall',         '2026-10-14 17:30:00'),
  ('TXN-C101-005', 'C101',   500.00, 'ZAR', 'Pretoria Branch',       '2026-10-18 11:20:00'),
  ('TXN-C101-006', 'C101', -2100.00, 'ZAR', 'Midrand POS',           '2026-10-22 15:00:00'),
  ('TXN-C101-007', 'C101',  8000.00, 'ZAR', 'Cape Town Branch',      '2026-10-27 10:05:00'),
  ('TXN-C101-008', 'C101',  -430.25, 'ZAR', 'Johannesburg Online',   '2026-10-31 19:45:00');
