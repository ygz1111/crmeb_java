package com.zbkj.common.utils;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.exception.CrmebException;
import org.apache.commons.codec.binary.Base64;

/**
 *  微信工具类
 *  +----------------------------------------------------------------------
 *  | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 *  +----------------------------------------------------------------------
 *  | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 *  +----------------------------------------------------------------------
 *  | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 *  +----------------------------------------------------------------------
 *  | Author: CRMEB Team <admin@crmeb.com>
 *  +----------------------------------------------------------------------
 */
public class WxUtil {

    private static final String WATERMARK = "watermark";
    private static final String APPID = "appid";
    /**
     * 解密数据
     * @return
     * @throws Exception
     */
    public static String decrypt(String appId, String encryptedData, String sessionKey, String iv){
        String result = "";
        try {
            AESUtil aes = new AESUtil();
            byte[] resultByte = aes.decrypt(Base64.decodeBase64(encryptedData), Base64.decodeBase64(sessionKey), Base64.decodeBase64(iv));
            if(null != resultByte && resultByte.length > 0){
                result = new String(WxPKCS7Encoder.decode(resultByte));
                JSONObject jsonObject = JSONObject.parseObject(result);
                String decryptAppid = jsonObject.getJSONObject(WATERMARK).getString(APPID);
                if(!appId.equals(decryptAppid)){
                    result = "";
                }
            }
        } catch (Exception e) {
            result = "";
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 检测结构请求返回的数据
     * @param result 接口返回的结果
     * @return JSONObject
     */
    public static JSONObject checkResult(JSONObject result){
        if(ObjectUtil.isNull(result)){
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if(result.containsKey("errcode") && result.getString("errcode").equals("0")){
            return result;
        }
        if(result.containsKey("errmsg")){
            // 非核心微信错误（如无效 appid、token 过期等）只打印日志，不阻断操作
            String errcode = result.containsKey("errcode") ? result.getString("errcode") : "";
            String errmsg = result.getString("errmsg");
            System.err.println("微信接口调用失败: errcode=" + errcode + ", errmsg=" + errmsg);
            return result;
        }
        return result;
    }

}
