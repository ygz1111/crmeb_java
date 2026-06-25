import request from '@/libs/request';

/**
 * 获取个性化推荐商品
 * @param {number} uid 用户ID
 * @param {number} n 推荐数量
 */
export function getRecommendList(uid, n = 10) {
  return request.get('api/front/recommend', {
    params: { uid, n }
  });
}

/**
 * 获取相似商品推荐
 * @param {number} productId 商品ID
 * @param {number} n 推荐数量
 */
export function getSimilarProducts(productId, n = 6) {
  return request.get(`api/front/recommend/similar/${productId}`, {
    params: { n }
  });
}
