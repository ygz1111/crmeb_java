import os

products = [
    (201, 129.00, 199.00),
    (202, 89.00, 159.00),
    (203, 168.00, 289.00),
    (204, 79.00, 139.00),
    (205, 138.00, 228.00),
    (206, 49.00, 99.00),
    (207, 119.00, 199.00),
    (208, 88.00, 158.00),
]

img_base = 'crmebimage/public/product/2026/06/29/clothing_'
attr_id = 1000
attr_val_id = 5000

lines = []
lines.append('-- 商品规格数据 - 修复产品201-208无法进入详情页')
lines.append('-- 单规格商品: 每个商品添加一个尺码-均码规格')
lines.append('')

for i, (pid, price, ot_price) in enumerate(products, 1):
    img = f'{img_base}{i:02d}.jpg'
    cost = f'{price/3:.2f}'
    attr_name = '尺码'
    attr_val = '均码'

    lines.append(
        f"INSERT INTO eb_store_product_attr "
        f"(id, product_id, attr_name, attr_values, sort, type) "
        f"VALUES ({attr_id}, {pid}, '{attr_name}', '{attr_val}', 0, 0);"
    )

    lines.append(
        f"INSERT INTO eb_store_product_attr_value "
        f"(id, product_id, suk, stock, sales, price, image, unique, cost, bar_code, ot_price, "
        f"weight, volume, brokerage, brokerage_two, type, quota, quota_show, attr_value) "
        f"VALUES ({attr_val_id}, {pid}, '{attr_val}', 500, 0, {price}, "
        f"'{img}', '', {cost}, '', {ot_price}, 0.00, 0.00, 0.00, 0.00, 0, 500, 500, "
        f"'{{\"{attr_name}\":\"{attr_val}\"}}');"
    )

    attr_id += 1
    attr_val_id += 1

out_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'clothing_product_attr.sql')
with open(out_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines) + '\n')

print(f'OK: {len(lines)} lines written to clothing_product_attr.sql')
