<template>
	<view :data-theme="theme">
		<!-- #ifndef APP-PLUS -->
		<view v-if="productType==='video'" class="crmeb-checkout__nav-bar">
			<nav-bar :navTitle="navTitle" @getNavH="getNavH"></nav-bar>
		</view>
		<!-- #endif -->
		<view class="crmeb-checkout" :style="'margin-top:'+(marTop)+'rpx;'">
			<view class="crmeb-checkout__address-section" :style="store_self_mention ? '':'padding-top:0;'">
				<!-- Shipping Tabs -->
				<view class="crmeb-checkout__shipping-tabs" v-if="store_self_mention">
					<view class="crmeb-checkout__shipping-tab" :class="shippingType == 0 ? 'on' : 'off'" @tap="addressType(0)">快递配送</view>
					<view class="crmeb-checkout__shipping-tab" :class="shippingType == 1 ? 'on' : 'off'" @tap="addressType(1)">门店自提</view>
				</view>
				<!-- Address -->
				<view class="crmeb-checkout__address" @tap="onAddress" v-if="shippingType == 0">
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
				<!-- Store Pickup -->
				<view class="crmeb-checkout__address" v-else @tap="showStoreList">
					<view class="crmeb-checkout__address-info" v-if="storeList.length>0">
						<view class="crmeb-checkout__address-name">{{system_store.name}}
							<text class="crmeb-checkout__address-phone">{{system_store.phone}}</text>
						</view>
						<view class="crmeb-checkout__address-detail">{{system_store.address}}{{", " + system_store.detailedAddress}}</view>
					</view>
					<view class="crmeb-checkout__address-info" v-else>
						<view class="crmeb-checkout__address-empty">暂无门店信息</view>
					</view>
					<text class="crmeb-checkout__address-arrow">&#8250;</text>
				</view>
			</view>
			<view class="crmeb-checkout__body">
				<orderGoods :cartInfo="cartInfo" :orderProNum="orderProNum"></orderGoods>
				<!-- Order Options -->
				<view class="crmeb-checkout__options">
					<!-- Coupon -->
					<view class="crmeb-checkout__option" @tap="couponTap"
						v-if="!orderInfoVo.bargainId && !orderInfoVo.combinationId && !orderInfoVo.seckillId && productType==='normal'">
						<text>优惠券</text>
						<view class="crmeb-checkout__option-value">{{couponTitle}}
							<text class="crmeb-checkout__arrow">&#8250;</text>
						</view>
					</view>
					<!-- Points -->
					<view class="crmeb-checkout__option"
						v-if="!orderInfoVo.bargainId && !orderInfoVo.combinationId && !orderInfoVo.seckillId && productType==='normal'">
						<text>积分抵扣</text>
						<view class="crmeb-checkout__option-value">
							<text>{{useIntegral ? "剩余积分":"当前积分"}}
								<text class="crmeb-checkout__points-num">{{useIntegral ? orderInfoVo.surplusIntegral : orderInfoVo.userIntegral || 0}}</text>
							</text>
							<checkbox-group @change="ChangeIntegral">
								<checkbox :checked="useIntegral ? true : false" :disabled="orderInfoVo.userIntegral==0 && !useIntegral" />
							</checkbox-group>
						</view>
					</view>
					<!-- Shipping Fee -->
					<view class="crmeb-checkout__option" v-if="shippingType==0">
						<text>快递费用</text>
						<text class="crmeb-checkout__option-value" v-if="parseFloat(orderInfoVo.freightFee) > 0">+￥{{orderInfoVo.freightFee}}</text>
						<text class="crmeb-checkout__option-value" v-else>免运费</text>
					</view>
					<!-- Self Pickup Contact -->
					<template v-else>
						<view class="crmeb-checkout__option">
							<text>联系人</text>
							<input class="crmeb-checkout__input" type="text" placeholder="请填写您的联系姓名" placeholder-style="color:#ccc;" @blur="realName" maxlength="20" />
						</view>
						<view class="crmeb-checkout__option">
							<text>联系电话</text>
							<input class="crmeb-checkout__input" type="number" placeholder="请填写您的联系电话" placeholder-style="color:#ccc;" @blur="phone" maxlength="11" />
						</view>
					</template>
					<!-- Remark -->
					<view class="crmeb-checkout__remark" v-if="textareaStatus">
						<view class="crmeb-checkout__remark-header">
							<text>备注信息</text>
							<text class="crmeb-checkout__remark-count">{{markNum ? markNum : 0}}/150</text>
						</view>
						<textarea v-if="coupon.coupon===false" placeholder-class="crmeb-checkout__ph" @input="bindHideKeyboard"
							:maxlength="150" value="" name="mark" placeholder="请添加备注（150字以内）" class="crmeb-checkout__textarea"></textarea>
					</view>
				</view>
				<!-- Price Summary -->
				<view class="crmeb-checkout__summary">
					<view class="crmeb-checkout__summary-item">
						<text>商品总价：</text>
						<text class="crmeb-checkout__summary-price">￥{{orderInfoVo.proTotalFee || 0}}</text>
					</view>
					<view class="crmeb-checkout__summary-item" v-if="orderInfoVo.couponFee > 0">
						<text>优惠券抵扣：</text>
						<text class="crmeb-checkout__summary-price">-￥{{orderInfoVo.couponFee}}</text>
					</view>
					<view class="crmeb-checkout__summary-item" v-if="orderInfoVo.deductionPrice > 0">
						<text>积分抵扣：</text>
						<text class="crmeb-checkout__summary-price">-￥{{orderInfoVo.deductionPrice}}</text>
					</view>
					<view class="crmeb-checkout__summary-item" v-if="orderInfoVo.freightFee > 0">
						<text>运费：</text>
						<text class="crmeb-checkout__summary-price">+￥{{orderInfoVo.freightFee}}</text>
					</view>
				</view>
				<view style="height:120rpx;"></view>
			</view>
			<!-- Footer -->
			<view class="crmeb-checkout__footer">
				<text>合计: <text class="crmeb-checkout__total">￥{{orderInfoVo.payFee || 0}}</text></text>
				<view class="crmeb-checkout__submit" @tap="SubOrder">提交订单</view>
			</view>
			<view class="alipaysubmit" v-html="formContent"></view>
			<couponListWindow :coupon="coupon" @ChangCouponsClone="ChangCouponsClone" :openType="openType"
				@ChangCoupons="ChangCoupons" :orderShow="orderShow"></couponListWindow>
		</view>
	</view>
</template>
