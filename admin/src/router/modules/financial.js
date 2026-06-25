// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import Layout from '@/layout';

const financialRouter = {
  path: '/financial',
  component: Layout,
  redirect: '/financial/flow',
  name: 'Financial',
  meta: {
    title: '财务',
    icon: 'clipboard',
  },
  children: [
    {
      path: 'flow',
      component: () => import('@/views/financial/record/charge/index'),
      name: 'FinancialFlow',
      meta: { title: '交易流水', icon: '' },
    },
  ],
};

export default financialRouter;
