<template>
	<view :data-theme="theme">
		<view class="crmeb-profile">
			<scroll-view scroll-y="true" class="crmeb-profile__scroll">
				<!-- 用户卡片 -->
				<view class="crmeb-profile__header">
					<view class="crmeb-profile__card">
						<view class="crmeb-profile__user" @click="goEdit()">
							<image class="crmeb-profile__avatar" :src='userInfo.avatar' v-if="userInfo.avatar && uid">
							</image>
							<image v-else class="crmeb-profile__avatar"
									src="/static/brand/avatar_default.png" mode=""></image>
							<view class="crmeb-profile__info">
								<view class="crmeb-profile__name" v-if="!isLogin" @tap="openAuto">
									点击登录账号
								</view>
								<view class="crmeb-profile__name" v-if="userInfo && uid">
									{{ userInfo && userInfo.nickname && uid ? userInfo.nickname : '' }}
									<view class="crmeb-profile__vip" v-if="userInfo.vip">
										<image :src="userInfo.vipIcon" alt="" />
										<view class="crmeb-profile__vip-txt">{{ userInfo.vipName || '' }}</view>
									</view>
								</view>
								<view class="crmeb-profile__phone-row" v-if="userInfo && userInfo.phone && uid">
									<text>{{ userInfo.phone }}</text>
								</view>
								<view class="crmeb-profile__bind-phone" v-if="!userInfo.phone && isLogin"
									@tap.stop="bindPhone">
									绑定手机号
								</view>
								<!-- #ifdef APP-PLUS -->
								<text class="crmeb-profile__settings" @click.stop="appUpdate()">&#9881;</text>
								<!-- #endif -->
							</view>
						</view>
						<!-- 统计数据 -->
						<view class="crmeb-profile__stats">
							<view class="crmeb-profile__stat-item" @click="goMenuPage('/pages/users/user_money/index')">
								<text class="crmeb-profile__stat-num">{{ userInfo.nowMoney && uid ? userInfo.nowMoney : 0 }}</text>
								<view class="crmeb-profile__stat-label">余额</view>
							</view>
							<view class="crmeb-profile__stat-item"
								@click="goMenuPage('/pages/users/user_integral/index')">
								<text class="crmeb-profile__stat-num">{{ userInfo.integral && uid ? userInfo.integral : 0 }}</text>
								<view class="crmeb-profile__stat-label">积分</view>
							</view>
							<view class="crmeb-profile__stat-item"
								@click="goMenuPage('/pages/users/user_coupon/index')">
								<text class="crmeb-profile__stat-num">{{ userInfo.couponCount && uid ? userInfo.couponCount : 0 }}</text>
								<view class="crmeb-profile__stat-label">优惠券</view>
							</view>
							<view class="crmeb-profile__stat-item"
								@click="goMenuPage('/pages/users/user_goods_collection/index')">
								<text class="crmeb-profile__stat-num">{{ userInfo.collectCount && uid ? userInfo.collectCount : 0 }}</text>
								<view class="crmeb-profile__stat-label">收藏</view>
							</view>
						</view>
					</view>
					<!-- 订单中心 -->
					<view class="crmeb-profile__orders">
						<view class="crmeb-profile__orders-header">
							<view class="crmeb-profile__orders-title">订单中心</view>
							<view class="crmeb-profile__orders-all" @click="menusTap('/pages/users/order_list/index')">
								查看全部 &#8250;
							</view>
						</view>
						<view class="crmeb-profile__orders-grid">
							<view class="crmeb-profile__order-item" v-for="(item, index) in orderMenu" :key="index"
								@click="menusTap(item.url)">
								<view class="crmeb-profile__order-icon">
									<text class="iconfont" :class="item.img"></text>
									<text class="crmeb-profile__order-num" v-if="item.num > 0">{{ item.num }}</text>
								</view>
								<view class="crmeb-profile__order-label">{{ item.title }}</view>
							</view>
						</view>
					</view>
				</view>
				<!-- 内容区 -->
				<view class="crmeb-profile__body">
					<!-- 我的发布（独立一级菜单） -->
					<view class="crmeb-profile__publish">
						<view class="crmeb-profile__publish-header">
							<view class="crmeb-profile__publish-title">我的发布</view>
							<view class="crmeb-profile__publish-all" @click="goMyPublish">
								查看全部 ›
							</view>
						</view>
						<view class="crmeb-profile__publish-stats">
							<view class="crmeb-profile__publish-stat-item">
								<text class="crmeb-profile__publish-stat-num">{{ publishStats.total || 0 }}</text>
								<view class="crmeb-profile__publish-stat-label">全部</view>
							</view>
							<view class="crmeb-profile__publish-stat-item">
								<text class="crmeb-profile__publish-stat-num on-sale">{{ publishStats.onSale || 0 }}</text>
								<view class="crmeb-profile__publish-stat-label">在售中</view>
							</view>
							<view class="crmeb-profile__publish-stat-item">
								<text class="crmeb-profile__publish-stat-num sold-out">{{ publishStats.soldOut || 0 }}</text>
								<view class="crmeb-profile__publish-stat-label">已售罄</view>
							</view>
							<view class="crmeb-profile__publish-stat-item">
								<text class="crmeb-profile__publish-stat-num off-shelf">{{ publishStats.offShelf || 0 }}</text>
								<view class="crmeb-profile__publish-stat-label">已下架</view>
							</view>
						</view>
						<view class="crmeb-profile__publish-action">
							<button class="publish-btn" @click="goPublish">
								<text class="btn-icon">+</text>
								<text>立即发布</text>
							</button>
						</view>
					</view>
					
					<!-- 我的服务 -->
					<view class="crmeb-profile__services" @click.native="bindEdit('userMenus')">
						<view class="crmeb-profile__services-title">我的服务</view>
						<view class="crmeb-profile__services-grid">
							<view class="crmeb-profile__service-item" v-for="(item, index) in MyMenus" :key="index"
								@click="menusTap(item.url)"
								v-if="!(item.url =='/pages/service/index' || (item.url =='/pages/promoter/user_spread_user/index' && !userInfo.isPromoter))">
								<image :src="item.pic" class="crmeb-profile__service-icon" />
								<text class="crmeb-profile__service-name">{{ item.name }}</text>
							</view>
							<!-- #ifndef MP -->
							<view class="crmeb-profile__service-item" @click="onClickService">
								<image :src="servicePic" class="crmeb-profile__service-icon" />
								<text class="crmeb-profile__service-name">联系客服</text>
							</view>
							<!-- #endif -->
							<!-- #ifdef MP -->
							<button class="crmeb-profile__service-item" hover-class='none'
								@click="onClickService"
								v-if="chatConfig.telephone_service_switch === 'open'">
								<image :src="servicePic" class="crmeb-profile__service-icon" />
								<text class="crmeb-profile__service-name">联系客服</text>
							</button>
							<template v-else-if="chatConfig.wx_chant_independent==='open'">
								<button class="crmeb-profile__service-item" open-type='contact' hover-class='none'>
									<image :src="servicePic" class="crmeb-profile__service-icon" />
									<text class="crmeb-profile__service-name">联系客服</text>
								</button>
							</template>
							<template v-else>
								<button class="crmeb-profile__service-item" hover-class='none'
									@click="wxChatService">
									<image :src="servicePic" class="crmeb-profile__service-icon" />
									<text class="crmeb-profile__service-name">联系客服</text>
								</button>
							</template>
							<!-- #endif -->
						</view>
					</view>
				</view>
			</scroll-view>
		</view>
		<pageFooter></pageFooter>
	</view>
</template>
<script>
	let sysHeight = uni.getSystemInfoSync().statusBarHeight + 'px';
	import pageFooter from '@/components/pageFooter/index.vue'
	import Cache from '@/utils/cache';
	import {
		goPage
	} from '@/libs/iframe.js'
	import {
		BACK_URL
	} from '@/config/cache';
	import {
		getMenuList
	} from '@/api/user.js';
	import {
		orderData
	} from '@/api/order.js';
	import {
		getMyPublishStats
	} from '@/api/secondhand.js';
	import {
		getCity,
		tokenIsExistApi
	} from '@/api/api.js';
	import {
		toLogin
	} from '@/libs/login.js';
	import {
		mapGetters
	} from "vuex";
	import {
		getCityList
	} from "@/utils";
	// #ifdef H5
	import Auth from '@/libs/wechat';
	// #endif
	import {
		getShare
	} from '@/api/public.js';
	import {
		setThemeColor
	} from '@/utils/setTheme.js'
	import animationType from '@/utils/animationType.js'
	const app = getApp();
	export default {
		components: {
			pageFooter
		},
		computed: mapGetters(['isLogin', 'chatUrl', 'uid', 'bottomNavigationIsCustom']),
		data() {
			return {
				urlDomain: this.$Cache.get("imgHost"),
				orderMenu: [
					{ img: 'icon-daifukuan', title: '待付款', url: '/pages/users/order_list/index?status=0', num: 0 },
					{ img: 'icon-daifahuo', title: '待发货', url: '/pages/users/order_list/index?status=1', num: 0 },
					{ img: 'icon-daishouhuo', title: '待收货', url: '/pages/users/order_list/index?status=2', num: 0 },
					{ img: 'icon-daipingjia', title: '待评价', url: '/pages/users/order_list/index?status=3', num: 0 },
					{ img: 'icon-a-shouhoutuikuan', title: '售后/退款', url: '/pages/users/user_return_list/index',
					num: 0 },
				],
				imgUrls: [],
					userMenu: [],
					isAuto: false,
				isShowAuth: false,
				orderStatusNum: {},
				MyMenus: [],
				wechatUrl: [],
				servicePic: `${this.$Cache.get("imgHost")}crmebimage/perset/staticImg/customer.png`,
				publishPic: '/static/brand/tabbar/publishwei.png',
				sysHeight: sysHeight,
				pageHeight: '100%',
				configApi: {},
				theme: '',
				bgColor: '#FF6B35',
				chatConfig: {
					consumer_hotline: '',
					telephone_service_switch: 'close',
					wx_chant_independent: 'open'
				},
				userInfo: {},
				publishStats: {
					total: 0,
					onSale: 0,
					soldOut: 0,
					offShelf: 0
				},
 				}
 			},
		onLoad() {
			app.globalData.theme = this.$Cache.get('theme')
			if (app.globalData.isIframe) {
				setTimeout(() => {
					let active;
					document.getElementById('pageIndex').children.forEach(dom => {
						dom.addEventListener('click', (e) => {
							e.stopPropagation();
							e.preventDefault();
							if (dom === active) return;
							dom.classList.add('borderShow');
							active && active.classList.remove('borderShow');
							active = dom;
						})
					})
				});
			}
			let that = this;
			// #ifdef H5 || APP-PLUS
			that.$set(that, 'pageHeight', app.globalData.windowHeight);
			// #endif
			that.$set(that, 'MyMenus', app.globalData.MyMenus);
			that.$set(that, 'chatConfig', Cache.getItem('chatConfig'));
			// #ifdef H5
			that.shareApi();
			// #endif
			that.bgColor = setThemeColor();
			// #ifdef APP-PLUS
			setTimeout(() => {
				uni.setNavigationBarColor({
					frontColor: '#ffffff',
					backgroundColor: that.bgColor,
				});
			}, 500)
			// #endif
			// #ifdef MP
			uni.setNavigationBarColor({
				frontColor: '#ffffff',
				backgroundColor: that.bgColor,
			});
			// #endif
		},
		onShow: function() {
			this.getMyMenus();
			this.getTokenIsExist();
			this.getPublishStats();
			this.theme = this.$Cache.get('theme')
			app.globalData.theme = this.$Cache.get('theme')
			if (!this.$Cache.getItem('cityList')) getCityList();
			!this.$store.state.app.bottomNavigationIsCustom && uni.showTabBar();
			// #ifdef H5
			let that = this;
			uni.getSystemInfo({
				success: function(res) {
					that.pageHeight = res.windowHeight + 'px'
				}
			});
			// #endif
		},
		methods: {
			getTokenIsExist() {
				tokenIsExistApi().then(res => {
					let tokenIsExist = res.data;
					if (this.isLogin && tokenIsExist) {
						this.getOrderData();
						this.$store.dispatch('USERINFO').then(res => {
							this.userInfo = res;
						});
					} else {
						this.$store.commit("LOGOUT");
						this.$store.commit('UPDATE_LOGIN', '');
						this.$store.commit('UPDATE_USERINFO', {});
					}
				})
			},
			bindEdit(name) {
				if (app.globalData.isIframe) {
					window.parent.postMessage({ name: name }, '*');
					return;
				}
			},
			menusTap(url) {
				if (!this.isLogin) {
					this.openAuto();
				} else {
					goPage().then(res => {
						uni.navigateTo({
							animationType: animationType.type,
							animationDuration: animationType.duration,
							url: url
						})
					})
				}
			},
			goMyPublish() {
				if (this.isLogin) {
					uni.navigateTo({
						url: '/pages/secondhand/mylist'
					});
				} else {
					this.openAuto();
				}
			},
			goPublish() {
				uni.switchTab({ url: '/pages/secondhand/publish' });
			},
			getPublishStats() {
				if (this.isLogin) {
					getMyPublishStats().then(res => {
						if (res.data) {
							this.publishStats = res.data;
						}
					}).catch(err => {
						console.log('获取发布统计失败', err);
					});
				}
			},
			onClickService() {
				if (this.chatConfig.telephone_service_switch === 'open') {
					uni.makePhoneCall({ phoneNumber: this.chatConfig.consumer_hotline });
				} else {
					// #ifdef APP-PLUS
					uni.navigateTo({
						animationType: animationType.type,
						animationDuration: animationType.duration,
						url: '/pages/users/web_page/index?webUel=' + this.chatUrl + '&title=客服'
					})
					// #endif
					// #ifndef APP-PLUS
					if (!app.globalData.isIframe) {
						location.href = this.chatUrl;
					} else {
						return false
					}
					// #endif
				}
			},
			wxChatService() {
				let chatUrlArr = this.chatUrl.split('?')
				uni.navigateTo({
					url: `/pages/users/web_page/index?webUel=${chatUrlArr[0]}&title=客服&${chatUrlArr[1]}`
				})
			},
			getOrderData() {
				let that = this;
				orderData().then(res => {
					that.orderMenu.forEach((item, index) => {
						switch (item.title) {
							case '待付款':
								item.num = res.data.unPaidCount
								break
							case '待发货':
								item.num = res.data.unShippedCount
								break
							case '待收货':
								item.num = res.data.receivedCount
								break
							case '待评价':
								item.num = res.data.evaluatedCount
								break
							case '售后/退款':
								item.num = res.data.refundCount
								break
						}
					})
					that.$set(that, 'orderMenu', that.orderMenu);
				})
			},
			openAuto() {
				Cache.set(BACK_URL, '')
				toLogin();
			},
			bindPhone() {
				uni.navigateTo({
					animationType: animationType.type,
					animationDuration: animationType.duration,
					url: '/pages/users/app_login/index'
				})
			},
			getMyMenus: function() {
				let that = this;
				getMenuList().then(res => {
					that.$set(that, 'MyMenus', res.data.routine_my_menus);
					that.wechatUrl = res.data.routine_my_menus.filter((item) => {
						return item.url.indexOf('service') !== -1
					})
					res.data.routine_my_menus.map((item) => {
						if (item.url.indexOf('service') !== -1) that.servicePic = item.pic
					})
					if (res.data.routine_my_banner) {
						that.imgUrls = res.data.routine_my_banner
					}
				}).catch(err => {
					console.log(err);
				});
			},
			goEdit() {
				if (this.isLogin == false) {
					this.openAuto();
				} else {
					uni.navigateTo({
						animationType: animationType.type,
						animationDuration: animationType.duration,
						url: '/pages/infos/user_info/index'
					})
				}
			},
			goMenuPage(url) {
				if (this.isLogin) {
					uni.navigateTo({
						animationType: animationType.type,
						animationDuration: animationType.duration,
						url
					})
				} else {
					this.openAuto()
				}
			},
			appUpdate() {
				uni.navigateTo({
					url: '/pages/users/app_update/app_update',
					animationType: animationType.type,
					animationDuration: animationType.duration,
				})
			},
			shareApi: function() {
				getShare().then(res => {
					this.$set(this, 'configApi', res.data);
					// #ifdef H5
					this.setOpenShare(res.data);
					// #endif
				})
			},
			setOpenShare: function(data) {
				let that = this;
				if (that.$wechat.isWeixin()) {
					let configAppMessage = {
						desc: data.synopsis,
						title: data.title,
						link: location.href,
						imgUrl: data.img
					};
					that.$wechat.wechatEvevt(["updateAppMessageShareData", "updateTimelineShareData"],
						configAppMessage);
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	/* ===== 转享 Design System: Profile ===== */
		$primary: #FF6B35;
		$primary-light: #FFF8F3;
		$secondary: #FFB627;
		$accent: #FF6B35;
		$fg: #1A1A2E;
		$muted: #FFF8F3;
		$radius: 16rpx;
		$radius-lg: 24rpx;

	page,
	body {
		height: 100%;
		background: $primary-light;
	}

	.crmeb-profile {
		display: flex;
		flex-direction: column;
		height: 100%;
	}

	.crmeb-profile__scroll {
		flex: 1;
		overflow-y: auto;
	}

	/* ===== Header Card ===== */
		.crmeb-profile__header {
			background: linear-gradient(135deg, $primary, $secondary);
			padding: 0 32rpx 40rpx;
			border-radius: 0 0 32rpx 32rpx;
		}

		.crmeb-profile__card {
			padding: 36rpx 0 24rpx;
		}

	.crmeb-profile__user {
		display: flex;
		align-items: center;
	}

	.crmeb-profile__avatar {
			width: 128rpx;
			height: 128rpx;
			border-radius: 50%;
			border: 4rpx solid rgba(255, 255, 255, 0.4);
			flex-shrink: 0;
			box-shadow: 0 4rpx 16rpx rgba(255, 107, 53, 0.25);
		}

	.crmeb-profile__info {
		flex: 1;
		margin-left: 20rpx;
		position: relative;
	}

	.crmeb-profile__name {
		display: flex;
		align-items: center;
		color: #fff;
		font-size: 34rpx;
		font-weight: 700;
	}

	.crmeb-profile__vip {
		display: flex;
		align-items: center;
		padding: 4rpx 16rpx;
		background: rgba(0, 0, 0, 0.2);
		border-radius: 24rpx;
		margin-left: 12rpx;
	}

	.crmeb-profile__vip image {
		width: 27rpx;
		height: 27rpx;
	}

	.crmeb-profile__vip-txt {
		font-size: 20rpx;
		color: #fff;
		margin-left: 8rpx;
	}

	.crmeb-profile__phone-row {
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.7);
		margin-top: 6rpx;
	}

	.crmeb-profile__bind-phone {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.8);
		margin-top: 6rpx;
		text-decoration: underline;
	}

	.crmeb-profile__settings {
		position: absolute;
		right: 0;
		top: 0;
		font-size: 40rpx;
		color: #fff;
	}

	/* Stats */
	.crmeb-profile__stats {
		display: flex;
		margin-top: 36rpx;
	}

	.crmeb-profile__stat-item {
		flex: 1;
		text-align: center;
		color: #fff;
	}

	.crmeb-profile__stat-num {
			font-size: 38rpx;
			font-weight: 700;
			font-feature-settings: 'tnum';
		}

	.crmeb-profile__stat-label {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.7);
		margin-top: 6rpx;
	}

	/* ===== Orders ===== */
		.crmeb-profile__orders {
			background: #fff;
			border-radius: $radius-lg;
			padding: 32rpx 28rpx;
				margin-top: 40rpx;
			position: relative;
			z-index: 2;
			box-shadow: 0 8rpx 32rpx rgba(255, 107, 53, 0.1);
		}

	.crmeb-profile__orders-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 32rpx;
	}

	.crmeb-profile__orders-title {
		font-size: 30rpx;
		font-weight: 600;
		color: $fg;
	}

	.crmeb-profile__orders-all {
		font-size: 26rpx;
		color: #64748b;
	}

	.crmeb-profile__orders-grid {
		display: flex;
		justify-content: space-between;
	}

	.crmeb-profile__order-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		width: 20%;
	}

	.crmeb-profile__order-icon {
		position: relative;
		text-align: center;
	}

	.crmeb-profile__order-icon .iconfont {
		font-size: 44rpx;
		color: $primary;
	}

	.crmeb-profile__order-num {
		position: absolute;
		right: -16rpx;
		top: -12rpx;
		min-width: 28rpx;
		background: #fff;
		color: $primary;
		border-radius: 20rpx;
		font-size: 20rpx;
		padding: 0 8rpx;
		text-align: center;
		border: 1rpx solid $primary;
		font-weight: 600;
	}

	.crmeb-profile__order-label {
		font-size: 24rpx;
		color: #454545;
		margin-top: 12rpx;
	}

	/* ===== Body ===== */
		.crmeb-profile__body {
			padding: 24rpx 24rpx 30rpx;
		}

		/* My Publish */
		.crmeb-profile__publish {
			background: #fff;
			border-radius: $radius-lg;
			padding: 32rpx 28rpx;
			box-shadow: 0 4rpx 20rpx rgba(255, 107, 53, 0.06);
			margin-bottom: 24rpx;
		}

		.crmeb-profile__publish-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 24rpx;
		}

		.crmeb-profile__publish-title {
			font-size: 30rpx;
			font-weight: 600;
			color: $fg;
		}

		.crmeb-profile__publish-all {
			font-size: 24rpx;
			color: #999;
		}

		.crmeb-profile__publish-stats {
			display: flex;
			justify-content: space-around;
			padding: 20rpx 0;
			background: $primary-light;
			border-radius: $radius;
			margin-bottom: 24rpx;
		}

		.crmeb-profile__publish-stat-item {
			display: flex;
			flex-direction: column;
			align-items: center;
		}

		.crmeb-profile__publish-stat-num {
			font-size: 36rpx;
			font-weight: 700;
			color: $fg;
			margin-bottom: 8rpx;
		}

		.crmeb-profile__publish-stat-num.on-sale {
			color: #4CAF50;
		}

		.crmeb-profile__publish-stat-num.sold-out {
			color: #9E9E9E;
		}

		.crmeb-profile__publish-stat-num.off-shelf {
			color: #FF9800;
		}

		.crmeb-profile__publish-stat-label {
			font-size: 22rpx;
			color: #666;
		}

		.crmeb-profile__publish-action {
			display: flex;
			justify-content: center;
		}

		.publish-btn {
			background: linear-gradient(135deg, $primary 0%, #FF4444 100%);
			color: #fff;
			border-radius: 50rpx;
			font-size: 28rpx;
			padding: 0 40rpx;
			height: 72rpx;
			line-height: 72rpx;
			border: none;
			box-shadow: 0 6rpx 16rpx rgba(255, 107, 53, 0.3);
			display: flex;
			align-items: center;
			gap: 8rpx;
		}

		.publish-btn .btn-icon {
			font-size: 32rpx;
			font-weight: bold;
		}

		/* Services */
		.crmeb-profile__services {
			background: #fff;
			border-radius: $radius-lg;
			padding: 32rpx 28rpx;
			box-shadow: 0 4rpx 20rpx rgba(255, 107, 53, 0.06);
		}

	.crmeb-profile__services-title {
		font-size: 30rpx;
		font-weight: 600;
		color: $fg;
		margin-bottom: 24rpx;
	}

	.crmeb-profile__services-grid {
		display: flex;
		flex-wrap: wrap;
	}

	.crmeb-profile__service-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		width: 25%;
		margin-bottom: 36rpx;
		font-size: 26rpx;
		color: #333;
		background: none;
		border: none;
		padding: 0;
	}

	.crmeb-profile__service-item::after {
		border: none;
	}

	.crmeb-profile__service-icon {
			width: 56rpx;
			height: 56rpx;
			margin-bottom: 12rpx;
			border-radius: 14rpx;
		}

		.crmeb-profile__service-name {
			font-size: 24rpx;
			color: #333;
		}
	</style>
