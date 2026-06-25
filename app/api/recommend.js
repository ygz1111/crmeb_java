import request from "@/utils/request.js";

/**
 * 获取个性化推荐商品（协同过滤算法）
 * @param {number} uid 用户ID
 * @param {number} n 推荐数量
 */
export function getRecommendList(uid, n = 10) {
  return request.get('recommend', { uid, n }, { noAuth: true });
}

/**
 * 获取相似商品推荐
 * @param {number} productId 商品ID
 * @param {number} n 推荐数量
 */
export function getSimilarProducts(productId, n = 6) {
  return request.get('recommend/similar/' + productId, { n }, { noAuth: true });
}
