<template>
	<view :data-theme="theme">
		<view class="crmeb-orders">
			<!-- Header -->
			<view class="crmeb-orders__header">
				<view class="crmeb-orders__header-content">
					<view class="crmeb-orders__header-text">
						<view class="crmeb-orders__header-title">订单信息</view>
						<view class="crmeb-orders__header-sub">消费订单：{{orderData.orderCount || 0}} 总消费：￥{{Number(orderData.sumPrice).toFixed(2) || 0}}</view>
					</view>
					<view class="crmeb-orders__header-icon">
						<image src="/static/images/orderTime.png" />
					</view>
				</view>
			</view>
			<!-- Tabs -->
			<view class="crmeb-orders__tabs">
				<view class="crmeb-orders__tab" :class="orderStatus==0 ? 'on' : ''" @click="statusClick(0)">待付款<text class="crmeb-orders__tab-num">{{orderData.unPaidCount || 0}}</text></view>
				<view class="crmeb-orders__tab" :class="orderStatus==1 ? 'on' : ''" @click="statusClick(1)">待发货<text class="crmeb-orders__tab-num">{{orderData.unShippedCount || 0}}</text></view>
				<view class="crmeb-orders__tab" :class="orderStatus==2 ? 'on' : ''" @click="statusClick(2)">待收货<text class="crmeb-orders__tab-num">{{orderData.receivedCount || 0}}</text></view>
				<view class="crmeb-orders__tab" :class="orderStatus==3 ? 'on' : ''" @click="statusClick(3)">待评价<text class="crmeb-orders__tab-num">{{orderData.evaluatedCount || 0}}</text></view>
				<view class="crmeb-orders__tab" :class="orderStatus==4 ? 'on' : ''" @click="statusClick(4)">已完成<text class="crmeb-orders__tab-num">{{orderData.completeCount || 0}}</text></view>
			</view>
			<!-- Order List -->
			<view class="crmeb-orders__list">
				<view class="crmeb-orders__card" v-for="(item,index) in orderList" :key="index">
					<view @click="goOrderDetails(item.orderId)">
						<view class="crmeb-orders__card-header">
							<view class="crmeb-orders__card-left">
								<text class="crmeb-orders__card-badge" v-if="item.activityType !== '普通' && item.activityType !== '核销'">{{item.activityType}}</text>
								<text class="crmeb-orders__card-date">{{item.createTime}}</text>
							</view>
							<text class="crmeb-orders__card-status">{{item.orderStatus}}</text>
						</view>
						<view class="crmeb-orders__card-items">
							<view class="crmeb-orders__card-item" v-for="(items,idx) in item.orderInfoList" :key="idx">
								<view class="crmeb-orders__card-img">
									<image :src="items.image" />
								</view>
								<view class="crmeb-orders__card-info">
									<view class="crmeb-orders__card-name line2">{{items.storeName}}</view>
									<view class="crmeb-orders__card-meta">
										<text>￥{{items.price}}</text>
										<text>x{{items.cartNum}}</text>
									</view>
								</view>
							</view>
						</view>
						<view class="crmeb-orders__card-total">共{{item.totalNum}}件商品，总金额 <text class="crmeb-orders__card-total-price">￥{{item.payPrice}}</text></view>
					</view>
					<view class="crmeb-orders__card-actions">
						<view class="crmeb-orders__action-btn outline" v-if="!item.paid" @click="cancelOrder(index,item.id)">取消订单</view>
						<view class="crmeb-orders__action-btn primary" v-if="!item.paid" @click="goPay(item.payPrice,item.orderId)">立即付款</view>
						<view class="crmeb-orders__action-btn primary" v-else-if="item.status== 0 || item.status== 1 || item.status== 3" @click="goOrderDetails(item.orderId)">查看详情</view>
						<view class="crmeb-orders__action-btn primary" v-else-if="item.status==2" @click="goOrderDetails(item.orderId)">去评价</view>
						<view class="crmeb-orders__action-btn outline" v-if="item.status == 3" @click="delOrder(item.id,index)">删除订单</view>
					</view>
				</view>
			</view>
			<view class="crmeb-orders__loading" v-if="orderList.length>0">
				<text class="crmeb-orders__loading-icon" :hidden="loading==false"></text>{{loadTitle}}
			</view>
			<view v-if="orderList.length == 0">
				<emptyPage title="暂无订单~"></emptyPage>
			</view>
				<!-- <home></home> -->
			<payment :payMode="payMode" :pay_close="pay_close" @onChangeFun="onChangeFun" :order_id="pay_order_id" :totalPrice="totalPrice"></payment>
		</view>
	</view>
</template>
<script>
	import { getOrderList, orderData as fetchOrderData, orderCancel, orderDel } from '@/api/order.js';
	import { toLogin } from '@/libs/login.js';
	import { mapGetters } from 'vuex';
	import emptyPage from '@/components/emptyPage.vue';
	import payment from '@/components/payment/index.vue';
	let app = getApp();
	export default {
		components: {
			emptyPage,
			payment
		},
		data() {
			return {
				loadTitle: '加载更多',
				loading: false,
				loadend: false,
				page: 1,
				limit: 10,
				orderStatus: -1,
				orderList: [],
				orderData: {
					orderCount: 0,
					sumPrice: 0,
					unPaidCount: 0,
					unShippedCount: 0,
					receivedCount: 0,
					evaluatedCount: 0,
					completeCount: 0
				},
				payMode: [],
				pay_close: false,
				pay_order_id: '',
				totalPrice: 0,
				theme: app.globalData.theme
			};
		},
		computed: mapGetters(['isLogin']),
		onShow() {
			if (this.isLogin) {
				this.getOrderList();
				this.getOrderData();
			} else {
				toLogin();
			}
		},
		onLoad(options) {
			if (options.status !== undefined) {
				this.orderStatus = parseInt(options.status);
			}
		},
		onReachBottom() {
			this.getOrderList();
		},
		methods: {
			statusClick(status) {
				if (this.orderStatus === status) return;
				this.orderStatus = status;
				this.page = 1;
				this.orderList = [];
				this.loadend = false;
				this.getOrderList();
			},
			getOrderData() {
				fetchOrderData().then(res => {
					this.orderData = res.data;
				});
			},
			getOrderList() {
				let that = this;
				if (that.loadend) return;
				if (that.loading) return;
				that.loading = true;
				that.loadTitle = '';
				let data = {
					page: that.page,
					limit: that.limit
				};
				if (that.orderStatus !== -1) {
					data.type = that.orderStatus;
				} else {
					data.type = -999;
				}
				getOrderList(data).then(res => {
					let list = res.data.list || [];
					if (list.length < that.limit) that.loadend = true;
					that.orderList = that.orderList.concat(list);
					that.page++;
					that.loading = false;
					that.loadTitle = that.loadend ? '—— 没有更多了 ——' : '加载更多';
				}).catch(() => {
					that.loading = false;
				});
			},
			goOrderDetails(orderId) {
				uni.navigateTo({
					url: '/pages/order/order_details/index?orderId=' + orderId
				});
			},
			cancelOrder(index, id) {
				uni.showModal({
					title: '提示',
					content: '确定要取消该订单吗？',
					success: (res) => {
						if (res.confirm) {
							orderCancel(id).then(() => {
								this.orderList.splice(index, 1);
								this.$util.Tips({ title: '取消成功' });
							});
						}
					}
				});
			},
			goPay(price, orderId) {
				this.totalPrice = price;
				this.pay_order_id = orderId;
				this.pay_close = true;
			},
			delOrder(id, index) {
				uni.showModal({
					title: '提示',
					content: '确定要删除该订单吗？',
					success: (res) => {
						if (res.confirm) {
							orderDel(id).then(() => {
								this.orderList.splice(index, 1);
								this.$util.Tips({ title: '删除成功' });
							});
						}
					}
				});
			},
			onChangeFun(e) {
				let opt = e;
				if (typeof e === 'string') {
					opt = e.split(',');
				}
				switch (opt[0]) {
					case 'pay_close':
						this.pay_close = false;
						break;
				}
			}
		}
	};
</script>

<style lang="scss" scoped>
	page {
		background-color: #f5f5f5;
	}

	.crmeb-orders {
		display: flex;
		flex-direction: column;
		min-height: 100vh;
	}

	/* Header */
	.crmeb-orders__header {
		background: linear-gradient(135deg, #FF6B35, #FF8F65);
		padding: 40rpx 32rpx;
	}

	.crmeb-orders__header-content {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.crmeb-orders__header-text {
		flex: 1;
	}

	.crmeb-orders__header-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #fff;
	}

	.crmeb-orders__header-sub {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.8);
		margin-top: 8rpx;
	}

	.crmeb-orders__header-icon image {
		width: 80rpx;
		height: 80rpx;
	}

	/* Tabs */
	.crmeb-orders__tabs {
		display: flex;
		background: #fff;
		padding: 0 20rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
		position: sticky;
		top: 0;
		z-index: 10;
	}

	.crmeb-orders__tab {
		flex: 1;
		text-align: center;
		padding: 24rpx 0;
		font-size: 26rpx;
		color: #666;
		position: relative;
	}

	.crmeb-orders__tab.on {
		color: #FF6B35;
		font-weight: 600;
	}

	.crmeb-orders__tab.on::after {
		content: '';
		position: absolute;
		bottom: 0;
		left: 50%;
		transform: translateX(-50%);
		width: 40rpx;
		height: 4rpx;
		background: #FF6B35;
		border-radius: 2rpx;
	}

	.crmeb-orders__tab-num {
		font-size: 20rpx;
		color: #999;
		margin-left: 4rpx;
	}

	/* Order List */
	.crmeb-orders__list {
		padding: 20rpx 24rpx;
	}

	.crmeb-orders__card {
		background: #fff;
		border-radius: 16rpx;
		padding: 24rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
	}

	.crmeb-orders__card-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 20rpx;
	}

	.crmeb-orders__card-left {
		display: flex;
		align-items: center;
		gap: 12rpx;
	}

	.crmeb-orders__card-badge {
		background: #FFF0E8;
		color: #FF6B35;
		font-size: 20rpx;
		padding: 4rpx 12rpx;
		border-radius: 6rpx;
	}

	.crmeb-orders__card-date {
		font-size: 24rpx;
		color: #999;
	}

	.crmeb-orders__card-status {
		font-size: 26rpx;
		color: #FF6B35;
		font-weight: 500;
	}

	.crmeb-orders__card-items {
		border-top: 1rpx solid #f0f0f0;
		padding-top: 20rpx;
	}

	.crmeb-orders__card-item {
		display: flex;
		margin-bottom: 16rpx;
	}

	.crmeb-orders__card-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 12rpx;
		overflow: hidden;
		flex-shrink: 0;
	}

	.crmeb-orders__card-img image {
		width: 100%;
		height: 100%;
	}

	.crmeb-orders__card-info {
		flex: 1;
		margin-left: 16rpx;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
	}

	.crmeb-orders__card-name {
		font-size: 26rpx;
		color: #333;
		line-height: 1.5;
	}

	.crmeb-orders__card-meta {
		display: flex;
		justify-content: space-between;
		font-size: 24rpx;
		color: #999;
	}

	.crmeb-orders__card-total {
		text-align: right;
		font-size: 24rpx;
		color: #666;
		padding-top: 16rpx;
		border-top: 1rpx solid #f0f0f0;
		margin-top: 8rpx;
	}

	.crmeb-orders__card-total-price {
		font-size: 28rpx;
		font-weight: 700;
		color: #e93323;
	}

	/* Actions */
	.crmeb-orders__card-actions {
		display: flex;
		justify-content: flex-end;
		gap: 16rpx;
		padding-top: 20rpx;
	}

	.crmeb-orders__action-btn {
		padding: 14rpx 28rpx;
		border-radius: 40rpx;
		font-size: 24rpx;
		text-align: center;
	}

	.crmeb-orders__action-btn.primary {
		background: linear-gradient(135deg, #FF6B35, #FF8F65);
		color: #fff;
	}

	.crmeb-orders__action-btn.outline {
		border: 1rpx solid #ddd;
		color: #666;
		background: #fff;
	}

	/* Loading */
	.crmeb-orders__loading {
		text-align: center;
		padding: 30rpx 0;
		font-size: 24rpx;
		color: #999;
	}
</style>
