package com.kinstalk.her.settings.view.data;

import com.kinstalk.her.settings.BuildConfig;
import com.kinstalk.her.settings.util.DebugUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Tracy on 2018/4/18.
 */

public class OkhttpClientHelper {
    private static final String TAG = "OKHttpClientHelper";
    // 消息头
    private static final String HEADER_X_HB_Client_Type = "X-HB-Client-Type";
    private static final String FROM_ANDROID = "android";
    private static HttpLoggingInterceptor sLogInterceptor = new HttpLoggingInterceptor(new DebugUtils.QAIHttpLogger(TAG))
            .setLevel(HttpLoggingInterceptor.Level.BODY);
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 10;
    private static final int WRITE_TIMEOUT = 10;
    private static OkHttpClient sSafeClient;
    private static final boolean SHOW_OKHTTP_LOG = !DebugUtils.isUserType() && !BuildConfig.IS_RELEASE;
    /**
     * 拦截器  给所有的请求添加消息头
     */
    private static Interceptor sHeaderInterceptor = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request()
                    .newBuilder()
                    .addHeader(HEADER_X_HB_Client_Type, FROM_ANDROID)
                    .build();
            return chain.proceed(request);
        }
    };
    public static synchronized OkHttpClient getOkHttpClient() {

        //设置 请求的缓存
//        File cacheFile = new File(BaseApplication.getInstance().getCacheDir(), CACHE_FOLDER);
//        Cache cache = new Cache(cacheFile, 1024 * 1024 * 50); //50Mb
        if(sSafeClient == null) {
            OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)//允许失败重试
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor(new RetryAndChangeIpInterceptor(2))//添加失败重试及重定向拦截器
                    .addInterceptor(sHeaderInterceptor);
//                .cache(cache);

            if (SHOW_OKHTTP_LOG) {
                okHttpBuilder.addInterceptor(sLogInterceptor);
            }
            sSafeClient = okHttpBuilder.build();
        }
        return sSafeClient;
    }

}
