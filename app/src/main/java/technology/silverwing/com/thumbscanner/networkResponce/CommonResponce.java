package technology.silverwing.com.thumbscanner.networkResponce;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CommonResponce implements Serializable {

    @SerializedName("Success")
    @Expose
    private
    String success;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

}
