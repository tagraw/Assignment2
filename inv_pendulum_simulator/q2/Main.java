import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Inverted Pendulum Simulator");
            Client client = new Client();
            client.init();

            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(client);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            client.start();

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    client.stop();
                    System.exit(0);
                }
            });
        });
    }
}
