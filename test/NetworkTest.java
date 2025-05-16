import com.oocourse.spec3.exceptions.ArticleIdNotFoundException;
import com.oocourse.spec3.exceptions.EmojiIdNotFoundException;
import com.oocourse.spec3.exceptions.EqualEmojiIdException;
import com.oocourse.spec3.exceptions.EqualMessageIdException;
import com.oocourse.spec3.exceptions.EqualPersonIdException;
import com.oocourse.spec3.exceptions.EqualRelationException;
import com.oocourse.spec3.exceptions.MessageIdNotFoundException;
import com.oocourse.spec3.exceptions.PersonIdNotFoundException;
import com.oocourse.spec3.exceptions.RelationNotFoundException;
import com.oocourse.spec3.exceptions.TagIdNotFoundException;
import com.oocourse.spec3.main.EmojiMessageInterface;
import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.main.PersonInterface;
import javafx.util.Pair;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class NetworkTest {
    Network network = new Network();
    int ans;
    private HashMap<Integer, ArrayList<Integer>> map = new HashMap<>();


    public NetworkTest() {

    }

    @Test
    public void delColdEmojiTest() {
        Network network = new Network();
        PersonInterface personA = new Person(1,"A",18);
        PersonInterface personB = new Person(2,"B",19);
        PersonInterface personC = new Person(3,"C",20);
        PersonInterface personD = new Person(4,"D",21);
        PersonInterface personE = new Person(5,"E",22);
        PersonInterface personF = new Person(6,"F",23);
        PersonInterface personG = new Person(7,"G",24);
        PersonInterface personH = new Person(8,"H",25);
        try {
            network.addPerson(personA);
            network.addPerson(personB);
            network.addPerson(personC);
            network.addPerson(personD);
            network.addPerson(personE);
            network.addPerson(personF);
            network.addPerson(personG);
            network.addPerson(personH);
        } catch (EqualPersonIdException e) {
            throw new RuntimeException(e);
        }
        try {
            network.addRelation(1,2,1);
            network.addRelation(1,3,2);
            network.addRelation(1,4,3);
            network.addRelation(1,5,4);
            network.addRelation(1,6,5);
            network.addRelation(1,7,6);
            network.addRelation(1,8,7);
            network.addRelation(2,3,8);
            network.addRelation(2,4,9);
            network.addRelation(2,5,10);
            network.addRelation(2,6,11);
            network.addRelation(2,7,12);
            network.addRelation(2,8,13);
        } catch (PersonIdNotFoundException | EqualRelationException e) {
            throw new RuntimeException(e);
        }
        try {
            network.storeEmojiId(1);
            network.storeEmojiId(2);
            network.storeEmojiId(3);
            network.storeEmojiId(4);
            network.storeEmojiId(5);
            network.storeEmojiId(6);
            network.storeEmojiId(7);
        } catch (EqualEmojiIdException e) {
            throw new RuntimeException(e);
        }
        EmojiMessageInterface emojiMessage1 = new EmojiMessage(1,1,personA,personB);
        EmojiMessageInterface emojiMessage2 = new EmojiMessage(2,1,personA,personC);
        EmojiMessageInterface emojiMessage3 = new EmojiMessage(3,1,personA,personD);
        EmojiMessageInterface emojiMessage4 = new EmojiMessage(4,1,personA,personE);
        EmojiMessageInterface emojiMessage5 = new EmojiMessage(5,1,personA,personF);
        EmojiMessageInterface emojiMessage6 = new EmojiMessage(6,2,personA,personG);
        EmojiMessageInterface emojiMessage7 = new EmojiMessage(7,2,personA,personH);
        EmojiMessageInterface emojiMessage8 = new EmojiMessage(8,2,personB,personA);
        EmojiMessageInterface emojiMessage9 = new EmojiMessage(9,2,personB,personC);
        EmojiMessageInterface emojiMessage10 = new EmojiMessage(10,3,personB,personD);
        EmojiMessageInterface emojiMessage11 = new EmojiMessage(11,3,personB,personE);
        EmojiMessageInterface emojiMessage12 = new EmojiMessage(12,4,personB,personF);
        EmojiMessageInterface emojiMessage13 = new EmojiMessage(13,1,personB,personG);
        try {
            network.addMessage(emojiMessage1);
            network.addMessage(emojiMessage2);
            network.addMessage(emojiMessage3);
            network.addMessage(emojiMessage4);
            network.addMessage(emojiMessage5);
            network.addMessage(emojiMessage6);
            network.addMessage(emojiMessage7);
            network.addMessage(emojiMessage8);
            network.addMessage(emojiMessage9);
            network.addMessage(emojiMessage10);
            network.addMessage(emojiMessage11);
            network.addMessage(emojiMessage12);
            network.addMessage(emojiMessage13);
        } catch (EqualMessageIdException | EmojiIdNotFoundException | EqualPersonIdException |
                 ArticleIdNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            network.sendMessage(1);
            network.sendMessage(2);
            network.sendMessage(3);
            network.sendMessage(4);
            network.sendMessage(5);
            network.sendMessage(6);
            network.sendMessage(7);
            network.sendMessage(8);
            network.sendMessage(9);
            network.sendMessage(10);
            network.sendMessage(12);
        } catch (RelationNotFoundException | MessageIdNotFoundException | TagIdNotFoundException e) {
            throw new RuntimeException(e);
        }
        network.deleteColdEmoji(4);
        MessageInterface[] messages = network.getMessages();
        int[] emojiIdList = network.getEmojiIdList();
        int[] emojiHeatList = network.getEmojiHeatList();
        assertEquals(1, messages.length);
        assertEquals(messages[0], emojiMessage13);
        assertEquals(2, emojiIdList.length);
        assertEquals(2, emojiHeatList.length);
        if (emojiIdList[0] == 1) {
            assertEquals(emojiIdList[1], 2);
            assertEquals(emojiHeatList[1], 4);
            assertEquals(emojiHeatList[0], 5);
        }
        else {
            assertEquals(emojiIdList[0], 2);
            assertEquals(emojiHeatList[0], 4);
            assertEquals(emojiIdList[1], 1);
            assertEquals(emojiHeatList[1], 5);
        }
    }
}