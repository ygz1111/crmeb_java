// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import {preOrderApi, loadPreOrderApi} from '@/api/order.js';
import request from '@/utils/request.js';
import util from 'utils/util'
import animationType from '@/utils/animationType.js'
/**
 * 去商品详情
 */
export function goShopDetail(item, uid) {
	return new Promise(resolve => {
		if (item.activityH5 && item.activityH5.type === "1") {
			uni.navigateTo({
				url: `/pages/activity/goods_seckill_details/index?id=${item.activityH5.id}`
			})
		} else if (item.activityH5 && item.activityH5.type === "2") {
			uni.navigateTo({
				url: `/pages/activity/goods_bargain_details/index?id=${item.activityH5.id}&startBargainUid=${uid}`
			})
		} else if (item.activityH5 && item.activityH5.type === "3") {
			uni.navigateTo({
				url: `/pages/activity/goods_combination_details/index?id=${item.activityH5.id}`
			})
		} else {
			resolve(item);
		}
	});
}

/**
 * 活动商品、普通商品、购物车、再次购买预下单
 * 期末项目简化版：预下单后自动创建订单，直接跳支付
 */
export function getPreOrder(preOrderType, orderDetails) {
	return new Promise((resolve, reject) => {
		uni.showLoading({ title: '加载中...' });
		preOrderApi({
			"preOrderType": preOrderType,
			"orderDetails": orderDetails
		}).then(res => {
			var preOrderNo = res.data.preOrderNo;
			// 获取预下单详情（包含价格信息）
			loadPreOrderApi(preOrderNo).then(detailRes => {
				uni.hideLoading();
				var orderInfoVo = detailRes.data.orderInfoVo || {};
				// 将订单数据存入全局变量，跳转到确认页
				getApp().globalData.orderData = {
					preOrderNo: preOrderNo,
					cartInfo: orderInfoVo.orderDetailList || [],
					orderDetails: orderDetails,
					orderProNum: orderInfoVo.orderProNum || orderDetails[0] ? orderDetails[0].productNum : 1,
					totalPrice: orderInfoVo.proTotalFee || 0,
					freightFee: orderInfoVo.freightFee || 0,
					payFee: orderInfoVo.payFee || 0
				};
				uni.navigateTo({
					url: '/pages/order/order_confirm/index?preOrderNo=' + preOrderNo
				});
			}).catch(err => {
				uni.hideLoading();
				return util.Tips({ title: typeof err === 'string' ? err : (err.message || '加载失败') });
			});
		}).catch(err => {
			uni.hideLoading();
			return util.Tips({ title: typeof err === 'string' ? err : (err.message || '加载失败') });
		});
	});
}


