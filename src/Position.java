import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Position {
    public int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Parsing message into Position object
    public static Optional<Position> parse(String message) {
        Pattern pattern = Pattern.compile("OK\\s(-?\\d+)\\s(-?\\d+)\\S");
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find() || message.contains(".")) {
            return Optional.empty();
        }
        return Optional.of(new Position(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
    }

    public boolean isEqual(Position pos2) {
        return this.x == pos2.x && this.y == pos2.y;
    }

    public boolean isAtTheEnd() {
        return this.x == 0 && this.y == 0;
    }
}
