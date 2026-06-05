<template>
    <view class="page" :data-theme="theme">
        <view class="mylist-container">
            <view class="header-bar">
                <text class="header-title">我的发布</text>
                <text class="header-count">共 {{ totalCount }} 件</text>
            </view>

            <view v-if="list.length === 0 && !loading" class="empty-state">
                <text class="empty-text">暂无发布</text>
                <button class="go-publish-btn" @click="goPublish">去发布</button>
            </view>

            <view v-for="(item, index) in list" :key="index" class="product-card" @click="goDetail(item)">
                <view class="card-left">
                    <image :src="item.image" class="product-img" mode="aspectFill"></image>
                </view>
                <view class="card-right">
                    <view class="product-name">{{ item.storeName }}</view>
                    <view class="product-info">
                        <text class="tag" v-if="item.aiCategory">{{ item.aiCategory }}</text>
                        <text class="tag condition" v-if="item.itemCondition">{{ item.itemCondition }}</text>
                    </view>
                    <view class="product-bottom">
                        <view class="price-row">
                            <text class="price">¥{{ item.price }}</text>
                            <text class="ai-price" v-if="item.aiPredictedPrice">AI估价: ¥{{ item.aiPredictedPrice }}</text>
                        </view>
                        <view class="action-row">
                            <text class="action-btn delete" @click.stop="handleDelete(item, index)">删除</text>
                        </view>
                    </view>
                </view>
            </view>

            <view v-if="loading" class="loading-more">加载中...</view>
            <view v-if="noMore && list.length > 0" class="loading-more">没有更多了</view>
        </view>

        <view class="bottom-btn-area">
            <button class="publish-fab" @click="goPublish">+ 发布</button>
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
            });
        },
        goPublish() {
            uni.switchTab({ url: '/pages/secondhand/publish' });
        },
        goDetail(item) {
            uni.navigateTo({ url: '/pages/goods/goods_details/index?id=' + item.id });
        },
        handleDelete(item, index) {
            let that = this;
            uni.showModal({
                title: '确认删除',
                content: '确定要删除该商品吗？',
                success: function (res) {
                    if (res.confirm) {
                        deleteSecondHand(item.id).then(r => {
                            if (r.data) {
                                that.list.splice(index, 1);
                                that.totalCount--;
                                that.$util.Tips({ title: '已删除' });
                            }
                        }).catch(() => {
                            that.$util.Tips({ title: '删除失败' });
                        });
                    }
                }
            });
        },
    },
    onReachBottom() {
        this.loadList();
    }
}
</script>

<style scoped>
.mylist-container { padding: 20rpx 24rpx; padding-bottom: 120rpx; }
.header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.header-title { font-size: 34rpx; font-weight: 600; color: #333; }
.header-count { font-size: 24rpx; color: #999; }
.empty-state { display: flex; flex-direction: column; align-items: center; padding-top: 120rpx; }
.empty-text { font-size: 28rpx; color: #999; margin-bottom: 30rpx; }
.go-publish-btn { background: #fc4141; color: #fff; border-radius: 40rpx; font-size: 28rpx; padding: 0 60rpx; height: 72rpx; line-height: 72rpx; border: none; }
.product-card { display: flex; background: #fff; border-radius: 16rpx; padding: 20rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06); }
.card-left { width: 180rpx; height: 180rpx; border-radius: 12rpx; overflow: hidden; flex-shrink: 0; }
.product-img { width: 100%; height: 100%; }
.card-right { flex: 1; margin-left: 20rpx; display: flex; flex-direction: column; justify-content: space-between; }
.product-name { font-size: 28rpx; color: #333; font-weight: 500; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.product-info { display: flex; gap: 12rpx; margin-top: 8rpx; }
.tag { font-size: 20rpx; color: #007aff; background: #e8f4fd; padding: 4rpx 12rpx; border-radius: 6rpx; }
.tag.condition { color: #ff9500; background: #fff3e0; }
.product-bottom { display: flex; justify-content: space-between; align-items: center; }
.price-row { display: flex; align-items: baseline; gap: 12rpx; }
.price { font-size: 34rpx; color: #fc4141; font-weight: 600; }
.ai-price { font-size: 22rpx; color: #999; }
.action-btn { font-size: 24rpx; padding: 6rpx 16rpx; border-radius: 6rpx; }
.action-btn.delete { color: #999; border: 1rpx solid #ddd; }
.loading-more { text-align: center; padding: 20rpx; font-size: 24rpx; color: #999; }
.bottom-btn-area { position: fixed; bottom: 40rpx; right: 40rpx; z-index: 99; }
.publish-fab { width: 120rpx; height: 120rpx; background: #fc4141; color: #fff; border-radius: 60rpx; font-size: 28rpx; font-weight: 500; box-shadow: 0 8rpx 24rpx rgba(252,65,65,0.4); border: none; line-height: 120rpx; padding: 0; }
</style>