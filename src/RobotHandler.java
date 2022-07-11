import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RobotHandler implements Runnable {
    private final int SERVER_TIMEOUT = 1000; // server timeout in ms
    private static final int TIMEOUT_RECHARGING = 5000;
    private static final int RECHARGING_MESSAGE_SIZE = 12;

    private Socket robotSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Robot robot;
    private CurrentStatus currentStatus;

    public RobotHandler(Socket robotSocket) throws IOException {
        this.robotSocket = robotSocket;
        this.in = new BufferedReader(new InputStreamReader(robotSocket.getInputStream()));
        this.out = new PrintWriter(robotSocket.getOutputStream());
        this.robot = new Robot();
        this.currentStatus = CurrentStatus.GETTING_NAME;
    }

    @Override
    public void run() {
        try {
            while (currentStatus != CurrentStatus.END) {
                // Choosing timeout depending on whether robot is recharging
                if (robot.isRecharging()) {
                    robotSocket.setSoTimeout(TIMEOUT_RECHARGING);
                } else {
                    robotSocket.setSoTimeout(SERVER_TIMEOUT);
                }
                String request, response;
                // No need to read string if we're asking for the first position
                if (currentStatus == CurrentStatus.ASKING_FOR_START_POS) {
                    request = Messages.ASKING_FOR_POS.asString();
                } else {
                    request = readInput();
                }
                System.out.println("[SERVER] Received string: " + request);
                if (request.equals(Messages.SERVER_SYNTAX_ERROR.asString())) {
                    response = Messages.SERVER_SYNTAX_ERROR.asString();
                } else if (request.equals(Messages.CLIENT_RECHARGING.asString()) || request.equals(Messages.CLIENT_FULL_POWER.asString())) {
                    continue; // keeping silent while recharging
                } else {
                    response = generateResponse(request);
                }
                System.out.println("[SERVER] Send response: " + response);
                out.print(response);
                out.flush();
            }
            System.out.println("[SERVER] Connection on thread " + Thread.currentThread().getId() + " is stopped");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanResources();
        }
    }

    private int maxMessageLength() {
        switch (currentStatus) {
            case GETTING_NAME -> {
                return 20;
            }
            case GETTING_KEY -> {
                return 5;
            }
            case GETTING_CONFIRMATION -> {
                return 7;
            }
            case PROCESSING_START_POS, DEFINING_START_DIRECTION, MOVING -> {
                return 12;
            }
            case READING_MESSAGE -> {
                return 100;
            }
        }
        return 0;
    }

    private String generateResponse(String request) {
        switch (currentStatus) {
            case GETTING_NAME -> {
                return createRobot(request);
            }
            case GETTING_KEY -> {
                return checkKey(request);
            }
            case GETTING_CONFIRMATION -> {
                return checkClientConfirmation(request);
            }
            case ASKING_FOR_START_POS -> {
                currentStatus = CurrentStatus.PROCESSING_START_POS;
                return Messages.SERVER_MOVE.asString();
            }
            case PROCESSING_START_POS -> {
                return checkProcessing(request);
            }
            case DEFINING_START_DIRECTION -> {
                return defineDirection(request);
            }
            case MOVING -> {
                return moveRobot(request);
            }
            case READING_MESSAGE -> {
                currentStatus = CurrentStatus.END;
                if (request.contains(Messages.CLIENT_RECHARGING.asString()) || robot.isRecharging()) {
                    return Messages.SERVER_LOGIC_ERROR.asString();
                }
                return Messages.SERVER_LOGOUT.asString();
            }
        }
        return null;
    }

    private String checkProcessing(String request) {
        if (Position.parse(request).isEmpty()) {
            return Messages.SERVER_SYNTAX_ERROR.asString();
        }
        robot.setPrevPosition(Position.parse(request).get());
        currentStatus = CurrentStatus.DEFINING_START_DIRECTION;
        return Messages.SERVER_MOVE.asString();
    }

    private String defineDirection(String request) {
        if (Position.parse(request).isEmpty()) {
            return Messages.SERVER_SYNTAX_ERROR.asString();
        }
        robot.setCurrPosition(Position.parse(request).get());
        if (robot.getPrevPosition().isEqual(robot.getCurrPosition())) {
            return definingDirectionWhenStuck();
        } else {
            return definingDirectionWhenFree();
        }
    }

    private String definingDirectionWhenStuck() {
        if (!robot.isLastMoveForward()) {
            robot.setLastMoveForward(true);
            return Messages.SERVER_MOVE.asString();
        }
        return solveStuck();
    }

    private String definingDirectionWhenFree() {
        if (robot.getCurrPosition().x < robot.getPrevPosition().x) {
            robot.setDirection(Direction.LEFT);
        }
        if (robot.getCurrPosition().x > robot.getPrevPosition().x) {
            robot.setDirection(Direction.RIGHT);
        }
        if (robot.getCurrPosition().y < robot.getPrevPosition().y) {
            robot.setDirection(Direction.DOWN);
        }
        if (robot.getCurrPosition().y > robot.getPrevPosition().y) {
            robot.setDirection(Direction.UP);
        }
        currentStatus = CurrentStatus.MOVING;
        robot.setLastMoveForward(true);
        return Messages.SERVER_MOVE.asString();
    }

    private String moveRobot(String request) {
        // Setting previous position and getting new current position from request
        robot.setPrevPosition(robot.getCurrPosition());
        if (Position.parse(request).isEmpty()) {
            return Messages.SERVER_SYNTAX_ERROR.asString();
        }
        robot.setCurrPosition(Position.parse(request).get());

        // If robot has finished its way to the target
        if (robot.getCurrPosition().isAtTheEnd()) {
            currentStatus = CurrentStatus.READING_MESSAGE;
            return Messages.SERVER_PICK_UP.asString();
        }

        if (robot.isHasToMove()) {
            robot.setHasToMove(false);
            robot.setLastMoveForward(true);
            return Messages.SERVER_MOVE.asString();
        }

        // If position hasn't changed -> we stuck
        if (robot.getPrevPosition().isEqual(robot.getCurrPosition()) && robot.isLastMoveForward()) {
            return solveStuck();
        }

        return robot.calcBestMove();
    }

    private String solveStuck() {
        robot.setHasToMove(true);
        robot.setLastMoveForward(false);
        // Default behavior if robot stuck at the first move
        if (robot.getDirection() == Direction.UNSET) {
            return Messages.SERVER_TURN_LEFT.asString();
        }
        return robot.avoidObstacle();
    }

    private String createRobot(String robotName) {
        this.robot = new Robot(robotName);
        currentStatus = CurrentStatus.GETTING_KEY;
        return Messages.SERVER_KEY_REQUEST.asString();
    }

    private String calcHash(Keys key) {
        // Getting name without ending sequence
        String robotName = robot.getName().substring(0, robot.getName().length() - 2);
        int hash = robot.calcNameHash(robotName);
        // Robot keys calculation
        robot.setClientKey((robot.getHashName() + key.getClientKey()) % 65536);
        hash = (hash + key.getServerKey()) % 65536;
        return hash + "\u0007\b";
    }

    private String checkKey(String keyId) {
        for (int i = 0; i < keyId.length() - 2; i++) {
            if (!Character.isDigit(keyId.charAt(i))) {
                return Messages.SERVER_SYNTAX_ERROR.asString();
            }
        }
        String receivedKey = "KEY" + keyId.substring(0, keyId.length() - 2);
        // Looking for KEY# among available keys where # is key number
        for (Keys key : Keys.values()) {
            if (key.name().equals(receivedKey)) {
                currentStatus = CurrentStatus.GETTING_CONFIRMATION;
                return calcHash(key);
            }
        }
        System.out.println("[SERVER] Given key was not found");
        currentStatus = CurrentStatus.END;
        return Messages.SERVER_KEY_OUT_OF_RANGE_ERROR.asString();
    }

    private String checkClientConfirmation(String receivedClientKey) {
        for (int i = 0; i < receivedClientKey.length() - 2; i++) {
            if (!Character.isDigit(receivedClientKey.charAt(i))) {
                return Messages.SERVER_SYNTAX_ERROR.asString();
            }
        }
        int receivedClient = Integer.parseInt(receivedClientKey.substring(0, receivedClientKey.length() - 2));
        // Checking if keys match
        if (receivedClient == robot.getClientKey()) {
            currentStatus = CurrentStatus.ASKING_FOR_START_POS;
            System.out.println("[SERVER] Login successful");
            return Messages.SERVER_OK.asString();
        }
        currentStatus = CurrentStatus.END;
        return Messages.SERVER_LOGIN_FAILED.asString();
    }

    // We have to read input by symbols because of weird java bug with readline function :(
    private String readInput() throws IOException {
        StringBuilder receivedString = new StringBuilder();
        int readChar, messageLength = 0;
        while ((readChar = in.read()) != -1) {
            receivedString.append((char) readChar);
            messageLength++;
            if (stringIsTooLong(messageLength)) {
                return generateError("[SERVER] Received message is too long", Messages.SERVER_SYNTAX_ERROR.asString());
            }

            // If '/a' read
            if (readChar == '\u0007') {
                if (stringIsTooLong(messageLength)) {
                    return generateError("[SERVER] Received message is too long", Messages.SERVER_SYNTAX_ERROR.asString());
                }
                // Reading next symbol after '/a'
                readChar = in.read();
                receivedString.append((char) readChar);
                messageLength++;
                if (readChar == '\b') {
                    return inputChecksWhenMessageIsRead(receivedString.toString(), messageLength);
                }
            }
        }
        // We haven't read '/a/b'
        return generateError("[SERVER] No ending sequence detected", Messages.SERVER_SYNTAX_ERROR.asString());
    }

    private String inputChecksWhenMessageIsRead(String receivedString, int messageLength) {
        if (receivedString.contains(Messages.CLIENT_RECHARGING.asString())) {
            if (robot.isRecharging()) {
                return generateError("[SERVER] Robot is already recharging", Messages.SERVER_LOGIC_ERROR.asString());
            }
            robot.setRecharging(true);
            return generateError("[SERVER] Robot is now recharging", Messages.CLIENT_RECHARGING.asString());
        } else if (receivedString.contains(Messages.CLIENT_FULL_POWER.asString())) {
            if (robot.isRecharging()) {
                robot.setRecharging(false);
                return generateError("[SERVER] Robot has full power", Messages.CLIENT_FULL_POWER.asString());
            }
            currentStatus = CurrentStatus.END;
            return generateError("[SERVER] Robot tried to finish recharging before starting", Messages.SERVER_LOGIC_ERROR.asString());
        } else if (messageLength > maxMessageLength()) {
            return generateError("[SERVER] Received message is too long", Messages.SERVER_SYNTAX_ERROR.asString());
        }
        // Returning received string if everything was fine
        return receivedString;
    }

    // Outputting error message and changing current status if needed
    private String generateError(String errorMessage, String errorType) {
        System.out.println(errorMessage);
        if (errorType.equals(Messages.SERVER_SYNTAX_ERROR.asString())) {
            currentStatus = CurrentStatus.END;
        }
        return errorType;
    }

    private boolean stringIsTooLong(int messageLength) {
        return messageLength >= maxMessageLength() && messageLength >= RECHARGING_MESSAGE_SIZE;
    }

    private void cleanResources() {
        try {
            robotSocket.close();
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
