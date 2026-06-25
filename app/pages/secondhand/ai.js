/**
 * ai.js
 * 二手闲置交易平台 - 客户端智能估价与 Qwen VL 智能视觉评估模块
 */

import { HTTP_REQUEST_URL } from '@/config/app.js';

// 是否使用模拟模式（后端未部署时设为 true）
const USE_MOCK = false;

// 模拟 AI 识别结果
function mockIdentifyProduct() {
  const mockResults = [
    {
      rawLabel: 'iPhone 14 Pro 128G 深空黑',
      confidence: '98.5%',
      name: 'iPhone 14 Pro 128G',
      category: '数码电子',
      suggestedOriginalPrice: 7999,
      defaultDesc: '自用一手爱机，成色极佳。无拆无修，屏幕几乎零划痕。带原装盒子，因换新机故低价出，非常抗打，买到就是赚到！'
    },
    {
      rawLabel: '小米平板 6 Pro 8GB+256GB',
      confidence: '96.2%',
      name: '小米平板 6 Pro',
      category: '数码电子',
      suggestedOriginalPrice: 2999,
      defaultDesc: '平板使用半年，屏幕完美无划痕，配件齐全，送原装保护套，性能强劲适合学习和娱乐！'
    },
    {
      rawLabel: '耐克 Air Max 270 运动鞋 42码',
      confidence: '94.8%',
      name: '耐克 Air Max 270',
      category: '服饰鞋帽',
      suggestedOriginalPrice: 1099,
      defaultDesc: '正品购入，穿了几次，成色95新，鞋底几乎无磨损，适合日常穿搭！'
    },
    {
      rawLabel: '高等数学同济第七版 教材',
      confidence: '99.2%',
      name: '高等数学教材',
      category: '图书教材',
      suggestedOriginalPrice: 49,
      defaultDesc: '教材完整无缺页，有少量笔记标注，适合备考复习使用！'
    },
    {
      rawLabel: '迪卡侬羽毛球拍套装',
      confidence: '92.5%',
      name: '羽毛球拍套装',
      category: '运动户外',
      suggestedOriginalPrice: 299,
      defaultDesc: '使用次数不多，拍线完好，送羽毛球一筒，适合入门和休闲运动！'
    }
  ];
  
  return mockResults[Math.floor(Math.random() * mockResults.length)];
}

// 将图片路径转换成 Base64 Data URL
async function getBase64Image(imagePath) {
  if (typeof imagePath === 'string') {
    // 已经是 base64 格式
    if (imagePath.startsWith('data:image')) {
      return imagePath;
    }
    
    // #ifdef H5
    // H5 环境：使用 fetch + FileReader
    return new Promise((resolve, reject) => {
      fetch(imagePath)
        .then(response => response.blob())
        .then(blob => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result);
          reader.onerror = () => reject(new Error('图片读取失败'));
          reader.readAsDataURL(blob);
        })
        .catch(() => reject(new Error('图片加载失败')));
    });
    // #endif
    
    // #ifndef H5
    // 小程序/APP 环境：使用文件管理器
    return new Promise((resolve, reject) => {
      uni.getFileSystemManager().readFile({
        filePath: imagePath,
        encoding: 'base64',
        success: (res) => {
          const base64 = 'data:image/jpeg;base64,' + res.data;
          resolve(base64);
        },
        fail: (err) => {
          console.error('Failed to convert image to base64:', err);
          reject(new Error('商品图格式转化失败，无法提交远程大模型识别'));
        }
      });
    });
    // #endif
  }
  throw new Error('不支持此类型商品的图像格式进行智能识别');
}

/**
 * 识别商品图片，自动映射分类与细节
 * @param {string} imagePath 图片路径
 */
export async function identifyProduct(imagePath) {
  console.log('AI 开始预测解析商品图片 (已更新为 Qwen 智能视觉大模型)...');
  
  // 使用模拟模式
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        const result = mockIdentifyProduct();
        console.log('模拟 AI 识别结果：', result);
        resolve(result);
      }, 1500); // 模拟 1.5 秒延迟
    });
  }
  
  try {
    const base64Data = await getBase64Image(imagePath);
    
    // H5 直连 AI Server，小程序走后端代理
    let identifyUrl = 'http://localhost:3000/api/identify';
    // #ifdef MP-WEIXIN
    identifyUrl = HTTP_REQUEST_URL + '/api/front/secondhand/identify';
    // #endif
    
    return new Promise((resolve, reject) => {
      uni.request({
        url: identifyUrl,
        method: 'POST',
        header: {
          'Content-Type': 'application/json'
        },
        data: {
          image: base64Data
        },
        success: (res) => {
          if (res.statusCode === 200) {
            const data = res.data;
            // H5 直连返回的是 AI 原始结果，小程序走代理返回 {code:200, data:...}
            if (data && data.code === 200 && data.data) {
              console.log('AI 识别成功(代理):', data.data);
              resolve(data.data);
            } else if (data && data.name) {
              console.log('AI 识别成功(直连):', data);
              resolve(data);
            } else {
              reject(new Error(data?.error || '识别失败'));
            }
          } else {
            reject(new Error('识别服务器返回异常'));
          }
        },
        fail: (err) => {
          console.error('AI identify request error:', err);
          reject(new Error('AI 连接识别服务异常，请重试'));
        }
      });
    });
  } catch (err) {
    console.error('AI recognition error:', err);
    throw new Error(err.message || 'AI 识别失败，请重试！');
  }
}

// 模拟空初始化支持以前版本的向后兼容
export async function initAI() {
  console.log('Qwen 实时视觉识别服务已开机预备完毕');
  return true;
}

/**
 * 智能估价计算方程式
 * 融合：估原价、二手成色率、商品类损耗折价率
 * @param {string} category 
 * @param {string} condition 
 * @param {number} originalPrice 
 */
export function calculateRecommendedPrice(category, condition, originalPrice) {
  if (!originalPrice || isNaN(originalPrice)) return 0;
  
  // A. 成色折损系数
  let conditionRatio = 0.50;
  switch (condition) {
    case '99新': conditionRatio = 0.85; break; // 近全新
    case '95新': conditionRatio = 0.70; break; // 微瑕轻微印记
    case '90新': conditionRatio = 0.55; break; // 正常成色
    case '85新': conditionRatio = 0.40; break; // 略微破损/划痕
    case '70新': conditionRatio = 0.20; break; // 饱经沧桑
  }

  // B. 品类行业折旧折让水平
  let categoryFactor = 1.0;
  if (category === '数码电子') {
    categoryFactor = 0.90; // 电子数码更新节奏快
  } else if (category === '图书教材') {
    categoryFactor = 0.45; // 期末教材基本是廉价走量
  } else if (category === '日用百货') {
    categoryFactor = 0.75;
  } else if (category === '运动户外') {
    categoryFactor = 0.85; // 体育球拍持久耐用
  } else if (category === '短袖') {
    categoryFactor = 0.65; // 短袖印花磨损略快，流转折让中等
  } else if (category === '裤子') {
    categoryFactor = 0.68;
  } else if (category === '鞋子') {
    categoryFactor = 0.70;
  } else if (category === '外套') {
    categoryFactor = 0.72; // 外套保值率相对高
  }

  const finalRate = conditionRatio * categoryFactor;
  const result = Math.round(originalPrice * finalRate);
  
  return Math.max(1, result); // 最低定价 1 元，绝对贴心合理
}
