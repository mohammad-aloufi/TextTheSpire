package textTheSpire;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.TheSilent;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.GremlinMatchGame;
import com.megacrit.cardcrawl.events.shrines.GremlinWheelGame;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import communicationmod.ChoiceScreenUtils;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.patches.GremlinMatchGamePatch;
import org.eclipse.swt.widgets.Display;

import javax.smartcardio.Card;
import java.util.ArrayList;

public class Choices extends AbstractWindow{

    private boolean checkedSave = false;
    private boolean haveSave;

    public Choices(Display display){
        isVisible = true;
        window = new Window(display,"Choices", 300, 300);
    }

    public String getText(){

        if(window.shell.isDisposed()){
            Display.getDefault().dispose();
            Gdx.app.exit();
        }

        StringBuilder s = new StringBuilder();
        s.append("\r\n");

        if(CommandExecutor.isInDungeon()){

            checkedSave = false;

            ChoiceScreenUtils.ChoiceType currChoice = ChoiceScreenUtils.getCurrentChoiceType();

            if(currChoice == ChoiceScreenUtils.ChoiceType.HAND_SELECT){
                s.append("Hand Selection\r\n");
                s.append(AbstractDungeon.handCardSelectScreen.selectionReason + "\r\n");
                s.append("Select " + AbstractDungeon.handCardSelectScreen.numCardsToSelect + "\r\n");
                s.append("Number Selected: " + AbstractDungeon.handCardSelectScreen.numSelected + "\r\n");
            }else if(currChoice == ChoiceScreenUtils.ChoiceType.GRID){
                s.append("Grid Selection\r\n");
                s.append("Number Selected: " + AbstractDungeon.gridSelectScreen.selectedCards.size() + "\r\n");
                if(AbstractDungeon.gridSelectScreen.upgradePreviewCard != null){
                    AbstractCard preview = AbstractDungeon.gridSelectScreen.upgradePreviewCard;
                    s.append("Upgrade Preview : " + TextTheSpire.inspectCard(preview));
                }
            }

            //If in combat check if choices exists, otherwise remove window
            if(AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

                if (ChoiceScreenUtils.isConfirmButtonAvailable()) {
                    s.append(ChoiceScreenUtils.getConfirmButtonText()).append("\r\n");
                }
                if (ChoiceScreenUtils.isCancelButtonAvailable()) {
                    s.append(ChoiceScreenUtils.getCancelButtonText()).append("\r\n");
                }

                int count = 1;
                ArrayList<String> cards = ChoiceScreenUtils.getCurrentChoiceList();

                if (cards.size() == 0) {
                    return s.toString();
                }

                for (String c : cards) {

                    s.append(count).append(":").append(c).append("\r\n");
                    count++;

                }

                return s.toString();

            }else{

                //If not in combat, check and display choices

                int count = 1;

                if (ChoiceScreenUtils.isConfirmButtonAvailable()) {
                    s.append(ChoiceScreenUtils.getConfirmButtonText()).append("\r\n");
                }
                if (ChoiceScreenUtils.isCancelButtonAvailable()) {
                    s.append(ChoiceScreenUtils.getCancelButtonText()).append("\r\n");
                }

                //Event choices
                if (ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.EVENT) {

                    s.append(AbstractDungeon.getCurrRoom().event.getClass().getSimpleName()).append("\r\n");

                    ArrayList<LargeDialogOptionButton> activeButtons = ChoiceScreenUtils.getActiveEventButtons();

                    if (activeButtons.size() > 0) {
                        for(LargeDialogOptionButton button : activeButtons) {
                            s.append(count).append(": ").append(stripColor(button.msg).toLowerCase()).append("\r\n");
                            count++;
                        }
                    } else if(AbstractDungeon.getCurrRoom().event instanceof GremlinWheelGame) {
                        s.append(count).append(": ").append("spin").append("\r\n");
                    } else if(AbstractDungeon.getCurrRoom().event instanceof GremlinMatchGame) {
                        GremlinMatchGame event = (GremlinMatchGame) (AbstractDungeon.getCurrRoom().event);
                        CardGroup gameCardGroup = (CardGroup) ReflectionHacks.getPrivate(event, GremlinMatchGame.class, "cards");
                        for (AbstractCard c : gameCardGroup.group) {
                            if (c.isFlipped) {
                                s.append(count).append(": ").append(String.format("card%d", GremlinMatchGamePatch.cardPositions.get(c.uuid))).append("\r\n");
                                count++;
                            }
                        }
                    }

                } else if (ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.SHOP_SCREEN) {

                    //Shop screen. Makes sure prices are shown
                    for (String c : priceShopScreenChoices()) {
                        s.append(count).append(":").append(c).append("\r\n");
                        count++;
                    }

                }else if (ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.MAP){

                    //Also shows current position
                    if (AbstractDungeon.firstRoomChosen)
                        s.append("Floor:").append(AbstractDungeon.currMapNode.y + 1).append(", X:").append(AbstractDungeon.currMapNode.x).append("\r\n");
                    else
                        s.append("Floor:0\r\n");

                    if (ChoiceScreenUtils.bossNodeAvailable()) {

                        s.append(count).append(":");
                        s.append("boss").append("\r\n");

                    } else if(!Inspect.has_inspected) {

                        //Displays node type and xPos for each choice
                        for (MapRoomNode n : ChoiceScreenUtils.getMapScreenNodeChoices()) {
                            s.append(count).append(":");
                            if(AbstractDungeon.player.hasRelic("WingedGreaves") && (AbstractDungeon.player.getRelic("WingedGreaves")).counter > 0 && !AbstractDungeon.getCurrMapNode().isConnectedTo(n)) {
                                s.append(Map.nodeType(n)).append("Winged ").append(n.x).append("\r\n");
                            } else {
                                s.append(Map.nodeType(n)).append(n.x).append("\r\n");
                            }
                            count++;
                        }

                    }else{

                        s.append("Inspected ").append(Map.nodeType(Inspect.destination)).append(Inspect.destination.y + 1).append(" ").append(Inspect.destination.x).append("\r\n");

                        for (MapRoomNode n : ChoiceScreenUtils.getMapScreenNodeChoices()) {
                            s.append(count).append(":");
                            s.append(Map.nodeType(n));
                            if (Inspect.inspected_map.contains(n)) {
                                s.append("On Track ").append(n.x).append("\r\n");
                            }else if(AbstractDungeon.player.hasRelic("WingedGreaves") && (AbstractDungeon.player.getRelic("WingedGreaves")).counter > 0 && !AbstractDungeon.getCurrMapNode().isConnectedTo(n)){
                                s.append("Winged ").append(n.x).append("\r\n");
                            }else{
                                s.append("Diverge ").append(n.x).append("\r\n");
                            }

                            count++;
                        }

                    }

                } else if(ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.COMBAT_REWARD) {
                    for(RewardItem reward : AbstractDungeon.combatRewardScreen.rewards) {
                        if(reward.type == RewardItem.RewardType.POTION)
                            s.append(count).append(":").append(reward.potion.name).append("\r\n");
                        else if(reward.type == RewardItem.RewardType.RELIC)
                            s.append(count).append(":").append(reward.relic.name).append("\r\n");
                        else
                            s.append(count).append(":").append(reward.type.name().toLowerCase()).append("\r\n");
                        count++;
                    }
                } else {
                    //Catch all for all remaining choices. They are usually displayed in a list with numbers a simple name
                    for (String c : ChoiceScreenUtils.getCurrentChoiceList()) {
                        s.append(count).append(":").append(c).append("\r\n");
                        count++;
                    }
                }

                return s.toString();

            }

        }else{

            //Not in dungeon. Check if save exists. checkedSave so we don't check each time.
            if(!checkedSave) {
                if (CardCrawlGame.characterManager.anySaveFileExists()) {
                    s.append("restart [class] [ascension] [seed]\r\n");
                    s.append("continue\r\n");
                    haveSave = true;
                } else {
                    s.append("start [class] [ascension] [seed]\r\n");
                    haveSave = false;
                }

                checkedSave = true;
            }else{
                if (haveSave) {
                    s.append("restart [class] [ascension] [seed]\r\n");
                    s.append("continue\r\n");
                } else {
                    s.append("start [class] [ascension] [seed]\r\n");
                }
            }

            TipTracker.disableAllFtues();

            for(AbstractPlayer.PlayerClass p : AbstractPlayer.PlayerClass.values()){

                s.append(p.name().toLowerCase()).append(" ");

                if(TextTheSpire.characterUnlocked(p.name().toLowerCase()))
                    s.append(TextTheSpire.ascensionLevel(p.name().toLowerCase())).append("\r\n");
                else
                    s.append("locked\r\n");

            }

            return s.toString();

        }

    }

    //Returns a list of all shop items with prices
    public static ArrayList<String> priceShopScreenChoices(){
        ArrayList<String> choices = new ArrayList<>();
        ArrayList<Object> shopItems = getAvailableShopItems();
        for (Object item : shopItems) {
            if (item instanceof String) {
                choices.add((String) item);
            } else if (item instanceof AbstractCard) {
                choices.add(((AbstractCard) item).name.toLowerCase() + "-" + ((AbstractCard) item).price);
            } else if (item instanceof StoreRelic) {
                choices.add(((StoreRelic)item).relic.name + "-" + ((StoreRelic) item).price);
            } else if (item instanceof StorePotion) {
                choices.add(((StorePotion)item).potion.name + "-" + ((StorePotion) item).price);
            }
        }
        return choices;
    }

    /*
    Gets a list of all shop items.
    Copied from CommunicationMod
     */
    public static ArrayList<Object> getAvailableShopItems() {
        ArrayList<Object> choices = new ArrayList<>();
        ShopScreen screen = AbstractDungeon.shopScreen;
        if(screen.purgeAvailable && AbstractDungeon.player.gold >= ShopScreen.actualPurgeCost) {
            choices.add("purge-" + ShopScreen.actualPurgeCost);
        }
        for(AbstractCard card : ChoiceScreenUtils.getShopScreenCards()) {
            if(card.price <= AbstractDungeon.player.gold) {
                choices.add(card);
            }
        }
        for(StoreRelic relic : ChoiceScreenUtils.getShopScreenRelics()) {
            if(relic.price <= AbstractDungeon.player.gold) {
                choices.add(relic);
            }
        }
        for(StorePotion potion : ChoiceScreenUtils.getShopScreenPotions()) {
            if(potion.price <= AbstractDungeon.player.gold) {
                choices.add(potion);
            }
        }
        return choices;
    }

    /*
    Params:
        input - an Event choice
    Returns:
        A String with color mods removed from input
     */
    public static String stripColor(String input) {
        input = input.replace("#r", "");
        input = input.replace("#g", "");
        input = input.replace("#b", "");
        input = input.replace("#y", "");
        return input;
    }

}
