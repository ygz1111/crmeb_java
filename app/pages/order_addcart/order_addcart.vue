<template>
	<view :data-theme="theme">
		<view class="crmeb-cart">
			<!-- Header -->
			<view class="crmeb-cart__header">
				<view class="crmeb-cart__header-inner">
					<view class="crmeb-cart__nav-title">购物车</view>
					<view class="crmeb-cart__nav-more" @tap="showNav">•••</view>
				</view>
			</view>
			<view class="crmeb-cart__dropdown" v-show="currentPage">
				<view class="crmeb-cart__dropdown-item" v-for="(item,index) in selectNavList" :key="index" @click="linkPage(item.url)">
					<text class="iconfont" :class="item.icon"></text>
					<text class="pl-20">{{item.name}}</text>
				</view>
			</view>
			<!-- Body -->
			<view class="crmeb-cart__body" :style="'top:'+navH+'rpx;'" @touchstart="touchStart">
				<!-- Guarantee Bar -->
				<view class="crmeb-cart__guarantee">
					<text class="crmeb-cart__guarantee-item">&#10003; 100%正品保证</text>
					<text class="crmeb-cart__guarantee-item">&#10003; 精挑细选</text>
					<text class="crmeb-cart__guarantee-item">&#10003; 售后无忧</text>
				</view>
				<!-- Cart Content -->
				<view class="crmeb-cart__content" v-if="(cartList.valid.length > 0 || cartList.invalid.length > 0)">
					<!-- Cart Header -->
					<view class="crmeb-cart__toolbar">
						<text>购物数量 <text class="crmeb-cart__count">{{cartCount}}</text></text>
						<view class="crmeb-cart__manage" @click="manage">{{ footerswitch ? '管理' : '取消'}}</view>
					</view>
					<!-- Valid Items -->
					<view class="crmeb-cart__items">
						<checkbox-group @change="checkboxChange">
							<view class="crmeb-cart__item" v-for="(item,index) in cartList.valid" :key="index">
								<checkbox :value="(item.id).toString()" :checked="item.checked" :disabled="!item.attrStatus && footerswitch" />
								<navigator :url="'/pages/goods/goods_details/index?id='+item.productId" hover-class="none" class="crmeb-cart__item-body">
									<view class="crmeb-cart__item-img">
										<image :src="item.image" />
									</view>
									<view class="crmeb-cart__item-info">
										<view class="crmeb-cart__item-name" :class="item.attrStatus?'':'disabled'">{{item.storeName}}</view>
										<view class="crmeb-cart__item-spec" v-if="item.suk">{{item.suk}}</view>
										<view class="crmeb-cart__item-price" v-if="item.attrStatus">￥{{item.vipPrice ? item.vipPrice : item.price}}</view>
										<view class="crmeb-cart__item-reselect" v-else>
											<text class="crmeb-cart__reselect-tip">请重新选择商品规格</text>
											<text class="crmeb-cart__reselect-btn" @click.stop="reElection(item)">重选</text>
										</view>
									</view>
									<view class="crmeb-cart__item-qty" v-if="item.attrStatus">
										<view class="crmeb-cart__qty-btn" :class="item.numSub ? 'disabled' : ''" @click.stop="subCart(index)">-</view>
										<view class="crmeb-cart__qty-num">{{item.cartNum}}</view>
										<view class="crmeb-cart__qty-btn" :class="item.numAdd ? 'disabled' : ''" @click.stop="addCart(index)">+</view>
									</view>
								</navigator>
							</view>
						</checkbox-group>
					</view>
					<!-- Invalid Items -->
					<view class="crmeb-cart__invalid" v-if="cartList.invalid.length > 0">
						<view class="crmeb-cart__invalid-header" @click="goodsOpen">
							<text class="crmeb-cart__invalid-toggle">{{ goodsHidden ? '&#9660;' : '&#9650;' }}</text>
							<text>失效商品</text>
							<text class="crmeb-cart__invalid-clear" @click.stop="unsetCart">&#128465; 清空</text>
						</view>
						<view class="crmeb-cart__invalid-list" :hidden="goodsHidden">
							<view class="crmeb-cart__invalid-item" v-for="(item,index) in cartList.invalid" :key="index">
								<text class="crmeb-cart__invalid-badge">失效</text>
								<view class="crmeb-cart__item-body">
									<view class="crmeb-cart__item-img">
										<image :src="item.image" />
									</view>
									<view class="crmeb-cart__item-info">
										<view class="crmeb-cart__item-name disabled">{{item.storeName}}</view>
										<view class="crmeb-cart__item-spec" v-if="item.suk">{{item.suk}}</view>
										<text class="crmeb-cart__invalid-tag">该商品已失效</text>
									</view>
								</view>
							</view>
						</view>
					</view>
					<view class="crmeb-cart__loading" v-if="cartList.invalid.length&&loadend">
						<text>{{loadTitleInvalid}}</text>
					</view>
				</view>
				<!-- Empty -->
				<view class="crmeb-cart__empty" v-if="(cartList.valid.length == 0 && cartList.invalid.length == 0 && canShow) || !isLogin">
					<view class="crmeb-cart__empty-img">
						<image :src="urlDomain+'crmebimage/perset/staticImg/noCart.png'" />
					</view>
					<recommend ref="recommendIndex"></recommend>
				</view>
			</view>
			<!-- Footer -->
			<view class="crmeb-cart__footer" v-if="cartList.valid.length > 0" :class="bottomNavigationIsCustom?'bottom-custom':''">
				<checkbox-group @change="checkboxAllChange">
					<checkbox value="all" :checked="!!isAllSelect" />
					<text class="crmeb-cart__select-all">全选({{selectValue.length}})</text>
				</checkbox-group>
				<view class="crmeb-cart__footer-right" v-if="footerswitch==true">
					<text class="crmeb-cart__total">￥{{selectCountPrice}}</text>
					<form @submit="subOrder" report-submit="true">
						<button class="crmeb-cart__checkout" formType="submit">立即下单</button>
					</form>
				</view>
				<view class="crmeb-cart__footer-actions" v-else>
					<form @submit="subCollect" report-submit="true">
						<button class="crmeb-cart__action-btn collect" formType="submit">收藏</button>
					</form>
					<form @submit="subDel" report-submit="true">
						<button class="crmeb-cart__action-btn delete" formType="submit">删除</button>
					</form>
				</view>
			</view>
			<productWindow :attr="attr" :isShow="1" :iSplus="1" :iScart="1" @myevent="onMyEvent" @ChangeAttr="ChangeAttr" @ChangeCartNum="ChangeCartNum" @attrVal="attrVal" @iptCartNum="iptCartNum" @goCat="reGoCat" id="product-window"></productWindow>
			<pageFooter></pageFooter>
		</view>
	</view>
</template>

<script>
	import pageFooter from '@/components/pageFooter/index.vue'
	// #ifdef APP-PLUS
	let sysHeight = uni.getSystemInfoSync().statusBarHeight + 'px';
	// #endif
	// #ifndef APP-PLUS
	let sysHeight = 0
	// #endif
	import {
		getCartList,
		getCartCounts,
		changeCartNum,
		cartDel,
		getResetCart
	} from '@/api/order.js';
	import {
		tokenIsExistApi
	} from '@/api/api.js';
	import {
		collectAll,
		getProductDetail
	} from '@/api/store.js';
	import {getShare} from '@/api/public.js';
	import {mapGetters} from "vuex";
	import recommend from '@/components/recommend';
	import productWindow from '@/components/productWindow';
	import animationType from '@/utils/animationType.js'
	import {
		Debounce
	} from '@/utils/validate.js'
	let app = getApp();
	export default {
		components: {
			recommend,
			productWindow,
			pageFooter
		},
		data() {
			return {
				urlDomain: this.$Cache.get("imgHost"),
				cartCount: 0,
				goodsHidden: false,
				footerswitch: true,
				hostProduct: [],
				cartList: {
					valid: [],
					invalid: []
				},
				isAllSelect: false,
				selectValue: [],
				selectCountPrice: 0.00,
				isAuto: false,
				isShowAuth: false,
				hotScroll: false,
				hotPage: 1,
				hotLimit: 10,
				loading: false,
				loadend: false,
				loadTitle: '加载更多',
				page: 1,
				limit: 20,
				loadingInvalid: false,
				loadendInvalid: false,
				loadTitleInvalid: '加载更多',
				pageInvalid: 1,
				limitInvalid: 20,
				attr: {
					cartAttr: false,
					productAttr: [],
					productSelect: {}
				},
				productValue: [],
				productInfo: {},
				attrValue: '',
				attrTxt: '请选择',
				cartId: 0,
				product_id: 0,
				sysHeight: sysHeight,
				canShow: false,
				configApi: {},
				theme:app.globalData.theme,
				navH:"",
				homeTop: 20,
				currentPage:false,
				selectNavList:[
					{name:'首页',icon:'icon-shouye8',url:'/pages/index/index'},
					{name:'搜索',icon:'icon-sousuo6',url:'/pages/goods/goods_search/index'},
					{name:'我的收藏',icon:'icon-shoucang3',url:'/pages/users/user_goods_collection/index'},
					{name:'个人中心',icon:'icon-gerenzhongxin1',url:'/pages/user/index'},
				],
				tokenIsExist: false,
			};
		},
		computed: mapGetters(['isLogin','bottomNavigationIsCustom']),
		onLoad: function(options) {
			this.getTokenIsExist();
			// #ifdef MP
			this.navH = app.globalData.navHeight;
			// #endif
			// #ifndef MP
			this.navH = 96;
			// #endif
			// #ifdef H5
			this.shareApi();
			// #endif
		},
		onReady() {
			this.$nextTick(function() {
				// #ifdef MP
				const menuButton = uni.getMenuButtonBoundingClientRect();
				const query = uni.createSelectorQuery().in(this);
				query.select('#home').boundingClientRect(data => {
					this.homeTop = menuButton.top * 2 + menuButton.height - data.height;
				}).exec();
				// #endif
			});
		},
		onShow: function() {
			this.canShow = false
			if (this.isLogin && this.tokenIsExist) {
				this.getIndex();
			};
		},
		methods: {
			getTokenIsExist() {
				tokenIsExistApi().then(res => {
					this.tokenIsExist = res.data;
					this.canShow = true;
					if (this.isLogin && this.tokenIsExist) {
						this.getIndex();
					}
				})
			},
			getIndex(){
				this.hotPage = 1;
				this.hostProduct = [],
				this.hotScroll = false,
				this.loadend = false;
				this.page = 1;
				this.cartList.valid = [];
				this.getCartList();
				this.loadendInvalid = false;
				this.pageInvalid = 1;
				this.cartList.invalid = [];
				this.getInvalidList();
				this.footerswitch = true;
				this.hotScroll = false;
				this.hotPage = 1;
				this.hotLimit = 10;
				this.cartList = { valid: [], invalid: [] },
				this.isAllSelect = false;
				this.selectValue = [];
				this.selectCountPrice = 0.00;
				this.cartCount = 0;
				this.isShowAuth = false;
			},
			reGoCat: function() {
				let that = this, productSelect = that.productValue[this.attrValue];
				if (that.attr.productAttr.length && productSelect === undefined)
					return that.$util.Tips({ title: "产品库存不足，请选择其它" });
				let q = {
					id: that.cartId,
					productId: that.product_id,
					num: that.attr.productSelect.cart_num,
					unique: that.attr.productSelect !== undefined ? that.attr.productSelect.unique : that.productInfo.id
				};
				getResetCart(q).then(function(res) {
					that.attr.cartAttr = false;
					that.$util.Tips({ title: "添加购物车成功", success: () => {
						that.loadend = false;
						that.page = 1;
						that.cartList.valid = [];
						that.getCartList();
						that.getCartNum();
					}});
				}).catch(res => {
					return that.$util.Tips({ title: res });
				});
			},
			onMyEvent: function() { this.$set(this.attr, 'cartAttr', false); },
			reElection: function(item) { this.getGoodsDetails(item) },
			getGoodsDetails: function(item) {
				uni.showLoading({ title: '加载中', mask: true });
				let that = this;
				that.cartId = item.id;
				that.product_id = item.productId;
				getProductDetail(item.productId).then(res => {
					uni.hideLoading();
					that.attr.cartAttr = true;
					let productInfo = res.data.productInfo;
					that.$set(that, 'productInfo', productInfo);
					that.$set(that, 'productValue', res.data.productValue);
					let productAttr = res.data.productAttr.map(item => ({
						attrName: item.attrName,
						attrValues: item.attrValues.split(','),
						id:item.id, isDel:item.isDel,
						productId:item.productId, type:item.type
					}));
					this.$set(that.attr,'productAttr',productAttr);
					that.DefaultSelect();
				}).catch(err => { uni.hideLoading(); })
			},
			ChangeAttr: function(res) {
				let productSelect = this.productValue[res];
				if (productSelect && productSelect.stock > 0) {
					this.$set(this.attr.productSelect, "image", productSelect.image);
					this.$set(this.attr.productSelect, "price", productSelect.price);
					this.$set(this.attr.productSelect, "stock", productSelect.stock);
					this.$set(this.attr.productSelect, "unique", productSelect.id);
					this.$set(this.attr.productSelect, "cart_num", 1);
					this.$set(this, "attrValue", res);
					this.$set(this, "attrTxt", "已选择");
				} else {
					this.$set(this.attr.productSelect, "image", this.productInfo.image);
					this.$set(this.attr.productSelect, "price", this.productInfo.price);
					this.$set(this.attr.productSelect, "stock", 0);
					this.$set(this.attr.productSelect, "unique", this.productInfo.id);
					this.$set(this.attr.productSelect, "cart_num", 0);
					this.$set(this, "attrValue", "");
					this.$set(this, "attrTxt", "请选择");
				}
			},
			DefaultSelect: function() {
				let productAttr = this.attr.productAttr;
				let value = [];
				for (let key in this.productValue) {
					if (this.productValue[key].stock > 0) {
						value = this.attr.productAttr.length ? key.split(",") : [];
						break;
					}
				}
				for (let i = 0; i < productAttr.length; i++) {
					this.$set(productAttr[i], "index", value[i]);
				}
				let productSelect = this.productValue[value.sort().join(",")];
				if (productSelect && productAttr.length) {
					this.$set(this.attr.productSelect, "storeName", this.productInfo.storeName);
					this.$set(this.attr.productSelect, "image", productSelect.image);
					this.$set(this.attr.productSelect, "price", productSelect.price);
					this.$set(this.attr.productSelect, "stock", productSelect.stock);
					this.$set(this.attr.productSelect, "unique", productSelect.id);
					this.$set(this.attr.productSelect, "cart_num", 1);
					this.$set(this, "attrValue", value.sort().join(","));
					this.$set(this, "attrTxt", "已选择");
				} else if (!productSelect && productAttr.length) {
					this.$set(this.attr.productSelect, "storeName", this.productInfo.storeName);
					this.$set(this.attr.productSelect, "image", this.productInfo.image);
					this.$set(this.attr.productSelect, "price", this.productInfo.price);
					this.$set(this.attr.productSelect, "stock", 0);
					this.$set(this.attr.productSelect, "unique", this.productInfo.id);
					this.$set(this.attr.productSelect, "cart_num", 0);
					this.$set(this, "attrValue", "");
					this.$set(this, "attrTxt", "请选择");
				} else if (!productSelect && !productAttr.length) {
					this.$set(this.attr.productSelect, "storeName", this.productInfo.storeName);
					this.$set(this.attr.productSelect, "image", this.productInfo.image);
					this.$set(this.attr.productSelect, "price", this.productInfo.price);
					this.$set(this.attr.productSelect, "stock", this.productInfo.stock);
					this.$set(this.attr.productSelect, "unique", this.productInfo.id || "");
					this.$set(this.attr.productSelect, "cart_num", 1);
					this.$set(this, "attrValue", "");
					this.$set(this, "attrTxt", "请选择");
				}
			},
			attrVal(val) {
				this.$set(this.attr.productAttr[val.indexw], 'index', this.attr.productAttr[val.indexw].attrValues[val.indexn]);
			},
			ChangeCartNum: function(changeValue) {
				let productSelect = this.productValue[this.attrValue];
				if (productSelect === undefined && !this.attr.productAttr.length) productSelect = this.attr.productSelect;
				if (productSelect === undefined) return;
				let stock = productSelect.stock || 0;
				let num = this.attr.productSelect;
				if (changeValue) {
					num.cart_num++;
					if (num.cart_num > stock) {
						this.$set(this.attr.productSelect, "cart_num", stock ? stock : 1);
						this.$set(this, "cart_num", stock ? stock : 1);
					}
				} else {
					num.cart_num--;
					if (num.cart_num < 1) {
						this.$set(this.attr.productSelect, "cart_num", 1);
						this.$set(this, "cart_num", 1);
					}
				}
			},
			iptCartNum: function(e) { this.$set(this.attr.productSelect, 'cart_num', e); },
			subDel: function(event) {
				let that = this, selectValue = that.selectValue;
				if (selectValue.length > 0)
					cartDel(selectValue).then(res => {
						that.loadend = false;
						that.page = 1;
						that.cartList.valid = [];
						that.getCartList();
						that.getCartNum();
					});
				else return that.$util.Tips({ title: '请选择产品' });
			},
			getSelectValueProductId: function() {
				let that = this;
				let validList = that.cartList.valid;
				let selectValue = that.selectValue;
				let productId = [];
				if (selectValue.length > 0) {
					for (let index in validList) {
						if (that.inArray(validList[index].id, selectValue)) {
							productId.push(validList[index].productId);
						}
					}
				};
				return productId;
			},
			subCollect: function(event) {
				let that = this, selectValue = that.selectValue;
				if (selectValue.length > 0) {
					collectAll(that.getSelectValueProductId()).then(res => {
						return that.$util.Tips({ title: '收藏成功', icon: 'success' });
					}).catch(err => {
						return that.$util.Tips({ title: err });
					});
				} else {
					return that.$util.Tips({ title: '请选择产品' });
				}
			},
			subOrder: Debounce(function(event) {
				let that = this, selectValue = that.selectValue;
				if (selectValue.length > 0) {
					that.getPreOrder();
				} else {
					return that.$util.Tips({ title: '请选择产品' });
				}
			}),
			getPreOrder: function() {
				let shoppingCartId = this.selectValue.map(item => ({ "shoppingCartId": Number(item) }))
				this.$Order.getPreOrder("shoppingCart", shoppingCartId);
			},
			checkboxAllChange: function(event) {
				let value = event.detail.value;
				if (value.length > 0) { this.setAllSelectValue(1) }
				else { this.setAllSelectValue(0) }
			},
			setAllSelectValue: function(status) {
				let that = this;
				let selectValue = [];
				let valid = that.cartList.valid;
				if (valid.length > 0) {
					let newValid = valid.map(item => {
						if (status) {
							if (that.footerswitch) {
								if (item.attrStatus) { item.checked = true; selectValue.push(item.id); }
								else { item.checked = false; }
							} else { item.checked = true; selectValue.push(item.id); }
							that.isAllSelect = true;
						} else { item.checked = false; that.isAllSelect = false; }
						return item;
					});
					that.$set(that.cartList, 'valid', newValid);
					that.selectValue = selectValue;
					that.switchSelect();
				}
			},
			checkboxChange: function(event) {
				let that = this;
				let value = event.detail.value;
				let valid = that.cartList.valid;
				let arr1 = [], arr2 = [], arr3 = [];
				let newValid = valid.map(item => {
					if (that.inArray(item.id, value)) {
						if (that.footerswitch) {
							if (item.attrStatus) { item.checked = true; arr1.push(item); }
							else { item.checked = false; }
						} else { item.checked = true; arr1.push(item); }
					} else { item.checked = false; arr2.push(item); }
					return item;
				});
				if (that.footerswitch) { arr3 = arr2.filter(item => !item.attrStatus); }
				that.$set(that.cartList, 'valid', newValid);
				that.isAllSelect = newValid.length === arr1.length + arr3.length;
				that.selectValue = value;
				that.switchSelect();
			},
			inArray: function(search, array) {
				for (let i in array) { if (array[i] == search) return true; }
				return false;
			},
			switchSelect: function() {
				let that = this;
				let validList = that.cartList.valid;
				let selectValue = that.selectValue;
				let selectCountPrice = 0.00;
				if (selectValue.length < 1) { that.selectCountPrice = selectCountPrice; }
				else {
					for (let index in validList) {
						if (that.inArray(validList[index].id, selectValue)) {
							selectCountPrice = that.$util.$h.Add(selectCountPrice, that.$util.$h.Mul(validList[index].cartNum, validList[index].vipPrice ? validList[index].vipPrice : validList[index].price))
						}
					}
					that.selectCountPrice = selectCountPrice;
				}
			},
			blurInput: function(index) {
				let item = this.cartList.valid[index];
				if (!item.cartNum) { item.cartNum = 1; this.$set(this.cartList, 'valid', this.cartList.valid) }
			},
			subCart: Debounce(function(index) {
				let that = this;
				let status = false;
				let item = that.cartList.valid[index];
				item.cartNum = Number(item.cartNum) - 1;
				if (item.cartNum < 1) status = true;
				if (item.cartNum <= 1) { item.cartNum = 1; item.numSub = true; }
				else { item.numSub = false; item.numAdd = false; }
				if (false == status) {
					that.setCartNum(item.id, item.cartNum, function(data) {
						that.cartList.valid[index] = item;
						that.switchSelect();
						that.getCartNum();
					});
				}
			}),
			addCart: Debounce(function(index) {
				let that = this;
				let item = that.cartList.valid[index];
				item.cartNum = Number(item.cartNum) + 1;
				if (item.cartNum < item.stock) {
					item.numAdd = false; item.numSub = false;
					that.setCartNum(item.id, item.cartNum, function(data) {
						that.cartList.valid[index] = item;
						that.switchSelect();
						that.getCartNum();
					});
				} else if (item.cartNum === item.stock) {
					item.numAdd = true; item.numSub = false;
					that.setCartNum(item.id, item.cartNum, function(data) {
						that.cartList.valid[index] = item;
						that.switchSelect();
						that.getCartNum();
					});
				} else { item.cartNum = item.stock; item.numAdd = true; item.numSub = false; }
			}),
			setCartNum(cartId, cartNum, successCallback) {
				let that = this;
				changeCartNum(cartId, cartNum).then(res => { successCallback && successCallback(res.data); });
			},
			getCartNum: function() {
				let that = this;
				getCartCounts(true, 'total').then(res => { that.cartCount = res.data.count; });
			},
			getCartData(data) {
				return new Promise((resolve, reject) => {
					getCartList(data).then((res) => { resolve(res.data); }).catch(function(err) {
						this.loading = false; this.canShow = true;
						this.$util.Tips({ title: err });
					})
				});
			},
			async getCartList() {
				uni.showLoading({ title: '加载中', mask: true });
				let that = this;
				let data = { page: that.page, limit: that.limit, isValid: true }
				getCartCounts(true, 'total').then(async c => {
					that.cartCount = c.data.count;
					for (let i = 0; i < Math.ceil(that.cartCount / that.limit); i++) {
						let cartList = await this.getCartData(data);
						let valid = cartList.list;
						let validList = that.$util.SplitArray(valid, that.cartList.valid);
						let selectValue = [];
						if (validList.length > 0) {
							for (let index in validList) {
								if (validList[index].cartNum == 1) validList[index].numSub = true;
								else validList[index].numSub = false;
								let stock = validList[index].stock ? validList[index].stock : 0;
								if (validList[index].cartNum == stock) validList[index].numAdd = true;
								else if (validList[index].cartNum == validList[index].stock) validList[index].numAdd = true;
								else validList[index].numAdd = false;
								if (validList[index].attrStatus) { validList[index].checked = true; selectValue.push(validList[index].id); }
								else validList[index].checked = false;
							}
						}
						that.$set(that.cartList, 'valid', validList);
						data.page +=1;
						that.selectValue = selectValue;
						let newArr = validList.filter(item => item.attrStatus);
						that.isAllSelect = newArr.length == selectValue.length && newArr.length;
						that.switchSelect();
					}
					that.loading = false; that.canShow = true; uni.hideLoading();
				});
			},
			getInvalidList: function() {
				let that = this;
				if (this.loadendInvalid) return false;
				if (this.loadingInvalid) return false;
				let data = { page: that.pageInvalid, limit: that.limitInvalid, isValid: false }
				getCartList(data).then(res => {
					let invalid = res.data.list, loadendInvalid = invalid.length < that.limitInvalid;
					let invalidList = that.$util.SplitArray(invalid, that.cartList.invalid);
					that.$set(that.cartList, 'invalid', invalidList);
					that.loadendInvalid = loadendInvalid;
					that.loadTitleInvalid = loadendInvalid ? '我也是有底线的~' : '加载更多';
					that.pageInvalid = that.pageInvalid + 1;
					that.loadingInvalid = false;
				}).catch(res => {
					that.loadingInvalid = false;
					that.loadTitleInvalid = '加载更多';
				})
			},
			goodsOpen: function() { let that = this; that.goodsHidden = !that.goodsHidden; },
			manage: function() {
				let that = this;
				that.footerswitch = !that.footerswitch;
				let arr1 = [], arr2 = [];
				let newValid = that.cartList.valid.map(item => {
					if (that.footerswitch) {
						if (item.attrStatus) { if (item.checked) arr1.push(item.id); }
						else item.checked = false, arr2.push(item);
					} else { if (item.checked) arr1.push(item.id); }
					return item;
				});
				that.cartList.valid = newValid;
				if (that.footerswitch) that.isAllSelect = newValid.length === arr1.length + arr2.length;
				else that.isAllSelect = newValid.length === arr1.length;
				that.selectValue = arr1;
				that.switchSelect();
			},
			unsetCart: function() {
				let that = this, ids = [];
				for (let i = 0, len = that.cartList.invalid.length; i < len; i++) ids.push(that.cartList.invalid[i].id);
				cartDel(ids).then(res => {
					that.$util.Tips({ title: '清除成功' });
					that.$set(that.cartList, 'invalid', []);
					that.getHostProduct();
				}).catch(res => {});
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
					let configAppMessage = { desc: data.synopsis, title: data.title, link: location.href, imgUrl: data.img };
					that.$wechat.wechatEvevt(["updateAppMessageShareData", "updateTimelineShareData"], configAppMessage);
				}
			},
			returns: function() { uni.switchTab({ url:'/pages/index/index' }) },
			showNav(){ this.currentPage = !this.currentPage; },
			linkPage(url){
				if(url == '/pages/index/index' || url == '/pages/user/index') { uni.switchTab({ url }) }
				else { uni.navigateTo({ animationType: animationType.type, animationDuration: animationType.duration, url }) }
				this.currentPage = false
			},
			touchStart(){ this.currentPage = false; }
		},
		onReachBottom() {
			let that = this;
			if (that.loadend) that.getInvalidList();
			if (that.cartList.valid.length == 0 && that.cartList.invalid.length == 0) that.$refs.recommendIndex.get_host_product();
		}
	}
</script>

<style lang="scss" scoped>
	/* ===== 转享 Design System: Cart ===== */
	$primary: #FF6B35;
	$primary-light: #FFF8F3;
	$secondary: #FFB627;
	$accent: #FF6B35;
	$fg: #0F172A;
	$muted-bg: #FFF0E8;
	$radius: 16rpx;
	$radius-sm: 12rpx;
	$radius-lg: 24rpx;

	.crmeb-cart { background: $primary-light; min-height: 100vh; }

	/* Header */
	.crmeb-cart__header {
		position: fixed; top: 0; left: 0; z-index: 99; width: 100%;
		background: linear-gradient(135deg, $primary, $secondary);
		padding-top: var(--status-bar-height);
	}
	.crmeb-cart__header-inner {
		height: 88rpx; width: 100%; display: flex; align-items: center;
		justify-content: center; position: relative;
	}
	.crmeb-cart__nav-title {
		font-size: 36rpx; color: #fff; font-weight: 600;
	}
	.crmeb-cart__nav-more {
		position: absolute; right: 30rpx; z-index: 100;
		font-size: 36rpx; color: #fff; letter-spacing: 2rpx;
	}
	.crmeb-cart__dropdown {
		position: fixed; right: 14rpx; width: 240rpx;
		background: #fff; box-shadow: 0 0 16rpx rgba(0,0,0,0.08); z-index: 999; border-radius: $radius-sm;
		z-index: 999;
	}
	.crmeb-cart__dropdown-item {
		height: 84rpx; line-height: 84rpx; padding: 0 20rpx;
		font-size: 28rpx; color: #333; display: flex; align-items: center; gap: 16rpx;
	}

	/* Body */
	.crmeb-cart__body { position: absolute; width: 100%; padding-top: 180rpx; }

	/* Guarantee */
	.crmeb-cart__guarantee {
		display: flex; justify-content: space-around; padding: 24rpx 24rpx;
		font-size: 22rpx; color: #fff; background: $primary;
		border-radius: 0 0 $radius-lg $radius-lg;
	}

	/* Content */
	.crmeb-cart__content { padding: 0 24rpx; }

	/* Toolbar */
	.crmeb-cart__toolbar {
		display: flex; justify-content: space-between; align-items: center;
		height: 88rpx; background: #fff; border-radius: $radius;
		padding: 0 24rpx; margin-top: 20rpx;
		font-size: 28rpx; color: $fg;
		box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
	}
	.crmeb-cart__count { margin-left: 12rpx; color: $primary; font-weight: 600; }
	.crmeb-cart__manage { font-size: 28rpx; color: #333; }

	/* Items */
	.crmeb-cart__items { background: #fff; border-radius: $radius; overflow: hidden; margin-top: 16rpx; }
	.crmeb-cart__item {
		padding: 24rpx; border-bottom: 1rpx solid $muted-bg;
		display: flex; align-items: flex-start;
	}
	.crmeb-cart__item-body { display: flex; flex: 1; margin-left: 12rpx; position: relative; }
	.crmeb-cart__item-img { width: 160rpx; height: 160rpx; border-radius: $radius-sm; overflow: hidden; flex-shrink: 0; box-shadow: 0 2rpx 8rpx rgba(255, 107, 53, 0.1); }
	.crmeb-cart__item-img image { width: 100%; height: 100%; }
	.crmeb-cart__item-info { flex: 1; margin-left: 16rpx; }
	.crmeb-cart__item-name { font-size: 26rpx; color: $fg; line-height: 36rpx; }
	.crmeb-cart__item-name.disabled { color: #999; }
	.crmeb-cart__item-spec { font-size: 24rpx; color: #999; margin-top: 12rpx; }
	.crmeb-cart__item-price { font-size: 32rpx; font-weight: 600; color: $primary; margin-top: 20rpx; }
	.crmeb-cart__item-reselect { margin-top: 20rpx; display: flex; align-items: center; gap: 12rpx; }
	.crmeb-cart__reselect-tip { font-size: 24rpx; color: #999; }
	.crmeb-cart__reselect-btn { font-size: 26rpx; color: $primary; border: 1rpx solid $primary; border-radius: 23rpx; padding: 6rpx 24rpx; }
	.crmeb-cart__item-qty { position: absolute; bottom: 0; right: 0; display: flex; align-items: center; }
	.crmeb-cart__qty-btn {
		width: 60rpx; height: 48rpx; border: 1rpx solid #a4a4a4;
		text-align: center; line-height: 46rpx; font-size: 34rpx; color: #a4a4a4;
	}
	.crmeb-cart__qty-btn:first-child { border-radius: 22rpx 0 0 22rpx; border-right: 0; }
	.crmeb-cart__qty-btn:last-child { border-radius: 0 22rpx 22rpx 0; border-left: 0; }
	.crmeb-cart__qty-btn.disabled { border-color: #e3e3e3; color: #dedede; }
	.crmeb-cart__qty-num { width: 66rpx; height: 48rpx; border: 1rpx solid #a4a4a4; text-align: center; line-height: 46rpx; font-size: 26rpx; color: $fg; }

	/* Invalid */
	.crmeb-cart__invalid { background: #fff; border-radius: $radius; margin-top: 20rpx; overflow: hidden; }
	.crmeb-cart__invalid-header {
		display: flex; align-items: center; height: 90rpx; padding: 0 24rpx;
		font-size: 28rpx; color: #333; gap: 12rpx;
	}
	.crmeb-cart__invalid-toggle { font-size: 28rpx; color: #424242; }
	.crmeb-cart__invalid-clear { margin-left: auto; font-size: 26rpx; color: #333; }
	.crmeb-cart__invalid-list { }
	.crmeb-cart__invalid-item {
		padding: 24rpx; border-top: 1rpx solid $muted-bg;
		display: flex; align-items: flex-start;
	}
	.crmeb-cart__invalid-badge { font-size: 22rpx; color: #ccc; margin-right: 12rpx; }
	.crmeb-cart__invalid-tag { font-size: 26rpx; color: #bbb; display: block; margin-top: 16rpx; }

	/* Loading */
	.crmeb-cart__loading { text-align: center; padding: 20rpx; font-size: 26rpx; color: #94a3b8; }

	/* Empty */
	.crmeb-cart__empty { background: #fff; padding-top: 1rpx; }
	.crmeb-cart__empty-img { width: 414rpx; height: 336rpx; margin: 78rpx auto 56rpx; }
	.crmeb-cart__empty-img image { width: 100%; height: 100%; }

	/* Footer */
	.crmeb-cart__footer {
		z-index: 999; width: 100%; height: 100rpx; background: #fff;
		position: fixed; bottom: 0; padding: 0 24rpx; box-sizing: border-box;
		border-top: 1rpx solid #eee; display: flex; align-items: center;
	}
	.crmeb-cart__select-all { font-size: 28rpx; color: $fg; margin-left: 14rpx; }
	.crmeb-cart__footer-right { margin-left: auto; display: flex; align-items: center; gap: 22rpx; }
	.crmeb-cart__total { font-size: 32rpx; font-weight: 600; color: $primary; }
	.crmeb-cart__checkout {
		color: #fff; font-size: 30rpx; background: linear-gradient(135deg, $primary, $secondary);
		width: 226rpx; height: 70rpx; border-radius: 50rpx; text-align: center; line-height: 70rpx; border: none;
	}
	.crmeb-cart__checkout::after { border: none; }
	.crmeb-cart__checkout:active {
		opacity: 0.9;
		transform: scale(0.97);
	}
	.crmeb-cart__footer-actions { margin-left: auto; display: flex; gap: 17rpx; }
	.crmeb-cart__action-btn { font-size: 14px; border-radius: 25px; padding: 0 30rpx; height: 60rpx; line-height: 60rpx; }
	.crmeb-cart__action-btn.collect { color: $primary; border: 1rpx solid $primary; background: #fff; }
	.crmeb-cart__action-btn.delete { color: #999; border: 1rpx solid #999; background: #fff; }

	.bottom-custom { bottom: calc(98rpx + env(safe-area-inset-bottom)); }

	/deep/ checkbox .uni-checkbox-input.uni-checkbox-input-checked { background: $primary; border: none !important; color: #fff !important; }
	/deep/ checkbox .wx-checkbox-input.wx-checkbox-input-checked { background: $primary; border: none !important; color: #fff !important; }
</style>
