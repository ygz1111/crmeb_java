<template>
    <view class="page" :data-theme="theme">
        <view class="mylist-container">
            <view class="header-bar">
                <text class="header-title">我的发布</text>
                <text class="header-count">共 {{ totalCount }} 件</text>
            </view>

            <!-- 优化空状态展示 -->
            <view v-if="list.length === 0 && !loading" class="empty-state">
                <view class="empty-icon-box">
                    <text class="empty-emoji">📦</text>
                </view>
                <text class="empty-text">还没有发布任何商品</text>
                <text class="empty-tip">快去发布你的闲置宝贝吧~</text>
                <button class="go-publish-btn" @click="goPublish">
                    <text class="btn-icon">+</text>
                    <text>立即发布</text>
                </button>
            </view>

            <!-- 商品列表 -->
            <view v-for="(item, index) in list" :key="index" class="product-card" @click="goDetail(item)">
                <view class="card-left">
                    <image :src="item.image" class="product-img" mode="aspectFill"></image>
                    <!-- 商品状态标识 -->
                    <view class="status-badge" :class="getStatusClass(item)">
                        {{ getStatusText(item) }}
                    </view>
                </view>
                <view class="card-right">
                    <view class="product-name">{{ item.storeName }}</view>
                    <view class="product-info">
                        <text class="tag" v-if="item.aiCategory">{{ item.aiCategory }}</text>
                        <text class="tag condition" v-if="item.itemCondition">{{ item.itemCondition }}</text>
                        <text class="tag location" v-if="item.location">{{ item.location }}</text>
                    </view>
                    <view class="product-desc" v-if="item.storeInfo">{{ item.storeInfo }}</view>
                    <view class="product-bottom">
                        <view class="price-row">
                            <text class="price">¥{{ item.price }}</text>
                            <text class="ot-price" v-if="item.otPrice">原价¥{{ item.otPrice }}</text>
                            <text class="ai-price" v-if="item.aiPredictedPrice">AI估价:¥{{ item.aiPredictedPrice }}</text>
                        </view>
                        <view class="action-row">
                            <text class="action-btn edit" @click.stop="handleEdit(item)">编辑</text>
                            <text class="action-btn delete" @click.stop="handleDelete(item, index)">删除</text>
                        </view>
                    </view>
                </view>
            </view>

            <view v-if="loading" class="loading-more">
                <text class="loading-icon">⟳</text>
                <text>加载中...</text>
            </view>
            <view v-if="noMore && list.length > 0" class="loading-more">— 我是有底线的 —</view>
        </view>

        <view class="bottom-btn-area">
            <button class="publish-fab" @click="goPublish">
                <text class="fab-icon">+</text>
            </button>
        </view>
    </view>
</template>

<script>
import { getMySecondHandList, deleteSecondHand } from '@/api/secondhand.js';
import { mapGetters } from 'vuex';
const app = getApp();

export default {
    data() {
        return {
            theme: app.globalData.theme,
            list: [],
            page: 1,
            limit: 10,
            totalCount: 0,
            loading: false,
            noMore: false,
            refreshing: false,
        }
    },
    computed: mapGetters(['isLogin', 'uid']),
    onShow() {
        if (this.isLogin) {
            this.page = 1;
            this.list = [];
            this.noMore = false;
            this.loadList();
        }
    },
    // 下拉刷新
    onPullDownRefresh() {
        this.refreshData();
    },
    methods: {
        loadList() {
            let that = this;
            if (that.loading || that.noMore) return;
            that.loading = true;
            getMySecondHandList(that.page, that.limit).then(res => {
                that.loading = false;
                if (res.data && res.data.list) {
                    that.list = that.list.concat(res.data.list);
                    that.totalCount = res.data.total || 0;
                    if (res.data.list.length < that.limit) {
                        that.noMore = true;
                    } else {
                        that.page++;
                    }
                }
            }).catch(() => {
                that.loading = false;
                uni.showToast({ title: '加载失败', icon: 'none' });
            });
        },
        // 下拉刷新数据
        refreshData() {
            let that = this;
            that.page = 1;
            that.list = [];
            that.noMore = false;
            that.loading = true;
            
            getMySecondHandList(1, that.limit).then(res => {
                that.loading = false;
                that.refreshing = false;
                uni.stopPullDownRefresh();
                
                if (res.data && res.data.list) {
                    that.list = res.data.list;
                    that.totalCount = res.data.total || 0;
                    if (res.data.list.length < that.limit) {
                        that.noMore = true;
                    } else {
                        that.page = 2;
                    }
                    uni.showToast({ title: '刷新成功', icon: 'success', duration: 1500 });
                }
            }).catch(() => {
                that.loading = false;
                that.refreshing = false;
                uni.stopPullDownRefresh();
                uni.showToast({ title: '刷新失败', icon: 'none' });
            });
        },
        goPublish() {
            uni.switchTab({ url: '/pages/secondhand/publish' });
        },
        goDetail(item) {
            const info = [
                '商品名称：' + (item.storeName || ''),
                '商品分类：' + (item.category || item.aiCategory || '未分类'),
                '商品成色：' + (item.itemCondition || '未设定'),
                '出售价格：¥' + (item.price || 0),
                '参考原价：¥' + (item.otPrice || '未填写'),
                '面交地点：' + (item.location || '未设定'),
                'AI估价：¥' + (item.aiPredictedPrice || '无'),
                'AI置信度：' + (item.aiConfidence || '无'),
                '商品描述：' + (item.storeInfo || '暂无描述'),
            ].join('\n');
            const that = this;
            uni.showModal({
                title: '商品详情',
                content: info,
                showCancel: true,
                confirmText: '编辑',
                cancelText: '关闭',
                success: (res) => {
                    if (res.confirm) {
                        that.handleEdit(item);
                    }
                }
            });
        },
        // 编辑商品
        handleEdit(item) {
            const editData = encodeURIComponent(JSON.stringify({
                id: item.id,
                storeName: item.storeName,
                storeInfo: item.storeInfo,
                category: item.category || item.aiCategory || '',
                itemCondition: item.itemCondition || '',
                price: item.price,
                otPrice: item.otPrice,
                image: item.image,
                location: item.location || ''
            }));
            uni.navigateTo({ url: '/pages/secondhand/publish?editData=' + editData });
        },
        // 删除商品
        handleDelete(item, index) {
            let that = this;
            uni.showModal({
                title: '确认删除',
                content: '删除后将无法恢复，确定要删除该商品吗？',
                confirmText: '删除',
                confirmColor: '#fc4141',
                success: function (res) {
                    if (res.confirm) {
                        uni.showLoading({ title: '删除中...' });
                        deleteSecondHand(item.id).then(r => {
                            uni.hideLoading();
                            if (r.data || r.code === 200) {
                                that.list.splice(index, 1);
                                that.totalCount--;
                                uni.showToast({ title: '删除成功', icon: 'success' });
                            } else {
                                uni.showToast({ title: r.message || '删除失败', icon: 'none' });
                            }
                        }).catch((err) => {
                            uni.hideLoading();
                            uni.showToast({ title: '删除失败，请重试', icon: 'none' });
                        });
                    }
                }
            });
        },
        // 获取商品状态样式
        getStatusClass(item) {
            if (!item.isShow) return 'off-shelf';
            if (item.stock <= 0) return 'sold-out';
            return 'on-sale';
        },
        // 获取商品状态文字
        getStatusText(item) {
            if (!item.isShow) return '已下架';
            if (item.stock <= 0) return '已售罄';
            return '在售中';
        },
    },
    onReachBottom() {
        this.loadList();
    }
}
</script>

<style scoped>
.mylist-container { padding: 20rpx 24rpx; padding-bottom: 160rpx; }
.header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; padding: 0 10rpx; }
.header-title { font-size: 36rpx; font-weight: 700; color: #333; }
.header-count { font-size: 24rpx; color: #999; background: #f5f5f5; padding: 6rpx 16rpx; border-radius: 20rpx; }

/* 空状态优化 */
.empty-state { 
    display: flex; 
    flex-direction: column; 
    align-items: center; 
    padding-top: 100rpx;
    padding-bottom: 60rpx;
}
.empty-icon-box {
    width: 200rpx;
    height: 200rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #FFF5F0 0%, #FFE8E0 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 30rpx;
}
.empty-emoji {
    font-size: 100rpx;
    opacity: 0.8;
}
.empty-text { 
    font-size: 32rpx; 
    color: #666; 
    margin-bottom: 16rpx;
    font-weight: 500;
}
.empty-tip { 
    font-size: 26rpx; 
    color: #999; 
    margin-bottom: 40rpx;
}
.go-publish-btn { 
    background: linear-gradient(135deg, #FF6B35 0%, #FF4444 100%);
    color: #fff; 
    border-radius: 50rpx; 
    font-size: 30rpx;
    padding: 0 50rpx;
    height: 80rpx;
    line-height: 80rpx;
    border: none;
    box-shadow: 0 8rpx 20rpx rgba(255, 107, 53, 0.3);
    display: flex;
    align-items: center;
    gap: 10rpx;
}
.btn-icon { font-size: 36rpx; font-weight: bold; }

/* 商品卡片 */
.product-card { 
    display: flex; 
    background: #fff; 
    border-radius: 16rpx; 
    padding: 20rpx; 
    margin-bottom: 20rpx; 
    box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
    transition: all 0.3s;
}
.product-card:active { transform: scale(0.98); }

.card-left { 
    width: 180rpx; 
    height: 180rpx; 
    border-radius: 12rpx; 
    overflow: hidden; 
    flex-shrink: 0;
    position: relative;
}
.product-img { width: 100%; height: 100%; }

/* 商品状态标识 */
.status-badge {
    position: absolute;
    top: 8rpx;
    left: 8rpx;
    font-size: 20rpx;
    padding: 4rpx 12rpx;
    border-radius: 6rpx;
    font-weight: 500;
}
.status-badge.on-sale {
    background: rgba(76, 175, 80, 0.9);
    color: #fff;
}
.status-badge.sold-out {
    background: rgba(158, 158, 158, 0.9);
    color: #fff;
}
.status-badge.off-shelf {
    background: rgba(255, 152, 0, 0.9);
    color: #fff;
}

.card-right { 
    flex: 1; 
    margin-left: 20rpx; 
    display: flex; 
    flex-direction: column; 
    justify-content: space-between;
}
.product-name { 
    font-size: 28rpx; 
    color: #333; 
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.4;
}
.product-info { display: flex; gap: 12rpx; margin-top: 10rpx; flex-wrap: wrap; }
.tag { 
    font-size: 20rpx; 
    color: #007aff; 
    background: #e8f4fd; 
    padding: 4rpx 12rpx; 
    border-radius: 6rpx;
}
.tag.condition { color: #ff9500; background: #fff3e0; }
.tag.location { color: #34c759; background: #e8f5e9; }

.product-desc {
    font-size: 22rpx;
    color: #888;
    line-height: 1.4;
    margin-top: 8rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.product-bottom { 
    display: flex; 
    justify-content: space-between; 
    align-items: center;
    margin-top: 10rpx;
}
.price-row { display: flex; align-items: baseline; gap: 12rpx; }
.price { font-size: 36rpx; color: #fc4141; font-weight: 700; }
.ot-price { font-size: 22rpx; color: #bbb; text-decoration: line-through; }
.ai-price { font-size: 22rpx; color: #999; }

.action-row { 
    display: flex;
    gap: 12rpx;
}
.action-btn { 
    font-size: 24rpx; 
    padding: 8rpx 20rpx; 
    border-radius: 8rpx;
    transition: all 0.2s;
}
.action-btn.edit { 
    color: #007aff;
    background: #e8f4fd;
    border: 1rpx solid #007aff;
}
.action-btn.delete { 
    color: #999;
    background: #f5f5f5;
    border: 1rpx solid #ddd;
}
.action-btn:active { opacity: 0.7; }

/* 加载状态 */
.loading-more { 
    text-align: center; 
    padding: 30rpx; 
    font-size: 24rpx; 
    color: #999;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10rpx;
}
.loading-icon {
    display: inline-block;
    animation: rotate 1s linear infinite;
}
@keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

/* 底部发布按钮 */
.bottom-btn-area { 
    position: fixed; 
    bottom: 40rpx; 
    right: 40rpx; 
    z-index: 99;
}
.publish-fab { 
    width: 120rpx; 
    height: 120rpx; 
    background: linear-gradient(135deg, #FF6B35 0%, #FF4444 100%);
    color: #fff; 
    border-radius: 60rpx;
    font-size: 28rpx;
    font-weight: 500;
    box-shadow: 0 8rpx 24rpx rgba(255, 107, 53, 0.5);
    border: none;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}
.fab-icon { font-size: 60rpx; font-weight: 300; }
</style>