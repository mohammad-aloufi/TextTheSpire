package textTheSpire;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;

public class Inspect {

    public Window inspect;

    public static ArrayList<MapRoomNode> inspected_map;
    public static boolean has_inspected = false;
    public static MapRoomNode destination;

    public Inspect(Display display){
        inspect = new Window(display,"Output",450,525);
    }

    public void setText(String s){
        inspect.setText(s);
    }

    public static String inspectMap(String[] tokens){
        int floor;
        int x;

        StringBuilder s = new StringBuilder();
        s.append("\r\n");

        try{
            floor = Integer.parseInt(tokens[1]);
            x = Integer.parseInt(tokens[2]);
        } catch (Exception e){
            return s.toString();
        }

        if(floor < 1 || floor > 15 || x < 0 || x > 6)
            return "";

        ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;
        int current_y = AbstractDungeon.currMapNode.y;
        if(current_y >= 15)
            current_y = -1;
        ArrayList<ArrayList<MapRoomNode>> m;

        if(!(current_y == -1)) {

            m = new ArrayList<ArrayList<MapRoomNode>>();

            ArrayList<MapRoomNode> current = new ArrayList<MapRoomNode>();
            current.add(AbstractDungeon.currMapNode);
            m.add(current);

            for (int i = (current_y + 1); i < map.size(); i++) {

                ArrayList<MapRoomNode> next_floor = new ArrayList<MapRoomNode>();

                for (MapRoomNode n : map.get(i)) {

                    for (MapRoomNode child : m.get(i - current_y - 1)) {
                        if (child.isConnectedTo(n)) {
                            next_floor.add(n);

                            break;
                        }
                    }

                }

                m.add(next_floor);

            }
        }else{
            m = map;
        }

        ArrayList<MapRoomNode> curr = new ArrayList<MapRoomNode>();
        ArrayList<MapRoomNode> prev = new ArrayList<MapRoomNode>();

        ArrayList<MapRoomNode> inspected = new ArrayList<MapRoomNode>();

        if(current_y == -1)
            current_y = 0;

        for(MapRoomNode child : m.get(floor - current_y - 1)){
            if(child.x == x){
                prev.add(child);
                inspected.add(child);
                destination = child;
                s.append("Floor ").append(floor).append("\r\n");
                s.append(Map.nodeType(child)).append(x).append("\r\n");
                break;
            }
        }

        if(prev.size() == 0)
            return s.toString();

        for(int i = (floor - current_y - 2);i>=0;i--){

            s.append("Floor ").append(i + current_y + 1).append("\r\n");

            for(MapRoomNode node : m.get(i)){

                boolean connected = false;

                for(MapRoomNode parent : prev){
                    if(node.isConnectedTo(parent)){
                        connected = true;
                        s.append(Map.nodeType(node)).append(node.x).append("\r\n");
                        curr.add(node);
                        inspected.add(node);
                        break;
                    }
                }

            }

            prev.clear();
            prev.addAll(curr);
            curr.clear();

        }

        inspected_map = inspected;
        has_inspected = true;

        return s.toString();

    }

}
