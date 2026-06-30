<template>
  <view class="secondhand-section">
    <view class="secondhand-header">
      <view class="header-left">
        <text class="header-icon">♻</text>
        <text class="header-title">二手物品</text>
      </view>
      <view class="header-right" @click="goMore">
        <text class="more-text">更多</text>
        <text class="arrow">▶</text>
      </view>
    </view>
    <view class="secondhand-list">
      <view class="item" v-for="(item, index) in list" :key="index" @click="goDetail(item)">
        <view class="pictrue">
          <image :src="item.image" mode="aspectFill" />
          <view class="condition-tag" v-if="item.itemCondition">{{ item.itemCondition }}</view>
        </view>
        <view class="info">
          <view class="title line2">{{ item.storeName }}</view>
          <view class="price-row">
            <text class="price">¥{{ item.price }}</text>
            <text class="ot-price" v-if="item.otPrice && item.otPrice > item.price">¥{{ item.otPrice }}</text>
          </view>
        </view>
      </view>
    </view>
    <view class="empty-tip" v-if="list.length === 0 && !loading">
      <text>暂无二手物品，去发布第一个吧~</text>
    </view>
  </view>
</template>

<script>
import { getSecondHandList } from '@/api/secondhand.js';

export default {
  name: 'secondHand',
  data() {
    return {
      list: [],
      loading: false,
    };
  },
  mounted() {
    this.loadList();
  },
  methods: {
    loadList() {
      if (this.loading) return;
      this.loading = true;
      getSecondHandList(1, 6).then(res => {
        this.loading = false;
        if (res && res.data && res.data.list) {
          this.list = res.data.list;
        }
      }).catch(() => {
        this.loading = false;
      });
    },
    goDetail(item) {
      if (!item || !item.id) return;
      // 二手物品没有营销活动，直接跳转商品详情页
      uni.navigateTo({
        url: `/pages/goods/goods_details/index?id=${item.id}`
      });
    },
    goMore() {
      uni.switchTab({ url: '/pages/secondhand/mylist' });
    }
  }
};
</script>

<style scoped>
.secondhand-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

.secondhand-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-icon {
  font-size: 36rpx;
  margin-right: 10rpx;
  color: #FF6B35;
}

.header-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.more-text {
  font-size: 26rpx;
  color: #999;
}

.arrow {
  font-size: 20rpx;
  color: #ccc;
  margin-left: 4rpx;
}

.secondhand-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.item {
  width: calc(33.33% - 11rpx);
  border-radius: 12rpx;
  overflow: hidden;
  background: #f8f8f8;
}

.pictrue {
  position: relative;
  width: 100%;
  height: 220rpx;
}

.pictrue image {
  width: 100%;
  height: 100%;
}

.condition-tag {
  position: absolute;
  top: 8rpx;
  left: 8rpx;
  background: rgba(255, 107, 53, 0.9);
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.info {
  padding: 12rpx 14rpx;
}

.title {
  font-size: 24rpx;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-top: 8rpx;
}

.price {
  font-size: 28rpx;
  color: #FF6B35;
  font-weight: 700;
}

.ot-price {
  font-size: 20rpx;
  color: #ccc;
  text-decoration: line-through;
  margin-left: 8rpx;
}

.empty-tip {
  text-align: center;
  padding: 40rpx;
  color: #999;
  font-size: 26rpx;
}
</style>
