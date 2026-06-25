<template>
    <view class="page" :data-theme="theme" :style="{height:winHeight+'px'}">
        <view class="publish-container">

            <!-- 图片上传 -->
            <view class="section">
                <view class="section-title">商品图片</view>
                <view class="upload-area" @click="uploadImage">
                    <image v-if="mainImage" :src="mainImage" class="main-img" mode="aspectFill"></image>
                    <view v-else class="upload-placeholder">
                        <view class="cam-icon">&#x1F4F7;</view>
                        <text class="upload-text">点击拍照或从相册选择</text>
                    </view>
                </view>
            </view>

            <!-- 基本信息 -->
            <view class="section">
                <view class="section-title">基本信息</view>
                <input class="form-input" v-model="formData.storeName" placeholder="商品名称（例如：iPhone 14 128G）" maxlength="50" />
                <textarea class="form-textarea" v-model="formData.storeInfo" placeholder="描述商品状况、品牌、配件等..." maxlength="500" />
            </view>

            <!-- 成色与价格 -->
            <view class="section">
                <view class="section-title">成色与价格</view>
                <view class="row-2">
                    <view class="half">
                        <view class="mini-label">成色</view>
                        <picker :range="conditionOptions" @change="onConditionChange">
                            <view class="picker-box">{{ conditionOptions[conditionIndex] || '请选择' }}</view>
                        </picker>
                    </view>
                    <view class="half">
                        <view class="mini-label">购入价格</view>
                        <view class="price-box">
                            <text class="rmb">¥</text>
                            <input class="price-inp" type="digit" v-model="formData.otPrice" placeholder="0.00" />
                        </view>
                    </view>
                </view>
                <view class="row-2">
                    <view class="half">
                        <view class="mini-label">售价</view>
                        <view class="price-box">
                            <text class="rmb">¥</text>
                            <input class="price-inp" type="digit" v-model="formData.price" placeholder="0.00" />
                        </view>
                    </view>
                    <view class="half">
                        <view class="mini-label">库存</view>
                        <input class="form-input small" type="number" v-model="formData.stock" placeholder="1" />
                    </view>
                </view>
            </view>

            <!-- AI 结果 -->
            <view class="section ai-section" v-if="aiResult">
                <view class="section-title">
                    AI 分析结果
                    <text class="ai-tag">AI</text>
                </view>
                <view class="ai-grid">
                    <view class="ai-cell">
                        <text class="ai-num">1</text>
                        <text class="ai-word">识别分类</text>
                        <text class="ai-val">{{ aiResult.category }}</text>
                    </view>
                    <view class="ai-cell" v-if="aiResult.predictedPrice">
                        <text class="ai-num">2</text>
                        <text class="ai-word">建议售价</text>
                        <text class="ai-val red">¥{{ aiResult.predictedPrice }}</text>
                    </view>
                </view>
            </view>

            <!-- 发布按钮 -->
            <button class="submit-btn" :disabled="submitting" @click="submitPublish">
                {{ submitting ? '发布中...' : '立即发布' }}
            </button>
        </view>
    </view>
</template>

<script>
import { publishSecondHand } from '@/api/secondhand.js';
import { mapGetters } from 'vuex';
const app = getApp();

export default {
    data() {
        return {
            theme: app.globalData.theme,
            winHeight: '',
            mainImage: '',
            formData: {
                storeName: '', storeInfo: '', otPrice: '', price: '',
                stock: '1', itemCondition: '', image: '', sliderImage: '', content: '',
            },
            conditionOptions: ['几乎全新', '轻微使用痕迹', '明显使用痕迹', '老旧/有瑕疵'],
            conditionIndex: 0,
            aiResult: null,
            submitting: false,
        }
    },
    computed: mapGetters(['isLogin', 'uid']),
    onLoad() {
        let that = this;
        uni.getSystemInfo({ success: function (res) { that.winHeight = res.windowHeight; } });
        this.checkLoginStatus();
    },
    onShow() {
        this.checkLoginStatus();
    },
    methods: {
        checkLoginStatus() {
            const token = this.$store.state.app.token;
            if (!token) {
                this.$util.Tips({ title: '请先登录' }, () => { uni.redirectTo({ url: '/pages/users/login/index' }); });
                return false;
            }
            return true;
        },
        uploadImage() {
            let that = this;
            if (!this.checkLoginStatus()) return;
            uni.chooseImage({
                count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
                success: function (res) {
                    that.mainImage = res.tempFilePaths[0];
                    uni.showLoading({ title: '上传中...' });
                    const token = that.$store.state.app.token;
                    uni.uploadFile({
                        url: 'http://localhost:20510/api/front/upload/image',
                        filePath: res.tempFilePaths[0],
                        name: 'multipart',
                        header: { 'Authori-zation': token },
                        formData: { model: 'product', pid: 1 },
                        success: function (uploadRes) {
                            uni.hideLoading();
                            let data = JSON.parse(uploadRes.data);
                            if (data.code === 200) {
                                that.formData.image = data.data.url;
                                that.$util.Tips({ title: '上传成功' });
                            } else {
                                that.$util.Tips({ title: data.message || '上传失败' });
                            }
                        },
                        fail: function () { uni.hideLoading(); that.$util.Tips({ title: '上传失败' }); }
                    });
                }
            });
        },
        onConditionChange(e) {
            this.conditionIndex = e.detail.value;
            this.formData.itemCondition = this.conditionOptions[e.detail.value];
        },
        submitPublish() {
            let that = this;
            if (!this.checkLoginStatus()) return;
            if (!that.formData.image) return that.$util.Tips({ title: '请上传商品图片' });
            if (!that.formData.storeName.trim()) return that.$util.Tips({ title: '请输入商品名称' });
            if (!that.formData.price || parseFloat(that.formData.price) <= 0) return that.$util.Tips({ title: '请输入有效价格' });
            if (!that.formData.itemCondition) that.formData.itemCondition = that.conditionOptions[0];
            that.submitting = true;
            uni.showLoading({ title: 'AI 分析中...' });
            publishSecondHand(that.formData).then(res => {
                uni.hideLoading(); that.submitting = false;
                console.log('publish response:', JSON.stringify(res));
                if (res && res.code === 200 && res.data && res.data.success) {
                    that.aiResult = {
                        category: res.data.aiCategory || '未知',
                        confidence: res.data.aiConfidence || 0,
                        predictedPrice: res.data.aiPredictedPrice || 0,
                    };
                    uni.showModal({
                        title: '发布成功',
                        content: '商品已发布！AI 建议售价：¥' + (that.aiResult.predictedPrice || that.formData.price),
                        showCancel: false,
                        success: function () { uni.redirectTo({ url: '/pages/secondhand/mylist' }); }
                    });
                } else {
                    that.$util.Tips({ title: (res && res.message) || (res && res.data && res.data.mlError) || '发布失败' });
                }
            }).catch(err => {
                uni.hideLoading(); that.submitting = false;
                console.log('publish error:', JSON.stringify(err));
                that.$util.Tips({ title: err.message || err.msg || '网络错误，请重试' });
            });
        },
    }
}
</script>

<style scoped>
page { background: #f5f5f5; }
.publish-container { padding: 20rpx 24rpx; padding-bottom: 100rpx; }

.section {
    background: #fff; border-radius: 16rpx;
    padding: 24rpx; margin-bottom: 20rpx;
}
.section-title {
    font-size: 30rpx; font-weight: 600; color: #333;
    margin-bottom: 20rpx; display: flex; align-items: center;
}

/* 上传区 */
.upload-area {
    width: 100%; height: 360rpx; background: #fafafa;
    border-radius: 12rpx; display: flex;
    align-items: center; justify-content: center;
    overflow: hidden; border: 2rpx dashed #ddd;
}
.main-img { width: 100%; height: 100%; }
.upload-placeholder { text-align: center; }
.cam-icon { font-size: 64rpx; margin-bottom: 12rpx; }
.upload-text { font-size: 26rpx; color: #999; }

/* 输入框 */
.form-input {
    width: 100%; height: 82rpx; background: #f8f8f8;
    border-radius: 10rpx; padding: 0 20rpx; font-size: 28rpx;
    box-sizing: border-box; margin-bottom: 16rpx;
}
.form-textarea {
    width: 100%; min-height: 140rpx; background: #f8f8f8;
    border-radius: 10rpx; padding: 18rpx 20rpx; font-size: 28rpx;
    box-sizing: border-box;
}
.form-input.small { margin-bottom: 0; }

/* 双列 */
.row-2 { display: flex; gap: 20rpx; margin-bottom: 16rpx; }
.row-2:last-child { margin-bottom: 0; }
.half { flex: 1; }
.mini-label { font-size: 24rpx; color: #999; margin-bottom: 8rpx; }

.picker-box {
    height: 82rpx; background: #f8f8f8; border-radius: 10rpx;
    padding: 0 20rpx; font-size: 28rpx; line-height: 82rpx; color: #333;
}
.price-box {
    display: flex; align-items: center; height: 82rpx;
    background: #f8f8f8; border-radius: 10rpx; padding: 0 20rpx;
}
.rmb { font-size: 30rpx; color: #fc4141; font-weight: 600; margin-right: 6rpx; }
.price-inp {
    flex: 1; height: 82rpx; background: transparent;
    font-size: 28rpx; border: none;
}

/* AI */
.ai-section { background: #f0f7ff; }
.ai-tag {
    font-size: 20rpx; color: #fff; background: #3b82f6;
    padding: 2rpx 12rpx; border-radius: 10rpx; margin-left: 12rpx;
}
.ai-grid { display: flex; }
.ai-cell { flex: 1; text-align: center; }
.ai-num {
    width: 40rpx; height: 40rpx; background: #3b82f6; color: #fff;
    border-radius: 50%; display: inline-flex; align-items: center;
    justify-content: center; font-size: 22rpx; margin-bottom: 8rpx;
}
.ai-word { font-size: 22rpx; color: #999; display: block; margin-bottom: 4rpx; }
.ai-val { font-size: 26rpx; color: #333; font-weight: 600; }
.ai-val.red { color: #fc4141; font-size: 32rpx; }

/* 按钮 */
.submit-btn {
    width: 100%; height: 90rpx; background: #fc4141; color: #fff;
    border-radius: 14rpx; font-size: 32rpx; font-weight: 500;
    border: none; line-height: 90rpx; margin-top: 8rpx;
}
.submit-btn[disabled] { opacity: 0.5; }
</style>