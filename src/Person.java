import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Person  implements PersonInterface {
    private String name;
    private int age;
    private int id;
    private HashMap<Integer, TagInterface> tags = new HashMap<>(); //tagId - tag
    private HashMap<Integer, Integer> value = new HashMap<>(); //personId - value
    private HashMap<Integer, PersonInterface> acquaintance = new HashMap<>(); //personId - person
    private int bestFriendId;
    private ArrayList<Integer> receivedArticles = new ArrayList<>();
    private int money = 0;
    private int socialValue = 0;
    private ArrayList<MessageInterface> messages = new ArrayList<>();

    public Person(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public HashMap<Integer, PersonInterface> getAcquaintance() {
        return acquaintance;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public boolean containsTag(int id) {
        return tags.containsKey(id);
    }

    @Override
    public TagInterface getTag(int id) {
        if (tags.containsKey(id)) {
            return tags.get(id);
        }
        return null;
    }

    @Override
    public void addTag(TagInterface tag) {
        if (!containsTag(tag.getId())) {
            tags.put(tag.getId(), tag);
        }
    }

    @Override
    public void delTag(int id) {
        tags.remove(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof PersonInterface) {
            return ((PersonInterface) obj).getId() == id;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isLinked(PersonInterface person) {
        if (person.getId() == id) {
            return true;
        }
        return acquaintance.containsKey(person.getId());
    }

    @Override
    public int queryValue(PersonInterface person) {
        if (value.containsKey(person.getId())) {
            return value.get(person.getId());
        }
        return 0;
    }

    @Override
    public List<Integer> getReceivedArticles() {
        return receivedArticles;
    }

    @Override
    public List<Integer> queryReceivedArticles() {
        List<Integer> articles = new ArrayList<>();
        for (int i = receivedArticles.size() - 1; i >= 0 && articles.size() < 5; i--) {
            articles.add(receivedArticles.get(i));
        }
        return articles;
    }

    @Override
    public void addSocialValue(int num) {
        this.socialValue += num;
    }

    @Override
    public int getSocialValue() {
        return socialValue;
    }

    @Override
    public List<MessageInterface> getMessages() {
        return messages;
    }

    @Override
    public List<MessageInterface> getReceivedMessages() {
        ArrayList<MessageInterface> ans = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0 && ans.size() < 5;i--) {
            ans.add(messages.get(i));
        }
        return ans;
    }

    @Override
    public void addMoney(int num) {
        this.money += num;
    }

    @Override
    public int getMoney() {
        return money;
    }

    public void tagDelPerson(PersonInterface person) {
        for (TagInterface tag : tags.values()) {
            if (tag.hasPerson(person)) {
                tag.delPerson(person);
            }
        }
    }

    public void updateBestFriendId() {
        if (isAcquaintanceEmpty()) {
            bestFriendId = 0;
            return;
        }
        int maxValue = Integer.MIN_VALUE;
        bestFriendId = Integer.MIN_VALUE;
        for (Integer id : acquaintance.keySet()) {
            if (value.get(id) > maxValue) {
                maxValue = value.get(id);
                bestFriendId = id;
            }
            else if (value.get(id) == maxValue) {
                if (id < bestFriendId) {
                    bestFriendId = id;
                }
            }
        }
    }

    public void addPerson(PersonInterface person, int value) {
        boolean flag = false;
        if (isAcquaintanceEmpty()) {
            bestFriendId = person.getId();
        }
        else {
            if (person.getId() == bestFriendId && value < this.value.get(bestFriendId)) {
                flag = true;
            }
            else if (value > this.value.get(bestFriendId)
                || (value == this.value.get(bestFriendId) && person.getId() < bestFriendId)) {
                bestFriendId = person.getId();
            }
        }
        acquaintance.put(person.getId(), person);
        this.value.put(person.getId(), value);
        if (flag) {
            updateBestFriendId();
        }
    }

    public void delPerson(PersonInterface person) {
        acquaintance.remove(person.getId());
        value.remove(person.getId());
        if (person.getId() == bestFriendId) {
            updateBestFriendId();
        }
    }

    public boolean isAcquaintanceEmpty() {
        return acquaintance.isEmpty();
    }

    public int queryBestAcquaintance() {
        return bestFriendId;
    }

    public void receiveMessage(MessageInterface message) {
        messages.add(message);
    }

    public void receiveArticle(int articleId) {
        receivedArticles.add(articleId);
    }

    public void notReceiveArticle(int articleId) {
        while (receivedArticles.remove(Integer.valueOf(articleId))) {}
    }

    public boolean strictEquals(PersonInterface person) {
        return person.getId() == id && name.equals(person.getName()) && age == person.getAge();
    }
}
