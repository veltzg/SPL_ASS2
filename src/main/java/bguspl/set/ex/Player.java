package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    protected Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * The queue for actions the thread needs to do by order.
     */
    private BlockingQueue<Integer> actionQueue;

    /**
     * Number of tokens the player placed on board
     */
    private int placedTokens;

    /**
     * its a try
     */
    private Dealer dealer;

    //protected boolean locked;
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.actionQueue = new ArrayBlockingQueue<>(3);
        this.placedTokens = 0;
        // its a try
        this.dealer = dealer;
        //this.locked = false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        if (!human) createArtificialIntelligence();
        while (!terminate) {
            // TODO implement main player loop
            //added
            if(!actionQueue.isEmpty())
                act(actionQueue.remove());
                //end
            else{
                synchronized (this) {
                    notifyAll();
                }
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
            while (!terminate) {
                // TODO implement player key press simulator
                Random rand = new Random();
                keyPressed(rand.nextInt(12));
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * synchronize!
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public synchronized void keyPressed(int slot) {
        // TODO implement
        actionQueue.add(slot);
        try {
            Thread.sleep(50);
        }
        catch (InterruptedException ignored){}
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, --score);
        synchronized (this) {
            notifyAll();
        }
    }

    public int getScore() {
        return score;
    }

    /*public void act(int slot) {
        //synchronized(table)
        if (placedTokens < 3) {
            if (!table.isPlayerTokenOnSlot(id, slot)) {
                table.placeToken(id, slot);
                placedTokens++;
                if (placedTokens == 3) {
                    try {
                        dealer.semaphore.acquire();
                        dealer.addToCheckList(this.id);
                    }
                    catch (InterruptedException ignored) { }
                }
            }
            else {
                table.removeToken(id, slot);
                decreasePlacedTokens();
            }
        }
    }*/
    public void act(int slot){
        if(placedTokens < 3) {
            if (!table.isPlayerTokenOnSlot(id, slot)) {
                table.placeToken(id, slot);
                placedTokens++;
                if (placedTokens == 3) {
                    try {
                        synchronized (this){
                        dealer.semaphore.acquire();
                        dealer.addToCheckList(this.id);
                        dealer.semaphore.release();
                        wait();
                        }
                        /*synchronized (dealer) {
                            locked = true;
                            System.out.println("I " + id + " was locked for the 1st time");
                            dealer.wait();
                            while (locked){
                                System.out.println("I " + id + " was locked again");
                                dealer.wait();
                            }
                            System.out.println("I " + id + " am Free");
                        } */
                    }
                    catch (InterruptedException ignored){};
                }
            }
            else {
                table.removeToken(id, slot);
                decreasePlacedTokens();
            }
            /*synchronized (this) {
                if (actionQueue.size() < 3) {
                    notifyAll();
                }*/
        }
    }


    public void decreasePlacedTokens() {
        placedTokens--;
    }

    public void removeAllTokens() {
        placedTokens = 0;
    }
}
