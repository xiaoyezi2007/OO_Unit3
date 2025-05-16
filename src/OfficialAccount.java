import com.oocourse.spec3.main.OfficialAccountInterface;
import com.oocourse.spec3.main.PersonInterface;

import java.util.HashMap;

public class OfficialAccount implements OfficialAccountInterface {
    private String name;
    private int id;
    private int ownerId;
    private HashMap<Integer, PersonInterface> followers = new HashMap<>();
    private HashMap<Integer, Integer> contributions = new HashMap<>();//personId-contribution
    private HashMap<Integer, Integer> articles = new HashMap<>();//articleId-contributor

    public OfficialAccount(int ownerId, int id, String name) {
        this.ownerId = ownerId;
        this.id = id;
        this.name = name;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public void addFollower(PersonInterface person) {
        if (!containsFollower(person)) {
            followers.put(person.getId(), person);
            contributions.put(person.getId(), 0);
        }
    }

    @Override
    public boolean containsFollower(PersonInterface person) {
        return followers.containsKey(person.getId());
    }

    @Override
    public void addArticle(PersonInterface person, int id) {
        if (!containsArticle(id)) {
            articles.put(id, person.getId());
            contributions.put(person.getId(), contributions.get(person.getId()) + 1);
        }
    }

    public void dispatchArticle(int articleId) {
        for (PersonInterface person : followers.values()) {
            ((Person) person).receiveArticle(articleId);
        }
    }

    @Override
    public boolean containsArticle(int id) {
        return articles.containsKey(id);
    }

    public void delArticle(int id) {
        contributions.put(articles.get(id), contributions.get(articles.get(id)) - 1);
        for (PersonInterface person : followers.values()) {
            ((Person) person).notReceiveArticle(id);
        }
        removeArticle(id);
    }

    @Override
    public void removeArticle(int id) {
        if (containsArticle(id)) {
            articles.remove(id);
        }
    }

    @Override
    public int getBestContributor() {
        int bestContributor = ownerId;
        int maxSum = Integer.MIN_VALUE;
        for (Integer id : contributions.keySet()) {
            if (contributions.get(id) > maxSum
                || (contributions.get(id) == maxSum && id < bestContributor)) {
                maxSum = contributions.get(id);
                bestContributor = id;
            }
        }
        return bestContributor;
    }
}
