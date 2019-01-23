package cn.wemarket.redis.proxy.common.dto;

/**
 * Netty 中通过 state 属性来保存当前序列化的状态
 * 然后下次反序列化的时候就可以从上次记录的 state 直接继续反序列化
 * 这样就避免了重复的问题
 * */
public class RedisDecodeState {
    private byte[] parseBuff;
    private int startPos;
    private int parsePos;
    private int endPos;
    private boolean incomplete = false;

    public byte[] getParseBuff() {
        return parseBuff;
    }

    public void setParseBuff(byte[] parseBuff) {
        this.parseBuff = parseBuff;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getParsePos() {
        return parsePos;
    }

    public void setParsePos(int parsePos) {
        this.parsePos = parsePos;
    }

    public int getEndPos() {
        return endPos;
    }

    public void setEndPos(int endPos) {
        this.endPos = endPos;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public void increaseParsePos(int i){
        this.parsePos = this.parsePos + 1;
    }

    public void reset(){
        this.parseBuff = null;
        this.startPos = -1;
        this.parsePos = -1;
        this.endPos = -1;
        this.incomplete = false;
    }
}
