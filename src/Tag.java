import java.util.HashMap;

import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;

public class Tag implements TagInterface {
    private final int id;
    private final HashMap<Integer, PersonInterface> persons = new HashMap<>();
    private int valueSum;

    public Tag(int id) {
        this.id = id;
        valueSum = 0;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof TagInterface) {
            return ((TagInterface) obj).getId() == id;
        }
        return false;
    }

    public void addValueSum(int value) {
        valueSum += value;
    }

    @Override
    public void addPerson(PersonInterface person) {
        if (!hasPerson(person)) {
            persons.put(person.getId(), person);
        }
        for (PersonInterface p : persons.values()) {
            if (p.isLinked(person)) {
                valueSum += p.queryValue(person);
            }
            if (person.isLinked(p)) {
                valueSum += person.queryValue(p);
            }
        }
    }

    @Override
    public boolean hasPerson(PersonInterface person) {
        return persons.containsKey(person.getId());
    }

    @Override
    public int getValueSum() {
        return valueSum;
    }

    @Override
    public int getAgeMean() {
        if (persons.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (PersonInterface p : persons.values()) {
            sum += p.getAge();
        }
        return sum / persons.size();
    }

    @Override
    public int getAgeVar() {
        if (persons.isEmpty()) {
            return 0;
        }
        int mean = getAgeMean();
        int sum = 0;
        for (PersonInterface p : persons.values()) {
            sum += (p.getAge() - mean) * (p.getAge() - mean);
        }
        return sum / persons.size();
    }

    @Override
    public void delPerson(PersonInterface person) {
        persons.remove(person.getId());
        for (PersonInterface p : persons.values()) {
            if (p.isLinked(person)) {
                valueSum -= p.queryValue(person);
            }
            if (person.isLinked(p)) {
                valueSum -= person.queryValue(p);
            }
        }
    }

    @Override
    public int getSize() {
        return persons.size();
    }

    public void addSocialValue(int value) {
        for (PersonInterface p : persons.values()) {
            p.addSocialValue(value);
        }
    }

    public void addMoney(int value) {
        for (PersonInterface p : persons.values()) {
            p.addMoney(value);
        }
    }

    public void receiveArticle(int id) {
        for (PersonInterface p : persons.values()) {
            ((Person) p).receiveArticle(id);
        }
    }

    public void receiveMessage(MessageInterface message) {
        for (PersonInterface p : persons.values()) {
            ((Person) p).receiveMessage(message);
        }
    }
}
