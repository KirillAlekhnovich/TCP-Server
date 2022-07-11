public enum Messages {
    SERVER_MOVE("102 MOVE\u0007\b"),
    SERVER_TURN_LEFT("103 TURN LEFT\u0007\b"),
    SERVER_TURN_RIGHT("104 TURN RIGHT\u0007\b"),
    SERVER_PICK_UP("105 GET MESSAGE\u0007\b"),
    SERVER_LOGOUT("106 LOGOUT\u0007\b"),
    SERVER_KEY_REQUEST("107 KEY REQUEST\u0007\b"),
    SERVER_OK("200 OK\u0007\b"),
    SERVER_LOGIN_FAILED("300 LOGIN FAILED\u0007\b"),
    SERVER_SYNTAX_ERROR("301 SYNTAX ERROR\u0007\b"),
    SERVER_LOGIC_ERROR("302 LOGIC ERROR\u0007\b"),
    SERVER_KEY_OUT_OF_RANGE_ERROR("303 KEY OUT OF RANGE\u0007\b"),
    CLIENT_RECHARGING("RECHARGING"),
    CLIENT_FULL_POWER("FULL POWER"),
    ASKING_FOR_POS("ASKING_FOR_POS");

    private final String message;

    Messages(String message) {
        this.message = message;
    }

    // Getter
    public String asString() {
        return message;
    }
}
