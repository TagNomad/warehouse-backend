-- 测试用户（密码 Admin@123 的 BCrypt 哈希；MERGE 保证重复启动不报错）
MERGE INTO tb_user (username, password, display_name, role, email, phone)
KEY (username)
VALUES ('admin',
        '$2b$10$0ImMTcs5Nivm9eW63gcmdOfm9vn7nihzTrdSZZlYwn8XQcJQYQxcS',
        '系统管理员',
        'ADMIN',
        'admin@example.com',
        '13800000001');
