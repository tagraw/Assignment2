
/**
 * This program runs as a server and controls the force to be applied to balance the Inverted Pendulum system running on the clients.
 */
import java.io.*;
import java.net.*;

public class ControlServer {

    private static ServerSocket serverSocket;
    private static final int port = 25533;

    /**
     * Main method that creates new socket and PoleServer instance and runs it.
     */
    public static void main(String[] args) throws IOException {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            System.out.println("Unable to set up port:" + ioe);
            System.exit(1);
        }
        System.out.println("Waiting for connection");
        do {
            Socket client = serverSocket.accept();
            System.out.println("\nnew client accepted.\n");
            PoleServer_handler handler = new PoleServer_handler(client);
        } while (true);
    }
}

/**
 * This class sends control messages to balance the pendulum on client side.
 */
class PoleServer_handler implements Runnable {

    // Set the number of poles
    private static final int NUM_POLES = 2;

    // leader
    private static final double TARGET_1 = 2.0;

    // spacing, pole 2 stays this far to right of pole 1
    private static final double GAP = 2.5;

    private static final double MAX_FORCE = 10.0;

    Socket connection = null;
    ObjectOutputStream out;
    ObjectInputStream in;
    static Socket clientSocket;
    Thread t;

    /**
     * Class Constructor
     */
    public PoleServer_handler(Socket socket) {
        t = new Thread(this);
        clientSocket = socket;

        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        t.start();
    }

    // double angle, angleDot, pos, posDot, action = 0, i = 0;
    /**
     * This method receives the pole positions and calculates the updated value
     * and sends them across to the client. It also sends the amount of force to
     * be applied to balance the pendulum.
     *
     * @throws ioException
     */
    void control_pendulum(ObjectOutputStream out, ObjectInputStream in) {
        try {
            while (true) {
                System.out.println("-----------------");

                // read data from client
                Object obj = in.readObject();

                // Do not process string data unless it is "bye", in which case,
                // we close the server
                if (obj instanceof String) {
                    System.out.println("STRING RECEIVED: " + (String) obj);
                    if (obj.equals("bye")) {
                        break;
                    }
                    continue;
                }

                double[] data = (double[]) (obj);
                assert (data.length == NUM_POLES * 4);
                double[] actions = new double[NUM_POLES];

                // left pole
                double ang1 = data[0];
                double angDot1 = data[1];
                double pos1 = data[2];
                double posDot1 = data[3];

                // right pole
                double ang2 = data[4];
                double angDot2 = data[5];
                double pos2 = data[6];
                double posDot2 = data[7];

                double action1 = leader_action(ang1, angDot1, pos1, posDot1);
                double action2 = follower_action(ang2, angDot2, pos2, posDot2, pos1, posDot1);

                actions[0] = clamp(action1, -MAX_FORCE, MAX_FORCE);
                actions[1] = clamp(action2, -MAX_FORCE, MAX_FORCE);

                sendMessage_doubleArray(actions);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (clientSocket != null) {
                System.out.println("closing down connection ...");
                out.writeObject("bye");
                out.flush();
                in.close();
                out.close();
                clientSocket.close();
            }
        } catch (IOException ioe) {
            System.out.println("unable to disconnect");
        }

        System.out.println("Session closed. Waiting for new connection...");

    }

    /**
     * This method calls the controller method to balance the pendulum.
     *
     * @throws ioException
     */
    public void run() {

        try {
            control_pendulum(out, in);

        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
        }

    }

    // Calculate the actions to be applied to the inverted pendulum from the
    // sensing data.
    // TODO: Current implementation assumes that each pole is controlled
    // independently. The interface needs to be changed if the control of one
    // pendulum needs sensing data from other pendulums.
    
    // task 2 controller (leader)
    double leader_action(double angle, double angleDot, double pos, double posDot) {
        double Kx = 0.1;
        double Kv = 0.3;
        double Kp = 30.0;
        double Kd = 3.0;

        double desiredAngle = Kx * (TARGET_1 - pos) - Kv * posDot;
        desiredAngle = Math.max(-0.2, Math.min(0.2, desiredAngle));

        double action = Kp * (angle - desiredAngle) + Kd * angleDot;

        return action;
    }

    // follower
    double follower_action(double angle, double angleDot, double pos, double posDot,
                                     double leaderPos, double leaderPosDot) {
        double Kx = 0.1;
        double Kv = 0.25;
        double Krel = 0.12;
        double Kp = 30.0;
        double Kd = 3.0;

        double followerTarget = leaderPos + GAP; // following

        double desiredAngle =
                Kx * (followerTarget - pos)
              - Kv * posDot
              + Krel * (leaderPosDot - posDot);

        desiredAngle = Math.max(-0.12, Math.min(0.12, desiredAngle));

        double action = Kp * (angle - desiredAngle) + Kd * angleDot;
        return action;
    }

    double clamp(double x, double low, double high) {
        if (x < low) {
            return low;
        }
        if (x > high) {
            return high;
        }
        return x;
    }

    // /**
    //  * This method sends the Double message on the object output stream.
    //  *
    //  * @throws ioException
    //  */
    // void sendMessage_double(double msg) {
    //     try {
    //         out.writeDouble(msg);
    //         out.flush();
    //         System.out.println("server>" + msg);
    //     } catch (IOException ioException) {
    //         ioException.printStackTrace();
    //     }
    // }

    /**
     * This method sends the Double message on the object output stream.
     */
    void sendMessage_doubleArray(double[] data) {
        try {
            out.writeObject(data);
            out.flush();

            System.out.print("server> ");
            for (int i = 0; i < data.length; i++) {
                System.out.print(data[i] + "  ");
            }
            System.out.println();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
