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
			<home></home>
			<payment :payMode="payMode" :pay_close="pay_close" @onChangeFun="onChangeFun" :order_id="pay_order_id" :totalPrice="totalPrice"></payment>
		</view>
	</view>
</template>
