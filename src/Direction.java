public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UNSET;

    public boolean isEqual(Direction dir2) {
        return this == dir2;
    }

    public boolean isRotatedClockwise(Direction dir2) {
        switch (this) {
            case UP -> {
                return dir2 == RIGHT;
            }
            case DOWN -> {
                return dir2 == LEFT;
            }
            case LEFT -> {
                return dir2 == UP;
            }
            case RIGHT -> {
                return dir2 == DOWN;
            }
        }
        return false;
    }

    public boolean isRotatedCounterClockwise(Direction dir2) {
        switch (this) {
            case UP -> {
                return dir2 == LEFT;
            }
            case DOWN -> {
                return dir2 == RIGHT;
            }
            case LEFT -> {
                return dir2 == DOWN;
            }
            case RIGHT -> {
                return dir2 == UP;
            }
        }
        return false;
    }
}
