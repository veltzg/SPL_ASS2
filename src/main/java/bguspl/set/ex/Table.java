package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)


    /**
     * Mapping tokens per player and slot
     */
    protected boolean [][] tokensPTS;

    /**
     *
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {
        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.tokensPTS = new boolean[env.config.players][env.config.tableSize];
        //connects dealer to player
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * synchronize(A)
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        env.ui.placeCard(card, slot);
    }

    /**
     * synchronize(A)
     * todo: env.ui.removeCard (deletes from the drawing)
     * todo: handle all other tokens that are placed on this slot
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        int cardToRemove = slotToCard[slot];
        slotToCard[slot] = null;
        cardToSlot[cardToRemove] = null;
        env.ui.removeCard(slot);
    }

    /**
     * synchronize!!
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        tokensPTS[player][slot] = true;
        env.ui.placeToken(player, slot);
    }

    /**
     * synchronized!!!
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        tokensPTS[player][slot] = false;
        env.ui.removeToken(player, slot);
        return tokensPTS[player][slot];
    }

    /**
     *
     * @param playerId - the player the tokens belongs to.
     * @return  - an Array of 3 cards contains of the cards the player placed his tokens on
     */
    public int[] getPlayerTokenedCards (int playerId) {
        int[] cardsToReturn = new int[3];
        int foundTokens = 0;
        for (int i = 0; i < tokensPTS[playerId].length && foundTokens < 3; i++) {
            if (tokensPTS[playerId][i]) {
                cardsToReturn[foundTokens] = slotToCard[i];
                foundTokens++;
            }
        }
        return cardsToReturn;
    }

    //added
    public int findEmptySlot(){
        for(int i = 0; i < slotToCard.length ; i++){
            if(slotToCard[i] == null)
                return i;
        }
        return -1;
    }

    //added
    /**
     *
     */
    public boolean isPlayerTokenOnSlot(int playerId, int slot) {
        return tokensPTS[playerId][slot];
    }

    /**
     *
     * @param slot
     * @return
     */
    public synchronized LinkedList<Integer> removeTokens(int slot) {
        LinkedList<Integer> removedPlayers = new LinkedList();
        for (int i = 0; i < tokensPTS.length; i++) {
            if (tokensPTS[i][slot]) {
                removedPlayers.add(i);
                tokensPTS[i][slot] = false;
            }
        }
        env.ui.removeTokens();
        return removedPlayers;
    }

    /**
     *
     */
    public void removeAllTokens() {
        for (int i = 0; i < tokensPTS.length; i++) {
            for (int j = 0; j < tokensPTS[i].length; j++) {
                tokensPTS[i][j] = false;
            }
        }
        env.ui.removeTokens();
    }

    public boolean removePlayersTokens (int player) {
        int foundTokens = 0;
        for (int i = 0; i < tokensPTS[player].length && foundTokens < 3; i++) {
            if (tokensPTS[player][i]) {
                tokensPTS[player][i] = false;
                env.ui.removeToken(player, i);
                foundTokens++;
            }
        }
        return (foundTokens == 3);
    }
}
