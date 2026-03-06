import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;

public class Client extends JPanel {
	static final long serialVersionUID = 1L;//give it a version so compiler won't complain
    TriggerType triggerType = TriggerType.TIMER_TRIGGER;
    // TriggerType triggerType = TriggerType.EVENT_TRIGGER;
    // threshold for event based sensor (in degrees)
    double threshold = 5;
    // The speed of simulation
    // (How many simulation second elapses when 1 second real time elapses)
    double simSpeed = 0.1;
    // Sensor sampling rate (per simulation second)
    double sensorSamplingRate = 100;
    // advance of simulation time (in second) per step
    double tau_sim = 0.01;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    Physics physics;
    Socket requestSocket;
    Thread physicsThread;
    Thread sensorThread, actuatorThread;
    Thread updatingUIThread;
    UpdatingUIThread animator;
    // frames per second for updating UI
    int fps = 10;
    // simulation time between two samples (in seconds)
    double sensorSamplingPeriod_sim = 1.0 / sensorSamplingRate;
    double sensorSamplingPeriod_phy = sensorSamplingPeriod_sim / simSpeed;

    String[] configInfo;

    final int APPLET_WIDTH = 800;
    final int APPLET_HEIGHT = 400;

    /**
     * This method initializes the pole state and sets up animation timing.
     */
    public void init() {

        this.setPreferredSize(new Dimension(APPLET_WIDTH, APPLET_HEIGHT));

        // Build configuration info string
        configInfo = new String[2];
        StringBuilder sb = new StringBuilder();
        sb.append("Sim. Speed: ").append(String.format("%.3f  ", simSpeed));
        sb.append("   Sim. Step: ").append(String.format("%.3f sec  ",tau_sim));
        configInfo[0] = sb.toString();

        sb = new StringBuilder();
        if(triggerType == TriggerType.EVENT_TRIGGER){
            sb.append("Event Based Sensor  ");
        }else{
            sb.append("Time Based Sensor  ");
        }
        sb.append(String.format("%.2f Hz", sensorSamplingRate));
        if(triggerType == TriggerType.EVENT_TRIGGER){
            sb.append("  Threshold: ").append(String.format("%.02f", threshold));
        }
        configInfo[1] = sb.toString();
        // -------------------------------------


        physics = new Physics(tau_sim, tau_sim / simSpeed);
        try {
            requestSocket = new Socket("localhost", 25533);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("Not able to bind to server");
        }
    }

    /**
     * This method starts animating by creating a new Thread.
     */
    public void start() {

        //Start animating!
        if (physicsThread == null) {
            physicsThread = new Thread(physics);
        }
        physicsThread.start();

        if (sensorThread == null) {
            sensorThread = new Thread(new Sensor(physics, out, triggerType, threshold, sensorSamplingPeriod_sim, sensorSamplingPeriod_phy));
        }
        sensorThread.start();

        if (actuatorThread == null) {
            actuatorThread = new Thread(new Actuator(physics, in));
        }
        actuatorThread.start();

        animator = new UpdatingUIThread(this, physics, (1000 / fps), configInfo);
        if (updatingUIThread == null) {
            updatingUIThread = new Thread(animator);
        }
        updatingUIThread.start();

    }

    /**
     * This method stops the animating thread and gets rid of the objects necessary for double buffering.
     */
    public void stop() {
        //Stop the animating thread.
        physicsThread = null;
        sensorThread = null;
        actuatorThread = null;

        if (updatingUIThread != null) {
            updatingUIThread.interrupt();
        }
        if (animator != null) {
            animator.stop();
        }

        try {
            if (out != null) {
                out.writeObject("bye"); // signal to close the sever
                out.flush();
            }

            if (in != null) {
                in.readObject();
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (requestSocket != null) {
                requestSocket.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    /**
     * This method paints the graphics by calling the update method.
     */
    @Override
    protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        if (animator != null) {
            animator.update(gr);
        }
    }
}
