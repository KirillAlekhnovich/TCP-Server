public enum Keys {
    KEY0(23019, 32037),
    KEY1(32037, 29295),
    KEY2(18789, 13603),
    KEY3(16443, 29533),
    KEY4(18189, 21952);

    private final int serverKey;
    private final int clientKey;

    Keys(int serverKey, int clientKey) {
        this.serverKey = serverKey;
        this.clientKey = clientKey;
    }

    public int getServerKey() {
        return serverKey;
    }

    public int getClientKey() {
        return clientKey;
    }
}
