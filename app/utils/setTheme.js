// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

let app = getApp();
export function setThemeColor(){
	switch (app.globalData.theme) {
		case 'theme1':
			return '#FF6B35';
			break;
		case 'theme2':
			return '#FF6B35';
			break;
		case 'theme3':
			return '#FF6B35';
			break;
		case 'theme4':
			return '#FF6B35';
			break;
		case 'theme5':
			return '#FF6B35';
			break;
	}
}
