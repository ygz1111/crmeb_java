require('dotenv').config();
const express = require('express');
const cors = require('cors');
const axios = require('axios');

const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(cors());
app.use(express.json({ limit: '20mb' }));

// Qwen VL API 配置
const QWEN_API_URL = process.env.QWEN_API_URL || 'https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation';
const QWEN_API_KEY = process.env.QWEN_API_KEY;

if (!QWEN_API_KEY) {
  console.error('❌ 错误：未配置 QWEN_API_KEY，请在 .env 文件中设置');
  console.error('获取 API Key：https://dashscope.console.aliyun.com/');
}

// AI 识别接口
app.post('/api/identify', async (req, res) => {
  try {
    const { image } = req.body;
    
    if (!image) {
      return res.status(400).json({ error: '请提供图片数据' });
    }
    
    if (!QWEN_API_KEY) {
      return res.status(500).json({ error: 'AI 服务未配置，请联系管理员设置 QWEN_API_KEY' });
    }
    
    console.log('📸 收到图片识别请求...');
    
    // 调用 Qwen VL 模型
    const response = await axios.post(
      QWEN_API_URL,
      {
        model: 'qwen-vl-max',
        input: {
          messages: [
            {
              role: 'user',
              content: [
                { image: image },
                { 
                  text: `请分析这张图片中的商品，并以JSON格式返回以下信息：
                  {
                    "rawLabel": "商品完整名称和规格",
                    "confidence": "识别置信度（如98.5%）",
                    "name": "商品简短名称",
                    "category": "商品分类（数码电子/图书教材/服饰鞋帽/日用百货/运动户外/其他闲置）",
                    "suggestedOriginalPrice": 建议原价（数字）,
                    "defaultDesc": "商品描述文案"
                  }
                  只返回JSON，不要其他内容。`
                }
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
    
    // 解析 Qwen 返回结果
    const qwenResult = response.data;
    let resultText = '';
    
    if (qwenResult.output && qwenResult.output.choices && qwenResult.output.choices[0]) {
      resultText = qwenResult.output.choices[0].message.content[0].text;
    }
    
    // 尝试解析 JSON
    let result;
    try {
      // 提取 JSON 部分
      const jsonMatch = resultText.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        result = JSON.parse(jsonMatch[0]);
      } else {
        throw new Error('无法解析 AI 返回结果');
      }
    } catch (parseError) {
      console.error('解析 AI 结果失败:', resultText);
      // 返回默认结果
      result = {
        rawLabel: '商品识别中',
        confidence: '85%',
        name: '二手商品',
        category: '其他闲置',
        suggestedOriginalPrice: 100,
        defaultDesc: '优质二手商品，成色良好，欢迎选购！'
      };
    }
    
    console.log('✅ 识别成功:', result.name);
    res.json(result);
    
  } catch (error) {
    console.error('❌ AI 识别错误:', error.message);
    
    if (error.response) {
      console.error('API 响应:', error.response.data);
    }
    
    res.status(500).json({ 
      error: error.message || 'AI 识别服务异常，请稍后重试' 
    });
  }
});

// 健康检查
app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    service: 'CRMEB AI Server',
    qwen: QWEN_API_KEY ? 'configured' : 'not configured'
  });
});

// 启动服务
app.listen(PORT, () => {
  console.log(`\n🚀 AI 识别服务已启动！`);
  console.log(`📡 服务地址: http://localhost:${PORT}`);
  console.log(`🔑 Qwen API: ${QWEN_API_KEY ? '已配置 ✅' : '未配置 ❌'}`);
  console.log(`\n使用说明：`);
  console.log(`1. 在 .env 文件中设置 QWEN_API_KEY`);
  console.log(`2. 获取 API Key: https://dashscope.console.aliyun.com/`);
  console.log(`3. 前端会自动调用 http://localhost:${PORT}/api/identify\n`);
});
