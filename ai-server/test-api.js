const axios = require('axios');
require('dotenv').config();

const QWEN_API_KEY = process.env.QWEN_API_KEY;
const QWEN_API_URL = 'https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation';

async function testQwenAPI() {
  console.log('测试 Qwen API Key...');
  console.log('API Key:', QWEN_API_KEY ? QWEN_API_KEY.substring(0, 20) + '...' : '未配置');
  
  // 使用一个简单的测试图片（1x1 红色像素）
  const testImage = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jN0gAAAABJRU5ErkJggg==';
  
  try {
    const response = await axios.post(
      QWEN_API_URL,
      {
        model: 'qwen-vl-max',
        input: {
          messages: [
            {
              role: 'user',
              content: [
                { image: testImage },
                { text: '这是什么？请简短回答。' }
              ]
            }
          ]
        },
        parameters: {
          result_format: 'message'
        }
      },
      {
        headers: {
          'Authorization': `Bearer ${QWEN_API_KEY}`,
          'Content-Type': 'application/json'
        },
        timeout: 30000
      }
    );
    
    console.log('\n✅ API Key 有效！');
    console.log('响应:', JSON.stringify(response.data, null, 2));
    
  } catch (error) {
    console.log('\n❌ API Key 无效或请求失败');
    console.log('错误信息:', error.message);
    
    if (error.response) {
      console.log('状态码:', error.response.status);
      console.log('错误详情:', JSON.stringify(error.response.data, null, 2));
    }
  }
}

testQwenAPI();
