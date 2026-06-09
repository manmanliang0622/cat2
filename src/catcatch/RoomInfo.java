package catcatch;

/** 透過 UDP 搜尋取得的遊戲房間資訊。 */
public record RoomInfo(
        String roomCode,
        String hostName,
        String ip,
        int port,
        int playerCount,
        boolean playing) {

    /** 顯示用的簡短描述，用在 ListView cell 的副標題。 */
    public String statusText() {
        return playing ? "遊戲進行中" : playerCount + " 位玩家・等待中";
    }
}
