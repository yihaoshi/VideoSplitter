public class CmdResult {

    private boolean success;
    private String msg;

    public CmdResult(boolean success, String msg) {
        this.success = success;
        this.msg = msg;
    }

    public CmdResult() {

    }

    public Boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}