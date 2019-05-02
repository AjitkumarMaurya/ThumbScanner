package technology.silverwing.com.thumbscanner.network;

import java.util.List;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Single;
import technology.silverwing.com.thumbscanner.networkResponce.CommonResponce;
import technology.silverwing.com.thumbscanner.networkResponce.LoginResponce;

public interface RestCall {

    @FormUrlEncoded
    @POST("SaveFingerPrintData")
    Single<List<CommonResponce>> sendData(@Field("aadhaarNo") String aadhaarNo, @Field("rationNo") String rationNo, @Field("areaCode") String areaCode,
                                              @Field("addedByID") String addedByID, @Field("RThumb") String RThumb,
                                              @Field("LThumb") String LThumb);
    @GET("UserLogin?")
    Single<List<LoginResponce>> getLogin(@Query("userMobile") String userMobile, @Query("userPassword") String userPassword);


}
