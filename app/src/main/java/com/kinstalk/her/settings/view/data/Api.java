package com.kinstalk.her.settings.view.data;

import android.text.TextUtils;
import android.util.Log;

import com.kinstalk.her.settings.HerSettingsApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;

public class Api {
    private static final String TAG = "Api";
    public static String TEST_URL = "https://uc.qspeaker.com/";
    private static final String USER_REGISTER_PATH = "user/register";
    private static final String VERIFICATION_PATH = "user/sendsms";
    private static final String CHANGE_PHONE_PATH = "user/modify_mobile";

    /**注册
     * @param info
     * @param callBack
     */
    public static void fetchPhoneNumber(final AccountInfo info, final PhoneResultCallBack callBack) {
        QchatThreadManager.getInstance().start(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "fetch Phone Number: Enter");
                String phone_number;
                okhttp3.Response response;

                try {
                    response = OkhttpClientHelper.getOkHttpClient().newCall(createPhoneNumbRequest(info)).execute();
                    //判断请求是否成功
                    if (response == null) {
                        Log.e(TAG, "fetch Phone number response empty");
                    } else if (!response.isSuccessful()) {
                        Log.e(TAG, "run: response:"+response);
                        Log.e(TAG, "fetch Phone number response failed");
                    } else {
                        if (!HttpHeaders.hasBody(response)) {
                            Log.e(TAG, "fetch Phone number rspBody empty");
                        } else {
                            //打印服务端返回结果
                            ResponseBody rspBody = response.body();
                            long contentLength = rspBody.contentLength();
                            if (contentLength != 0) {
                                String sBody = rspBody.string();
                                Log.i(TAG, "fetch Phone number body = : " + sBody);
                                try {
                                    JSONObject rspJson = new JSONObject(sBody);
                                    JSONObject user = rspJson.optJSONObject("user");
                                    phone_number = user.optString("mobile", null);
                                    callBack.getPhoneNumber(phone_number);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static Request createPhoneNumbRequest(AccountInfo info) {
        Log.i(TAG, "createPhoneNumbRequest");
        HttpUrl httpUrl = new Request.Builder()
                .url(TEST_URL)
                .build()
                .url()
                .newBuilder()
                .addEncodedPathSegment(USER_REGISTER_PATH)
                .addQueryParameter("tinyid", info.getTinyId())
                .addQueryParameter("din", info.getDin())
                .addQueryParameter("sn", Config.getMacForSn())
                .addQueryParameter("devicetype", "1")
                .addQueryParameter("nickname", info.getNickName())
                .addQueryParameter("version", SystemTool.getVersionName(HerSettingsApplication.getApplication()))
                .build();

        Log.i(TAG, "createPhoneNumbRequest:data = " + httpUrl.toString());

        return new Request.Builder()
                .url(httpUrl)
                .get()//requestBody)
                .build();
    }

    public interface PhoneResultCallBack {
        void getPhoneNumber(String result);
    }

    /**获取验证码
     * @param tinyId
     * @param mobile
     * @param callBack
     */
    public static void fetchVerification(final String tinyId, final String mobile, final VerifyCodeResultCallBack callBack) {
        QchatThreadManager.getInstance().start(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "fetch verification code: Enter");
                int verification_code;
                okhttp3.Response response;

                try {
                    response = OkhttpClientHelper.getOkHttpClient().newCall(createVerificationCodeRequest(tinyId,mobile)).execute();
                    //判断请求是否成功
                    if (response == null) {
                        Log.e(TAG, "fetch verification code response empty");
                    } else if (!response.isSuccessful()) {
                        Log.e(TAG, "run: response:"+response);
                        Log.e(TAG, "fetch verification code response failed");
                    } else {
                        if (!HttpHeaders.hasBody(response)) {
                            Log.e(TAG, "fetch verification code rspBody empty");
                        } else {
                            //打印服务端返回结果
                            ResponseBody rspBody = response.body();
                            long contentLength = rspBody.contentLength();
                            if (contentLength != 0) {
                                String sBody = rspBody.string();
                                Log.i(TAG, "fetch verification code body = : " + sBody);
                                try {
                                    JSONObject rspJson = new JSONObject(sBody);
                                    verification_code = rspJson.optInt("code");
                                    callBack.verificationCodeResult(verification_code);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static Request createVerificationCodeRequest(String tinyId,String mobile) {
        Log.i(TAG, "createVerificationCodeRequest");
        HttpUrl httpUrl = new Request.Builder()
                .url(TEST_URL)
                .build()
                .url()
                .newBuilder()
                .addEncodedPathSegment(VERIFICATION_PATH)
                .addQueryParameter("tinyid", tinyId)
                .addQueryParameter("mobile", mobile)
                .build();

        Log.i(TAG, "createPhoneNumbRequest:data = " + httpUrl.toString());

        return new Request.Builder()
                .url(httpUrl)
                .get()//requestBody)
                .build();
    }

    public interface VerifyCodeResultCallBack {
        void verificationCodeResult(int result);
    }

    /**重新绑定手机号
     * @param tinyId
     * @param mobile
     * @param smsCode
     * @param callBack
     */
    public static void reBindPhoneNumber(final String tinyId, final String mobile, final String smsCode,final RebindPhoneResultCallBack callBack) {
        QchatThreadManager.getInstance().start(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "reBindPhoneNumber: Enter");
                int verification_code;
                String phone_number;
                okhttp3.Response response;

                try {
                    response = OkhttpClientHelper.getOkHttpClient().newCall(createRebindPhoneRequest(tinyId,mobile,smsCode)).execute();
                    //判断请求是否成功
                    if (response == null) {
                        Log.e(TAG, "reBindPhoneNumber response empty");
                    } else if (!response.isSuccessful()) {
                        Log.e(TAG, "run: response:"+response);
                        Log.e(TAG, "reBindPhoneNumber response failed");
                    } else {
                        if (!HttpHeaders.hasBody(response)) {
                            Log.e(TAG, "reBindPhoneNumbere rspBody empty");
                        } else {
                            //打印服务端返回结果
                            ResponseBody rspBody = response.body();
                            long contentLength = rspBody.contentLength();
                            if (contentLength != 0) {
                                String sBody = rspBody.string();
                                Log.i(TAG, "reBindPhoneNumber body = : " + sBody);
                                try {
                                    JSONObject rspJson = new JSONObject(sBody);
                                    JSONObject data = rspJson.optJSONObject("data");
                                    verification_code = rspJson.optInt("code");
                                    phone_number = data.optString("mobile", null);
                                    if(!TextUtils.isEmpty(phone_number)){
                                        SettingDBTool.savePhoneWithTinyId(tinyId, phone_number);
                                    }
                                    callBack.getRebindResult(verification_code,phone_number);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static Request createRebindPhoneRequest(String tinyId,String mobile,String smsCode) {
        Log.i(TAG, "createPhoneNumbRequest");
        HttpUrl httpUrl = new Request.Builder()
                .url(TEST_URL)
                .build()
                .url()
                .newBuilder()
                .addEncodedPathSegment(CHANGE_PHONE_PATH)
                .addQueryParameter("tinyid", tinyId)
                .addQueryParameter("mobile", mobile)
                .addQueryParameter("smscode", smsCode)
                .build();

        Log.i(TAG, "createRebindPhoneRequest:data = " + httpUrl.toString());

        return new Request.Builder()
                .url(httpUrl)
                .get()//requestBody)
                .build();
    }

    public interface RebindPhoneResultCallBack {
        void getRebindResult(int code,String result);
    }
}
