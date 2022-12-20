package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * The list of the players that placed 3 tokens on the board.
     */
    private Queue<Integer> toCheckQueue;

    /**
     * The dealer semaphore
     */
    protected Semaphore semaphore;
    /**
     * Timer
     */
    private long startTime = 60;
    private final long TURN_TIME_INDICATOR = 0;




    /**
     * The class constructor
     *
     * @param env
     * @param table
     * @param players
     */
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        this.toCheckQueue = new LinkedList<>();
        this.semaphore = new Semaphore(1, true);
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        for (Player player: players) {
            Thread playerThread = new Thread(player);
            playerThread.start();
            System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        }
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() - env.config.turnTimeoutMillis + 1000;
        updateTimerDisplay(true);
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            if (!toCheckQueue.isEmpty()) {
                synchronized (this) {
                    int playerID = toCheckQueue.peek();
                    boolean isSet = dealerCheck(playerID);
                    if (isSet) {
                        removeCardsFromTable();
                        placeCardsOnTable();
                    } else {
                        removeTokensFromTable(playerID);
                    }
                    rewardOrPenalizePlayer(toCheckQueue.poll(), isSet);
                    /*players[playerID].locked = false;
                    System.out.println("Dealer is about to notify all but everyone else but " + playerID + " should get locked again");
                    notifyAll();*/
                }
            }
        }
    }


    private void removeTokensFromTable(int player) {
        if (table.removePlayersTokens(player)) {
            players[player].removeAllTokens();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        //To close all threads
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks if any cards should be removed from the table and returns them to the deck.
     */
    private void removeCardsFromTable() {
        int[] cardsToRemove = table.getPlayerTokenedCards(toCheckQueue.peek());
        synchronized (table) { //should it be syncronized here?
            for (int card : cardsToRemove) {
                System.out.println(card);
                Integer slot = table.cardToSlot[card];
                if (slot == null)
                    System.out.println(slot);
                LinkedList<Integer> playersToRemoveToken = table.removeTokens(slot);
                for (Integer player : playersToRemoveToken) {
                    players[player].decreasePlacedTokens();
                }
                table.removeCard(slot);
            }
        }
    }


    private boolean dealerCheck(int player) {
        int [] cardsToCheck = table.getPlayerTokenedCards(player);
        boolean isSet = env.util.testSet(cardsToCheck);
        return isSet;
    }

    private void rewardOrPenalizePlayer(int player, boolean wasRight) {
        if (wasRight) {
            players[player].point();
        }
        else {
            players[player].penalty();
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        while(!deck.isEmpty() && table.findEmptySlot() != -1){
            table.placeCard(randomChooseCardFromDeck(), table.findEmptySlot());
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        if(reshuffleTime - System.currentTimeMillis() > 5000){
            try{
                Thread.sleep(env.config.tableDelayMillis);
            }
            catch (InterruptedException ignored){}
        }
        else{
            try{
                Thread.sleep(10);
            }
            catch (InterruptedException ignored){}
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(reset){
            reshuffleTime = Long.MAX_VALUE;
            startTime = System.currentTimeMillis();
            if(env.config.turnTimeoutMillis > TURN_TIME_INDICATOR){
                reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
                //env.ui.setCountdown(60,false);
            }
        }
        if(env.config.turnTimeoutMillis > TURN_TIME_INDICATOR){
            long timeLeft = reshuffleTime-System.currentTimeMillis();
            boolean warn = timeLeft < env.config.turnTimeoutWarningMillis;
            env.ui.setCountdown(timeLeft, warn);
        }
        else if(env.config.turnTimeoutMillis == TURN_TIME_INDICATOR){
            long timePassed = System.currentTimeMillis() - startTime;
            env.ui.setElapsed(timePassed);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        for (int i = 0; i < table.slotToCard.length; i++) {
            int addToDeck = table.slotToCard[i];
            table.slotToCard[i] = null;
            table.cardToSlot[addToDeck] = null;
            deck.add(addToDeck);
        }
        table.removeAllTokens();
        for (Player player: players) {
            player.removeAllTokens();
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        int max = 0;
        int count = 0;
        for(Player p: players){
            if(p.getScore() > max){
                count = 1;
                max = p.getScore();
            }
            else{
                if(p.getScore() == max)
                    count++;
            }
        }
        int[] winners = new int[count];
        int i = 0;
        for(Player p: players) {
            if(p.getScore() == max)
                winners[i] = p.id;
            i++;
        }
        env.ui.announceWinner(winners);
    }

    //added
    private int randomChooseCardFromDeck(){
        Random rand = new Random();
        return deck.remove(rand.nextInt(deck.size()));
    }

    // its a try
    public synchronized void addToCheckList (int playerID) {
        toCheckQueue.add(playerID);
    }
}