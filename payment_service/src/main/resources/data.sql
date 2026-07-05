-- MERGE INTO "app_users" ("username", "password", "role")
-- KEY ("username")
-- VALUES
--     ('admin', 'admin123', 'ADMIN'),
--     ('user', 'user123', 'USER'),
--     ('supplier', 'supplier123', 'SUPPLIER');
INSERT INTO "app_users" ("username", "password", "role")
VALUES
    ('admin', 'admin123', 'ADMIN'),
    ('user', 'user123', 'USER'),
    ('supplier', 'supplier123', 'SUPPLIER')
    ON CONFLICT ("username")
DO NOTHING;
