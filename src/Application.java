import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Application {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("[SERVER] Invalid number of arguments");
            return;
        }
        int port = Integer.parseInt(args[0]);
        if (port < 1024) {
            System.out.println("[SERVER] Port number must be above 1024");
            return;
        }
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(port);
            System.out.println("[SERVER] Listener socket opened on: " + listener.getLocalSocketAddress());
            while (true) {
                Socket robotSocket = listener.accept();
                System.out.println("[SERVER] New robot registered");
                RobotHandler robotThread = new RobotHandler(robotSocket);
                new Thread(robotThread).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (listener != null) {
                listener.close();
            }
        }
    }
}
