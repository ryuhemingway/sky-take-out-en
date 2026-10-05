-- Migrates an existing sky_take_out database to the English, web-UI version of the app.
-- Fresh installs do not need this: sql/schema.sql already creates the English version.
--
--   mysql -u root -p sky_take_out < sql/migrate-to-english.sql
--
-- Requires MySQL 8. Run it once: the RENAME COLUMN fails if it has already run.
-- The UPDATEs match on the old Chinese values, so repeating them is harmless.
-- This file contains Chinese on purpose: those are the old values being replaced.

SET NAMES utf8mb4;

-- Login is by username in the web UI (openid was the WeChat user ID).
ALTER TABLE `user` RENAME COLUMN openid TO username;

UPDATE category SET name = 'Staples'   WHERE name = '主食';
UPDATE category SET name = 'Snacks'    WHERE name = '小吃';
UPDATE category SET name = 'Set Meals' WHERE name = '套餐';

UPDATE rider SET name = 'Rider Wang' WHERE name = '骑手小王';
UPDATE rider SET name = 'Rider Li'   WHERE name = '骑手小李';

UPDATE coupon SET name = 'New customer ¥5 off'     WHERE name = '新客立减5元';
UPDATE coupon SET name = '¥10 off orders over ¥60' WHERE name = '满60减10元';

UPDATE `user` SET name = 'Web User' WHERE name = '微信用户';

-- sex is VARCHAR(2), so it stores M/F rather than Male/Female.
UPDATE address_book SET sex = 'M' WHERE sex = '男';
UPDATE address_book SET sex = 'F' WHERE sex = '女';
UPDATE address_book SET label = 'Home'   WHERE label = '家';
UPDATE address_book SET label = 'Work'   WHERE label = '公司';
UPDATE address_book SET label = 'School' WHERE label = '学校';

-- Cart and order lines keep a copy of the dish or set meal name. Refresh copies that are
-- still Chinese once the source name is English (no-op until the dishes are translated).
UPDATE shopping_cart c JOIN dish d    ON c.dish_id = d.id    SET c.name = d.name WHERE c.name REGEXP '\\p{Han}' AND d.name NOT REGEXP '\\p{Han}';
UPDATE shopping_cart c JOIN setmeal s ON c.setmeal_id = s.id SET c.name = s.name WHERE c.name REGEXP '\\p{Han}' AND s.name NOT REGEXP '\\p{Han}';
UPDATE order_detail o JOIN dish d     ON o.dish_id = d.id    SET o.name = d.name WHERE o.name REGEXP '\\p{Han}' AND d.name NOT REGEXP '\\p{Han}';
UPDATE order_detail o JOIN setmeal s  ON o.setmeal_id = s.id SET o.name = s.name WHERE o.name REGEXP '\\p{Han}' AND s.name NOT REGEXP '\\p{Han}';

-- Everything still in Chinese: dish and set meal names, plus free text people typed.
SELECT 'dish' AS tbl, id, CONCAT_WS(' | ', name, description) AS text FROM dish WHERE CONCAT_WS(' ', name, description) REGEXP '\\p{Han}'
UNION ALL SELECT 'setmeal', id, CONCAT_WS(' | ', name, description) FROM setmeal WHERE CONCAT_WS(' ', name, description) REGEXP '\\p{Han}'
UNION ALL SELECT 'category', id, name FROM category WHERE name REGEXP '\\p{Han}'
UNION ALL SELECT 'coupon', id, name FROM coupon WHERE name REGEXP '\\p{Han}'
UNION ALL SELECT 'rider', id, name FROM rider WHERE name REGEXP '\\p{Han}'
UNION ALL SELECT 'employee', id, name FROM employee WHERE name REGEXP '\\p{Han}'
UNION ALL SELECT 'user', id, name FROM `user` WHERE name REGEXP '\\p{Han}'
UNION ALL SELECT 'address_book', id, CONCAT_WS(' | ', consignee, detail, label) FROM address_book WHERE CONCAT_WS(' ', consignee, detail, label) REGEXP '\\p{Han}'
UNION ALL SELECT 'orders', id, CONCAT_WS(' | ', consignee, address, remark) FROM orders WHERE CONCAT_WS(' ', consignee, address, remark) REGEXP '\\p{Han}'
UNION ALL SELECT 'order_detail', id, name FROM order_detail WHERE name REGEXP '\\p{Han}'
UNION ALL SELECT 'shopping_cart', id, name FROM shopping_cart WHERE name REGEXP '\\p{Han}';
