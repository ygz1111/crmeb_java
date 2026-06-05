// +----------------------------------------------------------------------
// | CRMEB [ CRMEB璧嬭兘寮€鍙戣€咃紝鍔╁姏浼佷笟鍙戝睍 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2024 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB骞朵笉鏄嚜鐢辫蒋浠讹紝鏈粡璁稿彲涓嶈兘鍘绘帀CRMEB鐩稿叧鐗堟潈
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import { login, logout, getInfo } from '@/api/user';
import { getToken, setToken, removeToken } from '@/utils/auth';
import router, { resetRouter } from '@/router';
import { isLoginApi } from '@/api/sms';
import Cookies from 'js-cookie';
import { Loading } from 'element-ui';
import * as roleApi from '@/api/roleApi.js';
import { formatFlatteningRoutes } from '@/utils/system.js';
import * as Auth from '@/libs/wechat';

const state = {
  token: getToken(),
  name: '',
  avatar: '',
  introduction: '',
  roles: [],
  isLogin: Cookies.get('isLogin'),
  permissions: [],
  captcha: {
    captchaVerification: '',
    secretKey: '',
    token: '',
  }, //婊戝潡楠岃瘉token
  // 鑿滃崟鏁版嵁
  menuList: JSON.parse(localStorage.getItem('MerPlatAdmin_MenuList')) || [],
  oneLvMenus: [],
  oneLvRoutes: JSON.parse(localStorage.getItem('MerPlatAdmin_oneLvRoutes')) || [],
  childMenuList: [],
};

const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token;
  },
  SET_ISLOGIN: (state, isLogin) => {
    state.isLogin = isLogin;
    Cookies.set(isLogin);
  },
  SET_INTRODUCTION: (state, introduction) => {
    state.introduction = introduction;
  },
  SET_NAME: (state, name) => {
    state.name = name;
  },
  SET_AVATAR: (state, avatar) => {
    state.avatar = avatar;
  },
  SET_ROLES: (state, roles) => {
    state.roles = roles;
  },
  SET_PERMISSIONS: (state, permissions) => {
    state.permissions = permissions;
  },
  SET_CAPTCHA: (state, captcha) => {
    state.captcha = captcha;
  },
  SET_MENU_LIST: (state, menuList) => {
    state.menuList = menuList;
  },
  setOneLvMenus(state, oneLvMenus) {
    state.oneLvMenus = oneLvMenus;
  },
  setOneLvRoute(state, oneLvRoutes) {
    state.oneLvRoutes = oneLvRoutes;
  },
  childMenuList(state, list) {
    state.childMenuList = list;
  },
};

const actions = {
  // user login
  login({ commit }, userInfo) {
    const { account, pwd, key, code, wxCode } = userInfo;
    Loading.service();
    return new Promise((resolve, reject) => {
      login(userInfo)
        .then((data) => {
          let loadingInstance = Loading.service();
          loadingInstance.close();
          commit('SET_TOKEN', data.token);
          Cookies.set('JavaInfo', JSON.stringify(data));
          setToken(data.token);
          resolve();
        })
        .catch((error) => {
          reject(error);
        });
    });
  },

  // 鐭俊鏄惁鐧诲綍
  isLogin({ commit }, userInfo) {
    return new Promise((resolve, reject) => {
      isLoginApi()
        .then(async (res) => {
          commit('SET_ISLOGIN', res.isLogin);
          resolve(res);
        })
        .catch((res) => {
          commit('SET_ISLOGIN', false);
          reject(res);
        });
    });
  },

  // get user info
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo(state.token)
        .then((data) => {
          if (!data) {
            reject('Verification failed, please Login again.');
          }
          const { roles, account } = data;
          // roles must be a non-empty array
          if (!roles || roles.length <= 0) {
            reject('getInfo: roles must be a non-null array!');
          }

          commit('SET_ROLES', roles);
          // commit('SET_ROLES', ['admin'])
          commit('SET_NAME', account);
          // commit('SET_AVATAR', avatar)
          commit('SET_AVATAR', 'http://kaifa.crmeb.net/system/images/admin_logo.png');
          commit('SET_INTRODUCTION', 'CRMEB admin');
          commit('SET_PERMISSIONS', data.permissionsList); //鏉冮檺鏍囪瘑
          resolve(data);
        })
        .catch((error) => {
          reject(error);
        });
    });
  },

  // user logout
  handleLogout({ commit, state, dispatch }) {
    Loading.service();
    return new Promise((resolve, reject) => {
      logout(state.token)
        .then(() => {
          let loadingInstance = Loading.service();
          loadingInstance.close();
          commit('SET_TOKEN', '');
          commit('SET_ROLES', []);
          commit('SET_PERMISSIONS', []);
          removeToken();
          resetRouter();
          // localStorage.clear();
          Cookies.remove('storeStaffList');
          Cookies.remove('JavaInfo');
          sessionStorage.removeItem('token');
          // reset visited views and cached views
          // to fixed https://github.com/PanJiaChen/vue-element-admin/issues/2485
          dispatch('tagsView/delAllViews', null, { root: true });
          resolve();
        })
        .catch((error) => {
          reject(error);
        });
    });
  },

  // remove token
  resetToken({ commit }) {
    return new Promise((resolve) => {
      commit('SET_TOKEN', '');
      commit('SET_ROLES', []);
      removeToken();
      resolve();
    });
  },
  // 璁剧疆token
  setToken({ commit }, state) {
    return new Promise((resolve) => {
      commit('SET_TOKEN', state.token);
      Cookies.set('JavaInfo', JSON.stringify(state));
      setToken(data.token);
      resolve();
    });
  },
  getMenus({ commit }) {
    function formatTwoStageRoutes(arr) {
      if (arr.length <= 0) return false;
      const newArr = [];
      const cacheList = [];
      arr.forEach((v) => {
        if (v && v.meta && v.meta.keepAlive) {
          newArr.push({ ...v });
          cacheList.push(v.name);
          this.$store.dispatch('keepAliveNames/setCacheKeepAlive', cacheList);
        }
      });
      return newArr;
    }

    return new Promise(async (resolve, reject) => {
      let accessRoutes = await roleApi.menuListApi();
      accessRoutes = replaceChildListWithChildren(accessRoutes);


      // === 答辩精简：过滤不需要的菜单模块 ===
      const hiddenPaths = ['/distribution', '/appSetting', '/maintain'];
      accessRoutes = accessRoutes.filter(item => !hiddenPaths.includes(item.path));
      // 精简财务模块子菜单 - 只保留交易流水
      const financialMenu = accessRoutes.find(item => item.path === '/financial');
      if (financialMenu && financialMenu.children) {
        financialMenu.children = financialMenu.children.filter(child => child.path === '/financial/flow');
      }
      // 移除短信相关子菜单
      const operationMenu = accessRoutes.find(item => item.path === '/operation');
      if (operationMenu && operationMenu.children) {
        operationMenu.children = operationMenu.children.filter(
          child => !['/operation/systemSms', '/operation/onePass', '/operation/onePassConfig'].includes(child.path)
        );
      }
      //澶勭悊绉诲姩绔矾鐢?
      !Auth.isPhone()
        ? (accessRoutes = accessRoutes.filter((item) => item.path !== '/javaMobile'))
        : (accessRoutes = accessRoutes.filter((item) => item.path === '/javaMobile'));
      // let accessRoutes = formatRoutes(menusAll);
      // const accessRoutes = await dispatch('permission/generateRoutes', roles, { root: true });
      commit('SET_MENU_LIST', accessRoutes);
      localStorage.setItem('MerPlatAdmin_MenuList', JSON.stringify(accessRoutes));
      let arr = formatFlatteningRoutes(router.options.routes);
      formatTwoStageRoutes(arr);
      let routes = formatFlatteningRoutes(accessRoutes);
      localStorage.setItem('MerPlatAdmin_oneLvRoutes', JSON.stringify(routes));
      commit('setOneLvMenus', arr);
      commit('setOneLvRoute', routes);
      resolve(resolve);
    });
  },
};

// 閫掑綊鍑芥暟锛岀敤浜庢浛鎹?childList 涓?children
function replaceChildListWithChildren(data) {
  return data.map((item) => {
    // 妫€鏌ユ槸鍚﹀瓨鍦?childList 瀛楁
    if (item.childList) {
      // 閫掑綊澶勭悊 childList 涓殑姣忎釜瀛愬璞?
      const children = replaceChildListWithChildren(item.childList);
      // 鍒涘缓涓€涓柊鐨勫璞★紝灏?childList 鏇挎崲涓?children
      const title = item.name;
      const path = item.component;
      return {
        ...item,
        children,
        title,
        path,
        // 鍒犻櫎鍘熸潵鐨?childList 瀛楁
        childList: undefined,
        name: undefined,
        component: undefined,
      };
    }
    // 濡傛灉涓嶅瓨鍦?childList 瀛楁锛岀洿鎺ヨ繑鍥炲師瀵硅薄
    return item;
  });
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
};
