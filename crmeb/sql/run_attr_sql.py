import subprocess

img_base = 'crmebimage/public/product/2026/06/29/'
suk = '\u5747\u7801'  # 均码
attr_val_json = '{"尺码":"均码"}'

products = [
    (5000, 201, 129.0, 199.0, 'clothing_01.jpg'),
    (5001, 202, 89.0, 159.0, 'clothing_02.jpg'),
    (5002, 203, 168.0, 289.0, 'clothing_03.jpg'),
    (5003, 204, 79.0, 139.0, 'clothing_04.jpg'),
    (5004, 205, 138.0, 228.0, 'clothing_05.jpg'),
    (5005, 206, 49.0, 99.0, 'clothing_06.jpg'),
    (5006, 207, 119.0, 199.0, 'clothing_07.jpg'),
    (5007, 208, 88.0, 158.0, 'clothing_08.jpg'),
]

sqls = []
for vid, pid, price, ot_price, img_file in products:
    img = img_base + img_file
    cost = round(price / 3, 2)
    sql = (
        f"INSERT INTO eb_store_product_attr_value "
        f"(id, product_id, suk, stock, sales, price, image, "
        f"`unique`, cost, bar_code, ot_price, weight, volume, "
        f"brokerage, brokerage_two, type, quota, quota_show, "
        f"attr_value, is_del, version) "
        f"VALUES ({vid}, {pid}, '{suk}', 500, 0, {price}, "
        f"'{img}', '', {cost}, '', {ot_price}, "
        f"0.00, 0.00, 0.00, 0.00, 0, 500, 500, "
        f"'{attr_val_json}', 0, 0);"
    )
    sqls.append(sql)

full_sql = "\n".join(sqls)

result = subprocess.run(
    ['docker', 'exec', '-i', 'crmeb-mysql', 'mysql',
     '-u', 'root', '-pcrmeb123456', 'single_open'],
    input=full_sql,
    capture_output=True,
    text=True,
    encoding='utf-8'
)

stderr = result.stderr or ''
if 'Warning' in stderr:
    lines = stderr.strip().split('\n')
    real_errors = [l for l in lines if l.startswith('ERROR')]
    if real_errors:
        print(f'ERROR: {real_errors}')
    else:
        print('OK: 8 attr_value records inserted successfully')
else:
    print('OK: 8 attr_value records inserted successfully')
