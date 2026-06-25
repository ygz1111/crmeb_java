<template>
  <view class="cf-recommend" v-if="products.length > 0">
    <view class="section-header">
      <view class="title-row">
        <text class="title">猜你喜欢</text>
        <text class="subtitle">基于协同过滤算法推荐</text>
      </view>
    </view>
    <view class="product-list">
      <view class="product-item" v-for="item in products" :key="item.id" @click="goDetail(item.id)">
        <view class="product-image">
          <image :src="getImageUrl(item.image)" mode="aspectFill"></image>
          <view class="second-hand-tag" v-if="item.is_second_hand">二手</view>
        </view>
        <view class="product-info">
          <text class="product-name">{{ item.store_name }}</text>
          <view class="price-row">
            <text class="price">¥{{ item.price }}</text>
            <text class="sales">已售{{ item.sales || 0 }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getRecommendList } from '@/api/recommend.js';

export default {
  name: 'CfRecommend',
  props: {
    limit: {
      type: Number,
      default: 10
    }
  },
  data() {
    return {
      products: [],
      imgHost: ''
    };
  },
  mounted() {
    this.imgHost = this.$Cache ? this.$Cache.get('imgHost') || '' : '';
    this.fetchRecommendations();
  },
  methods: {
    async fetchRecommendations() {
      try {
        const uid = this.$store.state.app.uid;
        if (!uid) {
          this.fetchPopular();
          return;
        }
        const res = await getRecommendList(uid, this.limit);
        if (res.code === 200 && res.data.length > 0) {
          this.products = res.data;
        } else {
          this.fetchPopular();
        }
      } catch (e) {
        this.fetchPopular();
      }
    },
    async fetchPopular() {
      try {
        const res = await getRecommendList(0, this.limit);
        if (res.code === 200) {
          this.products = res.data;
        }
      } catch (e) {
        console.error('获取推荐失败:', e);
      }
    },
    goDetail(id) {
      uni.navigateTo({
        url: '/pages/goods/goods_details/index?id=' + id
      });
    },
    getImageUrl(url) {
      if (!url) return '';
      if (url.startsWith('http')) return url;
      return this.imgHost + url;
    }
  }
};
</script>

<style scoped lang="scss">
.cf-recommend {
  padding: 20rpx 0;
}

.section-header {
  padding: 20rpx 30rpx;
}

.title-row {
  display: flex;
  align-items: baseline;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.subtitle {
  font-size: 22rpx;
  color: #999;
  margin-left: 16rpx;
}

.product-list {
  display: flex;
  flex-wrap: wrap;
  padding: 0 20rpx;
}

.product-item {
  width: calc(50% - 30rpx);
  margin: 0 15rpx 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.product-image {
  position: relative;
  width: 100%;
  height: 340rpx;
}

.product-image image {
  width: 100%;
  height: 100%;
}

.second-hand-tag {
  position: absolute;
  top: 16rpx;
  left: 16rpx;
  background: linear-gradient(135deg, #ff6b35, #ff8f65);
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}

.product-info {
  padding: 16rpx 20rpx;
}

.product-name {
  font-size: 26rpx;
  color: #333;
  line-height: 1.4;
  height: 72rpx;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
}

.price {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.sales {
  font-size: 22rpx;
  color: #999;
}
</style>
