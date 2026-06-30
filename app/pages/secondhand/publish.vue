<template>
  <view class="container">
    <!-- 顶部极简导航栏 -->
    <view class="nav-bar">
        <text class="nav-title">{{ isEditMode ? '编辑闲置宝贝' : '发布闲置宝贝' }}</text>
      <text class="nav-subtitle">AI 智能辅助估价与分类发布</text>
    </view>

    <scroll-view class="scroll-content" scroll-y="true">
      <!-- 1. 媒体资源上传区域 -->
      <view class="upload-section card">
        <view class="section-title">
          <text class="dot"></text>
          <text class="text">上传宝贝图片</text>
          <text class="tip">（AI 将根据首张图自动识别）</text>
        </view>

        <!-- 上传格栅 -->
        <view class="image-grid">
          <view 
            class="image-preview-wrapper" 
            v-for="(img, idx) in uploadImages" 
            :key="idx"
          >
            <image class="preview-img" :src="img" mode="aspectFill"></image>
            <view class="delete-btn" @tap="removeImage(idx)">
              <text class="delete-icon">×</text>
            </view>
            <view v-if="idx === 0" class="cover-tag">首图AI识别</view>
          </view>

          <!-- 槽位：触发上传 -->
          <view 
            v-if="uploadImages.length < 5" 
            class="upload-trigger" 
            @tap="handleChooseImage"
          >
            <view class="trigger-icon">+</view>
            <text class="trigger-text">拍照 / 传图</text>
            <text class="trigger-sub">{{ uploadImages.length }}/5</text>
          </view>
        </view>
      </view>

      <!-- 2. AI 识别诊断台 - 增强版 -->
      <view class="ai-hub card" v-if="aiWorking || aiResult">
        <view class="ai-header">
          <view class="ai-title">
            <text class="ai-sparkle">✦</text>
            <text class="ai-title-text">Campus AI 智能估价引擎</text>
          </view>
          <view class="ai-badge" :class="aiWorking ? 'loading' : 'done'">
            {{ aiWorking ? '分析中...' : '分析完成' }}
          </view>
        </view>

        <!-- 分步进度条 -->
        <view class="ai-progress" v-if="aiWorking">
          <view class="progress-step" :class="{ active: aiStep >= 1, done: aiStep > 1 }">
            <view class="step-dot">{{ aiStep > 1 ? 'Y' : '1' }}</view>
            <text class="step-label">图像预处理</text>
          </view>
          <view class="progress-line" :class="{ active: aiStep >= 2 }"></view>
          <view class="progress-step" :class="{ active: aiStep >= 2, done: aiStep > 2 }">
            <view class="step-dot">{{ aiStep > 2 ? 'Y' : '2' }}</view>
            <text class="step-label">特征提取</text>
          </view>
          <view class="progress-line" :class="{ active: aiStep >= 3 }"></view>
          <view class="progress-step" :class="{ active: aiStep >= 3, done: aiStep > 3 }">
            <view class="step-dot">{{ aiStep > 3 ? 'Y' : '3' }}</view>
            <text class="step-label">分类识别</text>
          </view>
          <view class="progress-line" :class="{ active: aiStep >= 4 }"></view>
          <view class="progress-step" :class="{ active: aiStep >= 4 }">
            <view class="step-dot">4</view>
            <text class="step-label">定价估算</text>
          </view>
        </view>

        <!-- 加载中动效 -->
        <view class="ai-loading-box" v-if="aiWorking">
          <view class="scan-line"></view>
          <text class="ai-loading-tips">{{ aiStepText }}</text>
          <text class="ai-loading-sub">Qwen VL 多模态大模型正在分析您的商品图片...</text>
        </view>

        <!-- AI 分析完成报告 -->
        <view class="ai-report" v-if="!aiWorking && aiResult">
          <!-- 识别结果卡片 -->
          <view class="report-card">
            <view class="report-row">
              <text class="report-lbl">识别主体</text>
              <text class="report-val highlight">{{ aiResult.name }}</text>
            </view>
            <view class="report-row">
              <text class="report-lbl">原始预测</text>
              <text class="report-val desc">{{ aiResult.rawLabel }}</text>
            </view>
            <view class="report-row">
              <text class="report-lbl">匹配分类</text>
              <text class="badge-cat">{{ aiResult.category }}</text>
            </view>
            <view class="report-row">
              <text class="report-lbl">置信度</text>
              <view class="confidence-bar">
                <view class="confidence-fill" :style="confidenceBarStyle"></view>
                <text class="confidence-text">{{ aiResult.confidence }}</text>
              </view>
            </view>
          </view>

          <!-- AI 估价仪表盘 -->
          <view class="ai-gauge-section" v-if="aiResult.suggestedOriginalPrice">
            <view class="gauge-title">AI 智能估价结果</view>
            <view class="gauge-row">
              <view class="gauge-item">
                <text class="gauge-label">市场参考价</text>
                <text class="gauge-value gray">¥{{ aiResult.suggestedOriginalPrice }}</text>
              </view>
              <view class="gauge-arrow">→</view>
              <view class="gauge-item primary">
                <text class="gauge-label">AI 建议售价</text>
                <text class="gauge-value orange">¥{{ suggestedPrice }}</text>
              </view>
              <view class="gauge-item">
                <text class="gauge-label">保值率</text>
                <text class="gauge-value green">{{ retentionRate }}%</text>
              </view>
            </view>
          </view>

          <!-- 折旧因子可视化 -->
          <view class="factor-section" v-if="aiResult.suggestedOriginalPrice">
            <view class="factor-title">定价因子分解</view>
            <view class="factor-bar-row">
              <text class="factor-label">成色折损</text>
              <view class="factor-bar-bg">
                <view class="factor-bar-fill condition" :style="conditionBarStyle"></view>
              </view>
              <text class="factor-val">{{ conditionPercent }}%</text>
            </view>
            <view class="factor-bar-row">
              <text class="factor-label">品类折旧</text>
              <view class="factor-bar-bg">
                <view class="factor-bar-fill category" :style="categoryBarStyle"></view>
              </view>
              <text class="factor-val">{{ categoryPercent }}%</text>
            </view>
            <view class="factor-formula">
              <text class="formula-text">定价公式: ¥{{ aiResult.suggestedOriginalPrice }} x {{ conditionPercent }}% x {{ categoryPercent }}% = ¥{{ suggestedPrice }}</text>
            </view>
          </view>

          <view class="ai-alert">
            <text class="alert-icon">*</text>
            <text class="alert-txt">AI 已自动填充标题、描述和定价，您可以手动调整后发布</text>
          </view>
        </view>
      </view>

      <!-- 3. 商品详情主表单 -->
      <view class="form-section card">
        <view class="section-title">
          <text class="dot blue"></text>
          <text class="text">完善商品信息</text>
        </view>

        <!-- 宝贝标题 -->
        <view class="form-item border-bottom">
          <text class="form-label">商品标题</text>
          <input 
            class="form-input" 
            type="text" 
            v-model="formData.title" 
            placeholder="写下商品核心特色，如：出学长九五新自行车"
            placeholder-class="ph"
          />
        </view>

        <!-- 类目选择 -->
        <view class="form-item border-bottom">
          <text class="form-label">商品分类</text>
          <picker 
            mode="selector" 
            :range="categoryOptions" 
            :value="categoryIndex" 
            @change="handleCategoryChange"
            class="form-picker"
          >
            <view class="picker-value">
              <text :class="formData.category ? '' : 'ph-color'">
                {{ formData.category || '请选择商品分类' }}
              </text>
              <text class="arrow">▶</text>
            </view>
          </picker>
        </view>

        <!-- 宝贝成色 -->
        <view class="form-item border-bottom">
          <text class="form-label">商品成色</text>
          <picker 
            mode="selector" 
            :range="conditionOptions" 
            :value="conditionIndex" 
            @change="handleConditionChange"
            class="form-picker"
          >
            <view class="picker-value">
              <text>{{ formData.condition }}</text>
              <text class="arrow">▶</text>
            </view>
          </picker>
        </view>

        <!-- 详细陈述 -->
        <view class="form-textarea-item">
          <text class="form-label">宝贝详情叙述</text>
          <textarea 
            class="form-textarea" 
            v-model="formData.desc"
            placeholder="请叙述物品使用状况，如入手渠道、有无破损、转手原因等细节..."
            placeholder-class="ph"
            maxlength="400"
          />
          <text class="word-limit">{{ formData.desc.length }}/400</text>
        </view>
      </view>

      <!-- 4. 价格定位卡片 -->
      <view class="price-section card">
        <view class="section-title">
          <text class="dot orange"></text>
          <text class="text">定价测算中心</text>
        </view>

        <view class="price-grid">
          <!-- 预估原价 -->
          <view class="price-box">
            <text class="price-lbl">参考原价 (¥)</text>
            <input 
              class="price-input" 
              type="number" 
              v-model="formData.originalPrice" 
              placeholder="0"
              @input="recalculatePrices"
            />
          </view>

          <!-- 售价 -->
          <view class="price-box active">
            <text class="price-lbl">设定售价 (¥)</text>
            <input 
              class="price-input orange-txt" 
              type="number" 
              v-model="formData.price" 
              placeholder="0"
            />
          </view>
        </view>

        <!-- AI 智能估算成果面板 -->
        <view class="ai-pricing-meter" v-if="formData.originalPrice > 0">
          <view class="meter-header">
            <text class="meter-icon">📈</text>
            <text class="meter-title">计算方程式结论</text>
          </view>
          <view class="meter-val-row">
            <text class="meter-descr">
              根据成色【<text class="bold">{{ formData.condition }}</text>】，
              品类【<text class="bold">{{ formData.category || '其他' }}</text>】
              自动给出的科学推荐售价为：
            </text>
            <view class="meter-price" @tap="applySuggestedPrice">
              <text class="currency">¥</text>
              <text class="number">{{ suggestedPrice }}</text>
              <text class="apply-badge">一键采纳</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 5. 交易交付信息 -->
      <view class="delivery-section card">
        <view class="section-title">
          <text class="dot green"></text>
          <text class="text">交付与位置</text>
        </view>

        <!-- 交付方式 -->
        <view class="form-item border-bottom">
          <text class="form-label">交付方式</text>
          <picker 
            mode="selector" 
            :range="locationOptions" 
            :value="locationIndex" 
            @change="handleLocationChange"
            class="form-picker"
          >
            <view class="picker-value">
              <text>{{ formData.location }}</text>
              <text class="arrow">▶</text>
            </view>
          </picker>
        </view>

        <!-- 收货地址 -->
        <view class="form-item border-bottom" @tap="showRegionPanel = true">
          <text class="form-label">所在地区</text>
          <view class="picker-value">
            <text :class="provinceIdx >= 0 ? '' : 'ph-color'">{{ regionDisplayText }}</text>
            <text class="arrow">▶</text>
          </view>
        </view>
        <!-- 省市区弹出面板 -->
        <view class="region-mask" v-if="showRegionPanel" @tap="showRegionPanel = false"></view>
        <view class="region-panel" v-if="showRegionPanel">
          <view class="region-panel-hd">
            <text @tap="showRegionPanel = false">取消</text>
            <text class="region-panel-ok" @tap="confirmRegion">确定</text>
          </view>
          <view class="region-panel-bd">
            <picker-view :value="[provinceIdx < 0 ? 0 : provinceIdx, cityIdx < 0 ? 0 : cityIdx, areaIdx < 0 ? 0 : areaIdx]" @change="onRegionPickerChange" :indicator-style="'height: 88rpx;'">
              <picker-view-column>
                <view class="region-col-item" v-for="(item, i) in provinceList" :key="i">{{ item.label }}</view>
              </picker-view-column>
              <picker-view-column>
                <view class="region-col-item" v-for="(item, i) in cityList" :key="i">{{ item.label }}</view>
              </picker-view-column>
              <picker-view-column>
                <view class="region-col-item" v-for="(item, i) in areaList" :key="i">{{ item.label }}</view>
              </picker-view-column>
            </picker-view>
          </view>
        </view>

        <!-- 详细地址 -->
        <view class="form-item border-bottom">
          <text class="form-label">详细地址</text>
          <input 
            class="form-input" 
            type="text" 
            v-model="formData.detailAddress" 
            placeholder="街道、门牌号等（如：建国路88号XX小区）"
            placeholder-class="ph"
          />
        </view>

        <!-- 是否支持包邮 -->
        <view class="form-item">
          <text class="form-label">支持快递包邮</text>
          <switch 
            :checked="formData.isPost" 
            @change="handlePostChange" 
            color="#FF6B00"
          />
        </view>
      </view>

      <!-- 底部发布按钮 -->
      <view class="submit-footer">
        <button 
          class="submit-button" 
          :loading="publishLoading" 
          :disabled="publishLoading"
          @tap="handleSubmit"
        >
          {{ isEditMode ? '保存修改' : '立即发布' }}
        </button>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { HTTP_REQUEST_URL } from '@/config/app.js';
import { identifyProduct, calculateRecommendedPrice } from './ai.js';
import { publishSecondHand, updateSecondHand } from '@/api/secondhand.js';
import { mapGetters } from 'vuex';
import provinces from '@/components/wPicker/city-data/province.js';
import citys from '@/components/wPicker/city-data/city.js';
import areas from '@/components/wPicker/city-data/area.js';

export default {
  computed: {
    ...mapGetters(['isLogin', 'uid']),
    regionDisplayText() {
      if (this.provinceIdx >= 0 && this.provinceList[this.provinceIdx]) {
        const parts = [this.provinceList[this.provinceIdx].label];
        if (this.cityIdx >= 0 && this.cityList[this.cityIdx]) parts.push(this.cityList[this.cityIdx].label);
        if (this.areaIdx >= 0 && this.areaList[this.areaIdx]) parts.push(this.areaList[this.areaIdx].label);
        return parts.join(' ');
      }
      return '请选择所在地区';
    },
    retentionRate() {
      if (!this.formData.originalPrice || !this.suggestedPrice) return 0;
      return Math.round((this.suggestedPrice / this.formData.originalPrice) * 100);
    },
    conditionPercent() {
      return Math.round(this.conditionRatio * 100);
    },
    categoryPercent() {
      return Math.round(this.categoryFactor * 100);
    },
    conditionBarStyle() {
      return { width: this.conditionPercent + '%' };
    },
    categoryBarStyle() {
      return { width: this.categoryPercent + '%' };
    },
    confidenceBarStyle() {
      if (!this.aiResult || !this.aiResult.confidence) return { width: '0%' };
      return { width: this.aiResult.confidence };
    }
  },
  onLoad(options) {
    // TabBar页面的onLoad只触发一次，使用onShow加载编辑数据
  },
  onShow() {
    // 从全局变量读取编辑数据（TabBar页面不支持URL传参）
    const editItem = getApp().globalData.editSecondHand;
    if (editItem && editItem.id && !this.isEditMode) {
      this.isEditMode = true;
      this.editId = editItem.id;
      this.formData.title = editItem.storeName || '';
      this.formData.category = editItem.category || '';
      this.formData.condition = editItem.itemCondition || '95新';
      this.formData.desc = editItem.storeInfo || '';
      this.formData.price = editItem.price || null;
      this.formData.originalPrice = editItem.otPrice || null;
      this.formData.location = editItem.location || '同城面交';
      this.formData.detailAddress = editItem.detailAddress || '';
      if (editItem.image) {
        this.uploadImages = [editItem.image];
      }
      if (editItem.sliderImage) {
        try {
          this.uploadImages = JSON.parse(editItem.sliderImage);
        } catch (e) {
          this.uploadImages = editItem.sliderImage.split(',').filter(function(u) { return u; });
        }
      }
      const catIdx = this.categoryOptions.indexOf(editItem.category);
      if (catIdx !== -1) this.categoryIndex = catIdx;
      const conIdx = this.conditionOptions.indexOf(editItem.itemCondition);
      if (conIdx !== -1) this.conditionIndex = conIdx;
      this.recalculatePrices();
      // 清除全局变量，避免下次进入时重复加载
      getApp().globalData.editSecondHand = null;
    }

    if (!this.isLogin) {
      uni.showModal({
        title: '提示',
        content: '请先登录后再发布商品',
        showCancel: false,
        success: () => {
          uni.switchTab({ url: '/pages/user/index' });
        }
      });
    }
  },
  data() {
    return {
      categoryOptions: ['数码电子', '图书教材', '服饰鞋帽', '日用百货', '运动户外', '其他闲置'],
      conditionOptions: ['99新', '95新', '90新', '85新', '70新'],
      locationOptions: ['同城面交', '快递邮寄（包邮）', '快递邮寄（到付）', '校园自提', '同城跑腿'],
      
      uploadImages: [],
      aiWorking: false,
      aiResult: null,
      aiStep: 0,
      aiStepText: '正在初始化 AI 引擎...',
      publishLoading: false,
      isEditMode: false,
      editId: null,
      
      categoryIndex: 0,
      conditionIndex: 1,
      locationIndex: 0,
      
      provinceList: provinces,
      cityList: [],
      areaList: [],
      provinceIdx: -1,
      cityIdx: -1,
      areaIdx: -1,
      showRegionPanel: false,
      tempProvinceIdx: 0,
      tempCityIdx: 0,
      tempAreaIdx: 0,
      
      formData: {
        title: '',
        category: '',
        condition: '95新',
        desc: '',
        originalPrice: null,
        price: null,
        location: '同城面交',
        detailAddress: '',
        isPost: false
      },
      
      suggestedPrice: 0,
      conditionRatio: 0.7,
      categoryFactor: 0.7
    };
  },
  
  watch: {
    'formData.condition': function() {
      this.recalculatePrices();
    },
    'formData.category': function() {
      this.recalculatePrices();
    }
  },
  
  methods: {
    onProvinceChange(e) {
      this.provinceIdx = Number(e.detail.value);
      this.cityList = citys[this.provinceIdx] || [];
      this.cityIdx = -1;
      this.areaList = [];
      this.areaIdx = -1;
    },
    onCityChange(e) {
      this.cityIdx = Number(e.detail.value);
      this.areaList = (areas[this.provinceIdx] && areas[this.provinceIdx][this.cityIdx]) || [];
      this.areaIdx = -1;
    },
    onAreaChange(e) {
      this.areaIdx = Number(e.detail.value);
    },
    onRegionPickerChange(e) {
      const val = e.detail.value;
      const pIdx = val[0];
      if (pIdx !== this.tempProvinceIdx) {
        this.tempProvinceIdx = pIdx;
        this.cityList = citys[pIdx] || [];
        this.tempCityIdx = 0;
        this.areaList = (areas[pIdx] && areas[pIdx][0]) || [];
        this.tempAreaIdx = 0;
      }
      const cIdx = val[1];
      if (cIdx !== this.tempCityIdx) {
        this.tempCityIdx = cIdx;
        this.areaList = (areas[pIdx] && areas[pIdx][cIdx]) || [];
        this.tempAreaIdx = 0;
      }
      this.tempAreaIdx = val[2];
    },
    confirmRegion() {
      this.provinceIdx = this.tempProvinceIdx;
      this.cityIdx = this.tempCityIdx;
      this.areaIdx = this.tempAreaIdx;
      this.cityList = citys[this.provinceIdx] || [];
      this.areaList = (areas[this.provinceIdx] && areas[this.provinceIdx][this.cityIdx]) || [];
      this.showRegionPanel = false;
    },
    handleChooseImage() {
      const that = this;
      uni.chooseImage({
        count: 5 - that.uploadImages.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: function(res) {
          const paths = res.tempFilePaths;
          that.uploadImages = that.uploadImages.concat(paths);
          
          if (that.uploadImages.length === paths.length) {
            that.startAiInference(that.uploadImages[0]);
          }
        }
      });
    },
    
    removeImage(idx) {
      this.uploadImages.splice(idx, 1);
      if (idx === 0) {
        if (this.uploadImages.length > 0) {
          this.startAiInference(this.uploadImages[0]);
        } else {
          this.aiResult = null;
        }
      }
    },
    
    async startAiInference(imgUrl) {
      const that = this;
      that.aiWorking = true;
      that.aiResult = null;
      that.aiStep = 0;
      
      const steps = [
        { step: 1, text: '正在预处理图像数据...', delay: 400 },
        { step: 2, text: '正在提取视觉特征向量...', delay: 600 },
        { step: 3, text: '正在调用 Qwen VL 多模态模型识别...', delay: 500 },
        { step: 4, text: '正在计算智能定价方案...', delay: 300 }
      ];
      
      for (const s of steps) {
        that.aiStep = s.step;
        that.aiStepText = s.text;
        await new Promise(r => setTimeout(r, s.delay));
      }
      
      try {
        const result = await identifyProduct(imgUrl);
        that.aiResult = result;
        
        that.formData.title = `出【${result.category}】${that.formData.condition} ${result.name}`;
        that.formData.category = result.category;
        that.formData.desc = result.defaultDesc;
        that.formData.originalPrice = result.suggestedOriginalPrice;
        
        that.recalculatePrices();
        
        const catIdx = that.categoryOptions.indexOf(result.category);
        if (catIdx !== -1) that.categoryIndex = catIdx;
        
      } catch (error) {
        uni.showToast({
          title: error.message || '识别失败，请手动修正表单',
          icon: 'none',
          duration: 3000
        });
      } finally {
        that.aiWorking = false;
      }
    },
    
    handleCategoryChange(e) {
      const selectVal = e.detail.value;
      this.categoryIndex = selectVal;
      this.formData.category = this.categoryOptions[selectVal];
    },
    
    handleConditionChange(e) {
      const selectVal = e.detail.value;
      this.conditionIndex = selectVal;
      this.formData.condition = this.conditionOptions[selectVal];
    },
    
    handleLocationChange(e) {
      const selectVal = e.detail.value;
      this.locationIndex = selectVal;
      this.formData.location = this.locationOptions[selectVal];
    },
    
    handlePostChange(e) {
      this.formData.isPost = e.detail.value;
    },
    
    recalculatePrices() {
      if (this.formData.originalPrice > 0) {
        // 更新折旧因子
        this.updateFactors();
        this.suggestedPrice = calculateRecommendedPrice(
          this.formData.category,
          this.formData.condition,
          this.formData.originalPrice
        );
      }
    },
    
    updateFactors() {
      const conditionMap = { '99新': 0.85, '95新': 0.70, '90新': 0.55, '85新': 0.40, '70新': 0.20 };
      const categoryMap = { '数码电子': 0.90, '图书教材': 0.45, '日用百货': 0.75, '运动户外': 0.85, '服饰鞋帽': 0.70, '其他闲置': 0.60 };
      this.conditionRatio = conditionMap[this.formData.condition] || 0.7;
      this.categoryFactor = categoryMap[this.formData.category] || 0.7;
    },
    
    applySuggestedPrice() {
      if (this.suggestedPrice > 0) {
        this.formData.price = this.suggestedPrice;
        uni.showToast({
          title: '已自动填入 AI 建议售价',
          icon: 'success'
        });
      }
    },
    
    async handleSubmit() {
      const that = this;
      
      // 检查登录状态
      if (!that.isLogin) {
        uni.showModal({
          title: '提示',
          content: '请先登录后再发布商品',
          showCancel: false,
          success: () => {
            uni.switchTab({ url: '/pages/user/index' });
          }
        });
        return;
      }
      
      if (that.uploadImages.length === 0) {
        uni.showToast({ title: '请至少上传一张宝贝正面图哦', icon: 'none' });
        return;
      }
      if (!that.formData.title.trim()) {
        uni.showToast({ title: '请输入商品标题名称', icon: 'none' });
        return;
      }
      if (!that.formData.category) {
        uni.showToast({ title: '请点选当前商品归类目', icon: 'none' });
        return;
      }
      if (!that.formData.price || that.formData.price <= 0) {
        uni.showToast({ title: '请填写合理的商品出售价格', icon: 'none' });
        return;
      }
      if (!that.formData.desc.trim() || that.formData.desc.length < 10) {
        uni.showToast({ title: '请多些描述几笔宝贝现状(至少10字)', icon: 'none' });
        return;
      }
      
      that.publishLoading = true;
      
      try {
        const uploadedImages = [];
        for (let i = 0; i < that.uploadImages.length; i++) {
          const img = that.uploadImages[i];
          // 编辑模式：已有图片URL直接使用，无需重新上传
          if (that.isEditMode && (img.indexOf("crmebimage/") !== -1 || img.indexOf("http") === 0)) {
            uploadedImages.push(img);
            continue;
          }
          const uploadResult = await that.uploadImage(img);
          if (uploadResult) {
            uploadedImages.push(uploadResult);
          }
        }
        
        // 分类名称映射（可以根据实际数据库分类ID调整）
        const categoryMap = {
          '数码电子': '283',
          '图书教材': '280',
          '服饰鞋帽': '282',
          '日用百货': '284',
          '运动户外': '281',
          '其他闲置': '284'
        };
        
        const submitData = {
          storeName: that.formData.title,
          storeInfo: that.formData.desc,
          cateId: categoryMap[that.formData.category] || '284',
          category: that.formData.category,
          price: that.formData.price,
          otPrice: that.formData.originalPrice || that.formData.price,
          itemCondition: that.formData.condition,
          image: uploadedImages[0] || '',
          sliderImage: JSON.stringify(uploadedImages),
          stock: 1,
          unitName: '件',
          aiCategory: that.aiResult?.category || that.formData.category,
          aiPredictedPrice: that.aiResult?.suggestedOriginalPrice || that.formData.originalPrice,
          aiConfidence: that.aiResult?.confidence || '',
          location: that.formData.location + (that.provinceIdx >= 0 ? ' ' + that.provinceList[that.provinceIdx].label + (that.cityIdx >= 0 ? that.cityList[that.cityIdx].label : '') + (that.areaIdx >= 0 ? that.areaList[that.areaIdx].label : '') : '') + (that.formData.detailAddress ? ' ' + that.formData.detailAddress : '')
        };

        // 编辑模式：带上商品ID
        if (that.isEditMode && that.editId) {
          submitData.id = that.editId;
        }

        console.log('提交数据：', submitData);

        let res;
        if (that.isEditMode) {
          res = await updateSecondHand(submitData);
        } else {
          res = await publishSecondHand(submitData);
        }

        that.publishLoading = false;

        if (res && res.code === 200) {
          if (that.isEditMode) {
            that.isEditMode = false;
            that.editId = null;
            uni.showToast({ title: '保存成功', icon: 'success' });
            setTimeout(() => {
              uni.switchTab({ url: '/pages/secondhand/mylist' });
            }, 800);
          } else {
            uni.showModal({
              title: '🎉 发布成功',
              content: '您的商品已成功发布到二手市场！是否查看我的发布？',
              confirmText: '查看',
              cancelText: '继续发布',
              success: function(modalRes) {
                that.resetForm();
                if (modalRes.confirm) {
                  uni.navigateTo({ url: '/pages/secondhand/mylist' });
                }
              }
            });
          }
        } else {
          uni.showToast({
            title: res?.message || (that.isEditMode ? '保存失败' : '发布失败'),
            icon: 'none',
            duration: 3000
          });
        }
      } catch (error) {
        that.publishLoading = false;
        console.error('失败详情:', error);
        uni.showToast({
          title: error.message || error.msg || (that.isEditMode ? '保存失败，请重试' : '发布失败，请检查网络'),
          icon: 'none'
        });
      }
    },
    
    uploadImage(imagePath) {
      const that = this;
      return new Promise(function(resolve, reject) {
        // 优先从store获取token，如果没有则从缓存获取
        const token = that.$store.state.app.token || uni.getStorageSync('LOGIN_STATUS');
        
        if (!token) {
          reject(new Error('请先登录'));
          return;
        }
        
        uni.uploadFile({
          url: HTTP_REQUEST_URL + '/api/front/upload/image',
          filePath: imagePath,
          name: 'multipart',
          header: { 'Authori-zation': token },
          formData: { model: 'product', pid: 1 },
          success: function(res) {
            const data = JSON.parse(res.data);
            if (data.code === 200) {
              resolve(data.data.url);
            } else {
              reject(new Error(data.message || '上传失败'));
            }
          },
          fail: function() {
            reject(new Error('上传失败'));
          }
        });
      });
    },
    
    resetForm() {
      this.uploadImages = [];
      this.aiResult = null;
      this.aiStep = 0;
      this.formData.title = '';
      this.formData.category = '';
      this.formData.condition = '95新';
      this.formData.desc = '';
      this.formData.originalPrice = null;
      this.formData.price = null;
      this.formData.location = '同城面交';
      this.formData.detailAddress = '';
      this.formData.isPost = false;
      this.provinceIdx = -1;
      this.cityIdx = -1;
      this.areaIdx = -1;
      this.cityList = [];
      this.areaList = [];
      this.categoryIndex = 0;
      this.conditionIndex = 1;
      this.suggestedPrice = 0;
      this.conditionRatio = 0.7;
      this.categoryFactor = 0.7;
    }
  }
};
</script>

<style scoped>
/* 极致高定质感背景，采用符合 Apple 全天候护眼的温暖淡色调 */
.container {
  min-height: 100vh;
  background-color: #F8F9FA;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #1A1A1A;
}

/* 顶部品牌定位区 */
.nav-bar {
  background-color: #FFFFFF;
  padding: 30rpx 40rpx;
  border-bottom: 1rpx solid #EAEAEA;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.02);
}
.nav-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #111111;
  letter-spacing: -0.5rpx;
}
.nav-subtitle {
  font-size: 24rpx;
  color: #888888;
  margin-top: 6rpx;
}

/* 滚动区域 */
.scroll-content {
  flex: 1;
  padding: 24rpx;
  padding-bottom: 200rpx;
  box-sizing: border-box;
}

/* 卡片统一范式 */
.card {
  background-color: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.015);
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}
.dot {
  width: 10rpx;
  height: 28rpx;
  background-color: #FF6B00;
  border-radius: 4rpx;
  margin-right: 16rpx;
}
.dot.blue { background-color: #007AFF; }
.dot.orange { background-color: #FF9500; }
.dot.green { background-color: #34C759; }

.section-title .text {
  font-size: 28rpx;
  font-weight: 600;
  color: #2F3E46;
}
.section-title .tip {
  font-size: 22rpx;
  color: #999999;
  margin-left: 10rpx;
}

/* 一、图片媒体上传格栅 */
.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.image-preview-wrapper {
  position: relative;
  width: calc(33.33% - 14rpx);
  height: 200rpx;
  border-radius: 12rpx;
  overflow: hidden;
  border: 1rpx solid #EAEAEA;
}
.preview-img {
  width: 100%;
  height: 100%;
}
.delete-btn {
  position: absolute;
  top: 6rpx;
  right: 6rpx;
  width: 36rpx;
  height: 36rpx;
  border-radius: 18rpx;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}
.delete-icon {
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: bold;
}
.cover-tag {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: rgba(255, 107, 0, 0.85);
  color: #FFFFFF;
  font-size: 18rpx;
  text-align: center;
  padding: 4rpx 0;
  font-weight: 500;
}

.upload-trigger {
  width: calc(33.33% - 14rpx);
  height: 200rpx;
  border-radius: 12rpx;
  border: 2rpx dashed #CCCCCC;
  background-color: #FAFBFB;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.trigger-icon {
  font-size: 50rpx;
  color: #888888;
  margin-bottom: 10rpx;
}
.trigger-text {
  font-size: 22rpx;
  color: #666666;
}
.trigger-sub {
  font-size: 18rpx;
  color: #999999;
  margin-top: 6rpx;
}

/* 二、AI诊断报告中心 - 增强版 */
.ai-hub {
  border: 2rpx solid #EAEEFB;
  background: linear-gradient(180deg, #F9FBFF 0%, #FFFFFF 100%);
}
.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 1px dashed #E5EBF5;
  margin-bottom: 20rpx;
}
.ai-sparkle {
  color: #007AFF;
  margin-right: 8rpx;
  font-size: 30rpx;
}
.ai-title-text {
  font-weight: 700;
  font-size: 26rpx;
  color: #007AFF;
}
.ai-badge {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-weight: 600;
}
.ai-badge.loading {
  background-color: #E2ECFF;
  color: #0066FF;
}
.ai-badge.done {
  background-color: #E4F9EA;
  color: #15BF56;
}

/* 分步进度条 */
.ai-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 10rpx;
  margin-bottom: 16rpx;
}
.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.step-dot {
  width: 40rpx;
  height: 40rpx;
  border-radius: 20rpx;
  background-color: #E8E8E8;
  color: #999;
  font-size: 20rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8rpx;
  transition: all 0.3s;
}
.progress-step.active .step-dot {
  background-color: #007AFF;
  color: #fff;
  animation: step-pulse 1s infinite;
}
.progress-step.done .step-dot {
  background-color: #34C759;
  color: #fff;
  animation: none;
}
@keyframes step-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
.step-label {
  font-size: 18rpx;
  color: #999;
}
.progress-step.active .step-label {
  color: #007AFF;
  font-weight: 600;
}
.progress-line {
  width: 40rpx;
  height: 4rpx;
  background-color: #E8E8E8;
  margin-bottom: 20rpx;
  transition: background 0.3s;
}
.progress-line.active {
  background-color: #007AFF;
}

/* AI 加载动效 */
.ai-loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 0rpx;
}
.scan-line {
  width: 80%;
  height: 4rpx;
  background: linear-gradient(90deg, transparent, #007AFF, transparent);
  border-radius: 2rpx;
  animation: scan 1.5s infinite linear;
  margin-bottom: 20rpx;
}
@keyframes scan {
  0% { transform: translateX(-100%); opacity: 0; }
  50% { opacity: 1; }
  100% { transform: translateX(100%); opacity: 0; }
}
.ai-loading-tips {
  font-size: 24rpx;
  color: #007AFF;
  font-weight: bold;
}
.ai-loading-sub {
  font-size: 20rpx;
  color: #999999;
  margin-top: 8rpx;
}

/* 报告显示 - 增强版 */
.ai-report {
  font-size: 26rpx;
  animation: fadeSlideUp 0.4s ease-out;
}
@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(20rpx); }
  to { opacity: 1; transform: translateY(0); }
}
.report-card {
  background: #F8FAFF;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}
.report-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.report-row:last-child {
  margin-bottom: 0;
}
.report-lbl {
  color: #666666;
  width: 140rpx;
  font-size: 24rpx;
}
.report-val {
  color: #111111;
  font-weight: 600;
  flex: 1;
}
.report-val.highlight {
  color: #007AFF;
  font-size: 30rpx;
}
.report-val.desc {
  color: #888888;
  font-weight: normal;
  font-size: 22rpx;
}
.badge-cat {
  background-color: #F0F4FC;
  color: #007AFF;
  font-size: 22rpx;
  font-weight: bold;
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
}

/* 置信度进度条 */
.confidence-bar {
  flex: 1;
  height: 32rpx;
  background: #E8E8E8;
  border-radius: 16rpx;
  position: relative;
  overflow: hidden;
}
.confidence-fill {
  height: 100%;
  background: linear-gradient(90deg, #007AFF, #5AC8FA);
  border-radius: 16rpx;
  transition: width 0.6s ease-out;
}
.confidence-text {
  position: absolute;
  right: 12rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20rpx;
  font-weight: bold;
  color: #333;
}

/* AI 估价仪表盘 */
.ai-gauge-section {
  background: linear-gradient(135deg, #FFF8F0 0%, #FFF2E6 100%);
  border: 1rpx solid #FFD6A5;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.gauge-title {
  font-size: 24rpx;
  font-weight: bold;
  color: #D44B00;
  margin-bottom: 20rpx;
  text-align: center;
}
.gauge-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.gauge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.gauge-item.primary {
  background: #FF6B00;
  border-radius: 12rpx;
  padding: 16rpx 12rpx;
  margin: 0 12rpx;
}
.gauge-label {
  font-size: 20rpx;
  color: #999;
  margin-bottom: 8rpx;
}
.gauge-item.primary .gauge-label {
  color: rgba(255,255,255,0.8);
}
.gauge-value {
  font-size: 32rpx;
  font-weight: 800;
}
.gauge-value.gray { color: #666; }
.gauge-value.orange { color: #FFFFFF; font-size: 36rpx; }
.gauge-value.green { color: #34C759; }
.gauge-arrow {
  font-size: 28rpx;
  color: #FF9500;
  margin: 0 4rpx;
}

/* 折旧因子可视化 */
.factor-section {
  background: #F8FAFF;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}
.factor-title {
  font-size: 22rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 16rpx;
}
.factor-bar-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.factor-label {
  font-size: 22rpx;
  color: #666;
  width: 140rpx;
}
.factor-bar-bg {
  flex: 1;
  height: 20rpx;
  background: #E8E8E8;
  border-radius: 10rpx;
  overflow: hidden;
  margin: 0 12rpx;
}
.factor-bar-fill {
  height: 100%;
  border-radius: 10rpx;
  transition: width 0.5s ease-out;
}
.factor-bar-fill.condition {
  background: linear-gradient(90deg, #FF9500, #FF6B00);
}
.factor-bar-fill.category {
  background: linear-gradient(90deg, #5AC8FA, #007AFF);
}
.factor-val {
  font-size: 22rpx;
  font-weight: bold;
  color: #333;
  width: 70rpx;
  text-align: right;
}
.factor-formula {
  background: #FFF;
  border-radius: 8rpx;
  padding: 12rpx 16rpx;
  margin-top: 8rpx;
}
.formula-text {
  font-size: 20rpx;
  color: #666;
  font-family: monospace;
}

.ai-alert {
  background-color: #FFF9E6;
  border-radius: 12rpx;
  padding: 16rpx;
  display: flex;
  margin-top: 10rpx;
}
.alert-icon {
  margin-right: 12rpx;
  font-size: 26rpx;
}
.alert-txt {
  font-size: 20rpx;
  color: #B27000;
  line-height: 28rpx;
}

/* 三、详情主表单 */
.form-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  font-size: 26rpx;
}
.border-bottom {
  border-bottom: 1rpx solid #F3F3F3;
}
.form-label {
  width: 150rpx;
  color: #333333;
  font-weight: 600;
}
.form-input {
  flex: 1;
  font-size: 26rpx;
  color: #111111;
}
.form-picker {
  flex: 1;
}
.picker-value {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 26rpx;
  color: #111111;
}
.arrow {
  font-size: 16rpx;
  color: #BBBBBB;
}
.ph-color {
  color: #BBBBBB;
}

.form-textarea-item {
  padding-top: 24rpx;
  font-size: 26rpx;
  position: relative;
}
.form-textarea {
  width: 100%;
  height: 180rpx;
  border: 1rpx solid #EFEFEF;
  background-color: #FAFAFA;
  border-radius: 8rpx;
  padding: 16rpx;
  box-sizing: border-box;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: #222222;
  line-height: 32rpx;
}
.ph {
  color: #BBBBBB;
}
.word-limit {
  position: absolute;
  right: 14rpx;
  bottom: 10rpx;
  font-size: 18rpx;
  color: #999999;
}

/* 四、计价多功能计算板 */
.price-grid {
  display: flex;
  gap: 24rpx;
  margin-bottom: 24rpx;
}
.price-box {
  flex: 1;
  border: 1rpx solid #EAEAEA;
  border-radius: 12rpx;
  padding: 20rpx;
}
.price-box.active {
  border-color: rgba(255, 107, 0, 0.4);
  background-color: #FFFBF7;
}
.price-lbl {
  font-size: 22rpx;
  color: #666666;
  display: block;
}
.price-input {
  font-size: 34rpx;
  font-weight: 700;
  margin-top: 10rpx;
  width: 100%;
  color: #111111;
}
.orange-txt {
  color: #FF6B00;
}

.ai-pricing-meter {
  background-color: #FFFBF2;
  border: 1rpx solid #FFEDCC;
  border-radius: 16rpx;
  padding: 20rpx;
}
.meter-header {
  display: flex;
  align-items: center;
  margin-bottom: 10rpx;
}
.meter-title {
  font-size: 22rpx;
  font-weight: bold;
  color: #B25900;
  margin-left: 8rpx;
}
.meter-val-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.meter-descr {
  font-size: 20rpx;
  color: #7A5328;
  line-height: 28rpx;
  flex: 1;
  padding-right: 15rpx;
}
.meter-descr .bold {
  font-weight: bold;
  color: #D44B00;
}
.meter-price {
  background-color: #FF6B00;
  color: #FFFFFF;
  border-radius: 10rpx;
  padding: 10rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.meter-price .currency {
  font-size: 16rpx;
  font-weight: bold;
}
.meter-price .number {
  font-size: 36rpx;
  font-weight: 800;
  line-height: 38rpx;
}
.apply-badge {
  font-size: 14rpx;
  font-weight: bold;
  background-color: rgba(255, 255, 255, 0.2);
  width: 100%;
  text-align: center;
  padding: 2rpx 0rpx;
  border-radius: 4rpx;
  margin-top: 4rpx;
}

/* 底部发布按钮 */
.submit-footer {
  margin-top: 40rpx;
  padding: 30rpx 0;
  padding-bottom: 60rpx;
}
.submit-button {
  background: linear-gradient(135deg, #FF6B00 0%, #FF3D00 100%);
  color: #FFFFFF;
  font-size: 32rpx;
  font-weight: bold;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 48rpx;
  border: none;
  box-shadow: 0 8rpx 20rpx rgba(255, 107, 0, 0.4);
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.region-pickers {
  flex: 1;
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #111111;
  overflow: hidden;
}
.region-pickers picker {
  flex: 1;
  min-width: 0;
  text-align: center;
}
.region-sep {
  color: #CCCCCC;
  margin: 0 4rpx;
  font-size: 22rpx;
  flex-shrink: 0;
}
.region-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
}
.region-panel {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  background: #fff;
  z-index: 1000;
  border-radius: 24rpx 24rpx 0 0;
  overflow: hidden;
}
.region-panel-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #F0F0F0;
  font-size: 30rpx;
  color: #333;
}
.region-panel-ok {
  color: #FF6B00;
  font-weight: 600;
}
.region-panel-bd {
  width: 100%;
  height: 480rpx;
}
.region-panel-bd picker-view {
  width: 100%;
  height: 100%;
}
.region-col-item {
  line-height: 88rpx;
  text-align: center;
  font-size: 28rpx;
  color: #333;
}
</style>
