<template>
	<view :data-theme="theme">
		<!-- #ifndef APP-PLUS -->
		<view v-if="productType==='video'" class="crmeb-checkout__nav-bar">
			<nav-bar :navTitle="navTitle" @getNavH="getNavH"></nav-bar>
		</view>
		<!-- #endif -->
		<view class="crmeb-checkout" :style="'margin-top:'+(marTop)+'rpx;'">
			<view class="crmeb-checkout__address-section">
				<!-- Address -->
				<view class="crmeb-checkout__address" @tap="onAddress">
					<view class="crmeb-checkout__address-info" v-if="addressInfo.realName">
						<view class="crmeb-checkout__address-name">{{addressInfo.realName}}
							<text class="crmeb-checkout__address-phone">{{addressInfo.phone}}</text>
						</view>
						<view class="crmeb-checkout__address-detail">
							<text class="crmeb-checkout__address-default" v-if="addressInfo.isDefault">[默认]</text>
							<text>{{addressInfo.province}}{{addressInfo.city}}{{addressInfo.district}}{{addressInfo.detail}}</text>
						</view>
					</view>
					<view class="crmeb-checkout__address-info" v-else>
						<view class="crmeb-checkout__address-empty">设置收货地址</view>
					</view>
					<text class="crmeb-checkout__address-arrow">&#8250;</text>
				</view>
			</view>
			<view class="crmeb-checkout__body">
				<!-- 商品信息 -->
				<view class="crmeb-checkout__goods" v-if="cartInfo.length > 0">
					<view class="crmeb-checkout__goods-item" v-for="(item, index) in cartInfo" :key="index">
						<image class="crmeb-checkout__goods-img" :src="item.image" mode="aspectFill"></image>
						<view class="crmeb-checkout__goods-info">
							<view class="crmeb-checkout__goods-name line2">{{item.productName}}</view>
							<view class="crmeb-checkout__goods-spec" v-if="item.sku">{{item.sku}}</view>
							<view class="crmeb-checkout__goods-price">
								<text>￥{{item.price}}</text>
								<text>x{{item.payNum}}</text>
							</view>
						</view>
					</view>
				</view>
				<!-- 价格汇总 -->
				<view class="crmeb-checkout__summary">
					<view class="crmeb-checkout__summary-item">
						<text>商品总价：</text>
						<text class="crmeb-checkout__summary-price">￥{{totalPrice}}</text>
					</view>
				</view>
				<view style="height:120rpx;"></view>
			</view>
			<!-- Footer -->
			<view class="crmeb-checkout__footer">
				<text>合计: <text class="crmeb-checkout__total">￥{{totalPrice}}</text></text>
				<view class="crmeb-checkout__submit" @tap="SubOrder">提交订单</view>
			</view>
		</view>
	</view>
</template>

<script>
import {getAddressDefault} from '@/api/user.js';
import request from '@/utils/request.js';

export default {
	data() {
		return {
			theme: 'theme1',
			productType: 'normal',
			marTop: 0,
			navTitle: '确认订单',
			shippingType: 1,
			addressInfo: {},
			cartInfo: [],
			orderProNum: 0,
			orderInfoVo: {
				proTotalFee: 0,
				freightFee: 0,
				couponFee: 0,
				deductionPrice: 0,
				payFee: 0
			},
			totalPrice: 0,
			preOrderNo: '',
			orderDetails: []
		};
	},
	onLoad(options) {
		let that = this;
		// 从URL获取preOrderNo
		if (options.preOrderNo) {
			this.preOrderNo = options.preOrderNo;
		}
		// 从全局变量获取订单数据（首次进入时）
		let orderData = getApp().globalData.orderData;
		if (orderData) {
			this.cartInfo = orderData.cartInfo || [];
			this.orderDetails = orderData.orderDetails || [];
			this.orderProNum = orderData.orderProNum || 0;
			this.totalPrice = orderData.totalPrice || 0;
			this.preOrderNo = orderData.preOrderNo || this.preOrderNo;
			getApp().globalData.orderData = null;
		}
		// 如果有地址ID参数（从地址列表返回），加载指定地址
		if (options.addressId) {
			this.loadAddressById(options.addressId);
		} else {
			this.loadAddress();
		}
	},
	onShow() {
		// 从地址列表返回时，检查是否有新选择的地址
		let addrData = getApp().globalData.selectedAddress;
		if (addrData) {
			this.addressInfo = addrData;
			getApp().globalData.selectedAddress = null;
		}
	},
	methods: {
		loadAddress() {
			let that = this;
			getAddressDefault().then(res => {
				if (res.data) {
					that.addressInfo = {
						id: res.data.id,
						realName: res.data.realName,
						phone: res.data.phone,
						province: res.data.province,
						city: res.data.city,
						district: res.data.district,
						detail: res.data.detail,
						isDefault: res.data.isDefault
					};
				}
			}).catch(() => {
				that.addressInfo = {};
			});
		},
		loadAddressById(id) {
			let that = this;
			let HTTP_R = getApp().globalData.apiUrl || '';
			uni.request({
				url: HTTP_R + '/api/front/address/detail/' + id,
				method: 'GET',
				header: { 'Authori-zation': uni.getStorageSync('LOGIN_STATUS') || '' },
				success: function(res) {
					if (res.data && res.data.code === 200 && res.data.data) {
						let d = res.data.data;
						that.addressInfo = {
							id: d.id,
							realName: d.realName,
							phone: d.phone,
							province: d.province,
							city: d.city,
							district: d.district,
							detail: d.detail,
							isDefault: d.isDefault
						};
					}
				}
			});
		},
		onAddress() {
			uni.navigateTo({
				url: '/pages/users/user_address_list/index?preOrderNo=' + this.preOrderNo
			});
		},
		SubOrder() {
			let that = this;
			if (!that.addressInfo.id) {
				return uni.showToast({ title: '请先选择收货地址', icon: 'none' });
			}
			uni.showLoading({ title: '提交订单中...' });
			request.post("order/create", {
				"preOrderNo": that.preOrderNo,
				"shippingType": that.shippingType,
				"addressId": that.addressInfo.id,
				"useIntegral": false
			}).then(res => {
				uni.hideLoading();
				var orderNo = res.data.orderNo || res.data.orderId;
				var payPrice = that.totalPrice;
				uni.redirectTo({
					url: '/pages/order/order_payment/index?orderNo=' + orderNo + '&payPrice=' + payPrice
				});
			}).catch(err => {
				uni.hideLoading();
				uni.showToast({ title: err || '提交失败', icon: 'none' });
			});
		}
	}
};
</script>

<style scoped>
.crmeb-checkout {
	padding: 20rpx;
	background: #f5f5f5;
	min-height: 100vh;
}
.crmeb-checkout__address-section {
	background: #fff;
	border-radius: 12rpx;
	margin-bottom: 20rpx;
	overflow: hidden;
}
.crmeb-checkout__address {
	display: flex;
	align-items: center;
	padding: 24rpx 30rpx;
}
.crmeb-checkout__address-info {
	flex: 1;
}
.crmeb-checkout__address-name {
	font-size: 30rpx;
	font-weight: 600;
	color: #333;
}
.crmeb-checkout__address-phone {
	font-size: 26rpx;
	color: #666;
	margin-left: 16rpx;
}
.crmeb-checkout__address-detail {
	font-size: 24rpx;
	color: #999;
	margin-top: 8rpx;
}
.crmeb-checkout__address-default {
	color: #ff6b35;
	font-size: 24rpx;
}
.crmeb-checkout__address-empty {
	font-size: 28rpx;
	color: #999;
}
.crmeb-checkout__address-arrow {
	font-size: 30rpx;
	color: #ccc;
}
.crmeb-checkout__body {
	background: #fff;
	border-radius: 12rpx;
	overflow: hidden;
}
.crmeb-checkout__goods-item {
	display: flex;
	padding: 20rpx 30rpx;
	border-bottom: 1rpx solid #f5f5f5;
}
.crmeb-checkout__goods-img {
	width: 150rpx;
	height: 150rpx;
	border-radius: 8rpx;
	flex-shrink: 0;
}
.crmeb-checkout__goods-info {
	flex: 1;
	margin-left: 20rpx;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}
.crmeb-checkout__goods-name {
	font-size: 28rpx;
	color: #333;
	line-height: 40rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}
.crmeb-checkout__goods-spec {
	font-size: 22rpx;
	color: #999;
	margin-top: 8rpx;
}
.crmeb-checkout__goods-price {
	display: flex;
	justify-content: space-between;
	font-size: 28rpx;
	color: #ff6b35;
	font-weight: 600;
}
.crmeb-checkout__summary {
	padding: 24rpx 30rpx;
	border-top: 1rpx solid #f5f5f5;
}
.crmeb-checkout__summary-item {
	display: flex;
	justify-content: space-between;
	font-size: 26rpx;
	color: #666;
	padding: 8rpx 0;
}
.crmeb-checkout__summary-price {
	color: #ff6b35;
	font-weight: 600;
}
.crmeb-checkout__footer {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	background: #fff;
	padding: 20rpx 30rpx;
	box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);
	height: 100rpx;
}
.crmeb-checkout__total {
	color: #ff6b35;
	font-size: 36rpx;
	font-weight: 700;
}
.crmeb-checkout__submit {
	background: linear-gradient(135deg, #ff6b35 0%, #ff3d00 100%);
	color: #fff;
	padding: 16rpx 60rpx;
	border-radius: 40rpx;
	font-size: 30rpx;
	font-weight: 600;
}
</style>
