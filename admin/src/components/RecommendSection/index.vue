<template>
  <div class="recommend-section" v-if="products.length > 0">
    <div class="section-header">
      <h3>猜你喜欢</h3>
      <span class="subtitle">基于协同过滤算法推荐</span>
    </div>
    <div class="product-grid">
      <div class="product-item" v-for="item in products" :key="item.id" @click="goDetail(item.id)">
        <div class="product-image">
          <img :src="item.image" :alt="item.store_name" />
          <span class="condition-tag" v-if="item.is_second_hand">二手</span>
        </div>
        <div class="product-info">
          <div class="product-name">{{ item.store_name }}</div>
          <div class="product-price">
            <span class="current-price">¥{{ item.price }}</span>
          </div>
          <div class="product-meta">
            <span class="sales">已售 {{ item.sales || 0 }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getRecommendList } from '@/api/recommend';

export default {
  name: 'RecommendSection',
  props: {
    uid: {
      type: [Number, String],
      required: true
    },
    limit: {
      type: Number,
      default: 10
    }
  },
  data() {
    return {
      products: []
    };
  },
  mounted() {
    this.fetchRecommendations();
  },
  methods: {
    async fetchRecommendations() {
      try {
        const res = await getRecommendList(this.uid, this.limit);
        if (res.code === 200) {
          this.products = res.data;
        }
      } catch (error) {
        console.error('获取推荐失败:', error);
      }
    },
    goDetail(id) {
      this.$router.push({
        path: '/product-detail',
        query: { id }
      });
    }
  }
};
</script>

<style scoped>
.recommend-section {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.section-header .subtitle {
  font-size: 12px;
  color: #999;
  margin-left: 10px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.product-item {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #f0f0f0;
}

.product-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-image {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
}

.product-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.condition-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #ff6b35;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.product-info {
  padding: 12px;
}

.product-name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
}

.product-price {
  margin-bottom: 4px;
}

.current-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff4d4f;
}

.product-meta {
  font-size: 12px;
  color: #999;
}
</style>
