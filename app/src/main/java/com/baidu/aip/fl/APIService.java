/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.fl;


import com.baidu.aip.fl.model.AccessToken;
import com.baidu.aip.fl.utils.DeviceUuidFactory;
import com.baidu.aip.fl.utils.HttpUtil;
import com.baidu.aip.fl.utils.OnResultListener;

import android.content.Context;

public class APIService {

    private static final String BASE_URL = "https://aip.baidubce.com";

    private static final String ACCESS_TOEKN_URL = BASE_URL + "/oauth/2.0/token?";

    private String accessToken;

    private String groupId;

    private APIService() {

    }

    private static volatile APIService instance;

    public static APIService getInstance() {
        if (instance == null) {
            synchronized (APIService.class) {
                if (instance == null) {
                    instance = new APIService();


                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        // 采用deviceId分组
        HttpUtil.getInstance().init();
        DeviceUuidFactory.init(context);
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * 设置accessToken 如何获取 accessToken 详情见:
     *
     * @param accessToken accessToken
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * 明文aksk获取token
     *
     * @param listener
     * @param context
     * @param ak
     * @param sk
     */
    public void initAccessTokenWithAkSk(final OnResultListener<AccessToken> listener, Context context, String ak,
                                        String sk) {

        StringBuilder sb = new StringBuilder();
        sb.append("client_id=").append(ak);
        sb.append("&client_secret=").append(sk);
        sb.append("&grant_type=client_credentials");
        HttpUtil.getInstance().getAccessToken(listener, ACCESS_TOEKN_URL, sb.toString());

    }

}
