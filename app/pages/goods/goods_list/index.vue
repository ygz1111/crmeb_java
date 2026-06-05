<template>
	<view :data-theme="theme">
		<view class="crmeb-product-list">
			<!-- 搜索栏 -->
			<view class="crmeb-pl__search">
				<!-- #ifdef H5 -->
				<view class="crmeb-pl__back iconfont icon-xiangzuo" @click="goback()"></view>
				<!-- #endif -->
				<view class="crmeb-pl__input-wrap">
					<text class="crmeb-pl__search-icon">&#x1F50D;</text>
					<input class="crmeb-pl__input" placeholder="搜索商品名称" placeholder-class="crmeb-pl__ph"
						confirm-type="search" :value='where.keyword' @confirm="searchSubmit" maxlength="20" />
				</view>
				<view class="crmeb-pl__toggle" @click="Changswitch">
					<text class="crmeb-pl__toggle-icon">{{ is_switch ? '&#9776;' : '&#9638;' }}</text>
				</view>
			</view>
			<!-- 排序导航 -->
			<view class="crmeb-pl__nav">
				<view class="crmeb-pl__nav-item" :class="title ? 'crmeb-pl__nav-active' : ''" @click="set_where(1)">
					{{ title ? title : '默认' }}
				</view>
				<view class="crmeb-pl__nav-item" @click="set_where(2)">
					价格
					<text class="crmeb-pl__arrow" :class="price === 1 ? 'up' : price === 2 ? 'down' : ''">&#9650;</text>
				</view>
				<view class="crmeb-pl__nav-item" @click="set_where(3)">
					销量
					<text class="crmeb-pl__arrow" :class="stock === 1 ? 'up' : stock === 2 ? 'down' : ''">&#9650;</text>
				</view>
				<view class="crmeb-pl__nav-item" :class="nows ? 'crmeb-pl__nav-active' : ''" @click="set_where(4)">新品
				</view>
			</view>
			<!-- 商品列表 -->
			<view class="crmeb-pl__goods" :class="is_switch ? 'grid' : 'list'">
				<view class="crmeb-pl__goods-item" :class="is_switch ? 'grid' : 'list'" hover-class="none"
					v-for="(item, index) in productList" :key="index" @click="godDetail(item)">
					<view class="crmeb-pl__goods-img" :class="is_switch ? 'grid' : 'list'">
						<image :src="item.image" mode="aspectFill"></image>
						<view class="crmeb-pl__badge" v-if="item.activityH5 && item.activityH5.type === '1'">秒杀</view>
						<view class="crmeb-pl__badge bargain" v-if="item.activityH5 && item.activityH5.type === '2'">砍价</view>
						<view class="crmeb-pl__badge group" v-if="item.activityH5 && item.activityH5.type === '3'">拼团</view>
					</view>
					<view class="crmeb-pl__goods-info" :class="is_switch ? 'grid' : 'list'">
						<view class="crmeb-pl__goods-name line1">{{ item.storeName }}</view>
						<view class="crmeb-pl__goods-price">￥<text class="crmeb-pl__price-num">{{ item.price }}</text></view>
						<view class="crmeb-pl__goods-meta">
							<view class="crmeb-pl__vip-price" v-if="item.vip_price && item.vip_price > 0">
								会员￥{{ item.vip_price }}
							</view>
							<view class="crmeb-pl__sales">已售{{ Number(item.sales) }}{{ item.unitName }}</view>
						</view>
					</view>
				</view>
			</view>
			<view class="crmeb-pl__loading" v-if="productList.length > 0">
				<text class="crmeb-pl__loading-icon" :hidden="loading == false"></text>
				{{ loadTitle }}
			</view>
		</view>
		<view class="crmeb-empty" v-if="productList.length == 0 && where.page > 1">
			<view class="crmeb-empty__img">
				<image :src="urlDomain + 'crmebimage/perset/staticImg/noShopper.png'"></image>
			</view>
			<recommend ref="recommendIndex"></recommend>
		</view>
	</view>
</template>

<script>
	import {
		getProductslist,
		getProductHot
	} from '@/api/store.js';
	import recommend from '@/components/recommend';
	import {
		mapGetters
	} from "vuex";
	import {
		goShopDetail
	} from '@/libs/order.js'
	import animationType from '@/utils/animationType.js'
	let app = getApp();
	export default {
		computed: mapGetters(['uid']),
		components: {
			recommend
		},
		data() {
			return {
				urlDomain: this.$Cache.get("imgHost"),
				productList: [],
				is_switch: true,
				where: {
					keyword: '',
					priceOrder: '',
					salesOrder: '',
					news: 0,
					page: 1,
					limit: 20,
					cid: '',
				},
				price: 0,
				stock: 0,
				nows: false,
				loadend: false,
				loading: false,
				loadTitle: '加载更多',
				title: '',
				theme: app.globalData.theme
			};
		},
		onLoad: function(options) {
			this.$set(this.where, 'cid', options.cid || '');
			this.title = options.title || '';
			this.$set(this.where, 'keyword', options.searchValue || '');
			this.get_product_list();
		},
		methods: {
			goback() {
				// #ifdef H5
				return history.back();
				// #endif
				// #ifndef H5
				return uni.navigateBack({
					delta: 1,
				})
				// #endif
			},
			godDetail(item) {
				goShopDetail(item, this.uid).then(res => {
					uni.navigateTo({
						animationType: animationType.type,
						animationDuration: animationType.duration,
						url: `/pages/goods/goods_details/index?id=${item.id}`
					})
				})
			},
			Changswitch: function() {
				let that = this;
				that.is_switch = !that.is_switch
			},
			searchSubmit: function(e) {
				let that = this;
				that.$set(that.where, 'keyword', e.detail.value);
				that.loadend = false;
				that.$set(that.where, 'page', 1)
				this.get_product_list(true);
			},
			set_where: function(e) {
				switch (e) {
					case 1:
						return;
					case 2:
						if (this.price == 0) this.price = 1;
						else if (this.price == 1) this.price = 2;
						else if (this.price == 2) this.price = 0;
						this.stock = 0;
						break;
					case 3:
						if (this.stock == 0) this.stock = 1;
						else if (this.stock == 1) this.stock = 2;
						else if (this.stock == 2) this.stock = 0;
						this.price = 0
						break;
					case 4:
						this.nows = !this.nows;
						break;
				}
				this.loadend = false;
				this.$set(this.where, 'page', 1);
				this.get_product_list(true);
			},
			setWhere: function() {
				if (this.price == 0) this.where.priceOrder = '';
				else if (this.price == 1) this.where.priceOrder = 'asc';
				else if (this.price == 2) this.where.priceOrder = 'desc';
				if (this.stock == 0) this.where.salesOrder = '';
				else if (this.stock == 1) this.where.salesOrder = 'asc';
				else if (this.stock == 2) this.where.salesOrder = 'desc';
				this.where.news = this.nows ? 1 : 0;
			},
			get_product_list: function(isPage) {
				let that = this;
				that.setWhere();
				if (that.loadend) return;
				if (that.loading) return;
				if (isPage === true) that.$set(that, 'productList', []);
				that.loading = true;
				that.loadTitle = '';
				getProductslist(that.where).then(res => {
					let list = res.data.list;
					let productList = that.$util.SplitArray(list, that.productList);
					let loadend = list.length < that.where.limit;
					that.loadend = loadend;
					that.loading = false;
					that.loadTitle = loadend ? '已全部加载' : '加载更多';
					that.$set(that, 'productList', productList);
					that.$set(that.where, 'page', that.where.page + 1);
					if (that.productList.length === 0) {
						this.get_host_product();
					}
				}).catch(err => {
					that.loading = false;
					that.loadTitle = '加载更多';
				});
			},
		},
		onReachBottom() {
			if (this.productList.length > 0) {
				this.get_product_list();
			} else {
				this.$refs.recommendIndex.get_host_product();
			}
		}
	}
</script>

<style lang="scss" scoped>
	/* ===== CRMEB Design System: Product List ===== */
	$primary: #059669;
	$primary-light: #ECFDF5;
	$secondary: #10B981;
	$accent: #D97706;
	$fg: #0F172A;
	$muted-bg: #F0F8F6;
	$radius: 16rpx;
	$radius-sm: 12rpx;

	.crmeb-product-list {
		min-height: 100vh;
		background: $primary-light;
	}

	/* Search */
	.crmeb-pl__search {
		display: flex;
		align-items: center;
		padding: 12rpx 24rpx;
		background: linear-gradient(135deg, $primary, $secondary);
		position: sticky;
		top: 0;
		z-index: 10;
	}

	.crmeb-pl__back {
		font-size: 40rpx;
		color: #fff;
		margin-right: 16rpx;
		width: 60rpx;
	}

	.crmeb-pl__input-wrap {
		flex: 1;
		display: flex;
		align-items: center;
		height: 64rpx;
		background: rgba(255, 255, 255, 0.95);
		border-radius: 50rpx;
		padding: 0 24rpx;
	}

	.crmeb-pl__search-icon {
		font-size: 28rpx;
		margin-right: 12rpx;
		color: #94a3b8;
	}

	.crmeb-pl__input {
		flex: 1;
		height: 100%;
		font-size: 26rpx;
		color: $fg;
	}

	.crmeb-pl__ph {
		color: #94a3b8;
	}

	.crmeb-pl__toggle {
		width: 62rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-left: 12rpx;
	}

	.crmeb-pl__toggle-icon {
		font-size: 36rpx;
		color: #fff;
	}

	/* Nav */
	.crmeb-pl__nav {
		display: flex;
		height: 80rpx;
		background: #fff;
		border-bottom: 1rpx solid $muted-bg;
	}

	.crmeb-pl__nav-item {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 26rpx;
		color: #454545;
	}

	.crmeb-pl__nav-active {
		color: $primary;
		font-weight: 600;
	}

	.crmeb-pl__arrow {
		font-size: 16rpx;
		margin-left: 6rpx;
		color: #ccc;
		transform: rotate(0deg);
		transition: transform 0.2s;
	}

	.crmeb-pl__arrow.up {
		color: $primary;
		transform: rotate(0deg);
	}

	.crmeb-pl__arrow.down {
		color: $primary;
		transform: rotate(180deg);
	}

	/* Goods Grid */
	.crmeb-pl__goods {
		padding: 20rpx;
	}

	.crmeb-pl__goods.grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 20rpx;
	}

	.crmeb-pl__goods.list {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
	}

	.crmeb-pl__goods-item {
		background: #fff;
		border-radius: $radius;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(5, 150, 105, 0.06);
		transition: transform 0.2s;
	}

	.crmeb-pl__goods-item:active {
		transform: scale(0.98);
	}

	.crmeb-pl__goods-item.grid {
		display: flex;
		flex-direction: column;
	}

	.crmeb-pl__goods-item.list {
		display: flex;
		flex-direction: row;
	}

	.crmeb-pl__goods-img {
		position: relative;
		overflow: hidden;
	}

	.crmeb-pl__goods-img.grid {
		width: 100%;
		height: 330rpx;
	}

	.crmeb-pl__goods-img.list {
		width: 200rpx;
		height: 200rpx;
		flex-shrink: 0;
	}

	.crmeb-pl__goods-img image {
		width: 100%;
		height: 100%;
	}

	.crmeb-pl__badge {
		position: absolute;
		top: 12rpx;
		left: 12rpx;
		padding: 4rpx 14rpx;
		background: linear-gradient(135deg, #dc2626, #ef4444);
		color: #fff;
		font-size: 20rpx;
		border-radius: 20rpx;
		font-weight: 600;
	}

	.crmeb-pl__badge.bargain {
		background: linear-gradient(135deg, $accent, #f59e0b);
	}

	.crmeb-pl__badge.group {
		background: linear-gradient(135deg, #7c3aed, #8b5cf6);
	}

	.crmeb-pl__goods-info {
		padding: 16rpx 20rpx;
	}

	.crmeb-pl__goods-info.grid {
		padding: 16rpx 20rpx 20rpx;
	}

	.crmeb-pl__goods-info.list {
		flex: 1;
		padding: 16rpx 20rpx;
		display: flex;
		flex-direction: column;
		justify-content: center;
	}

	.crmeb-pl__goods-name {
		font-size: 26rpx;
		color: $fg;
		line-height: 36rpx;
		margin-bottom: 12rpx;
	}

	.crmeb-pl__goods-price {
		color: $primary;
		font-size: 24rpx;
		font-weight: 700;
	}

	.crmeb-pl__price-num {
		font-size: 34rpx;
	}

	.crmeb-pl__goods-meta {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 8rpx;
	}

	.crmeb-pl__vip-price {
		font-size: 22rpx;
		color: $accent;
		font-weight: 500;
		background: #fef3c7;
		padding: 2rpx 12rpx;
		border-radius: 20rpx;
	}

	.crmeb-pl__sales {
		font-size: 22rpx;
		color: #94a3b8;
	}

	/* Loading */
	.crmeb-pl__loading {
		text-align: center;
		padding: 30rpx;
		font-size: 26rpx;
		color: #94a3b8;
	}

	/* Empty */
	.crmeb-empty {
		background: #fff;
		padding-bottom: 30rpx;
		margin-top: 172rpx;
	}
</style>
