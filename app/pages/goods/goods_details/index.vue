<template>
	<view :data-theme="theme">
		<skeleton :show="showSkeleton" :isNodes="isNodes" ref="skeleton" loading="chiaroscuro" selector="skeleton" bgcolor="#FFF"></skeleton>
		<view class="crmeb-detail skeleton" :style="{visibility: showSkeleton ? 'hidden' : 'visible'}">
			<!-- Navbar -->
			<view class="crmeb-detail__navbar" :class="opacity>0.6?'crmeb-detail__navbar--solid':''">
				<view class="crmeb-detail__navbar-inner" :style="'height:'+navH+'rpx;'">
					<view class="crmeb-detail__navbar-row">
						<!-- #ifdef MP -->
						<view class="crmeb-detail__nav-left" id="home" :style="{ top: homeTop + 'rpx' }">
							<text class="crmeb-detail__nav-btn" @tap="returns">&#8592;</text>
							<text class="crmeb-detail__nav-btn" @tap="showNav">&#8942;</text>
						</view>
						<!-- #endif -->
						<!-- #ifdef H5 || APP-PLUS -->
						<view class="crmeb-detail__nav-back" :class="opacity>0.5?'on':''" :style="{ top: homeTop + 'rpx' }" v-if="returnShow" @tap="returns">&#8592;</view>
						<!-- #endif -->
						<!-- #ifdef H5 || APP-PLUS -->
						<view class="crmeb-detail__nav-tabs" v-show="opacity > 0.6">
							<view class="crmeb-detail__nav-tab" :class="navActive === index ? 'on' : ''" v-for="(item,index) in navList" :key='index' @tap="tap(index)">{{ item }}</view>
						</view>
						<!-- #endif -->
						<!-- #ifdef H5 || APP-PLUS -->
						<view class="crmeb-detail__nav-more" :style="{ top: homeTop + 'rpx' }" @tap="showNav">&#8942;</view>
						<!-- #endif -->
					</view>
				</view>
			</view>
			<view class="crmeb-detail__dropdown" v-show="currentPage" :style="{ top: navH + 'rpx' }">
				<view class="crmeb-detail__dropdown-item" v-for="(item,index) in selectNavList" :key="index" @click="linkPage(item.url)">
					<text class="iconfont" :class="item.icon"></text>
					<text class="crmeb-detail__dropdown-label">{{item.name}}</text>
				</view>
			</view>
			<view class="crmeb-detail__scroll-wrap">
				<scroll-view :scroll-top="scrollTop" scroll-y="true" scroll-with-animation="true" :style="'height:'+height+'px;'" @scroll="scroll">
					<view id="past0">
						<productConSwiper :imgUrls="sliderImage" :videoline="videoLink" @videoPause="videoPause"></productConSwiper>
						<activity-style v-if="productInfo.activityStyle" :productInfo="productInfo"></activity-style>
						<view class="crmeb-detail__section">
							<!-- Price Card -->
							<view class="crmeb-detail__price-card">
								<view class="crmeb-detail__price-row">
									<view class="crmeb-detail__price" v-if="!productInfo.activityStyle">
										<text class="crmeb-detail__price-sign">￥</text>
										<text class="crmeb-detail__price-num">{{attr.productSelect.price}}</text>
										<view class="crmeb-detail__vip-badge" v-if="attr.productSelect.vipPrice && attr.productSelect.vipPrice > 0">
											<image :src="urlDomain+'crmebimage/perset/staticImg/vip_badge.png'" class="crmeb-detail__vip-icon" />
											<text>￥{{attr.productSelect.vipPrice}}</text>
										</view>
									</view>
									<view class="crmeb-detail__share-btn" @click="listenerActionSheet">&#x21B1;</view>
								</view>
								<view class="crmeb-detail__title">{{productInfo.storeName}}</view>
								<view class="crmeb-detail__meta">
									<text>原价:￥{{attr.productSelect.otPrice || 0}}</text>
									<text>库存:{{productInfo.stock || 0}}{{productInfo.unitName || ''}}</text>
									<text>销量:{{Math.floor(productInfo.sales) + Math.floor(productInfo.ficti) || 0}}{{productInfo.unitName || ''}}</text>
								</view>
								<view class="crmeb-detail__coupon" v-if="defaultCoupon.length>0 && type=='normal'" @click="couponTap">
									<text>优惠券：</text>
									<text class="crmeb-detail__coupon-tag">满{{defaultCoupon[0].minPrice}}减{{defaultCoupon[0].money}}</text>
									<text class="crmeb-detail__arrow">&#8250;</text>
								</view>
								<view class="crmeb-detail__activity" v-if="activityH5.length">
									<text>活动：</text>
									<view class="crmeb-detail__activity-tags">
										<view class="crmeb-detail__activity-tag seckill" v-for="(item,index) in activityH5" :key="index" @click="goActivity(item)" v-if="item.type === '1'">秒杀</view>
										<view class="crmeb-detail__activity-tag bargain" v-for="(item,index) in activityH5" :key="index" @click="goActivity(item)" v-if="item.type === '2'">砍价</view>
										<view class="crmeb-detail__activity-tag group" v-for="(item,index) in activityH5" :key="index" @click="goActivity(item)" v-if="item.type === '3'">拼团</view>
									</view>
								</view>
							</view>
							<!-- Select Specs -->
							<view class="crmeb-detail__specs" @click="selecAttr">
								<view class="crmeb-detail__specs-row">
									<text class="crmeb-detail__specs-label">{{attrTxt}}：</text>
									<text class="crmeb-detail__specs-value">{{attrValue || '请选择商品规格'}}</text>
									<text class="crmeb-detail__arrow">&#8250;</text>
								</view>
								<view class="crmeb-detail__specs-thumbs" v-if="skuArr.length > 1">
									<image :src="item.image" v-for="(item,index) in skuArr.slice(0,4)" :key="index" class="crmeb-detail__specs-thumb" />
									<text class="crmeb-detail__specs-count">共{{skuArr.length}}种规格</text>
								</view>
							</view>
							<!-- Reviews -->
							<view class="crmeb-detail__reviews" id="past1">
								<view class="crmeb-detail__reviews-header">
									<text>用户评价 <text class="crmeb-detail__reviews-count">({{replyCount}})</text></text>
									<navigator class="crmeb-detail__reviews-link" hover-class="none" :url="'/pages/goods/goods_comment_list/index?productId='+id">好评 {{replyChance || 0}}% &#8250;</navigator>
								</view>
								<userEvaluation v-if="replyCount" :reply="reply"></userEvaluation>
							</view>
							<!-- Recommendations -->
							<view class="crmeb-detail__recommend" v-if="good_list.length" id="past2">
								<view class="crmeb-detail__recommend-title">优品推荐</view>
								<swiper indicator-dots="true" :autoplay="autoplay" :circular="circular" :interval="interval" :duration="duration" indicator-color="#999" :indicator-active-color="indicatorBg" :style="'height:'+clientHeight+'px'">
									<swiper-item v-for="(item,indexw) in good_list" :key="indexw">
										<view class="crmeb-detail__recommend-grid" :id="'list'+indexw">
											<view class="crmeb-detail__recommend-item" v-for="(val,indexn) in item.list" :key="indexn" @click="goDetail(val)">
												<view class="crmeb-detail__recommend-img">
													<image :src="val.image" />
													<text class="crmeb-detail__recommend-badge" v-if="val.activityH5 && val.activityH5.type === '1'">秒杀</text>
													<text class="crmeb-detail__recommend-badge bargain" v-if="val.activityH5 && val.activityH5.type === '2'">砍价</text>
													<text class="crmeb-detail__recommend-badge group" v-if="val.activityH5 && val.activityH5.type === '3'">拼团</text>
												</view>
												<view class="crmeb-detail__recommend-name line1">{{val.storeName}}</view>
												<view class="crmeb-detail__recommend-price">¥{{val.price}}</view>
											</view>
										</view>
									</swiper-item>
								</swiper>
							</view>
						</view>
					</view>
					<!-- Product Details -->
					<view class="crmeb-detail__desc" id="past3">
						<view class="crmeb-detail__desc-title">产品详情</view>
						<view class="crmeb-detail__desc-content">
							<jyf-parser :html="description" ref="article" :tag-style="tagStyle"></jyf-parser>
						</view>
					</view>
					<view style="height:120rpx;"></view>
				</scroll-view>
			</view>
			<!-- Footer -->
			<view class="crmeb-detail__footer">
				<!-- #ifdef MP -->
				<button class="crmeb-detail__footer-btn" @click="onClickService" v-if="chatConfig.telephone_service_switch === 'open'">
					<text>&#9742;</text>
					<text>客服</text>
				</button>
				<template v-else>
					<button class="crmeb-detail__footer-btn" open-type="contact" v-if="chatConfig.wx_chant_independent=='open'">
						<text>&#9742;</text>
						<text>客服</text>
					</button>
					<button class="crmeb-detail__footer-btn" @click="wxChatService" v-else>
						<text>&#9742;</text>
						<text>客服</text>
					</button>
				</template>
				<!-- #endif -->
				<!-- #ifndef MP -->
				<view class="crmeb-detail__footer-btn" @click="onClickService">
					<text>&#9742;</text>
					<text>客服</text>
				</view>
				<!-- #endif -->
				<view class="crmeb-detail__footer-btn" @click="setCollect" v-if="type === 'normal'">
					<text>{{ userCollect ? '&#9829;' : '&#9825;' }}</text>
					<text>收藏</text>
				</view>
				<navigator class="crmeb-detail__footer-btn" open-type="switchTab" url="/pages/order_addcart/order_addcart" hover-class="none" v-if="type === 'normal'">
					<text>&#128722;</text>
					<text>购物车</text>
				</navigator>
				<view class="crmeb-detail__footer-actions" v-if="type === 'normal'">
					<view v-if="attr.productSelect.stock <= 0">
						<form @submit="joinCart" report-submit="true"><button class="crmeb-detail__action-btn disabled" form-type="submit">已售罄</button></form>
					</view>
					<view class="crmeb-detail__action-group" v-else>
						<form @submit="joinCart" report-submit="true"><button class="crmeb-detail__action-btn cart" form-type="submit">加入购物车</button></form>
						<form @submit="goBuy" report-submit="true"><button class="crmeb-detail__action-btn buy" form-type="submit">立即购买</button></form>
					</view>
				</view>
				<view class="crmeb-detail__footer-actions" v-if="type === 'video'">
					<form @submit="goBuy" report-submit="true" v-if="attr.productSelect.stock > 0"><button class="crmeb-detail__action-btn buy" form-type="submit">立即购买</button></form>
					<form report-submit="true" v-else><button class="crmeb-detail__action-btn disabled" form-type="submit">已售罄</button></form>
				</view>
			</view>
			<!-- Modals -->
			<shareRedPackets :sharePacket="sharePacket" @listenerActionSheet="listenerActionSheet" @showShare="showShare"></shareRedPackets>
			<productWindow :attr="attr" :isShow="1" :iSplus="1" @myevent="onMyEvent" @ChangeAttr="ChangeAttr" @ChangeCartNum="ChangeCartNum" @attrVal="attrVal" @iptCartNum="iptCartNum" id="product-window" @getImg="showImg"></productWindow>
			<couponListWindow :coupon="coupon" :typeNum="couponDeaultType[0].useType" @ChangCouponsClone="ChangCouponsClone" @ChangCoupons="ChangCoupons" @ChangCouponsUseState="ChangCouponsUseState" @tabCouponType="tabCouponType"></couponListWindow>
			<view class="crmeb-detail__share-sheet" :class="posters ? 'on' : ''">
				<view class="crmeb-detail__share-grid">
					<button class="crmeb-detail__share-item" hover-class="none" v-if="weixinStatus === true" @click="H5ShareBox = true">
						<image :src="urlDomain+'crmebimage/perset/staticImg/weixin.png'" class="crmeb-detail__share-icon" />
						<text>分享给好友</text>
					</button>
					<button class="crmeb-detail__share-item" open-type="share" hover-class="none">
						<image :src="urlDomain+'crmebimage/perset/staticImg/weixin.png'" class="crmeb-detail__share-icon" />
						<text>分享给好友</text>
					</button>
					<view class="crmeb-detail__share-item" @click="getpreviewImage">
						<image :src="urlDomain+'crmebimage/perset/staticImg/changan.png'" class="crmeb-detail__share-icon" />
						<text>预览发图</text>
					</view>
					<button class="crmeb-detail__share-item" hover-class="none" @click="savePosterPath">
						<image :src="urlDomain+'crmebimage/perset/staticImg/haibao.png'" class="crmeb-detail__share-icon" />
						<text>保存海报</text>
					</button>
				</view>
				<view class="crmeb-detail__share-cancel" @click="posterImageClose">取消</view>
			</view>
			<cus-previewImg ref="cusPreviewImg" :list="skuArr" @changeSwitch="changeSwitch" @shareFriend="listenerActionSheet" />
			<view class="crmeb-detail__mask" v-if="posters" @click="closePosters"></view>
			<view class="crmeb-detail__mask" v-if="canvasStatus"></view>
			<view class="crmeb-detail__mask--transparent" v-if="currentPage" @touchmove="hideNav" @click="hideNav()"></view>
			<view class="crmeb-detail__poster" v-if="canvasStatus">
				<image :src="imagePath" />
			</view>
			<view class="crmeb-detail__canvas">
				<canvas style="width:750px;height:1190px;" canvas-id="firstCanvas"></canvas>
				<canvas canvas-id="qrcode" :style="{width: qrcodeSize+'px', height: qrcodeSize+'px'}" />
			</view>
			<view class="crmeb-detail__h5-share" v-if="H5ShareBox">
				<image :src="urlDomain+'crmebimage/perset/staticImg/share-info.png'" @click="H5ShareBox = false" />
			</view>
		</view>
	</view>
</template>
