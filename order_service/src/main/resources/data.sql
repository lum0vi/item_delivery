-- INSERT INTO "app_users" ("username", "password", "role")
-- VALUES
--     ('admin', 'admin123', 'ADMIN'),
--     ('user', 'user123', 'USER'),
--     ('supplier', 'supplier123', 'SUPPLIER')
--     ON CONFLICT ("username")
-- DO UPDATE SET
--     "password" = EXCLUDED."password",
--            "role" = EXCLUDED."role";
INSERT INTO "app_users" ("username", "password", "role")
VALUES
    ('admin', 'admin123', 'ADMIN'),
    ('user', 'user123', 'USER'),
    ('supplier', 'supplier123', 'SUPPLIER')
    ON CONFLICT ("username") DO NOTHING;
