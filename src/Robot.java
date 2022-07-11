public class Robot {
    private final String name;
    private int hashName;
    private int clientKey;
    private Position prevPosition;
    private Position currPosition;
    private Direction direction = Direction.UNSET;
    private boolean lastMoveForward = true;
    private boolean hasToMove = false;
    private boolean recharging = false;

    public Robot() {
        name = null;
    }

    public Robot(String name) {
        this.name = name;
    }

    public int calcNameHash(String name) {
        for (int i = 0; i < name.length(); i++) {
            hashName += name.charAt(i);
        }
        hashName *= 1000;
        hashName %= 65536;
        return hashName;
    }

    public void setClientKey(int clientKey) {
        this.clientKey = clientKey;
    }

    public String getName() {
        return name;
    }

    public int getHashName() {
        return hashName;
    }

    public int getClientKey() {
        return clientKey;
    }

    public Position getPrevPosition() {
        return prevPosition;
    }

    public void setPrevPosition(Position prevPosition) {
        this.prevPosition = prevPosition;
    }

    public Position getCurrPosition() {
        return currPosition;
    }

    public void setCurrPosition(Position currPosition) {
        this.currPosition = currPosition;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean isLastMoveForward() {
        return lastMoveForward;
    }

    public void setLastMoveForward(boolean lastMoveForward) {
        this.lastMoveForward = lastMoveForward;
    }

    public boolean isHasToMove() {
        return hasToMove;
    }

    public void setHasToMove(boolean hasToMove) {
        this.hasToMove = hasToMove;
    }

    public boolean isRecharging() {
        return recharging;
    }

    public void setRecharging(boolean recharging) {
        this.recharging = recharging;
    }

    // Finding 2 best directions according to current position and target
    public Pair<Direction, Direction> determineTargetArea() {
        if (currPosition.x >= 0) {
            if (currPosition.y >= 0) {
                return new Pair(Direction.LEFT, Direction.DOWN);
            } else {
                return new Pair(Direction.LEFT, Direction.UP);
            }
        } else {
            if (currPosition.y >= 0) {
                return new Pair(Direction.RIGHT, Direction.DOWN);
            } else {
                return new Pair(Direction.RIGHT, Direction.UP);
            }
        }
    }

    public String calcBestMove() {
        Pair<Direction, Direction> targetArea = determineTargetArea();
        // Deciding which way to move (among 2 best ones)
        Direction bestDir = targetArea.first;
        if (Math.abs(currPosition.x) < Math.abs(currPosition.y)) {
            bestDir = targetArea.second;
        }

        // If best direction is equal to current we just move forward
        if (direction.isEqual(bestDir)) {
            setLastMoveForward(true);
            return Messages.SERVER_MOVE.asString();
        }

        // If best direction is rotated clockwise we turn right
        if (direction.isRotatedClockwise(bestDir)) {
            setDirection(bestDir);
            setLastMoveForward(false);
            return Messages.SERVER_TURN_RIGHT.asString();
        }

        // If best direction is rotated counter-clockwise we turn left
        if (direction.isRotatedCounterClockwise(bestDir)) {
            setDirection(bestDir);
            setLastMoveForward(false);
            return Messages.SERVER_TURN_LEFT.asString();
        }

        // If best direction is opposite we turn left
        switch (direction) {
            case UP -> setDirection(Direction.LEFT);
            case DOWN -> setDirection(Direction.RIGHT);
            case LEFT -> setDirection(Direction.DOWN);
            case RIGHT -> setDirection(Direction.UP);
        }
        setLastMoveForward(false);
        return Messages.SERVER_TURN_LEFT.asString();
    }

    public String avoidObstacle() {
        // Finding 2nd best direction in order to avoid obstacle
        Pair<Direction, Direction> targetArea = determineTargetArea();
        Direction secondBestDir = targetArea.second;
        if (direction == targetArea.second) {
            secondBestDir = targetArea.first;
        }

        // Turning robot depending on required direction
        if (direction.isRotatedClockwise(secondBestDir)) {
            setDirection(secondBestDir);
            return Messages.SERVER_TURN_RIGHT.asString();
        }
        if (direction.isRotatedCounterClockwise(secondBestDir)) {
            setDirection(secondBestDir);
            return Messages.SERVER_TURN_LEFT.asString();
        }

        // If required direction is opposite
        switch (direction) {
            case UP -> setDirection(Direction.LEFT);
            case DOWN -> setDirection(Direction.RIGHT);
            case LEFT -> setDirection(Direction.DOWN);
            case RIGHT -> setDirection(Direction.UP);
        }
        return Messages.SERVER_TURN_LEFT.asString();
    }
}
