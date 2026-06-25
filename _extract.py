import zipfile
z = zipfile.ZipFile(r'D:\RECM\crmeb_java\校园二手交易ai发布平台.zip', 'r')
for n in z.namelist():
    if 'goods' in n.lower() and 'detail' in n.lower():
        print(n)
z.close()
