import urllib.request
import json

url = 'http://localhost:20510/api/front/secondhand/identify'
data = json.dumps({"image": "data:image/jpeg;base64,/9j/4AAQ"}).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})
try:
    resp = urllib.request.urlopen(req, timeout=30)
    result = resp.read().decode('utf-8')
    print('Status:', resp.status)
    print('Response:', result)
except urllib.error.HTTPError as e:
    print(f'HTTP Error: {e.code}')
    print(e.read().decode('utf-8'))
except Exception as e:
    print(f'Error: {type(e).__name__}: {e}')
